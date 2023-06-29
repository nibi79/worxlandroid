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
package org.openhab.binding.worxlandroid.internal.api;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.openhab.binding.worxlandroid.internal.api.dto.ProductItemStatus;
import org.openhab.binding.worxlandroid.internal.api.dto.UsersMeResponse;
import org.openhab.core.auth.client.oauth2.AccessTokenResponse;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.reflect.TypeToken;

/**
 * {@link WorxApiHandler} is a API request
 *
 * @author Nils - Initial contribution
 */
@NonNullByDefault
@Component(service = WorxApiHandler.class)
public class WorxApiHandler {
    private static final String APIURL_BASE = "https://api.worxlandroid.com/api/v2/";
    private static final String APIURL_PRODUCTITEMS = APIURL_BASE + "product-items";
    private static final String APIURL_USER_ME = APIURL_BASE + "users/me";

    private final Logger logger = LoggerFactory.getLogger(WorxApiHandler.class);
    private final HttpClient httpClient;
    private final WorxApiDeserializer deserializer;

    @Activate
    public WorxApiHandler(final @Reference HttpClientFactory httpClientFactory,
            final @Reference WorxApiDeserializer deserializer) {
        this.httpClient = httpClientFactory.getCommonHttpClient();
        this.deserializer = deserializer;
    }

    protected <T> T callWebApiGet(String url, AccessTokenResponse token, Type type) throws WebApiException {
        Request request = httpClient.newRequest(url).method("GET");
        request.header("Authorization", "%s %s".formatted(token.getTokenType(), token.getAccessToken()));
        request.header("Content-Type", "application/json; utf-8");

        logger.debug("URI: {}", request.getURI().toString());
        try {
            ContentResponse response = request.send();
            if (response.getStatus() == 200) {
                String result = response.getContentAsString();

                // hiding secret data for log
                logger.debug("Worx Landroid WebApi Response: {}",
                        result.replaceAll("_token\":\"[^\"]*\"", "_token\":\"***hidden for log***\"")
                                .replaceAll("pkcs12\":\"[^\"]*\"", "pkcs12\":\"***hidden for log***\""));
                return deserializer.deserialize(type, result);
            } else {
                throw new WebApiException(
                        String.format("Error calling Worx Landroid WebApi! HTTP Status = %s", response.getStatus()));
            }
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            throw new WebApiException(e);
        }
    }

    public WorxApiDeserializer getDeserializer() {
        return deserializer;
    }

    public List<ProductItemStatus> retrieveDeviceStatus(AccessTokenResponse token) throws WebApiException {
        Type type = new TypeToken<List<ProductItemStatus>>() {
        }.getType();
        return callWebApiGet("%s?status=1".formatted(APIURL_PRODUCTITEMS), token, type);
    }

    public ProductItemStatus retrieveDeviceStatus(AccessTokenResponse token, String serialNumber)
            throws WebApiException {
        Type type = new TypeToken<ProductItemStatus>() {
        }.getType();
        return callWebApiGet("%s/%s?status=1".formatted(APIURL_PRODUCTITEMS, serialNumber), token, type);
    }

    public UsersMeResponse retrieveUsersMe(AccessTokenResponse token) throws WebApiException {
        Type type = new TypeToken<UsersMeResponse>() {
        }.getType();
        return callWebApiGet(APIURL_USER_ME, token, type);
    }
}
