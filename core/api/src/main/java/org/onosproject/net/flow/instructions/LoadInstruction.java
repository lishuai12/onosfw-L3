package org.onosproject.net.flow.instructions;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.flow.instructions.L3ModificationInstruction.ModIPInstruction;

public abstract class LoadInstruction implements Instruction {

    public enum LoadSubType {
        NXMARPSHA,
        NXMARPSPA,
        NXMARPOP
    }

    @Override
    public Type type() {
        // TODO Auto-generated method stub
        return Type.LOAD;
    }

    /**
     * Returns the subtype of the modification instruction.
     *
     * @return type of instruction
     */
    public abstract LoadSubType subtype();

    /**
     * Represents a L3 TTL modification instruction.
     */
    public static final class NxmArpShaInstruction extends LoadInstruction {

        private final LoadSubType subtype;
        private final MacAddress mac;

        public NxmArpShaInstruction(LoadSubType subtype, MacAddress mac) {
            this.subtype = subtype;
            this.mac = mac;
        }

        @Override
        public LoadSubType subtype() {
            return this.subtype;
        }

        @Override
        public String toString() {
            return toStringHelper(subtype().toString())
                    .add("mac", mac).toString();
        }

        public MacAddress mac() {
            return this.mac;
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), subtype(), mac);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof NxmArpShaInstruction) {
                NxmArpShaInstruction that = (NxmArpShaInstruction) obj;
                return  Objects.equals(mac, that.mac) &&
                        Objects.equals(this.subtype(), that.subtype());
            }
            return false;
        }
    }

    /**
     * Represents a L3 TTL modification instruction.
     */
    public static final class NxmArpSpaInstruction extends LoadInstruction {

        private final LoadSubType subtype;
        private final IpAddress ip;

        public NxmArpSpaInstruction(LoadSubType subtype, IpAddress ip) {
            this.subtype = subtype;
            this.ip = ip;
        }

        @Override
        public LoadSubType subtype() {
            return this.subtype;
        }


        public IpAddress ip() {
            return this.ip;
        }

        @Override
        public String toString() {
            return toStringHelper(subtype().toString())
                    .add("ip", ip).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), subtype(), ip);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof NxmArpSpaInstruction) {
                NxmArpSpaInstruction that = (NxmArpSpaInstruction) obj;
                return  Objects.equals(ip, that.ip) &&
                        Objects.equals(this.subtype(), that.subtype());
            }
            return false;
        }
    }

    /**
     * Represents a L3 TTL modification instruction.
     */
    public static final class NxmArpOpInstruction extends LoadInstruction {

        private final LoadSubType subtype;
        private final long op;


        public NxmArpOpInstruction(LoadSubType subtype, long op) {
            this.subtype = subtype;
            this.op = op;
        }

        @Override
        public LoadSubType subtype() {
            return this.subtype;
        }

        public long op() {
            return this.op;
        }

        @Override
        public String toString() {
            return toStringHelper(subtype().toString())
                    .add("op", op).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), subtype(), op);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof ModIPInstruction) {
                NxmArpOpInstruction that = (NxmArpOpInstruction) obj;
                return  Objects.equals(op, that.op) &&
                        Objects.equals(this.subtype(), that.subtype());
            }
            return false;
        }
    }
}
