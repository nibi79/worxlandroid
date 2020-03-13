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

import static org.openhab.binding.worxlandroid.internal.WorxLandroidBindingConstants.CHANNEL_ACTION;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.worxlandroid.internal.mqtt.AWSMessage;
import org.openhab.binding.worxlandroid.internal.mqtt.AWSTopic;
import org.openhab.binding.worxlandroid.internal.webapi.WebApiException;
import org.openhab.binding.worxlandroid.internal.webapi.WorxLandroidWebApiImpl;
import org.openhab.binding.worxlandroid.internal.webapi.response.ProductItemsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.google.gson.JsonObject;

/***
 * The{@link WorxLandroidMowerHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Nils - Initial contribution
 *
 */
@NonNullByDefault
public class WorxLandroidMowerHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(WorxLandroidMowerHandler.class);

    private @Nullable String mowerId;
    private @Nullable WorxLandroidWebApiImpl apiHandler;

    private @Nullable AWSTopic awsTopic;
    private String mqttCommandIn = "";

    public WorxLandroidMowerHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {

        WorxLandroidBridgeHandler bridgeHandler = getWorxLandroidBridgeHandler();

        if (bridgeHandler != null) {
            mowerId = getThing().getUID().getId();

            logger.debug("Initializing WorxLandroidMowerHandler for mowerId '{}'", mowerId);

            if (getBridge().getStatus() == ThingStatus.ONLINE) {
                apiHandler = ((WorxLandroidBridgeHandler) getBridge().getHandler()).getWorxLandroidWebApiImpl();

                try {

                    ProductItemsResponse productItemsResponse = apiHandler.retrieveUserDevices();

                    JsonObject mowerDataJson = productItemsResponse.getMowerDataById(mowerId);

                    if (mowerDataJson != null) {
                        Map<String, String> props = new LinkedHashMap<>();

                        props.put("serial_number", mowerDataJson.get("serial_number").getAsString());

                        mqttCommandIn = mowerDataJson.get("mqtt_topics").getAsJsonObject().get("command_in")
                                .getAsString();
                        props.put("command_in", mqttCommandIn);

                        String mqttCommandOut = mowerDataJson.get("mqtt_topics").getAsJsonObject().get("command_out")
                                .getAsString();
                        props.put("command_out", mqttCommandOut);

                        updateThing(editThing().withProperties(props).build());

                        AWSTopic awsTopic = new AWSTopic(mqttCommandOut, AWSIotQos.QOS0);
                        bridgeHandler.subcribeTopic(awsTopic);

                        String payload = "{}";
                        AWSMessage message = new AWSMessage(mqttCommandIn, AWSIotQos.QOS0, payload);
                        bridgeHandler.publishMessage(message);

                        updateStatus(ThingStatus.ONLINE);

                    } else {
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.GONE);
                        return;
                    }

                } catch (WebApiException | AWSIotException e) {
                    logger.error("initialize mower: id {} - {}::{}", mowerId, getThing().getLabel(),
                            getThing().getUID());
                }

                updateStatus(ThingStatus.ONLINE);

            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.BRIDGE_OFFLINE);
            }
        } else {
            updateStatus(ThingStatus.OFFLINE);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Initialize thing: {}::{}", getThing().getLabel(), getThing().getUID());
        }
    }

    /**
     * @return
     */
    protected synchronized @Nullable WorxLandroidBridgeHandler getWorxLandroidBridgeHandler() {

        Bridge bridge = getBridge();
        if (bridge == null) {
            return null;
        }

        ThingHandler handler = bridge.getHandler();
        if (handler instanceof WorxLandroidBridgeHandler) {
            return (WorxLandroidBridgeHandler) handler;
        } else {
            return null;
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

        try {

            if (command instanceof RefreshType) {
                return;
            }

            WorxLandroidBridgeHandler bridgeHandler = getWorxLandroidBridgeHandler();
            if (bridgeHandler == null) {
                logger.error("no bridgeHandler");
                return;
            }

            AWSMessage message;
            switch (channelUID.getId()) {

                case CHANNEL_ACTION:
                    String cmd = String.format("{\"cmd\":%s}", command.toString());
                    message = new AWSMessage(mqttCommandIn, AWSIotQos.QOS0, cmd);
                    bridgeHandler.publishMessage(message);
                    break;

                default:
                    logger.debug("command for ChannelUID not supported: {}", channelUID.getAsString());
                    break;
            }
        } catch (AWSIotException e) {
            logger.error("error: {}", e.getLocalizedMessage());
        }
    }
}
