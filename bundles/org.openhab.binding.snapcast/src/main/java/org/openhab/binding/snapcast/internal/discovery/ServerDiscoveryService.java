/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.binding.snapcast.internal.discovery;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.jmdns.ServiceInfo;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.snapcast.internal.SnapcastBindingConstants;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.mdns.MDNSDiscoveryParticipant;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.annotations.Component;

/**
 * Discovery service for snapcast servers
 *
 * @author Steffen Brandemann - Initial contribution
 */
@Component(service = MDNSDiscoveryParticipant.class, immediate = true)
public class ServerDiscoveryService implements MDNSDiscoveryParticipant {

    public ServerDiscoveryService() {
    }

    @Override
    public Set<@NonNull ThingTypeUID> getSupportedThingTypeUIDs() {
        return Collections.singleton(SnapcastBindingConstants.THING_TYPE_SERVER);
    }

    @Override
    public String getServiceType() {
        return "_snapcast-jsonrpc._tcp.local.";
    }

    @Override
    public @Nullable DiscoveryResult createResult(ServiceInfo service) {
        DiscoveryResult result = null;

        ThingUID uid = getThingUID(service);
        String label = getLabel(service);
        Map<String, Object> properties = getProperties(service);

        if (uid != null && properties != null) {
            result = DiscoveryResultBuilder.create(uid).withProperties(properties).withLabel(label).build();
        }

        return result;
    }

    @Override
    public @Nullable ThingUID getThingUID(ServiceInfo service) {
        String name = service.getName();
        if (name != null) {
            return new ThingUID(SnapcastBindingConstants.THING_TYPE_SERVER, name.replaceAll("[#\\s]", ""));
        } else {
            return null;
        }
    }

    private String getLabel(ServiceInfo service) {
        return service.getName();
    }

    private Map<String, Object> getProperties(ServiceInfo service) {
        final String[] hostAddresses = service.getHostAddresses();
        if (hostAddresses != null && hostAddresses.length > 0) {
            Map<String, Object> properties = new HashMap<>();
            properties.put(SnapcastBindingConstants.CONFIG_SERVER_HOST, hostAddresses[0]);
            properties.put(SnapcastBindingConstants.CONFIG_SERVER_PORT, service.getPort());
            return properties;
        } else {
            return null;
        }
    }
}
