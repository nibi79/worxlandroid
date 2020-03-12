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
package org.openhab.binding.worxlandroid.internal;

/**
 * The {@link WorxLandroidConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Nils - Initial contribution
 */
public class WorxLandroidConfiguration {

    public String webapiUsername;
    public String webapiPassword;

    public String getWebapiUsername() {
        return webapiUsername;
    }

    public void setWebapiUsername(String webapiUsername) {
        this.webapiUsername = webapiUsername;
    }

    public String getWebapiPassword() {
        return webapiPassword;
    }

    public void setWebapiPassword(String webapiPassword) {
        this.webapiPassword = webapiPassword;
    }

    @Override
    public String toString() {
        return String.format("LandroidWorxConfiguration [webapiPassword='%s',webapiPassword='*****']", webapiUsername);
    }
}
