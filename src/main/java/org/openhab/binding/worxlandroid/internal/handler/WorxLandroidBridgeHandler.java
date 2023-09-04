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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.worxlandroid.internal.api.WebApiException;
import org.openhab.binding.worxlandroid.internal.api.WorxApiHandler;
import org.openhab.binding.worxlandroid.internal.api.dto.ProductItemStatus;
import org.openhab.binding.worxlandroid.internal.api.dto.UsersMeResponse;
import org.openhab.binding.worxlandroid.internal.config.WebApiConfiguration;
import org.openhab.binding.worxlandroid.internal.mqtt.AWSClient;
import org.openhab.binding.worxlandroid.internal.mqtt.AWSClientCallbackI;
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

import software.amazon.awssdk.crt.mqtt.MqttMessage;

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
    private final OAuthClientService oAuthClientService;
    private final WorxApiHandler apiHandler;
    private final AWSClient awsClient;
    private final OAuthFactory oAuthFactory;

    // private Optional<ScheduledFuture<?>> reconnectJob = Optional.empty();
    private String accessToken = "";

    public WorxLandroidBridgeHandler(Bridge bridge, WorxApiHandler apiHandler, OAuthFactory oAuthFactory) {
        super(bridge);
        this.apiHandler = apiHandler;
        this.awsClient = new AWSClient(this);
        this.oAuthFactory = oAuthFactory;
        this.oAuthClientService = oAuthFactory.createOAuthClientService(getThing().getUID().getAsString(),
                APIURL_OAUTH_TOKEN, null, CLIENT_ID, null, "*", true);
        oAuthClientService.addAccessTokenRefreshListener(this);
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
        try {
            this.accessToken = oAuthClientService
                    .getAccessTokenByResourceOwnerPasswordCredentials(config.username, config.password, "*")
                    .getAccessToken();
            ProductItemStatus productItemStatus = Objects
                    .requireNonNull(apiHandler.retrieveDeviceStatus(accessToken).get(0));

            if (thing.getProperties().isEmpty()) {
                UsersMeResponse user = apiHandler.retrieveUsersMe(accessToken);
                Map<String, String> properties = new HashMap<>(apiHandler.getDeserializer().toMap(user));
                properties.put("mqtt_endpoint", productItemStatus.mqttEndpoint);
                properties.put("uuid", productItemStatus.uuid);
                updateProperties(properties);
            }
            oAuthClientService.refreshToken();
        } catch (IOException | WebApiException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        } catch (OAuthException | OAuthResponseException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "@text/oauth-connection-error");
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Landroid Bridge is read-only and does not handle commands");
    }

    // private void disposeReconnectJob() {
    // reconnectJob.ifPresent(job -> job.cancel(true));
    // reconnectJob = Optional.empty();
    // }

    @Override
    public void dispose() {
        oAuthClientService.removeAccessTokenRefreshListener(this);
        oAuthFactory.ungetOAuthService(getThing().getUID().getAsString());
        awsClient.disconnect();
        // disposeReconnectJob();

        super.dispose();
    }

    public boolean isOnline() {
        return getThing().getStatus() == ThingStatus.ONLINE;
    }

    // private void reconnectToWorx() {
    // logger.debug("try to reconnect to AWS...");
    // if (awsClient.refreshConnection(accessToken)) {
    // updateStatus(ThingStatus.ONLINE);
    // } else {
    // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Reconnection unsuccessfull");
    // }
    // }

    public void subcribeTopic(String topic, Consumer<MqttMessage> handler) {
        awsClient.subscribe(topic, handler);
        logger.debug("subscribed to topic: {}", topic);
    }

    public void unsubcribeTopic(String topic) {
        awsClient.unsubscribe(topic);
        logger.debug("unsubscribed from topic: {}", topic);
    }

    public void publishMessage(String topic, String cmd) {
        logger.debug("publish on topic: '{}' - message: '{}'", topic, cmd);
        awsClient.publish(topic, cmd);
    }

    public void publishMessage(String topic, Object command) {
        publishMessage(topic, apiHandler.getDeserializer().toJson(command));
    }

    @Override
    public void onAWSConnectionSuccess() {
        logger.debug("AWS connection is available");
        if (!isOnline()) {
            updateStatus(ThingStatus.ONLINE);
        }
    }

    @Override
    public void onAWSConnectionClosed() {
        logger.debug("AWS connection closed -> refreshing token to reconnectToWorx");
        try {
            oAuthClientService.refreshToken();
        } catch (OAuthException | IOException | OAuthResponseException e) {
            logger.warn("Error refreshing token: {}", e.getMessage());
        }
        // TODO NB stauts prüfen um log nachrichten zu vermeiden???
        // if (!reconnectToWorx() && getThing().getStatus() != ThingStatus.OFFLINE) {
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "AWS connection closed!");
        // }
    }

    @Override
    public void onAccessTokenResponse(AccessTokenResponse tokenResponse) {
        accessToken = tokenResponse.getAccessToken();
        Map<String, String> properties = editProperties();
        awsClient.disconnect();
        if (awsClient.initialize(Objects.requireNonNull(properties.get("mqtt_endpoint")),
                Objects.requireNonNull(properties.get("id")), Objects.requireNonNull(properties.get("uuid")),
                accessToken)) {
            // updateStatus(ThingStatus.ONLINE);
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Reconnection to AWS unsuccessfull");
        }
        // if (isOnline()) {
        // logger.debug("refreshConnectionToken -> reconnectToWorx");
        // reconnectToWorx();
        // }
        // if (awsClient.refreshConnection(accessToken)) {
        // updateStatus(ThingStatus.ONLINE);
        // } else {
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Reconnection unsuccessfull");
        // }

        // if (config.reconnectInterval > 0) {
        // reconnectJob = Optional.of(scheduler.scheduleWithFixedDelay(() -> reconnectToWorx(), 60,
        // config.reconnectInterval, TimeUnit.SECONDS));
        // }

    }

    public @Nullable ProductItemStatus retrieveDeviceStatus(String serialNumber) throws WebApiException {
        return apiHandler.retrieveDeviceStatus(accessToken, serialNumber);
    }

    public List<ProductItemStatus> retrieveAllDevices() throws WebApiException {
        return apiHandler.retrieveDeviceStatus(accessToken);
    }
}
