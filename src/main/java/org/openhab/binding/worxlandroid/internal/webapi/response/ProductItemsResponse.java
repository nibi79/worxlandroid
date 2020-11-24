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

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * The {@link ProductItemsResponse} class
 *
 * @author Nils - Initial contribution
 *
 */
@NonNullByDefault
public class ProductItemsResponse extends WebApiResponse {

    private final Logger logger = LoggerFactory.getLogger(ProductItemsResponse.class);

    /**
     * @param jsonResponse
     */
    public ProductItemsResponse(String jsonResponse) {
        super(jsonResponse);
    }

    /**
     * Return mower data by id from response.
     *
     * @param serialNumber
     * @return
     */
    public @Nullable JsonObject getMowerDataById(@Nullable String serialNumber) {

        if (serialNumber != null) {

            JsonElement jsonResponse = getJsonResponse();

            if (jsonResponse.isJsonArray()) {

                JsonArray jsonArray = jsonResponse.getAsJsonArray();
                for (JsonElement jsonElement : jsonArray) {

                    if (jsonElement.isJsonObject()) {
                        JsonObject jsonObject = jsonElement.getAsJsonObject();
                        String id = jsonObject.get("serial_number").getAsString();
                        if (id != null && id.equals(serialNumber)) {
                            return jsonObject;
                        }
                    }
                }
            }
        }
        logger.warn("No data for mower serialnumber: {}", serialNumber);
        return null;
    }

    /**
     * @param serialNumber
     * @return
     */
    public Map<String, String> getDataAsPropertyMap(@Nullable String serialNumber) {

        JsonObject jsonObject = getMowerDataById(serialNumber);
        jsonObject = jsonObject == null ? EMPTY_JSON_OBJECT : jsonObject;

        return super.getDataAsPropertyMap(null, "UNKNOWN", jsonObject);
    }
}
