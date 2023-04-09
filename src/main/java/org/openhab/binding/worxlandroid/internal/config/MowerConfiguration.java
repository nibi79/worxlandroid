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

/**
 * The {@link MowerConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Nils - Initial contribution
 */
public class MowerConfiguration {

    public int refreshStatusInterval = 600;
    public int pollingInterval = 3600;
    public int reconnectInterval = 0;

    public int getRefreshStatusInterval() {
        return refreshStatusInterval;
    }

    public void setRefreshStatusInterval(int refreshStatusInterval) {
        this.refreshStatusInterval = refreshStatusInterval;
    }

    public int getPollingInterval() {
        return pollingInterval;
    }

    public void setPollingInterval(int pollingInterval) {
        this.pollingInterval = pollingInterval;
    }

    public int getReconnectInterval() {
        return reconnectInterval;
    }

    public void setReconnectInterval(int reconnectInterval) {
        this.reconnectInterval = reconnectInterval;
    }

    @Override
    public String toString() {
        return String.format(
                "MowerConfiguration [pollingInterval='%d', refreshStatusInterval='%d', reconnectInterval='%d']",
                pollingInterval, refreshStatusInterval, reconnectInterval);
    }
}
