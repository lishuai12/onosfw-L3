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
package org.onosproject.driver.pipeline;

import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.DefaultGroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.NextGroup;
import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.behaviour.PipelinerContext;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModEtherInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModVlanIdInstruction;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveStore;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupEvent;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.group.GroupListener;
import org.onosproject.net.group.GroupService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.slf4j.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalNotification;

/**
 * Driver for Broadcom's OF-DPA v1.0 TTP.
 *
 */
public class OFDPA1Pipeline extends AbstractHandlerBehaviour implements Pipeliner {

    protected static final int PORT_TABLE = 0;
    protected static final int VLAN_TABLE = 10;
    protected static final int TMAC_TABLE = 20;
    protected static final int UNICAST_ROUTING_TABLE = 30;
    protected static final int MULTICAST_ROUTING_TABLE = 40;
    protected static final int BRIDGING_TABLE = 50;
    protected static final int ACL_TABLE = 60;
    protected static final int MAC_LEARNING_TABLE = 254;

    private static final int HIGHEST_PRIORITY = 0xffff;
    private static final int DEFAULT_PRIORITY = 0x8000;
    protected static final int LOWEST_PRIORITY = 0x0;

    /*
     * Group keys are normally generated by using the next Objective id. In the
     * case of a next objective resulting in a group chain, each group derives a
     * group key from the next objective id in the following way:
     * The upper 4 bits of the group-key are used to denote the position of the
     * group in the group chain. For example, in the chain
     * group0 --> group1 --> group2 --> port
     * group0's group key would have the upper 4 bits as 0, group1's upper four
     * bits would be 1, and so on
     */
    private static final int GROUP0MASK = 0x0;
    private static final int GROUP1MASK = 0x10000000;

    /*
     * OFDPA requires group-id's to have a certain form.
     * L2 Interface Groups have <4bits-0><12bits-vlanid><16bits-portid>
     * L3 Unicast Groups have <4bits-2><28bits-index>
     */
    private static final int L2INTERFACEMASK = 0x0;
    private static final int L3UNICASTMASK = 0x20000000;

    private final Logger log = getLogger(getClass());
    private ServiceDirectory serviceDirectory;
    protected FlowRuleService flowRuleService;
    private CoreService coreService;
    private GroupService groupService;
    private FlowObjectiveStore flowObjectiveStore;
    protected DeviceId deviceId;
    protected ApplicationId driverId;

    private KryoNamespace appKryo = new KryoNamespace.Builder()
        .register(KryoNamespaces.API)
        .register(GroupKey.class)
        .register(DefaultGroupKey.class)
        .register(OfdpaGroupChain.class)
        .register(byte[].class)
        .build();

    private Cache<GroupKey, OfdpaGroupChain> pendingNextObjectives;
    private ConcurrentHashMap<GroupKey, GroupChainElem> pendingGroups;

    private ScheduledExecutorService groupChecker =
            Executors.newScheduledThreadPool(2, groupedThreads("onos/pipeliner",
                                                               "ofdpa1-%d"));

    @Override
    public void init(DeviceId deviceId, PipelinerContext context) {
        this.serviceDirectory = context.directory();
        this.deviceId = deviceId;

        pendingNextObjectives = CacheBuilder.newBuilder()
                .expireAfterWrite(20, TimeUnit.SECONDS)
                .removalListener((RemovalNotification<GroupKey, OfdpaGroupChain> notification) -> {
                    if (notification.getCause() == RemovalCause.EXPIRED) {
                        fail(notification.getValue().nextObjective(),
                             ObjectiveError.GROUPINSTALLATIONFAILED);
                    }
                }).build();

        groupChecker.scheduleAtFixedRate(new GroupChecker(), 0, 500, TimeUnit.MILLISECONDS);
        pendingGroups = new ConcurrentHashMap<GroupKey, GroupChainElem>();

        coreService = serviceDirectory.get(CoreService.class);
        flowRuleService = serviceDirectory.get(FlowRuleService.class);
        groupService = serviceDirectory.get(GroupService.class);
        flowObjectiveStore = context.store();

        groupService.addListener(new InnerGroupListener());

        driverId = coreService.registerApplication(
                "org.onosproject.driver.OFDPA1Pipeline");

        initializePipeline();

    }

    @Override
    public void filter(FilteringObjective filteringObjective) {
        if (filteringObjective.type() == FilteringObjective.Type.PERMIT) {
            processFilter(filteringObjective,
                          filteringObjective.op() == Objective.Operation.ADD,
                          filteringObjective.appId());
        } else {
            fail(filteringObjective, ObjectiveError.UNSUPPORTED);
        }
    }

    @Override
    public void forward(ForwardingObjective fwd) {
        Collection<FlowRule> rules;
        FlowRuleOperations.Builder flowOpsBuilder = FlowRuleOperations.builder();

        rules = processForward(fwd);
        switch (fwd.op()) {
            case ADD:
                rules.stream()
                        .filter(rule -> rule != null)
                        .forEach(flowOpsBuilder::add);
                break;
            case REMOVE:
                rules.stream()
                        .filter(rule -> rule != null)
                        .forEach(flowOpsBuilder::remove);
                break;
            default:
                fail(fwd, ObjectiveError.UNKNOWN);
                log.warn("Unknown forwarding type {}", fwd.op());
        }


        flowRuleService.apply(flowOpsBuilder.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                pass(fwd);
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                fail(fwd, ObjectiveError.FLOWINSTALLATIONFAILED);
            }
        }));

    }

    @Override
    public void next(NextObjective nextObjective) {
        switch (nextObjective.type()) {
        case SIMPLE:
            Collection<TrafficTreatment> treatments = nextObjective.next();
            if (treatments.size() != 1) {
                log.error("Next Objectives of type Simple should only have a "
                        + "single Traffic Treatment. Next Objective Id:{}", nextObjective.id());
               fail(nextObjective, ObjectiveError.BADPARAMS);
               return;
            }
            processSimpleNextObjective(nextObjective);
            break;
        case HASHED:
        case BROADCAST:
        case FAILOVER:
            fail(nextObjective, ObjectiveError.UNSUPPORTED);
            log.warn("Unsupported next objective type {}", nextObjective.type());
            break;
        default:
            fail(nextObjective, ObjectiveError.UNKNOWN);
            log.warn("Unknown next objective type {}", nextObjective.type());
        }
    }

    /**
     * As per OFDPA 1.0 TTP, filtering of VLAN ids, MAC addresses (for routing)
     * and IP addresses configured on switch ports happen in different tables.
     * Note that IP filtering rules need to be added to the ACL table, as there
     * is no mechanism to send to controller via IP table.
     *
     * @param filt
     * @param install
     * @param applicationId
     */
    private void processFilter(FilteringObjective filt,
                               boolean install, ApplicationId applicationId) {
        // This driver only processes filtering criteria defined with switch
        // ports as the key
        PortCriterion p = null; EthCriterion e = null; VlanIdCriterion v = null;
        Collection<IPCriterion> ips = new ArrayList<IPCriterion>();
        if (!filt.key().equals(Criteria.dummy()) &&
                filt.key().type() == Criterion.Type.IN_PORT) {
            p = (PortCriterion) filt.key();
        } else {
            log.warn("No key defined in filtering objective from app: {}. Not"
                    + "processing filtering objective", applicationId);
            fail(filt, ObjectiveError.UNKNOWN);
            return;
        }
        // convert filtering conditions for switch-intfs into flowrules
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        for (Criterion c : filt.conditions()) {
            if (c.type() == Criterion.Type.ETH_DST) {
                e = (EthCriterion) c;
            } else if (c.type() == Criterion.Type.VLAN_VID) {
                v = (VlanIdCriterion) c;
            } else if (c.type() == Criterion.Type.IPV4_DST) {
                ips.add((IPCriterion) c);
            } else {
                log.error("Unsupported filter {}", c);
                fail(filt, ObjectiveError.UNSUPPORTED);
                return;
            }
        }

        log.debug("adding VLAN filtering rule in VLAN table: {}", e.mac());
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        selector.matchInPort(p.port());
        selector.matchVlanId(v.vlanId());
        treatment.transition(TMAC_TABLE);
        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DEFAULT_PRIORITY)
                .fromApp(applicationId)
                .makePermanent()
                .forTable(VLAN_TABLE).build();
        ops =  ops.add(rule);

        log.debug("adding MAC filtering rules in TMAC table: {}", e.mac());
        selector = DefaultTrafficSelector.builder();
        treatment = DefaultTrafficTreatment.builder();
        selector.matchInPort(p.port());
        selector.matchVlanId(v.vlanId());
        selector.matchEthType(Ethernet.TYPE_IPV4);
        selector.matchEthDst(e.mac());
        treatment.transition(UNICAST_ROUTING_TABLE);
        rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DEFAULT_PRIORITY)
                .fromApp(applicationId)
                .makePermanent()
                .forTable(TMAC_TABLE).build();
        ops = ops.add(rule);

        log.debug("adding IP filtering rules in ACL table");
        for (IPCriterion ipaddr : ips) {
            selector = DefaultTrafficSelector.builder();
            treatment = DefaultTrafficTreatment.builder();
            selector.matchEthType(Ethernet.TYPE_IPV4);
            selector.matchIPDst(ipaddr.ip());
            treatment.setOutput(PortNumber.CONTROLLER);
            rule = DefaultFlowRule.builder()
                    .forDevice(deviceId)
                    .withSelector(selector.build())
                    .withTreatment(treatment.build())
                    .withPriority(HIGHEST_PRIORITY)
                    .fromApp(applicationId)
                    .makePermanent()
                    .forTable(ACL_TABLE).build();
            ops =  ops.add(rule);
        }

        ops = install ? ops.add(rule) : ops.remove(rule);
        // apply filtering flow rules
        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Applied filtering rules");
                pass(filt);
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("Failed to apply filtering rules");
                fail(filt, ObjectiveError.FLOWINSTALLATIONFAILED);
            }
        }));

    }


    /**
     * As per the OFDPA 1.0 TTP, packets are sent out of ports by using
     * a chain of groups, namely an L3 Unicast Group that points to an L2 Interface
     * Group which in turns points to an output port. The Next Objective passed
     * in by the application has to be broken up into a group chain
     * to satisfy this TTP.
     *
     * @param nextObj  the nextObjective of type SIMPLE
     */
    private void processSimpleNextObjective(NextObjective nextObj) {
        // break up simple next objective to GroupChain objects
        TrafficTreatment treatment = nextObj.next().iterator().next();
        // for the l2interface group, get vlan and port info
        // for the l3unicast group, get the src/dst mac and vlan info
        TrafficTreatment.Builder l3utt = DefaultTrafficTreatment.builder();
        TrafficTreatment.Builder l2itt = DefaultTrafficTreatment.builder();
        VlanId vlanid = null;
        long portNum = 0;
        for (Instruction ins : treatment.allInstructions()) {
            if (ins.type() == Instruction.Type.L2MODIFICATION) {
                L2ModificationInstruction l2ins = (L2ModificationInstruction) ins;
                switch (l2ins.subtype()) {
                case ETH_DST:
                    l3utt.setEthDst(((ModEtherInstruction) l2ins).mac());
                    break;
                case ETH_SRC:
                    l3utt.setEthSrc(((ModEtherInstruction) l2ins).mac());
                    break;
                case VLAN_ID:
                    vlanid = ((ModVlanIdInstruction) l2ins).vlanId();
                    l3utt.setVlanId(vlanid);
                    break;
                case DEC_MPLS_TTL:
                case MPLS_LABEL:
                case MPLS_POP:
                case MPLS_PUSH:
                case VLAN_PCP:
                case VLAN_POP:
                case VLAN_PUSH:
                default:
                    break;
                }
            } else if (ins.type() == Instruction.Type.OUTPUT) {
                portNum = ((OutputInstruction) ins).port().toLong();
                l2itt.add(ins);
            } else {
                log.warn("Driver does not handle this type of TrafficTreatment"
                        + " instruction in nextObjectives:  {}", ins.type());
            }
        }

        // assemble information for ofdpa l2interface group
        int l2gk = nextObj.id() | GROUP1MASK;
        final GroupKey l2groupkey = new DefaultGroupKey(appKryo.serialize(l2gk));
        Integer l2groupId = L2INTERFACEMASK | (vlanid.toShort() << 16) | (int) portNum;

        // assemble information for ofdpa l3unicast group
        int l3gk = nextObj.id() | GROUP0MASK;
        final GroupKey l3groupkey = new DefaultGroupKey(appKryo.serialize(l3gk));
        Integer l3groupId = L3UNICASTMASK | (int) portNum;
        l3utt.group(new DefaultGroupId(l2groupId));
        GroupChainElem gce = new GroupChainElem(l3groupkey, l3groupId,
                                                l3utt.build(), nextObj.appId());

        // create object for local and distributed storage
        List<GroupKey> gkeys = new ArrayList<GroupKey>();
        gkeys.add(l3groupkey); // group0 in chain
        gkeys.add(l2groupkey); // group1 in chain
        OfdpaGroupChain ofdpaGrp = new OfdpaGroupChain(gkeys, nextObj);

        // store l2groupkey with the groupChainElem for the l3group that depends on it
        pendingGroups.put(l2groupkey, gce);

        // store l3groupkey with the ofdpaGroupChain for the nextObjective that depends on it
        pendingNextObjectives.put(l3groupkey, ofdpaGrp);

        // create group description for the ofdpa l2interfacegroup and send to groupservice
        GroupBucket bucket =
                DefaultGroupBucket.createIndirectGroupBucket(l2itt.build());
        GroupDescription groupDescription = new DefaultGroupDescription(deviceId,
                             GroupDescription.Type.INDIRECT,
                             new GroupBuckets(Collections.singletonList(bucket)),
                             l2groupkey,
                             l2groupId,
                             nextObj.appId());
        groupService.addGroup(groupDescription);
    }

    /**
     * Processes next element of a group chain. Assumption is that if this
     * group points to another group, the latter has already been created
     * and this driver has received notification for it. A second assumption is
     * that if there is another group waiting for this group then the appropriate
     * stores already have the information to act upon the notification for the
     * creating of this group.
     *
     * @param gce the group chain element to be processed next
     */
    private void processGroupChain(GroupChainElem gce) {
        GroupBucket bucket = DefaultGroupBucket
                .createIndirectGroupBucket(gce.getBucketActions());
        GroupDescription groupDesc = new DefaultGroupDescription(deviceId,
                             GroupDescription.Type.INDIRECT,
                             new GroupBuckets(Collections.singletonList(bucket)),
                             gce.getGkey(),
                             gce.getGivenGroupId(),
                             gce.getAppId());
        groupService.addGroup(groupDesc);
    }

    private Collection<FlowRule> processForward(ForwardingObjective fwd) {
        switch (fwd.flag()) {
            case SPECIFIC:
                return processSpecific(fwd);
            case VERSATILE:
                return processVersatile(fwd);
            default:
                fail(fwd, ObjectiveError.UNKNOWN);
                log.warn("Unknown forwarding flag {}", fwd.flag());
        }
        return Collections.emptySet();
    }

    /**
     * In the OF-DPA 1.0 pipeline, versatile forwarding objectives go to the
     * ACL table.
     * @param fwd  the forwarding objective of type 'versatile'
     * @return     a collection of flow rules to be sent to the switch. An empty
     *             collection may be returned if there is a problem in processing
     *             the flow rule
     */
    private Collection<FlowRule> processVersatile(ForwardingObjective fwd) {
        log.info("Processing versatile forwarding objective");
        TrafficSelector selector = fwd.selector();

        EthTypeCriterion ethType =
                (EthTypeCriterion) selector.getCriterion(Criterion.Type.ETH_TYPE);
        if (ethType == null) {
            log.error("Versatile forwarding objective must include ethType");
            fail(fwd, ObjectiveError.UNKNOWN);
            return Collections.emptySet();
        }
        if (ethType.ethType().toShort() == Ethernet.TYPE_ARP) {
            log.warn("Installing ARP rule to table 60");

            // currently need to punt from ACL table should use:
            // OF apply-actions-instruction
            // To use OF write-actions-instruction
            /*TrafficTreatment.Builder tb = DefaultTrafficTreatment.builder();
            fwd.treatment().allInstructions().stream()
                .forEach(ti -> tb.deferred().add(ti));*/

            FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                    .fromApp(fwd.appId())
                    .withPriority(fwd.priority())
                    .forDevice(deviceId)
                    .withSelector(fwd.selector())
                    .withTreatment(fwd.treatment())
                    .makePermanent()
                    .forTable(ACL_TABLE);

            return Collections.singletonList(ruleBuilder.build());
        }

        // XXX not handling other versatile flows yet
        return Collections.emptySet();
    }

    /**
     * In the OF-DPA 1.0 pipeline, specific forwarding refers to the IP table
     * (unicast or multicast) or the L2 table (mac + vlan).
     *
     * @param fwd the forwarding objective of type 'specific'
     * @return    a collection of flow rules. Typically there will be only one
     *            for this type of forwarding objective. An empty set may be
     *            returned if there is an issue in processing the objective.
     */
    private Collection<FlowRule> processSpecific(ForwardingObjective fwd) {
        log.debug("Processing specific forwarding objective");
        TrafficSelector selector = fwd.selector();
        EthTypeCriterion ethType =
                (EthTypeCriterion) selector.getCriterion(Criterion.Type.ETH_TYPE);
        // XXX currently supporting only the L3 unicast table
        if (ethType == null || ethType.ethType().toShort() != Ethernet.TYPE_IPV4) {
            fail(fwd, ObjectiveError.UNSUPPORTED);
            return Collections.emptySet();
        }

        TrafficSelector filteredSelector =
                DefaultTrafficSelector.builder()
                        .matchEthType(Ethernet.TYPE_IPV4)
                        .matchIPDst(
                                ((IPCriterion)
                                        selector.getCriterion(Criterion.Type.IPV4_DST)).ip())
                        .build();

        TrafficTreatment.Builder tb = DefaultTrafficTreatment.builder();

        if (fwd.nextId() != null) {
            NextGroup next = flowObjectiveStore.getNextGroup(fwd.nextId());
            List<GroupKey> gkeys = appKryo.deserialize(next.data());
            Group group = groupService.getGroup(deviceId, gkeys.get(0));
            if (group == null) {
                log.warn("The group left!");
                fail(fwd, ObjectiveError.GROUPMISSING);
                return Collections.emptySet();
            }
            tb.deferred().group(group.id());
        }
        tb.transition(ACL_TABLE);
        FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                .fromApp(fwd.appId())
                .withPriority(fwd.priority())
                .forDevice(deviceId)
                .withSelector(filteredSelector)
                .withTreatment(tb.build());

        if (fwd.permanent()) {
            ruleBuilder.makePermanent();
        } else {
            ruleBuilder.makeTemporary(fwd.timeout());
        }

        ruleBuilder.forTable(UNICAST_ROUTING_TABLE);
        return Collections.singletonList(ruleBuilder.build());
    }

    private void pass(Objective obj) {
        if (obj.context().isPresent()) {
            obj.context().get().onSuccess(obj);
        }
    }


    private void fail(Objective obj, ObjectiveError error) {
        if (obj.context().isPresent()) {
            obj.context().get().onError(obj, error);
        }
    }


    protected void initializePipeline() {
        processPortTable();
        processVlanTable();
        processTmacTable();
        processIpTable();
        //processMcastTable();
        //processBridgingTable();
        //processAclTable();
        //processGroupTable();
        //processMplsTable();
    }

    protected void processMplsTable() {
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.MPLS_UNICAST);
        selector.matchMplsLabel(MplsLabel.mplsLabel(0xff));
        selector.matchMplsBos(true);
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.popMpls(Ethernet.TYPE_IPV4);
        treatment.transition(ACL_TABLE);
        FlowRule test = DefaultFlowRule.builder().forDevice(deviceId)
                .withSelector(selector.build()).withTreatment(treatment.build())
                .withPriority(LOWEST_PRIORITY).fromApp(driverId).makePermanent()
                .forTable(25).build();
        ops = ops.add(test);

        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Initialized mpls table");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("Failed to initialize mpls table");
            }
        }));

    }

    protected void processPortTable() {
        //XXX is table miss entry enough or do we need to do the maskable in-port 0?
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchInPort(PortNumber.portNumber(0)); // should be maskable?
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.transition(VLAN_TABLE);
        FlowRule tmisse = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(LOWEST_PRIORITY)
                .fromApp(driverId)
                .makePermanent()
                .forTable(PORT_TABLE).build();
        /*ops = ops.add(tmisse);

        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Initialized port table");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("Failed to initialize port table");
            }
        }));*/

    }

    private void processVlanTable() {
        // Table miss entry is not required as ofdpa default is to drop
        // In OF terms, the absence of a t.m.e. also implies drop
    }


    protected void processTmacTable() {
        //table miss entry
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        selector = DefaultTrafficSelector.builder();
        treatment = DefaultTrafficTreatment.builder();
        treatment.transition(BRIDGING_TABLE);
        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(LOWEST_PRIORITY)
                .fromApp(driverId)
                .makePermanent()
                .forTable(TMAC_TABLE).build();
        /*ops =  ops.add(rule); // XXX bug in ofdpa
        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Initialized tmac table");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("Failed to initialize tmac table");
            }
        }));*/
    }

    protected void processIpTable() {
        //table miss entry
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        selector = DefaultTrafficSelector.builder();
        treatment = DefaultTrafficTreatment.builder();
        treatment.transition(ACL_TABLE);
        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(LOWEST_PRIORITY)
                .fromApp(driverId)
                .makePermanent()
                .forTable(UNICAST_ROUTING_TABLE).build();
        /*ops =  ops.add(rule);
        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Initialized IP table");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("Failed to initialize unicast IP table");
            }
        }));*/
    }

    private class GroupChecker implements Runnable {
        @Override
        public void run() {
            Set<GroupKey> keys = pendingGroups.keySet().stream()
                    .filter(key -> groupService.getGroup(deviceId, key) != null)
                    .collect(Collectors.toSet());
            Set<GroupKey> otherkeys = pendingNextObjectives.asMap().keySet().stream()
                    .filter(otherkey -> groupService.getGroup(deviceId, otherkey) != null)
                    .collect(Collectors.toSet());
            keys.addAll(otherkeys);

            keys.stream().forEach(key -> {
                //first check for group chain
                GroupChainElem gce = pendingGroups.remove(key);
                if (gce != null) {
                    log.info("Group service processed group key {}. Processing next "
                            + "group in group chain with group key {}",
                            appKryo.deserialize(key.key()),
                            appKryo.deserialize(gce.getGkey().key()));
                    processGroupChain(gce);
                } else {
                    OfdpaGroupChain obj = pendingNextObjectives.getIfPresent(key);
                    log.info("Group service processed group key {}. Done implementing "
                            + "next objective: {}", appKryo.deserialize(key.key()),
                            obj.nextObjective().id());
                    if (obj != null) {
                        pass(obj.nextObjective());
                        pendingNextObjectives.invalidate(key);
                        flowObjectiveStore.putNextGroup(obj.nextObjective().id(), obj);
                    }
                }
            });
        }
    }

    private class InnerGroupListener implements GroupListener {
        @Override
        public void event(GroupEvent event) {
            log.debug("received group event of type {}", event.type());
            if (event.type() == GroupEvent.Type.GROUP_ADDED) {
                GroupKey key = event.subject().appCookie();
                // first check for group chain
                GroupChainElem gce = pendingGroups.remove(key);
                if (gce != null) {
                    log.info("group ADDED with group key {} .. "
                            + "Processing next group in group chain with group key {}",
                            appKryo.deserialize(key.key()),
                            appKryo.deserialize(gce.getGkey().key()));
                    processGroupChain(gce);
                } else {
                    OfdpaGroupChain obj = pendingNextObjectives.getIfPresent(key);
                    if (obj != null) {
                        log.info("group ADDED with key {}.. Done implementing next "
                                + "objective: {}",
                                appKryo.deserialize(key.key()), obj.nextObjective().id());
                        pass(obj.nextObjective());
                        pendingNextObjectives.invalidate(key);
                        flowObjectiveStore.putNextGroup(obj.nextObjective().id(), obj);
                    }
                }
            }
        }
    }

    /**
     * Represents a group-chain that implements a Next-Objective from
     * the application. Includes information about the next objective Id, and the
     * group keys for the groups in the group chain. The chain is expected to
     * look like group0 --> group 1 --> outPort. Information about the groups
     * themselves can be fetched from the Group Service using the group keys from
     * objects instantiating this class.
     */
    private class OfdpaGroupChain implements NextGroup {
        private final NextObjective nextObj;
        private final List<GroupKey> gkeys;

        /** expected group chain: group0 --> group1 --> port. */
        public OfdpaGroupChain(List<GroupKey> gkeys, NextObjective nextObj) {
            this.gkeys = gkeys;
            this.nextObj = nextObj;
        }

        @SuppressWarnings("unused")
        public List<GroupKey> groupKeys() {
            return gkeys;
        }

        public NextObjective nextObjective() {
            return nextObj;
        }

        @Override
        public byte[] data() {
            return appKryo.serialize(gkeys);
        }

    }

    /**
     * Represents a group element that is part of a chain of groups.
     * Stores enough information to create a Group Description to add the group
     * to the switch by requesting the Group Service. Objects instantiating this
     * class are meant to be temporary and live as long as it is needed to wait for
     * preceding groups in the group chain to be created.
     */
    private class GroupChainElem {
        private TrafficTreatment bucketActions;
        private Integer givenGroupId;
        private GroupKey gkey;
        private ApplicationId appId;

        public GroupChainElem(GroupKey gkey, Integer givenGroupId,
                              TrafficTreatment tr, ApplicationId appId) {
            this.bucketActions = tr;
            this.givenGroupId = givenGroupId;
            this.gkey = gkey;
            this.appId = appId;
        }

        public TrafficTreatment getBucketActions() {
            return bucketActions;
        }

        public Integer getGivenGroupId() {
            return givenGroupId;
        }

        public GroupKey getGkey() {
            return gkey;
        }

        public ApplicationId getAppId() {
            return appId;
        }

    }
}
