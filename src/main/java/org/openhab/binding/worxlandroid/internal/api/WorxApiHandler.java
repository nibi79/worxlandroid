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
    private static final String URL_BASE = "https://api.worxlandroid.com/api/v2/";
    private static final String URL_PRODUCT_ITEMS = URL_BASE + "product-items";
    private static final String URL_USERS_ME = URL_BASE + "users/me";

    private static final Type PRODUCT_ITEM_STATUS_LIST = new TypeToken<List<ProductItemStatus>>() {
    }.getType();
    private static final Type PRODUCT_ITEM_STATUS = new TypeToken<ProductItemStatus>() {
    }.getType();
    private static final Type USERS_ME = new TypeToken<UsersMeResponse>() {
    }.getType();

    private final Logger logger = LoggerFactory.getLogger(WorxApiHandler.class);
    private final HttpClient httpClient;
    private final WorxApiDeserializer deserializer;

    @Activate
    public WorxApiHandler(final @Reference HttpClientFactory httpClientFactory,
            final @Reference WorxApiDeserializer deserializer) {
        this.httpClient = httpClientFactory.getCommonHttpClient();
        this.deserializer = deserializer;
    }

    private <T> T webApiGet(String url, String accessToken, Type type) throws WebApiException {
        Request request = httpClient.newRequest(url).method("GET");
        request.header("Authorization", "Bearer %s".formatted(accessToken));
        request.header("Content-Type", "application/json; utf-8");

        logger.debug("URI: {}", request.getURI().toString());
        try {
            ContentResponse response = request.send();
            if (response.getStatus() == 200) {
                String result = response.getContentAsString();
                logger.debug("Worx Landroid Api Response: {}", result);
                return deserializer.deserialize(type, result);
            }
            throw new WebApiException(
                    "Error calling Worx Landroid Api! HTTP Status = %d".formatted(response.getStatus()));
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            throw new WebApiException(e);
        }
    }

    public WorxApiDeserializer getDeserializer() {
        return deserializer;
    }

    public List<ProductItemStatus> retrieveDeviceStatus(String token) throws WebApiException {
        return webApiGet("%s?status=1".formatted(URL_PRODUCT_ITEMS), token, PRODUCT_ITEM_STATUS_LIST);
    }

    public ProductItemStatus retrieveDeviceStatus(String token, String serialNumber) throws WebApiException {
        return webApiGet("%s/%s?status=1".formatted(URL_PRODUCT_ITEMS, serialNumber), token, PRODUCT_ITEM_STATUS);
    }

    public UsersMeResponse retrieveMe(String token) throws WebApiException {
        return webApiGet(URL_USERS_ME, token, USERS_ME);
    }
}
