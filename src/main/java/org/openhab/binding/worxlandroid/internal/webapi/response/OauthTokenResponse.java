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
package org.openhab.binding.worxlandroid.internal.webapi.response;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link OauthTokenResponse} class
 *
 * @author Nils - Initial contribution
 *
 */
@NonNullByDefault
public class OauthTokenResponse extends WebApiResponse {

    /**
     * @param jsonResponse
     */
    public OauthTokenResponse(String jsonResponse) {
        super(jsonResponse);
    }

    /**
     * @return "token_type" from api response
     */
    public String getAccessType() {

        return getJsonResponseAsJsonObject().getAsJsonPrimitive("token_type").getAsString();
    }

    /**
     * @return "access_token" from api response
     */
    public String getAccessToken() {

        return getJsonResponseAsJsonObject().getAsJsonPrimitive("access_token").getAsString();
    }

    /**
     * @return "expires_in" from api response
     */
    public int getExpiresIn() {

        return getJsonResponseAsJsonObject().getAsJsonPrimitive("expires_in").getAsInt();
    }

    /**
     * @return "refresh_token" from api response
     */
    public String getRefreshToken() {

        return getJsonResponseAsJsonObject().getAsJsonPrimitive("refresh_token").getAsString();
    }
}
