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
package org.onosproject.vtn.manager.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.app.ApplicationService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.BridgeConfig;
import org.onosproject.net.behaviour.BridgeDescription;
import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.LogicalClockService;
import org.onosproject.store.service.StorageService;
import org.onosproject.vtn.manager.VTNService;
import org.onosproject.vtn.table.ArpService;
import org.onosproject.vtn.table.ClassifierService;
import org.onosproject.vtn.table.DNatService;
import org.onosproject.vtn.table.L2ForwardService;
import org.onosproject.vtn.table.L3ForwardService;
import org.onosproject.vtn.table.SNatService;
import org.onosproject.vtn.table.impl.ArpServiceImpl;
import org.onosproject.vtn.table.impl.ClassifierServiceImpl;
import org.onosproject.vtn.table.impl.DNatServiceImpl;
import org.onosproject.vtn.table.impl.L2ForwardServiceImpl;
import org.onosproject.vtn.table.impl.L3ForwardServiceImpl;
import org.onosproject.vtn.table.impl.SNatServiceImpl;
import org.onosproject.vtn.util.VtnConfig;
import org.onosproject.vtnrsc.FixedIp;
import org.onosproject.vtnrsc.FloatingIp;
import org.onosproject.vtnrsc.Router;
import org.onosproject.vtnrsc.RouterGateway;
import org.onosproject.vtnrsc.RouterId;
import org.onosproject.vtnrsc.RouterInterface;
import org.onosproject.vtnrsc.SegmentationId;
import org.onosproject.vtnrsc.SubnetId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.TenantNetwork;
import org.onosproject.vtnrsc.TenantNetworkId;
import org.onosproject.vtnrsc.VirtualPort;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.event.VtnRscEvent;
import org.onosproject.vtnrsc.event.VtnRscEventFeedback;
import org.onosproject.vtnrsc.event.VtnRscListener;
import org.onosproject.vtnrsc.floatingip.FloatingIpService;
import org.onosproject.vtnrsc.router.RouterService;
import org.onosproject.vtnrsc.routerinterface.RouterInterfaceService;
import org.onosproject.vtnrsc.service.VtnRscService;
import org.onosproject.vtnrsc.subnet.SubnetService;
import org.onosproject.vtnrsc.tenantnetwork.TenantNetworkService;
import org.onosproject.vtnrsc.virtualport.VirtualPortService;
import org.slf4j.Logger;

import com.google.common.collect.Sets;

/**
 * Provides implementation of VTNService.
 */
@Component(immediate = true)
@Service
public class VTNManager implements VTNService {
    private final Logger log = getLogger(getClass());
    private static final String APP_ID = "org.onosproject.app.vtn";
    private ScheduledExecutorService backgroundService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ApplicationService appService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TenantNetworkService tenantNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected VirtualPortService virtualPortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LogicalClockService clockService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected SubnetService subnetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected VtnRscService vtnRscService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FloatingIpService floatingIpService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RouterService routerService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RouterInterfaceService routerInterfaceService;

    private ApplicationId appId;
    private ClassifierService classifierService;
    private L2ForwardService l2ForwardService;
    private ArpService arpService;
    private L3ForwardService l3ForwardService;
    private SNatService snatService;
    private DNatService dnatService;

    private final HostListener hostListener = new InnerHostListener();
    private final DeviceListener deviceListener = new InnerDeviceListener();
    private final VtnRscListener l3EventListener = new VtnL3EventListener();

    private static final String IFACEID = "ifaceid";
    private static final String CONTROLLER_IP_KEY = "ipaddress";
    private static final String SWITCH_CHANNEL_ID = "channelId";
    private static final String DRIVER_NAME = "onosfw";
    private static final String PORT_HEAD = "vxlan";
    private static final String EX_PORT_NAME = "eth0";
    private static final int SUBNET_NUM = 2;
    private static final String SWITCHES_OF_CONTROLLER = "switchesOfController";
    private static final String SWITCH_OF_LOCAL_HOST_PORTS = "switchOfLocalHostPorts";
    private static final String HOST_OF_NETWORK = "hostOfNetwork";
    private static final String ROUTERINF_FLAG_OF_TENANT = "routerInfFlagOfTenant";
    private static final String PIPELINES_OF_DEVICE = "pipelinesOfDevice";
    private static final String HOSTS_OF_SUBNET = "hostsOfSubnet";
    private static final String EX_PORTNUMBER_OF_DEVICE = "exPortNumberOfDevice";

    private EventuallyConsistentMap<IpAddress, InternalControllerOfSwitches> switchesOfController;
    private EventuallyConsistentMap<DeviceId, NetworkOfLocalHostPorts> switchOfLocalHostPorts;
    private EventuallyConsistentMap<HostId, HostOfNetwork> hostOfNetwork;
    private EventuallyConsistentMap<TenantId, Boolean> routerInfFlagOfTenant;
    private EventuallyConsistentMap<DeviceId, Pipeliner> pipelinesOfDevice;
    private EventuallyConsistentMap<SubnetId, Map<HostId, Host>> hostsOfSubnet;
    private EventuallyConsistentMap<DeviceId, PortNumber> exPortNumberOfDevice;

    @Activate
    public void activate() {
        appId = coreService.registerApplication(APP_ID);

        classifierService = new ClassifierServiceImpl(flowObjectiveService,
                                                      appId);
        l2ForwardService = new L2ForwardServiceImpl(flowObjectiveService,
                                                    appId);
        arpService = new ArpServiceImpl(flowObjectiveService, appId);
        l3ForwardService = new L3ForwardServiceImpl(flowObjectiveService,
                                                    appId);
        snatService = new SNatServiceImpl(flowObjectiveService, appId);
        dnatService = new DNatServiceImpl(flowObjectiveService, appId);
        deviceService.addListener(deviceListener);
        hostService.addListener(hostListener);
        vtnRscService.addListener(l3EventListener);
        backgroundService = newSingleThreadScheduledExecutor(groupedThreads("onos-apps/vtn",
                                                                            "manager-background"));
        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API);

        switchesOfController = storageService
                .<IpAddress, InternalControllerOfSwitches>eventuallyConsistentMapBuilder()
                .withName(SWITCHES_OF_CONTROLLER).withSerializer(serializer)
                .withTimestampProvider((k, v) -> clockService.getTimestamp())
                .build();

        switchOfLocalHostPorts = storageService
                .<DeviceId, NetworkOfLocalHostPorts>eventuallyConsistentMapBuilder()
                .withName(SWITCH_OF_LOCAL_HOST_PORTS).withSerializer(serializer)
                .withTimestampProvider((k, v) -> clockService.getTimestamp())
                .build();

        hostOfNetwork = storageService
                .<HostId, HostOfNetwork>eventuallyConsistentMapBuilder()
                .withName(HOST_OF_NETWORK).withSerializer(serializer)
                .withTimestampProvider((k, v) -> clockService.getTimestamp())
                .build();

        routerInfFlagOfTenant = storageService
                .<TenantId, Boolean>eventuallyConsistentMapBuilder()
                .withName(ROUTERINF_FLAG_OF_TENANT).withSerializer(serializer)
                .withTimestampProvider((k, v) -> clockService.getTimestamp())
                .build();

        pipelinesOfDevice = storageService
                .<DeviceId, Pipeliner>eventuallyConsistentMapBuilder()
                .withName(PIPELINES_OF_DEVICE).withSerializer(serializer)
                .withTimestampProvider((k, v) -> clockService.getTimestamp())
                .build();

        hostsOfSubnet = storageService
                .<SubnetId, Map<HostId, Host>>eventuallyConsistentMapBuilder()
                .withName(HOSTS_OF_SUBNET).withSerializer(serializer)
                .withTimestampProvider((k, v) -> clockService.getTimestamp())
                .build();

        exPortNumberOfDevice = storageService
                .<DeviceId, PortNumber>eventuallyConsistentMapBuilder()
                .withName(EX_PORTNUMBER_OF_DEVICE).withSerializer(serializer)
                .withTimestampProvider((k, v) -> clockService.getTimestamp())
                .build();

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        deviceService.addListener(deviceListener);
        hostService.addListener(hostListener);
        backgroundService.shutdown();
        log.info("Stopped");
    }

    @Override
    public void onControllerDetected(Device controllerDevice) {
        if (controllerDevice == null) {
            log.error("The controller device is null");
            return;
        }

        // Create br-int in controller
        String localIpAddress = controllerDevice.annotations()
                .value(CONTROLLER_IP_KEY);
        IpAddress localIp = IpAddress.valueOf(localIpAddress);
        DeviceId controllerDeviceId = controllerDevice.id();
        DriverHandler handler = driverService.createHandler(controllerDeviceId);

        if (mastershipService.isLocalMaster(controllerDeviceId)) {
            VtnConfig.applyBridgeConfig(handler, EX_PORT_NAME);
            log.info("A new ovs is created in node {}", localIp.toString());
            switchesOfController.put(localIp,
                                     new InternalControllerOfSwitches());
        }

        // Create tunnel in br-int
        Iterable<Device> devices = deviceService.getAvailableDevices();
        Sets.newHashSet(devices).stream()
                .filter(d -> Device.Type.CONTROLLER == d.type())
                .filter(d -> !controllerDeviceId.equals(d.id())).forEach(d -> {
                    DriverHandler tunHandler = driverService
                            .createHandler(d.id());
                    String remoteIpAddress = d.annotations()
                            .value(CONTROLLER_IP_KEY);
                    IpAddress remoteIp = IpAddress.valueOf(remoteIpAddress);
                    if (remoteIp.toString()
                            .equalsIgnoreCase(localIp.toString())) {
                        log.error("The localIp and remoteIp are the same");
                        return;
                    }
                    if (mastershipService.isLocalMaster(controllerDeviceId)) {
                        VtnConfig.applyTunnelConfig(handler, localIp, remoteIp);
                        log.info("Add tunnel between {} and {}", localIp,
                                 remoteIp);
                    }
                    if (mastershipService.isLocalMaster(d.id())) {
                        VtnConfig.applyTunnelConfig(tunHandler, remoteIp,
                                                    localIp);
                        log.info("Add tunnel between {} and {}", remoteIp,
                                 localIp);
                    }
                });
    }

    @Override
    public void onControllerVanished(Device controllerDevice) {
        if (controllerDevice == null) {
            log.error("The device is null");
            return;
        }
        // remove tunnel
        String dstIp = controllerDevice.annotations().value(CONTROLLER_IP_KEY);
        IpAddress dstIpAddress = IpAddress.valueOf(dstIp);
        DeviceId controllerDeviceId = controllerDevice.id();
        if (mastershipService.isLocalMaster(controllerDeviceId)) {
            switchesOfController.remove(dstIpAddress);
        }

        Iterable<Device> devices = deviceService.getAvailableDevices();
        Sets.newHashSet(devices).stream()
                .filter(d -> d.type() == Device.Type.CONTROLLER)
                .filter(d -> !controllerDeviceId.equals(d.id())).forEach(d -> {
                    DriverHandler tunHandler = driverService
                            .createHandler(d.id());
                    String srcIp = d.annotations().value(CONTROLLER_IP_KEY);
                    IpAddress srcIpAddress = IpAddress.valueOf(srcIp);
                    if (mastershipService.isLocalMaster(d.id())) {
                        VtnConfig.applyTunnelConfig(tunHandler, srcIpAddress,
                                                    dstIpAddress);
                        log.info("Remove tunnel between {} and {}",
                                 srcIpAddress, dstIpAddress);
                    }
                });
    }

    @Override
    public void onOvsDetected(Device device) {
        if (device == null) {
            log.error("The device is null");
            return;
        }
        if (!mastershipService.isLocalMaster(device.id())) {
            return;
        }

        PortNumber export = getExPort(device.id());
        exPortNumberOfDevice.put(device.id(), export);

        Iterable<Device> devices = deviceService.getAvailableDevices();
        String controllerIp = getControllerIpOfSwitch(device.id());
        if (controllerIp == null) {
            log.error("Can't find controller of device: {}",
                      device.id().toString());
            return;
        }
        IpAddress ipAddress = IpAddress.valueOf(controllerIp);
        if (!switchesOfController.containsKey(ipAddress)) {
            log.error("Can't find controller of device: {}",
                      device.id().toString());
            return;
        }

        InternalControllerOfSwitches switches = switchesOfController
                .get(ipAddress);
        switches.getDeviceIds().add(device.id());

        switchOfLocalHostPorts.put(device.id(), new NetworkOfLocalHostPorts());

        // send table-miss rules
        // Pipeliner pipeLiner = driver.createBehaviour(
        // new DefaultDriverData(driver,
        // device.id()),
        // Pipeliner.class);
        // pipeLiner.init(device.id(), context);
        // pipelinesOfDevice.put(device.id(), pipeLiner);

        DeviceId localControllerId = getControllerId(device.id(), devices);
        DriverHandler handler = driverService.createHandler(localControllerId);

        Set<PortNumber> ports = VtnConfig.getPortNumbers(handler);

        if (hostOfNetwork != null) {
            Sets.newHashSet(hostOfNetwork.keySet().iterator()).stream()
                    .forEach(hostId -> {
                        Host host = hostOfNetwork.get(hostId).getHost();
                        MacAddress hostMac = host.mac();
                        SegmentationId segmentationId = hostOfNetwork
                                .get(hostId).getNetwork().segmentationId();
                        DeviceId remoteDeviceId = host.location().deviceId();

                        String remoteControllerIp = getControllerIpOfSwitch(remoteDeviceId);
                        if (remoteControllerIp == null) {
                            log.error("Can't find remote controller of device: {}",
                                      remoteDeviceId.toString());
                            return;
                        }

                        IpAddress remoteIpAddress = IpAddress
                                .valueOf(remoteControllerIp);
                        String tunnelName = "vxlan-"
                                + remoteIpAddress.toString();

                        ports.stream()
                                .filter(p -> p.name()
                                        .equalsIgnoreCase(tunnelName))
                                .forEach(p -> {
                            l2ForwardService
                                    .programTunnelOut(device.id(),
                                                      segmentationId, p,
                                                      hostMac,
                                                      Objective.Operation.ADD);
                        });

                    });
        }
    }

    @Override
    public void onOvsVanished(Device device) {
        if (device == null) {
            log.error("The device is null");
            return;
        }
        if (!mastershipService.isLocalMaster(device.id())) {
            return;
        }

        exPortNumberOfDevice.remove(device.id());

        Iterable<Device> devices = deviceService.getAvailableDevices();
        String controllerIp = getControllerIpOfSwitch(device.id());
        if (controllerIp == null) {
            log.error("Can't find controller of device: {}",
                      device.id().toString());
            return;
        }
        IpAddress ipAddress = IpAddress.valueOf(controllerIp);
        if (!switchesOfController.containsKey(ipAddress)) {
            log.error("Can't find controller of device: {}",
                      device.id().toString());
            return;
        }
        InternalControllerOfSwitches switches = switchesOfController
                .get(ipAddress);
        switches.getDeviceIds().remove(device.id());

        switchOfLocalHostPorts.remove(device.id());

        pipelinesOfDevice.remove(device.id());

        DeviceId localControllerId = getControllerId(device.id(), devices);
        DriverHandler handler = driverService.createHandler(localControllerId);
        Set<PortNumber> ports = VtnConfig.getPortNumbers(handler);
        if (hostOfNetwork != null) {
            Sets.newHashSet(hostOfNetwork.keySet().iterator()).stream()
                    .forEach(hostId -> {
                        Host host = hostOfNetwork.get(hostId).getHost();
                        MacAddress hostMac = host.mac();
                        SegmentationId segmentationId = hostOfNetwork
                                .get(hostId).getNetwork().segmentationId();
                        DeviceId remoteDeviceId = host.location().deviceId();

                        String remoteControllerIp = getControllerIpOfSwitch(remoteDeviceId);
                        IpAddress remoteIpAddress = IpAddress
                                .valueOf(remoteControllerIp);
                        String tunnelName = "vxlan-"
                                + remoteIpAddress.toString();

                        ports.stream()
                                .filter(p -> p.name()
                                        .equalsIgnoreCase(tunnelName))
                                .forEach(p -> {
                            l2ForwardService
                                    .programTunnelOut(device.id(),
                                                      segmentationId, p,
                                                      hostMac,
                                                      Objective.Operation.REMOVE);
                        });

                    });
        }

    }

    @Override
    public void onHostDetected(Host host) {
        log.info("====onHostDetected====");
        Iterable<Device> devices = deviceService.getAvailableDevices();
        DeviceId deviceId = host.location().deviceId();
        if (!mastershipService.isLocalMaster(deviceId)) {
            return;
        }
        PortNumber inPort = host.location().port();
        MacAddress mac = host.mac();
        Iterable<Port> ports = deviceService.getPorts(deviceId);
        String controllerIp = getControllerIpOfSwitch(deviceId);
        IpAddress ipAddress = IpAddress.valueOf(controllerIp);

        String ifaceId = host.annotations().value(IFACEID);
        if (ifaceId == null) {
            log.error("The ifaceId of Host is null");
            return;
        }

        VirtualPortId virtualPortId = VirtualPortId.portId(ifaceId);
        VirtualPort virtualPort = virtualPortService.getPort(virtualPortId);
        Iterator<FixedIp> fixip = virtualPort.fixedIps().iterator();
        SubnetId subnetId = null;
        if (fixip.hasNext()) {
            subnetId = fixip.next().subnetId();
        }
        if (subnetId != null) {
            Map<HostId, Host> hosts = new ConcurrentHashMap();
            if (hostsOfSubnet.get(subnetId) != null) {
                hosts = hostsOfSubnet.get(subnetId);
            }
            hosts.put(host.id(), host);
            hostsOfSubnet.put(subnetId, hosts);
        }

        if (virtualPort == null) {
            log.error("The virtualPort of host is null");
            return;
        }

        TenantNetwork network = tenantNetworkService
                .getNetwork(virtualPort.networkId());
        if (network == null) {
            log.error("Can't find network of the host");
            return;
        }
        SegmentationId segmentationId = network.segmentationId();
        hostOfNetwork.put(host.id(), new HostOfNetwork(host, network));

        // Get all the vm in the node of the current vm and it's network
        Map<TenantNetworkId, Set<PortNumber>> localHostPorts = switchOfLocalHostPorts
                .get(deviceId).getNetworkOfLocalHostPorts();
        Set<PortNumber> networkOflocalHostPorts = localHostPorts
                .get(network.id());
        if (networkOflocalHostPorts == null) {
            networkOflocalHostPorts = new HashSet<PortNumber>();
            localHostPorts.putIfAbsent(network.id(), networkOflocalHostPorts);
        }
        networkOflocalHostPorts.add(inPort);

        Collection<PortNumber> localTunnelPorts = new ArrayList<>();
        Sets.newHashSet(ports).stream()
                .filter(p -> !p.number().equals(PortNumber.LOCAL))
                .forEach(p -> {
                    if (p.annotations().value("portName")
                            .startsWith(PORT_HEAD)) {
                        localTunnelPorts.add(p.number());
                    }
                });

        l2ForwardService.programLocalBcastRules(deviceId, segmentationId,
                                                inPort, networkOflocalHostPorts,
                                                localTunnelPorts,
                                                Objective.Operation.ADD);

        l2ForwardService.programLocalOut(deviceId, segmentationId, inPort, mac,
                                         Objective.Operation.ADD);

        l2ForwardService.programTunnelBcastRules(deviceId, segmentationId,
                                                 networkOflocalHostPorts,
                                                 localTunnelPorts,
                                                 Objective.Operation.ADD);

        programTunnelOuts(devices, ipAddress, segmentationId, mac,
                          Objective.Operation.ADD);

        classifierService.programLocalIn(deviceId, segmentationId, inPort, mac,
                                         appId, Objective.Operation.ADD);

        classifierService.programTunnelIn(deviceId, segmentationId,
                                          localTunnelPorts,
                                          Objective.Operation.ADD);

        // apply L3 openflow rules
        applyHostMonitoredL3Rules(host, Objective.Operation.ADD);

    }

    @Override
    public void onHostVanished(Host host) {
        log.info("====onHostVanished====");
        DeviceId deviceId = host.location().deviceId();
        if (!mastershipService.isLocalMaster(deviceId)) {
            return;
        }
        String ifaceId = host.annotations().value(IFACEID);
        if (ifaceId == null) {
            log.error("The ifaceId of Host is null");
            return;
        }

        VirtualPortId virtualPortId = VirtualPortId.portId(ifaceId);
        VirtualPort virtualPort = virtualPortService.getPort(virtualPortId);
        Iterator<FixedIp> fixip = virtualPort.fixedIps().iterator();
        SubnetId subnetId = null;
        if (fixip.hasNext()) {
            subnetId = fixip.next().subnetId();
        }
        if (subnetId != null) {
            Map<HostId, Host> hosts = new ConcurrentHashMap();
            if (hostsOfSubnet.get(subnetId) != null) {
                hosts = hostsOfSubnet.get(subnetId);
            }
            hosts.remove(host.id());
            if (hosts.size() != 0) {
                hostsOfSubnet.put(subnetId, hosts);
            } else {
                hostsOfSubnet.remove(subnetId);
            }
        }

        PortNumber inPort = host.location().port();
        MacAddress mac = host.mac();
        Iterable<Device> devices = deviceService.getAvailableDevices();
        Iterable<Port> ports = deviceService.getPorts(deviceId);
        String controllerIp = getControllerIpOfSwitch(deviceId);

        HostOfNetwork tmpHostOfNetwork = hostOfNetwork.remove(host.id());
        SegmentationId segmentationId = tmpHostOfNetwork.getNetwork()
                .segmentationId();
        TenantNetworkId tenantNetworkId = tmpHostOfNetwork.getNetwork().id();
        Set<PortNumber> localVmPorts = switchOfLocalHostPorts.get(deviceId)
                .getNetworkOfLocalHostPorts().get(tenantNetworkId);

        Collection<PortNumber> localTunnelPorts = new ArrayList<>();
        Sets.newHashSet(ports).stream()
                .filter(p -> !p.number().equals(PortNumber.LOCAL))
                .forEach(p -> {
                    if (p.annotations().value("portName")
                            .startsWith(PORT_HEAD)) {
                        localTunnelPorts.add(p.number());
                    }
                });

        l2ForwardService.programLocalBcastRules(deviceId, segmentationId,
                                                inPort, localVmPorts,
                                                localTunnelPorts,
                                                Objective.Operation.REMOVE);

        l2ForwardService.programLocalOut(deviceId, segmentationId, inPort, mac,
                                         Objective.Operation.REMOVE);

        localVmPorts.remove(inPort);

        l2ForwardService.programTunnelBcastRules(deviceId, segmentationId,
                                                 localVmPorts, localTunnelPorts,
                                                 Objective.Operation.REMOVE);

        programTunnelOuts(devices, IpAddress.valueOf(controllerIp),
                          segmentationId, mac, Objective.Operation.REMOVE);

        classifierService.programLocalIn(deviceId, segmentationId, inPort, mac,
                                         appId, Objective.Operation.REMOVE);

        if (localVmPorts.size() == 0) {
            classifierService.programTunnelIn(deviceId, segmentationId,
                                              localTunnelPorts,
                                              Objective.Operation.REMOVE);
        }

        if (localVmPorts.isEmpty()
                && tenantNetworkService.getNetwork(tenantNetworkId) == null) {
            switchOfLocalHostPorts.get(deviceId).getNetworkOfLocalHostPorts()
                    .remove(tenantNetworkId);
        }

        // apply L3 openflow rules
        applyHostMonitoredL3Rules(host, Objective.Operation.REMOVE);
    }

    // Used to get channelId from the device annotations.
    private String getControllerIpOfSwitch(DeviceId deviceId) {
        Device device = deviceService.getDevice(deviceId);
        String url = device.annotations().value(SWITCH_CHANNEL_ID);
        return url.substring(0, url.lastIndexOf(":"));
    }

    private DeviceId getControllerId(DeviceId deviceId,
                                     Iterable<Device> devices) {
        for (Device device : devices) {
            if (device.type() == Device.Type.CONTROLLER && device.id()
                    .toString().contains(getControllerIpOfSwitch(deviceId))) {
                return device.id();
            }
        }
        log.info("Can not find controller for device : {}", deviceId);
        return null;
    }

    private void programTunnelOuts(Iterable<Device> devices,
                                   IpAddress ipAddress,
                                   SegmentationId segmentationId,
                                   MacAddress dstMac,
                                   Objective.Operation type) {
        String tunnelName = "vxlan-" + ipAddress.toString();
        Sets.newHashSet(devices).stream()
                .filter(d -> d.type() == Device.Type.CONTROLLER).forEach(d -> {
                    DriverHandler handler = driverService.createHandler(d.id());
                    BridgeConfig bridgeConfig = handler
                            .behaviour(BridgeConfig.class);
                    Collection<BridgeDescription> bridgeDescriptions = bridgeConfig
                            .getBridges();
                    Set<PortNumber> ports = bridgeConfig.getPortNumbers();
                    Iterator<BridgeDescription> it = bridgeDescriptions
                            .iterator();
                    if (it.hasNext()) {
                        BridgeDescription sw = it.next();
                        ports.stream()
                                .filter(p -> p.name()
                                        .equalsIgnoreCase(tunnelName))
                                .forEach(p -> {
                            l2ForwardService.programTunnelOut(sw.deviceId(),
                                                              segmentationId, p,
                                                              dstMac, type);
                        });
                    }
                });
    }

    private class InnerDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            Device device = event.subject();
            if (Device.Type.CONTROLLER == device.type()
                    && DeviceEvent.Type.DEVICE_ADDED == event.type()) {
                backgroundService.execute(() -> {
                    onControllerDetected(device);
                });
            } else if (Device.Type.CONTROLLER == device.type()
                    && DeviceEvent.Type.DEVICE_AVAILABILITY_CHANGED == event
                            .type()) {
                backgroundService.execute(() -> {
                    onControllerVanished(device);
                });
            } else if (Device.Type.SWITCH == device.type()
                    && DeviceEvent.Type.DEVICE_ADDED == event.type()) {
                backgroundService.execute(() -> {
                    onOvsDetected(device);
                });
            } else if (Device.Type.SWITCH == device.type()
                    && DeviceEvent.Type.DEVICE_AVAILABILITY_CHANGED == event
                            .type()) {
                backgroundService.execute(() -> {
                    onOvsVanished(device);
                });
            } else {
                log.info("Do nothing for this device type");
            }
        }
    }

    private class InnerHostListener implements HostListener {

        @Override
        public void event(HostEvent event) {
            Host host = event.subject();
            if (HostEvent.Type.HOST_ADDED == event.type()) {
                backgroundService.execute(() -> {
                    onHostDetected(host);
                });
            } else if (HostEvent.Type.HOST_REMOVED == event.type()) {
                backgroundService.execute(() -> {
                    onHostVanished(host);
                });
            } else if (HostEvent.Type.HOST_UPDATED == event.type()) {
                backgroundService.execute(() -> {
                    onHostVanished(host);
                    onHostDetected(host);
                });
            }
        }

    }

    // Processing context for initializing pipeline driver behaviours.
    private class InternalControllerOfSwitches {
        private final Set<DeviceId> deviceIds = new HashSet<DeviceId>();

        public Set<DeviceId> getDeviceIds() {
            return deviceIds;
        }
    }

    // Processing context for initializing pipeline driver behaviours.
    private class NetworkOfLocalHostPorts {
        private final Map<TenantNetworkId, Set<PortNumber>> networkOfLocalHostPorts =
                                      new HashMap<TenantNetworkId, Set<PortNumber>>();

        public Map<TenantNetworkId, Set<PortNumber>> getNetworkOfLocalHostPorts() {
            return networkOfLocalHostPorts;
        }
    }

    // Processing context for initializing pipeline driver behaviours.
    private final class HostOfNetwork {
        private final Host host;
        private final TenantNetwork network;

        public HostOfNetwork(Host host, TenantNetwork network) {
            checkNotNull(host, "Host is not null");
            checkNotNull(network, "Network is not null");
            this.host = host;
            this.network = network;
        }

        public Host getHost() {
            return host;
        }

        public TenantNetwork getNetwork() {
            return network;
        }
    }

    private class VtnL3EventListener implements VtnRscListener {
        @Override
        public void event(VtnRscEvent event) {
            VtnRscEventFeedback l3Feedback = event.subject();
            if (VtnRscEvent.Type.ROUTER_INTERFACE_PUT == event.type()) {
                backgroundService.execute(() -> {
                    onRouterInterfaceDetected(l3Feedback);
                });
            } else
                if (VtnRscEvent.Type.ROUTER_INTERFACE_DELETE == event.type()) {
                backgroundService.execute(() -> {
                    onRouterInterfaceVanished(l3Feedback);
                });
            } else if (VtnRscEvent.Type.FLOATINGIP_PUT == event.type()) {
                backgroundService.execute(() -> {
                    programFloatingIpEvent(l3Feedback,
                                           VtnRscEvent.Type.FLOATINGIP_PUT);
                });
            } else if (VtnRscEvent.Type.FLOATINGIP_DELETE == event.type()) {
                backgroundService.execute(() -> {
                    programFloatingIpEvent(l3Feedback,
                                           VtnRscEvent.Type.FLOATINGIP_DELETE);
                });
            }
        }

    }

    @Override
    public void onRouterInterfaceDetected(VtnRscEventFeedback l3Feedback) {
        Objective.Operation operation = Objective.Operation.ADD;
        RouterInterface routerInf = l3Feedback.routerInterface();
        Iterable<RouterInterface> interfaces = routerInterfaceService
                .getRouterInterfaces();
        Set<RouterInterface> interfacesSet = Sets.newHashSet(interfaces)
                .stream().filter(r -> r.tenantId().equals(routerInf.tenantId()))
                .collect(Collectors.toSet());
        if (routerInfFlagOfTenant.get(routerInf.tenantId()) != null) {
            programRouterInterface(routerInf, operation);
        } else {
            if (interfacesSet.size() > 1) {
                programInterfacesSet(interfacesSet, operation);
            }
        }
    }

    @Override
    public void onRouterInterfaceVanished(VtnRscEventFeedback l3Feedback) {
        Objective.Operation operation = Objective.Operation.REMOVE;
        RouterInterface routerInf = l3Feedback.routerInterface();
        Iterable<RouterInterface> interfaces = routerInterfaceService
                .getRouterInterfaces();
        Set<RouterInterface> interfacesSet = Sets.newHashSet(interfaces)
                .stream().filter(r -> r.tenantId().equals(routerInf.tenantId()))
                .collect(Collectors.toSet());
        if (routerInfFlagOfTenant.get(routerInf.tenantId()) != null) {
            programRouterInterface(routerInf, operation);
            log.info("====interfacesSet size===={}", interfacesSet.size());
            if (interfacesSet.size() == 1) {
                routerInfFlagOfTenant.remove(routerInf.tenantId());
                interfacesSet.stream().forEach(r -> {
                    programRouterInterface(r, operation);
                });
            }
        }
    }

    private void programInterfacesSet(Set<RouterInterface> interfacesSet,
                                      Objective.Operation operation) {
        int subnetVmNum = 0;
        for (RouterInterface r : interfacesSet) {
            // Get all the host of the subnet
            Map<HostId, Host> hosts = hostsOfSubnet.get(r.subnetId());
            if (hosts.size() > 0) {
                subnetVmNum++;
                if (subnetVmNum >= SUBNET_NUM) {
                    routerInfFlagOfTenant.put(r.tenantId(), true);
                    interfacesSet.stream().forEach(f -> {
                        programRouterInterface(f, operation);
                    });
                    break;
                }
            }
        }
    }

    private void programRouterInterface(RouterInterface routerInf,
                                        Objective.Operation operation) {
        log.info("====programRouterInterface===={}", routerInf);
        SegmentationId l3vni = vtnRscService.getL3vni(routerInf.tenantId());
        // Get all the host of the subnet
        Map<HostId, Host> hosts = hostsOfSubnet.get(routerInf.subnetId());
        hosts.values().stream().forEach(h -> {
            applyEastWestL3Flows(h, l3vni, operation);
        });
    }

    private void applyEastWestL3Flows(Host h, SegmentationId l3vni,
                                      Objective.Operation operation) {
        if (!mastershipService.isLocalMaster(h.location().deviceId())) {
            log.debug("not master device:{}", h.location().deviceId());
            return;
        }
        String ifaceId = h.annotations().value(IFACEID);
        VirtualPort hPort = virtualPortService
                .getPort(VirtualPortId.portId(ifaceId));
        IpAddress srcIp = null;
        IpAddress srcGwIp = null;
        MacAddress srcVmGwMac = null;
        SubnetId srcSubnetId = null;
        Iterator<FixedIp> srcIps = hPort.fixedIps().iterator();
        if (srcIps.hasNext()) {
            FixedIp fixedIp = srcIps.next();
            srcIp = fixedIp.ip();
            srcSubnetId = fixedIp.subnetId();
            srcGwIp = subnetService.getSubnet(srcSubnetId).gatewayIp();
            FixedIp fixedGwIp = FixedIp.fixedIp(srcSubnetId, srcGwIp);
            srcVmGwMac = virtualPortService.getPort(fixedGwIp).macAddress();
        }
        TenantNetwork network = tenantNetworkService
                .getNetwork(hPort.networkId());
        // Classifier rules
        classifierService
                .programArpClassifierRules(h.location().deviceId(), srcGwIp,
                                           network.segmentationId(), operation);
        classifierService
                .programL3InPortClassifierRules(h.location().deviceId(),
                                                h.location().port(), h.mac(),
                                                srcVmGwMac, l3vni, operation);
        // Arp rules
        arpService.programPrivateArpRules(h.location().deviceId(), srcGwIp,
                                          network.segmentationId(), srcVmGwMac,
                                          operation);
        Iterable<Device> devices = deviceService.getAvailableDevices();
        IpAddress srcArpIp = srcIp;
        MacAddress srcArpGwMac = srcVmGwMac;
        Sets.newHashSet(devices).stream()
                .filter(d -> Device.Type.SWITCH == d.type()).forEach(d -> {
                    // L3FWD rules
                    l3ForwardService.programRouteRules(d.id(), l3vni, srcArpIp,
                                                       network.segmentationId(),
                                                       srcArpGwMac, h.mac(),
                                                       operation);
                });
    }

    @Override
    public void programFloatingIpEvent(VtnRscEventFeedback l3Feedback,
                                       VtnRscEvent.Type type) {
        FloatingIp floaingIp = l3Feedback.floatingIp();
        if (floaingIp != null) {
            VirtualPortId vmPortId = floaingIp.portId();
            VirtualPort vmPort = virtualPortService.getPort(vmPortId);
            VirtualPort fipPort = virtualPortService
                    .getPort(floaingIp.networkId(), floaingIp.floatingIp());
            Set<Host> hostSet = hostService.getHostsByMac(vmPort.macAddress());
            Host host = null;
            for (Host h : hostSet) {
                String ifaceid = h.annotations().value(IFACEID);
                if (ifaceid != null && ifaceid.equals(vmPortId.portId())) {
                    host = h;
                    break;
                }
            }
            if (host != null && vmPort != null && fipPort != null) {
                DeviceId deviceId = host.location().deviceId();
                PortNumber exPort = exPortNumberOfDevice.get(deviceId);
                SegmentationId l3vni = vtnRscService
                        .getL3vni(vmPort.tenantId());
                // Floating ip BIND
                if (type == VtnRscEvent.Type.FLOATINGIP_PUT) {
                    applyNorthSouthL3Flows(deviceId, host, vmPort, fipPort,
                                           floaingIp, l3vni, exPort,
                                           Objective.Operation.ADD);
                } else if (type == VtnRscEvent.Type.FLOATINGIP_DELETE) {
                    // Floating ip UNBIND
                    applyNorthSouthL3Flows(deviceId, host, vmPort, fipPort,
                                           floaingIp, l3vni, exPort,
                                           Objective.Operation.REMOVE);
                }
            }
        }
    }

    private void applyNorthSouthL3Flows(DeviceId deviceId, Host host,
                                        VirtualPort vmPort, VirtualPort fipPort,
                                        FloatingIp floatingIp,
                                        SegmentationId l3Vni, PortNumber exPort,
                                        Objective.Operation operation) {
        if (!mastershipService.isLocalMaster(deviceId)) {
            log.debug("not master device:{}", deviceId);
            return;
        }
        List gwIpMac = getGwIpAndMac(vmPort);
        IpAddress dstVmGwIp = (IpAddress) gwIpMac.get(0);
        MacAddress dstVmGwMac = (MacAddress) gwIpMac.get(1);
        FixedIp fixedGwIp = getGwFixedIp(floatingIp);
        MacAddress fGwMac = null;
        if (fixedGwIp != null) {
            fGwMac = virtualPortService.getPort(fixedGwIp).macAddress();
        }
        TenantNetwork vmNetwork = tenantNetworkService
                .getNetwork(vmPort.networkId());
        TenantNetwork fipNetwork = tenantNetworkService
                .getNetwork(fipPort.networkId());
        // L3 external network access to internal network
        MacAddress exPortMac = MacAddress.valueOf("08:00:27:fb:39:9b");
        MacAddress exGwMac = MacAddress.valueOf("4c:1f:cc:9d:e3:64");
        classifierService.programArpClassifierRules(deviceId, floatingIp.floatingIp(),
                                                    fipNetwork.segmentationId(),
                                                    operation);
        classifierService.programL3ExPortClassifierRules(deviceId, exPort,
                                                         floatingIp.floatingIp(), operation);
        arpService.programPublicArpRules(deviceId, floatingIp.floatingIp(),
                                         fipNetwork.segmentationId(), exPortMac,
                                         operation);
        dnatService.programDnatRules(deviceId, floatingIp.floatingIp(),
                                     fGwMac, floatingIp.fixedIp(),
                                     l3Vni, operation);
        l3ForwardService
                .programRouteRules(deviceId, l3Vni, floatingIp.fixedIp(),
                                   vmNetwork.segmentationId(), dstVmGwMac,
                                   vmPort.macAddress(), operation);
        // L3 internal network access to external network
        classifierService.programArpClassifierRules(deviceId, dstVmGwIp,
                                                    vmNetwork.segmentationId(), operation);
        classifierService.programL3InPortClassifierRules(deviceId,
                                                         host.location().port(),
                                                         host.mac(), dstVmGwMac,
                                                         l3Vni, operation);
        arpService.programPrivateArpRules(deviceId, dstVmGwIp,
                                          vmNetwork.segmentationId(), dstVmGwMac,
                                          operation);

        snatService.programSnatRules(deviceId, l3Vni, floatingIp.fixedIp(),
                                     exGwMac, exPortMac,
                                     floatingIp.floatingIp(),
                                     fipNetwork.segmentationId(), operation);
        l2ForwardService.programExternalOut(deviceId, fipNetwork.segmentationId(),
                                         exPort, exGwMac, operation);
    }

    private PortNumber getExPort(DeviceId deviceId) {
        List<Port> ports = deviceService.getPorts(deviceId);
        PortNumber exPort = null;
        for (Port port : ports) {
            String portName = port.annotations().value(AnnotationKeys.PORT_NAME);
            if (portName != null && portName.equals(EX_PORT_NAME)) {
                exPort = port.number();
                break;
            }
        }
        return exPort;
    }

    private List getGwIpAndMac(VirtualPort port) {
        List list = new ArrayList();
        MacAddress gwMac = null;
        SubnetId subnetId = null;
        IpAddress gwIp = null;
        Iterator<FixedIp> fixips = port.fixedIps().iterator();
        if (fixips.hasNext()) {
            FixedIp fixip = fixips.next();
            subnetId = fixip.subnetId();
            gwIp = subnetService.getSubnet(subnetId).gatewayIp();
            FixedIp fixedGwIp = FixedIp.fixedIp(fixip.subnetId(), gwIp);
            gwMac = virtualPortService.getPort(fixedGwIp).macAddress();
        }
        list.add(gwIp);
        list.add(gwMac);
        return list;
    }

    private FixedIp getGwFixedIp(FloatingIp floatingIp) {
        RouterId routerId = floatingIp.routerId();
        Router router = routerService.getRouter(routerId);
        RouterGateway routerGateway = router.externalGatewayInfo();
        Iterable<FixedIp> externalFixedIps = routerGateway.externalFixedIps();
        FixedIp fixedGwIp = null;
        if (externalFixedIps != null) {
            Iterator<FixedIp> exFixedIps = externalFixedIps.iterator();
            if (exFixedIps.hasNext()) {
                fixedGwIp = exFixedIps.next();
            }
        }
        return fixedGwIp;
    }

    private void applyHostMonitoredL3Rules(Host host,
                                           Objective.Operation operation) {
        String ifaceId = host.annotations().value(IFACEID);
        DeviceId deviceId = host.location().deviceId();
        VirtualPortId portId = VirtualPortId.portId(ifaceId);
        VirtualPort port = virtualPortService.getPort(portId);
        PortNumber exPort = exPortNumberOfDevice.get(deviceId);
        SegmentationId l3vni = vtnRscService.getL3vni(port.tenantId());
        Iterator<FixedIp> fixips = port.fixedIps().iterator();
        SubnetId sid = null;
        IpAddress hostIp = null;
        if (fixips.hasNext()) {
            FixedIp fixip = fixips.next();
            sid = fixip.subnetId();
            hostIp = fixip.ip();
        }
        final SubnetId subnetId = sid;
        // L3 internal network access to each other
        Iterable<RouterInterface> interfaces = routerInterfaceService
                .getRouterInterfaces();
        Set<RouterInterface> interfacesSet = Sets.newHashSet(interfaces)
                .stream().filter(r -> r.tenantId().equals(port.tenantId()))
                .collect(Collectors.toSet());
        long count = interfacesSet.stream()
                .filter(r -> !r.subnetId().equals(subnetId)).count();
        if (count > 0) {
            if (operation == Objective.Operation.ADD) {
                if (routerInfFlagOfTenant.get(port.tenantId()) != null) {
                    applyEastWestL3Flows(host, l3vni, operation);
                } else {
                    if (interfacesSet.size() > 1) {
                        programInterfacesSet(interfacesSet, operation);
                    }
                }
            } else if (operation == Objective.Operation.REMOVE) {
                if (routerInfFlagOfTenant.get(port.tenantId()) != null) {
                    applyEastWestL3Flows(host, l3vni, operation);
                }
            }
        }
        // L3 external and internal network access to each other
        FloatingIp floatingIp = null;
        Iterable<FloatingIp> floatingIps = floatingIpService.getFloatingIps();
        Set<FloatingIp> floatingIpSet = Sets.newHashSet(floatingIps).stream()
                .filter(f -> f.tenantId().equals(port.tenantId()))
                .collect(Collectors.toSet());
        for (FloatingIp f : floatingIpSet) {
            IpAddress fixedIp = f.fixedIp();
            if (fixedIp.equals(hostIp)) {
                floatingIp = f;
                break;
            }
        }
        if (floatingIp != null) {
            VirtualPort fipPort = virtualPortService
                    .getPort(floatingIp.portId());
            applyNorthSouthL3Flows(deviceId, host, port, fipPort, floatingIp,
                                   l3vni, exPort, operation);
        }
    }
}
