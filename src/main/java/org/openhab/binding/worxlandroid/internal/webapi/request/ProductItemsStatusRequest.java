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
package org.openhab.binding.worxlandroid.internal.webapi.request;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.worxlandroid.internal.webapi.WebApiAuth;
import org.openhab.binding.worxlandroid.internal.webapi.WebApiException;
import org.openhab.binding.worxlandroid.internal.webapi.response.ProductItemsStatusResponse;

/**
 * The {@link ProductItemsStatusRequest} class
 *
 * @author Nils - Initial contribution
 *
 */
@NonNullByDefault
public class ProductItemsStatusRequest extends WebApiRequest<ProductItemsStatusResponse> {

    private static final String APIURL_PRODUCTITEMS = APIURL_BASE + "product-items";

    /**
     * @param httpClient
     */
    public ProductItemsStatusRequest(HttpClient httpClient) {
        super(httpClient);
    }

    /**
     * @param auth
     * @param serialNumber
     * @return
     * @throws WebApiException
     */
    public ProductItemsStatusResponse call(WebApiAuth auth, String serialNumber) throws WebApiException {

        return callWebApiGet(String.format("%s?status=1", APIURL_PRODUCTITEMS), auth);
    }
}
