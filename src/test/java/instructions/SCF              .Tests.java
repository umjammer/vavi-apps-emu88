package instructions;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;


class SCF_tests extends InstructionsExecutionTestsBase {

    private static final byte SCF_opcode = 0x37;

    @Test
    public void SCF_sets_CF_correctly() {
        sut.setC(false);

        execute(SCF_opcode, null);

        assertEquals(true, sut.isC());
    }

    @Test
    public void SCF_resets_H_and_N() {
        assertResetsFlags(SCF_opcode, null, "H", "N");
    }

    @Test
    public void SCF_does_not_change_SF_ZF_PF() {
        assertDoesNotChangeFlags(SCF_opcode, null, "S", "Z", "P");
    }

    @Disabled("not implemented")
    @Test
    public void SCF_sets_bits_3_and_5_from_A() {
        sut.setA(withBit((byte) (sut.getA() & 0xff), 3, true));
        sut.setA(withBit((byte) (sut.getA() & 0xff), 5, false));
        execute(SCF_opcode, null);
        assertEquals(true, sut.is3());
        assertEquals(false, sut.is5());

        sut.setA(withBit((byte) (sut.getA() & 0xff), 3, false));
        sut.setA(withBit((byte) (sut.getA() & 0xff), 5, true));
        execute(SCF_opcode, null);
        assertEquals(false, sut.is3());
        assertEquals(true, sut.is5());
    }

    @Disabled("not implemented")
    @ParameterizedTest
    @ValueSource(ints = {0x00, 0x0F, 0xF0, 0xFF})
    public void SCF_sets_bits_3_and_5_from_A(int value) {
        sut.setA(value);
        execute(SCF_opcode, null);
        assertEquals(getBit((byte) (sut.getA() & 0xff), 3), sut.is3());
        assertEquals(getBit((byte) (sut.getA() & 0xff), 5), sut.is5());
    }

    @Test
    public void SCF_returns_proper_T_states() {
        int states = execute(SCF_opcode, null);
        assertEquals(4, states);
    }
}
