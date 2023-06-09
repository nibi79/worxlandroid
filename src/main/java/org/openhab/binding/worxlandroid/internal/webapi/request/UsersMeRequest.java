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
import org.openhab.binding.worxlandroid.internal.webapi.WebApiDeserializer;
import org.openhab.binding.worxlandroid.internal.webapi.WebApiException;
import org.openhab.binding.worxlandroid.internal.webapi.response.UsersMeResponse;
import org.openhab.core.auth.client.oauth2.AccessTokenResponse;

/**
 * The {@link UsersMeRequest} class
 *
 * @author Nils - Initial contribution
 *
 */
@NonNullByDefault
public class UsersMeRequest extends WebApiRequest<UsersMeResponse> {
    private static final String APIURL_USER_ME = APIURL_BASE + "users/me";

    public UsersMeRequest(HttpClient httpClient, WebApiDeserializer deserializer) {
        super(httpClient, deserializer);
    }

    public UsersMeResponse call(AccessTokenResponse token) throws WebApiException {
        return callWebApiGet(APIURL_USER_ME, token);
    }
}
