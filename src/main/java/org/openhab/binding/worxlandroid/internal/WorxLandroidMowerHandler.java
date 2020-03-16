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

import static org.openhab.binding.worxlandroid.internal.WorxLandroidBindingConstants.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.worxlandroid.internal.codes.WorxLandroidActionCodes;
import org.openhab.binding.worxlandroid.internal.codes.WorxLandroidErrorCodes;
import org.openhab.binding.worxlandroid.internal.codes.WorxLandroidStatusCodes;
import org.openhab.binding.worxlandroid.internal.mqtt.AWSMessage;
import org.openhab.binding.worxlandroid.internal.mqtt.AWSMessageCallback;
import org.openhab.binding.worxlandroid.internal.mqtt.AWSTopic;
import org.openhab.binding.worxlandroid.internal.webapi.WebApiException;
import org.openhab.binding.worxlandroid.internal.webapi.WorxLandroidWebApiImpl;
import org.openhab.binding.worxlandroid.internal.webapi.response.ProductItemsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The{@link WorxLandroidMowerHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Nils - Initial contribution
 *
 */
@NonNullByDefault
public class WorxLandroidMowerHandler extends BaseThingHandler implements AWSMessageCallback {

    private final Logger logger = LoggerFactory.getLogger(WorxLandroidMowerHandler.class);

    private @Nullable String mowerId;
    private @Nullable WorxLandroidWebApiImpl apiHandler;

    private @Nullable AWSTopic awsTopic;
    private String mqttCommandIn = "";

    private @Nullable ScheduledFuture<?> future;

    /**
     * Defines a runnable for a refresh job
     */
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            try {
                WorxLandroidBridgeHandler bridgeHandler = getWorxLandroidBridgeHandler();

                if (bridgeHandler != null) {

                    if (getBridge().getStatus() == ThingStatus.ONLINE) {

                        ProductItemsResponse productItemsResponse = apiHandler.retrieveUserDevices();
                        JsonObject mowerDataJson = productItemsResponse.getMowerDataById(mowerId);

                        if (mowerDataJson != null && mowerDataJson.get("online").getAsBoolean()) {
                            updateStatus(ThingStatus.ONLINE);
                        } else {
                            updateStatus(ThingStatus.OFFLINE);
                        }
                    }
                }

            } catch (IllegalStateException e) {
                logger.debug("Thread {}: Refreshing Thing failed, handler might be OFFLINE", mowerId);
            } catch (Exception e) {
                logger.error("Thread {}: Unknown error", mowerId, e);
            }
        }
    };

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

                        AWSTopic awsTopic = new AWSTopic(mqttCommandOut, AWSIotQos.QOS0, this);
                        bridgeHandler.subcribeTopic(awsTopic);

                        String payload = "{}";
                        AWSMessage message = new AWSMessage(mqttCommandIn, AWSIotQos.QOS0, payload);
                        bridgeHandler.publishMessage(message);

                        if (mowerDataJson.get("online").getAsBoolean()) {
                            updateStatus(ThingStatus.ONLINE);
                        } else {
                            updateStatus(ThingStatus.OFFLINE);
                        }
                        future = scheduler.scheduleWithFixedDelay(runnable, 0, 60, TimeUnit.SECONDS);

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

            WorxLandroidActionCodes actionCode = WorxLandroidActionCodes.valueOf(command.toString());
            logger.debug("{}", actionCode.toString());

            AWSMessage message;
            switch (channelUID.getId()) {

                case CHANNEL_ACTION:
                    String cmd = String.format("{\"cmd\":%s}", actionCode.getCode());
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

    @Override
    public void processMessage(@Nullable AWSIotMessage message) {

        JsonElement jsonElement = new JsonParser().parse(message.getStringPayload());

        if (jsonElement.isJsonObject()) {

            // cfg
            if (jsonElement.getAsJsonObject().get("cfg") != null) {
                updateStateCfg(jsonElement.getAsJsonObject().get("cfg").getAsJsonObject());
            }

            // dat
            if (jsonElement.getAsJsonObject().get("dat") != null) {
                updateStateDat(jsonElement.getAsJsonObject().get("dat").getAsJsonObject());
            }

        }
    }

    /**
     * Update states for data values
     *
     * @param dat
     */
    private void updateStateDat(JsonObject dat) {

        // dat/mac -> macAddress
        if (dat.get("mac") != null) {
            updateState(CHANNELNAME_MAC_ADRESS, new StringType(dat.get("mac").getAsString()));
        }

        // dat/fw -> firmware
        if (dat.get("fw") != null) {
            updateState(CHANNELNAME_FIRMWARE, new DecimalType(dat.get("fw").getAsBigDecimal()));
        }

        // dat/bt
        if (dat.get("bt") != null) {
            JsonObject bt = dat.getAsJsonObject("bt");
            // dat/bt/t -> batteryTemperature
            if (bt.get("t") != null) {
                updateState(CHANNELNAME_BATTERY_TEMPERATURE, new DecimalType(bt.get("t").getAsBigDecimal()));
            }
            // dat/bt/v -> batteryVoltage
            if (bt.get("v") != null) {
                updateState(CHANNELNAME_BATTERY_VOLTAGE, new DecimalType(bt.get("v").getAsBigDecimal()));
            }
            // dat/bt/p -> batteryLevel
            if (bt.get("p") != null) {
                updateState(CHANNELNAME_BATTERY_LEVEL, new DecimalType(bt.get("p").getAsLong()));
            }
            // dat/bt/nr -> batteryChargeCycle
            if (bt.get("nr") != null) {
                updateState(CHANNELNAME_BATTERY_CHARGE_CYCLE, new DecimalType(bt.get("nr").getAsBigDecimal()));
            }
            // dat/bt/c -> batteryCharging
            if (bt.get("c") != null) {
                // TODO boolean OnOffType?
                updateState(CHANNELNAME_BATTERY_CHARGING, new StringType(bt.get("c").getAsString()));
            }
            // TODO dat/bt/m -> ?
        }

        // dat/dmp
        if (dat.get("dmp") != null) {
            JsonArray dmp = dat.getAsJsonArray("dmp");
            // dat/dmp.[0] -> pitch
            if (dmp.get(0) != null) {
                updateState(CHANNELNAME_PITCH, new DecimalType(dmp.get(0).getAsBigDecimal()));
            }
            // dat/dmp.[1] -> roll
            if (dmp.get(1) != null) {
                updateState(CHANNELNAME_ROLL, new DecimalType(dmp.get(1).getAsBigDecimal()));
            }
            // dat/dmp.[2] -> yaw
            if (dmp.get(2) != null) {
                updateState(CHANNELNAME_YAW, new DecimalType(dmp.get(2).getAsBigDecimal()));
            }
        }

        // dat/st
        if (dat.get("st") != null) {
            JsonObject st = dat.getAsJsonObject("st");
            // dat/st/b -> totalBladeTime
            if (st.get("b") != null) {
                updateState(CHANNELNAME_TOTAL_BLADE_TIME, new DecimalType(st.get("b").getAsLong()));
            }
            // dat/st/d -> totalDistance
            if (st.get("d") != null) {
                updateState(CHANNELNAME_TOTAL_DISTANCE, new DecimalType(st.get("d").getAsLong()));
            }
            if (st.get("wt") != null) {
                // dat/st/wt -> totalTime
                updateState(CHANNELNAME_TOTAL_TIME, new DecimalType(st.get("wt").getAsLong()));
            }
            // TODO dat/st/bl -> ?
        }

        if (dat.get("ls") != null) {
            // dat/ls -> statusCode
            long statusCode = dat.get("ls").getAsLong();
            updateState(CHANNELNAME_STATUS_CODE, new DecimalType(statusCode));

            WorxLandroidStatusCodes code = WorxLandroidStatusCodes.getByCode((int) statusCode) == null
                    ? WorxLandroidStatusCodes.UNKNOWN
                    : WorxLandroidStatusCodes.getByCode((int) statusCode);
            updateState(CHANNELNAME_STATUS_DESCRIPTION, new StringType(code.getDescription()));
            logger.info("{}", code.toString());
        }
        // dat/le -> errorCode
        if (dat.get("le") != null) {
            long errorCode = dat.get("le").getAsLong();
            updateState(CHANNELNAME_ERROR_CODE, new DecimalType(errorCode));

            WorxLandroidErrorCodes code = WorxLandroidErrorCodes.getByCode((int) errorCode) == null
                    ? WorxLandroidErrorCodes.UNKNOWN
                    : WorxLandroidErrorCodes.getByCode((int) errorCode);
            updateState(CHANNELNAME_ERROR_DESCRIPTION, new StringType(code.getDescription()));
            logger.info("{}", code.toString());
        }
        // TODO dat/lz -> ?
        // dat/rsi -> wifiQuality
        if (dat.get("rsi") != null) {
            updateState(CHANNELNAME_WIFI_QUALITY, new DecimalType(dat.get("rsi").getAsLong()));
        }

        // TODO dat/lk -> ?
        // TODO dat/act -> ?
        // TODO dat/conn -> ?
        // TODO dat/modules/US/stat -> ?
    }

    /**
     * Update states for cfg values
     *
     * @param cfg
     */
    private void updateStateCfg(JsonObject cfg) {

        // cfg/id -> id
        if (cfg.get("id") != null) {
            updateState(CHANNELNAME_ID, new DecimalType(cfg.get("id").getAsLong()));
        }

        // cfg/lg -> language
        if (cfg.get("lg") != null) {
            updateState(CHANNELNAME_LANGUAGE, new StringType(cfg.get("lg").getAsString()));
        }

        // TODO cfg/dt + cfg/tm
        // TODO updateState(CHANNELNAME_LANGUAGE, new DateTimeType(cfg.get("lg").getAsString()));

        // TODO cfg/sc

        // cfg/cmd -> command
        if (cfg.get("cmd") != null) {
            updateState(CHANNELNAME_COMMAND, new DecimalType(cfg.get("cmd").getAsLong()));
        }

        // TODO cfg/mz
        // TODO cfg/mzv

        // cfg/rd -> rainDelay
        if (cfg.get("rd") != null) {
            updateState(CHANNELNAME_RAIN_DELAY, new DecimalType(cfg.get("rd").getAsLong()));
        }

        // cfg/sn -> serialNumber
        if (cfg.get("sn") != null) {
            updateState(CHANNELNAME_SERIAL_NUMBER, new StringType(cfg.get("sn").getAsString()));
        }

        // TODO cfg/modules
    }

}
