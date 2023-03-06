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

/**
 * The {@link WorxLandroidActionCodes} hosts action codes
 *
 * @author Nils - Initial contribution
 */
public enum WorxLandroidActionCodes implements Codes {

    START(1, "start"),
    STOP(2, "stop"),
    HOME(3, "home"),
    ZONETRAINING(4, "zonetraining"),
    LOCK(5, "lock"),
    UNLOCK(6, "unlock");

    private final int code;
    private final String description;

    WorxLandroidActionCodes(int code, String description) {
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
        return String.format("%s | ActionCode: %d - %s", this.name(), this.getCode(), this.getDescription());
    }
}
