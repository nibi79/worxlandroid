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
package org.openhab.binding.worxlandroid.internal.webapi;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.worxlandroid.internal.webapi.response.ApiResponse;
import org.openhab.core.i18n.TimeZoneProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

/**
 * The {@link WebApiDeserializer} is responsible to instantiate suitable Gson (de)serializer
 *
 * @author Gaël L'hopital - Initial contribution
 */
@NonNullByDefault
@Component(service = WebApiDeserializer.class)
public class WebApiDeserializer {
    private static final DateTimeFormatter WORX_FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ssX");

    private final Gson gson;

    @Activate
    public WebApiDeserializer(@Reference TimeZoneProvider timeZoneProvider) {
        gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(ZonedDateTime.class,
                        (JsonDeserializer<ZonedDateTime>) (json, type, context) -> ZonedDateTime
                                .parse(json.getAsJsonPrimitive().getAsString() + "Z", WORX_FORMATTER)
                                .withZoneSameInstant(timeZoneProvider.getTimeZone()))
                .create();
    }

    public <T extends ApiResponse> T deserialize(Class<T> clazz, String json) throws WebApiException {
        try {
            @Nullable
            T result = gson.fromJson(json, clazz);
            if (result != null) {
                result.checkValid();
                return result;
            }
            throw new WebApiException("Deserialization of '%s' resulted in null value".formatted(json));
        } catch (JsonSyntaxException e) {
            throw new WebApiException("Unexpected error deserializing '%s'".formatted(json), e);
        }
    }

    public Map<String, String> toMap(Object object) {
        Map<String, String> fromObject = gson.fromJson(gson.toJson(object), new TypeToken<HashMap<String, Object>>() {
        }.getType());
        return fromObject != null ? Map.copyOf(fromObject) : Map.of();
    }
}
