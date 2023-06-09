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
package org.openhab.binding.worxlandroid.internal.webapi.request;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.openhab.binding.worxlandroid.internal.webapi.WebApiException;
import org.openhab.binding.worxlandroid.internal.webapi.response.WebApiResponse;
import org.openhab.core.auth.client.oauth2.AccessTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link WebApiRequest} is a API request
 *
 * @author Nils - Initial contribution
 */
@NonNullByDefault
public abstract class WebApiRequest<T extends WebApiResponse> {
    protected static final String APIURL_BASE = "https://api.worxlandroid.com/api/v2/";

    private final Logger logger = LoggerFactory.getLogger(WebApiRequest.class);
    private final Class<T> typeParameterClass;
    private final HttpClient httpClient;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public WebApiRequest(HttpClient httpClient) {
        Type superClass = getClass().getGenericSuperclass();
        if (superClass == null) {
            throw new IllegalArgumentException("Generic superclass should not be null.");
        }

        this.typeParameterClass = ((Class) ((ParameterizedType) superClass).getActualTypeArguments()[0]);
        this.httpClient = httpClient;
    }

    protected HttpClient getHttpClient() {
        return httpClient;
    }

    protected T callWebApiGet(String url, AccessTokenResponse token) throws WebApiException {
        Request request = getHttpClient().newRequest(url).method("GET");
        request.header("Authorization", "%s %s".formatted(token.getTokenType(), token.getAccessToken()));
        request.header("Content-Type", "application/json; utf-8");

        return callWebApi(request);
    }

    protected synchronized T callWebApi(Request request) throws WebApiException {
        if (logger.isDebugEnabled()) {
            logger.debug("URI: {}", request.getURI().toString());
        }
        try {
            ContentResponse response = request.send();
            if (response.getStatus() == 200) {
                String result = response.getContentAsString();

                // hiding secret data for log
                logger.debug("Worx Landroid WebApi Response: {}",
                        result.replaceAll("_token\":\"[^\"]*\"", "_token\":\"***hidden for log***\"")
                                .replaceAll("pkcs12\":\"[^\"]*\"", "pkcs12\":\"***hidden for log***\""));

                Constructor<T> ctor = typeParameterClass.getConstructor(String.class);

                return ctor.newInstance(new Object[] { result });
            } else {
                throw new WebApiException(
                        String.format("Error calling Worx Landroid WebApi! HTTP Status = %s", response.getStatus()));
            }
        } catch (SecurityException | InterruptedException | TimeoutException | ExecutionException
                | NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            throw new WebApiException(e);
        }
    }
}
