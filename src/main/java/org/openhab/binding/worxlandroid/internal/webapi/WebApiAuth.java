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
package org.openhab.binding.worxlandroid.internal.webapi;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * {@link WebApiAuth} authorization data dor Worx Ladroid API.
 *
 * @author Nils - Initial contribution
 *
 */
@NonNullByDefault
public class WebApiAuth {

    private String accessType;
    private String accessToken;

    /**
     * @param accessType
     * @param accessToken
     */
    public WebApiAuth(String accessType, String accessToken) {
        super();
        this.accessType = accessType;
        this.accessToken = accessToken;
    }

    public String getAccessType() {
        return accessType;
    }

    public void setAccessType(String accessType) {
        this.accessType = accessType;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * @return authorization string: 'Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ...'
     */
    public String getAuthorization() {
        return String.format("%s %s", accessType, accessToken);
    }
}
