package instructions;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class JP_and_JP_cc_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> JP_cc_Source() {
        return Stream.of(
                arguments("Z", (byte) 0xC2, false),
                arguments("C", (byte) 0xD2, false),
                arguments("P", (byte) 0xE2, false),
                arguments("S", (byte) 0xF2, false),
                arguments("Z", (byte) 0xCA, true),
                arguments("C", (byte) 0xDA, true),
                arguments("P", (byte) 0xEA, true),
                arguments("S", (byte) 0xFA, true)
        );
    }

    static Stream<Arguments> JP_Source() {
        return Stream.of(
                arguments(null, (byte) 0xC3, false)
        );
    }

    @ParameterizedTest
    @MethodSource("JP_cc_Source")
    public void JP_cc_does_not_jump_if_flag_not_set(String flagName, byte opcode, boolean flagValue) {
        short instructionAddress = fixture.create(Short.TYPE);

        setFlag(flagName, !flagValue);
        executeAt(instructionAddress, opcode, null);

        assertEquals(add(instructionAddress, 3) & 0xffff, sut.getPC());
    }

    @ParameterizedTest
    @MethodSource("JP_cc_Source")
    public void JP_cc_returns_proper_T_states_if_no_jump_is_made(String flagName, byte opcode, boolean flagValue) {
        setFlag(flagName, !flagValue);
        int states = execute(opcode, null);

        assertEquals(10, states);
    }

    @ParameterizedTest
    @MethodSource({"JP_cc_Source", "JP_Source"})
    public void JP_cc_jumps_to_proper_address_if_flag_is_set_JP_jumps_always(String flagName, byte opcode, boolean flagValue) {
        short instructionAddress = fixture.create(Short.TYPE);
        short jumpAddress = fixture.create(Short.TYPE);

        setFlagIfNotNull(flagName, flagValue);
        executeAt(instructionAddress, opcode, null, getLowByte(jumpAddress), getHighByte(jumpAddress));

        assertEquals(jumpAddress, sut.getPC());
    }

    private void setFlagIfNotNull(String flagName, boolean flagValue) {
        if (flagName != null) setFlag(flagName, flagValue);
    }

    @ParameterizedTest
    @MethodSource({"JP_cc_Source", "JP_Source"})
    public void JP_and_JP_cc_return_proper_T_states_if_jump_is_made(String flagName, byte opcode, boolean flagValue) {
        setFlagIfNotNull(flagName, flagValue);
        int states = execute(opcode, null);

        assertEquals(10, states);
    }

    @ParameterizedTest
    @MethodSource({"JP_cc_Source", "JP_Source"})
    public void JP_and_JP_cc_do_not_modify_flags(String flagName, byte opcode, boolean flagValue) {
        sut.setF(fixture.create(Byte.TYPE));
        setFlagIfNotNull(flagName, flagValue);
        int value = sut.getF();

        execute(opcode, null);

        assertEquals(value, sut.getF());
    }
}
