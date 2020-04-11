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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.worxlandroid.internal.codes.WorxLandroidActionCodes;
import org.openhab.binding.worxlandroid.internal.codes.WorxLandroidDayCodes;
import org.openhab.binding.worxlandroid.internal.codes.WorxLandroidErrorCodes;
import org.openhab.binding.worxlandroid.internal.codes.WorxLandroidStatusCodes;
import org.openhab.binding.worxlandroid.internal.config.MowerConfiguration;
import org.openhab.binding.worxlandroid.internal.mqtt.AWSMessage;
import org.openhab.binding.worxlandroid.internal.mqtt.AWSMessageCallback;
import org.openhab.binding.worxlandroid.internal.mqtt.AWSTopic;
import org.openhab.binding.worxlandroid.internal.vo.Mower;
import org.openhab.binding.worxlandroid.internal.vo.ScheduledDay;
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
import com.google.gson.JsonPrimitive;

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

    private Mower mower = new Mower("NOT_INITIALIZED");
    private @Nullable WorxLandroidWebApiImpl apiHandler;

    @SuppressWarnings("unused")
    private @Nullable AWSTopic awsTopic;
    private String mqttCommandIn = "";

    @SuppressWarnings("unused")
    private @Nullable ScheduledFuture<?> refreshStatusJob;
    @SuppressWarnings("unused")
    private @Nullable ScheduledFuture<?> pollingJob;

    /**
     * Defines a runnable for a refresh status job.
     * Checks if mower is online.
     */
    private Runnable refreshStatusRunnable = new Runnable() {
        @Override
        public void run() {
            try {

                if (isBridgeOnline() && apiHandler != null) {

                    ProductItemsResponse productItemsResponse = apiHandler.retrieveUserDevices();
                    JsonObject mowerDataJson = productItemsResponse.getMowerDataById(mower.getSerialNumber());

                    boolean online = mowerDataJson != null && mowerDataJson.get("online").getAsBoolean();
                    mower.setOnline(online);
                    updateState(CHANNELNAME_ONLINE, OnOffType.from(online));
                    DateTimeType d = new DateTimeType();
                    updateState(CHANNELNAME_LAST_UPDATE_ONLINE_STATUS, new DateTimeType());
                    updateStatus(online ? ThingStatus.ONLINE : ThingStatus.OFFLINE);
                }
            } catch (IllegalStateException e) {
                logger.debug("\"RefreshStatusRunnable {}: Refreshing Thing failed, handler might be OFFLINE",
                        mower.getSerialNumber());
            } catch (Exception e) {
                logger.error("RefreshStatusRunnable {}: Unknown error", mower.getSerialNumber(), e);
            }
        }
    };

    /**
     * Defines a runnable for a polling job.
     * Polls AWS mqtt.
     */
    private Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {

            try {

                if (isBridgeOnline()) {

                    WorxLandroidBridgeHandler bridgeHandler = getWorxLandroidBridgeHandler();
                    if (bridgeHandler != null) {

                        AWSMessage message = new AWSMessage(mqttCommandIn, AWSIotQos.QOS0, AWSMessage.EMPTY_PAYLOAD);
                        bridgeHandler.publishMessage(message);
                    }
                }
            } catch (AWSIotException e) {
                logger.error("PollingRunnable {}: {}", e.getLocalizedMessage(), mower.getSerialNumber());
            }
        }
    };

    /**
     * @param thing
     */
    public WorxLandroidMowerHandler(Thing thing) {
        super(thing);
    }

    /**
     * @return
     */
    private boolean isBridgeOnline() {

        Bridge bridge = getBridge();
        return bridge != null && bridge.getStatus() == ThingStatus.ONLINE;
    }

    @Override
    public void initialize() {

        mower = new Mower(getThing().getUID().getId());

        logger.debug("Initializing WorxLandroidMowerHandler for serialNumber '{}'", mower.getSerialNumber());

        if (isBridgeOnline()) {

            WorxLandroidBridgeHandler bridgeHandler = getWorxLandroidBridgeHandler();

            if (bridgeHandler != null) {

                apiHandler = bridgeHandler.getWorxLandroidWebApiImpl();

                try {

                    ProductItemsResponse productItemsResponse = apiHandler.retrieveUserDevices();
                    JsonObject mowerDataJson = productItemsResponse.getMowerDataById(mower.getSerialNumber());

                    if (mowerDataJson != null) {

                        ThingBuilder thingBuilder = editThing();

                        // set mower properties
                        Map<String, String> props = productItemsResponse.getDataAsPropertyMap(mower.getSerialNumber());
                        thingBuilder.withProperties(props);

                        mqttCommandIn = props.get("command_in");
                        String mqttCommandOut = props.get("command_out");

                        // lock channel only when supported
                        boolean lockSupported = Boolean.parseBoolean(props.get("lock"));
                        mower.setLockSupported(lockSupported);
                        if (!lockSupported) {
                            thingBuilder.withoutChannel(new ChannelUID(thing.getUID(), CHANNELNAME_LOCK));
                        }

                        // rainDelay channel only when supported
                        boolean rainDelaySupported = Boolean.parseBoolean(props.get("rain_delay"));
                        mower.setRainDelaySupported(rainDelaySupported);
                        if (!rainDelaySupported) {
                            thingBuilder.withoutChannel(new ChannelUID(thing.getUID(), CHANNELNAME_RAIN_DELAY));
                        }

                        // multizone channels only when supported
                        boolean multiZoneSupported = Boolean.parseBoolean(props.get("multi_zone"));
                        mower.setMultiZoneSupported(multiZoneSupported);
                        if (!multiZoneSupported) {
                            // remove zome meter channels
                            for (int zoneIndex = 0; zoneIndex < 4; zoneIndex++) {
                                String channelNameZoneMeter = String.format("cfgMultiZones#zone%dMeter", zoneIndex + 1);
                                thingBuilder.withoutChannel(new ChannelUID(thing.getUID(), channelNameZoneMeter));
                            }
                            // remove allocation channels
                            for (int allocationIndex = 0; allocationIndex < 10; allocationIndex++) {
                                String channelNameAllocation = CHANNELNAME_PREFIX_ALLOCATION + allocationIndex;
                                thingBuilder.withoutChannel(new ChannelUID(thing.getUID(), channelNameAllocation));
                            }
                        }

                        updateThing(thingBuilder.build());

                        // handle AWS
                        AWSTopic awsTopic = new AWSTopic(mqttCommandOut, AWSIotQos.QOS0, this);
                        bridgeHandler.subcribeTopic(awsTopic);

                        AWSMessage message = new AWSMessage(mqttCommandIn, AWSIotQos.QOS0, AWSMessage.EMPTY_PAYLOAD);
                        bridgeHandler.publishMessage(message);

                        updateStatus(
                                mowerDataJson.get("online").getAsBoolean() ? ThingStatus.ONLINE : ThingStatus.OFFLINE);

                        // scheduled jobs
                        startScheduledJobs();

                    } else {
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.GONE);
                        return;
                    }

                } catch (WebApiException | AWSIotException e) {
                    logger.error("initialize mower: id {} - {}::{}", mower.getSerialNumber(), getThing().getLabel(),
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
     * Start scheduled jobs
     */
    private void startScheduledJobs() {

        MowerConfiguration config = getConfigAs(MowerConfiguration.class);

        refreshStatusJob = scheduler.scheduleWithFixedDelay(refreshStatusRunnable, 30,
                config.getRefreshStatusInterval(), TimeUnit.SECONDS);

        pollingJob = scheduler.scheduleWithFixedDelay(pollingRunnable, 60, config.getPollingInterval(),
                TimeUnit.SECONDS);
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
    public void dispose() {
        if (refreshStatusJob != null) {
            refreshStatusJob.cancel(true);
        }

        if (pollingJob != null) {
            pollingJob.cancel(true);
        }
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {

        if (ThingStatus.OFFLINE.equals(bridgeStatusInfo.getStatus())) {
            mower.setOnline(false);
        }
        super.bridgeStatusChanged(bridgeStatusInfo);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

        try {

            if (command instanceof RefreshType) {
                return;
            }

            if (getThing().getStatus() != ThingStatus.ONLINE) {
                logger.error("handleCommand mower: {} ({}) is offline!", getThing().getLabel(),
                        mower.getSerialNumber());
                return;
            }

            WorxLandroidBridgeHandler bridgeHandler = getWorxLandroidBridgeHandler();
            if (bridgeHandler == null) {
                logger.error("no bridgeHandler");
                return;
            }

            String cmd = AWSMessage.EMPTY_PAYLOAD;

            if (channelUID.getId().startsWith("cfgMultiZones#zone")) {
                JsonObject jsonObject = new JsonObject();
                JsonArray mz = new JsonArray();

                // extract zone of from channel
                Pattern pattern = Pattern.compile("cfgMultiZones#zone(\\d)Meter");
                Matcher matcher = pattern.matcher(channelUID.getId());
                int zone = 0;
                if (matcher.find()) {
                    zone = Integer.parseInt(matcher.group(1));
                }
                mower.setZoneMeter(zone - 1, Integer.parseInt(command.toString()));

                for (int zoneIndex = 0; zoneIndex < 4; zoneIndex++) {
                    mz.add(mower.getZoneMeter(zoneIndex));
                }

                jsonObject.add("mz", mz);

                cmd = jsonObject.toString();
                logger.debug("{}", jsonObject.toString());

            } else if (channelUID.getId().startsWith(CHANNELNAME_PREFIX_ALLOCATION)) {
                JsonObject jsonObject = new JsonObject();
                JsonArray mzv = new JsonArray();

                // extract allocation index of from channel
                Pattern pattern = Pattern.compile(CHANNELNAME_PREFIX_ALLOCATION + "(\\d)");
                Matcher matcher = pattern.matcher(channelUID.getId());
                int allocationIndex = 0;
                if (matcher.find()) {
                    allocationIndex = Integer.parseInt(matcher.group(1));
                }

                mower.setAllocation(allocationIndex, Integer.parseInt(command.toString()));

                for (int i = 0; i < 10; i++) {
                    mzv.add(mower.getAllocation(i));
                }

                jsonObject.add("mzv", mzv);

                cmd = jsonObject.toString();
                logger.debug("{}", jsonObject.toString());

            } else

            // update schedule
            // TODO ugly check
            if (channelUID.getId().startsWith("cfgSc")) {
                // update mower data

                // update schedule or timeExtension?
                if (channelUID.getId().equals(CHANNELNAME_SC_TIME_EXTENSION)) {
                    mower.setTimeExtension(Integer.parseInt(command.toString()));
                } else {
                    // extract name of from channel
                    Pattern pattern = Pattern.compile("cfgSc(.*?)#");
                    Matcher matcher = pattern.matcher(channelUID.getId());
                    String day = "";
                    if (matcher.find()) {
                        day = (matcher.group(1));
                    }

                    WorxLandroidDayCodes dayCodeUpdated = WorxLandroidDayCodes.valueOf(day.toUpperCase());
                    ScheduledDay scheduledDayUpdated = mower.getScheduledDay(dayCodeUpdated);

                    String chName = channelUID.getId().split("#")[1];
                    switch (chName) {
                        case "scheduleStartHour":
                            scheduledDayUpdated.setHours(Integer.parseInt(command.toString()));
                            break;

                        case "scheduleStartMinutes":
                            scheduledDayUpdated.setMinutes(Integer.parseInt(command.toString()));
                            break;

                        case "scheduleDuration":
                            scheduledDayUpdated.setDuration(Integer.parseInt(command.toString()));
                            break;

                        case "scheduleEdgecut":
                            scheduledDayUpdated.setEdgecut(OnOffType.ON.equals(command));
                            break;

                        default:
                            break;
                    }
                }

                // generate 'sc' message
                JsonObject jsonObject = new JsonObject();
                JsonObject sc = new JsonObject();

                // timeExtension
                sc.add("p", new JsonPrimitive(mower.getTimeExtension()));

                // schedule
                JsonArray jsonArray = new JsonArray();

                for (WorxLandroidDayCodes dayCode : WorxLandroidDayCodes.values()) {

                    JsonArray scDay = new JsonArray();
                    ScheduledDay scheduledDay = mower.getScheduledDay(dayCode);

                    String minutes = scheduledDay.getMinutes() < 10 ? scheduledDay.getMinutes() + "0"
                            : String.valueOf(scheduledDay.getMinutes());
                    scDay.add(String.format("%d:%s", scheduledDay.getHour(), minutes));
                    scDay.add(scheduledDay.getDuration());
                    scDay.add(scheduledDay.isEdgecut() ? 1 : 0);
                    jsonArray.add(scDay);
                }
                sc.add("d", jsonArray);
                jsonObject.add("sc", sc);

                cmd = jsonObject.toString();
                logger.debug("{}", jsonObject.toString());
            } else {

                switch (channelUID.getId()) {

                    // start action
                    case CHANNELNAME_ACTION:
                        WorxLandroidActionCodes actionCode = WorxLandroidActionCodes.valueOf(command.toString());
                        logger.debug("{}", actionCode.toString());
                        cmd = String.format("{\"cmd\":%s}", actionCode.getCode());
                        break;

                    // poll
                    case CHANNELNAME_POLL:
                        cmd = AWSMessage.EMPTY_PAYLOAD;
                        updateState(CHANNELNAME_POLL, OnOffType.OFF);
                        break;

                    // update rainDelay
                    case CHANNELNAME_RAIN_DELAY:
                        cmd = String.format("{\"rd\":%s}", command);
                        break;

                    // lock/unlock
                    case CHANNELNAME_LOCK:
                        WorxLandroidActionCodes lockCode = OnOffType.ON.equals(command) ? WorxLandroidActionCodes.LOCK
                                : WorxLandroidActionCodes.UNLOCK;
                        logger.debug("{}", lockCode.toString());
                        cmd = String.format("{\"cmd\":%s}", lockCode.getCode());
                        break;

                    default:
                        logger.debug("command for ChannelUID not supported: {}", channelUID.getAsString());
                        break;
                }
            }

            AWSMessage message = new AWSMessage(mqttCommandIn, AWSIotQos.QOS0, cmd);
            bridgeHandler.publishMessage(message);

        } catch (

        AWSIotException e) {
            logger.error("error: {}", e.getLocalizedMessage());
        }
    }

    @Override
    public void processMessage(@Nullable AWSIotMessage message) {

        if (message == null) {
            logger.debug("message is null!");
            return;
        }

        updateStatus(ThingStatus.ONLINE);

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
            // dat/bt/c -> batteryCharging - 1=charging
            if (bt.get("c") != null) {
                boolean state = bt.get("c").getAsInt() == 1 ? Boolean.TRUE : Boolean.FALSE;
                updateState(CHANNELNAME_BATTERY_CHARGING, OnOffType.from(state));
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

        // dat/lk -> lock
        if (mower.isLockSupported() && dat.get("lk") != null) {
            boolean lock = dat.get("lk").getAsInt() == 1 ? Boolean.TRUE : Boolean.FALSE;
            updateState(CHANNELNAME_LOCK, OnOffType.from(lock));
        }

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

        // cfg/dt + cfg/tm
        // "tm": "17:09:34","dt": "13/03/2020",
        String dateTime = String.format("%s %s", cfg.get("dt").getAsString(), cfg.get("tm").getAsString());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        LocalDateTime localeDateTime = LocalDateTime.parse(dateTime, formatter);

        ZoneId zoneId = ZoneId.of(getThing().getProperties().get("time_zone"));
        ZonedDateTime zonedDateTime = ZonedDateTime.of(localeDateTime, zoneId);
        updateState(CHANNELNAME_DATETIME, new DateTimeType(zonedDateTime));

        // TODO cfg/sc
        if (cfg.get("sc") != null) {
            JsonObject sc = cfg.getAsJsonObject("sc");
            // TODO cfg/sc/m
            // cfg/sc/p
            if (sc.get("p") != null) {
                int timeExtension = sc.get("p").getAsInt();
                mower.setTimeExtension(timeExtension);
                updateState(CHANNELNAME_SC_TIME_EXTENSION, new DecimalType(timeExtension));
            }
            if (sc.get("d") != null) {
                JsonArray d = sc.get("d").getAsJsonArray();

                for (WorxLandroidDayCodes dayCode : WorxLandroidDayCodes.values()) {

                    JsonArray shedule = d.get(dayCode.getCode()).getAsJsonArray();
                    ScheduledDay scheduledDay = mower.getScheduledDay(dayCode);

                    String time[] = shedule.get(0).getAsString().split(":");

                    // hour
                    String channelNameStartHour = String.format("cfgSc%s#scheduleStartHour", dayCode.getDescription());
                    scheduledDay.setHours(Integer.parseInt(time[0]));
                    updateState(channelNameStartHour, new DecimalType(time[0]));

                    // minutes
                    String channelNameStartMin = String.format("cfgSc%s#scheduleStartMinutes",
                            dayCode.getDescription());
                    scheduledDay.setMinutes(Integer.parseInt(time[1]));
                    updateState(channelNameStartMin, new DecimalType(time[1]));

                    // duration
                    String channelNameDuration = String.format("cfgSc%s#scheduleDuration", dayCode.getDescription());
                    scheduledDay.setDuration(shedule.get(1).getAsInt());
                    updateState(channelNameDuration, new DecimalType(shedule.get(1).getAsLong()));

                    // edgecut
                    String channelNameEdgecut = String.format("cfgSc%s#scheduleEdgecut", dayCode.getDescription());
                    boolean edgecut = shedule.get(2).getAsInt() == 1 ? Boolean.TRUE : Boolean.FALSE;
                    scheduledDay.setEdgecut(edgecut);
                    updateState(channelNameEdgecut, OnOffType.from(edgecut));
                }

            }
        }

        // cfg/cmd -> command
        if (cfg.get("cmd") != null) {
            updateState(CHANNELNAME_COMMAND, new DecimalType(cfg.get("cmd").getAsLong()));
        }

        if (mower.isMultiZoneSupported()) {

            // zone meters
            if (cfg.get("mz") != null) {
                JsonArray multizones = cfg.get("mz").getAsJsonArray();
                for (int zoneIndex = 0; zoneIndex < 4; zoneIndex++) {
                    int meters = multizones.get(zoneIndex).getAsInt();
                    mower.setZoneMeter(zoneIndex, meters);
                    String channelNameZoneMeter = String.format("cfgMultiZones#zone%dMeter", zoneIndex + 1);
                    updateState(channelNameZoneMeter, new DecimalType(meters));
                }
            }

            // allocation zones
            if (cfg.get("mzv") != null) {
                JsonArray multizoneAllocations = cfg.get("mzv").getAsJsonArray();
                for (int allocationIndex = 0; allocationIndex < 10; allocationIndex++) {
                    int zone = multizoneAllocations.get(allocationIndex).getAsInt();
                    mower.setAllocation(allocationIndex, zone);
                    String channelNameAlloction = CHANNELNAME_PREFIX_ALLOCATION + allocationIndex;
                    updateState(channelNameAlloction,
                            new DecimalType(multizoneAllocations.get(allocationIndex).getAsLong()));
                }
            }
        }

        // cfg/rd -> rainDelay
        if (mower.isRainDelaySupported() && cfg.get("rd") != null) {
            updateState(CHANNELNAME_RAIN_DELAY, new DecimalType(cfg.get("rd").getAsLong()));
        }

        // cfg/sn -> serialNumber
        if (cfg.get("sn") != null) {
            updateState(CHANNELNAME_SERIAL_NUMBER, new StringType(cfg.get("sn").getAsString()));
        }

        // TODO cfg/modules
    }

}
