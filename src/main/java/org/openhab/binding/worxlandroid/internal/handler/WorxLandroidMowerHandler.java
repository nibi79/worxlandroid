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

import static org.openhab.binding.worxlandroid.internal.WorxLandroidBindingConstants.*;
import static org.openhab.binding.worxlandroid.internal.utils.ChannelTypeUtils.toQuantityType;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
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
import org.openhab.binding.worxlandroid.internal.deserializer.WebApiDeserializer;
import org.openhab.binding.worxlandroid.internal.mqtt.AWSException;
import org.openhab.binding.worxlandroid.internal.mqtt.AWSMessage;
import org.openhab.binding.worxlandroid.internal.mqtt.AWSMessageCallback;
import org.openhab.binding.worxlandroid.internal.mqtt.AWSTopic;
import org.openhab.binding.worxlandroid.internal.vo.Mower;
import org.openhab.binding.worxlandroid.internal.vo.ScheduledDay;
import org.openhab.binding.worxlandroid.internal.webapi.WebApiException;
import org.openhab.binding.worxlandroid.internal.webapi.WorxLandroidWebApi;
import org.openhab.binding.worxlandroid.internal.webapi.dto.ProductItemStatus;
import org.openhab.binding.worxlandroid.internal.webapi.dto.ProductItemStatus.Battery;
import org.openhab.binding.worxlandroid.internal.webapi.dto.ProductItemStatus.Cfg;
import org.openhab.binding.worxlandroid.internal.webapi.dto.ProductItemStatus.Dat;
import org.openhab.binding.worxlandroid.internal.webapi.dto.ProductItemStatus.Ots;
import org.openhab.binding.worxlandroid.internal.webapi.dto.ProductItemStatus.Payload;
import org.openhab.binding.worxlandroid.internal.webapi.dto.ProductItemStatus.Rain;
import org.openhab.binding.worxlandroid.internal.webapi.dto.ProductItemStatus.Schedule;
import org.openhab.binding.worxlandroid.internal.webapi.dto.ProductItemStatus.St;
import org.openhab.binding.worxlandroid.internal.webapi.response.ProductItemsResponse;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
    private @Nullable WorxLandroidWebApi apiHandler;

    private @Nullable AWSTopic awsTopic;
    private @Nullable String mqttCommandIn;

    private @Nullable ScheduledFuture<?> refreshStatusJob;
    private @Nullable ScheduledFuture<?> pollingJob;
    private @Nullable ScheduledFuture<?> reconnectJob;

    private boolean restoreZoneMeter = false;
    private int[] zoneMeterRestoreValues = {};
    private final WebApiDeserializer deserializer;

    /**
     * Defines a runnable for a refresh status job.
     * Checks if mower is online.
     */
    private Runnable refreshStatusRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (isBridgeOnline() && apiHandler != null) {
                    // // TODO NB hier oder in der bridge??
                    // WorxLandroidBridgeHandler bridge = getWorxLandroidBridgeHandler();
                    //
                    // if (!bridge.isTokenValid()) {
                    // bridge.reconnectAWSClient();
                    //
                    // if (!apiHandler.refreshToken()) {
                    // logger.debug("Refresh Access Token failed.");
                    // }
                    // logger.info("Refresh Access Token success.");
                    // }

                    ProductItemsResponse productItemsResponse = apiHandler.retrieveUserDevices();
                    JsonObject mowerDataJson = productItemsResponse.getMowerDataById(mower.getSerialNumber());

                    boolean online = mowerDataJson != null && mowerDataJson.get("online").getAsBoolean();
                    mower.setOnline(online);
                    updateState(new ChannelUID(thing.getUID(), GROUP_COMMON, CHANNEL_ONLINE), OnOffType.from(online));

                    updateState(new ChannelUID(thing.getUID(), GROUP_COMMON, CHANNEL_ONLINE_TIMESTAMP),
                            new DateTimeType());
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
                        AWSMessage message = new AWSMessage(mqttCommandIn, AWSMessage.EMPTY_PAYLOAD);
                        bridgeHandler.publishMessage(message);
                        logger.debug("send polling message");
                    }
                }
            } catch (AWSException e) {
                logger.error("PollingRunnable {}: {}", e.getLocalizedMessage(), mower.getSerialNumber());
            }
        }
    };

    public WorxLandroidMowerHandler(Thing thing, WebApiDeserializer deserializer) {
        super(thing);
        this.deserializer = deserializer;
    }

    private boolean isBridgeOnline() {
        WorxLandroidBridgeHandler bridgeHandler = getWorxLandroidBridgeHandler();
        return bridgeHandler != null ? bridgeHandler.isBridgeOnline() : false;
    }

    @SuppressWarnings("null")
    @Override
    public void initialize() {
        MowerConfiguration config = getConfigAs(MowerConfiguration.class);

        if (config.serialNumber.isBlank()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "@text/conf-error-no-serial");
            return;
        }

        mower = new Mower(config.serialNumber);

        logger.debug("Initializing WorxLandroidMowerHandler for serial number '{}'", config.serialNumber);

        if (isBridgeOnline()) {
            WorxLandroidBridgeHandler bridgeHandler = getWorxLandroidBridgeHandler();

            if (bridgeHandler != null) {
                apiHandler = bridgeHandler.getWorxLandroidWebApiImpl();

                try {
                    ProductItemsResponse productItemsResponse = apiHandler.retrieveUserDevices();
                    JsonObject mowerDataJson = productItemsResponse.getMowerDataById(config.serialNumber);

                    if (mowerDataJson != null) {
                        ThingBuilder thingBuilder = editThing();

                        // set mower properties
                        Map<String, String> props = productItemsResponse.getDataAsPropertyMap(config.serialNumber);
                        thingBuilder.withProperties(props);

                        mqttCommandIn = props.get("command_in");
                        String mqttCommandOut = props.get("command_out");

                        String fwv = props.get("firmware_version");
                        float firmwareVersion = fwv != null ? Float.parseFloat(fwv) : 0;

                        // lock channel only when supported
                        boolean lockSupported = Boolean.parseBoolean(props.get("lock"));
                        mower.setLockSupported(lockSupported);
                        if (!lockSupported) {
                            thingBuilder.withoutChannel(new ChannelUID(thing.getUID(), CHANNEL_LOCK));
                        }

                        // rainDelay channel only when supported
                        boolean rainDelaySupported = Boolean.parseBoolean(props.get("rain_delay"));
                        mower.setRainDelaySupported(rainDelaySupported);
                        if (!rainDelaySupported) {
                            thingBuilder.withoutChannel(new ChannelUID(thing.getUID(), CHANNEL_RAIN_DELAY));
                        }

                        // rainDelayStart channel only when supported
                        boolean rainDelayStart = false;
                        String rds = props.get("rain_delay_start");
                        if (rds != null) {
                            float rainDelayStartVersion = -1;
                            try {
                                rainDelayStartVersion = Float.parseFloat(rds);
                                rainDelayStart = firmwareVersion >= rainDelayStartVersion;
                            } catch (NumberFormatException e) {
                                logger.debug("Cannot format 'rain_delay_start': {}", rds);
                            }
                        }
                        mower.setRainDelayStartSupported(rainDelayStart);
                        if (!rainDelayStart) {
                            thingBuilder.withoutChannel(new ChannelUID(thing.getUID(), CHANNEL_RAIN_STATE));
                            thingBuilder.withoutChannel(new ChannelUID(thing.getUID(), CHANNEL_RAIN_COUNTER));
                        }

                        // multizone channels only when supported
                        boolean multiZoneSupported = Boolean.parseBoolean(props.get("multi_zone"));
                        mower.setMultiZoneSupported(multiZoneSupported);
                        if (!multiZoneSupported) {
                            // remove lastZome channel
                            thingBuilder.withoutChannel(new ChannelUID(thing.getUID(), CHANNEL_LAST_ZONE));
                            // remove zone meter channels
                            for (int zoneIndex = 0; zoneIndex < 4; zoneIndex++) {
                                String channelNameZoneMeter = String.format("%s#zone-%d", GROUP_MULTI_ZONES,
                                        zoneIndex + 1);
                                thingBuilder.withoutChannel(new ChannelUID(thing.getUID(), channelNameZoneMeter));
                            }
                            // remove allocation channels
                            for (int allocationIndex = 0; allocationIndex < 10; allocationIndex++) {
                                String channelNameAllocation = "%s-%d".formatted(CHANNEL_PREFIX_ALLOCATION,
                                        allocationIndex);
                                thingBuilder.withoutChannel(new ChannelUID(thing.getUID(), channelNameAllocation));
                            }
                        }

                        // oneTimeScheduler channel only when supported
                        boolean oneTimeScheduler = false;
                        String ots = props.get("one_time_scheduler");
                        if (ots != null) {
                            float oneTimeSchedulerVersion = -1;
                            try {
                                oneTimeSchedulerVersion = Float.parseFloat(ots);
                                oneTimeScheduler = firmwareVersion >= oneTimeSchedulerVersion;
                            } catch (NumberFormatException e) {
                                logger.debug("Cannot format 'one_time_scheduler': {}", ots);
                            }
                        }
                        mower.setOneTimeSchedulerSupported(oneTimeScheduler);
                        if (!oneTimeScheduler) {
                            ThingUID uid = thing.getUID();
                            thingBuilder.withoutChannel(new ChannelUID(uid, GROUP_ONE_TIME, CHANNEL_DURATION));
                            thingBuilder.withoutChannel(new ChannelUID(uid, GROUP_ONE_TIME, CHANNEL_EDGECUT));
                            thingBuilder.withoutChannel(new ChannelUID(uid, GROUP_ONE_TIME, CHANNEL_MODE));
                        }

                        // Scheduler 2 channels only when supported version
                        boolean scheduler2Supported = false;
                        String sts = props.get("scheduler_two_slots");
                        if (sts != null) {
                            float schedulerTwoSlotsVersion = -1;
                            try {
                                schedulerTwoSlotsVersion = Float.parseFloat(sts);
                                scheduler2Supported = firmwareVersion >= schedulerTwoSlotsVersion;
                            } catch (NumberFormatException e) {
                                logger.debug("Cannot format 'scheduler_two_slots': {}", sts);
                            }
                        }
                        mower.setScheduler2Supported(scheduler2Supported);
                        if (!scheduler2Supported) {
                            // remove schedule2 channels
                            ThingUID uid = thing.getUID();
                            for (WorxLandroidDayCodes dayCode : WorxLandroidDayCodes.values()) {
                                String groupName = "%s2".formatted(dayCode.getDescription());
                                thingBuilder.withoutChannel(new ChannelUID(uid, groupName, CHANNEL_ENABLE));
                                thingBuilder.withoutChannel(new ChannelUID(uid, groupName, CHANNEL_START_HOUR));
                                thingBuilder.withoutChannel(new ChannelUID(uid, groupName, CHANNEL_START_MINUTES));
                                thingBuilder.withoutChannel(new ChannelUID(uid, groupName, CHANNEL_DURATION));
                                thingBuilder.withoutChannel(new ChannelUID(uid, groupName, CHANNEL_EDGECUT));
                            }
                        }

                        updateThing(thingBuilder.build());

                        List<ProductItemStatus> productItemsStatusResponse = apiHandler
                                .retrieveDeviceStatus(config.serialNumber);
                        processStatusMessage(productItemsStatusResponse.get(0));

                        // handle AWS
                        if (mqttCommandOut != null) {
                            awsTopic = new AWSTopic(mqttCommandOut, this);
                            bridgeHandler.subcribeTopic(awsTopic);
                        }

                        AWSMessage message = new AWSMessage(mqttCommandIn, AWSMessage.EMPTY_PAYLOAD);
                        bridgeHandler.publishMessage(message);

                        updateStatus(
                                mowerDataJson.get("online").getAsBoolean() ? ThingStatus.ONLINE : ThingStatus.OFFLINE);

                        // scheduled jobs
                        startScheduledJobs();
                    } else {
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.GONE);
                        return;
                    }
                } catch (WebApiException | AWSException e) {
                    logger.error("initialize mower: id {} - {}::{}", config.serialNumber, getThing().getLabel(),
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
     * Start scheduled jobs.
     * Jobs are only started if interval > 0
     */
    private void startScheduledJobs() {
        MowerConfiguration config = getConfigAs(MowerConfiguration.class);

        int refreshStatusInterval = config.refreshStatusInterval;
        if (refreshStatusInterval > 0) {
            refreshStatusJob = scheduler.scheduleWithFixedDelay(refreshStatusRunnable, 30, refreshStatusInterval,
                    TimeUnit.SECONDS);
        }

        int pollingIntervall = config.pollingInterval;
        if (pollingIntervall > 0) {
            pollingJob = scheduler.scheduleWithFixedDelay(pollingRunnable, 60, pollingIntervall, TimeUnit.SECONDS);
        }

        int reconnectInterval = config.reconnectInterval;
        if (reconnectInterval > 0) {
            reconnectJob = scheduler.scheduleWithFixedDelay(() -> {
                WorxLandroidBridgeHandler bridgeHandler = getWorxLandroidBridgeHandler();
                if (bridgeHandler != null) {
                    logger.debug("reconnecting");
                    bridgeHandler.reconnectToWorx();
                }
            }, 60, reconnectInterval, TimeUnit.SECONDS);
        }
    }

    /**
     * @return
     */
    private synchronized @Nullable WorxLandroidBridgeHandler getWorxLandroidBridgeHandler() {
        Bridge bridge = getBridge();
        if (bridge != null) {
            ThingHandler handler = bridge.getHandler();
            if (handler instanceof WorxLandroidBridgeHandler bridgeHandler) {
                return bridgeHandler;
            }
        }
        return null;
    }

    @Override
    public void dispose() {
        if (refreshStatusJob != null) {
            refreshStatusJob.cancel(true);
        }

        if (pollingJob != null) {
            pollingJob.cancel(true);
        }

        if (reconnectJob != null) {
            reconnectJob.cancel(true);
        }
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        if (ThingStatus.OFFLINE.equals(bridgeStatusInfo.getStatus())) {
            mower.setOnline(false);
        }

        // TODO NB workaround reconnect nötig???
        if (ThingStatus.ONLINE.equals(bridgeStatusInfo.getStatus())) {
            try {
                // awsTopic = new AWSTopic(awsTopic.getTopic(), this);
                WorxLandroidBridgeHandler bridgeHandler = getWorxLandroidBridgeHandler();
                if (bridgeHandler != null) {
                    bridgeHandler.subcribeTopic(awsTopic);
                }
            } catch (AWSException e) {
            }
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
            if (CHANNEL_ENABLE.equals(channelUID.getIdWithoutGroup())
                    && GROUP_MULTI_ZONES.equals(channelUID.getGroupId())) {
                mower.setMultiZoneEnable(OnOffType.ON.equals(command));
                sendZoneMeter();
                return;
            }

            if (CHANNEL_LAST_ZONE.equals(channelUID.getId())) {
                if (mower.getStatus() != WorxLandroidStatusCodes.HOME.code) {
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
            if (channelUID.getGroupId().equals(GROUP_MULTI_ZONES)) {
                String names[] = channelUID.getIdWithoutGroup().split("-");
                int zone = Integer.valueOf(names[1]);
                mower.setZoneMeter(zone - 1, Integer.parseInt(command.toString()));
                sendZoneMeter();
                return;
            }
            // channel: multizone allocation (mzv)
            if (channelUID.getId().contains(CHANNEL_PREFIX_ALLOCATION)) {
                String[] channelName = channelUID.getIdWithoutGroup().split("-");
                int allocationIndex = Integer.valueOf(channelName[1]);

                JsonObject jsonObject = new JsonObject();
                JsonArray mzv = new JsonArray();

                // extract allocation index of from channel
                // Pattern pattern = Pattern.compile(CHANNEL_PREFIX_ALLOCATION + "(\\d)");
                // Matcher matcher = pattern.matcher(channelUID.getId());
                // int allocationIndex = 0;
                // if (matcher.find()) {
                // allocationIndex = Integer.parseInt(matcher.group(1));
                // }

                mower.setAllocation(allocationIndex, Integer.parseInt(command.toString()));

                for (int i = 0; i < 10; i++) {
                    mzv.add(mower.getAllocation(i));
                }

                jsonObject.add("mzv", mzv);
                sendCommand(jsonObject.toString());
                return;
            }

            if (CHANNEL_MODE.equals(channelUID.getIdWithoutGroup())) {
                // generate 'sc' message
                JsonObject jsonObject = new JsonObject();
                JsonObject sc = new JsonObject();

                // mode
                sc.add("m", new JsonPrimitive(Integer.parseInt(command.toString())));

                jsonObject.add("sc", sc);

                sendCommand(jsonObject.toString());
                return;
            }

            if (CHANNEL_DURATION.equals(channelUID.getIdWithoutGroup())) {
                sendOneTimeSchedule(0, Integer.parseInt(command.toString()));
                return;
            }

            if (CHANNEL_EDGECUT.equals(channelUID.getIdWithoutGroup())) {
                sendOneTimeSchedule(OnOffType.ON.equals(command) ? 1 : 0, 0);
                return;
            }

            // update schedule
            // TODO ugly check
            if (CHANNEL_ENABLE.equals(channelUID.getIdWithoutGroup())
                    || channelUID.getGroupId().equals(GROUP_SCHEDULE)) {
                // update mower data

                // update enable mowing or schedule or timeExtension/enable?
                if (CHANNEL_ENABLE.equals(channelUID.getIdWithoutGroup())) {
                    mower.setEnable(OnOffType.ON.equals(command));
                } else if (CHANNEL_TIME_EXTENSION.equals(channelUID.getIdWithoutGroup())) {
                    mower.setTimeExtension(Integer.parseInt(command.toString()));
                } else {
                    setScheduledDays(channelUID, command);
                }

                sendSchedule();
                return;
            }

            String cmd = AWSMessage.EMPTY_PAYLOAD;

            switch (channelUID.getIdWithoutGroup()) {
                // start action
                case CHANNEL_ACTION:
                    WorxLandroidActionCodes actionCode = WorxLandroidActionCodes.valueOf(command.toString());
                    logger.debug("{}", actionCode.toString());
                    cmd = "{\"cmd\":%s}".formatted(actionCode.code);
                    break;

                // poll
                case CHANNEL_POLL:
                    cmd = AWSMessage.EMPTY_PAYLOAD;
                    updateState(CHANNEL_POLL, OnOffType.OFF);
                    break;

                // update rainDelay
                case CHANNEL_RAIN_DELAY:
                    cmd = "{\"rd\":%s}".formatted(command);
                    break;

                // lock/unlock
                case CHANNEL_LOCK:
                    WorxLandroidActionCodes lockCode = OnOffType.ON.equals(command) ? WorxLandroidActionCodes.LOCK
                            : WorxLandroidActionCodes.UNLOCK;
                    logger.debug("{}", lockCode.toString());
                    cmd = "{\"cmd\":%s}".formatted(lockCode.code);
                    break;

                default:
                    logger.debug("command for ChannelUID not supported: {}", channelUID.getAsString());
                    break;
            }
            sendCommand(cmd);
        } catch (AWSException e) {
            logger.error("error: {}", e.getLocalizedMessage());
        }
    }

    /**
     * @param bc
     * @param wtm
     * @throws AWSIotException
     */
    private void sendOneTimeSchedule(int bc, int wtm) throws AWSException {
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

        if (scheduledDayUpdated == null) {
            return;
        }

        String chName = channelUID.getIdWithoutGroup();
        switch (chName) {
            case CHANNEL_ENABLE:
                scheduledDayUpdated.setEnable(OnOffType.ON.equals(command));
                break;

            case CHANNEL_START_HOUR:
                scheduledDayUpdated.setHours(Integer.parseInt(command.toString()));
                break;

            case CHANNEL_START_MINUTES:
                scheduledDayUpdated.setMinutes(Integer.parseInt(command.toString()));
                break;

            case CHANNEL_DURATION:
                scheduledDayUpdated.setDuration(Integer.parseInt(command.toString()));
                break;

            case CHANNEL_EDGECUT:
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
    private void sendSchedule() throws AWSException {
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

            if (scheduledDay == null) {
                return jsonArray;
            }

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
    private void sendZoneMeter() throws AWSException {
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
     * @throws AWSException
     * @throws AWSIotException
     */
    private void sendCommand(String cmd) throws AWSException {
        logger.debug("send command: {}", cmd);

        WorxLandroidBridgeHandler bridgeHandler = getWorxLandroidBridgeHandler();

        if (bridgeHandler != null) {
            AWSMessage message = new AWSMessage(mqttCommandIn, cmd);
            bridgeHandler.publishMessage(message);
        }
    }

    @Override
    public void processMessage(AWSMessage message) {
        updateStatus(ThingStatus.ONLINE);

        try {
            Payload payload = deserializer.deserialize(Payload.class, message.payload());
            processStatusMessage(payload);
        } catch (WebApiException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void processStatusMessage(Payload payload) {
        updateStateCfg(payload.cfg, null);
        updateStateDat(payload.dat);
    }

    /**
     * @param productItemStatus
     */
    private void processStatusMessage(ProductItemStatus productItemStatus) {
        // cfg
        // if (productItemStatus.get("cfg") != null) {
        updateStateCfg(productItemStatus.lastStatus.payload.cfg, productItemStatus.timeZone);
        // }

        // dat
        // if (productItemStatus.get("dat") != null) {
        updateStateDat(productItemStatus.lastStatus.payload.dat);
        // }
    }

    /**
     * Update states for data values
     *
     * @param dat
     */
    private void updateStateDat(Dat dat) {
        // updateState(CHANNEL_MAC_ADRESS, dat.mac.isEmpty() ? UnDefType.NULL : new StringType(dat.mac));
        updateState(CHANNEL_FIRMWARE, dat.fw.isEmpty() ? UnDefType.UNDEF : new StringType(dat.fw));

        if (dat.battery != null) {
            Battery battery = dat.battery;
            updateState(new ChannelUID(thing.getUID(), GROUP_BATTERY, CHANNEL_TEMPERATURE),
                    battery.temperature != -1 ? toQuantityType(battery.temperature, SIUnits.CELSIUS) : UnDefType.NULL);
            updateState(new ChannelUID(thing.getUID(), GROUP_BATTERY, CHANNEL_VOLTAGE),
                    battery.voltage != -1 ? toQuantityType(battery.voltage, Units.VOLT) : UnDefType.NULL);
            updateState(new ChannelUID(thing.getUID(), GROUP_BATTERY, CHANNEL_LEVEL),
                    battery.level != -1 ? new DecimalType(battery.level) : UnDefType.NULL);

            updateState(new ChannelUID(thing.getUID(), GROUP_BATTERY, CHANNEL_CHARGE_CYCLE),
                    battery.chargeCycle != -1 ? new DecimalType(battery.chargeCycle) : UnDefType.NULL);

            long batteryChargeCyclesCurrent = battery.chargeCycle;
            String batteryChargeCyclesReset = getThing().getProperties().get("battery_charge_cycles_reset");
            if (batteryChargeCyclesReset != null && !batteryChargeCyclesReset.isEmpty()) {
                batteryChargeCyclesCurrent = battery.chargeCycle - Long.valueOf(batteryChargeCyclesReset);
            }
            updateState(new ChannelUID(thing.getUID(), GROUP_BATTERY, CHANNEL_CHARGE_CYCLE_CURRENT),
                    new DecimalType(batteryChargeCyclesCurrent));
            updateState(new ChannelUID(thing.getUID(), GROUP_BATTERY, CHANNEL_CHARGING),
                    OnOffType.from(battery.isCharging()));
        }

        // dat/dmp.[0] -> pitch
        if (dat.dmp.get(0) != null) {
            updateState(new ChannelUID(thing.getUID(), GROUP_ORIENTATION, CHANNEL_PITCH),
                    toQuantityType(dat.dmp.get(0), Units.DEGREE_ANGLE));
        }
        // dat/dmp.[1] -> roll
        if (dat.dmp.get(1) != null) {
            updateState(new ChannelUID(thing.getUID(), GROUP_ORIENTATION, CHANNEL_ROLL),
                    toQuantityType(dat.dmp.get(1), Units.DEGREE_ANGLE));
        }
        // dat/dmp.[2] -> yaw
        if (dat.dmp.get(2) != null) {
            updateState(new ChannelUID(thing.getUID(), GROUP_ORIENTATION, CHANNEL_YAW),
                    toQuantityType(dat.dmp.get(2), Units.DEGREE_ANGLE));
        }

        // dat/st
        if (dat.st != null) {
            St st = dat.st;
            // dat/st/b -> totalBladeTime
            if (st.totalBladeTime != -1) {
                updateState(new ChannelUID(thing.getUID(), GROUP_METRICS, CHANNEL_TOTAL_BLADE_TIME),
                        toQuantityType(st.totalBladeTime, Units.MINUTE));

                long bladeTimeCurrent = st.totalBladeTime;
                String bladeWorkTimeReset = getThing().getProperties().get("blade_work_time_reset");
                if (bladeWorkTimeReset != null && !bladeWorkTimeReset.isEmpty()) {
                    bladeTimeCurrent = st.totalBladeTime - Long.valueOf(bladeWorkTimeReset);
                }
                updateState(new ChannelUID(thing.getUID(), GROUP_METRICS, CHANNEL_CURRENT_BLADE_TIME),
                        toQuantityType(bladeTimeCurrent, Units.MINUTE));
            }

            updateState(new ChannelUID(thing.getUID(), GROUP_METRICS, CHANNEL_TOTAL_DISTANCE),
                    st.totalDistance != -1 ? toQuantityType(st.totalDistance, SIUnits.METRE) : UnDefType.NULL);
            updateState(new ChannelUID(thing.getUID(), GROUP_METRICS, CHANNEL_TOTAL_TIME),
                    st.totalTime != -1 ? toQuantityType(st.totalTime, Units.MINUTE) : UnDefType.NULL);
            // TODO dat/st/bl -> ?
        }

        // dat/ls -> statusCode
        mower.setStatus(dat.statusCode);
        updateState(new ChannelUID(thing.getUID(), GROUP_GENERAL, CHANNEL_STATUS_CODE),
                new DecimalType(dat.statusCode));

        WorxLandroidStatusCodes code = WorxLandroidStatusCodes.getByCode(dat.statusCode);
        // updateState(CHANNEL_STATUS_DESCRIPTION, new StringType(code.getDescription()));
        logger.debug("{}", code.toString());

        // restore
        if (restoreZoneMeter) {
            if (code != WorxLandroidStatusCodes.HOME && code != WorxLandroidStatusCodes.START_SEQUENCE
                    && code != WorxLandroidStatusCodes.LEAVING_HOME && code != WorxLandroidStatusCodes.SEARCHING_ZONE) {
                restoreZoneMeter = false;
                mower.setZoneMeters(zoneMeterRestoreValues);
                try {
                    sendZoneMeter();
                } catch (AWSException e) {
                    // TODO Auto-generated catch block
                }
            }
        }

        // dat/le -> errorCode
        updateState(new ChannelUID(thing.getUID(), GROUP_GENERAL, CHANNEL_ERROR_CODE), new DecimalType(dat.errorCode));

        WorxLandroidErrorCodes errorCode = WorxLandroidErrorCodes.getByCode(dat.errorCode);
        // updateState(CHANNEL_ERROR_DESCRIPTION, new StringType(errorCode.getDescription()));
        logger.debug("{}", code.toString());

        // dat/lz -> lastZone
        int lastZone = mower.getAllocation(dat.lastZone);
        updateState(new ChannelUID(thing.getUID(), GROUP_GENERAL, CHANNEL_LAST_ZONE), new DecimalType(lastZone));

        updateState(new ChannelUID(thing.getUID(), GROUP_GENERAL, CHANNEL_WIFI_QUALITY),
                new DecimalType(dat.wifiQuality));

        // dat/lk -> lock
        if (mower.isLockSupported()) {
            updateState(CHANNEL_LOCK, OnOffType.from(dat.isLocked()));
        }

        if (mower.isRainDelayStartSupported()) {
            // dat/rain
            if (dat.rain != null) {
                Rain rain = dat.rain;
                updateState(new ChannelUID(thing.getUID(), GROUP_RAIN, CHANNEL_RAIN_STATE),
                        OnOffType.from(rain.isRaining()));
                // dat/rain/cnt -> rainCounter
                updateState(new ChannelUID(thing.getUID(), GROUP_RAIN, CHANNEL_RAIN_COUNTER),
                        new DecimalType(rain.counter));
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
     * @param zoneId
     */
    private void updateStateCfg(Cfg cfg, @Nullable ZoneId zoneId) {
        updateState(new ChannelUID(thing.getUID(), GROUP_CONFIG, CHANNEL_ID),
                cfg.id != -1 ? new DecimalType(cfg.id) : UnDefType.NULL);
        updateState(new ChannelUID(thing.getUID(), GROUP_CONFIG, CHANNEL_LANGUAGE),
                !cfg.lg.isEmpty() ? new StringType(cfg.lg) : UnDefType.NULL);

        LocalDateTime dateTime = cfg.getDateTime();
        State resultDateTime = UnDefType.NULL;
        if (dateTime != null && zoneId != null) {
            ZonedDateTime zonedDateTime = ZonedDateTime.of(dateTime, zoneId);
            resultDateTime = new DateTimeType(zonedDateTime);
        }
        updateState(new ChannelUID(thing.getUID(), GROUP_CONFIG, CHANNEL_TIMESTAMP), resultDateTime);

        // TODO cfg/sc
        if (cfg.sc != null) {
            Schedule sc = cfg.sc;

            // cfg/sc/m -> scheduleMode
            if (mower.isOneTimeSchedulerSupported()) {
                updateState(new ChannelUID(thing.getUID(), GROUP_SCHEDULE, CHANNEL_MODE),
                        sc.scheduleMode != -1 ? new DecimalType(sc.scheduleMode) : UnDefType.NULL);

                // cdg/sc/ots
                if (sc.ots != null) {
                    Ots ots = sc.ots;
                    updateState(new ChannelUID(thing.getUID(), GROUP_ONE_TIME, CHANNEL_EDGECUT),
                            OnOffType.from(ots.getEdgeCut()));
                    updateState(new ChannelUID(thing.getUID(), GROUP_ONE_TIME, CHANNEL_DURATION),
                            ots.duration != -1 ? new DecimalType(ots.duration) : UnDefType.NULL);
                }
            }

            // cfg/sc/p
            if (sc.timeExtension != -1) {
                mower.setTimeExtension(sc.timeExtension);
                updateState(new ChannelUID(thing.getUID(), GROUP_SCHEDULE, CHANNEL_TIME_EXTENSION),
                        toQuantityType(sc.timeExtension, Units.PERCENT));
                // mower enable
                updateState(new ChannelUID(thing.getUID(), GROUP_COMMON, CHANNEL_ENABLE),
                        OnOffType.from(mower.isEnable()));
            }

            if (sc.d != null) {
                updateStateCfgScDays(1, sc.d);
            }

            if (sc.dd != null) {
                updateStateCfgScDays(2, sc.dd);
            }
        }

        // cfg/cmd -> command
        updateState(new ChannelUID(thing.getUID(), GROUP_CONFIG, CHANNEL_COMMAND),
                cfg.cmd != -1 ? new DecimalType(cfg.cmd) : UnDefType.NULL);

        if (mower.isMultiZoneSupported()) {
            // zone meters
            if (!cfg.multiZones.isEmpty()) {
                for (int zoneIndex = 0; zoneIndex < 4; zoneIndex++) {
                    int meters = cfg.multiZones.get(zoneIndex);
                    mower.setZoneMeter(zoneIndex, meters);
                    updateState("cfgMultiZones#zone%dMeter".formatted(zoneIndex + 1), new DecimalType(meters));
                }
            }

            // multizone enable is initialized and set by zone meters
            updateState(new ChannelUID(thing.getUID(), GROUP_MULTI_ZONES, CHANNEL_ENABLE),
                    OnOffType.from(mower.isMultiZoneEnable()));

            // allocation zones
            if (!cfg.multizoneAllocations.isEmpty()) {
                for (int allocationIndex = 0; allocationIndex < 10; allocationIndex++) {
                    mower.setAllocation(allocationIndex, cfg.multizoneAllocations.get(allocationIndex));
                    String channelNameAllocation = "%s-%d".formatted(CHANNEL_PREFIX_ALLOCATION, allocationIndex);
                    updateState(new ChannelUID(thing.getUID(), GROUP_MULTI_ZONES, channelNameAllocation),
                            new DecimalType(cfg.multizoneAllocations.get(allocationIndex)));
                }
            }
        }

        updateState(new ChannelUID(thing.getUID(), GROUP_CONFIG, CHANNEL_RAIN_DELAY),
                mower.isRainDelaySupported() && cfg.rainDelay != -1 ? toQuantityType(cfg.rainDelay, Units.MINUTE)
                        : UnDefType.NULL);

        // TODO cfg/modules
    }

    /**
     * @param scDSlot scheduled day slot
     * @param d scheduled day JSON
     */
    private void updateStateCfgScDays(int scDSlot, List<List<String>> d) {
        for (WorxLandroidDayCodes dayCode : WorxLandroidDayCodes.values()) {
            List<String> shedule = d.get(dayCode.code);

            ScheduledDay scheduledDay = scDSlot == 1 ? mower.getScheduledDay(dayCode) : mower.getScheduledDay2(dayCode);

            if (scheduledDay == null) {
                return;
            }
            ThingUID thingUid = thing.getUID();
            String groupName = String.format("%s%s", dayCode.getDescription(),
                    scDSlot == 1 ? "" : String.valueOf(scDSlot));

            String time[] = shedule.get(0).split(":");

            // hour
            scheduledDay.setHours(Integer.parseInt(time[0]));
            updateState(new ChannelUID(thingUid, groupName, CHANNEL_START_HOUR), new DecimalType(time[0]));

            // minutes
            scheduledDay.setMinutes(Integer.parseInt(time[1]));
            updateState(new ChannelUID(thingUid, groupName, CHANNEL_START_MINUTES), new DecimalType(time[1]));

            // duration (and implicit enable)
            int duration = Integer.valueOf(shedule.get(1));
            scheduledDay.setDuration(duration);
            updateState(new ChannelUID(thingUid, groupName, CHANNEL_DURATION), toQuantityType(duration, Units.MINUTE));
            // enable
            updateState(new ChannelUID(thingUid, groupName, CHANNEL_ENABLE), OnOffType.from(scheduledDay.isEnable()));

            // edgecut
            boolean edgecut = Integer.valueOf(shedule.get(2)) == 1;
            scheduledDay.setEdgecut(edgecut);
            updateState(new ChannelUID(thingUid, groupName, CHANNEL_EDGECUT), OnOffType.from(edgecut));
        }
    }
}
