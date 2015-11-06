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
package org.onosproject.vtnrsc.service;

import java.util.Iterator;

import org.onlab.packet.MacAddress;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.vtnrsc.SegmentationId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.event.VtnRscListener;

/**
 * Service for interacting with the inventory of Vtn resource.
 */
public interface VtnRscService {
    /**
     * Adds the specified listener.
     *
     * @param listener
     */
    void addListener(VtnRscListener listener);

    /**
     * Remove the specified listener.
     *
     * @param listener
     */
    void removeListener(VtnRscListener listener);

    /**
     * Shuts down the map and breaks communication between different instances.
     *
     */
    void destroy();

    /**
     * return the SegmentationId of tenant.
     *
     * @param tenantId tenant identifier
     * @return SegmentationId the SegmentationId of tenant
     */
    SegmentationId getL3vni(TenantId tenantId);

    /**
     * get Normal Ovs list of the specific tenant.
     *
     * @param tenantId tenant identifier
     * @return iterable collection of Device
     */
    Iterator<Device> getNormalOvsOfTenant(TenantId tenantId);

    /**
     * get Service function Ovs list of the specific tenant.
     *
     * @param tenantId tenant identifier
     * @return iterable collection of Device
     */
    Iterator<Device> getSFOvsOfTenant(TenantId tenantId);

    /**
     * get gateway mac address of the specific host.
     *
     * @param hostId host identifier
     * @return MacAddress of host
     */
    MacAddress getGatewayMac(HostId hostId);

    /**
     * check service function or not of the specific port.
     *
     * @param portId port identifier
     * @return true or false
     */
    boolean isServiceFunction(VirtualPortId portId);

    /**
     * get device identifier mapping to the specific port.
     *
     * @param portId port identifier
     * @return device identifier
     */
    DeviceId getSFToSFFMaping(VirtualPortId portId);
}
