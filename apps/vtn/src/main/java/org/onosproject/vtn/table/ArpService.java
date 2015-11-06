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
package org.onosproject.vtn.table;

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.vtnrsc.SegmentationId;

/**
 * ArpTable interface providing the rules in ARP table.
 */
public interface ArpService {

    /**
     * Assemble the public network arp rules.
     *
     * @param deviceId Device Id
     * @param dstGwFIP destination floating gateway ip
     * @param exVni the vni of external network
     * @param fGwMac floating gateway mac
     * @param type the operation type of the flow rules
     */
    void programPublicArpRules(DeviceId deviceId, IpAddress dstGwFIP,
                               SegmentationId exVni, MacAddress fGwMac,
                               Objective.Operation type);

    /**
     * Assemble the private network arp rules.
     *
     * @param deviceId Device Id
     * @param dstVmGwIP destination vm gateway ip
     * @param srcVni the vni of the source network
     * @param dstVmGwMac destination vm gateway mac
     * @param type the operation type of the flow rules
     */
    void programPrivateArpRules(DeviceId deviceId, IpAddress dstVmGwIP,
                                SegmentationId srcVni, MacAddress dstVmGwMac,
                                Objective.Operation type);
}
