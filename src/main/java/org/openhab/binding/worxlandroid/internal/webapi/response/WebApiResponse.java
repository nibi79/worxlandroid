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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

/**
 * {@link WebApiResponse} is an abstract class for an API response
 *
 * @author Nils - Initial contribution
 */
@NonNullByDefault
public abstract class WebApiResponse implements ApiResponse {

    private final Logger logger = LoggerFactory.getLogger(WebApiResponse.class);

    protected static final JsonObject EMPTY_JSON_OBJECT = new JsonParser().parse("{}").getAsJsonObject();

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

        if (jsonResponse.isJsonObject()) {
            return jsonResponse.getAsJsonObject();
        } else {
            logger.warn("Cannot get response as JsonObject");
            return EMPTY_JSON_OBJECT;
        }
    }

    /**
     * @param memberName
     * @return
     */
    public String getMemberDataAsString(String memberName) {
        JsonElement json = getJsonResponseAsJsonObject().get(memberName);
        return json == null || json instanceof JsonNull ? "" : json.getAsString();
    }

    @Override
    public String toString() {
        return jsonResponse.toString();
    }

    /**
     * Transfer json data (except arrays) to property map.
     *
     * @return the property map
     */
    public Map<String, String> getDataAsPropertyMap() {
        return getDataAsPropertyMap(null, "UNKNOWN", getJsonResponseAsJsonObject());
    }

    /**
     * Transfer json data (except arrays) to property map.
     *
     * @param props
     * @param key
     * @param obj
     * @return the property map
     */
    protected Map<String, String> getDataAsPropertyMap(@Nullable Map<String, String> props, String key, Object obj) {

        if (props == null) {
            props = new LinkedHashMap<>();
        }

        if (obj instanceof JsonObject) {
            JsonObject jsonObject = (JsonObject) obj;
            for (Entry<String, JsonElement> jsonEntry : jsonObject.entrySet()) {
                getDataAsPropertyMap(props, jsonEntry.getKey(), jsonEntry.getValue());
            }
        } else if (obj instanceof JsonPrimitive) {
            props.put(key, ((JsonPrimitive) obj).getAsString());
        } else if (obj instanceof JsonNull) {
            props.put(key, "");
        }
        return props;
    }
}
