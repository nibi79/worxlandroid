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

/**
 * The {@link ProductItemsStatusResponse} class
 *
 * @author Nils - Initial contribution
 *
 */
@NonNullByDefault
public class ProductItemsStatusResponse extends WebApiResponse {

    private final Logger logger = LoggerFactory.getLogger(ProductItemsStatusResponse.class);

    /**
     * @param jsonResponse
     */
    public ProductItemsStatusResponse(String jsonResponse) {
        super(jsonResponse);
    }
}
