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
import java.lang.reflect.Type;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.openhab.binding.worxlandroid.internal.deserializer.WebApiDeserializer;
import org.openhab.binding.worxlandroid.internal.webapi.WebApiException;
import org.openhab.core.auth.client.oauth2.AccessTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link WebApiRequest} is a API request
 *
 * @author Nils - Initial contribution
 */
@NonNullByDefault
public abstract class WebApiRequest<T> {
    protected static final String APIURL_BASE = "https://api.worxlandroid.com/api/v2/";

    private final Logger logger = LoggerFactory.getLogger(WebApiRequest.class);
    private final @Nullable Class<?> typeParameterClass;
    private final HttpClient httpClient;
    private final @Nullable WebApiDeserializer deserializer;
    private final @Nullable Type typeToken;

    public WebApiRequest(HttpClient httpClient, Class<?> clazz, @Nullable WebApiDeserializer deserializer) {
        this.typeParameterClass = clazz;
        this.httpClient = httpClient;
        this.deserializer = deserializer;
        typeToken = null;
    }

    public WebApiRequest(HttpClient httpClient, Type typeToken, WebApiDeserializer deserializer) {
        this.typeParameterClass = null;
        this.typeToken = typeToken;
        this.httpClient = httpClient;
        this.deserializer = deserializer;
    }

    @SuppressWarnings("unchecked")
    protected T callWebApiGet(String url, AccessTokenResponse token) throws WebApiException {
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
                if (deserializer != null) {
                    Class<?> typeParameter = typeParameterClass;
                    if (typeParameter != null) {
                        return (T) deserializer.deserialize(typeParameter, result);
                    }
                    Type localTypeToken = typeToken;
                    if (localTypeToken != null) {
                        return (T) deserializer.deserialize(localTypeToken, result);
                    }
                }
                Constructor<T> ctor = (Constructor<T>) typeParameterClass.getConstructor(String.class);
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
