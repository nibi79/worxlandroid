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
package org.openhab.binding.worxlandroid.internal.handler;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.worxlandroid.internal.api.WebApiException;
import org.openhab.binding.worxlandroid.internal.api.WorxApiHandler;
import org.openhab.binding.worxlandroid.internal.api.dto.ProductItemStatus;
import org.openhab.binding.worxlandroid.internal.api.dto.UsersMeResponse;
import org.openhab.binding.worxlandroid.internal.config.WebApiConfiguration;
import org.openhab.binding.worxlandroid.internal.mqtt.AWSClient;
import org.openhab.binding.worxlandroid.internal.mqtt.AWSClientCallbackI;
import org.openhab.binding.worxlandroid.internal.mqtt.AWSClientI;
import org.openhab.binding.worxlandroid.internal.mqtt.AWSMessage;
import org.openhab.binding.worxlandroid.internal.mqtt.AWSTopicI;
import org.openhab.core.auth.client.oauth2.AccessTokenRefreshListener;
import org.openhab.core.auth.client.oauth2.AccessTokenResponse;
import org.openhab.core.auth.client.oauth2.OAuthClientService;
import org.openhab.core.auth.client.oauth2.OAuthException;
import org.openhab.core.auth.client.oauth2.OAuthFactory;
import org.openhab.core.auth.client.oauth2.OAuthResponseException;
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
 * @author Gaël L'hopital - Refactored with oAuthFactory
 */
@NonNullByDefault
public class WorxLandroidBridgeHandler extends BaseBridgeHandler
        implements AWSClientCallbackI, AccessTokenRefreshListener {
    private static final String APIURL_OAUTH_TOKEN = "https://id.eu.worx.com/" + "oauth/token";
    private static final String CLIENT_ID = "013132A8-DB34-4101-B993-3C8348EA0EBC";

    private final Logger logger = LoggerFactory.getLogger(WorxLandroidBridgeHandler.class);

    private final OAuthFactory oAuthFactory;
    private final WorxApiHandler apiHandler;

    private Optional<OAuthClientService> oAuthClientService = Optional.empty();
    private Optional<AWSClientI> awsClient = Optional.empty();
    private Optional<ScheduledFuture<?>> reconnectJob = Optional.empty();

    public WorxLandroidBridgeHandler(Bridge bridge, WorxApiHandler apiHandler, OAuthFactory oAuthFactory) {
        super(bridge);
        this.oAuthFactory = oAuthFactory;
        this.apiHandler = apiHandler;
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Landroid API bridge handler.");

        WebApiConfiguration config = getConfigAs(WebApiConfiguration.class);

        if (config.username.isBlank()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "@text/conf-error-no-username");
            return;
        }

        if (config.password.isBlank()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "@text/conf-error-no-password");
            return;
        }

        updateStatus(ThingStatus.UNKNOWN);

        OAuthClientService clientService = oAuthFactory.createOAuthClientService(getThing().getUID().getAsString(),
                APIURL_OAUTH_TOKEN, null, CLIENT_ID, null, "*", true);
        clientService.addAccessTokenRefreshListener(this);

        try {
            AccessTokenResponse token = clientService.getAccessTokenByResourceOwnerPasswordCredentials(config.username,
                    config.password, "*");

            UsersMeResponse user = apiHandler.retrieveUsersMe(token);
            ProductItemStatus productItemStatus = apiHandler.retrieveDeviceStatus(token).get(0);

            if (thing.getProperties().isEmpty()) {
                Map<String, String> properties = new HashMap<>(apiHandler.getDeserializer().toMap(user));
                properties.put("mqtt_endpoint", productItemStatus.mqttEndpoint);
                updateProperties(properties);
            }

            AWSClient localAwsClient = new AWSClient(productItemStatus.mqttEndpoint, this, token.getAccessToken(),
                    user.id, productItemStatus.uuid);

            logger.debug("try to connect to AWS...");
            boolean awsConnected = localAwsClient.connect();

            logger.debug("AWS connected: {}", awsConnected);

            awsClient = Optional.of(localAwsClient);
            oAuthClientService = Optional.of(clientService);
        } catch (OAuthException | IOException | WebApiException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        } catch (OAuthResponseException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "@text/oauth-connection-error");
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Landroid Bridge is read-only and does not handle commands");
    }

    private @Nullable AccessTokenResponse getAccessTokenResponse() {
        return oAuthClientService.map(service -> {
            try {
                return service.getAccessTokenResponse();
            } catch (OAuthException | IOException | OAuthResponseException e) {
                logger.warn("Error getting access token : {}", e.getMessage());
            }
            return null;
        }).orElse(null);
    }

    private void disposeReconnect() {
        reconnectJob.ifPresent(job -> job.cancel(true));
        reconnectJob = Optional.empty();
    }

    @Override
    public void dispose() {
        awsClient.ifPresent(client -> client.disconnect());
        awsClient = Optional.empty();

        oAuthClientService.ifPresent(oAuth -> oAuth.removeAccessTokenRefreshListener(this));
        oAuthClientService = Optional.empty();

        disposeReconnect();

        super.dispose();
    }

    public boolean isBridgeOnline() {
        return getThing().getStatus() == ThingStatus.ONLINE;
    }

    private boolean reconnectToWorx() {
        AccessTokenResponse tokenResponse = getAccessTokenResponse();
        if (tokenResponse != null) {
            return awsClient.map(client -> {
                try {
                    logger.debug("try to reconnect to AWS...");
                    boolean connected = client.refreshConnection(tokenResponse.getAccessToken());
                    logger.debug("AWS reconnected: {}", connected);
                    return connected;
                } catch (UnsupportedEncodingException e) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                            "Error: " + e.getMessage());
                }
                return false;
            }).orElse(false);
        }
        return false;
    }

    /**
     * @param awsTopic
     * @throws AWSIotException
     */
    public void subcribeTopic(AWSTopicI awsTopic) {
        awsClient.ifPresent(client -> {
            client.subscribe(awsTopic);
            logger.debug("subsribed to topic -> {}", awsTopic.getTopic());
        });
    }

    /**
     * @param awsMessage
     * @throws AWSIotException
     */
    public boolean publishMessage(String topic, String cmd) {
        AWSMessage message = new AWSMessage(topic, cmd);
        return awsClient.map(client -> {
            logger.debug("publish topic -> {}", message.topic());
            logger.debug("publish message -> {}", message.payload());
            client.publish(message);
            return true;
        }).orElse(false);
    }

    public void publishMessage(String topic, Object command) {
        publishMessage(topic, apiHandler.getDeserializer().toJson(command));
    }

    @Override
    public void onAWSConnectionSuccess() {
        logger.debug("AWS connection is available");
        if (!isBridgeOnline()) {
            updateStatus(ThingStatus.ONLINE);
            disposeReconnect();
            WebApiConfiguration config = getConfigAs(WebApiConfiguration.class);
            if (config.reconnectInterval > 0) {
                reconnectJob = Optional.of(scheduler.scheduleWithFixedDelay(() -> reconnectToWorx(), 60,
                        config.reconnectInterval, TimeUnit.SECONDS));
            }
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

    @Override
    public void onAccessTokenResponse(AccessTokenResponse tokenResponse) {
        if (isBridgeOnline()) {
            logger.debug("refreshConnectionToken -> reconnectToWorx");
            reconnectToWorx();
        }
    }

    public @Nullable ProductItemStatus retrieveDeviceStatus(String serialNumber) throws WebApiException {
        AccessTokenResponse tokenResponse = getAccessTokenResponse();
        if (tokenResponse != null) {
            return apiHandler.retrieveDeviceStatus(tokenResponse, serialNumber);
        }
        return null;
    }

    public List<ProductItemStatus> retrieveAllDevices() throws WebApiException {
        AccessTokenResponse tokenResponse = getAccessTokenResponse();
        if (tokenResponse != null) {
            return apiHandler.retrieveDeviceStatus(tokenResponse);
        }
        return List.of();
    }
}
