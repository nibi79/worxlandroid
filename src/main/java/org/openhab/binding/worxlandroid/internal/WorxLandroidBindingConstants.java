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
package org.openhab.binding.worxlandroid.internal;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link WorxLandroidBindingConstants} class defines datCommon constants, which are
 * used across the whole binding.
 *
 * @author Nils - Initial contribution
 */
@NonNullByDefault
public class WorxLandroidBindingConstants {

    public static final String BINDING_ID = "worxlandroid";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, "worxlandroidBridge");
    public static final ThingTypeUID THING_TYPE_MOWER = new ThingTypeUID(BINDING_ID, "mower");

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Set.of(THING_TYPE_MOWER);

    // List channel ids

    // common
    public static final String CHANNELNAME_ONLINE = "common#online";
    public static final String CHANNELNAME_LAST_UPDATE_ONLINE_STATUS = "common#lastUpdateOnlineStatus";
    public static final String CHANNELNAME_POLL = "common#poll";
    public static final String CHANNELNAME_ACTION = "common#action";
    public static final String CHANNELNAME_ENABLE = "common#enable";
    public static final String CHANNELNAME_LOCK = "common#lock";

    // cfgCommon
    public static final String CHANNELNAME_ID = "cfgCommon#id";
    public static final String CHANNELNAME_SERIAL_NUMBER = "cfgCommon#serialNumber";
    public static final String CHANNELNAME_LANGUAGE = "cfgCommon#language";
    public static final String CHANNELNAME_DATETIME = "cfgCommon#lastUpdate";
    public static final String CHANNELNAME_COMMAND = "cfgCommon#command";
    public static final String CHANNELNAME_RAIN_DELAY = "cfgCommon#rainDelay";

    // cfgSc
    public static final String CHANNELNAME_SC_PREFIX = "cfgSc";
    public static final String CHANNELNAME_SC_TIME_EXTENSION = "cfgSc#scheduleTimeExtension";
    public static final String CHANNELNAME_SC_MODE = "cfgSc#scheduleMode";

    // cfgScXXXday
    public static final String CHANNELNAME_SC_ENABLE_SUFFIX = "enable";
    public static final String CHANNELNAME_SC_START_HOUR_SUFFIX = "scheduleStartHour";
    public static final String CHANNELNAME_SC_START_MINUTES_SUFFIX = "scheduleStartMinutes";
    public static final String CHANNELNAME_SC_DURATION_SUFFIX = "scheduleDuration";
    public static final String CHANNELNAME_SC_EDGECUT_SUFFIX = "scheduleEdgecut";

    // cfgOneTime
    public static final String CHANNELNAME_ONE_TIME_SC_EDGECUT = "cfgOneTimeSc#scheduleEdgecut";
    public static final String CHANNELNAME_ONE_TIME_SC_DURATION = "cfgOneTimeSc#scheduleDuration";

    // datCommon
    public static final String CHANNELNAME_MAC_ADRESS = "datCommon#macAdress";
    public static final String CHANNELNAME_FIRMWARE = "datCommon#firmware";
    public static final String CHANNELNAME_WIFI_QUALITY = "datCommon#wifiQuality";
    public static final String CHANNELNAME_LAST_ZONE = "datCommon#lastZone";
    public static final String CHANNELNAME_STATUS_CODE = "datCommon#statusCode";
    public static final String CHANNELNAME_STATUS_DESCRIPTION = "datCommon#statusDescription";
    public static final String CHANNELNAME_ERROR_CODE = "datCommon#errorCode";
    public static final String CHANNELNAME_ERROR_DESCRIPTION = "datCommon#errorDescription";

    // datBattery
    public static final String CHANNELNAME_BATTERY_TEMPERATURE = "datBattery#batteryTemperature";
    public static final String CHANNELNAME_BATTERY_VOLTAGE = "datBattery#batteryVoltage";
    public static final String CHANNELNAME_BATTERY_LEVEL = "datBattery#batteryLevel";
    public static final String CHANNELNAME_BATTERY_CHARGE_CYCLE = "datBattery#batteryChargeCycle";
    public static final String CHANNELNAME_BATTERY_CHARGE_CYCLE_CURRENT = "datBattery#batteryChargeCycleCurrent";
    public static final String CHANNELNAME_BATTERY_CHARGING = "datBattery#batteryCharging";

    // datDmp
    public static final String CHANNELNAME_PITCH = "datDmp#pitch";
    public static final String CHANNELNAME_ROLL = "datDmp#roll";
    public static final String CHANNELNAME_YAW = "datDmp#yaw";

    // datSt
    public static final String CHANNELNAME_TOTAL_BLADE_TIME = "datSt#totalBladeTime";
    public static final String CHANNELNAME_CURRENT_BLADE_TIME = "datSt#currentBladeTime";
    public static final String CHANNELNAME_TOTAL_DISTANCE = "datSt#totalDistance";
    public static final String CHANNELNAME_TOTAL_TIME = "datSt#totalTime";

    // datRain
    public static final String CHANNELNAME_RAIN_STATE = "datRain#state";
    public static final String CHANNELNAME_RAIN_COUNTER = "datRain#counter";

    //
    public static final String CHANNELNAME_PREFIX_ALLOCATION = "cfgMultiZones#allocation";
    public static final String CHANNELNAME_MULTIZONE_ENABLE = "cfgMultiZones#enable";
}
