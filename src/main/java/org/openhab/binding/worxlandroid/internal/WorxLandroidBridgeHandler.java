/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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

import static org.openhab.binding.worxlandroid.internal.webapi.response.ApiResponse.MN_MQTTENDPOINT;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.worxlandroid.internal.config.BridgeConfiguration;
import org.openhab.binding.worxlandroid.internal.discovery.MowerDiscoveryService;
import org.openhab.binding.worxlandroid.internal.mqtt.AWSMessage;
import org.openhab.binding.worxlandroid.internal.mqtt.AWSTopic;
import org.openhab.binding.worxlandroid.internal.webapi.WebApiException;
import org.openhab.binding.worxlandroid.internal.webapi.WorxLandroidWebApiImpl;
import org.openhab.binding.worxlandroid.internal.webapi.response.UsersCertificateResponse;
import org.openhab.binding.worxlandroid.internal.webapi.response.UsersMeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMqttClient;

/**
 * The {@link WorxLandroidBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Nils - Initial contribution
 */
@NonNullByDefault
public class WorxLandroidBridgeHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(WorxLandroidBridgeHandler.class);

    private static final String EMPTY_PASSWORD = "";

    private WorxLandroidWebApiImpl apiHandler;
    private @Nullable MowerDiscoveryService discoveryService;

    private @Nullable String awsMqttEndpoint;
    private @Nullable AWSIotMqttClient awsMqttClient;

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
        // if (CHANNEL_1.equals(channelUID.getId())) {
        // if (command instanceof RefreshType) {
        // // TODO: handle data refresh
        // }
        //
        // // TODO: handle command
        //
        // // Note: if communication with thing fails for some reason,
        // // indicate that by setting the status with detail information:
        // // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
        // // "Could not control device at IP address x.x.x.x");
        //
        // }
    }

    @Override
    public void initialize() {

        try {

            BridgeConfiguration config = getConfigAs(BridgeConfiguration.class);
            boolean connected = apiHandler.connect(config.getWebapiUsername(), config.getWebapiPassword());

            if (connected) {

                UsersMeResponse usersMeResponse = apiHandler.retrieveWebInfo();
                awsMqttEndpoint = usersMeResponse.getMemberDataAsString(MN_MQTTENDPOINT);
                Map<String, String> props = usersMeResponse.getDataAsPropertyMap();

                updateThing(editThing().withProperties(props).build());

                UsersCertificateResponse usersCertificateResponse = apiHandler.retrieveAwsCertificate();

                // TODO test this
                if (!usersCertificateResponse.isActive()) {
                    logger.error("Connection blocked from Worx, please try again in 24h");
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                            "Connection blocked from Worx, please try again in 24h!");
                    return;
                }

                byte[] p12 = Base64.getDecoder().decode(usersCertificateResponse.getPkcs12().getBytes());
                KeyStore keystore = KeyStore.getInstance("PKCS12");
                keystore.load(new ByteArrayInputStream(p12), EMPTY_PASSWORD.toCharArray());

                awsMqttClient = new AWSIotMqttClient(awsMqttEndpoint, "android-" + MqttAsyncClient.generateClientId(),
                        keystore, EMPTY_PASSWORD);
                awsMqttClient.connect();
                logger.info("AWS connected");

                updateStatus(ThingStatus.ONLINE);

                // Trigger discovery of mowers
                scheduler.submit(runnable);

            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Error connecting to Worx Landroid WebApi!");
            }
        } catch (WebApiException | KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException
                | AWSIotException e) {
            logger.error("error: {}", e.getLocalizedMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Error: " + e.getLocalizedMessage());
        }

    }

    @Override
    public void dispose() {

        try {

            if (awsMqttClient != null) {
                awsMqttClient.disconnect();
            }
            super.dispose();

        } catch (AWSIotException e) {
            logger.error("{}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE);
        }
    }

    /**
     * @return
     */
    public WorxLandroidWebApiImpl getWorxLandroidWebApiImpl() {

        return apiHandler;
    }

    /**
     * @param discoveryService
     */
    public void setDiscovery(MowerDiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    /**
     * @param awsTopic
     * @throws AWSIotException
     */
    @SuppressWarnings("null")
    public void subcribeTopic(AWSTopic awsTopic) throws AWSIotException {

        if (awsMqttClient == null) {
            logger.error("MqttClient is not initialized. Cannot subsribe to topic -> {}", awsTopic.getTopic());
            return;
        }

        logger.debug("subsribe to topic -> {}", awsTopic.getTopic());
        awsMqttClient.subscribe(awsTopic);
    }

    /**
     * @param awsMessage
     * @throws AWSIotException
     */
    @SuppressWarnings("null")
    public void publishMessage(AWSMessage awsMessage) throws AWSIotException {

        if (awsMqttClient == null) {
            logger.error("MqttClient is not initialized. Cannot publish message to topic -> {}", awsMessage.getTopic());
            return;
        }

        logger.debug("publish topic -> {}", awsMessage.getTopic());
        logger.debug("publish message -> {}", awsMessage.getStringPayload());
        awsMqttClient.publish(awsMessage);

    }
}