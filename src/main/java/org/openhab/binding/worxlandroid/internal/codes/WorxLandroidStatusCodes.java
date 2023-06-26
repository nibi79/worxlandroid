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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link WorxLandroidStatusCodes} hosts status codes
 *
 * @author Nils - Initial contribution
 */
@NonNullByDefault
public enum WorxLandroidStatusCodes implements Codes {
    UNKNOWN(-1, "Unknown"),
    IDLE(0, "Idle"),
    HOME(1, "Home"),
    START_SEQUNCE(2, "Start sequence"),
    LEAVING_HOME(3, "Leaving home"),
    FOLLOW_WIRE(4, "Follow wire"),
    SEARCHING_HOME(5, "Searching home"),
    SEARCHING_WIRE(6, "Searching wire"),
    MOWING(7, "Mowing"),
    LIFTED(8, "Lifted"),
    TRAPPED(9, "Trapped"),
    BLADE_BLOCKED(10, "Blade blocked"),
    DEBUG(11, "Debug"),
    REMOTE_CONTROL(12, "Remote control"),
    GOING_HOME(30, "Going home"),
    ZONE_TRAINING(31, "Zone training'"),
    BORDER_CUT(32, "Border cut"),
    SEARCHING_ZONE(33, "Searching zone"),
    PAUSE(34, "Pause");

    private final int code;
    private final String description;

    WorxLandroidStatusCodes(int code, String description) {
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
    public static WorxLandroidStatusCodes getByCode(int code) {
        WorxLandroidStatusCodes result = Codes.lookup(WorxLandroidStatusCodes.class, code);
        return result != null ? result : WorxLandroidStatusCodes.UNKNOWN;
    }

    @Override
    public String toString() {
        return String.format("%s | StatusCode: %d - %s", name(), getCode(), getDescription());
    }
}
