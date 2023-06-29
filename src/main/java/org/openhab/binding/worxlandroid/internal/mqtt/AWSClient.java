/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.worxlandroid.internal.mqtt;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.crt.mqtt.MqttClientConnection;
import software.amazon.awssdk.crt.mqtt.MqttMessage;
import software.amazon.awssdk.crt.mqtt.QualityOfService;
import software.amazon.awssdk.iot.AwsIotMqttConnectionBuilder;

/**
 * {@link AWSClient} AWS client
 *
 * @author Nils - Initial contribution
 */
@NonNullByDefault
public class AWSClient implements AWSClientI {
    private static final QualityOfService QOS = QualityOfService.AT_MOST_ONCE;
    private static final String CUSTOM_AUTHORIZER_NAME = "com-worxlandroid-customer";
    private static final String MQTT_USERNAME = "openhab";

    private final ScheduledExecutorService scheduler = ThreadPoolManager.getScheduledPool("AWSClient");
    private final Logger logger = LoggerFactory.getLogger(AWSClient.class);
    private final String clientId;
    private final AWSClientCallbackI clientCallback;
    private final String endpoint;

    private Optional<MqttClientConnection> connection = Optional.empty();
    private HashSet<AWSTopicI> subscriptions = new HashSet<>();

    private @Nullable LocalDateTime lastResumed;
    private @Nullable LocalDateTime interrupted;

    public AWSClient(String clientEndpoint, AWSClientCallbackI clientCallback, String token, String userId,
            String productUuid) throws UnsupportedEncodingException {
        this.clientCallback = clientCallback;
        this.endpoint = clientEndpoint;
        this.clientId = "WX/USER/%s/%s/%s".formatted(userId, MQTT_USERNAME, productUuid);

        createNewConnection(token);
    }

    private void createNewConnection(String token) throws UnsupportedEncodingException {
        String[] tok = token.replaceAll("_", "/").replaceAll("-", "+").split("\\.");
        String customAuthorizerSig = tok[2];
        String jwt = tok[0] + "." + tok[1];

        connection = Optional.of(AwsIotMqttConnectionBuilder.newDefaultBuilder()
                .withCustomAuthorizer(MQTT_USERNAME, CUSTOM_AUTHORIZER_NAME, customAuthorizerSig, null)
                .withWebsockets(true).withClientId(clientId).withCleanSession(false).withEndpoint(endpoint)
                .withUsername(MQTT_USERNAME).withConnectionEventCallbacks(this)// .withKeepAliveSecs(600)
                .withWebsocketHandshakeTransform((handshakeArgs) -> {
                    HttpRequest httpRequest = handshakeArgs.getHttpRequest();
                    httpRequest.addHeader("x-amz-customauthorizer-name", CUSTOM_AUTHORIZER_NAME);
                    httpRequest.addHeader("x-amz-customauthorizer-signature", customAuthorizerSig);
                    httpRequest.addHeader("jwt", jwt);
                    handshakeArgs.complete(httpRequest);
                }).build());
    }

    @Override
    public boolean connect() {
        return connection.map(mqttClient -> {
            try {
                CompletableFuture<Boolean> connected = mqttClient.connect();
                boolean sessionPresent = connected.get();
                logger.debug("connected to {} session!", (!sessionPresent ? "new" : "existing"));
                onConnectionResumed(sessionPresent);
                return true;
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Exception: {}", e.getLocalizedMessage());
            }
            return false;
        }).orElse(false);
    }

    @Override
    public void onConnectionInterrupted(int errorCode) {
        interrupted = LocalDateTime.now();

        logger.debug("connection interrupted errorcode: {}", errorCode);
        if (errorCode != 0) {
            scheduler.schedule(() -> {
                if (!isImmediatlyResumed()) {
                    clientCallback.onAWSConnectionClosed();
                }
            }, 10, TimeUnit.SECONDS);
        }
    }

    /**
     * workaround -> after 20 minutes the connection is interrupted but immediately resumed (~0,5sec).
     * ConnectionBuilder with ".withKeepAliveSecs(300)" doesn't work
     *
     */
    private boolean isImmediatlyResumed() {
        // is lastResumed betweeen interrupted und now?
        LocalDateTime localInterrupted = interrupted;
        LocalDateTime localLastResumed = lastResumed;
        if (localInterrupted != null && localLastResumed != null) {
            logger.debug("lastResumed: {}  interrupted {} im: {}", lastResumed, interrupted, lastResumed != null
                    && lastResumed.isAfter(localInterrupted) && localLastResumed.isBefore(LocalDateTime.now()));
            if (lastResumed != null && lastResumed.isAfter(localInterrupted)
                    && localLastResumed.isBefore(LocalDateTime.now())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onConnectionResumed(boolean sessionPresent) {
        lastResumed = LocalDateTime.now();
        logger.debug("last connection resume {}", lastResumed);
        clientCallback.onAWSConnectionSuccess();
    }

    @Override
    public @Nullable CompletableFuture<Void> disconnect() {
        return connection.map(mqttClient -> {
            subscriptions.forEach(topic -> mqttClient.unsubscribe(topic.getTopic()));
            return mqttClient.disconnect();
        }).orElse(null);
    }

    @Override
    public void subscribe(AWSTopicI awsTopic) {
        connection.ifPresent(mqttClient -> {
            subscriptions.add(awsTopic);
            mqttClient.subscribe(awsTopic.getTopic(), QOS, t -> awsTopic.onMessage(t));
        });
    }

    @Override
    public void publish(AWSMessage awsMessageI) {
        connection.ifPresent(mqttClient -> {
            byte[] bytes = awsMessageI.payload().getBytes(StandardCharsets.UTF_8);
            MqttMessage mqttMessage = new MqttMessage(awsMessageI.topic(), bytes, QOS);
            mqttClient.publish(mqttMessage);
        });
    }

    @Override
    public boolean refreshConnection(String token) throws UnsupportedEncodingException {
        connection.ifPresent(mqttClient -> {
            disconnect();
            mqttClient.close();
        });
        connection = Optional.empty();

        logger.debug("reconnecting...");

        createNewConnection(token);
        if (connect()) {
            subscriptions.forEach(this::subscribe);
            return true;
        }
        return false;
    }
}
