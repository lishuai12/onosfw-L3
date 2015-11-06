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
package org.onosproject.vtn.util;

import java.util.Set;

import org.onlab.packet.IpAddress;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.BridgeConfig;
import org.onosproject.net.behaviour.BridgeName;
import org.onosproject.net.behaviour.DefaultTunnelDescription;
import org.onosproject.net.behaviour.IpTunnelEndPoint;
import org.onosproject.net.behaviour.TunnelConfig;
import org.onosproject.net.behaviour.TunnelDescription;
import org.onosproject.net.behaviour.TunnelEndPoint;
import org.onosproject.net.driver.DriverHandler;

/**
 * Applies configuration to the device.
 */
public final class VtnConfig {

    private static final String DEFAULT_BRIDGE_NAME = "br-int";

    /**
     * Constructs a vtn config object. Utility classes should not have a
     * public or default constructor, otherwise IDE will compile unsuccessfully. This
     * class should not be instantiated.
     */
    private VtnConfig() {
    }

    /**
     * Creates or update bridge in the controller device.
     *
     * @param deviceId controller-type deviceId
     */
    public static void applyBridgeConfig(DriverHandler handler, String exPortName) {
        BridgeConfig bridgeConfig = handler.behaviour(BridgeConfig.class);
        bridgeConfig.addBridge(BridgeName.bridgeName(DEFAULT_BRIDGE_NAME), exPortName);
    }

    /**
     * Creates or update tunnel in the controller device.
     *
     * @param deviceId controller-type deviceId
     * @param srcIp the ipAddress of the local controller device
     * @param dstIp the ipAddress of the remote controller device
     */
    public static void applyTunnelConfig(DriverHandler handler, IpAddress srcIp,
                                  IpAddress dstIp) {
        TunnelConfig tunnelConfig = handler.behaviour(TunnelConfig.class);
        TunnelEndPoint tunnelAsSrc = IpTunnelEndPoint.ipTunnelPoint(srcIp);
        TunnelEndPoint tunnelAsDst = IpTunnelEndPoint.ipTunnelPoint(dstIp);
        TunnelDescription tunnel = new DefaultTunnelDescription(
                                                                tunnelAsSrc,
                                                                tunnelAsDst,
                                                                TunnelDescription.Type.VXLAN,
                                                                null);
        tunnelConfig.createTunnel(tunnel);
    }

    /**
     * Creates external port in the controller device.
     *
     * @param handler DriverHandler
     * @param portName port name
     */
    public static void applyExPortConfig(DriverHandler handler, String portName) {
        BridgeConfig bridgeConfig = handler.behaviour(BridgeConfig.class);
        bridgeConfig.addPort(portName);
    }

    /**
     * Remove external port in the controller device.
     *
     * @param handler DriverHandler
     * @param portName port name
     */
    public static void removeExPortConfig(DriverHandler handler, String portName) {
        BridgeConfig bridgeConfig = handler.behaviour(BridgeConfig.class);
        bridgeConfig.deletePort(portName);
    }

    /**
     * Creates or update tunnel in the controller device.
     *
     * @param deviceId controller-type deviceId
     * @param srcIp the ipAddress of the local controller device
     * @param dstIp the ipAddress of the remote controller device
     */
    public static void removeTunnelConfig(DriverHandler handler, IpAddress srcIp,
                                   IpAddress dstIp) {
        TunnelConfig tunnelConfig = handler.behaviour(TunnelConfig.class);
        TunnelEndPoint tunnelAsSrc = IpTunnelEndPoint.ipTunnelPoint(srcIp);
        TunnelEndPoint tunnelAsDst = IpTunnelEndPoint.ipTunnelPoint(dstIp);
        TunnelDescription tunnel = new DefaultTunnelDescription(
                                                                tunnelAsSrc,
                                                                tunnelAsDst,
                                                                TunnelDescription.Type.VXLAN,
                                                                null);
        tunnelConfig.removeTunnel(tunnel);
    }

    /**
     * Gets ports in the controller device.
     *
     * @param deviceId controller-type deviceId
     */
    public static Set<PortNumber> getPortNumbers(DriverHandler handler) {
        BridgeConfig bridgeConfig = handler.behaviour(BridgeConfig.class);
        return bridgeConfig.getPortNumbers();
    }

}
