package simplify.graph;

import java.util.List;
import java.util.logging.Logger;

import org.jf.dexlib2.builder.BuilderInstruction;
import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.iface.instruction.OffsetInstruction;
import org.jf.dexlib2.iface.instruction.SwitchElement;
import org.jf.dexlib2.iface.instruction.SwitchPayload;
import org.jf.dexlib2.writer.builder.BuilderMethod;
import org.jf.util.SparseArray;

import simplify.Simplifier;

public class CallGraphBuilder {

    private static final Logger log = Logger.getLogger(Simplifier.class.getSimpleName());

    public static Node build(BuilderMethod method) {
        log.fine("Building graph for: " + method.getName());

        MutableMethodImplementation impl = (MutableMethodImplementation) method.getImplementation();
        List<BuilderInstruction> instructions = impl.getInstructions();
        SparseArray<BuilderInstruction> instructionOffsets = getInstructionOffsets(instructions);
        SparseArray<Node> nodeIndicies = getNodeIndicies(instructions);

        Node rootNode = nodeIndicies.get(0);
        int currentCodeOffset = 0;
        for (int i = 0; i < instructions.size(); i++) {
            BuilderInstruction instruction = instructions.get(i);
            Node currentNode = nodeIndicies.get(instruction.getLocation().getIndex());

            if (instruction instanceof OffsetInstruction) {
                int targetOffset = currentCodeOffset + ((OffsetInstruction) instruction).getCodeOffset();
                BuilderInstruction targetInstruction = instructionOffsets.get(targetOffset);
                if (targetInstruction instanceof SwitchPayload) {
                    SwitchPayload payload = (SwitchPayload) targetInstruction;
                    for (SwitchElement element : payload.getSwitchElements()) {
                        BuilderInstruction switchInstruction = instructionOffsets.get(element.getOffset());
                        Node child = nodeIndicies.get(switchInstruction.getLocation().getIndex());
                        currentNode.addChild(child);
                    }
                } else {
                    Node child = nodeIndicies.get(targetInstruction.getLocation().getIndex());
                    currentNode.addChild(child);
                }
            }

            if (instruction.getOpcode().canContinue() && ((i + 1) < instructions.size())) {
                Node child = nodeIndicies.get(instructions.get(i + 1).getLocation().getIndex());
                currentNode.addChild(child);
            }

            currentCodeOffset += instruction.getCodeUnits();
        }

        return rootNode;
    }

    private static SparseArray<BuilderInstruction> getInstructionOffsets(List<BuilderInstruction> instructions) {
        SparseArray<BuilderInstruction> result = new SparseArray<BuilderInstruction>();

        int currentCodeOffset = 0;
        for (BuilderInstruction instruction : instructions) {
            result.append(currentCodeOffset, instruction);
            currentCodeOffset += instruction.getCodeUnits();
        }

        return result;
    }

    private static SparseArray<Node> getNodeIndicies(List<BuilderInstruction> instructions) {
        SparseArray<Node> result = new SparseArray<Node>(instructions.size());

        for (BuilderInstruction instruction : instructions) {
            result.put(instruction.getLocation().getIndex(), new Node(instruction));
        }

        return result;
    }

}
