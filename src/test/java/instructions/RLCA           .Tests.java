package instructions;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;


class RLCA_tests extends InstructionsExecutionTestsBase {

    private static final byte RLCA_opcode = 0x07;

    @Test
    public void RLCA_rotates_byte_correctly() {
        final byte[] values = new byte[] {0xA, 0x14, 0x28, 0x50, (byte) 0xA0, 0x41, (byte) 0x82, 0x05};
        sut.setA(0x05);

        for (byte value : values) {
            execute(RLCA_opcode, null);
            assertEquals(value & 0xff, sut.getA());
        }
    }

    @Test
    public void RLCA_sets_CF_correctly() {
        sut.setA(0x60);

        execute(RLCA_opcode, null);
        assertEquals(false, sut.isC());

        execute(RLCA_opcode, null);
        assertEquals(true, sut.isC());

        execute(RLCA_opcode, null);
        assertEquals(true, sut.isC());

        execute(RLCA_opcode, null);
        assertEquals(false, sut.isC());
    }

    @Test
    public void RLCA_resets_H_and_N() {
        assertResetsFlags(RLCA_opcode, null, "H", "N");
    }

    @Test
    public void RLCA_does_not_change_SF_ZF_PF() {
        assertDoesNotChangeFlags(RLCA_opcode, null, "S", "Z", "P");
    }

    @Disabled("not implemented")
    @ParameterizedTest
    @ValueSource(ints = {0x00, 0xD7, 0x28, 0xFF})
    public void RLCA_sets_bits_3_and_5_from_A(int value) {
        sut.setA(value);
        execute(RLCA_opcode, null);
        assertEquals(getBit((byte) (sut.getA() & 0xff), 3), sut.is3());
        assertEquals(getBit((byte) (sut.getA() & 0xff), 5), sut.is5());
    }

    @Test
    public void RLCA_returns_proper_T_states() {
        int states = execute(RLCA_opcode, null);
        assertEquals(4, states);
    }
}
