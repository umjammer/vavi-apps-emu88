package instructions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class EXX_tests extends InstructionsExecutionTestsBase {
    private static final byte EXX_opcode = (byte) 0xD9;

    @Test
    public void EXX_exchanges_registers_correctly() {
        short BC = fixture.create(Short.TYPE);
        short DE = fixture.create(Short.TYPE);
        short HL = fixture.create(Short.TYPE);
        short altBC = fixture.create(Short.TYPE);
        short altDE = fixture.create(Short.TYPE);
        short altHL = fixture.create(Short.TYPE);

        sut.setBC(BC);
        sut.setDE(DE);
        sut.setHL(HL);
        sut.setBC2(altBC);
        sut.setDE2(altDE);
        sut.setHL2(altHL);

        execute(EXX_opcode, null);

        assertEquals(altBC, sut.getBC());
        assertEquals(altDE, sut.getDE());
        assertEquals(altHL, sut.getHL());
        assertEquals(BC, sut.getBC2());
        assertEquals(DE, sut.getDE2());
        assertEquals(HL, sut.getHL2());
    }

    @Test
    public void EXX_does_not_change_flags() {
        assertNoFlagsAreModified(EXX_opcode, null);
    }

    @Test
    public void EXX_returns_proper_T_states() {
        int states = execute(EXX_opcode, null);
        assertEquals(4, states);
    }
}