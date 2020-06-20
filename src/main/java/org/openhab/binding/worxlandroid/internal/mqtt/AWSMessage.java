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
package org.openhab.binding.worxlandroid.internal.mqtt;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotQos;

/**
 * {@link AWSMessage} AWS message
 *
 * @author Nils - Initial contribution
 */
@NonNullByDefault
public class AWSMessage extends AWSIotMessage {

    private final Logger logger = LoggerFactory.getLogger(AWSMessage.class);

    public static final String EMPTY_PAYLOAD = "{}";

    /**
     * @param topic
     * @param qos
     * @param payload
     */
    public AWSMessage(String topic, AWSIotQos qos, String payload) {
        super(topic, qos, payload);
    }

    @Override
    public void onSuccess() {
        // called when message publishing succeeded
        logger.debug("onSuccess");
    }

    @Override
    public void onFailure() {
        // called when message publishing failed
        logger.error("onFailure");
    }

    @Override
    public void onTimeout() {
        // called when message publishing timed out
        logger.error("onTimeout");
    }
}