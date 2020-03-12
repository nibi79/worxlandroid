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
package org.openhab.binding.worxlandroid.internal.webapi.request;

import java.util.Base64;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.openhab.binding.worxlandroid.internal.webapi.WebApiException;
import org.openhab.binding.worxlandroid.internal.webapi.response.OauthTokenResponse;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * The {@link OauthTokenRequest} class
 *
 * @author Nils - Initial contribution
 *
 */
@NonNullByDefault
public class OauthTokenRequest extends WebApiRequest<OauthTokenResponse> {

    private static final String APIURL_OAUTH_TOKEN = APIURL_BASE + "oauth/token";

    private static final String WEBAPI_SECRET_BASE64 = "bkNIM0EwV3ZNWW42NnZHb3JqU3JuR1oyWXRqUVdEaUN2amc3ak54Sw==";

    /**
     * @param httpClient
     */
    public OauthTokenRequest(HttpClient httpClient) {
        super(httpClient);
    }

    /**
     * @param username
     * @param password
     * @return
     * @throws WebApiException
     */
    public OauthTokenResponse call(String username, String password) throws WebApiException {

        Request request = getHttpClient().POST(APIURL_OAUTH_TOKEN);

        String secret = new String(Base64.getDecoder().decode(WEBAPI_SECRET_BASE64));

        JsonObject jsonContent = new JsonObject();
        jsonContent.add("grant_type", new JsonPrimitive("password"));
        jsonContent.add("client_secret", new JsonPrimitive(secret));
        jsonContent.add("username", new JsonPrimitive(username));
        jsonContent.add("password", new JsonPrimitive(password));
        jsonContent.add("scope", new JsonPrimitive("*"));
        jsonContent.add("client_id", new JsonPrimitive(1));
        jsonContent.add("type", new JsonPrimitive("app"));

        request.content(new StringContentProvider(jsonContent.toString()), "application/json");

        return callWebApi(request);
    }

}
