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
package org.openhab.binding.worxlandroid.internal.webapi;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.worxlandroid.internal.webapi.response.ProductItemsResponse;
import org.openhab.binding.worxlandroid.internal.webapi.response.ProductItemsStatusResponse;
import org.openhab.binding.worxlandroid.internal.webapi.response.ProductsResponse;
import org.openhab.binding.worxlandroid.internal.webapi.response.UsersCertificateResponse;
import org.openhab.binding.worxlandroid.internal.webapi.response.UsersMeResponse;

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
    // public boolean connect(String username, String password) throws WebApiException;

    /**
     * Refresh Bearer Token to Worx Landroid Web API.
     *
     * @return
     * @throws WebApiException
     */
    // public boolean refreshToken() throws WebApiException;

    /**
     * Retrieve AWS certificate
     *
     * /**
     *
     * @return
     * @throws WebApiException
     */
    public UsersCertificateResponse retrieveAwsCertificate() throws WebApiException;

    /**
     * Retrieve Info
     *
     * @return
     * @throws WebApiException
     */
    public UsersMeResponse retrieveWebInfo() throws WebApiException;

    /**
     * Retrieve user devices
     *
     * @return
     * @throws WebApiException
     */
    public ProductItemsResponse retrieveUserDevices() throws WebApiException;

    /**
     * Retrieve product information
     *
     * @return
     * @throws WebApiException
     */
    public ProductsResponse retrieveDevices() throws WebApiException;

    /**
     * Retrieve status of device
     *
     * @param serialNumber
     * @return
     * @throws WebApiException
     */
    public ProductItemsStatusResponse retrieveDeviceStatus(String serialNumber) throws WebApiException;
}
