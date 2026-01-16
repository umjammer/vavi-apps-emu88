package instructions;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class CALL_and_CALL_cc_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> CALL_cc_Source() {
        return Stream.of(
                arguments("Z", (byte) 0xC4, false),
                arguments("C", (byte) 0xD4, false),
                arguments("P", (byte) 0xE4, false),
                arguments("S", (byte) 0xF4, false),
                arguments("Z", (byte) 0xCC, true),
                arguments("C", (byte) 0xDC, true),
                arguments("P", (byte) 0xEC, true),
                arguments("S", (byte) 0xFC, true)
        );
    }

    static Stream<Arguments> CALL_Source() {
        return Stream.of(
                arguments(null, (byte) 0xCD, false)
        );
    }

    @ParameterizedTest
    @MethodSource("CALL_cc_Source")
    public void CALL_cc_does_not_jump_if_flag_not_set(String flagName, byte opcode, boolean flagValue) {
        short instructionAddress = fixture.create(Short.TYPE);

        setFlag(flagName, !flagValue);
        executeAt(instructionAddress, opcode, null);

        assertEquals(add(instructionAddress, 3) & 0xffff, sut.getPC());
    }

    @ParameterizedTest
    @MethodSource("CALL_cc_Source")
    public void CALL_cc_returns_proper_T_states_if_no_jump_is_made(String flagName, byte opcode, boolean flagValue) {
        setFlag(flagName, !flagValue);
        int states = execute(opcode, null);

        assertEquals(10, states);
    }

    @ParameterizedTest
    @MethodSource({"CALL_cc_Source", "CALL_Source"})
    public void CALL_cc_pushes_SP_and_jumps_to_proper_address_if_flag_is_set_CALL_jumps_always(String flagName, byte opcode, boolean flagValue) {
        short instructionAddress = fixture.create(Short.TYPE);
        short callAddress = fixture.create(Short.TYPE);
        short oldSP = fixture.create(Short.TYPE);
        sut.setSP(oldSP);

        setFlagIfNotNull(flagName, flagValue);
        executeAt(instructionAddress, opcode, null, getLowByte(callAddress), getHighByte(callAddress));

        assertEquals(callAddress, sut.getPC());
        assertEquals(sub(oldSP, 2) & 0xffff, sut.getSP());
        assertEquals(add(instructionAddress, 3), readShortFromMemory(sut.getSP()));
    }

    private void setFlagIfNotNull(String flagName, boolean flagValue) {
        if (flagName != null) setFlag(flagName, flagValue);
    }

    @ParameterizedTest
    @MethodSource({"CALL_cc_Source", "CALL_Source"})
    public void CALL_and_CALL_cc_return_proper_T_states_if_jump_is_made(String flagName, byte opcode, boolean flagValue) {
        setFlagIfNotNull(flagName, flagValue);
        int states = execute(opcode, null);

        assertEquals(17, states);
    }

    @ParameterizedTest
    @MethodSource({"CALL_cc_Source", "CALL_Source"})
    public void CALL_and_CALL_cc_do_not_modify_flags(String flagName, byte opcode, boolean flagValue) {
        sut.setF(fixture.create(Byte.TYPE));
        setFlagIfNotNull(flagName, flagValue);
        int value = sut.getF();

        execute(opcode, null);

        assertEquals(value, sut.getF());
    }
}
