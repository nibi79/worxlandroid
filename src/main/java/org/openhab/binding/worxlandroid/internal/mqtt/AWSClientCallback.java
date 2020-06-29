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

/**
 * {@link AWSClientCallback} Callback for AWS connection events
 *
 * @author Nils - Initial contribution
 */
public interface AWSClientCallback {

    /**
     * callback method on connection success
     */
    public void onAWSConnectionSuccess();

    /**
     * callback method on connection failure
     */
    public void onAWSConnectionFailure();

    /**
     * callback method on connection closed
     */
    public void onAWSConnectionClosed();
}
