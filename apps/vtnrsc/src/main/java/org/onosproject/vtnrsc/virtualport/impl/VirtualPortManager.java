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
package org.onosproject.vtnrsc.virtualport.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.onosproject.vtnrsc.AllowedAddressPair;
import org.onosproject.vtnrsc.BindingHostId;
import org.onosproject.vtnrsc.DefaultVirtualPort;
import org.onosproject.vtnrsc.FixedIp;
import org.onosproject.vtnrsc.SecurityGroup;
import org.onosproject.vtnrsc.SubnetId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.TenantNetworkId;
import org.onosproject.vtnrsc.VirtualPort;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.tenantnetwork.TenantNetworkService;
import org.onosproject.vtnrsc.virtualport.VirtualPortService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides implementation of the VirtualPort APIs.
 */
@Component(immediate = true)
@Service
public class VirtualPortManager implements VirtualPortService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String VIRTUALPORT_ID_NULL = "VirtualPort ID cannot be null";
    private static final String VIRTUALPORT_NOT_NULL = "VirtualPort  cannot be null";
    private static final String TENANTID_NOT_NULL = "TenantId  cannot be null";
    private static final String FIXEDIP_NOT_NULL = "FixedIp  cannot be null";
    private static final String IP_NOT_NULL = "Ip  cannot be null";
    private static final String NETWORKID_NOT_NULL = "NetworkId  cannot be null";
    private static final String DEVICEID_NOT_NULL = "DeviceId  cannot be null";

    protected EventuallyConsistentMap<VirtualPortId, VirtualPort> vPortStore;
    protected EventuallyConsistentMap<VirtualPortId, VirtualPort> vDeletePortStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TenantNetworkService networkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Activate
    public void activate() {

        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(VirtualPortId.class,
                          TenantNetworkId.class,
                          VirtualPort.State.class,
                          TenantId.class,
                          AllowedAddressPair.class,
                          FixedIp.class,
                          BindingHostId.class,
                          SecurityGroup.class,
                          SubnetId.class,
                          IpAddress.class,
                          DefaultVirtualPort.class);

        vDeletePortStore = storageService
                .<VirtualPortId, VirtualPort>eventuallyConsistentMapBuilder()
                .withName("vDeletePortStore").withSerializer(serializer)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();

        vPortStore = storageService
                .<VirtualPortId, VirtualPort>eventuallyConsistentMapBuilder()
                .withName("vPortStore").withSerializer(serializer)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        vPortStore.clear();
        log.info("Stoppped");
    }

    @Override
    public boolean exists(VirtualPortId vPortId) {
        checkNotNull(vPortId, VIRTUALPORT_ID_NULL);
        return vPortStore.containsKey(vPortId);
    }

    @Override
    public VirtualPort getPort(VirtualPortId vPortId) {
        checkNotNull(vPortId, VIRTUALPORT_ID_NULL);
        VirtualPort vPort = vPortStore.get(vPortId);
        if (vPort == null) {
            vPort = vDeletePortStore.get(vPortId);
        }
        return vPort;
    }

    @Override
    public VirtualPort getPort(FixedIp fixedIP) {
        checkNotNull(fixedIP, FIXEDIP_NOT_NULL);
        List<VirtualPort> vPorts = new ArrayList<>();
        vPortStore.values().stream().forEach(p -> {
            Iterator<FixedIp> fixedIps = p.fixedIps().iterator();
            while (fixedIps.hasNext()) {
                if (fixedIps.next().equals(fixedIP)) {
                    vPorts.add(p);
                    break;
                }
            }
        });
        if (vPorts.size() == 0) {
            return getDeletedPort(fixedIP);
        }
        return vPorts.get(0);
    }

    @Override
    public VirtualPort getPort(TenantId tenantId, IpAddress ip) {
        checkNotNull(tenantId, TENANTID_NOT_NULL);
        checkNotNull(ip, IP_NOT_NULL);
        List<VirtualPort> vPorts = new ArrayList<>();
        vPortStore.values().stream().filter(p -> p.tenantId().equals(tenantId))
                .forEach(p -> {
                    Iterator<FixedIp> fixedIps = p.fixedIps().iterator();
                    while (fixedIps.hasNext()) {
                        if (fixedIps.next().ip().equals(ip)) {
                            vPorts.add(p);
                            break;
                        }
                    }
                });
        if (vPorts.size() == 0) {
            return getDeletedPort(tenantId, ip);
        }
        return vPorts.get(0);
    }

    @Override
    public VirtualPort getPort(TenantNetworkId networkId, IpAddress ip) {
        checkNotNull(networkId, NETWORKID_NOT_NULL);
        checkNotNull(ip, IP_NOT_NULL);
        List<VirtualPort> vPorts = new ArrayList<>();
        vPortStore.values().stream().filter(p -> p.networkId().equals(networkId))
                .forEach(p -> {
                    Iterator<FixedIp> fixedIps = p.fixedIps().iterator();
                    while (fixedIps.hasNext()) {
                        if (fixedIps.next().ip().equals(ip)) {
                            vPorts.add(p);
                            break;
                        }
                    }
                });
        if (vPorts.size() == 0) {
            return getDeletedPort(networkId, ip);
        }
        return vPorts.get(0);
    }

    private VirtualPort getDeletedPort(FixedIp fixedIP) {
        List<VirtualPort> vPorts = new ArrayList<>();
        vDeletePortStore.values().stream().forEach(p -> {
            Iterator<FixedIp> fixedIps = p.fixedIps().iterator();
            while (fixedIps.hasNext()) {
                if (fixedIps.next().equals(fixedIP)) {
                    vPorts.add(p);
                    break;
                }
            }
        });
        if (vPorts.size() == 0) {
            return null;
        }
        VirtualPort vPort = vPorts.get(0);
        return vPort;
    }

    private VirtualPort getDeletedPort(TenantId tenantId, IpAddress ip) {
        List<VirtualPort> vPorts = new ArrayList<>();
        vDeletePortStore.values().stream().filter(p -> p.tenantId().equals(tenantId))
                .forEach(p -> {
                    Iterator<FixedIp> fixedIps = p.fixedIps().iterator();
                    while (fixedIps.hasNext()) {
                        if (fixedIps.next().ip().equals(ip)) {
                            vPorts.add(p);
                            break;
                        }
                    }
                });
        if (vPorts.size() == 0) {
            return null;
        }
        VirtualPort vPort = vPorts.get(0);
        return vPort;
    }

    private VirtualPort getDeletedPort(TenantNetworkId networkId, IpAddress ip) {
        List<VirtualPort> vPorts = new ArrayList<>();
        vDeletePortStore.values().stream().filter(p -> p.networkId().equals(networkId))
                .forEach(p -> {
                    Iterator<FixedIp> fixedIps = p.fixedIps().iterator();
                    while (fixedIps.hasNext()) {
                        if (fixedIps.next().ip().equals(ip)) {
                            vPorts.add(p);
                            break;
                        }
                    }
                });
        if (vPorts.size() == 0) {
            return null;
        }
        VirtualPort vPort = vPorts.get(0);
        return vPort;
    }

    @Override
    public Collection<VirtualPort> getPorts() {
        return Collections.unmodifiableCollection(vPortStore.values());
    }

    @Override
    public Collection<VirtualPort> getPorts(TenantNetworkId networkId) {
        checkNotNull(networkId, NETWORKID_NOT_NULL);
        return vPortStore.values().stream()
                .filter(d -> d.networkId().equals(networkId))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<VirtualPort> getPorts(TenantId tenantId) {
        checkNotNull(tenantId, TENANTID_NOT_NULL);
        return vPortStore.values().stream()
                .filter(d -> d.tenantId().equals(tenantId))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<VirtualPort> getPorts(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICEID_NOT_NULL);
        return vPortStore.values().stream()
                .filter(d -> d.deviceId().equals(deviceId))
                .collect(Collectors.toList());
    }

    @Override
    public boolean createPorts(Iterable<VirtualPort> vPorts) {
        checkNotNull(vPorts, VIRTUALPORT_NOT_NULL);
        for (VirtualPort vPort : vPorts) {
            log.debug("vPortId is  {} ", vPort.portId().toString());
            vPortStore.put(vPort.portId(), vPort);
            if (!vPortStore.containsKey(vPort.portId())) {
                log.debug("The virtualPort is created failed whose identifier is {} ",
                          vPort.portId().toString());
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean updatePorts(Iterable<VirtualPort> vPorts) {
        checkNotNull(vPorts, VIRTUALPORT_NOT_NULL);
        if (vPorts != null) {
            for (VirtualPort vPort : vPorts) {
                vPortStore.put(vPort.portId(), vPort);
                if (!vPortStore.containsKey(vPort.portId())) {
                    log.debug("The virtualPort is not exist whose identifier is {}",
                              vPort.portId().toString());
                    return false;
                }

                vPortStore.put(vPort.portId(), vPort);

                if (!vPort.equals(vPortStore.get(vPort.portId()))) {
                    log.debug("The virtualPort is updated failed whose  identifier is {}",
                              vPort.portId().toString());
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean removePorts(Iterable<VirtualPortId> vPortIds) {
        checkNotNull(vPortIds, VIRTUALPORT_ID_NULL);
        if (vPortIds != null) {
            for (VirtualPortId vPortId : vPortIds) {
                vDeletePortStore.put(vPortId, vPortStore.get(vPortId));
                vPortStore.remove(vPortId);
                if (vPortStore.containsKey(vPortId)) {
                    log.debug("The virtualPort is removed failed whose identifier is {}",
                              vPortId.toString());
                    return false;
                }
            }
        }
        return true;
    }

}
