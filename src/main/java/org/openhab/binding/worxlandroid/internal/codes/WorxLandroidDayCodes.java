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
 * The {@link WorxLandroidDayCodes} hosts action codes
 *
 * @author Nils - Initial contribution
 */
public enum WorxLandroidDayCodes implements Codes {

    SUN(0, "Sunday"),
    MON(1, "Monday"),
    TUE(2, "Tuesday"),
    WED(3, "Wednesday"),
    THU(4, "Thursday"),
    FRI(5, "Friday"),
    SAT(6, "Saturday");

    private final int code;
    private final String description;

    WorxLandroidDayCodes(int code, String description) {
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

    @Override
    public String toString() {
        return String.format("%s | DayCode: %d - %s", this.name(), this.getCode(), this.getDescription());
    }

}
