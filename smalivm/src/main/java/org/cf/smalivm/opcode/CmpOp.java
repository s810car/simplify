package org.cf.smalivm.opcode;

import org.cf.smalivm.context.HeapItem;
import org.cf.smalivm.context.MethodState;
import org.cf.util.Utils;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.formats.Instruction23x;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CmpOp extends MethodStateOp {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(CmpOp.class.getSimpleName());

    static CmpOp create(Instruction instruction, int address) {
        String opName = instruction.getOpcode().name;
        int childAddress = address + instruction.getCodeUnits();

        Instruction23x instr = (Instruction23x) instruction;
        int destRegister = instr.getRegisterA();
        int lhsRegister = instr.getRegisterB();
        int rhsRegister = instr.getRegisterC();

        return new CmpOp(address, opName, childAddress, destRegister, lhsRegister, rhsRegister);
    }

    private final int destRegister;
    private final int lhsRegister;
    private final int rhsRegister;

    public CmpOp(int address, String opName, int childAddress, int destRegister, int lhsRegister, int rhsRegister) {
        super(address, opName, childAddress);

        this.destRegister = destRegister;
        this.lhsRegister = lhsRegister;
        this.rhsRegister = rhsRegister;
    }

    @Override
    public int[] execute(MethodState mState) {
        HeapItem lhsItem = mState.readRegister(lhsRegister);
        HeapItem rhsItem = mState.readRegister(rhsRegister);

        HeapItem item;
        if ((lhsItem.isUnknown()) || (rhsItem.isUnknown())) {
            item = HeapItem.newUnknown("I");
        } else {
            Object lhs = lhsItem.getValue();
            Object rhs = rhsItem.getValue();

            assert lhs instanceof Number;
            assert rhs instanceof Number;
            assert lhs.getClass() == rhs.getClass();
            assert lhsItem.getType().equals(rhsItem.getType());

            int cmp = cmp((Number) lhs, (Number) rhs);
            item = new HeapItem(cmp, "I");
        }

        mState.assignRegister(destRegister, item);

        return getPossibleChildren();
    }

    private int cmp(Number val1, Number val2) {
        boolean arg1IsNan = ((val1 instanceof Float) && ((Float) val1).isNaN())
                        || ((val1 instanceof Double) && ((Double) val1).isNaN());
        boolean arg2IsNan = ((val2 instanceof Float) && ((Float) val2).isNaN())
                        || ((val2 instanceof Double) && ((Double) val2).isNaN());

        int value = 0;
        if (arg1IsNan || arg2IsNan) {
            if (getName().startsWith("cmpg")) {
                value = 1;
            } else { // cmpl
                value = -1;
            }
        } else {
            if (getName().endsWith("float")) {
                Float castVal1 = Utils.getFloatValue(val1);
                Float castVal2 = Utils.getFloatValue(val2);
                // The docs say "b == c" but I don't think they mean identity.
                value = Float.compare(castVal1, castVal2);
            } else if (getName().endsWith("double")) {
                Double castVal1 = Utils.getDoubleValue(val1);
                Double castVal2 = Utils.getDoubleValue(val2);
                // The docs say "b == c" but I don't think they mean identity.
                value = Double.compare(castVal1, castVal2);
            } else {
                Long castVal1 = Utils.getLongValue(val1);
                Long castVal2 = Utils.getLongValue(val2);
                value = Long.compare(castVal1, castVal2);
            }
        }

        return value;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getName());
        sb.append(" r").append(destRegister).append(", r").append(lhsRegister).append(", r").append(rhsRegister);

        return sb.toString();
    }

}
