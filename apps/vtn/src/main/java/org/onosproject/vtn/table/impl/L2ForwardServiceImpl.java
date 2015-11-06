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

import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective.Flag;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.vtn.table.L2ForwardService;
import org.onosproject.vtnrsc.SegmentationId;
import org.slf4j.Logger;

import com.google.common.collect.Sets;

/**
 * Provides implementation of L2ForwardService.
 */
public final class L2ForwardServiceImpl implements L2ForwardService {
    private final Logger log = getLogger(getClass());

    private static final int MAC_PRIORITY = 0xffff;

    private final FlowObjectiveService flowObjectiveService;
    private final ApplicationId appId;

    /**
     * Constructor.
     *
     * @param flowObjectiveService FlowObjectiveService
     * @param appId the application id of vtn
     */
    public L2ForwardServiceImpl(FlowObjectiveService flowObjectiveService,
                          ApplicationId appId) {
        this.flowObjectiveService = checkNotNull(flowObjectiveService,
                                                 "FlowObjectiveService can not be null");
        this.appId = checkNotNull(appId, "ApplicationId can not be null");
    }

    @Override
    public void programLocalBcastRules(DeviceId deviceId,
                                       SegmentationId segmentationId,
                                       PortNumber inPort,
                                       Iterable<PortNumber> localVmPorts,
                                       Iterable<PortNumber> localTunnelPorts,
                                       Objective.Operation type) {
        if (localVmPorts == null || localTunnelPorts == null) {
            log.info("No other host port and tunnel in the device");
            return;
        }
        Sets.newHashSet(localVmPorts).stream().forEach(lp -> {
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchInPort(lp).matchEthDst(MacAddress.BROADCAST)
                    .add(Criteria.matchTunnelId(Long
                            .parseLong(segmentationId.toString())))
                    .build();
            TrafficTreatment.Builder treatment = DefaultTrafficTreatment
                    .builder();
            boolean flag = false;
            for (PortNumber outPort : localVmPorts) {
                flag = true;
                if (outPort != lp) {
                    treatment.setOutput(outPort);
                }
            }
            if (type.equals(Objective.Operation.REMOVE) && inPort == lp) {
                flag = false;
            }
            for (PortNumber outport : localTunnelPorts) {
                treatment.setOutput(outport);
            }
            ForwardingObjective.Builder objective = DefaultForwardingObjective
                    .builder().withTreatment(treatment.build())
                    .withSelector(selector).fromApp(appId).makePermanent()
                    .withFlag(Flag.SPECIFIC).withPriority(MAC_PRIORITY);
            if (flag) {
                flowObjectiveService.forward(deviceId, objective.add());
            } else {
                flowObjectiveService.forward(deviceId, objective.remove());
            }
        });
    }

    @Override
    public void programTunnelBcastRules(DeviceId deviceId,
                                        SegmentationId segmentationId,
                                        Iterable<PortNumber> localVmPorts,
                                        Iterable<PortNumber> localTunnelPorts,
                                        Objective.Operation type) {
        if (localVmPorts == null || localTunnelPorts == null) {
            log.info("No other host port or tunnel ports in the device");
            return;
        }
        Sets.newHashSet(localTunnelPorts).stream().forEach(tp -> {
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchInPort(tp)
                    .add(Criteria.matchTunnelId(Long
                            .parseLong(segmentationId.toString())))
                    .matchEthDst(MacAddress.BROADCAST).build();
            TrafficTreatment.Builder treatment = DefaultTrafficTreatment
                    .builder();

            for (PortNumber outPort : localVmPorts) {
                treatment.setOutput(outPort);
            }

            ForwardingObjective.Builder objective = DefaultForwardingObjective
                    .builder().withTreatment(treatment.build())
                    .withSelector(selector).fromApp(appId).makePermanent()
                    .withFlag(Flag.SPECIFIC).withPriority(MAC_PRIORITY);
            if (type.equals(Objective.Operation.ADD)) {
                if (Sets.newHashSet(localVmPorts).size() == 0) {
                    flowObjectiveService.forward(deviceId, objective.remove());
                } else {
                    flowObjectiveService.forward(deviceId, objective.add());
                }
            } else {
                flowObjectiveService.forward(deviceId, objective.remove());
            }
        });
    }

    @Override
    public void programLocalOut(DeviceId deviceId,
                                SegmentationId segmentationId,
                                PortNumber outPort, MacAddress sourceMac,
                                Objective.Operation type) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchTunnelId(Long.parseLong(segmentationId.toString()))
                .matchEthDst(sourceMac).build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(outPort).build();
        ForwardingObjective.Builder objective = DefaultForwardingObjective
                .builder().withTreatment(treatment).withSelector(selector)
                .fromApp(appId).withFlag(Flag.SPECIFIC)
                .withPriority(MAC_PRIORITY);
        if (type.equals(Objective.Operation.ADD)) {
            flowObjectiveService.forward(deviceId, objective.add());
        } else {
            flowObjectiveService.forward(deviceId, objective.remove());
        }

    }

    @Override
    public void programTunnelOut(DeviceId deviceId,
                                 SegmentationId segmentationId,
                                 PortNumber tunnelOutPort, MacAddress dstMac,
                                 Objective.Operation type) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthDst(dstMac).add(Criteria.matchTunnelId(Long
                        .parseLong(segmentationId.toString())))
                .build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(tunnelOutPort).build();
        ForwardingObjective.Builder objective = DefaultForwardingObjective
                .builder().withTreatment(treatment).withSelector(selector)
                .fromApp(appId).withFlag(Flag.SPECIFIC)
                .withPriority(MAC_PRIORITY);
        log.info("====dstMac===={}", dstMac);
        log.info("====segmentationId===={}", segmentationId);
        log.info("====tunnelOutPort===={}", tunnelOutPort);
        if (type.equals(Objective.Operation.ADD)) {
            flowObjectiveService.forward(deviceId, objective.add());
        } else {
            flowObjectiveService.forward(deviceId, objective.remove());
        }

    }

    @Override
    public void programExternalOut(DeviceId deviceId,
                                SegmentationId segmentationId,
                                PortNumber outPort, MacAddress sourceMac,
                                Objective.Operation type) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchTunnelId(Long.parseLong(segmentationId.toString()))
                .matchEthDst(sourceMac).build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(outPort).build();
        ForwardingObjective.Builder objective = DefaultForwardingObjective
                .builder().withTreatment(treatment).withSelector(selector)
                .fromApp(appId).withFlag(Flag.SPECIFIC)
                .withPriority(MAC_PRIORITY);
        if (type.equals(Objective.Operation.ADD)) {
            flowObjectiveService.forward(deviceId, objective.add());
        } else {
            flowObjectiveService.forward(deviceId, objective.remove());
        }

    }

}
