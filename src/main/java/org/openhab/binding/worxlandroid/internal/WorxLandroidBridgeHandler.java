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
package org.openhab.binding.worxlandroid.internal;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.worxlandroid.internal.config.BridgeConfiguration;
import org.openhab.binding.worxlandroid.internal.discovery.MowerDiscoveryService;
import org.openhab.binding.worxlandroid.internal.mqtt.AWSClient;
import org.openhab.binding.worxlandroid.internal.mqtt.AWSClientCallback;
import org.openhab.binding.worxlandroid.internal.mqtt.AWSClientI;
import org.openhab.binding.worxlandroid.internal.mqtt.AWSException;
import org.openhab.binding.worxlandroid.internal.mqtt.AWSMessageI;
import org.openhab.binding.worxlandroid.internal.mqtt.AWSTopicI;
import org.openhab.binding.worxlandroid.internal.webapi.WebApiException;
import org.openhab.binding.worxlandroid.internal.webapi.WorxLandroidWebApiImpl;
import org.openhab.binding.worxlandroid.internal.webapi.response.ProductItemsStatusResponse;
import org.openhab.binding.worxlandroid.internal.webapi.response.UsersMeResponse;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link WorxLandroidBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Nils - Initial contribution
 */
@NonNullByDefault
public class WorxLandroidBridgeHandler extends BaseBridgeHandler implements AWSClientCallback {

    private final Logger logger = LoggerFactory.getLogger(WorxLandroidBridgeHandler.class);

    private WorxLandroidWebApiImpl apiHandler;
    private @Nullable MowerDiscoveryService discoveryService;

    private @Nullable AWSClientI awsClient;

    /**
     * Defines a runnable for a discovery
     */
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (discoveryService != null) {
                discoveryService.discoverMowers();
            }
        }
    };

    private Runnable refreshConnectionToken = new Runnable() {
        @Override
        public void run() {

            if (isBridgeOnline()) {
                // TODO NB
                if (!apiHandler.isTokenValid()) {
                    logger.debug("refreshConnectionToken -> reconnectToWorx");
                    reconnectToWorx();
                }
            }
        }
    };

    /**
     * @param bridge
     * @param httpClient
     */
    public WorxLandroidBridgeHandler(Bridge bridge, HttpClient httpClient) {
        super(bridge);
        apiHandler = new WorxLandroidWebApiImpl(httpClient);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public void initialize() {

        try {

            BridgeConfiguration config = getConfigAs(BridgeConfiguration.class);
            logger.debug("try to connect to API...");
            boolean connected = apiHandler.connect(config.getWebapiUsername(), config.getWebapiPassword());
            if (!connected) {
                logger.debug("API not connected - retry...");
                connected = apiHandler.connect(config.getWebapiUsername(), config.getWebapiPassword());
            }

            logger.debug("API connected: {}", connected);

            if (connected) {

                UsersMeResponse usersMeResponse = apiHandler.retrieveWebInfo();

                Map<String, String> props = usersMeResponse.getDataAsPropertyMap();
                @Nullable
                String userId = props.get("id");

                if (userId == null) {
                    // TODO NB
                    return;
                }

                updateThing(editThing().withProperties(props).build());

                ProductItemsStatusResponse productItemsStatusResponse = apiHandler.retrieveDeviceStatus(userId);
                Map<String, String> firstDeviceProps = productItemsStatusResponse.getArrayDataAsPropertyMap();

                @Nullable
                String awsMqttEndpoint = firstDeviceProps.get("mqtt_endpoint");
                @Nullable
                String deviceId = firstDeviceProps.get("uuid");
                String customAuthorizerName = "com-worxlandroid-customer";
                String usernameMqtt = "openhab";
                String clientId = String.format("WX/USER/%s/%s/%s", userId, usernameMqtt, deviceId);

                awsClient = new AWSClient(awsMqttEndpoint, clientId, this, usernameMqtt, customAuthorizerName,
                        apiHandler.getAccessToken());

                logger.debug("try to connect to AWS...");
                boolean awsConnected = awsClient.connect();
                logger.debug("AWS connected: {}", awsConnected);

                // TODO NB start referesh token job, maybe expire_in and one shot instead of use periodic trigger
                scheduler.scheduleWithFixedDelay(refreshConnectionToken, 60, 60, TimeUnit.SECONDS);

                // Trigger discovery of mowers
                scheduler.submit(runnable);

            } else {
                updateStatus(ThingStatus.UNKNOWN, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Error connecting to Worx Landroid WebApi!");
            }
        } catch (WebApiException | UnsupportedEncodingException e) {
            logger.error("Iniialization error - class: {}", e.getClass().getName());
            logger.error("Iniialization error - message: {}", e.getMessage());
            logger.error("Iniialization error - stacktrace: {}", e.getStackTrace().toString());
            logger.error("Iniialization error - toString: {}", e.toString());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Error: " + e.getMessage());
        }
    }

    @Override
    public void dispose() {

        if (awsClient != null) {
            awsClient.disconnect();
        }
        super.dispose();
    }

    /**
     * @return
     */
    public WorxLandroidWebApiImpl getWorxLandroidWebApiImpl() {

        return apiHandler;
    }

    /**
     * @return
     */
    public boolean isBridgeOnline() {
        // TODO NB
        return getThing().getStatus() == ThingStatus.ONLINE;
    }

    /**
     * @param discoveryService
     */
    public void setDiscovery(MowerDiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    /**
     *
     */
    public boolean reconnectToWorx() {
        try {

            // TODO NB
            if (!apiHandler.isTokenValid()) {
                logger.debug("first try to refresh token...");
                if (!apiHandler.refreshToken()) {
                    logger.debug("first try failed -> second try to refresh token...");
                    apiHandler.refreshToken();
                }
            }

            if (apiHandler.isTokenValid()) {
                logger.debug("try to reconnect to AWS...");
                boolean connected = awsClient.refreshConnection(apiHandler.getAccessToken());
                logger.debug("AWS reconnected: {}", connected);
                return connected;
            }
        } catch (UnsupportedEncodingException e) {
            logger.error("Iniialization error - class: {}", e.getClass().getName());
            logger.error("Iniialization error - message: {}", e.getMessage());
            logger.error("Iniialization error - stacktrace: {}", e.getStackTrace().toString());
            logger.error("Iniialization error - toString: {}", e.toString());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Error: " + e.getMessage());
        }

        return false;
    }

    /**
     * @param awsTopic
     * @throws AWSIotException
     */
    public void subcribeTopic(@Nullable AWSTopicI awsTopic) throws AWSException {

        if (awsTopic == null) {
            return;
        }
        if (awsClient == null) {
            logger.error("MqttClient is not initialized. Cannot subsribe to topic -> {}", awsTopic.getTopic());
            return;
        }
        logger.debug("subsribe to topic -> {}", awsTopic.getTopic());
        awsClient.subscribe(awsTopic);
    }

    /**
     * @param awsMessage
     * @throws AWSIotException
     */
    @SuppressWarnings("null")
    public void publishMessage(AWSMessageI awsMessage) throws AWSException {

        if (awsClient == null) {
            logger.error("MqttClient is not initialized. Cannot publish message to topic -> {}", awsMessage.getTopic());
            return;
        }

        logger.debug("publish topic -> {}", awsMessage.getTopic());
        logger.debug("publish message -> {}", awsMessage.getPayload());
        awsClient.publish(awsMessage);
    }

    @Override
    public void onAWSConnectionSuccess() {
        logger.debug("AWS connection is available");
        if (!isBridgeOnline()) {
            updateStatus(ThingStatus.ONLINE);
        }
    }

    @Override
    public void onAWSConnectionClosed() {
        logger.debug("AWS connection closed -> reconnectToWorx");
        boolean reconnected = reconnectToWorx();

        // TODO NB stauts prüfen um log nachrichten zu vermeiden???
        if (!reconnected && getThing().getStatus() != ThingStatus.OFFLINE) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "AWS connection closed!");
        }
    }
}
