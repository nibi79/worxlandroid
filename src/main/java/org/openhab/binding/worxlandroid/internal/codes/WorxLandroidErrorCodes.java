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
package org.openhab.binding.worxlandroid.internal.codes;

/**
 * The {@link WorxLandroidErrorCodes} hosts errorCodes for SYNO.API.Auth.
 *
 * @author Nils - Initial contribution
 */
public enum WorxLandroidErrorCodes implements Codes {

    NO_ERROR(0, "No error"),
    TRAPPED(1, "Trapped"),
    LIFTED(2, "Lifted"),
    WIRE_MISSING(3, "Wire missing"),
    OUTSIDE_WIRE(4, "Outside wire"),
    RAINING(5, "Raining"),
    CLOSE_DOOR_TO_MOW(6, "Close door to mow"),
    CLOSE_DOOR_TO_GO_HOME(7, "Close door to go home"),
    BLADE_MOTOR_BLOCKED(8, "Blade motor blocked"),
    WHEEL_MOTOR_BLOKED(9, "Wheel motor blocked"),
    TRAPPED_TIMEOUT(10, "Trapped timeout"),
    UPSIDE_DOWN(11, "Upside down"),
    BATTERY_LOW(12, "Battery low"),
    REVERSE_WIRE(13, "Reverse wire"),
    CHARGE_ERROR(14, "Charge error"),
    TIMEOUT_FINDING_HOME(15, "Timeout finding home"),
    MOWER_LOCKED(16, "Mower locked"),
    BATTERY_OVER_TEMPERATURE(17, "Battery over temperature");

    private final int code;
    private final String description;

    WorxLandroidErrorCodes(int code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public int getCode() {
        return this.code;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    /**
     *
     * @param code
     * @return
     */
    public static Codes getByCode(int code) {
        return Codes.lookup(WorxLandroidErrorCodes.class, code);
    }

    @Override
    public String toString() {
        return String.format("%s | ErrorCode: %d - %s", this.name(), this.getCode(), this.getDescription());
    }
}
