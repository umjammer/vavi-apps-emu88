package instructions;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;


class RRCA_tests extends InstructionsExecutionTestsBase {

    private static final byte RRCA_opcode = 0x0F;

    @Test
    public void RRCA_rotates_byte_correctly() {
        final byte[] values = new byte[] {(byte) 0x82, 0x41, (byte) 0xA0, 0x50, 0x28, 0x14, 0x0A, 0x05};
        sut.setA(0x05);

        for (byte value : values) {
            execute(RRCA_opcode, null);
            assertEquals(value & 0xff, sut.getA());
        }
    }

    @Test
    public void RRCA_sets_CF_correctly() {
        sut.setA(0x06);

        execute(RRCA_opcode, null);
        assertEquals(false, sut.isC());

        execute(RRCA_opcode, null);
        assertEquals(true, sut.isC());

        execute(RRCA_opcode, null);
        assertEquals(true, sut.isC());

        execute(RRCA_opcode, null);
        assertEquals(false, sut.isC());
    }

    @Test
    public void RRCA_resets_H_and_N() {
        assertResetsFlags(RRCA_opcode, null, "H", "N");
    }

    @Test
    public void RRCA_does_not_change_SF_ZF_PF() {
        assertDoesNotChangeFlags(RRCA_opcode, null, "S", "Z", "P");
    }

    @Disabled("not implemented")
    @ParameterizedTest
    @ValueSource(ints = {0x00, 0xD7, 0x28, 0xFF})
    public void RRCA_sets_bits_3_and_5_from_A(int value) {
        sut.setA(value);
        execute(RRCA_opcode, null);
        assertEquals(getBit((byte) (sut.getA() & 0xff), 3), sut.is3());
        assertEquals(getBit((byte) (sut.getA() & 0xff), 5), sut.is5());
    }

    @Test
    public void RRCA_returns_proper_T_states() {
        int states = execute(RRCA_opcode, null);
        assertEquals(4, states);
    }
}
