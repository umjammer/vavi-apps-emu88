package instructions;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class LD_aa_rr_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> LD_aa_rr_Source() {
        return Stream.of(
                arguments("HL", (byte) 0x22, null),
                arguments("DE", (byte) 0x53, (byte) 0xED),
                arguments("BC", (byte) 0x43, (byte) 0xED),
                arguments("SP", (byte) 0x73, (byte) 0xED),
                arguments("IX", (byte) 0x22, (byte) 0xDD),
                arguments("IY", (byte) 0x22, (byte) 0xFD)
        );
    }

    @ParameterizedTest
    @MethodSource("LD_aa_rr_Source")
    public void LD_aa_rr_loads_value_in_memory(String reg, byte opcode, Byte prefix) {
        short address = fixture.create(Short.TYPE);
        short oldValue = fixture.create(Short.TYPE);
        short newValue = fixture.create(Short.TYPE);

        setReg(reg, newValue);
        writeShortToMemory(address, oldValue);

        execute(opcode, prefix, toByteArray(address));

        assertEquals(newValue, readShortFromMemory(address));
    }

    @ParameterizedTest
    @MethodSource("LD_aa_rr_Source")
    public void LD_rr_r_do_not_modify_flags(String reg, byte opcode, Byte prefix) {
        assertNoFlagsAreModified(opcode, prefix);
    }

    @ParameterizedTest
    @MethodSource("LD_aa_rr_Source")
    public void LD_rr_r_returns_proper_T_states(String reg, byte opcode, Byte prefix) {
        int states = execute(opcode, prefix);
        assertEquals(reg.equals("HL") ? 16 : 20, states);
    }
}
