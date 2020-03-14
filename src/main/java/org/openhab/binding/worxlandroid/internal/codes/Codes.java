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

import java.util.stream.Stream;

/**
 * The {@link Codes} is an interface for error codes
 *
 * @author Nils - Initial contribution
 */
public interface Codes {

    /**
     * @return
     */
    public int getCode();

    /**
     * @return
     */
    public String getDescription();

    /**
     *
     * Lookup ErrorCode.
     *
     * @param e
     * @param code
     * @return
     */
    static <E extends Enum<E> & Codes> E lookup(Class<E> e, int code) {
        return Stream.of(e.getEnumConstants()).filter(x -> x.getCode() == code).findAny().orElse(null);
    }
}
