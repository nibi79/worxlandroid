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

import java.io.IOException;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.worxlandroid.internal.deserializer.WebApiDeserializer;
import org.openhab.binding.worxlandroid.internal.webapi.dto.ProductItemStatus;
import org.openhab.binding.worxlandroid.internal.webapi.dto.UsersMeResponse;
import org.openhab.binding.worxlandroid.internal.webapi.request.ProductItemsRequest;
import org.openhab.binding.worxlandroid.internal.webapi.request.ProductItemsStatusRequest;
import org.openhab.binding.worxlandroid.internal.webapi.request.ProductsRequest;
import org.openhab.binding.worxlandroid.internal.webapi.request.UsersCertificateRequest;
import org.openhab.binding.worxlandroid.internal.webapi.request.UsersMeRequest;
import org.openhab.binding.worxlandroid.internal.webapi.response.ProductItemsResponse;
import org.openhab.binding.worxlandroid.internal.webapi.response.ProductsResponse;
import org.openhab.binding.worxlandroid.internal.webapi.response.UsersCertificateResponse;
import org.openhab.core.auth.client.oauth2.AccessTokenResponse;
import org.openhab.core.auth.client.oauth2.OAuthClientService;
import org.openhab.core.auth.client.oauth2.OAuthException;
import org.openhab.core.auth.client.oauth2.OAuthResponseException;

/**
 * The {@link WorxLandroidApi} is a facade for Worx Landroid Web API.
 *
 * @author Nils - Initial contribution
 *
 */
@NonNullByDefault
public class WorxLandroidWebApi {
    private final HttpClient httpClient;
    private final OAuthClientService oAuthClientService;
    private final WebApiDeserializer deserializer;

    public WorxLandroidWebApi(HttpClient httpClient, OAuthClientService oAuthClientService,
            WebApiDeserializer deserializer) {
        this.httpClient = httpClient;
        this.oAuthClientService = oAuthClientService;
        this.deserializer = deserializer;
    }

    public AccessTokenResponse getAccessTokenResponse() throws WebApiException {
        try {
            AccessTokenResponse result = oAuthClientService.getAccessTokenResponse();
            if (result != null) {
                return result;
            }
            throw new WebApiException("No token response available");
        } catch (OAuthException | IOException | OAuthResponseException e) {
            throw new WebApiException("Error reading access token response", e);
        }
    }

    public String getAccessToken() throws WebApiException {
        return getAccessTokenResponse().getAccessToken();
    }

    public UsersCertificateResponse retrieveAwsCertificate() throws WebApiException {
        UsersCertificateRequest awsCertificateRequest = new UsersCertificateRequest(httpClient);
        return awsCertificateRequest.call(getAccessTokenResponse());
    }

    public ProductItemsResponse retrieveUserDevices() throws WebApiException {
        ProductItemsRequest productItemsRequest = new ProductItemsRequest(httpClient);
        return productItemsRequest.call(getAccessTokenResponse());
    }

    public ProductsResponse retrieveDevices() throws WebApiException {
        ProductsRequest productsRequest = new ProductsRequest(httpClient);
        return productsRequest.call(getAccessTokenResponse());
    }

    public List<ProductItemStatus> retrieveDeviceStatus(String userId) throws WebApiException {
        return new ProductItemsStatusRequest(httpClient, deserializer).call(getAccessTokenResponse(), userId);
    }

    public UsersMeResponse retrieveWebInfo() throws WebApiException {
        return new UsersMeRequest(httpClient, deserializer).call(getAccessTokenResponse());
    }
}
