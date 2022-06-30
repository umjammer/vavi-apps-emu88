package instructions;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class LD_r_r_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> LD_r_r_Source() {
        List<Arguments> combinations = new ArrayList<>();

        final String[] registers = new String[] {"B", "C", "D", "E", "H", "L", null, "A"};
        for (int src = 0; src <= 7; src++) {
            for (int dest = 0; dest <= 7; dest++) {
                if (src == 6 || dest == 6) continue;
                byte opcode = (byte) (src | (dest << 3) | 0x40);
                combinations.add(arguments(registers[dest], registers[src], opcode));
            }
        }

        return combinations.stream();
    }

    @ParameterizedTest
    @MethodSource("LD_r_r_Source")
    public void LD_r_r_loads_register_with_value(String dest, String src, byte opcode) {
        byte oldValue = fixture.create(Byte.TYPE);
        byte newValue = fixture.create(Byte.TYPE);

        setReg(dest, oldValue);
        setReg(src, newValue);

        execute(opcode, null);

        assertEquals(newValue, this.getRegB(dest));
    }

    @ParameterizedTest
    @MethodSource("LD_r_r_Source")
    public void LD_r_r_do_not_modify_flags(String dest, String src, byte opcode) {
        assertNoFlagsAreModified(opcode, null);
    }

    @ParameterizedTest
    @MethodSource("LD_r_r_Source")
    public void LD_r_r_returns_proper_T_states(String dest, String src, byte opcode) {
        int states = execute(opcode, null);
        assertEquals(4, states);
    }
}
