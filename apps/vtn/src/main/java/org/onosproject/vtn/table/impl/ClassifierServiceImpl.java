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
import org.onlab.packet.Ethernet;
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
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective.Flag;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.Objective.Operation;
import org.onosproject.vtn.table.ClassifierService;
import org.onosproject.vtnrsc.SegmentationId;
import org.slf4j.Logger;

import com.google.common.collect.Sets;

/**
 * Provides implementation of ClassifierService.
 */
public class ClassifierServiceImpl implements ClassifierService {
    private final Logger log = getLogger(getClass());

    private static final EtherType ETH_TYPE = EtherType.ARP;
    private static final int L3_CLAFFIFIER_PRIORITY = 0xffff;
    private static final int ARP_CLAFFIFIER_PRIORITY = 60000;
    private static final int L2_CLAFFIFIER_PRIORITY = 50000;

    private final FlowObjectiveService flowObjectiveService;
    private final ApplicationId appId;

    /**
     * Constructor.
     *
     * @param flowObjectiveService FlowObjectiveService
     * @param appId the application id of vtn
     */
    public ClassifierServiceImpl(FlowObjectiveService flowObjectiveService,
                          ApplicationId appId) {
        this.flowObjectiveService = checkNotNull(flowObjectiveService,
                                                 "FlowObjectiveService can not be null");
        this.appId = checkNotNull(appId, "ApplicationId can not be null");
    }

    @Override
    public void programLocalIn(DeviceId deviceId,
                               SegmentationId segmentationId, PortNumber inPort,
                               MacAddress srcMac, ApplicationId appid,
                               Objective.Operation type) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPort(inPort).matchEthSrc(srcMac).build();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.add(Instructions
                .modTunnelId(Long.parseLong(segmentationId.toString())));
        ForwardingObjective.Builder objective = DefaultForwardingObjective
                .builder().withTreatment(treatment.build())
                .withSelector(selector).fromApp(appId).makePermanent()
                .withFlag(Flag.SPECIFIC).withPriority(L2_CLAFFIFIER_PRIORITY);
        if (type.equals(Objective.Operation.ADD)) {
            log.debug("programLocalIn-->ADD");
            flowObjectiveService.forward(deviceId, objective.add());
        } else {
            log.debug("programLocalIn-->REMOVE");
            flowObjectiveService.forward(deviceId, objective.remove());
        }
    }

    @Override
    public void programTunnelIn(DeviceId deviceId,
                                SegmentationId segmentationId,
                                Iterable<PortNumber> localTunnelPorts,
                                Objective.Operation type) {
        if (localTunnelPorts == null) {
            log.info("No tunnel port in device");
            return;
        }
        Sets.newHashSet(localTunnelPorts).stream().forEach(tp -> {
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchInPort(tp).add(Criteria.matchTunnelId(Long
                            .parseLong(segmentationId.toString())))
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .build();
            ForwardingObjective.Builder objective = DefaultForwardingObjective
                    .builder().withTreatment(treatment).withSelector(selector)
                    .fromApp(appId).makePermanent().withFlag(Flag.SPECIFIC)
                    .withPriority(L2_CLAFFIFIER_PRIORITY);
            if (type.equals(Objective.Operation.ADD)) {
                log.debug("programTunnelIn-->ADD");
                flowObjectiveService.forward(deviceId, objective.add());
            } else {
                log.debug("programTunnelIn-->REMOVE");
                flowObjectiveService.forward(deviceId, objective.remove());
            }
        });
    }

    @Override
    public void programL3ExPortClassifierRules(DeviceId deviceId,
                                               PortNumber exPort,
                                               IpAddress fIp,
                                               Operation type) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4).matchInPort(exPort)
                .matchIPDst(IpPrefix.valueOf(fIp, 32)).build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder().build();
        ForwardingObjective.Builder objective = DefaultForwardingObjective
                .builder().withTreatment(treatment).withSelector(selector)
                .fromApp(appId).withFlag(Flag.SPECIFIC)
                .withPriority(L3_CLAFFIFIER_PRIORITY);
        if (type.equals(Objective.Operation.ADD)) {
            log.debug("L3ExToInClassifierRules-->ADD");
            flowObjectiveService.forward(deviceId, objective.add());
        } else {
            log.debug("L3ExToInClassifierRules-->REMOVE");
            flowObjectiveService.forward(deviceId, objective.remove());
        }
    }

    @Override
    public void programL3InPortClassifierRules(DeviceId deviceId,
                                                 PortNumber inPort,
                                                 MacAddress srcMac,
                                                 MacAddress dstVmGwMac,
                                                 SegmentationId l3Vni,
                                                 Operation type) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPort(inPort).matchEthSrc(srcMac).matchEthDst(dstVmGwMac)
                .build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setTunnelId(Long.parseLong(l3Vni.segmentationId())).build();
        ForwardingObjective.Builder objective = DefaultForwardingObjective
                .builder().withTreatment(treatment).withSelector(selector)
                .fromApp(appId).withFlag(Flag.SPECIFIC)
                .withPriority(L3_CLAFFIFIER_PRIORITY);
        if (type.equals(Objective.Operation.ADD)) {
            log.debug("L3InternalClassifierRules-->ADD");
            flowObjectiveService.forward(deviceId, objective.add());
        } else {
            log.debug("L3InternalClassifierRules-->REMOVE");
            flowObjectiveService.forward(deviceId, objective.remove());
        }
    }

    @Override
    public void programArpClassifierRules(DeviceId deviceId,
                                          IpAddress srcGwIp,
                                          SegmentationId srcVni,
                                          Operation type) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(ETH_TYPE.ethType().toShort())
                .matchArpTpa(IpPrefix.valueOf(srcGwIp, 32)).build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setTunnelId(Long.parseLong(srcVni.segmentationId())).build();
        ForwardingObjective.Builder objective = DefaultForwardingObjective
                .builder().withTreatment(treatment).withSelector(selector)
                .fromApp(appId).withFlag(Flag.SPECIFIC)
                .withPriority(ARP_CLAFFIFIER_PRIORITY);
        if (type.equals(Objective.Operation.ADD)) {
            log.debug("ArpClassifierRules-->ADD");
            flowObjectiveService.forward(deviceId, objective.add());
        } else {
            log.debug("ArpClassifierRules-->REMOVE");
            flowObjectiveService.forward(deviceId, objective.remove());
        }
    }

}
