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

    private final ProductItemStatus product;

    private final int[] zoneMeter;
    private final int[] zoneMeterRestore;
    private final int[] allocations = new int[10];
    private final Map<WorxLandroidDayCodes, ScheduledDay> scheduledDays = new HashMap<>(7);
    private final Map<WorxLandroidDayCodes, ScheduledDay> scheduledDays2 = new HashMap<>(7);

    private boolean enable;
    private boolean multiZoneEnable;
    private int timeExtension;
    private int timeExtensionRestore = 0;
    private WorxLandroidStatusCodes status = WorxLandroidStatusCodes.UNKNOWN;

    public Mower(ProductItemStatus product) {
        this.product = product;
        this.zoneMeter = new int[getMultiZoneCount()];
        this.zoneMeterRestore = new int[getMultiZoneCount()];

        // initialize scheduledDay map for each day
        for (WorxLandroidDayCodes dayCode : WorxLandroidDayCodes.values()) {
            scheduledDays.put(dayCode, new ScheduledDay());
            scheduledDays2.put(dayCode, new ScheduledDay());
        }
        if (product.features.schedulerTwoSlots < getFirmwareVersion()) {
            scheduledDays2.clear();
        }
    }

    public String getSerialNumber() {
        return product.serialNumber;
    }

    public int getTimeExtension() {
        return timeExtension;
    }

    public double getFirmwareVersion() {
        return product.firmwareVersion;
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
        return product.features.lock;
    }

    public boolean rainDelaySupported() {
        return product.features.rainDelay;
    }

    public boolean rainDelayStartSupported() {
        return product.features.rainDelayStart < getFirmwareVersion();
    }

    public boolean multiZoneSupported() {
        return product.features.multiZone;
    }

    public boolean scheduler2Supported() {
        return !scheduledDays2.isEmpty();
    }

    public boolean oneTimeSchedulerSupported() {
        return product.features.oneTimeScheduler < getFirmwareVersion();
    }

    public @Nullable ScheduledDay getScheduledDay(int scDSlot, WorxLandroidDayCodes dayCode) {
        return scDSlot == 1 ? scheduledDays.get(dayCode) : scheduler2Supported() ? scheduledDays2.get(dayCode) : null;
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
        return scheduler2Supported() ? getScheduleArray(scheduledDays2) : new Object[] {};
    }

    public void put(WorxLandroidDayCodes dayCode, ScheduledDay scheduledDay) {
        scheduledDays.put(dayCode, scheduledDay);
    }

    public void putScheduledDay2(WorxLandroidDayCodes dayCode, ScheduledDay scheduledDay) {
        if (scheduler2Supported()) {
            scheduledDays2.put(dayCode, scheduledDay);
        }
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
        return multiZoneSupported() ? product.features.multiZoneZones : 0;
    }

    public String getMqttCommandIn() {
        return product.mqttTopics.commandIn;
    }

    public String getMqttCommandOut() {
        return product.mqttTopics.commandOut;
    }

    public ZoneId getZoneId() {
        return product.timeZone;
    }

    public String getMacAddress() {
        return product.macAddress;
    }

    public String getId() {
        return product.id;
    }

    public String getLanguage() {
        return product.lastStatus.payload.cfg.lg;
    }
}
