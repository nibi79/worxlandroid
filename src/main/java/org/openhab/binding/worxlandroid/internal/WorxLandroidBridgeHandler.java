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

import static org.openhab.binding.worxlandroid.internal.WorxLandroidBindingConstants.CHANNEL_1;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.worxlandroid.internal.discovery.MowerDiscoveryService;
import org.openhab.binding.worxlandroid.internal.webapi.WebApiException;
import org.openhab.binding.worxlandroid.internal.webapi.WorxLandroidWebApiImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link WorxLandroidBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Nils - Initial contribution
 */
@NonNullByDefault
public class WorxLandroidBridgeHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(WorxLandroidBridgeHandler.class);

    private @Nullable WorxLandroidConfiguration config;
    private WorxLandroidWebApiImpl apiHandler;
    private @Nullable MowerDiscoveryService discoveryService;

    public WorxLandroidBridgeHandler(Thing thing, HttpClient httpClient) {
        super(thing);
        apiHandler = new WorxLandroidWebApiImpl(httpClient);

    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (CHANNEL_1.equals(channelUID.getId())) {
            if (command instanceof RefreshType) {
                // TODO: handle data refresh
            }

            // TODO: handle command

            // Note: if communication with thing fails for some reason,
            // indicate that by setting the status with detail information:
            // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
            // "Could not control device at IP address x.x.x.x");

        }
    }

    @Override
    public void initialize() {
        // logger.debug("Start initializing!");
        config = getConfigAs(WorxLandroidConfiguration.class);

        try {

            boolean connected = apiHandler.connect(config.getWebapiUsername(), config.getWebapiPassword());
            if (connected) {
                apiHandler.retrieveWebInfo();
                apiHandler.retrieveAwsCertificate();
                apiHandler.retrieveUserDevices();
                apiHandler.retrieveDevices();

                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Error connecting to Worx Landroid WebApi!");
            }
        } catch (WebApiException e) {
            logger.error(e.getErrorMsg());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Errorcode: " + e.getErrorCode());
        }

    }

    /**
     * @return
     */
    public WorxLandroidWebApiImpl getWorxLandroidWebApiImpl() {

        return apiHandler;
    }

    /**
     * @param discoveryService
     */
    public void setDiscovery(MowerDiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }
}