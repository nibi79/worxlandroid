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

    private String serialNumber;
    private boolean online;
    private boolean lockSupported;
    private boolean rainDelaySupported;
    private boolean multiZoneSupported;

    private int timeExtension;

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

    public void setTimeExtension(int timeExtension) {
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
}
