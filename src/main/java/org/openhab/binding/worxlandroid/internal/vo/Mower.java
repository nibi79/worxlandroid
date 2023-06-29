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
package org.openhab.binding.worxlandroid.internal.vo;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.worxlandroid.internal.api.dto.ProductItemStatus;
import org.openhab.binding.worxlandroid.internal.codes.WorxLandroidDayCodes;
import org.openhab.binding.worxlandroid.internal.codes.WorxLandroidStatusCodes;

/**
 * {@link Mower}
 *
 * @author Nils - Initial contribution
 */
@NonNullByDefault
public class Mower {
    private static final int TIME_EXTENSION_DISABLE = -100;
    private static final int[] MULTI_ZONE_METER_DISABLE = { 0, 0, 0, 0 };
    private static final int[] MULTI_ZONE_METER_ENABLE = { 1, 0, 0, 0 };

    private final String serialNumber;
    private final String mqttCommandIn;
    private final String mqttCommandOut;
    private final boolean lockSupported;
    private final boolean rainDelaySupported;
    private final double firmwareVersion;
    private final boolean rainDelayStartSupported;
    private final boolean multiZoneSupported;
    private final boolean oneTimeSchedulerSupported;
    private final int multiZoneCount;
    private final ZoneId zoneId;

    private final int[] zoneMeter;
    private final int[] zoneMeterRestore;

    private final int[] allocations = new int[10];
    private final Map<WorxLandroidDayCodes, ScheduledDay> scheduledDays = new HashMap<>(7);
    private final Optional<Map<WorxLandroidDayCodes, ScheduledDay>> scheduledDays2;

    private boolean enable;
    private WorxLandroidStatusCodes status = WorxLandroidStatusCodes.UNKNOWN;
    private int timeExtension;
    private int timeExtensionRestore = 0;

    private boolean multiZoneEnable;

    // multizone meter

    // multizone allocations

    public Mower(ProductItemStatus product) {
        this.serialNumber = product.serialNumber;
        this.mqttCommandIn = product.mqttTopics.commandIn;
        this.mqttCommandOut = product.mqttTopics.commandOut;
        this.lockSupported = product.features.lock;
        this.firmwareVersion = product.firmwareVersion;
        this.rainDelaySupported = product.features.rainDelay;
        this.rainDelayStartSupported = product.features.rainDelayStart < firmwareVersion;
        this.oneTimeSchedulerSupported = product.features.oneTimeScheduler < firmwareVersion;
        this.multiZoneSupported = product.features.multiZone;
        this.multiZoneCount = multiZoneSupported ? product.features.multiZoneZones : 0;
        this.zoneMeter = new int[multiZoneCount];
        this.zoneMeterRestore = new int[multiZoneCount];
        this.zoneId = product.timeZone;

        this.scheduledDays2 = Optional
                .ofNullable(product.features.schedulerTwoSlots < firmwareVersion ? new HashMap<>(7) : null);
        // initialize scheduledDay map for each day
        for (WorxLandroidDayCodes dayCode : WorxLandroidDayCodes.values()) {
            scheduledDays.put(dayCode, new ScheduledDay());
            scheduledDays2.ifPresent(sched -> sched.put(dayCode, new ScheduledDay()));
        }
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public int getTimeExtension() {
        return timeExtension;
    }

    /**
     * timeExtension = -100 disables mowing (enable=false).
     * timeExtension > -100 enables mowing (enable=true).
     *
     * @param timeExtension
     */
    public void setTimeExtension(int timeExtension) {
        if (timeExtension == TIME_EXTENSION_DISABLE) {
            storeTimeExtension();
            this.enable = false;
        } else {
            this.enable = true;
        }

        this.timeExtension = timeExtension;
    }

    public boolean lockSupported() {
        return lockSupported;
    }

    public boolean rainDelaySupported() {
        return rainDelaySupported;
    }

    public boolean rainDelayStartSupported() {
        return rainDelayStartSupported;
    }

    public boolean multiZoneSupported() {
        return multiZoneSupported;
    }

    public boolean scheduler2Supported() {
        return scheduledDays2.isPresent();
    }

    public boolean oneTimeSchedulerSupported() {
        return oneTimeSchedulerSupported;
    }

    public @Nullable ScheduledDay getScheduledDay(int scDSlot, WorxLandroidDayCodes dayCode) {
        return scDSlot == 1 ? scheduledDays.get(dayCode) : scheduledDays2.map(sched -> sched.get(dayCode)).orElse(null);
    }

    @SuppressWarnings("null")
    private Object[] getScheduleArray(Map<WorxLandroidDayCodes, ScheduledDay> schedule) {
        Object[] result = new Object[7];
        for (WorxLandroidDayCodes dayCode : WorxLandroidDayCodes.values()) {
            result[dayCode.code] = schedule.get(dayCode).getArray();
        }
        return result;
    }

    public Object[] getSheduleArray1() {
        return getScheduleArray(scheduledDays);
    }

    public Object[] getSheduleArray2() {
        return scheduledDays2.map(schedule -> getScheduleArray(schedule)).orElse(new Object[] {});
    }

    public void put(WorxLandroidDayCodes dayCode, ScheduledDay scheduledDay) {
        scheduledDays.put(dayCode, scheduledDay);
    }

    public void putScheduledDay2(WorxLandroidDayCodes dayCode, ScheduledDay scheduledDay) {
        scheduledDays2.ifPresent(sched -> sched.put(dayCode, scheduledDay));
    }

    public boolean isMultiZoneEnable() {
        return multiZoneEnable;
    }

    public void setMultiZoneEnable(boolean multiZoneEnable) {
        this.multiZoneEnable = multiZoneEnable;

        if (multiZoneEnable && isZoneMeterDisabled()) {
            restoreZoneMeter();
            if (isZoneMeterDisabled()) {
                System.arraycopy(MULTI_ZONE_METER_ENABLE, 0, zoneMeter, 0, zoneMeter.length);
            }
        } else {
            storeZoneMeter();
            System.arraycopy(MULTI_ZONE_METER_DISABLE, 0, zoneMeter, 0, zoneMeter.length);
        }
    }

    public int getZoneMeter(int zoneIndex) {
        return zoneMeter[zoneIndex];
    }

    public int[] getZoneMeters() {
        return Arrays.copyOf(zoneMeter, zoneMeter.length);
    }

    public void setZoneMeters(int[] zoneMeterInput) {
        System.arraycopy(zoneMeterInput, 0, zoneMeter, 0, zoneMeter.length);
    }

    public void setZoneMeter(int zoneIndex, int meter) {
        zoneMeter[zoneIndex] = meter;
        this.multiZoneEnable = !isZoneMeterDisabled();
    }

    public int getAllocation(int allocationIndex) {
        return allocations[allocationIndex];
    }

    public int[] getAllocations() {
        return Arrays.copyOf(allocations, allocations.length);
    }

    public void setAllocation(int allocationIndex, int zoneIndex) {
        allocations[allocationIndex] = zoneIndex;
    }

    public boolean isEnable() {
        return enable;
    }

    /**
     * Enable/Disables mowing using timeExtension.
     * disable: timeExtension = -100
     * enable: timeExtension > -100
     *
     */
    public void setEnable(boolean enable) {
        this.enable = enable;

        if (enable && timeExtension == TIME_EXTENSION_DISABLE) {
            restoreTimeExtension();
        } else {
            storeTimeExtension();
            this.timeExtension = TIME_EXTENSION_DISABLE;
        }
    }

    /**
     * Stores timeExtension to timeExtensionRestore for restore,
     */
    private void storeTimeExtension() {
        if (this.timeExtension > TIME_EXTENSION_DISABLE) {
            this.timeExtensionRestore = this.timeExtension;
        }
    }

    /**
     * Restores timeExtension from timeExtensionRestore.
     */
    private void restoreTimeExtension() {
        this.timeExtension = this.timeExtensionRestore;
    }

    /**
     * Stores zoneMeter to zoneMeterRestore for restore,
     */
    private void storeZoneMeter() {
        if (!isZoneMeterDisabled()) {
            System.arraycopy(zoneMeter, 0, zoneMeterRestore, 0, zoneMeter.length);
        }
    }

    /**
     * Restores zoneMeter from zoneMeterRestore.
     */
    private void restoreZoneMeter() {
        System.arraycopy(zoneMeterRestore, 0, zoneMeter, 0, zoneMeter.length);
    }

    /**
     * @return false if less than 2 meters are 0
     */
    private boolean isZoneMeterDisabled() {
        for (int i = 0; i < zoneMeter.length; i++) {
            if (zoneMeter[i] != 0) {
                return false;
            }
        }
        return true;
    }

    public WorxLandroidStatusCodes getStatus() {
        return status;
    }

    public void setStatus(WorxLandroidStatusCodes status) {
        this.status = status;
    }

    public int getMultiZoneCount() {
        return multiZoneCount;
    }

    public String getMqttCommandIn() {
        return mqttCommandIn;
    }

    public String getMqttCommandOut() {
        return mqttCommandOut;
    }

    public ZoneId getZoneId() {
        return zoneId;
    }

}
