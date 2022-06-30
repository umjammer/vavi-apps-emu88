package instructions;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class DI_EI_tests extends InstructionsExecutionTestsBase {

    private static final byte DI_opcode = (byte) 0xF3;
    private static final byte EI_opcode = (byte) 0xFB;

    @Test
    public void DI_resets_IFF() {
        sut.setIff1(true);
        sut.setIff2(true);

        execute(DI_opcode, null);

        assertEquals(false, sut.isIff1());
        assertEquals(false, sut.isIff2());
    }

    @Test
    public void EI_sets_IFF() {
        sut.setIff1(false);
        sut.setIff2(false);

        execute(EI_opcode, null);

        assertEquals(true, sut.isIff1());
        assertEquals(true, sut.isIff2());
    }

    // TODO
//    @ParameterizedTest
//    @ValueSource(bytes = {EI_opcode, DI_opcode})
//    public void EI_fires_FetchFinished_with_isEiOrDi_true(byte opcode) {
//        AtomicBoolean eventFired = new AtomicBoolean(false);
//
//        sut.instructionFetchFinished().addListener(e -> {
//            eventFired.set(true);
//            assertTrue(e.isEiOrDiInstruction());
//        });
//
//        execute(opcode, null);
//
//        assertTrue(eventFired.get());
//    }

    @ParameterizedTest
    @ValueSource(bytes = {EI_opcode, DI_opcode})
    public void DI_EI_do_not_change_flags(byte opcode) {
        assertNoFlagsAreModified(opcode, null);
    }

    @ParameterizedTest
    @ValueSource(bytes = {EI_opcode, DI_opcode})
    public void DI_EI_return_proper_T_states(byte opcode) {
        int states = execute(opcode, null);
        assertEquals(4, states);
    }
}
