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
import org.openhab.binding.worxlandroid.internal.webapi.WebApiAuth;
import org.openhab.binding.worxlandroid.internal.webapi.WebApiException;
import org.openhab.binding.worxlandroid.internal.webapi.response.UsersCertificateResponse;

/**
 * The {@link UsersCertificateRequest} class
 *
 * @author Nils - Initial contribution
 *
 */
@NonNullByDefault
public class UsersCertificateRequest extends WebApiRequest<UsersCertificateResponse> {

    private static final String APIURL_USERS_CERTIFICATE = APIURL_BASE + "users/certificate";

    /**
     * @param httpClient
     */
    public UsersCertificateRequest(HttpClient httpClient) {
        super(httpClient);
    }

    /**
     * @param auth
     * @return
     * @throws WebApiException
     */
    public UsersCertificateResponse call(WebApiAuth auth) throws WebApiException {

        return callWebApiGet(APIURL_USERS_CERTIFICATE, auth);
    }
}
