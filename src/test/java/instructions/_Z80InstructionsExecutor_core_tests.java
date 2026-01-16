package instructions;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class Z80InstructionsExecutor_core_test extends InstructionsExecutionTestsBase {

    private static final byte NOP_opcode = 0x00;
    private static final byte LD_BC_nn_opcode = 0x01;
    private static final byte ADD_HL_BC_opcode = 0x09;
    private static final byte IN_B_C_opcode = 0x40;
    private static final byte RLC_B_opcode = 0x00;

    // TODO after fetch
//    @Test
//    public void InstructionsExecution_fire_FetchFinished_event_and_return_proper_T_states_count() {
//        AtomicInteger fetchFinishedEventsCount = new AtomicInteger(0);
//
//        sut.instructionFetchFinished().addListener(e -> fetchFinishedEventsCount.incrementAndGet());
//
//        setMemoryContents((byte) 0);
//        assertEquals(4, execute(NOP_opcode, null));
//        setMemoryContents((byte) 0, fixture.create(Byte.TYPE), fixture.create(Byte.TYPE));
//        assertEquals(10, execute(LD_BC_nn_opcode, null));
//
//        setMemoryContents((byte) 0);
//        assertEquals(8, execute((byte) 0xCB, null, (byte) 0));
//
//        assertEquals(15, execute(ADD_HL_BC_opcode, (byte) 0xDD));
//        assertEquals(15, execute(ADD_HL_BC_opcode, (byte) 0xFD));
//
//        assertEquals(12, execute(IN_B_C_opcode, (byte) 0xED));
//
//        assertEquals(23, execute((byte) 0xCB, (byte) 0xDD), (byte) 0);
//        assertEquals(23, execute((byte) 0xCB, (byte) 0xFD), (byte) 0);
//
//        assertEquals(8, fetchFinishedEventsCount.get());
//    }

    // TODO after fetch
//    @Test
//    public void Unsupported_instructions_just_return_8_TStates_elapsed() {
//        AtomicInteger fetchFinishedEventsCount = new AtomicInteger(0);
//
//        sut.instructionFetchFinished().addListener(e -> fetchFinishedEventsCount.incrementAndGet());
//
//        assertEquals(8, execute((byte) 0x3F, (byte) 0xED));
//        assertEquals(8, execute((byte) 0xC0, (byte) 0xED));
//        assertEquals(8, execute((byte) 0x80, (byte) 0xED));
//        assertEquals(8, execute((byte) 0x9F, (byte) 0xED));
//
//        assertEquals(4, fetchFinishedEventsCount.get());
//    }

    // TODO after fetch
//    @Test
//    public void Unsupported_instructions_invoke_overridable_method_ExecuteUnsopported_ED_Instruction() {
//        sut = newFakeInstructionExecutor();
//
//        execute((byte) 0x3F, (byte) 0xED);
//        execute((byte) 0xC0, (byte) 0xED);
//
//        assertArrayEquals(new Byte[] {0x3F, (byte) 0xC0}, ((FakeInstructionExecutor) sut).unsupportedExecuted.toArray());
//    }

    @Test
    public void Execute_increases_R_appropriately() {
        sut.setR((byte) 0xFE);

        execute(NOP_opcode, null);
        assertEquals(0xFF, sut.getR());

        execute(LD_BC_nn_opcode, null, fixture.create(Byte.TYPE), fixture.create(Byte.TYPE));
        assertEquals(0x80, sut.getR());

        execute(RLC_B_opcode, (byte) 0xCB);
        assertEquals(0x82, sut.getR());

        execute(ADD_HL_BC_opcode, (byte) 0xDD);
        assertEquals(0x84, sut.getR());

        execute(ADD_HL_BC_opcode, (byte) 0xFD);
        assertEquals(0x86, sut.getR());

        execute(IN_B_C_opcode, (byte) 0xED);
        assertEquals(0x88, sut.getR());

        execute((byte) 0xCB, null, (byte) 0xDD, (byte) 0);
        assertEquals(0x8A, sut.getR());

        execute((byte) 0xCB, null, (byte) 0xFD, (byte) 0);
        assertEquals(0x8C, sut.getR());
    }

    // TODO after fetch
//    @Test
//    public void DD_FD_not_followed_by_valid_opcode_are_trated_as_nops() {
//        AtomicInteger fetchFinishedEventsCount = new AtomicInteger(0);
//
//        sut.instructionFetchFinished().addListener(e -> fetchFinishedEventsCount.incrementAndGet());
//
//        assertEquals(4, execute((byte) 0xFD, (byte) 0xDD));
//        assertEquals(4, execute((byte) 0x01, (byte) 0xFD));
//        assertEquals(10, execute((byte) 0x01, null, fixture.create(Byte.TYPE), fixture.create(Byte.TYPE)));
//
//        assertEquals(3, fetchFinishedEventsCount.get());
//    }

    @Test
    void testAddressFixture() {
        assertNotEquals((short) 1, createAddressFixture((short) 1));
        assertNotEquals((short) 0, createAddressFixture((short) 0, (short) 1, (short) 2));
        assertNotEquals((short) 1, createAddressFixture((short) 0, (short) 1, (short) 2));
        assertNotEquals((short) 2, createAddressFixture((short) 0, (short) 1, (short) 2));
        assertTrue(3 != createAddressFixture((short) 0, (short) 1, (short) 2, (short) 3));
        assertNotEquals((short) 1, createAddressFixture());
    }
}
