package instructions;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;


class CCF_tests extends InstructionsExecutionTestsBase {

    private static final byte CCF_opcode = 0x3F;

    @Test
    void CCF_complements_CF_correctly() {
        sut.setC(false);
        execute(CCF_opcode, null);
        assertEquals(true, sut.isC());

        sut.setC(true);
        execute(CCF_opcode, null);
        assertEquals(false, sut.isC());
    }

    @Test
    void CCF_sets_H_as_previous_carry() {
        sut.setC(false);
        sut.setH(true);
        execute(CCF_opcode, null);
        assertEquals(false, sut.isH());

        sut.setC(true);
        sut.setH(false);
        execute(CCF_opcode, null);
        assertEquals(true, sut.isH());
    }

    @Test
    void CCF_resets_N() {
        assertResetsFlags(CCF_opcode, null, "N");
    }

    @Test
    void CCF_does_not_change_SF_ZF_PF() {
        assertDoesNotChangeFlags(CCF_opcode, null, "S", "Z", "P");
    }

    @Disabled("not implemented")
    @ParameterizedTest
    @ValueSource(ints = {0x00, 0x0F, 0xF0, 0xFF})
    void CCF_sets_bits_3_and_5_from_A(int value) {
        sut.setA((byte) value);
        execute(CCF_opcode, null);
        assertEquals(getBit((byte) (sut.getA() & 0xff), 3), sut.is3());
        assertEquals(getBit((byte) (sut.getA() & 0xff), 5), sut.is5());
    }

    @Test
    void CCF_returns_proper_T_states() {
        int states = execute(CCF_opcode, null);
        assertEquals(4, states);
    }
}
