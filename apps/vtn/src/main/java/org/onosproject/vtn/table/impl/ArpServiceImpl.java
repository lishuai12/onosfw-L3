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
package org.onosproject.vtn.table.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import org.onlab.packet.EthType.EtherType;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective.Flag;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.Objective.Operation;
import org.onosproject.vtn.table.ArpService;
import org.onosproject.vtnrsc.SegmentationId;
import org.slf4j.Logger;

/**
 * ArpTable class providing the rules in ARP table.
 */
public class ArpServiceImpl implements ArpService {
    private final Logger log = getLogger(getClass());

    private static final int ARP_PRIORITY = 0xffff;
    private static final short ARP_RESPONSE = 0x2;
    private static final EtherType ARP_TYPE = EtherType.ARP;

    private final FlowObjectiveService flowObjectiveService;
    private final ApplicationId appId;

    /**
     * Construct a ArpServiceImpl object.
     *
     * @param flowObjectiveService FlowObjectiveService
     * @param appId the application id of vtn
     */
    public ArpServiceImpl(FlowObjectiveService flowObjectiveService,
                          ApplicationId appId) {
        this.flowObjectiveService = checkNotNull(flowObjectiveService,
                                                 "FlowObjectiveService can not be null");
        this.appId = checkNotNull(appId, "ApplicationId can not be null");
    }

    @Override
    public void programPublicArpRules(DeviceId deviceId, IpAddress dstFIP,
                                      SegmentationId exVni, MacAddress fMac,
                                      Operation type) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(ARP_TYPE.ethType().toShort())
                .matchArpTpa(IpPrefix.valueOf(dstFIP, 32))
                .matchTunnelId(Long.parseLong(exVni.segmentationId())).build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .moveNxmEthSrcToNxmEthDst().setEthSrc(fMac)
                .loadNxmArpOp(ARP_RESPONSE).moveNxmArpShaToNxmArpTha()
                .moveNxmArpSpaToNxmArpTpa().loadNxmArpSha(fMac)
                .loadNxmArpSpa(dstFIP).setOutput(PortNumber.IN_PORT).build();

        ForwardingObjective.Builder objective = DefaultForwardingObjective
                .builder().withTreatment(treatment).withSelector(selector)
                .fromApp(appId).withFlag(Flag.SPECIFIC)
                .withPriority(ARP_PRIORITY);
        if (type.equals(Objective.Operation.ADD)) {
            log.debug("PublicArpRules-->ADD");
            flowObjectiveService.forward(deviceId, objective.add());
        } else {
            log.debug("PublicArpRules-->REMOVE");
            flowObjectiveService.forward(deviceId, objective.remove());
        }
    }

    @Override
    public void programPrivateArpRules(DeviceId deviceId, IpAddress dstVmGwIP,
                                       SegmentationId srcVni,
                                       MacAddress dstVmGwMac, Operation type) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(ARP_TYPE.ethType().toShort())
                .matchArpTpa(IpPrefix.valueOf(dstVmGwIP, 32))
                .matchTunnelId(Long.parseLong(srcVni.segmentationId())).build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .moveNxmEthSrcToNxmEthDst().setEthSrc(dstVmGwMac)
                .loadNxmArpOp(ARP_RESPONSE).moveNxmArpShaToNxmArpTha()
                .moveNxmArpSpaToNxmArpTpa().loadNxmArpSha(dstVmGwMac)
                .loadNxmArpSpa(dstVmGwIP).setOutput(PortNumber.IN_PORT).build();

        ForwardingObjective.Builder objective = DefaultForwardingObjective
                .builder().withTreatment(treatment).withSelector(selector)
                .fromApp(appId).withFlag(Flag.SPECIFIC)
                .withPriority(ARP_PRIORITY);

        if (type.equals(Objective.Operation.ADD)) {
            log.debug("PrivateArpRules-->ADD");
            flowObjectiveService.forward(deviceId, objective.add());
        } else {
            log.debug("PrivateArpRules-->REMOVE");
            flowObjectiveService.forward(deviceId, objective.remove());
        }
    }
}
