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
package org.onosproject.vtnrsc.router.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;

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
import org.onosproject.vtnrsc.DefaultRouter;
import org.onosproject.vtnrsc.FixedIp;
import org.onosproject.vtnrsc.Router;
import org.onosproject.vtnrsc.RouterGateway;
import org.onosproject.vtnrsc.RouterId;
import org.onosproject.vtnrsc.SubnetId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.TenantNetworkId;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.exception.BadRequestExecption;
import org.onosproject.vtnrsc.router.RouterService;
import org.onosproject.vtnrsc.subnet.SubnetService;
import org.onosproject.vtnrsc.tenantnetwork.TenantNetworkService;
import org.onosproject.vtnrsc.virtualport.VirtualPortService;
import org.slf4j.Logger;

/**
 * Provides implementation of the Router service.
 */
@Component(immediate = true)
@Service
public class RouterManager
        implements RouterService {

    private static final String ROUTER_ID_NULL = "Router ID cannot be null";
    private static final String ROUTER_NOT_NULL = "Router cannot be null";
    private static final String ROUTER = "vtn-router-store";
    private static final String VTNRSC_APP = "org.onosproject.vtnrsc";

    private final Logger log = getLogger(getClass());

    protected EventuallyConsistentMap<RouterId, Router> routerStore;
    protected ApplicationId appId;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TenantNetworkService tenantNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected VirtualPortService virtualPortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected SubnetService subnetService;

    @Activate
    public void activate() {
        appId = coreService.registerApplication(VTNRSC_APP);
        KryoNamespace.Builder serializer = KryoNamespace
                .newBuilder()
                .register(KryoNamespaces.API)
                .register(Router.class, RouterId.class, DefaultRouter.class,
                          TenantNetworkId.class, TenantId.class,
                          VirtualPortId.class, DefaultRouter.class,
                          RouterGateway.class, Router.RouterStatus.class,
                          SubnetId.class);
        routerStore = storageService
                .<RouterId, Router>eventuallyConsistentMapBuilder()
                .withName(ROUTER).withSerializer(serializer)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        routerStore.destroy();
        log.info("Stopped");
    }

    @Override
    public boolean exists(RouterId routerId) {
        checkNotNull(routerId, ROUTER_ID_NULL);
        return routerStore.containsKey(routerId);
    }

    @Override
    public Iterable<Router> getRouters() {
        return Collections.unmodifiableCollection(routerStore.values());
    }

    @Override
    public Router getRouter(RouterId routerId) {
        checkNotNull(routerId, ROUTER_ID_NULL);
        return routerStore.get(routerId);
    }

    @Override
    public boolean createRouters(Iterable<Router> routers) {
        checkNotNull(routers, ROUTER_NOT_NULL);
        for (Router router : routers) {
            verifyRouterData(router);
            routerStore.put(router.routerId(), router);
            if (!routerStore.containsKey(router.routerId())) {
                log.debug("The identified router whose identifier is {}  create failed",
                          router.routerId().toString());
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean updateRouters(Iterable<Router> routers) {
        checkNotNull(routers, ROUTER_NOT_NULL);
        if (routers != null) {
            for (Router router : routers) {
                if (!routerStore.containsKey(router.routerId())) {
                    log.debug("The routers is not exist whose identifier is {}",
                              router.routerId().toString());
                    throw new BadRequestExecption("routers ID doesn't exist");
                }
                verifyRouterData(router);
                routerStore.put(router.routerId(), router);
                if (!router.equals(routerStore.get(router.routerId()))) {
                    log.debug("The router is updated failed whose identifier is {}",
                              router.routerId().toString());
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean removeRouters(Iterable<RouterId> routerIds) {
        checkNotNull(routerIds, ROUTER_ID_NULL);
        if (routerIds != null) {
            for (RouterId routerId : routerIds) {
                if (!routerStore.containsKey(routerId)) {
                    log.debug("The router is not exist whose identifier is {}",
                              routerId.toString());
                    throw new BadRequestExecption("router ID doesn't exist");
                }
                Router router = routerStore.get(routerId);
                routerStore.remove(routerId, router);
                if (routerStore.containsKey(routerId)) {
                    log.debug("The router deleted is failed whose identifier is {}",
                              routerId.toString());
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void addListener(EventuallyConsistentMapListener<RouterId, Router> listener) {
        routerStore.addListener(listener);
    }

    @Override
    public void removeListener(EventuallyConsistentMapListener<RouterId, Router> listener) {
        routerStore.removeListener(listener);
    }

    /**
     * verify validity of Router data.
     *
     * @param routers router instance
     */
    private void verifyRouterData(Router routers) {

        if (routers.gatewayPortid() != null
                && !virtualPortService.exists(routers.gatewayPortid())) {
            log.debug("The gateway port ID is not exist whose identifier is {}",
                      routers.gatewayPortid().toString());
            throw new BadRequestExecption("gateway port ID doesn't exist");
        }

        if (routers.externalGatewayInfo() != null) {
            RouterGateway routerGateway = routers.externalGatewayInfo();
            if (!tenantNetworkService.exists(routerGateway.networkId())) {
                log.debug("The network ID of gateway info is not exist whose identifier is {}",
                          routers.routerId().toString());
                throw new BadRequestExecption(
                                              "network ID of gateway info doesn't exist");
            }
            Iterable<FixedIp> fixedIps = routerGateway.externalFixedIps();
            for (FixedIp fixedIp : fixedIps) {
                if (!subnetService.exists(fixedIp.subnetId())) {
                    log.debug("The subnet ID of gateway info is not exist whose identifier is {}",
                              routers.routerId().toString());
                    throw new BadRequestExecption(
                                                  "subnet ID of gateway info doesn't exist");
                }
            }
        }
    }
}
