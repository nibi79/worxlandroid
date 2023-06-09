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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import software.amazon.awssdk.crt.mqtt.MqttClientConnectionEvents;

/**
 *
 * @author Nils - Initial contribution
 */
@NonNullByDefault
public interface AWSClientI extends MqttClientConnectionEvents {

    public @Nullable String getEndpoint();

    public String getClientId();

    public String getUsernameMqtt();

    public boolean connect();

    public @Nullable CompletableFuture<Void> disconnect();

    public void subscribe(AWSTopicI awsTopic);

    public void publish(AWSMessage awsMessage);

    public boolean refreshConnection(String token) throws UnsupportedEncodingException;
}
