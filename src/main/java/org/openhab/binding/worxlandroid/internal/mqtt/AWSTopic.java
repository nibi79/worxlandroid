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

import java.nio.charset.StandardCharsets;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.crt.mqtt.MqttMessage;

/**
 * {@link AWSTopic} AWS topic
 *
 * @author Nils - Initial contribution
 */
@NonNullByDefault
public class AWSTopic {
    private final Logger logger = LoggerFactory.getLogger(AWSTopic.class);
    private final AWSMessageCallback callback;
    private final String topic;

    public AWSTopic(String topic, AWSMessageCallback awsMessageCallback) {
        this.topic = topic;
        this.callback = awsMessageCallback;
    }

    public void onMessage(MqttMessage mqttMessage) {
        String payload = new String(mqttMessage.getPayload(), StandardCharsets.UTF_8);
        logger.debug("onMessage: {}", payload);
        callback.processMessage(payload);
    }

    public String getTopic() {
        return topic;
    }
}
