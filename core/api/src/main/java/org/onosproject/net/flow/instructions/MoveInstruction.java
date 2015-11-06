package org.onosproject.net.flow.instructions;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;

public abstract class MoveInstruction implements Instruction {

    public enum MoveSubType {
        NXMARPSHATONXMARPTHA,
        NXMETHSRCTONXMETHDST,
        NXMARPSPATONXMARPTPA,
        NXMIPSRCTONXMIPDST
    }

    @Override
    public Type type() {
        // TODO Auto-generated method stub
        return Type.MOVE;
    }

    /**
     * Returns the subtype of the modification instruction.
     *
     * @return type of instruction
     */
    public abstract MoveSubType subtype();

    /**
     * Represents a L3 TTL modification instruction.
     */
    public static final class NxmArpShaToNxmArpThaInstruction extends MoveInstruction {

        private final MoveSubType subtype;


        public NxmArpShaToNxmArpThaInstruction(MoveSubType subtype) {
            this.subtype = subtype;
        }

        @Override
        public MoveSubType subtype() {
            return this.subtype;
        }

        @Override
        public String toString() {
            return toStringHelper(subtype().toString()).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), subtype());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof NxmArpShaToNxmArpThaInstruction) {
                NxmArpShaToNxmArpThaInstruction that = (NxmArpShaToNxmArpThaInstruction) obj;
                return Objects.equals(this.subtype(), that.subtype());
            }
            return false;
        }
    }

    /**
     * Represents a L3 TTL modification instruction.
     */
    public static final class NxmEthSrcToNxmEthDstInstruction extends MoveInstruction {

        private final MoveSubType subtype;

        NxmEthSrcToNxmEthDstInstruction(MoveSubType subtype) {
            this.subtype = subtype;
        }

        @Override
        public MoveSubType subtype() {
            return this.subtype;
        }

        @Override
        public String toString() {
            return toStringHelper(subtype().toString()).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), subtype());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof NxmEthSrcToNxmEthDstInstruction) {
                NxmEthSrcToNxmEthDstInstruction that = (NxmEthSrcToNxmEthDstInstruction) obj;
                return Objects.equals(this.subtype(), that.subtype());
            }
            return false;
        }
    }

    /**
     * Represents a L3 TTL modification instruction.
     */
    public static final class NxmArpSpaToNxmArpTpaInstruction extends MoveInstruction {

        private final MoveSubType subtype;


        NxmArpSpaToNxmArpTpaInstruction(MoveSubType subtype) {
            this.subtype = subtype;

        }

        @Override
        public MoveSubType subtype() {
            return this.subtype;
        }

        @Override
        public String toString() {
            return toStringHelper(subtype().toString()).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), subtype());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof NxmArpSpaToNxmArpTpaInstruction) {
                NxmArpSpaToNxmArpTpaInstruction that = (NxmArpSpaToNxmArpTpaInstruction) obj;
                return Objects.equals(this.subtype(), that.subtype());
            }
            return false;
        }
    }

    /**
     * Represents a L3 TTL modification instruction.
     */
    public static final class NxmIpSrcToNxmIpDstInstruction extends MoveInstruction {

        private final MoveSubType subtype;


        NxmIpSrcToNxmIpDstInstruction(MoveSubType subtype) {
            this.subtype = subtype;

        }

        @Override
        public MoveSubType subtype() {
            return this.subtype;
        }

        @Override
        public String toString() {
            return toStringHelper(subtype().toString()).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), subtype());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof NxmIpSrcToNxmIpDstInstruction) {
                NxmIpSrcToNxmIpDstInstruction that = (NxmIpSrcToNxmIpDstInstruction) obj;
                return Objects.equals(this.subtype(), that.subtype());
            }
            return false;
        }
    }
}
