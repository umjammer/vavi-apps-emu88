package instructions;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class PUSH_rr_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> PUSH_rr_Source() {
        return Stream.of(
                arguments("BC", (byte) 0xC5, null),
                arguments("DE", (byte) 0xD5, null),
                arguments("HL", (byte) 0xE5, null),
                arguments("AF", (byte) 0xF5, null),
                arguments("IX", (byte) 0xE5, (byte) 0xDD),
                arguments("IY", (byte) 0xE5, (byte) 0xFD)
        );
    }

    @ParameterizedTest
    @MethodSource("PUSH_rr_Source")
    public void PUSH_rr_loads_stack_with_value_and_decreases_SP(String reg, byte opcode, Byte prefix) {
        short value = fixture.create(Short.TYPE);
        setReg(reg, value);
        short oldSP = fixture.create(Short.TYPE);
        sut.setSP(oldSP);

        execute(opcode, prefix);

        assertEquals(sub(oldSP, 2) & 0xffff, sut.getSP());
        assertEquals(value, readShortFromMemory(sut.getSP()));
    }

    @ParameterizedTest
    @MethodSource("PUSH_rr_Source")
    public void PUSH_rr_do_not_modify_flags(String reg, byte opcode, Byte prefix) {
        assertNoFlagsAreModified(opcode, null);
    }

    @ParameterizedTest
    @MethodSource("PUSH_rr_Source")
    public void PUSH_rr_returns_proper_T_states(String reg, byte opcode, Byte prefix) {
        int states = execute(opcode, prefix);
        assertEquals(reg.startsWith("I") ? 15 : 11, states);
    }
}
