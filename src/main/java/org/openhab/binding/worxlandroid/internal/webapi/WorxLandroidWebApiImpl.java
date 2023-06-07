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
import org.eclipse.jdt.annotation.Nullable;
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
@NonNullByDefault
public class WorxLandroidWebApiImpl implements WorxLandroidApi {
    private final Logger logger = LoggerFactory.getLogger(WorxLandroidWebApiImpl.class);

    private final HttpClient httpClient;
    private @Nullable WebApiAuth apiAuth;
    private @Nullable OauthTokenResponse authResponse;

    public @Nullable OauthTokenResponse getAuthResponse() {
        return authResponse;
    }

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
            OauthTokenResponse localAuthResponse = authRequest.call(username, password);
            authResponse = localAuthResponse;
            apiAuth = new WebApiAuth(authResponse.getAccessType(), localAuthResponse.getAccessToken(),
                    localAuthResponse.getRefreshToken(), localAuthResponse.getExpiresIn());

            return true;

        } catch (WebApiException e) {
            logger.error("Error connecting to Worx Landroid WebApi! Error = {}", e.getErrorMsg());
            return false;
        }
    }

    public @Nullable String getAccessToken() {
        WebApiAuth localApiAuth = apiAuth;
        return localApiAuth != null ? localApiAuth.getAccessToken() : null;
    }

    public boolean isTokenValid() {
        WebApiAuth localApiAuth = apiAuth;
        return localApiAuth != null ? localApiAuth.isTokenValid() : false;
    }

    @Override
    public boolean refreshToken() {
        WebApiAuth localApiAuth = apiAuth;
        if (localApiAuth != null) {
            logger.debug("refreshToken -> token is: {}", localApiAuth.isTokenValid());
            if (!localApiAuth.isTokenValid()) {
                try {
                    OauthTokenRequest authRequest = new OauthTokenRequest(httpClient);
                    OauthTokenResponse authResponse = authRequest.refresh(localApiAuth);

                    localApiAuth.setAccessToken(authResponse.getAccessToken());
                    localApiAuth.setRefreshToken(authResponse.getRefreshToken());
                    localApiAuth.setExpire(authResponse.getExpiresIn());

                    return true;

                } catch (WebApiException e) {
                    logger.error("Error connecting to Worx Landroid WebApi! Error = {}", e.getErrorMsg());
                    return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }

    @Override
    public UsersCertificateResponse retrieveAwsCertificate() throws WebApiException {
        WebApiAuth localApiAuth = apiAuth;
        if (localApiAuth == null) {
            throw new WebApiException("Worx Landroid WebApi not connected!");
        }

        UsersCertificateRequest awsCertificateRequest = new UsersCertificateRequest(httpClient);
        return awsCertificateRequest.call(localApiAuth);
    }

    @Override
    public UsersMeResponse retrieveWebInfo() throws WebApiException {
        WebApiAuth localApiAuth = apiAuth;
        if (localApiAuth == null) {
            throw new WebApiException("Worx Landroid WebApi not connected!");
        }

        UsersMeRequest webInfoRequest = new UsersMeRequest(httpClient);
        return webInfoRequest.call(localApiAuth);
    }

    @Override
    public ProductItemsResponse retrieveUserDevices() throws WebApiException {
        WebApiAuth localApiAuth = apiAuth;
        if (localApiAuth == null) {
            throw new WebApiException("Worx Landroid WebApi not connected!");
        }

        ProductItemsRequest productItemsRequest = new ProductItemsRequest(httpClient);
        return productItemsRequest.call(localApiAuth);
    }

    @Override
    public ProductItemsStatusResponse retrieveDeviceStatus(String serialNumber) throws WebApiException {
        WebApiAuth localApiAuth = apiAuth;
        if (localApiAuth == null) {
            throw new WebApiException("Worx Landroid WebApi not connected!");
        }

        ProductItemsStatusRequest productItemsStatusRequest = new ProductItemsStatusRequest(httpClient);
        return productItemsStatusRequest.call(localApiAuth, serialNumber);
    }

    @Override
    public ProductsResponse retrieveDevices() throws WebApiException {
        WebApiAuth localApiAuth = apiAuth;
        if (localApiAuth == null) {
            throw new WebApiException("Worx Landroid WebApi not connected!");
        }

        ProductsRequest productsRequest = new ProductsRequest(httpClient);
        return productsRequest.call(localApiAuth);
    }
}
