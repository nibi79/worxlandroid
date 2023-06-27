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
package org.openhab.binding.worxlandroid.internal.codes;

import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link WorxLandroidErrorCodes} hosts error codes
 *
 * @author Nils - Initial contribution
 */
@NonNullByDefault
public enum WorxLandroidErrorCodes {
    UNKNOWN(-1),
    NO_ERR(0),
    TRAPPED(1),
    LIFTED(2),
    WIRE_MISSING(3),
    OUTSIDE_WIRE(4),
    RAINING(5),
    CLOSE_DOOR_TO_MOW(6),
    CLOSE_DOOR_TO_GO_HOME(7),
    BLADE_MOTOR_BLOCKED(8),
    WHEEL_MOTOR_BLOKED(9),
    TRAPPED_TIMEOUT(10),
    UPSIDE_DOWN(11),
    BATTERY_LOW(12),
    REVERSE_WIRE(13),
    CHARGE_ERROR(14),
    TIMEOUT_FINDING_HOME(15),
    MOWER_LOCKED(16),
    BATTERY_OVER_TEMPERATURE(17);

    public final int code;

    WorxLandroidErrorCodes(int code) {
        this.code = code;
    }

    public static WorxLandroidErrorCodes getByCode(int searched) {
        return Stream.of(WorxLandroidErrorCodes.values()).filter(e -> e.code == searched).findAny().orElse(UNKNOWN);
    }
}
