package instructions;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static vavi.apps.em88.Z80.inc16bitInternal;


class RET_and_RET_cc_tests extends InstructionsExecutionTestsBase {

    private static final int RETI_opcode = 0x4D;
    private static final int RETI_prefix = 0xED;

    static Stream<Arguments> RET_cc_Source() {
        return Stream.of(
                arguments("Z", (byte) 0xC0, false),
                arguments("C", (byte) 0xD0, false),
                arguments("P", (byte) 0xE0, false),
                arguments("S", (byte) 0xF0, false),
                arguments("Z", (byte) 0xC8, true),
                arguments("C", (byte) 0xD8, true),
                arguments("P", (byte) 0xE8, true),
                arguments("S", (byte) 0xF8, true)
        );
    }

    static Stream<Arguments> RET_Source() {
        return Stream.of(
                arguments(null, (byte) 0xC9, false)
        );
    }

    @ParameterizedTest
    @MethodSource("RET_cc_Source")
    public void RET_cc_does_not_return_if_flag_not_set(String flagName, byte opcode, boolean flagValue) {
        short instructionAddress = fixture.create(Short.TYPE);
        int oldSP = sut.getSP();

        setFlag(flagName, !flagValue);
        executeAt(instructionAddress, opcode, null);

        assertEquals(inc(instructionAddress) & 0xffff, sut.getPC());
        assertEquals(oldSP, sut.getSP());
    }

    @ParameterizedTest
    @MethodSource("RET_cc_Source")
    public void RET_cc_returns_proper_T_states_if_no_jump_is_made(String flagName, byte opcode, boolean flagValue) {
        setFlag(flagName, !flagValue);
        int states = execute(opcode, null);

        assertEquals(5, states);
    }

    @ParameterizedTest
    @MethodSource({"RET_cc_Source", "RET_Source"})
    public void RET_cc_returns_to_proper_address_if_flag_is_set_RET_return_always(String flagName, byte opcode, boolean flagValue) {
        short instructionAddress = fixture.create(Short.TYPE);
        short returnAddress = fixture.create(Short.TYPE);
        short oldSP = fixture.create(Short.TYPE);

        sut.setSP(oldSP);
        sut.getBus().pokew(oldSP, returnAddress);

        setFlagIfNotNull(flagName, flagValue);
        executeAt(instructionAddress, opcode, null);

        assertEquals(returnAddress, sut.getPC());
        assertEquals(add(oldSP, 2) & 0xffff, sut.getSP());
    }

    private void setFlagIfNotNull(String flagName, boolean flagValue) {
        if (flagName != null) setFlag(flagName, flagValue);
    }

    @ParameterizedTest
    @MethodSource({"RET_cc_Source", "RET_Source"})
    public void RET_and_RET_cc_return_proper_T_states_if_jump_is_made(String flagName, byte opcode, boolean flagValue) {
        setFlagIfNotNull(flagName, flagValue);
        int states = execute(opcode, null);

        assertEquals(flagName == null ? 10 : 11, states);
    }

    @ParameterizedTest
    @MethodSource({"RET_cc_Source", "RET_Source"})
    public void RET_and_RET_cc_do_not_modify_flags(String flagName, byte opcode, boolean flagValue) {
        sut.setF(fixture.create(Byte.TYPE));
        setFlagIfNotNull(flagName, flagValue);
        int value = sut.getF();

        execute(opcode, null);

        assertEquals(value, sut.getF());
    }

    // TODO after fetch
//    @ParameterizedTest
//    @MethodSource({"RET_cc_Source", "RET_Source"})
//    public void RET_fires_FetchFinished_with_isRet_true_if_flag_is_set(String flagName, byte opcode, boolean flagValue) {
//        AtomicBoolean eventFired = new AtomicBoolean(false);
//
//        sut.setF(255);
//        sut.instructionFetchFinished().addListener(e -> {
//            eventFired.set(true);
//            if ((opcode & 0x0F) == 0)
//                assertFalse(e.isRetInstruction());
//            else
//                assertTrue(e.isRetInstruction());
//        });
//
//        execute(opcode, null);
//
//        assertTrue(eventFired.get());
//    }

    @Test
    public void RETI_returns_to_pushed_address() {
        short instructionAddress = fixture.create(Short.TYPE);
        short returnAddress = fixture.create(Short.TYPE);
        short oldSP = fixture.create(Short.TYPE);

        sut.setSP(oldSP);
        sut.getBus().pokew(oldSP, returnAddress);

        executeAt(instructionAddress, (byte) RETI_opcode, (byte) RETI_prefix);

        assertEquals(returnAddress, sut.getPC());
        assertEquals(add(oldSP, 2) & 0xffff, sut.getSP());
    }

    @Test
    public void RETI_returns_proper_T_states() {
        int states = execute((byte) RETI_opcode, (byte) RETI_prefix);
        assertEquals(14, states);
    }
}
