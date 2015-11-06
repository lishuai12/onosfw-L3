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
 * SNatTable interface providing the rules in SNAT table.
 */
public interface SNatService {

    /**
     * Assemble the SNAT table rules.
     *
     * @param deviceId Device Id
     * @param l3Vni the vni of L3 network
     * @param srcIP source ip
     * @param fGwMac floating gateway mac
     * @param fMac floating mac
     * @param fIP floating ip
     * @param externalVni external network VNI
     * @param type the operation type of the flow rules
     */
    void programSnatRules(DeviceId deviceId, SegmentationId l3Vni,
                          IpAddress srcIP, MacAddress fGwMac, MacAddress fMac,
                          IpAddress fIP, SegmentationId externalVni,
                          Objective.Operation type);
}
