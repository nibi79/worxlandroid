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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link UsersMeResponse} class
 *
 * @author Nils - Initial contribution
 *
 */
@NonNullByDefault
public class UsersMeResponse extends WebApiResponse {

    /**
     * @param jsonResponse
     */
    public UsersMeResponse(String jsonResponse) {
        super(jsonResponse);
    }

    /**
     * @return response data as property list
     */
    public Map<String, String> getDataAsPropertyList() {
        Map<String, String> props = new LinkedHashMap<>();
        props.put(MN_ID, getMemberDataAsString(MN_ID));
        props.put(MN_EMAIL, getMemberDataAsString(MN_EMAIL));
        props.put(MN_NAME, getMemberDataAsString(MN_NAME));
        props.put(MN_SURNAME, getMemberDataAsString(MN_SURNAME));
        props.put(MN_USER_TYPE, getMemberDataAsString(MN_USER_TYPE));
        props.put(MN_LOCALE, getMemberDataAsString(MN_LOCALE));
        props.put(MN_PUSHNOTIFICATIONS, getMemberDataAsString(MN_PUSHNOTIFICATIONS));
        props.put(MN_LOCATION_LATITUDE, getMemberDataAsString(MN_LOCATION_LATITUDE));
        props.put(MN_LOCATION_LONGITUDE, getMemberDataAsString(MN_LOCATION_LONGITUDE));
        props.put(MN_TERMSOFUSEAGREED, getMemberDataAsString(MN_TERMSOFUSEAGREED));
        props.put(MN_COUNTRYID, getMemberDataAsString(MN_COUNTRYID));
        props.put(MN_MQTTENDPOINT, getMemberDataAsString(MN_MQTTENDPOINT));
        props.put(MN_ACTIONSONGOOGLEPINCODE, getMemberDataAsString(MN_ACTIONSONGOOGLEPINCODE));
        props.put(MN_CREATEDAT, getMemberDataAsString(MN_CREATEDAT));
        props.put(MN_UPDATEDAT, getMemberDataAsString(MN_UPDATEDAT));
        return props;
    }

}
