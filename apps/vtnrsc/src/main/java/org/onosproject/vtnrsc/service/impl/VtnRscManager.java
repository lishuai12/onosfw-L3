/*
 * Copyright 2015 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.vtnrsc.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.MacAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapEvent;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.store.service.LogicalClockService;
import org.onosproject.store.service.StorageService;
import org.onosproject.vtnrsc.FloatingIp;
import org.onosproject.vtnrsc.FloatingIpId;
import org.onosproject.vtnrsc.Router;
import org.onosproject.vtnrsc.RouterId;
import org.onosproject.vtnrsc.RouterInterface;
import org.onosproject.vtnrsc.SegmentationId;
import org.onosproject.vtnrsc.SubnetId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.event.VtnRscEvent;
import org.onosproject.vtnrsc.event.VtnRscEventFeedback;
import org.onosproject.vtnrsc.event.VtnRscListener;
import org.onosproject.vtnrsc.floatingip.FloatingIpService;
import org.onosproject.vtnrsc.router.RouterService;
import org.onosproject.vtnrsc.routerinterface.RouterInterfaceService;
import org.onosproject.vtnrsc.service.VtnRscService;
import org.slf4j.Logger;

import com.google.common.collect.Sets;

/**
 * Provides implementation of the VtnRsc service.
 */
@Component(immediate = true)
@Service
public class VtnRscManager implements VtnRscService {
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LogicalClockService clockService;

    private volatile boolean destroyed = false;
    private final Logger log = getLogger(getClass());
    private EventuallyConsistentMapListener<FloatingIpId, FloatingIp> floatingIpListener =
                                                                        new InnerFloatingIpListener();
    private EventuallyConsistentMapListener<RouterId, Router> routerListener = new InnerRouterListener();
    private EventuallyConsistentMapListener<SubnetId, RouterInterface> routerInterfaceListener =
                                                                        new InnerRouterInterfaceListener();
    private final Set<VtnRscListener> listeners = Sets.newCopyOnWriteArraySet();
    private EventuallyConsistentMap<TenantId, SegmentationId> l3vniMap;

    private static final String ERROR_DESTROYED = " map is already destroyed";
    private static final String RUNNELOPTOPOIC = "tunnel-ops-ids";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RouterService routerService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FloatingIpService floatingIpService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RouterInterfaceService routerInterfaceService;

    @Activate
    public void activate() {
        routerService.addListener(routerListener);
        floatingIpService.addListener(floatingIpListener);
        routerInterfaceService.addListener(routerInterfaceListener);

        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API);
        l3vniMap = storageService
                .<TenantId, SegmentationId>eventuallyConsistentMapBuilder()
                .withName("l3vniMap").withSerializer(serializer)
                .withTimestampProvider((k, v) -> clockService.getTimestamp())
                .build();
    }

    @Deactivate
    public void deactivate() {
        routerService.removeListener(routerListener);
        floatingIpService.removeListener(floatingIpListener);
        routerInterfaceService.removeListener(routerInterfaceListener);
        log.info("Stopped");
    }

    @Override
    public void addListener(VtnRscListener listener) {
        checkState(!destroyed, ERROR_DESTROYED);
        listeners.add(checkNotNull(listener));
    }

    @Override
    public void removeListener(VtnRscListener listener) {
        checkState(!destroyed, ERROR_DESTROYED);
        listeners.remove(checkNotNull(listener));
    }

    @Override
    public void destroy() {
        destroyed = true;
        listeners.clear();
    }

    @Override
    public SegmentationId getL3vni(TenantId tenantId) {
        SegmentationId l3vni = l3vniMap.get(tenantId);
        if (l3vni == null) {
            long segmentationId = coreService.getIdGenerator(RUNNELOPTOPOIC)
                    .getNewId();
            l3vni = SegmentationId.segmentationId(String
                    .valueOf(segmentationId));
            l3vniMap.put(tenantId, l3vni);
        }
        return l3vni;
    }

    private class InnerFloatingIpListener
            implements
            EventuallyConsistentMapListener<FloatingIpId, FloatingIp> {

        @Override
        public void event(EventuallyConsistentMapEvent<FloatingIpId, FloatingIp> event) {
            FloatingIp floatingIp = event.value();
            if (EventuallyConsistentMapEvent.Type.PUT == event.type()) {
                notifyListeners(new VtnRscEvent(
                                                VtnRscEvent.Type.FLOATINGIP_PUT,
                                                new VtnRscEventFeedback(
                                                                        floatingIp)));
            }
            if (EventuallyConsistentMapEvent.Type.REMOVE == event.type()) {
                notifyListeners(new VtnRscEvent(
                                                VtnRscEvent.Type.FLOATINGIP_DELETE,
                                                new VtnRscEventFeedback(
                                                                        floatingIp)));
            }
        }
    }

    private class InnerRouterListener
            implements EventuallyConsistentMapListener<RouterId, Router> {

        @Override
        public void event(EventuallyConsistentMapEvent<RouterId, Router> event) {
            Router router = event.value();
            if (EventuallyConsistentMapEvent.Type.PUT == event.type()) {
                notifyListeners(new VtnRscEvent(VtnRscEvent.Type.ROUTER_PUT,
                                                new VtnRscEventFeedback(router)));
            }
            if (EventuallyConsistentMapEvent.Type.REMOVE == event.type()) {
                notifyListeners(new VtnRscEvent(VtnRscEvent.Type.ROUTER_PUT,
                                                new VtnRscEventFeedback(router)));
            }
        }
    }

    private class InnerRouterInterfaceListener
            implements
            EventuallyConsistentMapListener<SubnetId, RouterInterface> {

        @Override
        public void event(EventuallyConsistentMapEvent<SubnetId, RouterInterface> event) {
            RouterInterface routerInterface = event.value();
            if (EventuallyConsistentMapEvent.Type.PUT == event.type()) {
                notifyListeners(new VtnRscEvent(
                                                VtnRscEvent.Type.ROUTER_INTERFACE_PUT,
                                                new VtnRscEventFeedback(
                                                                        routerInterface)));
            }
            if (EventuallyConsistentMapEvent.Type.REMOVE == event.type()) {
                notifyListeners(new VtnRscEvent(
                                                VtnRscEvent.Type.ROUTER_INTERFACE_DELETE,
                                                new VtnRscEventFeedback(
                                                                        routerInterface)));
            }
        }
    }

    private void notifyListeners(VtnRscEvent event) {
        listeners.forEach(listener -> listener.event(event));
    }

    @Override
    public Iterator<Device> getNormalOvsOfTenant(TenantId tenantId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterator<Device> getSFOvsOfTenant(TenantId tenantId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MacAddress getGatewayMac(HostId hostId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isServiceFunction(VirtualPortId portId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public DeviceId getSFToSFFMaping(VirtualPortId portId) {
        // TODO Auto-generated method stub
        return null;
    }
}
