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
package org.openhab.binding.worxlandroid.internal;

import static org.openhab.binding.worxlandroid.internal.WorxLandroidBindingConstants.*;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.worxlandroid.internal.deserializer.WebApiDeserializer;
import org.openhab.binding.worxlandroid.internal.discovery.MowerDiscoveryService;
import org.openhab.binding.worxlandroid.internal.handler.WorxLandroidBridgeHandler;
import org.openhab.binding.worxlandroid.internal.handler.WorxLandroidMowerHandler;
import org.openhab.core.auth.client.oauth2.OAuthFactory;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link WorxLandroidHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Nils - Initial contribution
 * @author Gaël L'hopital - Added oAuthFactory
 */
@NonNullByDefault
@Component(configurationPid = "binding.worxlandroid", service = ThingHandlerFactory.class)
public class WorxLandroidHandlerFactory extends BaseThingHandlerFactory {
    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(THING_TYPE_MOWER, THING_TYPE_BRIDGE);

    private final Map<ThingUID, ServiceRegistration<?>> discoveryServiceRegs = new HashMap<>();
    private final HttpClient httpClient;
    private final OAuthFactory oAuthFactory;
    private final WebApiDeserializer deserializer;

    @Activate
    public WorxLandroidHandlerFactory(final @Reference HttpClientFactory httpClientFactory,
            final @Reference OAuthFactory oAuthFactory, final @Reference WebApiDeserializer deserializer) {
        this.httpClient = httpClientFactory.getCommonHttpClient();
        this.oAuthFactory = oAuthFactory;
        this.deserializer = deserializer;
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (THING_TYPE_BRIDGE.equals(thingTypeUID)) {
            WorxLandroidBridgeHandler bridgeHandler = new WorxLandroidBridgeHandler((Bridge) thing, httpClient,
                    oAuthFactory, deserializer);
            MowerDiscoveryService discoveryService = new MowerDiscoveryService(bridgeHandler);
            bridgeHandler.setDiscovery(discoveryService);
            discoveryServiceRegs.put(thing.getUID(), bundleContext.registerService(DiscoveryService.class.getName(),
                    discoveryService, new Hashtable<>()));

            return bridgeHandler;
        } else if (THING_TYPE_MOWER.equals(thingTypeUID)) {
            return new WorxLandroidMowerHandler(thing, deserializer);
        }
        return null;
    }

    @Override
    protected void removeHandler(ThingHandler handler) {
        if (handler instanceof WorxLandroidBridgeHandler) {
            ServiceRegistration<?> serviceReg = discoveryServiceRegs.remove(handler.getThing().getUID());
            if (serviceReg != null) {
                serviceReg.unregister();
            }
        }
        super.removeHandler(handler);
    }
}
