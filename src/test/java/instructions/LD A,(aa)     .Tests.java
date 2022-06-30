package instructions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class LD_A_aa_tests extends InstructionsExecutionTestsBase {

    private static final byte LD_A_aa_opcode = 0x3A;

    @Test
    public void LD_A_aa_loads_value_from_memory() {
        // TODO got error when 1
        short address = createAddressFixture();
        byte oldValue = fixture.create(Byte.TYPE);
        byte newValue = fixture.create(Byte.TYPE);

        sut.setA(oldValue);
        sut.getBus().pokeb(address & 0xffff, newValue & 0xff);

        execute(LD_A_aa_opcode, null, getLowByte(address), getHighByte(address));

        assertEquals(newValue & 0xff, sut.getA());
    }

    @Test
    public void LD_A_aa_does_not_modify_flags() {
        assertNoFlagsAreModified(LD_A_aa_opcode, null);
    }

    @Test
    public void LD_rr_r_returns_proper_T_states() {
        int states = execute(LD_A_aa_opcode, null);
        assertEquals(13, states);
    }
}
