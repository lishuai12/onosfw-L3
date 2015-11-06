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
package org.onosproject.vtnrsc.floatingip.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.onosproject.vtnrsc.DefaultFloatingIp;
import org.onosproject.vtnrsc.FloatingIp;
import org.onosproject.vtnrsc.FloatingIpId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.TenantNetworkId;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.RouterId;
import org.onosproject.vtnrsc.exception.BadRequestExecption;
import org.onosproject.vtnrsc.floatingip.FloatingIpService;
import org.onosproject.vtnrsc.router.RouterService;
import org.onosproject.vtnrsc.tenantnetwork.TenantNetworkService;
import org.onosproject.vtnrsc.virtualport.VirtualPortService;
import org.slf4j.Logger;

/**
 * Provides implementation of the FloatingIp service.
 */
@Component(immediate = true)
@Service
public class FloatingIpManager implements FloatingIpService {
    private static final String FLOATINGIP_ID_NOT_NULL = "Floatingip ID cannot be null";
    private static final String FLOATINGIP_NOT_NULL = "Floatingip cannot be null";
    private static final String FLOATINGIP = "vtn-floatingip-store";
    private static final String VTNRSC_APP = "org.onosproject.vtnrsc";

    private final Logger log = getLogger(getClass());

    protected EventuallyConsistentMap<FloatingIpId, FloatingIp> floatingIpStore;
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
    protected RouterService routerService;

    @Activate
    public void activate() {
        appId = coreService.registerApplication(VTNRSC_APP);
        KryoNamespace.Builder serializer = KryoNamespace
                .newBuilder()
                .register(KryoNamespaces.API)
                .register(FloatingIp.class, FloatingIpId.class,
                          TenantNetworkId.class, TenantId.class,
                          FloatingIp.FloatIpStatus.class, RouterId.class,
                          VirtualPortId.class, DefaultFloatingIp.class);
        floatingIpStore = storageService
                .<FloatingIpId, FloatingIp>eventuallyConsistentMapBuilder()
                .withName(FLOATINGIP).withSerializer(serializer)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        floatingIpStore.destroy();
        log.info("Stopped");
    }

    @Override
    public Iterable<FloatingIp> getFloatingIps() {
        return Collections.unmodifiableCollection(floatingIpStore.values());
    }

    @Override
    public FloatingIp getFloatingIp(FloatingIpId floatingIpId) {
        checkNotNull(floatingIpId, FLOATINGIP_ID_NOT_NULL);
        return floatingIpStore.get(floatingIpId);
    }

    @Override
    public boolean exists(FloatingIpId floatingIpId) {
        checkNotNull(floatingIpId, FLOATINGIP_ID_NOT_NULL);
        return floatingIpStore.containsKey(floatingIpId);
    }

    @Override
    public boolean floatingIpIsUsed(IpAddress floatingIpAddr,
                                    FloatingIpId floatingIpId) {
        checkNotNull(floatingIpAddr, "Floating IP address cannot be null");
        checkNotNull(floatingIpId, "Floating IP Id cannot be null");
        Iterable<FloatingIp> floatingIps = getFloatingIps();
        for (FloatingIp floatingIp : floatingIps) {
            if (floatingIp.floatingIp().equals(floatingIpAddr)
                    && !floatingIp.id().equals(floatingIpId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean fixedIpIsUsed(IpAddress fixedIpAddr, TenantId tenantId,
                                 FloatingIpId floatingIpId) {
        checkNotNull(fixedIpAddr, "Fixed IP address cannot be null");
        checkNotNull(tenantId, "Tenant Id cannot be null");
        checkNotNull(floatingIpId, "Floating IP Id cannot be null");
        Iterable<FloatingIp> floatingIps = getFloatingIps();
        for (FloatingIp floatingIp : floatingIps) {
            IpAddress fixedIp = floatingIp.fixedIp();
            if (fixedIp != null) {
                if (floatingIp.fixedIp().equals(fixedIpAddr)
                        && floatingIp.tenantId().equals(tenantId)
                        && !floatingIp.id().equals(floatingIpId)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean createFloatingIp(Iterable<FloatingIp> floatingIps) {
        checkNotNull(floatingIps, FLOATINGIP_NOT_NULL);
        boolean result = true;
        for (FloatingIp floatingIp : floatingIps) {
            if (!tenantNetworkService.exists(floatingIp.networkId())) {
                log.debug("The network identifier {} that the floating Ip {} create for is not exist",
                          floatingIp.networkId().toString(), floatingIp.id()
                                  .toString());
                throw new BadRequestExecption(
                                              "Floating network ID doesn't exist");
            }

            VirtualPortId portId = floatingIp.portId();
            if (portId != null && !virtualPortService.exists(portId)) {
                log.debug("The port identifier {} that the floating Ip {} create for is not exist",
                          floatingIp.portId().toString(), floatingIp.id()
                                  .toString());
                throw new BadRequestExecption("Port ID doesn't exist");
            }

            RouterId routerId = floatingIp.routerId();
            if (routerId != null && !routerService.exists(routerId)) {
                log.debug("The router identifier {} that the floating Ip {} create for is not exist",
                          floatingIp.routerId().toString(), floatingIp.id()
                                  .toString());
                throw new BadRequestExecption("Router ID doesn't exist");
            }

            if (floatingIpIsUsed(floatingIp.floatingIp(), floatingIp.id())) {
                log.debug("The floaing Ip {} that the floating Ip {} create for is used",
                          floatingIp.floatingIp().toString(), floatingIp.id()
                                  .toString());
                throw new BadRequestExecption("The floating IP address is used");
            }

            if (floatingIp.fixedIp() != null && fixedIpIsUsed(floatingIp.fixedIp(), floatingIp.tenantId(),
                                                              floatingIp.id())) {
                log.debug("The fixed Ip {} that the floating Ip {} create for is used",
                          floatingIp.fixedIp().toString(), floatingIp.id()
                                  .toString());
                throw new BadRequestExecption("The fixed IP address is used");
            }

            if (portId != null) {
                floatingIpStore.put(floatingIp.id(), floatingIp);
                if (!floatingIpStore.containsKey(floatingIp.id())) {
                    log.debug("The floating Ip is created failed whose identifier is {}  ",
                              floatingIp.id().toString());
                    result = false;
                }
            } else {
                FloatingIp oldFloatingIp = floatingIpStore.get(floatingIp.id());
                if (oldFloatingIp != null) {
                    floatingIpStore.remove(floatingIp.id(), oldFloatingIp);
                    if (floatingIpStore.containsKey(floatingIp.id())) {
                        log.debug("The floating Ip is created failed whose identifier is {}",
                                  floatingIp.id().toString());
                        result = false;
                    }
                }
            }
        }

        return result;
    }

    @Override
    public boolean updateFloatingIp(Iterable<FloatingIp> floatingIps) {
        checkNotNull(floatingIps, FLOATINGIP_NOT_NULL);
        boolean result = true;
        if (floatingIps != null) {
            for (FloatingIp floatingIp : floatingIps) {
//                if (!floatingIpStore.containsKey(floatingIp.id())) {
//                    log.debug("The floatingIp is not exist whose identifier is {}",
//                              floatingIp.id().toString());
//                    throw new BadRequestExecption("FloatingIP ID doesn't exist");
//                }
                if (!tenantNetworkService.exists(floatingIp.networkId())) {
                    log.debug("The network identifier {} that the floating Ip {} create for is not exist",
                              floatingIp.networkId().toString(), floatingIp
                                      .id().toString());
                    throw new BadRequestExecption(
                                                  "Floating network ID doesn't exist");
                }

                VirtualPortId portId = floatingIp.portId();
                if (portId != null && !virtualPortService.exists(portId)) {
                    log.debug("The port identifier {} that the floating Ip {} create for is not exist",
                              floatingIp.portId().toString(), floatingIp.id()
                                      .toString());
                    throw new BadRequestExecption("Port ID doesn't exist");
                }

                RouterId routerId = floatingIp.routerId();
                if (routerId != null && !routerService.exists(routerId)) {
                    log.debug("The router identifier {} that the floating Ip {} create for is not exist",
                              floatingIp.routerId().toString(), floatingIp.id()
                                      .toString());
                    throw new BadRequestExecption("Router ID doesn't exist");
                }

                if (floatingIpIsUsed(floatingIp.floatingIp(), floatingIp.id())) {
                    log.debug("The floaing Ip {} that the floating Ip {} create for is used",
                              floatingIp.floatingIp().toString(), floatingIp
                                      .id().toString());
                    throw new BadRequestExecption(
                                                  "The floating IP address is used");
                }

                if (floatingIp.fixedIp() != null && fixedIpIsUsed(floatingIp.fixedIp(), floatingIp.tenantId(),
                                  floatingIp.id())) {
                    log.debug("The fixed Ip {} that the floating Ip {} create for is used",
                              floatingIp.fixedIp().toString(), floatingIp.id()
                                      .toString());
                    throw new BadRequestExecption(
                                                  "The fixed IP address is used");
                }

                if (portId != null) {
                    floatingIpStore.put(floatingIp.id(), floatingIp);
                    if (!floatingIpStore.containsKey(floatingIp.id())) {
                        log.debug("The floating Ip is updated failed whose identifier is {}",
                                  floatingIp.id().toString());
                        result = false;
                    }
                } else {
                    FloatingIp oldFloatingIp = floatingIpStore.get(floatingIp.id());
                    if (oldFloatingIp != null) {
                        floatingIpStore.remove(floatingIp.id(), oldFloatingIp);
                        if (floatingIpStore.containsKey(floatingIp.id())) {
                            log.debug("The floating Ip is updated failed whose identifier is {}",
                                      floatingIp.id().toString());
                            result = false;
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
    public boolean removeFloatingIp(Iterable<FloatingIpId> floatingIpIds) {
        checkNotNull(floatingIpIds, FLOATINGIP_ID_NOT_NULL);
        boolean result = true;
        if (floatingIpIds != null) {
            for (FloatingIpId floatingIpId : floatingIpIds) {
                if (!floatingIpStore.containsKey(floatingIpId)) {
                    log.debug("The floatingIp is not exist whose identifier is {}",
                              floatingIpId.toString());
                    throw new BadRequestExecption("FloatingIP ID doesn't exist");
                }
                FloatingIp floatingIp = floatingIpStore.get(floatingIpId);
                if (floatingIp != null) {
                    floatingIpStore.remove(floatingIpId, floatingIp);
                    if (floatingIpStore.containsKey(floatingIpId)) {
                        log.debug("The floating Ip is deleted failed whose identifier is {}",
                                  floatingIpId.toString());
                        result = false;
                    }
                }
            }
        }
        return result;
    }

    @Override
    public void addListener(EventuallyConsistentMapListener<FloatingIpId, FloatingIp> listener) {
        floatingIpStore.addListener(listener);
    }

    @Override
    public void removeListener(EventuallyConsistentMapListener<FloatingIpId, FloatingIp> listener) {
        floatingIpStore.removeListener(listener);
    }
}
