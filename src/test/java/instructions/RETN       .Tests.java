package instructions;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class RETN_tests extends InstructionsExecutionTestsBase {

    private static final byte opcode = 0x45;
    private static final byte prefix = (byte) 0xED;

    @Test
    public void RETN_returns_to_proper_address() {
        short instructionAddress = fixture.create(Short.TYPE);
        short returnAddress = fixture.create(Short.TYPE);
        short oldSP = fixture.create(Short.TYPE);

        sut.setSP(oldSP);
        sut.getBus().pokew(oldSP, returnAddress);

        executeAt(instructionAddress, opcode, prefix);

        assertEquals(returnAddress, sut.getPC());
        assertEquals(add(oldSP, 2) & 0xffff, sut.getSP());
    }

    @Test
    public void RETN_returns_proper_T_states() {
        int states = execute(opcode, prefix);

        assertEquals(14, states);
    }

    @Test
    public void RETN_does_not_modify_flags() {
        assertDoesNotChangeFlags(opcode, prefix);
    }

    // TODO after fetch
//    @Test
//    public void RETN_fires_FetchFinished_with_isRet_true() {
//        AtomicBoolean eventFired = new AtomicBoolean(false);
//
//        sut.instructionFetchFinished().addListener(e -> {
//            eventFired.set(true);
//            assertTrue(e.isRetInstruction());
//        });
//
//        execute(opcode, prefix);
//
//        assertTrue(eventFired.get());
//    }

    static Stream<Arguments> source() {
        return Stream.of(
                arguments(false, true),
                arguments(true, false)
        );
    }

    @ParameterizedTest
    @MethodSource("source")
    public void RETN_copies_IFF2_to_IFF1(boolean initialIFF1, boolean initialIFF2) {
        sut.setIff1(initialIFF1);
        sut.setIff2(initialIFF2);

        execute(opcode, prefix);

        assertEquals(initialIFF2, sut.isIff2());
        assertEquals(sut.isIff2(), sut.isIff1());
    }
}
