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

import java.io.UnsupportedEncodingException;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.NonNull;

import software.amazon.awssdk.crt.mqtt.MqttClientConnectionEvents;

/**
 *
 * @author Nils
 */
public interface AWSClientI extends MqttClientConnectionEvents {

    public String getEndpoint();

    public String getClientId();

    public String getUsernameMqtt();

    public boolean connect();

    public CompletableFuture<Void> disconnect();

    public void subscribe(@NonNull AWSTopicI awsTopic);

    public void publish(AWSMessageI awsMessageI);

    public boolean refreshConnection(String token) throws UnsupportedEncodingException;
}
