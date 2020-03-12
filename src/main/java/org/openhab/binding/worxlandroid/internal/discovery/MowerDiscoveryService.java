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

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.openhab.binding.worxlandroid.internal.WorxLandroidBindingConstants;
import org.openhab.binding.worxlandroid.internal.WorxLandroidBridgeHandler;
import org.openhab.binding.worxlandroid.internal.webapi.WorxLandroidWebApiImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

            // CameraResponse response = apiHandler.getApiCamera().listCameras();
            //
            // if (response.isSuccess()) {
            // JsonArray cameras = response.getCameras();
            //
            // ThingUID bridgeUID = bridgeHandler.getThing().getUID();
            //
            // if (cameras != null) {
            // for (JsonElement camera : cameras) {
            //
            // if (camera.isJsonObject()) {
            // JsonObject cam = camera.getAsJsonObject();
            //
            // String cameraId = cam.get("id").getAsString();
            //
            // CameraResponse cameraDetails = apiHandler.getApiCamera().getInfo(cameraId);
            //
            // ThingUID thingUID = new ThingUID(THING_TYPE_CAMERA, bridgeUID, cameraId);
            //
            // Map<String, Object> properties = cameraDetails.getCameraProperties(cameraId);
            //
            // DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID)
            // .withProperties(properties).withBridge(bridgeHandler.getThing().getUID())
            // .withLabel(cam.get("name").getAsString()).build();
            //
            // thingDiscovered(discoveryResult);
            //
            // logger.debug("Discovered a camera thing with ID '{}'", cameraId);
            // }
            // }
            // }
            // }
            //
            // } catch (WebApiException e) {
            // if (e.getErrorCode() == WebApiAuthErrorCodes.INSUFFICIENT_USER_PRIVILEGE.getCode()) {
            // logger.debug("Discovery Thread; Wrong/expired credentials");
            // try {
            // bridgeHandler.reconnect(false);
            // } catch (WebApiException ee) {
            // logger.error("Discovery Thread; Attempt to reconnect failed");
            // }
            // }
        } catch (Exception npe) {
            logger.error("Error in WebApiException", npe);
        }
    }

    @Override
    protected void startBackgroundDiscovery() {
        startScan();
    }

}