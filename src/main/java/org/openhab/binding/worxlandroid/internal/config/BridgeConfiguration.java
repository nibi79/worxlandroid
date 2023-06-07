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
package org.openhab.binding.worxlandroid.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link BridgeConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Nils - Initial contribution
 */
@NonNullByDefault
public class BridgeConfiguration {

    public @Nullable String webapiUsername;
    public @Nullable String webapiPassword;

    public @Nullable String getWebapiUsername() {
        return webapiUsername;
    }

    public void setWebapiUsername(String webapiUsername) {
        this.webapiUsername = webapiUsername;
    }

    public @Nullable String getWebapiPassword() {
        return webapiPassword;
    }

    public void setWebapiPassword(String webapiPassword) {
        this.webapiPassword = webapiPassword;
    }

    @Override
    public String toString() {
        return String.format("BridgeConfiguration [webapiPassword='%s', webapiPassword='*****']", webapiUsername);
    }
}
