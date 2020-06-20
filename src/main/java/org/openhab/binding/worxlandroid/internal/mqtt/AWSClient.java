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

import java.security.KeyStore;

import com.amazonaws.services.iot.client.AWSIotMqttClient;

/**
 * {@link AWSClient} AWS client
 *
 * @author Nils - Initial contribution
 */
public class AWSClient extends AWSIotMqttClient {

    private static final int MAX_CONNECTION_RETRIES = 90000;
    private static final int BASE_RETRY_DELAY = 30000;

    private AWSClientCallback clientCallback;
    private String endpoint;

    /**
     * @param clientEndpoint
     * @param clientId
     * @param keyStore
     * @param keyPassword
     */
    public AWSClient(String clientEndpoint, String clientId, KeyStore keyStore, String keyPassword,
            AWSClientCallback clienCallback) {

        super(clientEndpoint, clientId, keyStore, keyPassword);

        this.clientCallback = clienCallback;
        this.endpoint = clientEndpoint;
        this.setBaseRetryDelay(BASE_RETRY_DELAY);
        this.setMaxConnectionRetries(MAX_CONNECTION_RETRIES);
    }

    @Override
    public void onConnectionSuccess() {
        super.onConnectionSuccess();
        clientCallback.onAWSConnectionSuccess();
    }

    @Override
    public void onConnectionFailure() {
        super.onConnectionFailure();
        clientCallback.onAWSConnectionFailure();
    }

    @Override
    public void onConnectionClosed() {
        super.onConnectionClosed();
        clientCallback.onAWSConnectionClosed();
    }

    public String getEndpoint() {
        return endpoint;
    }
}
