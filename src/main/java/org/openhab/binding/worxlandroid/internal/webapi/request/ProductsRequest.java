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
import org.openhab.binding.worxlandroid.internal.webapi.WebApiException;
import org.openhab.binding.worxlandroid.internal.webapi.response.ProductsResponse;
import org.openhab.core.auth.client.oauth2.AccessTokenResponse;

/**
 * The {@link ProductsRequest} class
 *
 * @author Nils - Initial contribution
 *
 */
@NonNullByDefault
public class ProductsRequest extends WebApiRequest<ProductsResponse> {
    private static final String APIURL_PRODUCTS = APIURL_BASE + "products";

    public ProductsRequest(HttpClient httpClient) {
        super(httpClient, ProductsResponse.class, null);
    }

    public ProductsResponse call(AccessTokenResponse token) throws WebApiException {
        return callWebApiGet(APIURL_PRODUCTS, token);
    }
}
