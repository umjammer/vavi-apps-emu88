package instructions;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;


class CPL_tests extends InstructionsExecutionTestsBase {

    private static final byte CPL_opcode = 0x2F;

    @Test
    public void CPL_complements_byte_correctly() {
        byte value = fixture.create(Byte.TYPE);
        sut.setA(value);

        execute(CPL_opcode, null);

        assertEquals((~value) & 0xff, sut.getA());
    }

    @Test
    public void CPL_sets_H_and_N() {
        assertSetsFlags(CPL_opcode, null, "H", "N");
    }

    @Test
    public void CPL_does_not_change_SF_ZF_PF_CF() {
        assertDoesNotChangeFlags(CPL_opcode, null, "S", "Z", "P", "C");
    }

    @Disabled("not implemented")
    @ParameterizedTest
    @ValueSource(ints = {0x00, 0x0F, 0xF0, 0xFF})
    public void CPL_sets_bits_3_and_5_from_A(int value) {
        sut.setA((byte) value);
        execute(CPL_opcode, null);
        assertEquals(getBit((byte) (sut.getA() & 0xff), 3), sut.is3());
        assertEquals(getBit((byte) (sut.getA() & 0xff), 5), sut.is5());
    }

    @Test
    public void CPL_returns_proper_T_states() {
        int states = execute(CPL_opcode, null);
        assertEquals(4, states);
    }
}
