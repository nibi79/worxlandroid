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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.openhab.binding.worxlandroid.internal.webapi.WebApiAuth;
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
    private static final String CONTENT_PROVIDER = "application/json";
    private static final String CLIENT_ID = "013132A8-DB34-4101-B993-3C8348EA0EBC";
    private static final String APIURL_OAUTH_TOKEN = "https://id.eu.worx.com/" + "oauth/token";

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
        request.header("Content-Type", "application/json; utf-8");

        JsonObject jsonContent = new JsonObject();
        jsonContent.add("grant_type", new JsonPrimitive("password"));
        jsonContent.add("username", new JsonPrimitive(username));
        jsonContent.add("password", new JsonPrimitive(password));
        jsonContent.add("scope", new JsonPrimitive("*"));
        jsonContent.add("client_id", new JsonPrimitive(CLIENT_ID));

        request.content(new StringContentProvider(jsonContent.toString()), CONTENT_PROVIDER);

        return callWebApi(request);
    }

    /**
     * @param username
     * @param password
     * @return
     * @throws WebApiException
     */
    public OauthTokenResponse refresh(WebApiAuth auth) throws WebApiException {
        Request request = getHttpClient().POST(APIURL_OAUTH_TOKEN);
        // TODO NB
        request.header("Authorization", auth.getAuthorization());
        request.header("Content-Type", "application/json; utf-8");

        JsonObject jsonContent = new JsonObject();
        jsonContent.add("grant_type", new JsonPrimitive("refresh_token"));
        jsonContent.add("refresh_token", new JsonPrimitive(auth.getRefreshToken()));
        jsonContent.add("client_id", new JsonPrimitive(CLIENT_ID));

        request.content(new StringContentProvider(jsonContent.toString()), CONTENT_PROVIDER);

        return callWebApi(request);
    }
}
