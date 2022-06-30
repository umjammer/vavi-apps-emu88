package instructions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class EX_DE_HL_tests extends InstructionsExecutionTestsBase {

    private static final byte EX_DE_HL_opcode = (byte) 0xEB;

    @Test
    public void EX_DE_HL_exchanges_the_AF_registers() {
        short DE = fixture.create(Short.TYPE);
        short HL = fixture.create(Short.TYPE);

        sut.setDE(DE);
        sut.setHL(HL);

        execute(EX_DE_HL_opcode, null);

        assertEquals(HL, sut.getDE());
        assertEquals(DE, sut.getHL());
    }

    @Test
    public void EX_DE_HL_does_not_change_flags() {
        assertNoFlagsAreModified(EX_DE_HL_opcode, null);
    }

    @Test
    public void EX_DE_HL_returns_proper_T_states() {
        int states = execute(EX_DE_HL_opcode, null);
        assertEquals(4, states);
    }
}