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
package org.openhab.binding.worxlandroid.internal.webapi;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link WorxLandroidApi} is an interface for the Worx Landroid API
 *
 * @author Nils - Initial contribution
 *
 */
@NonNullByDefault
public interface WorxLandroidApi {

    /**
     * Establish connection to Worx Landroid Web API.
     *
     * @return
     * @throws WebApiException
     */
    public boolean connect(String username, String password) throws WebApiException;

    /**
     * Retrieve AWS certificate
     *
     * @throws WebApiException
     */
    public void retrieveAwsCertificate() throws WebApiException;

    /**
     * Retrieve Info
     *
     * @throws WebApiException
     */
    public void retrieveWebInfo() throws WebApiException;

    /**
     * Retrieve user devices
     *
     * @throws WebApiException
     */
    public void retrieveUserDevices() throws WebApiException;

    /**
     * Reterieve product information
     *
     * @throws WebApiException
     */
    public void retrieveDevices() throws WebApiException;

}
