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

import java.util.Collections;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link WorxLandroidBindingConstants} class defines datCommon constants, which are
 * used across the whole binding.
 *
 * @author Nils - Initial contribution
 */
@NonNullByDefault
public class WorxLandroidBindingConstants {

    private static final String BINDING_ID = "worxlandroid";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, "worxlandroidBridge");
    public static final ThingTypeUID THING_TYPE_MOWER = new ThingTypeUID(BINDING_ID, "mower");

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.singleton(THING_TYPE_MOWER);

    // List of all Channel ids
    public static final String CHANNEL_ACTION = "datCommon#action";

    // channelnames cfg
    public static final String CHANNELNAME_ID = "cfgCommon#id";
    public static final String CHANNELNAME_LANGUAGE = "cfgCommon#language";
    public static final String CHANNELNAME_COMMAND = "cfgCommon#command";
    public static final String CHANNELNAME_RAIN_DELAY = "cfgCommon#rainDelay";
    public static final String CHANNELNAME_SERIAL_NUMBER = "cfgCommon#serialNumber";

    public static final String CHANNELNAME_MAC_ADRESS = "datCommon#macAdress";
    public static final String CHANNELNAME_FIRMWARE = "datCommon#firmware";
    public static final String CHANNELNAME_BATTERY_TEMPERATURE = "datBattery#batteryTemperature";
    public static final String CHANNELNAME_BATTERY_VOLTAGE = "datBattery#batteryVoltage";
    public static final String CHANNELNAME_BATTERY_LEVEL = "datBattery#batteryLevel";
    public static final String CHANNELNAME_BATTERY_CHARGE_CYCLE = "datBattery#batteryChargeCycle";
    public static final String CHANNELNAME_BATTERY_CHARGING = "datBattery#batteryCharging";
    public static final String CHANNELNAME_PITCH = "datDmp#pitch";
    public static final String CHANNELNAME_ROLL = "datDmp#roll";
    public static final String CHANNELNAME_YAW = "datDmp#yaw";
    public static final String CHANNELNAME_TOTAL_BLADE_TIME = "datSt#totalBladeTime";
    public static final String CHANNELNAME_TOTAL_DISTANCE = "datSt#totalDistance";
    public static final String CHANNELNAME_TOTAL_TIME = "datSt#totalTime";
    public static final String CHANNELNAME_STATUS_CODE = "datCommon#statusCode";
    public static final String CHANNELNAME_STATUS_DESCRIPTION = "datCommon#statusDescription";
    public static final String CHANNELNAME_ERROR_CODE = "datCommon#errorCode";
    public static final String CHANNELNAME_ERROR_DESCRIPTION = "datCommon#errorDescription";
    public static final String CHANNELNAME_WIFI_QUALITY = "datCommon#wifiQuality";
}
