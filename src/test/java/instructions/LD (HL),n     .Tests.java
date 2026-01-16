package instructions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class LD_aHL_n_tests extends InstructionsExecutionTestsBase {

    private static final byte LD_aHL_n_opcode = 0x36;

    @Test
    public void LD_aHL_n_loads_value_in_memory() {
        short address = fixture.create(Short.TYPE);
        byte oldValue = fixture.create(Byte.TYPE);
        byte newValue = fixture.create(Byte.TYPE);

        sut.getBus().pokeb(address & 0xffff, oldValue & 0xff);
        sut.setHL(address & 0xffff);

        execute(LD_aHL_n_opcode, null, newValue);

        assertEquals(newValue & 0xff, sut.getBus().peekb(address & 0xffff));
    }

    @Test
    public void LD_aHL_n_does_not_modify_flags() {
        assertNoFlagsAreModified(LD_aHL_n_opcode, null);
    }

    @Test
    public void LD_aHL_n_returns_proper_T_states() {
        int states = execute(LD_aHL_n_opcode, null);
        assertEquals(10, states);
    }
}
