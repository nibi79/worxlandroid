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
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.AWSIotTopic;

/**
 * {@link AWSTopic} AWS topic
 *
 * @author Nils - Initial contribution
 */
@NonNullByDefault
public class AWSTopic extends AWSIotTopic {

    private final Logger logger = LoggerFactory.getLogger(AWSTopic.class);
    private AWSMessageCallback callback;

    /**
     * @param topic
     * @param qos
     * @param awsMessageCallback
     */
    public AWSTopic(String topic, AWSIotQos qos, AWSMessageCallback awsMessageCallback) {
        super(topic, qos);
        callback = awsMessageCallback;
    }

    @Override
    public void onMessage(@Nullable AWSIotMessage message) {

        logger.info("onMessage: {}", message.getStringPayload());
        callback.processMessage(message);
    }
}