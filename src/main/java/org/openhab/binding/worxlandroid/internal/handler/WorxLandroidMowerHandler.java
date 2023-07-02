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

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.worxlandroid.internal.api.WebApiException;
import org.openhab.binding.worxlandroid.internal.api.WorxApiDeserializer;
import org.openhab.binding.worxlandroid.internal.api.dto.ProductItemStatus;
import org.openhab.binding.worxlandroid.internal.api.dto.ProductItemStatus.Battery;
import org.openhab.binding.worxlandroid.internal.api.dto.ProductItemStatus.Cfg;
import org.openhab.binding.worxlandroid.internal.api.dto.ProductItemStatus.Dat;
import org.openhab.binding.worxlandroid.internal.api.dto.ProductItemStatus.Ots;
import org.openhab.binding.worxlandroid.internal.api.dto.ProductItemStatus.Payload;
import org.openhab.binding.worxlandroid.internal.api.dto.ProductItemStatus.Rain;
import org.openhab.binding.worxlandroid.internal.api.dto.ProductItemStatus.Schedule;
import org.openhab.binding.worxlandroid.internal.api.dto.ProductItemStatus.St;
import org.openhab.binding.worxlandroid.internal.codes.WorxLandroidActionCodes;
import org.openhab.binding.worxlandroid.internal.codes.WorxLandroidDayCodes;
import org.openhab.binding.worxlandroid.internal.codes.WorxLandroidStatusCodes;
import org.openhab.binding.worxlandroid.internal.config.MowerConfiguration;
import org.openhab.binding.worxlandroid.internal.mqtt.AWSMessage;
import org.openhab.binding.worxlandroid.internal.mqtt.AWSMessageCallback;
import org.openhab.binding.worxlandroid.internal.mqtt.AWSTopic;
import org.openhab.binding.worxlandroid.internal.vo.Mower;
import org.openhab.binding.worxlandroid.internal.vo.ScheduledDay;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
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
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The{@link WorxLandroidMowerHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Nils - Initial contribution
 *
 */
@NonNullByDefault
public class WorxLandroidMowerHandler extends BaseThingHandler implements AWSMessageCallback {
    record ZoneMeterCommand(int[] mz) {
    }

    record ZoneMeterAlloc(int[] mzv) {
    }

    record ScheduleCommandMode(int m) {
    }

    record MowerCommand(int cmd) {
        public MowerCommand(WorxLandroidActionCodes actionCode) {
            this(actionCode.code);
        }
    }

    record SetRainDelay(int rd) {
    }

    record ScheduleCommand(ScheduleCommandMode sc) {

        public ScheduleCommand(int m) {
            this(new ScheduleCommandMode(m));
        }
    }

    record ScheduleDaysP(int p, Object d, @Nullable Object dd) {
    }

    record ScheduleDaysCommand(ScheduleDaysP sc) {

        public ScheduleDaysCommand(int p, Object[] d, Object[] dd) {
            this(new ScheduleDaysP(p, d, dd));
        }

        public ScheduleDaysCommand(int p, Object[] d) {
            this(new ScheduleDaysP(p, d, null));
        }
    }

    record OTS(OTSCommand ots) {

    }

    record OTSCommand(int bc, int wtm) {
        // bc = bordercut
        // wtm = work time minutes
    }

    record OneTimeCommand(OTS sc) {
        public OneTimeCommand(int bc, int wtm) {
            this(new OTS(new OTSCommand(bc, wtm)));
        }
    }

    private final Logger logger = LoggerFactory.getLogger(WorxLandroidMowerHandler.class);
    private final WorxApiDeserializer deserializer;

    private Optional<AWSTopic> awsTopic = Optional.empty();
    private Optional<Mower> mower = Optional.empty();
    private Optional<ScheduledFuture<?>> refreshJob = Optional.empty();
    private Optional<ScheduledFuture<?>> pollingJob = Optional.empty();

    private boolean restoreZoneMeter = false;
    private int[] zoneMeterRestoreValues = {};

    public WorxLandroidMowerHandler(Thing thing, WorxApiDeserializer deserializer) {
        super(thing);
        this.deserializer = deserializer;
    }

    @Override
    public void initialize() {
        MowerConfiguration config = getConfigAs(MowerConfiguration.class);

        if (config.serialNumber.isBlank()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "@text/conf-error-no-serial");
            return;
        }

        WorxLandroidBridgeHandler bridgeHandler = getWorxLandroidBridgeHandler();
        if (bridgeHandler != null) {
            logger.debug("Initializing WorxLandroidMowerHandler for serial number '{}'", config.serialNumber);
            try {
                ProductItemStatus product = bridgeHandler.retrieveDeviceStatus(config.serialNumber);
                if (product != null) {
                    Mower theMower = new Mower(product);
                    ThingUID thingUid = thing.getUID();
                    ThingBuilder thingBuilder = editThing();

                    if (!theMower.lockSupported()) { // lock channel only when supported
                        thingBuilder.withoutChannel(new ChannelUID(thingUid, GROUP_COMMON, CHANNEL_LOCK));
                    }

                    if (!theMower.rainDelaySupported()) { // rainDelay channel only when supported
                        thingBuilder.withoutChannel(new ChannelUID(thingUid, GROUP_RAIN, CHANNEL_DELAY));
                    }

                    if (!theMower.rainDelayStartSupported()) { // // rainDelayStart channel only when supported
                        thingBuilder.withoutChannel(new ChannelUID(thingUid, GROUP_RAIN, CHANNEL_RAIN_STATE));
                        thingBuilder.withoutChannel(new ChannelUID(thingUid, GROUP_RAIN, CHANNEL_RAIN_COUNTER));
                    }

                    if (!theMower.multiZoneSupported()) { // multizone channels only when supported
                        // remove lastZome channel
                        thingBuilder.withoutChannel(new ChannelUID(thingUid, GROUP_RAIN, CHANNEL_LAST_ZONE));
                        // remove zone meter channels
                        for (int zoneIndex = 0; zoneIndex < theMower.getMultiZoneCount(); zoneIndex++) {
                            thingBuilder.withoutChannel(
                                    new ChannelUID(thingUid, GROUP_MULTI_ZONES, "zone-%d".formatted(zoneIndex + 1)));
                        }
                        // remove allocation channels
                        for (int allocationIndex = 0; allocationIndex < 10; allocationIndex++) {
                            thingBuilder.withoutChannel(new ChannelUID(thingUid, GROUP_MULTI_ZONES,
                                    "%s-%d".formatted(CHANNEL_PREFIX_ALLOCATION, allocationIndex)));
                        }
                    }

                    if (!theMower.oneTimeSchedulerSupported()) { // oneTimeScheduler channel only when supported
                        thingBuilder.withoutChannel(new ChannelUID(thingUid, GROUP_ONE_TIME, CHANNEL_DURATION));
                        thingBuilder.withoutChannel(new ChannelUID(thingUid, GROUP_ONE_TIME, CHANNEL_EDGECUT));
                        thingBuilder.withoutChannel(new ChannelUID(thingUid, GROUP_ONE_TIME, CHANNEL_MODE));
                    }

                    if (!theMower.scheduler2Supported()) { // Scheduler 2 channels only when supported version
                        for (WorxLandroidDayCodes dayCode : WorxLandroidDayCodes.values()) {
                            String groupName = "%s2".formatted(dayCode.getDescription().toLowerCase());
                            thingBuilder.withoutChannel(new ChannelUID(thingUid, groupName, CHANNEL_ENABLE));
                            thingBuilder.withoutChannel(new ChannelUID(thingUid, groupName, CHANNEL_DURATION));
                            thingBuilder.withoutChannel(new ChannelUID(thingUid, groupName, CHANNEL_EDGECUT));
                            thingBuilder.withoutChannel(new ChannelUID(thingUid, groupName, CHANNEL_TIME));
                        }
                    }

                    updateThing(thingBuilder.build());

                    processStatusMessage(theMower, product);

                    AWSTopic commandOutTopic = new AWSTopic(theMower.getMqttCommandOut(), this);
                    bridgeHandler.subcribeTopic(commandOutTopic);
                    bridgeHandler.publishMessage(theMower.getMqttCommandIn(), AWSMessage.EMPTY_PAYLOAD);

                    mower = Optional.of(theMower);
                    awsTopic = Optional.of(commandOutTopic);
                    updateStatus(product.online ? ThingStatus.ONLINE : ThingStatus.OFFLINE);
                    startScheduledJobs(bridgeHandler, theMower);
                }
            } catch (WebApiException e) {
                logger.error("initialize mower: id {} - {}::{}", config.serialNumber, getThing().getLabel(),
                        getThing().getUID());
            }
            updateStatus(ThingStatus.ONLINE);
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.BRIDGE_OFFLINE);
        }
    }

    private void processStatusMessage(Mower theMower, ProductItemStatus product) {
        processStatusMessage(theMower, product.lastStatus.payload);
        initializeProperties(product);
    }

    private void processStatusMessage(Mower theMower, Payload payload) {
        updateStateCfg(theMower, payload.cfg);
        updateStateDat(theMower, payload.dat);
    }

    void initializeProperties(ProductItemStatus product) {
        Map<String, String> properties = editProperties();
        properties.put(Thing.PROPERTY_SERIAL_NUMBER, product.serialNumber);
        properties.put(Thing.PROPERTY_FIRMWARE_VERSION, Double.toString(product.firmwareVersion));
        properties.put(Thing.PROPERTY_MAC_ADDRESS, product.macAddress);
        properties.put(Thing.PROPERTY_VENDOR, "Worx");
        properties.put("productId", product.id);
        properties.put("language", product.lastStatus.payload.cfg.lg);
        updateProperties(properties);
    }

    /**
     * Start scheduled jobs.
     * Jobs are only started if interval > 0
     *
     * @param bridgeHandler
     *
     * @param theMower
     */
    private void startScheduledJobs(WorxLandroidBridgeHandler bridgeHandler, Mower theMower) {
        MowerConfiguration config = getConfigAs(MowerConfiguration.class);

        if (config.refreshStatusInterval > 0) {
            refreshJob = Optional.of(scheduler.scheduleWithFixedDelay(() -> {
                try {
                    ProductItemStatus product = bridgeHandler.retrieveDeviceStatus(config.serialNumber);
                    updateChannelDateTime(GROUP_COMMON, CHANNEL_ONLINE_TIMESTAMP, ZonedDateTime.now());
                    updateChannelOnOff(GROUP_COMMON, CHANNEL_ONLINE, product != null && product.online);
                    updateStatus(product != null ? ThingStatus.ONLINE : ThingStatus.OFFLINE);
                } catch (WebApiException e) {
                    logger.debug("Refreshing Thing {} failed, handler might be OFFLINE", config.serialNumber);
                }
            }, 3, config.refreshStatusInterval, TimeUnit.SECONDS));
        }

        if (config.pollingInterval > 0) {
            pollingJob = Optional.of(scheduler.scheduleWithFixedDelay(() -> {
                bridgeHandler.publishMessage(theMower.getMqttCommandIn(), AWSMessage.EMPTY_PAYLOAD);
                logger.debug("send polling message");
            }, 5, config.pollingInterval, TimeUnit.SECONDS));
        }
    }

    /**
     * @return
     */
    private synchronized @Nullable WorxLandroidBridgeHandler getWorxLandroidBridgeHandler() {
        Bridge bridge = getBridge();
        if (bridge != null && bridge.getHandler() instanceof WorxLandroidBridgeHandler bridgeHandler
                && bridgeHandler.isBridgeOnline()) {
            return bridgeHandler;
        }
        return null;
    }

    @Override
    public void dispose() {
        refreshJob.ifPresent(job -> job.cancel(true));
        refreshJob = Optional.empty();

        pollingJob.ifPresent(job -> job.cancel(true));
        pollingJob = Optional.empty();
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        // TODO NB workaround reconnect nötig???
        WorxLandroidBridgeHandler bridgeHandler = getWorxLandroidBridgeHandler();
        if (ThingStatus.ONLINE.equals(bridgeStatusInfo.getStatus()) && bridgeHandler != null) {
            initialize();
            // awsTopic = new AWSTopic(awsTopic.getTopic(), this);
            awsTopic.ifPresent(topic -> bridgeHandler.subcribeTopic(topic));
        }
        super.bridgeStatusChanged(bridgeStatusInfo);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            return;
        }

        if (getThing().getStatus() != ThingStatus.ONLINE) {
            logger.error("handleCommand mower: {} is offline!", getThing().getUID());
            return;
        }

        WorxLandroidBridgeHandler bridgeHandler = getWorxLandroidBridgeHandler();
        if (bridgeHandler == null) {
            logger.error("no bridgeHandler");
            return;
        }

        mower.ifPresent(theMower -> {
            String groupId = channelUID.getGroupId();
            String channelId = channelUID.getIdWithoutGroup();
            if (GROUP_MULTI_ZONES.equals(groupId)) {
                handleMultiZonesCommand(theMower, channelId, command);
            } else if (GROUP_SCHEDULE.equals(groupId)) {
                handleScheduleCommand(theMower, channelId, Integer.parseInt(command.toString()));
            } else if (GROUP_ONE_TIME.equals(groupId)) {
                handleOneTimeSchedule(theMower, channelId, command);
            } else if (GROUP_COMMON.equals(groupId)) {
                handleCommonGroup(theMower, channelId, command);
            } else if (groupId != null && groupId.contains("day")) {
                setScheduledDays(theMower, groupId, channelId, command);
                sendCommand(theMower,
                        theMower.scheduler2Supported()
                                ? new ScheduleDaysCommand(theMower.getTimeExtension(), theMower.getSheduleArray1(),
                                        theMower.getSheduleArray2())
                                : new ScheduleDaysCommand(theMower.getTimeExtension(), theMower.getSheduleArray1()));

            } else if (CHANNEL_DELAY.equals(channelId)) {
                int delaySec = commandToInt(command, Units.SECOND);
                sendCommand(theMower, new SetRainDelay(delaySec));
            } else {
                logger.debug("command for ChannelUID not supported: {}", channelUID.getAsString());
            }
        });
    }

    private void handleCommonGroup(Mower theMower, String channel, Command command) {
        if (CHANNEL_ACTION.equals(channel)) {
            WorxLandroidActionCodes actionCode = WorxLandroidActionCodes.valueOf(command.toString());
            sendCommand(theMower, new MowerCommand(actionCode));
        } else if (CHANNEL_POLL.equals(channel)) {
            sendCommand(theMower, AWSMessage.EMPTY_PAYLOAD);
            updateState(CHANNEL_POLL, OnOffType.OFF);
        } else if (CHANNEL_LOCK.equals(channel)) {
            WorxLandroidActionCodes lockCode = OnOffType.ON.equals(command) ? WorxLandroidActionCodes.LOCK
                    : WorxLandroidActionCodes.UNLOCK;
            sendCommand(theMower, new MowerCommand(lockCode));
        } else if (CHANNEL_ENABLE.equals(channel)) {
            theMower.setEnable(OnOffType.ON.equals(command));
            sendCommand(theMower,
                    theMower.scheduler2Supported()
                            ? new ScheduleDaysCommand(theMower.getTimeExtension(), theMower.getSheduleArray1(),
                                    theMower.getSheduleArray2())
                            : new ScheduleDaysCommand(theMower.getTimeExtension(), theMower.getSheduleArray1()));
        } else {
            logger.warn("No action identified for command {} on channel {}", command, channel);
        }
    }

    private void handleOneTimeSchedule(Mower theMower, String channel, Command command) {
        if (CHANNEL_DURATION.equals(channel)) {
            sendCommand(theMower, new OneTimeCommand(0, Integer.parseInt(command.toString())));
        } else if (CHANNEL_EDGECUT.equals(channel)) {
            sendCommand(theMower, new OneTimeCommand(OnOffType.ON.equals(command) ? 1 : 0, 0));
        } else {
            logger.warn("No action identified for command {} on channel {}", command, channel);
        }
    }

    private void handleScheduleCommand(Mower theMower, String channel, int command) {
        if (CHANNEL_MODE.equals(channel)) {
            sendCommand(theMower, new ScheduleCommand(command));
        } else if (CHANNEL_TIME_EXTENSION.equals(channel)) {
            theMower.setTimeExtension(command);
            sendCommand(theMower,
                    theMower.scheduler2Supported()
                            ? new ScheduleDaysCommand(theMower.getTimeExtension(), theMower.getSheduleArray1(),
                                    theMower.getSheduleArray2())
                            : new ScheduleDaysCommand(theMower.getTimeExtension(), theMower.getSheduleArray1()));
        } else {
            logger.warn("No action identified for command {} on channel {}", command, channel);
        }
    }

    private void handleMultiZonesCommand(Mower theMower, String channel, Command command) {
        if (CHANNEL_ENABLE.equals(channel)) {
            theMower.setMultiZoneEnable(OnOffType.ON.equals(command));
            sendCommand(theMower, new ZoneMeterCommand(theMower.getZoneMeters()));
        } else if (CHANNEL_LAST_ZONE.equals(channel)) {
            if (!WorxLandroidStatusCodes.HOME.equals(theMower.getStatus())) {
                logger.warn("Cannot start zone because mower must be at HOME!");
                return;
            }
            zoneMeterRestoreValues = theMower.getZoneMeters();
            restoreZoneMeter = true;

            int meter = theMower.getZoneMeter(Integer.parseInt(command.toString()));
            for (int zoneIndex = 0; zoneIndex < 4; zoneIndex++) {
                theMower.setZoneMeter(zoneIndex, meter);
            }
            sendCommand(theMower, new ZoneMeterCommand(theMower.getZoneMeters()));
            scheduler.schedule(() -> sendCommand(theMower, new MowerCommand(WorxLandroidActionCodes.START)), 2000,
                    TimeUnit.MILLISECONDS);
        } else {
            String[] names = channel.split("-");
            int index = Integer.valueOf(names[1]);

            if (CHANNEL_PREFIX_ZONE.equals(names[0])) {
                int meterValue = commandToInt(command, SIUnits.METRE);
                theMower.setZoneMeter(index - 1, meterValue);
                sendCommand(theMower, new ZoneMeterCommand(theMower.getZoneMeters()));

            } else if (CHANNEL_PREFIX_ALLOCATION.equals(names[0])) {
                theMower.setAllocation(index, Integer.parseInt(command.toString()));
                sendCommand(theMower, new ZoneMeterAlloc(theMower.getAllocations()));
            } else {
                logger.warn("No action identified for command {} on channel {}", command, channel);
            }
        }
    }

    private int commandToInt(Command command, Unit<?> targetUnit) {
        if (command instanceof QuantityType<?> qtty) {
            QuantityType<?> inTarget = qtty.toUnit(targetUnit);
            if (inTarget != null) {
                return inTarget.intValue();
            }
        }
        return Integer.parseInt(command.toString());
    }

    /**
     * Set scheduled days
     *
     * @param theMower
     *
     * @param scDaysIndex 1 or 2
     * @param channelUID
     * @param command
     */
    private void setScheduledDays(Mower theMower, String groupId, String channelId, Command command) {
        int scDaysSlot = groupId.endsWith("2") ? 2 : 1;
        WorxLandroidDayCodes dayCodeUpdated = WorxLandroidDayCodes.valueOf(groupId.replace("2", ""));

        ScheduledDay scheduledDayUpdated = theMower.getScheduledDay(scDaysSlot, dayCodeUpdated);
        if (scheduledDayUpdated == null) {
            return;
        }

        if (CHANNEL_ENABLE.equals(channelId)) {
            scheduledDayUpdated.setEnable(OnOffType.ON.equals(command));
        } else if (CHANNEL_TIME.equals(channelId)) {
            int hour = -1;
            int minute = -1;

            if (command instanceof DateTimeType dateTime) {
                ZonedDateTime zdt = dateTime.getZonedDateTime();
                hour = zdt.getHour();
                minute = zdt.getMinute();
            } else if (command instanceof StringType stringType) {
                String[] elements = stringType.toString().split(":");
                try {
                    hour = Integer.valueOf(elements[0]);
                    minute = Integer.valueOf(elements[1]);
                } catch (NumberFormatException ignore) {
                }
            }

            if (minute >= 0 && hour >= 0) {
                scheduledDayUpdated.setHours(hour);
                scheduledDayUpdated.setMinutes(minute);
            } else {
                logger.warn("Incorrect command {} on channel {}:{} ", command, groupId, channelId);
            }
        } else if (CHANNEL_DURATION.equals(channelId)) {
            scheduledDayUpdated.setDuration(Integer.parseInt(command.toString()));
        } else if (CHANNEL_EDGECUT.equals(channelId)) {
            scheduledDayUpdated.setEdgecut(OnOffType.ON.equals(command));
        }
    }

    private void sendCommand(Mower theMower, Object command) {
        logger.debug("send command: {}", command);

        WorxLandroidBridgeHandler bridgeHandler = getWorxLandroidBridgeHandler();
        if (bridgeHandler != null) {
            bridgeHandler.publishMessage(theMower.getMqttCommandIn(), command);
        }
    }

    @Override
    public void processMessage(AWSMessage message) {
        updateStatus(ThingStatus.ONLINE);

        try {
            Payload payload = deserializer.deserialize(Payload.class, message.payload());
            mower.ifPresent(theMower -> processStatusMessage(theMower, payload));
        } catch (WebApiException e) {
            logger.warn("Error processing incoming AWS message : {}", e.getMessage());
        }
    }

    /**
     * Update states for data values
     *
     * @param theMower
     *
     * @param dat
     */
    private void updateStateDat(Mower theMower, Dat dat) {

        if (dat.battery instanceof Battery battery) {
            updateChannelQuantity(GROUP_BATTERY, CHANNEL_TEMPERATURE, battery.temp != -1 ? battery.temp : null,
                    SIUnits.CELSIUS);
            updateChannelQuantity(GROUP_BATTERY, CHANNEL_VOLTAGE, battery.voltage != -1 ? battery.voltage : null,
                    Units.VOLT);
            updateChannelDecimal(GROUP_BATTERY, CHANNEL_LEVEL, battery.level);
            updateChannelDecimal(GROUP_BATTERY, CHANNEL_CHARGE_CYCLE, battery.chargeCycle);

            long batteryChargeCyclesCurrent = battery.chargeCycle;
            String batteryChargeCyclesReset = getThing().getProperties().get("battery_charge_cycles_reset");
            if (batteryChargeCyclesReset != null && !batteryChargeCyclesReset.isEmpty()) {
                batteryChargeCyclesCurrent = battery.chargeCycle - Long.valueOf(batteryChargeCyclesReset);
            }
            updateChannelDecimal(GROUP_BATTERY, CHANNEL_CHARGE_CYCLE_CURRENT, batteryChargeCyclesCurrent);
            updateChannelOnOff(GROUP_BATTERY, CHANNEL_CHARGING, battery.charging);
        }

        updateChannelQuantity(GROUP_ORIENTATION, CHANNEL_PITCH, dat.dataMotionProcessor[0], Units.DEGREE_ANGLE);
        updateChannelQuantity(GROUP_ORIENTATION, CHANNEL_ROLL, dat.dataMotionProcessor[1], Units.DEGREE_ANGLE);
        updateChannelQuantity(GROUP_ORIENTATION, CHANNEL_YAW, dat.dataMotionProcessor[2], Units.DEGREE_ANGLE);

        // dat/st
        if (dat.st != null) {
            St st = dat.st;
            // dat/st/b -> totalBladeTime
            if (st.totalBladeTime != -1) {
                updateChannelQuantity(GROUP_METRICS, CHANNEL_TOTAL_BLADE_TIME, st.totalBladeTime, Units.MINUTE);

                long bladeTimeCurrent = st.totalBladeTime;
                String bladeWorkTimeReset = getThing().getProperties().get("blade_work_time_reset");
                if (bladeWorkTimeReset != null && !bladeWorkTimeReset.isEmpty()) {
                    bladeTimeCurrent = st.totalBladeTime - Long.valueOf(bladeWorkTimeReset);
                }
                updateChannelQuantity(GROUP_METRICS, CHANNEL_CURRENT_BLADE_TIME, bladeTimeCurrent, Units.MINUTE);
            }

            updateChannelQuantity(GROUP_METRICS, CHANNEL_TOTAL_DISTANCE,
                    st.totalDistance != -1 ? st.totalDistance : null, SIUnits.METRE);
            updateChannelQuantity(GROUP_METRICS, CHANNEL_TOTAL_TIME, st.totalTime != -1 ? st.totalTime : null,
                    Units.MINUTE);
            // TODO dat/st/bl -> ?
        }
        // dat/ls -> statusCode
        theMower.setStatus(dat.statusCode);
        updateChannelString(GROUP_COMMON, CHANNEL_STATUS_CODE, dat.statusCode.name());
        updateChannelString(GROUP_COMMON, CHANNEL_ERROR_CODE, dat.errorCode.name());

        // restore
        if (restoreZoneMeter) {
            if (dat.statusCode != WorxLandroidStatusCodes.HOME
                    && dat.statusCode != WorxLandroidStatusCodes.START_SEQUENCE
                    && dat.statusCode != WorxLandroidStatusCodes.LEAVING_HOME
                    && dat.statusCode != WorxLandroidStatusCodes.SEARCHING_ZONE) {
                restoreZoneMeter = false;
                theMower.setZoneMeters(zoneMeterRestoreValues);
                sendCommand(theMower, new ZoneMeterCommand(theMower.getZoneMeters()));
            }
        }

        updateChannelDecimal(GROUP_MULTI_ZONES, CHANNEL_LAST_ZONE, theMower.getAllocation(dat.lastZone));

        int rssi = dat.wifiQuality;
        updateChannelDecimal(GROUP_WIFI, CHANNEL_WIFI_QUALITY, rssi <= 0 ? toQoS(rssi) : null);
        updateChannelQuantity(GROUP_WIFI, CHANNEL_RSSI,
                rssi <= 0 ? new QuantityType<>(rssi, Units.DECIBEL_MILLIWATTS) : null);

        if (theMower.lockSupported()) {
            updateChannelOnOff(GROUP_COMMON, CHANNEL_LOCK, dat.isLocked());
        }

        if (theMower.rainDelayStartSupported() && dat.rain instanceof Rain rain) {
            updateChannelOnOff(GROUP_RAIN, CHANNEL_RAIN_STATE, rain.raining);
            updateChannelDecimal(GROUP_RAIN, CHANNEL_RAIN_COUNTER, rain.counter);
        }
    }

    /**
     * Update states for cfg values
     *
     * @param theMower
     *
     * @param cfg
     * @param zoneId
     */
    private void updateStateCfg(Mower theMower, Cfg cfg) {
        updateChannelDateTime(GROUP_CONFIG, CHANNEL_TIMESTAMP, cfg.getDateTime(theMower.getZoneId()));

        if (cfg.sc instanceof Schedule sc) {
            if (theMower.oneTimeSchedulerSupported()) {
                updateChannelDecimal(GROUP_SCHEDULE, CHANNEL_MODE, sc.scheduleMode != -1 ? sc.scheduleMode : null);

                if (sc.ots instanceof Ots ots) {
                    updateChannelOnOff(GROUP_ONE_TIME, CHANNEL_EDGECUT, ots.getEdgeCut());
                    updateChannelDecimal(GROUP_ONE_TIME, CHANNEL_DURATION, ots.duration != -1 ? ots.duration : null);
                }
            }

            if (sc.timeExtension != -1) {
                theMower.setTimeExtension(sc.timeExtension);
                updateChannelQuantity(GROUP_SCHEDULE, CHANNEL_TIME_EXTENSION, sc.timeExtension, Units.PERCENT);
                updateChannelOnOff(GROUP_COMMON, CHANNEL_ENABLE, theMower.isEnable());
            }

            if (sc.d != null) {
                updateStateCfgScDays(theMower, 1, sc.d);
                if (sc.dd != null) {
                    updateStateCfgScDays(theMower, 2, sc.dd);
                }
            }

        }

        updateChannelDecimal(GROUP_CONFIG, CHANNEL_COMMAND, cfg.cmd != -1 ? cfg.cmd : null);

        if (theMower.multiZoneSupported()) {
            for (int zoneIndex = 0; zoneIndex < cfg.multiZones.size(); zoneIndex++) {
                int meters = cfg.multiZones.get(zoneIndex);
                theMower.setZoneMeter(zoneIndex, meters);
                updateChannelQuantity(GROUP_MULTI_ZONES, CHANNEL_PREFIX_ZONE.formatted(zoneIndex + 1), meters,
                        SIUnits.METRE);
            }

            // multizone enable is initialized and set by zone meters
            updateChannelOnOff(GROUP_MULTI_ZONES, CHANNEL_ENABLE, theMower.isMultiZoneEnable());

            for (int allocationIndex = 0; allocationIndex < cfg.multizoneAllocations.size(); allocationIndex++) {
                theMower.setAllocation(allocationIndex, cfg.multizoneAllocations.get(allocationIndex));
                updateChannelDecimal(GROUP_MULTI_ZONES, CHANNEL_PREFIX_ALLOCATION.formatted(allocationIndex),
                        cfg.multizoneAllocations.get(allocationIndex));
            }
        }

        updateChannelQuantity(GROUP_RAIN, CHANNEL_DELAY,
                theMower.rainDelaySupported() && cfg.rainDelay != -1 ? cfg.rainDelay : null, Units.MINUTE);
    }

    /**
     * @param theMower
     * @param scDSlot scheduled day slot
     * @param d scheduled day JSON
     */
    private void updateStateCfgScDays(Mower theMower, int scDSlot, List<List<String>> d) {
        List<ZonedDateTime> nextStarts = new ArrayList<>();
        List<ZonedDateTime> nextEnds = new ArrayList<>();

        for (WorxLandroidDayCodes dayCode : WorxLandroidDayCodes.values()) {
            List<String> shedule = d.get(dayCode.code);

            ScheduledDay scheduledDay = theMower.getScheduledDay(scDSlot, dayCode);
            if (scheduledDay == null) {
                return;
            }

            String groupName = "%s%s".formatted(dayCode.getDescription().toLowerCase(),
                    scDSlot == 1 ? "" : String.valueOf(scDSlot));
            String time[] = shedule.get(0).split(":");

            scheduledDay.setHours(Integer.parseInt(time[0]));
            scheduledDay.setMinutes(Integer.parseInt(time[1]));
            scheduledDay.setDuration(Integer.valueOf(shedule.get(1)));
            scheduledDay.setEdgecut(Integer.valueOf(shedule.get(2)) == 1);

            updateChannelOnOff(groupName, CHANNEL_ENABLE, scheduledDay.isEnable());
            updateChannelOnOff(groupName, CHANNEL_EDGECUT, scheduledDay.isEdgecut());
            updateChannelQuantity(groupName, CHANNEL_DURATION, scheduledDay.getDuration(), Units.MINUTE);

            if (scheduledDay.isEnable()) {
                ZonedDateTime scheduleStart = ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES)
                        .withHour(scheduledDay.getHour()).withMinute(scheduledDay.getMinutes());
                scheduleStart = ZonedDateTime.from(dayCode.dayOfWeek.adjustInto(scheduleStart));
                updateChannelDateTime(groupName, CHANNEL_TIME, scheduleStart);
                ZonedDateTime scheduleEnd = scheduleStart.plusMinutes(scheduledDay.getDuration());
                if (scheduleStart.isBefore(ZonedDateTime.now())) {
                    scheduleStart = scheduleStart.plusDays(7);
                }
                if (scheduleEnd.isBefore(ZonedDateTime.now())) {
                    scheduleEnd = scheduleEnd.plusDays(7);
                }

                nextStarts.add(scheduleStart);
                nextEnds.add(scheduleEnd);
            }
        }
        if (!nextStarts.isEmpty()) {
            Collections.sort(nextStarts);
            Collections.sort(nextEnds);
            updateChannelDateTime(GROUP_SCHEDULE, CHANNEL_START, nextStarts.get(0));
            updateChannelDateTime(GROUP_SCHEDULE, CHANNEL_STOP, nextEnds.get(0));
        }
    }

    private void updateIfActive(String group, String channelId, State state) {
        ChannelUID id = new ChannelUID(getThing().getUID(), group, channelId);
        if (isLinked(id)) {
            updateState(id, state);
        }
    }

    protected void updateChannelDateTimeState(String group, String channelId, @Nullable ZonedDateTime timestamp) {
        updateIfActive(group, channelId, timestamp == null ? UnDefType.NULL : new DateTimeType(timestamp));
    }

    protected void updateChannelQuantity(String group, String channelId, @Nullable Number d, Unit<?> unit) {
        if (d == null) {
            updateIfActive(group, channelId, UnDefType.NULL);
        } else {
            updateChannelQuantity(group, channelId, new QuantityType<>(d, unit));
        }
    }

    protected void updateChannelQuantity(String group, String channelId, @Nullable QuantityType<?> quantity) {
        updateIfActive(group, channelId, quantity != null ? quantity : UnDefType.NULL);
    }

    protected void updateChannelOnOff(String group, String channelId, boolean value) {
        updateIfActive(group, channelId, OnOffType.from(value));
    }

    protected void updateChannelDateTime(String group, String channelId, @Nullable ZonedDateTime timestamp) {
        updateIfActive(group, channelId, timestamp == null ? UnDefType.NULL : new DateTimeType(timestamp));
    }

    protected void updateChannelString(String group, String channelId, @Nullable String value) {
        updateIfActive(group, channelId, value == null || value.isEmpty() ? UnDefType.NULL : new StringType(value));
    }

    protected void updateChannelDecimal(String group, String channelId, @Nullable Number value) {
        updateIfActive(group, channelId, value == null || value.equals(-1) ? UnDefType.NULL : new DecimalType(value));
    }

    private int toQoS(int rssi) {
        return rssi > -50 ? 4 : rssi > -60 ? 3 : rssi > -70 ? 2 : rssi > -85 ? 1 : 0;
    }
}
