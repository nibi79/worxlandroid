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
package org.openhab.binding.worxlandroid.internal.vo;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.worxlandroid.internal.codes.WorxLandroidDayCodes;

/**
 * {@link Mower}
 *
 * @author Nils - Initial contribution
 */
@NonNullByDefault
public class Mower {

    private static final int TIME_EXTENSION_DISABLE = -100;
    private boolean enable;

    private String serialNumber;
    private boolean online;

    private int timeExtension;
    private int timeExtensionRestore = 0;

    private boolean lockSupported;
    private boolean rainDelaySupported;
    private boolean multiZoneSupported;

    // multizone meter
    int[] zoneMeter = new int[4];

    // multizone allocations
    int[] allocations = new int[10];

    private Map<WorxLandroidDayCodes, ScheduledDay> scheduledDays = new LinkedHashMap<WorxLandroidDayCodes, ScheduledDay>();

    /**
     * @param serialNumber
     */
    public Mower(String serialNumber) {
        super();
        this.serialNumber = serialNumber;

        // initialize scheduledDay map for each day
        for (WorxLandroidDayCodes dayCode : WorxLandroidDayCodes.values()) {
            scheduledDays.put(dayCode, new ScheduledDay());
        }
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
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

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public boolean isLockSupported() {
        return lockSupported;
    }

    public void setLockSupported(boolean lockSupported) {
        this.lockSupported = lockSupported;
    }

    public boolean isRainDelaySupported() {
        return rainDelaySupported;
    }

    public void setRainDelaySupported(boolean rainDelaySupported) {
        this.rainDelaySupported = rainDelaySupported;
    }

    public boolean isMultiZoneSupported() {
        return multiZoneSupported;
    }

    public void setMultiZoneSupported(boolean multiZoneSupported) {
        this.multiZoneSupported = multiZoneSupported;
    }

    /**
     *
     * @param dayCode
     * @return
     */
    public ScheduledDay getScheduledDay(WorxLandroidDayCodes dayCode) {
        return scheduledDays.get(dayCode);
    }

    public void put(WorxLandroidDayCodes dayCode, ScheduledDay scheduledDay) {
        scheduledDays.put(dayCode, scheduledDay);

    }

    /**
     * @param zoneIndex
     * @return
     */
    public int getZoneMeter(int zoneIndex) {
        return zoneMeter[zoneIndex];
    }

    /**
     * @param zoneIndex
     * @param meter
     */
    public void setZoneMeter(int zoneIndex, int meter) {
        zoneMeter[zoneIndex] = meter;
    }

    /**
     * @param allocationIndex
     * @return
     */
    public int getAllocation(int allocationIndex) {
        return allocations[allocationIndex];
    }

    /**
     * @param allocationIndex
     * @param zoneIndex
     */
    public void setAllocation(int allocationIndex, int zoneIndex) {
        allocations[allocationIndex] = zoneIndex;
    }

    /**
     * @return
     */
    public boolean isEnable() {
        return enable;
    }

    /**
     * Enable/Disables mowing using timeExtension.
     * disable: timeExtension = -100
     * enable: timeExtension > -100
     *
     * @param enable
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
}
