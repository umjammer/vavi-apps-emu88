package instructions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static vavi.apps.em88.Z80.dec8bitInternal;


class DJNZ_tests extends InstructionsExecutionTestsBase {

    private static final byte DJNZ_opcode = 0x10;

    @Test
    public void DJNZ_decreases_B() {
        byte value = fixture.create(Byte.TYPE);
        sut.setB(value);

        execute(DJNZ_opcode, null);

        assertEquals(dec(value) & 0xff, sut.getB());
    }

    @Test
    public void DJNZ_does_not_jump_if_B_decreases_to_zero() {
        short instructionAddress = fixture.create(Short.TYPE);

        sut.setB((byte) 1);
        executeAt(instructionAddress, DJNZ_opcode, null, fixture.create(Byte.TYPE));

        assertEquals(instructionAddress + 2, sut.getPC());
    }

    @Test
    public void DJNZ_returns_proper_T_states_when_no_jump_is_done() {
        sut.setB((byte) 1);
        int states = execute(DJNZ_opcode, null);

        assertEquals(8, states);
    }

    @Test
    public void DJNZ_jumps_to_proper_address_if_B_does_not_decrease_to_zero() {
        short instructionAddress = fixture.create(Short.TYPE);

        sut.setB((byte) 0);
        executeAt(instructionAddress, DJNZ_opcode, null, (byte) 0x7F);
        assertEquals(add(instructionAddress, 129) & 0xffff, sut.getPC());

        sut.setB((byte) 0);
        executeAt(instructionAddress, DJNZ_opcode, null, (byte) 0x80);
        assertEquals(sub(instructionAddress, 126) & 0xffff, sut.getPC());
    }

    @Test
    public void DJNZ_returns_proper_T_states_when_jump_is_done() {
        sut.setB((byte) 0);
        int states = execute(DJNZ_opcode, null);

        assertEquals(13, states);
    }

    @Test
    public void DJNZ_does_not_modify_flags() {
        assertNoFlagsAreModified(DJNZ_opcode, null);
    }
}