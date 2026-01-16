package instructions;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class JR_and_JR_cc_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> jr_cc_Source() {
        return Stream.of(
                arguments("Z", (byte) 0x20, false),
                arguments("C", (byte) 0x30, false),
                arguments("Z", (byte) 0x28, true),
                arguments("C", (byte) 0x38, true)
        );
    }

    static Stream<Arguments> jr_Source() {
        return Stream.of(
                arguments(null, (byte) 0x18, false)
        );
    }

    @ParameterizedTest
    @MethodSource("jr_cc_Source")
    public void JR_cc_does_not_jump_if_flag_not_set(String flagName, byte opcode, boolean flagValue) {
        short instructionAddress = fixture.create(Short.TYPE);

        setFlag(flagName, !flagValue);
        executeAt(instructionAddress, opcode, null, fixture.create(Byte.TYPE));

        assertEquals(add(instructionAddress, 2) & 0xffff, sut.getPC());
    }

    @ParameterizedTest
    @MethodSource("jr_cc_Source")
    public void JR_cc_returns_proper_T_states_if_no_jump_is_made(String flagName, byte opcode, boolean flagValue) {
        setFlag(flagName, !flagValue);
        int states = execute(opcode, null);

        assertEquals(7, states);
    }

    @ParameterizedTest
    @MethodSource({"jr_cc_Source", "jr_Source"})
    public void JR_cc_jumps_to_proper_address_if_flag_is_set_JR_jumps_always(String flagName, byte opcode, boolean flagValue) {
        short instructionAddress = fixture.create(Short.TYPE);

        setFlagIfNotNull(flagName, flagValue);
        executeAt(instructionAddress, opcode, null, (byte) 0x7F);
        assertEquals(add(instructionAddress, 129) & 0xffff, sut.getPC());

        setFlagIfNotNull(flagName, flagValue);
        executeAt(instructionAddress, opcode, null, (byte) 0x80);
        assertEquals(sub(instructionAddress, 126) & 0xffff, sut.getPC());
    }

    private void setFlagIfNotNull(String flagName, boolean flagValue) {
        if (flagName != null) setFlag(flagName, flagValue);
    }

    @ParameterizedTest
    @MethodSource({"jr_cc_Source", "jr_Source"})
    public void JR_and_JR_cc_returns_proper_T_states_if_jump_is_made(String flagName, byte opcode, boolean flagValue) {
        setFlagIfNotNull(flagName, flagValue);
        int states = execute(opcode, null);

        assertEquals(12, states);
    }

    @ParameterizedTest
    @MethodSource({"jr_cc_Source", "jr_Source"})
    public void JR_and_JR_cc_do_not_modify_flags(String flagName, byte opcode, boolean flagValue) {
        sut.setF(fixture.create(Byte.TYPE));
        setFlagIfNotNull(flagName, flagValue);
        int value = sut.getF();

        execute(opcode, null);

        assertEquals(value, sut.getF());
    }
}
