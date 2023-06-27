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
    public static final ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, "bridge");
    public static final ThingTypeUID THING_TYPE_MOWER = new ThingTypeUID(BINDING_ID, "mower");

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Set.of(THING_TYPE_MOWER);

    // Channel group ids
    public static final String GROUP_COMMON = "common";
    public static final String GROUP_CONFIG = "config";
    public static final String GROUP_MULTI_ZONES = "multi-zones";
    public static final String GROUP_SCHEDULE = "schedule";
    public static final String GROUP_ONE_TIME = "one-time";
    public static final String GROUP_GENERAL = "general";
    public static final String GROUP_BATTERY = "battery";
    public static final String GROUP_ORIENTATION = "orientation";
    public static final String GROUP_METRICS = "metrics";
    public static final String GROUP_RAIN = "rain";

    // List channel ids
    // common
    public static final String CHANNEL_ONLINE = "online";
    public static final String CHANNEL_ONLINE_TIMESTAMP = "online-timestamp";
    public static final String CHANNEL_POLL = "poll";
    public static final String CHANNEL_ACTION = "action";
    public static final String CHANNEL_ENABLE = "enable";
    public static final String CHANNEL_LOCK = "lock";

    // cfgCommon
    public static final String CHANNEL_ID = "id";
    public static final String CHANNEL_LANGUAGE = "language";
    public static final String CHANNEL_TIMESTAMP = "timestamp";
    public static final String CHANNEL_COMMAND = "command";
    public static final String CHANNEL_RAIN_DELAY = "rain-delay";

    // cfgSc
    public static final String CHANNEL_TIME_EXTENSION = "time-extension";
    public static final String CHANNEL_MODE = "mode";

    // cfgScXXXday
    public static final String CHANNEL_START_HOUR = "hour";
    public static final String CHANNEL_START_MINUTES = "minutes";
    public static final String CHANNEL_DURATION = "duration";
    public static final String CHANNEL_EDGECUT = "edgecut";

    // datCommon
    public static final String CHANNEL_FIRMWARE = "firmware";
    public static final String CHANNEL_WIFI_QUALITY = "wifi-quality";
    public static final String CHANNEL_LAST_ZONE = "last-zone";
    public static final String CHANNEL_STATUS_CODE = "status";
    public static final String CHANNEL_ERROR_CODE = "error";

    // datBattery
    public static final String CHANNEL_TEMPERATURE = "temperature";
    public static final String CHANNEL_VOLTAGE = "voltage";
    public static final String CHANNEL_LEVEL = "level";
    public static final String CHANNEL_CHARGE_CYCLE = "carge-cycle";
    public static final String CHANNEL_CHARGE_CYCLE_CURRENT = "charge-cycle-current";
    public static final String CHANNEL_CHARGING = "charging";

    // datDmp
    public static final String CHANNEL_PITCH = "pitch";
    public static final String CHANNEL_ROLL = "roll";
    public static final String CHANNEL_YAW = "yaw";

    // datSt
    public static final String CHANNEL_TOTAL_BLADE_TIME = "total-blade-time";
    public static final String CHANNEL_CURRENT_BLADE_TIME = "current-blade-time";
    public static final String CHANNEL_TOTAL_DISTANCE = "total-distance";
    public static final String CHANNEL_TOTAL_TIME = "total-time";

    // datRain
    public static final String CHANNEL_RAIN_STATE = "state";
    public static final String CHANNEL_RAIN_COUNTER = "counter";

    //
    public static final String CHANNEL_PREFIX_ALLOCATION = "allocation";
}
