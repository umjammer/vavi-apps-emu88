package instructions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class LD_aa_A_tests extends InstructionsExecutionTestsBase {

    private static final byte LD_aa_A_opcode = 0x32;

    @Test
    public void LD_aa_A_loads_value_in_memory() {
        short address = fixture.create(Short.TYPE);
        byte oldValue = fixture.create(Byte.TYPE);
        byte newValue = fixture.create(Byte.TYPE);

        sut.setA(newValue & 0xff);
        sut.getBus().pokeb(address & 0xffff, oldValue & 0xff);

        execute(LD_aa_A_opcode, null, getLowByte(address), getHighByte(address));

        assertEquals(newValue & 0xff, sut.getBus().peekb(address & 0xffff));
    }

    @Test
    public void LD_aa_A_does_not_modify_flags() {
        assertNoFlagsAreModified(LD_aa_A_opcode, null);
    }

    @Test
    public void LD_rr_r_returns_proper_T_states() {
        int states = execute(LD_aa_A_opcode, null);
        assertEquals(13, states);
    }
}
