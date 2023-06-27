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
 * The {@link WorxLandroidStatusCodes} hosts status codes
 *
 * @author Nils - Initial contribution
 */
@NonNullByDefault
public enum WorxLandroidStatusCodes {
    UNKNOWN(-1),
    IDLE(0),
    HOME(1),
    START_SEQUENCE(2),
    LEAVING_HOME(3),
    FOLLOW_WIRE(4),
    SEARCHING_HOME(5),
    SEARCHING_WIRE(6),
    MOWING(7),
    LIFTED(8),
    TRAPPED(9),
    BLADE_BLOCKED(10),
    DEBUG(11),
    REMOTE_CONTROL(12),
    GOING_HOME(30),
    ZONE_TRAINING(31),
    BORDER_CUT(32),
    SEARCHING_ZONE(33),
    PAUSE(34);

    public final int code;

    WorxLandroidStatusCodes(int code) {
        this.code = code;
    }

    public static WorxLandroidStatusCodes getByCode(int searched) {
        return Stream.of(WorxLandroidStatusCodes.values()).filter(e -> e.code == searched).findAny().orElse(UNKNOWN);
    }
}
