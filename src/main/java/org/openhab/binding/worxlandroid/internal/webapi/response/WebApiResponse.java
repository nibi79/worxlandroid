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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * {@link WebApiResponse} is an abstract class for an API response
 *
 * @author Nils - Initial contribution
 */
@NonNullByDefault
public abstract class WebApiResponse {

    private final Logger logger = LoggerFactory.getLogger(WebApiResponse.class);

    private JsonElement jsonResponse = new JsonParser().parse("{}");

    /**
     * @param jsonResponse
     */
    public WebApiResponse(String jsonResponse) {
        try {
            this.jsonResponse = new JsonParser().parse(jsonResponse);
        } catch (JsonSyntaxException e) {
            // keep default value
        }
    }

    /**
     * Returns the json response
     *
     * @return
     */
    public JsonElement getJsonResponse() {
        return jsonResponse;
    }

    /**
     * Returns the json response as JsonObject
     *
     * @return
     */
    public JsonObject getJsonResponseAsJsonObject() {
        return jsonResponse.getAsJsonObject();
    }

    @Override
    public String toString() {
        return jsonResponse.toString();
    }

}
