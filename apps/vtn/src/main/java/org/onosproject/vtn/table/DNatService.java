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
 * DNatTable interface providing the rules in DNAT table.
 */
public interface DNatService {

    /**
     * Assemble the DNAT table rules.
     *
     * @param deviceId Device Id
     * @param fIP floating ip
     * @param fGwMac floating ip gateway mac
     * @param dstVmIp destination vm ip
     * @param l3Vni the vni of L3 network
     * @param type the operation type of the flow rules
     */
    void programDnatRules(DeviceId deviceId, IpAddress fIP,
                          MacAddress fGwMac, IpAddress dstVmIp,
                          SegmentationId l3Vni, Objective.Operation type);
}
