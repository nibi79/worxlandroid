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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.openhab.binding.worxlandroid.internal.webapi.WebApiAuth;
import org.openhab.binding.worxlandroid.internal.webapi.WebApiException;
import org.openhab.binding.worxlandroid.internal.webapi.response.WebApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link WebApiRequest} is a API request
 *
 * @author Nils - Initial contribution
 */
@NonNullByDefault
public abstract class WebApiRequest<T extends WebApiResponse> {

    private final Logger logger = LoggerFactory.getLogger(WebApiRequest.class);

    protected static final String APIURL_BASE = "https://api.worxlandroid.com/api/v2/";

    final Class<T> typeParameterClass;

    private HttpClient httpClient;

    /**
     * @param httpClient
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public WebApiRequest(HttpClient httpClient) {
        super();

        this.typeParameterClass = ((Class) ((ParameterizedType) getClass().getGenericSuperclass())
                .getActualTypeArguments()[0]);

        this.httpClient = httpClient;
    }

    /**
     * @return
     */
    public HttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * @param uri
     * @param auth
     * @return
     * @throws WebApiException
     */
    protected T callWebApiGet(String url, WebApiAuth auth) throws WebApiException {

        Request request = getHttpClient().newRequest(url).method("GET");
        request.header("Authorization", auth.getAuthorization());
        request.header("Content-Type", "application/json; utf-8");

        return callWebApi(request);
    }

    /**
     * @param request
     * @return
     * @throws WebApiException
     */
    protected synchronized T callWebApi(Request request) throws WebApiException {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("URI: {}", request.getURI().toString());
            }

            ContentResponse response = request.send();

            if (response.getStatus() == 200) {
                byte[] rawResponse = response.getContent();
                String result = new String(rawResponse);// , encoding);

                // hide secret data for log
                String debugResultString = result;
                debugResultString = debugResultString.replaceAll("_token\":\"[^\"]*\"",
                        "_token\":\"***hidden for debug log***\"");
                debugResultString = debugResultString.replaceAll("pkcs12\":\"[^\"]*\"",
                        "pkcs12\":\"***hidden for debug log***\"");

                logger.debug("Worx Landroid WebApi Response: {}", debugResultString);

                Constructor<T> ctor = typeParameterClass.getConstructor(String.class);

                T vo = ctor.newInstance(new Object[] { result });

                return vo;

            } else {
                throw new WebApiException(
                        String.format("Error calling Worx Landroid WebApi! HTTP Status = %s", response.getStatus()));
            }

        } catch (InterruptedException | TimeoutException | ExecutionException | InstantiationException
                | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
                | SecurityException e) {
            throw new WebApiException(e);
        }
    }
}
