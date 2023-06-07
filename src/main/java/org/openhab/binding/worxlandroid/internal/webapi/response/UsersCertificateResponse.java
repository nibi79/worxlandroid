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
package org.openhab.binding.worxlandroid.internal.webapi.response;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link UsersCertificateResponse} class
 *
 * @author Nils - Initial contribution
 *
 */
@NonNullByDefault
public class UsersCertificateResponse extends WebApiResponse {

    /**
     * @param jsonResponse
     */
    public UsersCertificateResponse(String jsonResponse) {
        super(jsonResponse);
    }

    /**
     * @return "pkcs12" from api response
     */
    public String getPkcs12() {

        return getJsonResponseAsJsonObject().get("pkcs12").getAsString();
    }

    /**
     * @return "active" from api response
     */
    public boolean isActive() {

        String active = getJsonResponseAsJsonObject().get("active").getAsString();
        return Boolean.parseBoolean(active);
    }
}
