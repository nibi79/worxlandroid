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

import java.time.ZonedDateTime;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.worxlandroid.internal.webapi.WebApiException;

/**
 * The {@link UsersMeResponse} class
 *
 * @author Nils - Initial contribution
 *
 */
@NonNullByDefault
public class UsersMeResponse extends ApiResponse {
    public String id = "";
    public String user_type = "";
    public boolean pushNotifications;
    public String location = "";
    public String mqqtEndpoint = "";
    public String actionsOnGooglePinCode = "";
    public @Nullable ZonedDateTime createdAt;
    public @Nullable ZonedDateTime updatedAt;

    @Override
    public void checkValid() throws WebApiException {
        if (id.isEmpty()) {
            throw new WebApiException("User ID is empty");
        }
    }
}
