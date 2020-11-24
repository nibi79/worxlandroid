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

import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.worxlandroid.internal.webapi.request.OauthTokenRequest;
import org.openhab.binding.worxlandroid.internal.webapi.request.ProductItemsRequest;
import org.openhab.binding.worxlandroid.internal.webapi.request.ProductItemsStatusRequest;
import org.openhab.binding.worxlandroid.internal.webapi.request.ProductsRequest;
import org.openhab.binding.worxlandroid.internal.webapi.request.UsersCertificateRequest;
import org.openhab.binding.worxlandroid.internal.webapi.request.UsersMeRequest;
import org.openhab.binding.worxlandroid.internal.webapi.response.OauthTokenResponse;
import org.openhab.binding.worxlandroid.internal.webapi.response.ProductItemsResponse;
import org.openhab.binding.worxlandroid.internal.webapi.response.ProductItemsStatusResponse;
import org.openhab.binding.worxlandroid.internal.webapi.response.ProductsResponse;
import org.openhab.binding.worxlandroid.internal.webapi.response.UsersCertificateResponse;
import org.openhab.binding.worxlandroid.internal.webapi.response.UsersMeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link WorxLandroidApi} is a facade for Worx Landroid Web API.
 *
 * @author Nils - Initial contribution
 *
 */
public class WorxLandroidWebApiImpl implements WorxLandroidApi {

    private final Logger logger = LoggerFactory.getLogger(WorxLandroidWebApiImpl.class);

    private final HttpClient httpClient;
    private WebApiAuth apiAuth;

    /**
     * @param httpClient
     */
    public WorxLandroidWebApiImpl(HttpClient httpClient) {
        super();
        this.httpClient = httpClient;
    }

    @Override
    public boolean connect(String username, String password) {

        try {
            OauthTokenRequest authRequest = new OauthTokenRequest(httpClient);
            OauthTokenResponse authResponse = authRequest.call(username, password);

            apiAuth = new WebApiAuth(authResponse.getAccessType(), authResponse.getAccessToken());

            return true;

        } catch (WebApiException e) {

            logger.error("Error connecting to Worx Landroid WebApi! Error = {}", e.getErrorMsg());
            return false;
        }
    }

    @Override
    public UsersCertificateResponse retrieveAwsCertificate() throws WebApiException {

        if (apiAuth == null) {
            throw new WebApiException("Worx Landroid WebApi not connected!");
        }

        UsersCertificateRequest awsCertificateRequest = new UsersCertificateRequest(httpClient);
        return awsCertificateRequest.call(apiAuth);
    }

    @Override
    public UsersMeResponse retrieveWebInfo() throws WebApiException {

        if (apiAuth == null) {
            throw new WebApiException("Worx Landroid WebApi not connected!");
        }

        UsersMeRequest webInfoRequest = new UsersMeRequest(httpClient);
        return webInfoRequest.call(apiAuth);
    }

    @Override
    public ProductItemsResponse retrieveUserDevices() throws WebApiException {

        if (apiAuth == null) {
            throw new WebApiException("Worx Landroid WebApi not connected!");
        }

        ProductItemsRequest productItemsRequest = new ProductItemsRequest(httpClient);
        return productItemsRequest.call(apiAuth);
    }

    @Override
    public ProductItemsStatusResponse retrieveDeviceStatus(String serialNumber) throws WebApiException {

        if (apiAuth == null) {
            throw new WebApiException("Worx Landroid WebApi not connected!");
        }

        ProductItemsStatusRequest productItemsStatusRequest = new ProductItemsStatusRequest(httpClient);
        return productItemsStatusRequest.call(apiAuth, serialNumber);
    }

    @Override
    public ProductsResponse retrieveDevices() throws WebApiException {

        if (apiAuth == null) {
            throw new WebApiException("Worx Landroid WebApi not connected!");
        }

        ProductsRequest productsRequest = new ProductsRequest(httpClient);
        return productsRequest.call(apiAuth);
    }
}
