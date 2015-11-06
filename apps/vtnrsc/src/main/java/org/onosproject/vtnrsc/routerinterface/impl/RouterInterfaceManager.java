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
package org.onosproject.vtnrsc.routerinterface.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.HashMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.onosproject.vtnrsc.DefaultRouterInterfaceStore;
import org.onosproject.vtnrsc.RouterId;
import org.onosproject.vtnrsc.RouterInterface;
import org.onosproject.vtnrsc.RouterInterfaceStore;
import org.onosproject.vtnrsc.SubnetId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.exception.BadRequestExecption;
import org.onosproject.vtnrsc.router.RouterService;
import org.onosproject.vtnrsc.routerinterface.RouterInterfaceService;
import org.onosproject.vtnrsc.subnet.SubnetService;
import org.onosproject.vtnrsc.virtualport.VirtualPortService;
import org.slf4j.Logger;

/**
 * Provides implementation of the Router interface service.
 */

@Component(immediate = true)
@Service
public class RouterInterfaceManager implements RouterInterfaceService {
    private static final String ROUTRE_ID_NULL = "Router ID cannot be null";
    private static final String ROUTER_INTERFACE_NULL = "Router Interface cannot be null";
    private static final String ROUTER_INTERFACE = "vtn-router-interface-store";
    private static final String VTNRSC_APP = "org.onosproject.vtnrsc";

    private final Logger log = getLogger(getClass());

    protected EventuallyConsistentMap<RouterId, RouterInterfaceStore> routerInterfaceStoreMap;
    protected EventuallyConsistentMap<SubnetId, RouterInterface> routerInterfaceStoreSingal;
    protected ApplicationId appId;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected VirtualPortService virtualPortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected SubnetService subnetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RouterService routerService;

    @Activate
    public void activate() {
        appId = coreService.registerApplication(VTNRSC_APP);
        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(RouterId.class, TenantId.class, VirtualPortId.class,
                          RouterInterfaceStore.class, RouterInterface.class,
                          DefaultRouterInterfaceStore.class, SubnetId.class);
        routerInterfaceStoreMap = storageService
                .<RouterId, RouterInterfaceStore>eventuallyConsistentMapBuilder()
                .withName(ROUTER_INTERFACE).withSerializer(serializer)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();
        routerInterfaceStoreSingal = storageService
                .<SubnetId, RouterInterface>eventuallyConsistentMapBuilder()
                .withName(ROUTER_INTERFACE).withSerializer(serializer)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        routerInterfaceStoreMap.destroy();
        routerInterfaceStoreSingal.destroy();
        log.info("Stopped");
    }

    @Override
    public boolean exists(RouterId routerId) {
        checkNotNull(routerId, ROUTRE_ID_NULL);
        return routerInterfaceStoreMap.containsKey(routerId);
    }

    @Override
    public Iterable<RouterInterface> getRouterInterfaces() {
        return Collections
                .unmodifiableCollection(routerInterfaceStoreSingal.values());
    }

    @Override
    public RouterInterfaceStore getRouterInterfaces(RouterId routerId) {
        checkNotNull(routerId, ROUTRE_ID_NULL);
        return routerInterfaceStoreMap.get(routerId);
    }

    @Override
    public boolean addRouterInterface(RouterInterface routerInterface) {
        checkNotNull(routerInterface, ROUTER_INTERFACE_NULL);
        verifyRouterInterfaceData(routerInterface);
        if (exists(routerInterface.routerId())) {
            RouterInterfaceStore routerInterfaceStore = getRouterInterfaces(routerInterface
                    .routerId());
            routerInterfaceStore.addInterface(routerInterface.subnetId(),
                                              routerInterface);
            routerInterfaceStoreMap.put(routerInterface.routerId(),
                                        routerInterfaceStore);
        } else {
            HashMap<SubnetId, RouterInterface> interfaces = new HashMap<SubnetId, RouterInterface>();
            interfaces.put(routerInterface.subnetId(), routerInterface);
            RouterInterfaceStore routerInterfaceStore = new DefaultRouterInterfaceStore(interfaces);
            routerInterfaceStoreMap.put(routerInterface.routerId(),
                                        routerInterfaceStore);
        }
        if (!routerInterfaceStoreMap.containsKey(routerInterface.routerId())) {
            log.debug("The identified router whose identifier is {}  create failed",
                      routerInterface.routerId().toString());
            return false;
        }
        routerInterfaceStoreSingal.put(routerInterface.subnetId(),
                                       routerInterface);
        if (!routerInterfaceStoreSingal
                .containsKey(routerInterface.subnetId())) {
            log.debug("The identified subnet whose identifier is {}  create failed",
                      routerInterface.subnetId().toString());
            return false;
        }
        return true;
    }

    @Override
    public boolean removeRouterInterface(RouterInterface routerInterface) {
        checkNotNull(routerInterface, ROUTER_INTERFACE_NULL);
        if (!routerInterfaceStoreMap.containsKey(routerInterface.routerId())) {
            log.debug("The router interface is not exist whose identifier is {}",
                      routerInterface.routerId().toString());
            throw new BadRequestExecption("router ID doesn't exist");
        }
        RouterInterfaceStore routerInterfaceStore = routerInterfaceStoreMap
                .get(routerInterface.routerId());
        routerInterfaceStore.removeInterface(routerInterface.subnetId());
        if (routerInterfaceStore.interfaces().size() == 0) {
            routerInterfaceStoreMap.remove(routerInterface.routerId());
            if (routerInterfaceStoreMap.containsKey(routerInterface.routerId())) {
                log.debug("The identified router whose identifier is {}  create failed",
                          routerInterface.routerId().toString());
                return false;
            }
        } else {
            routerInterfaceStoreMap.put(routerInterface.routerId(),
                                        routerInterfaceStore);
        }
        routerInterfaceStoreSingal.remove(routerInterface.subnetId(),
                                          routerInterface);
        if (routerInterfaceStoreSingal
                .containsKey(routerInterface.subnetId())) {
            log.debug("The identified subnet whose identifier is {}  create failed",
                      routerInterface.subnetId().toString());
            return false;
        }
        return true;
    }

    @Override
    public void addListener(EventuallyConsistentMapListener<SubnetId, RouterInterface> listener) {
        routerInterfaceStoreSingal.addListener(listener);
    }

    @Override
    public void removeListener(EventuallyConsistentMapListener<SubnetId, RouterInterface> listener) {
        routerInterfaceStoreSingal.removeListener(listener);
    }

    /**
     * verify validity of Router interface data.
     *
     * @param routers router instance
     */
    private void verifyRouterInterfaceData(RouterInterface routerInterface) {

        if (!subnetService.exists(routerInterface.subnetId())) {
            log.debug("The subnet ID of interface is not exist whose identifier is {}",
                      routerInterface.subnetId().toString());
            throw new BadRequestExecption("subnet ID of interface doesn't exist");
        }
        if (!virtualPortService.exists(routerInterface.portId())) {
            log.debug("The port ID of interface is not exist whose identifier is {}",
                      routerInterface.portId().toString());
            throw new BadRequestExecption("port ID of interface doesn't exist");
        }
        if (!routerService.exists(routerInterface.routerId())) {
            log.debug("The router ID of interface is not exist whose identifier is {}",
                      routerInterface.routerId().toString());
            throw new BadRequestExecption("router ID of interface doesn't exist");
        }
    }
}
