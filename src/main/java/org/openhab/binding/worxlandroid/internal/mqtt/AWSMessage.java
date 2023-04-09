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
package org.openhab.binding.worxlandroid.internal.mqtt;

import org.openhab.binding.worxlandroid.internal.codes.WorxLandroidActionCodes;

/**
 * {@link AWSMessage} AWS message
 *
 * @author Nils - Initial contribution
 */
public class AWSMessage implements AWSMessageI {

    public static final String EMPTY_PAYLOAD = "{}";
    public static final String CMD_START = String.format("{\"cmd\":%d}", WorxLandroidActionCodes.START.getCode());

    private String topic;

    private String payload;

    /**
     * @param topic
     * @param qos
     * @param payload
     */
    public AWSMessage(String topic, String payload) {
        this.topic = topic;
        this.payload = payload;
    }

    @Override
    public String getTopic() {
        return topic;
    }

    @Override
    public String getPayload() {
        return payload;
    }
}
