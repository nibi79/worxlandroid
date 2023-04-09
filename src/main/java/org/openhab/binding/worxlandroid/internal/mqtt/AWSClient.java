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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNull;
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
public class AWSClient implements AWSClientI {
    private final Logger logger = LoggerFactory.getLogger(AWSClient.class);

    private static final QualityOfService QOS = QualityOfService.AT_MOST_ONCE;
    private MqttClientConnection connection;
    private AWSClientCallback clientCallback;
    private String endpoint;

    private String clientId;
    private String usernameMqtt;
    private String customAuthorizerName;

    private LocalDateTime lastResumed;
    private LocalDateTime interrupted;
    private CompletableFuture<Boolean> connected;
    private HashSet<AWSTopicI> subscriptions = new HashSet<>();

    protected final ScheduledExecutorService scheduler = ThreadPoolManager.getScheduledPool("AWSClient");

    private Runnable checkImmediatlyResumed = new Runnable() {
        @Override
        public void run() {

            if (!isImmediatlyResumed()) {

                clientCallback.onAWSConnectionClosed();
            }
        }
    };

    /**
     * @param clientEndpoint
     * @param clientId
     * @param clientCallback
     * @param usernameMqtt
     * @param customAuthorizerName
     * @param customAuthorizerSig
     * @param jwt
     * @throws UnsupportedEncodingException
     */
    public AWSClient(String clientEndpoint, String clientId, AWSClientCallback clientCallback, String usernameMqtt,
            String customAuthorizerName, String token) throws UnsupportedEncodingException {

        this.clientCallback = clientCallback;
        this.endpoint = clientEndpoint;
        this.clientId = clientId;
        this.usernameMqtt = usernameMqtt;
        this.customAuthorizerName = customAuthorizerName;

        createNewConnection(token);
    }

    /**
     * @param token
     * @throws UnsupportedEncodingException
     */
    private void createNewConnection(String token) throws UnsupportedEncodingException {

        String[] tok = token.replaceAll("_", "/").replaceAll("-", "+").split("\\.");
        String customAuthorizerSig = tok[2];
        String jwt = tok[0] + "." + tok[1];

        connection = AwsIotMqttConnectionBuilder.newDefaultBuilder()
                .withCustomAuthorizer(usernameMqtt, customAuthorizerName, customAuthorizerSig, null)
                .withWebsockets(true).withClientId(clientId).withCleanSession(false).withEndpoint(endpoint)
                .withUsername(usernameMqtt).withConnectionEventCallbacks(this).withKeepAliveSecs(1200)
                .withWebsocketHandshakeTransform((handshakeArgs) -> {
                    HttpRequest httpRequest = handshakeArgs.getHttpRequest();
                    httpRequest.addHeader("x-amz-customauthorizer-name", customAuthorizerName);
                    httpRequest.addHeader("x-amz-customauthorizer-signature", customAuthorizerSig);
                    httpRequest.addHeader("jwt", jwt);
                    handshakeArgs.complete(httpRequest);
                }).build();
    }

    @Override
    public String getEndpoint() {
        return endpoint;
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public String getUsernameMqtt() {
        return usernameMqtt;
    }

    @Override
    public boolean connect() {
        try {
            connected = connection.connect();

            boolean sessionPresent = connected.get();
            logger.debug("connected to {} session!", (!sessionPresent ? "new" : "existing"));
            onConnectionResumed(sessionPresent);

            return true;

        } catch (InterruptedException | ExecutionException e) {
            logger.error("Exception: {}", e.getLocalizedMessage());
        }

        return false;
    }

    @Override
    public void onConnectionInterrupted(int errorCode) {

        interrupted = LocalDateTime.now();

        logger.debug("connection interrupted errorcode: {}", errorCode);
        if (errorCode != 0) {
            scheduler.schedule(checkImmediatlyResumed, 10, TimeUnit.SECONDS);
        }
    }

    /**
     * workaround -> after 20 minutes the connection is interrupted but immediately resumed (~0,5sec).
     * ConnectionBuilder with ".withKeepAliveSecs(300)" doesn't work
     *
     * @return
     */
    private boolean isImmediatlyResumed() {
        // is lastResumed betweeen interrupted und now?
        logger.debug("lastResumed: {}  interrupted {} im: {}", lastResumed, interrupted,
                lastResumed != null && lastResumed.isAfter(interrupted) && lastResumed.isBefore(LocalDateTime.now()));
        if (lastResumed != null && lastResumed.isAfter(interrupted) && lastResumed.isBefore(LocalDateTime.now())) {
            return true;
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
    public CompletableFuture<Void> disconnect() {
        unsubsribeTopics();
        return connection.disconnect();
    }

    @Override
    public void subscribe(@NonNull AWSTopicI awsTopic) {
        subscriptions.add(awsTopic);
        connection.subscribe(awsTopic.getTopic(), QOS, t -> awsTopic.onMessage(t));
    }

    @Override
    public void publish(AWSMessageI awsMessageI) {
        byte[] bytes = awsMessageI.getPayload().getBytes(StandardCharsets.UTF_8);
        MqttMessage mqttMessage = new MqttMessage(awsMessageI.getTopic(), bytes, QOS);

        connection.publish(mqttMessage);
    }

    @Override
    public boolean refreshConnection(String token) throws UnsupportedEncodingException {

        if (connection == null) {
            return false;
        }

        logger.debug("reconnecting...");

        disconnect();
        connection.close();

        createNewConnection(token);
        boolean result = connect();
        subsribeTopics();

        // TODO NB return?
        return result;
    }

    private void subsribeTopics() {
        for (AWSTopicI awsTopic : subscriptions) {
            subscribe(awsTopic);
        }
    }

    private void unsubsribeTopics() {
        for (AWSTopicI awsTopic : subscriptions) {
            connection.unsubscribe(awsTopic.getTopic());
        }
    }
}
