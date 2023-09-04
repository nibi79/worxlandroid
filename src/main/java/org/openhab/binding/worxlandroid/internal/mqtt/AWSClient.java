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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.crt.mqtt.MqttClientConnection;
import software.amazon.awssdk.crt.mqtt.MqttClientConnectionEvents;
import software.amazon.awssdk.crt.mqtt.MqttMessage;
import software.amazon.awssdk.crt.mqtt.QualityOfService;
import software.amazon.awssdk.iot.AwsIotMqttConnectionBuilder;

/**
 * {@link AWSClient} AWS client
 *
 * @author Nils - Initial contribution
 */
@NonNullByDefault
public class AWSClient implements MqttClientConnectionEvents {
    private static final QualityOfService QOS = QualityOfService.AT_MOST_ONCE;
    private static final String CUSTOM_AUTHORIZER_NAME = "com-worxlandroid-customer";
    private static final String MQTT_USERNAME = "openhab";

    private final ScheduledExecutorService scheduler = ThreadPoolManager.getScheduledPool("AWSClient");
    private final Logger logger = LoggerFactory.getLogger(AWSClient.class);
    private final Map<String, Consumer<MqttMessage>> subscriptions = new HashMap<>();
    private final AWSClientCallbackI clientCallback;

    private Optional<MqttClientConnection> connection = Optional.empty();
    private LocalDateTime lastResumed = LocalDateTime.MIN;
    private LocalDateTime interrupted = LocalDateTime.MIN;
    private String clientId = "";
    private String endpoint = "";

    public AWSClient(AWSClientCallbackI clientCallback) {
        this.clientCallback = clientCallback;
    }

    public boolean initialize(String clientEndpoint, String userId, String productUuid, String accessToken) {
        this.endpoint = clientEndpoint;
        this.clientId = "WX/USER/%s/%s/%s".formatted(userId, MQTT_USERNAME, productUuid);
        return createNewConnection(accessToken);
    }

    private boolean createNewConnection(String token) {
        String[] tok = token.replaceAll("_", "/").replaceAll("-", "+").split("\\.");
        String customAuthorizerSig = tok[2];
        String jwt = tok[0] + "." + tok[1];

        try {
            MqttClientConnection mqttClient = AwsIotMqttConnectionBuilder.newDefaultBuilder()
                    .withCustomAuthorizer(MQTT_USERNAME, CUSTOM_AUTHORIZER_NAME, customAuthorizerSig, null,
                            MQTT_USERNAME, token)
                    .withWebsockets(true).withClientId(clientId).withCleanSession(false).withEndpoint(endpoint)
                    .withUsername(MQTT_USERNAME).withConnectionEventCallbacks(this).withKeepAliveSecs(300)
                    .withWebsocketHandshakeTransform(handshakeArgs -> {
                        HttpRequest httpRequest = handshakeArgs.getHttpRequest();
                        httpRequest.addHeader("x-amz-customauthorizer-name", CUSTOM_AUTHORIZER_NAME);
                        httpRequest.addHeader("x-amz-customauthorizer-signature", customAuthorizerSig);
                        httpRequest.addHeader("jwt", jwt);
                        handshakeArgs.complete(httpRequest);
                    }).build();
            mqttClient.connect().thenAccept(b -> {
                boolean sessionPresent = b;
                // sessionPresent = mqttClient.connect().get();
                logger.debug("connected to {} session!", (!sessionPresent ? "new" : "existing"));
                onConnectionResumed(sessionPresent);
                if (sessionPresent) {
                    connection = Optional.of(mqttClient);
                }
            });
        } catch (/* InterruptedException | ExecutionException | */UnsupportedEncodingException e) {
            logger.error("Error establishing MQTT connection to AWS: {}", e.getMessage());
        }
        return connection.isPresent();
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
        // is lastResumed between interrupted and now?
        boolean isBetween = lastResumed.isAfter(interrupted) && lastResumed.isBefore(LocalDateTime.now());
        logger.debug("lastResumed: {}  interrupted: {} in: {}", lastResumed, interrupted, isBetween);
        return isBetween;
    }

    @Override
    public void onConnectionResumed(boolean sessionPresent) {
        lastResumed = LocalDateTime.now();
        logger.debug("last connection resume {}", lastResumed);
        if (sessionPresent) {
            clientCallback.onAWSConnectionSuccess();
        } else {
            clientCallback.onAWSConnectionClosed();
        }
    }

    public void disconnect() {
        connection.ifPresent(mqttClient -> {
            // subscriptions.keySet().stream().forEach(mqttClient::unsubscribe);
            mqttClient.disconnect();
            mqttClient.close();
        });
        connection = Optional.empty();
    }

    public void subscribe(String topic, Consumer<MqttMessage> handler) {
        connection.ifPresent(mqttClient -> {
            subscriptions.put(topic, handler);
            mqttClient.subscribe(topic, QOS, handler);
        });
    }

    public void unsubscribe(String topic) {
        connection.ifPresent(mqttClient -> {
            subscriptions.remove(topic);
            mqttClient.unsubscribe(topic);
        });
    }

    public void publish(String topic, String payload) {
        connection.ifPresent(mqttClient -> {
            MqttMessage mqttMessage = new MqttMessage(topic, payload.getBytes(StandardCharsets.UTF_8), QOS);
            mqttClient.publish(mqttMessage);
        });
    }

    public boolean refreshConnection(String token) {
        logger.debug("reconnecting...");

        disconnect();
        boolean connected = createNewConnection(token);
        if (connected) {
            logger.debug("reconnected");
            subscriptions.forEach(this::subscribe);
        }
        return connected;
    }
}
