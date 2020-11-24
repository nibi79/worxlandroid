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
import org.openhab.binding.worxlandroid.internal.webapi.response.ProductItemsStatusResponse;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
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

    private boolean restoreZoneMeter = false;
    private int[] zoneMeterRestoreValues = {};

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

                        float firmwareVersion = Float.parseFloat(props.get("firmware_version"));

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

                        // rainDelayStart channel only when supported
                        boolean rainDelayStart = false;
                        if (props.get("rain_delay_start") != null) {
                            float rainDelayStartVersion = -1;
                            try {
                                rainDelayStartVersion = Float.parseFloat(props.get("rain_delay_start"));
                                rainDelayStart = firmwareVersion >= rainDelayStartVersion;
                            } catch (NumberFormatException e) {
                                logger.debug("Cannot format 'rain_delay_start': {}", props.get("rain_delay_start"));
                            }
                        }
                        mower.setRainDelayStartSupported(rainDelayStart);
                        if (!rainDelayStart) {
                            thingBuilder.withoutChannel(new ChannelUID(thing.getUID(), CHANNELNAME_RAIN_STATE));
                            thingBuilder.withoutChannel(new ChannelUID(thing.getUID(), CHANNELNAME_RAIN_COUNTER));
                        }

                        // multizone channels only when supported
                        boolean multiZoneSupported = Boolean.parseBoolean(props.get("multi_zone"));
                        mower.setMultiZoneSupported(multiZoneSupported);
                        if (!multiZoneSupported) {
                            // remove lastZome channel
                            thingBuilder.withoutChannel(new ChannelUID(thing.getUID(), CHANNELNAME_LAST_ZONE));
                            // remove zone meter channels
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

                        // oneTimeScheduler channel only when supported
                        boolean oneTimeScheduler = false;
                        if (props.get("one_time_scheduler") != null) {
                            float oneTimeSchedulerVersion = -1;
                            try {
                                oneTimeSchedulerVersion = Float.parseFloat(props.get("one_time_scheduler"));
                                oneTimeScheduler = firmwareVersion >= oneTimeSchedulerVersion;
                            } catch (NumberFormatException e) {
                                logger.debug("Cannot format 'one_time_scheduler': {}", props.get("one_time_scheduler"));
                            }
                        }
                        mower.setOneTimeSchedulerSupported(oneTimeScheduler);
                        if (!oneTimeScheduler) {
                            thingBuilder
                                    .withoutChannel(new ChannelUID(thing.getUID(), CHANNELNAME_ONE_TIME_SC_DURATION));
                            thingBuilder
                                    .withoutChannel(new ChannelUID(thing.getUID(), CHANNELNAME_ONE_TIME_SC_EDGECUT));
                            thingBuilder.withoutChannel(new ChannelUID(thing.getUID(), CHANNELNAME_SC_MODE));
                        }

                        // Scheduler 2 channels only when supported version
                        boolean scheduler2Supported = false;
                        if (props.get("scheduler_two_slots") != null) {
                            float schedulerTwoSlotsVersion = -1;
                            try {
                                schedulerTwoSlotsVersion = Float.parseFloat(props.get("scheduler_two_slots"));
                                scheduler2Supported = firmwareVersion >= schedulerTwoSlotsVersion;
                            } catch (NumberFormatException e) {
                                logger.debug("Cannot format 'scheduler_two_slots': {}",
                                        props.get("scheduler_two_slots"));
                            }
                        }
                        mower.setScheduler2Supported(scheduler2Supported);
                        if (!scheduler2Supported) {
                            // remove schedule2 channels
                            for (WorxLandroidDayCodes dayCode : WorxLandroidDayCodes.values()) {

                                String channelNameSc2 = String.format("%s%s2", CHANNELNAME_SC_PREFIX,
                                        dayCode.getDescription());

                                String channelNameEnable = String.format("%s#%s", channelNameSc2,
                                        CHANNELNAME_SC_ENABLE_SUFFIX);
                                thingBuilder.withoutChannel(new ChannelUID(thing.getUID(), channelNameEnable));

                                String channelNameStartHour = String.format("%s#%s", channelNameSc2,
                                        CHANNELNAME_SC_START_HOUR_SUFFIX);
                                thingBuilder.withoutChannel(new ChannelUID(thing.getUID(), channelNameStartHour));

                                String channelNameStartMinutes = String.format("%s#%s", channelNameSc2,
                                        CHANNELNAME_SC_START_MINUTES_SUFFIX);
                                thingBuilder.withoutChannel(new ChannelUID(thing.getUID(), channelNameStartMinutes));

                                String channelNameDuration = String.format("%s#%s", channelNameSc2,
                                        CHANNELNAME_SC_DURATION_SUFFIX);
                                thingBuilder.withoutChannel(new ChannelUID(thing.getUID(), channelNameDuration));

                                String channelNameEdgString = String.format("%s#%s", channelNameSc2,
                                        CHANNELNAME_SC_EDGECUT_SUFFIX);
                                thingBuilder.withoutChannel(new ChannelUID(thing.getUID(), channelNameEdgString));
                            }
                        }

                        updateThing(thingBuilder.build());

                        ProductItemsStatusResponse productItemsStatusResponse = apiHandler
                                .retrieveDeviceStatus(mower.getSerialNumber());
                        processStatusMessage(productItemsStatusResponse.getJsonResponseAsJsonObject());

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

            // return;
            // channel: multizone enable or multizone meters (mz)
            if (CHANNELNAME_MULTIZONE_ENABLE.equals(channelUID.getId())) {

                mower.setMultiZoneEnable(OnOffType.ON.equals(command));
                sendZoneMeter();
                return;

            }

            if (CHANNELNAME_LAST_ZONE.equals(channelUID.getId())) {

                if (mower.getStatus() != WorxLandroidStatusCodes.HOME.getCode()) {
                    logger.warn("Cannot start zone because mower must be at HOME!");
                    return;
                }
                zoneMeterRestoreValues = mower.getZoneMeters();
                restoreZoneMeter = true;

                int meter = mower.getZoneMeter(Integer.parseInt(command.toString()));
                for (int zoneIndex = 0; zoneIndex < 4; zoneIndex++) {
                    mower.setZoneMeter(zoneIndex, meter);
                }
                sendZoneMeter();

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                }
                // start
                sendCommand(AWSMessage.CMD_START);

                return;
            }
            if (channelUID.getId().startsWith("cfgMultiZones#zone")) {
                // extract zone of from channel
                Pattern pattern = Pattern.compile("cfgMultiZones#zone(\\d)Meter");
                Matcher matcher = pattern.matcher(channelUID.getId());
                int zone = 0;
                if (matcher.find()) {
                    zone = Integer.parseInt(matcher.group(1));
                }
                mower.setZoneMeter(zone - 1, Integer.parseInt(command.toString()));
                sendZoneMeter();
                return;

            }
            // channel: multizone allocation (mzv)
            if (channelUID.getId().startsWith(CHANNELNAME_PREFIX_ALLOCATION)) {

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
                sendCommand(jsonObject.toString());
                return;
            }

            if (CHANNELNAME_SC_MODE.equals(channelUID.getId())) {
                // generate 'sc' message
                JsonObject jsonObject = new JsonObject();
                JsonObject sc = new JsonObject();

                // mode
                sc.add("m", new JsonPrimitive(Integer.parseInt(command.toString())));

                jsonObject.add("sc", sc);

                sendCommand(jsonObject.toString());
                return;
            }

            if (CHANNELNAME_ONE_TIME_SC_DURATION.equals(channelUID.getId())) {

                sendOneTimeSchedule(0, Integer.parseInt(command.toString()));
                return;
            }

            if (CHANNELNAME_ONE_TIME_SC_EDGECUT.equals(channelUID.getId())) {

                sendOneTimeSchedule(OnOffType.ON.equals(command) ? 1 : 0, 0);
                return;
            }

            // update schedule
            // TODO ugly check
            if (CHANNELNAME_ENABLE.equals(channelUID.getId()) || channelUID.getId().startsWith(CHANNELNAME_SC_PREFIX)) {
                // update mower data

                // update enable mowing or schedule or timeExtension/enable?
                if (CHANNELNAME_ENABLE.equals(channelUID.getId())) {

                    mower.setEnable(OnOffType.ON.equals(command));
                } else if (CHANNELNAME_SC_TIME_EXTENSION.equals(channelUID.getId())) {

                    mower.setTimeExtension(Integer.parseInt(command.toString()));

                } else {

                    setScheduledDays(channelUID, command);
                }

                sendSchedule();
                return;
            }

            String cmd = AWSMessage.EMPTY_PAYLOAD;

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

            sendCommand(cmd);

        } catch (

        AWSIotException e) {
            logger.error("error: {}", e.getLocalizedMessage());
        }
    }

    /**
     * @param bc
     * @param wtm
     * @throws AWSIotException
     */
    private void sendOneTimeSchedule(int bc, int wtm) throws AWSIotException {

        // generate 'sc' message
        JsonObject jsonObject = new JsonObject();
        JsonObject sc = new JsonObject();
        JsonObject ots = new JsonObject();

        // edgecut
        ots.add("bc", new JsonPrimitive(bc));
        // work time minutes
        ots.add("wtm", new JsonPrimitive(wtm));

        sc.add("ots", ots);
        jsonObject.add("sc", sc);

        sendCommand(jsonObject.toString());
    }

    /**
     * Set scheduled days
     *
     * @param scDaysIndex 1 or 2
     * @param channelUID
     * @param command
     */
    private void setScheduledDays(ChannelUID channelUID, Command command) {

        // extract name of from channel
        Pattern pattern = Pattern.compile("cfgSc(.*?)#");
        Matcher matcher = pattern.matcher(channelUID.getId());

        int scDaysSlot = 1;
        String day = "";
        if (matcher.find()) {
            day = (matcher.group(1));
            scDaysSlot = day.endsWith("day") ? 1 : 2;
            day = scDaysSlot == 1 ? day : day.substring(0, day.length() - 1);
        }

        WorxLandroidDayCodes dayCodeUpdated = WorxLandroidDayCodes.valueOf(day.toUpperCase());
        ScheduledDay scheduledDayUpdated = scDaysSlot == 1 ? mower.getScheduledDay(dayCodeUpdated)
                : mower.getScheduledDay2(dayCodeUpdated);

        String chName = channelUID.getId().split("#")[1];
        switch (chName) {
            case CHANNELNAME_SC_ENABLE_SUFFIX:
                scheduledDayUpdated.setEnable(OnOffType.ON.equals(command));
                break;

            case CHANNELNAME_SC_START_HOUR_SUFFIX:
                scheduledDayUpdated.setHours(Integer.parseInt(command.toString()));
                break;

            case CHANNELNAME_SC_START_MINUTES_SUFFIX:
                scheduledDayUpdated.setMinutes(Integer.parseInt(command.toString()));
                break;

            case CHANNELNAME_SC_DURATION_SUFFIX:
                scheduledDayUpdated.setDuration(Integer.parseInt(command.toString()));
                break;

            case CHANNELNAME_SC_EDGECUT_SUFFIX:
                scheduledDayUpdated.setEdgecut(OnOffType.ON.equals(command));
                break;

            default:
                break;
        }
    }

    /**
     * Send 'sc' message with 'p', 'd', 'dd'.
     *
     * @throws AWSIotException
     */
    private void sendSchedule() throws AWSIotException {
        // generate 'sc' message
        JsonObject jsonObject = new JsonObject();
        JsonObject sc = new JsonObject();

        // timeExtension
        sc.add("p", new JsonPrimitive(mower.getTimeExtension()));

        // schedule
        JsonArray jsonArrayD = generateScheduleDaysJson(1);
        sc.add("d", jsonArrayD);

        // schedule 2
        if (mower.isScheduler2Supported()) {
            JsonArray jsonArrayDd = generateScheduleDaysJson(2);
            sc.add("dd", jsonArrayDd);
        }

        jsonObject.add("sc", sc);

        sendCommand(jsonObject.toString());
    }

    /**
     * Generates JSON object for scheduled days.
     *
     * @param scDSlot scheduled day slot
     * @return
     */
    private JsonArray generateScheduleDaysJson(int scDSlot) {
        JsonArray jsonArray = new JsonArray();

        for (WorxLandroidDayCodes dayCode : WorxLandroidDayCodes.values()) {

            JsonArray scDay = new JsonArray();
            ScheduledDay scheduledDay = scDSlot == 1 ? mower.getScheduledDay(dayCode) : mower.getScheduledDay2(dayCode);

            String minutes = scheduledDay.getMinutes() < 10 ? "0" + scheduledDay.getMinutes()
                    : String.valueOf(scheduledDay.getMinutes());
            scDay.add(String.format("%d:%s", scheduledDay.getHour(), minutes));
            scDay.add(scheduledDay.getDuration());
            scDay.add(scheduledDay.isEdgecut() ? 1 : 0);
            jsonArray.add(scDay);
        }
        return jsonArray;
    }

    /**
     * Send 'mz' message.
     *
     * @throws AWSIotException
     */
    private void sendZoneMeter() throws AWSIotException {

        JsonObject jsonObject = new JsonObject();
        JsonArray mz = new JsonArray();

        for (int zoneIndex = 0; zoneIndex < 4; zoneIndex++) {
            mz.add(mower.getZoneMeter(zoneIndex));
        }

        jsonObject.add("mz", mz);
        sendCommand(jsonObject.toString());
    }

    /**
     * Send given command.
     *
     * @param cmd
     * @throws AWSIotException
     */
    private void sendCommand(String cmd) throws AWSIotException {

        logger.debug("send command: {}", cmd);

        WorxLandroidBridgeHandler bridgeHandler = getWorxLandroidBridgeHandler();
        AWSMessage message = new AWSMessage(mqttCommandIn, AWSIotQos.QOS0, cmd);
        bridgeHandler.publishMessage(message);
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
            processStatusMessage(jsonElement.getAsJsonObject());
        }
    }

    /**
     * @param jsonMessage
     */
    public void processStatusMessage(JsonObject jsonMessage) {
        // cfg
        if (jsonMessage.get("cfg") != null) {
            updateStateCfg(jsonMessage.get("cfg").getAsJsonObject());
        }

        // dat
        if (jsonMessage.get("dat") != null) {
            updateStateDat(jsonMessage.get("dat").getAsJsonObject());
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
                long batteryChargeCycle = bt.get("nr").getAsLong();
                updateState(CHANNELNAME_BATTERY_CHARGE_CYCLE, new DecimalType(batteryChargeCycle));

                long batteryChargeCyclesCurrent = batteryChargeCycle;
                String batteryChargeCyclesReset = getThing().getProperties().get("battery_charge_cycles_reset");
                if (batteryChargeCyclesReset != null && !batteryChargeCyclesReset.isEmpty()) {
                    batteryChargeCyclesCurrent = batteryChargeCycle - Long.valueOf(batteryChargeCyclesReset);
                }
                updateState(CHANNELNAME_BATTERY_CHARGE_CYCLE_CURRENT, new DecimalType(batteryChargeCyclesCurrent));
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
                long totalBladeTime = st.get("b").getAsLong();
                updateState(CHANNELNAME_TOTAL_BLADE_TIME, new DecimalType(totalBladeTime));

                long bladeTimeCurrent = totalBladeTime;
                String bladeWorkTimeReset = getThing().getProperties().get("blade_work_time_reset");
                if (bladeWorkTimeReset != null && !bladeWorkTimeReset.isEmpty()) {
                    bladeTimeCurrent = totalBladeTime - Long.valueOf(bladeWorkTimeReset);
                }
                updateState(CHANNELNAME_CURRENT_BLADE_TIME, new DecimalType(bladeTimeCurrent));
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
            mower.setStatus(statusCode);
            updateState(CHANNELNAME_STATUS_CODE, new DecimalType(statusCode));

            WorxLandroidStatusCodes code = WorxLandroidStatusCodes.getByCode((int) statusCode) == null
                    ? WorxLandroidStatusCodes.UNKNOWN
                    : WorxLandroidStatusCodes.getByCode((int) statusCode);
            updateState(CHANNELNAME_STATUS_DESCRIPTION, new StringType(code.getDescription()));
            logger.debug("{}", code.toString());

            // restore
            if (restoreZoneMeter) {
                if (statusCode != WorxLandroidStatusCodes.HOME.getCode()
                        && statusCode != WorxLandroidStatusCodes.START_SEQUNCE.getCode()
                        && statusCode != WorxLandroidStatusCodes.LEAVING_HOME.getCode()
                        && statusCode != WorxLandroidStatusCodes.SEARCHING_ZONE.getCode()) {
                    restoreZoneMeter = false;
                    mower.setZoneMeters(zoneMeterRestoreValues);
                    try {
                        sendZoneMeter();
                    } catch (AWSIotException e) {
                        // TODO Auto-generated catch block
                    }
                }
            }
        }
        // dat/le -> errorCode
        if (dat.get("le") != null) {
            long errorCode = dat.get("le").getAsLong();
            updateState(CHANNELNAME_ERROR_CODE, new DecimalType(errorCode));

            WorxLandroidErrorCodes code = WorxLandroidErrorCodes.getByCode((int) errorCode) == null
                    ? WorxLandroidErrorCodes.UNKNOWN
                    : WorxLandroidErrorCodes.getByCode((int) errorCode);
            updateState(CHANNELNAME_ERROR_DESCRIPTION, new StringType(code.getDescription()));
            logger.debug("{}", code.toString());
        }

        // dat/lz -> lastZone
        if (dat.get("lz") != null) {
            int lastZoneIndex = dat.get("lz").getAsInt();

            int lastZone = mower.getAllocation(lastZoneIndex);
            updateState(CHANNELNAME_LAST_ZONE, new DecimalType(lastZone));
        }

        // dat/rsi -> wifiQuality
        if (dat.get("rsi") != null) {
            updateState(CHANNELNAME_WIFI_QUALITY, new DecimalType(dat.get("rsi").getAsLong()));
        }

        // dat/lk -> lock
        if (mower.isLockSupported() && dat.get("lk") != null) {
            boolean lock = dat.get("lk").getAsInt() == 1 ? Boolean.TRUE : Boolean.FALSE;
            updateState(CHANNELNAME_LOCK, OnOffType.from(lock));
        }

        if (mower.isRainDelayStartSupported()) {
            // dat/rain
            if (dat.get("rain") != null) {
                JsonObject rain = dat.getAsJsonObject("rain");
                // dat/rain/s -> rainState
                if (rain.get("s") != null) {
                    boolean state = rain.get("s").getAsInt() == 1 ? Boolean.TRUE : Boolean.FALSE;
                    updateState(CHANNELNAME_RAIN_STATE, OnOffType.from(state));
                }
                // dat/rain/cnt -> rainCounter
                if (rain.get("cnt") != null) {
                    updateState(CHANNELNAME_RAIN_COUNTER, new DecimalType(rain.get("cnt").getAsLong()));
                }
            }
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

        ZoneId zoneId = ZoneId.getAvailableZoneIds().contains("time_zone") ? ZoneId.of("time_zone")
                : ZoneId.systemDefault();
        ZonedDateTime zonedDateTime = ZonedDateTime.of(localeDateTime, zoneId);
        updateState(CHANNELNAME_DATETIME, new DateTimeType(zonedDateTime));

        // TODO cfg/sc
        if (cfg.get("sc") != null) {

            JsonObject sc = cfg.getAsJsonObject("sc");

            // cfg/sc/m -> scheduleMode
            if (mower.isOneTimeSchedulerSupported()) {
                if (sc.get("m") != null) {
                    int mode = sc.get("m").getAsInt();
                    updateState(CHANNELNAME_SC_MODE, new DecimalType(mode));
                }

                // cdg/sc/ots
                if (sc.get("ots") != null) {
                    JsonObject ots = sc.getAsJsonObject("ots");

                    // cdg/sc/ots/bc ->
                    if (ots.get("bc") != null) {
                        boolean edgecut = ots.get("bc").getAsInt() == 1 ? Boolean.TRUE : Boolean.FALSE;
                        updateState(CHANNELNAME_ONE_TIME_SC_EDGECUT, OnOffType.from(edgecut));
                    }
                    // cdg/sc/ots/wtm
                    if (ots.get("wtm") != null) {
                        int duration = ots.get("wtm").getAsInt();
                        updateState(CHANNELNAME_ONE_TIME_SC_DURATION, new DecimalType(duration));
                    }
                }
            }

            // cfg/sc/p
            if (sc.get("p") != null) {
                int timeExtension = sc.get("p").getAsInt();
                mower.setTimeExtension(timeExtension);
                updateState(CHANNELNAME_SC_TIME_EXTENSION, new DecimalType(timeExtension));
                // mower enable
                updateState(CHANNELNAME_ENABLE, OnOffType.from(mower.isEnable()));
            }

            // cfg/sc/d
            if (sc.get("d") != null) {
                JsonArray d = sc.get("d").getAsJsonArray();
                updateStateCfgScDays(1, d);
            }

            // cfg/sc/dd
            if (sc.get("dd") != null) {
                JsonArray dd = sc.get("dd").getAsJsonArray();
                updateStateCfgScDays(2, dd);
            }
        }

        // cfg/cmd -> command
        if (cfg.get("cmd") != null)

        {
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

            // multizone enable is initialized and set by zone meters
            updateState(CHANNELNAME_MULTIZONE_ENABLE, OnOffType.from(mower.isMultiZoneEnable()));

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

    /**
     * @param scDSlot scheduled day slot
     * @param scDJson scheduled day JSON
     */
    private void updateStateCfgScDays(int scDSlot, JsonArray scDJson) {

        for (WorxLandroidDayCodes dayCode : WorxLandroidDayCodes.values()) {

            JsonArray shedule = scDJson.get(dayCode.getCode()).getAsJsonArray();

            ScheduledDay scheduledDay = scDSlot == 1 ? mower.getScheduledDay(dayCode) : mower.getScheduledDay2(dayCode);
            String channelNamePrefix = String.format("%s%s%s", CHANNELNAME_SC_PREFIX, dayCode.getDescription(),
                    scDSlot == 1 ? "" : String.valueOf(scDSlot));

            String time[] = shedule.get(0).getAsString().split(":");

            // hour
            String channelNameStartHour = String.format("%s#%s", channelNamePrefix, CHANNELNAME_SC_START_HOUR_SUFFIX);
            scheduledDay.setHours(Integer.parseInt(time[0]));
            updateState(channelNameStartHour, new DecimalType(time[0]));

            // minutes
            String channelNameStartMin = String.format("%s#%s", channelNamePrefix, CHANNELNAME_SC_START_MINUTES_SUFFIX);
            scheduledDay.setMinutes(Integer.parseInt(time[1]));
            updateState(channelNameStartMin, new DecimalType(time[1]));

            // duration (and implicit enable)
            String channelNameDuration = String.format("%s#%s", channelNamePrefix, CHANNELNAME_SC_DURATION_SUFFIX);
            int duration = shedule.get(1).getAsInt();
            scheduledDay.setDuration(duration);
            updateState(channelNameDuration, new DecimalType(shedule.get(1).getAsLong()));
            // enable
            String channelNameEnable = String.format("%s#%s", channelNamePrefix, CHANNELNAME_SC_ENABLE_SUFFIX);
            updateState(channelNameEnable, OnOffType.from(scheduledDay.isEnable()));

            // edgecut
            String channelNameEdgecut = String.format("%s#%s", channelNamePrefix, CHANNELNAME_SC_EDGECUT_SUFFIX);
            boolean edgecut = shedule.get(2).getAsInt() == 1 ? Boolean.TRUE : Boolean.FALSE;
            scheduledDay.setEdgecut(edgecut);
            updateState(channelNameEdgecut, OnOffType.from(edgecut));
        }
    }
}
