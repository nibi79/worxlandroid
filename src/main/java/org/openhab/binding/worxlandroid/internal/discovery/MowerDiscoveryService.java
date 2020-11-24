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
package org.openhab.binding.worxlandroid.internal.discovery;

import static org.openhab.binding.worxlandroid.internal.WorxLandroidBindingConstants.THING_TYPE_MOWER;

import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.worxlandroid.internal.WorxLandroidBindingConstants;
import org.openhab.binding.worxlandroid.internal.WorxLandroidBridgeHandler;
import org.openhab.binding.worxlandroid.internal.webapi.WorxLandroidWebApiImpl;
import org.openhab.binding.worxlandroid.internal.webapi.response.ProductItemsResponse;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * The {@link MowerDiscoveryService} is a service for discovering your mowers through Worx Landroid API
 *
 * @author Nils - Initial contribution
 */
@NonNullByDefault
public class MowerDiscoveryService extends AbstractDiscoveryService {

    private final Logger logger = LoggerFactory.getLogger(MowerDiscoveryService.class);

    @Nullable
    private WorxLandroidBridgeHandler bridgeHandler = null;

    /**
     * Maximum time to search for devices in seconds.
     */
    private static final int SEARCH_TIME = 20;

    public MowerDiscoveryService() {
        super(WorxLandroidBindingConstants.SUPPORTED_THING_TYPES, SEARCH_TIME);
    }

    public MowerDiscoveryService(WorxLandroidBridgeHandler bridgeHandler) throws IllegalArgumentException {
        super(SEARCH_TIME);
        this.bridgeHandler = bridgeHandler;
    }

    /**
     * Public method for triggering mower discovery
     */
    public void discoverMowers() {
        startScan();
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return WorxLandroidBindingConstants.SUPPORTED_THING_TYPES;
    }

    @Override
    protected void startScan() {

        if (bridgeHandler == null) {
            return;
        }
        // Trigger no scan if offline
        if (bridgeHandler.getThing().getStatus() != ThingStatus.ONLINE) {
            return;
        }

        try {
            WorxLandroidWebApiImpl apiHandler = bridgeHandler.getWorxLandroidWebApiImpl();
            if (apiHandler == null) {
                return;
            }

            ProductItemsResponse productItemsResponse = apiHandler.retrieveUserDevices();

            if (productItemsResponse.getJsonResponse().isJsonArray()) {
                JsonArray mowers = productItemsResponse.getJsonResponse().getAsJsonArray();
                ThingUID bridgeUID = bridgeHandler.getThing().getUID();

                if (mowers != null) {
                    for (JsonElement mowerElement : mowers) {
                        if (mowerElement.isJsonObject()) {
                            JsonObject mower = mowerElement.getAsJsonObject();

                            String serialNumber = mower.get("serial_number").getAsString();

                            ThingUID thingUID = new ThingUID(THING_TYPE_MOWER, bridgeUID, serialNumber);

                            Map<String, Object> properties = null;

                            DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID)
                                    .withProperties(properties).withBridge(bridgeHandler.getThing().getUID())
                                    .withLabel(mower.get("name").getAsString()).build();

                            thingDiscovered(discoveryResult);

                            logger.debug("Discovered a mower thing with ID '{}'", serialNumber);
                        }
                    }
                }
            }
        } catch (Exception npe) {
            logger.error("Error in WebApiException", npe);
        }
    }

    @Override
    protected void startBackgroundDiscovery() {
        startScan();
    }
}
