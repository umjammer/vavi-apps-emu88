package instructions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class EX_AF_AF_tests extends InstructionsExecutionTestsBase {

    private static final byte EX_AF_AF_opcode = 0x08;

    @Test
    public void EX_AF_AF_exchanges_the_AF_registers() {
        short mainValue = fixture.create(Short.TYPE);
        short alternateValue = fixture.create(Short.TYPE);

        sut.setAF(mainValue & 0xffff);
        sut.setAF2(alternateValue & 0xffff);

        execute(EX_AF_AF_opcode, null);

        assertEquals(alternateValue & 0xffff, sut.getAF());
        assertEquals(mainValue & 0xffff, sut.getAF2());
    }

    @Test
    public void EX_AF_AF_returns_proper_T_states() {
        int states = execute(EX_AF_AF_opcode, null);
        assertEquals(4, states);
    }
}
