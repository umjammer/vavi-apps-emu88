package instructions;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class RLA_tests extends InstructionsExecutionTestsBase {

    private static final byte RLA_opcode = 0x17;

    @Test
    public void RLA_rotates_byte_correctly() {
        final byte[] values = new byte[] {0x6, 0xC, 0x18, 0x30, 0x60, (byte) 0xC0, (byte) 0x80, 0};
        sut.setA((byte) 0x03);

        for (byte value : values) {
            execute(RLA_opcode, null);
            assertEquals(value, (byte) (sut.getA() & 0xFE));
        }
    }

    @Test
    public void RLA_sets_bit_0_from_CF() {
        sut.setA((byte) (fixture.create(Byte.TYPE) | 1));
        sut.setC(false);
        execute(RLA_opcode, null);
        assertFalse((sut.getA() & 0x01) != 0);

        sut.setA((byte) (fixture.create(Byte.TYPE) & 0xFE));
        sut.setC(true);
        execute(RLA_opcode, null);
        assertTrue((sut.getA() & 0x01) != 0);
    }

    @Test
    public void RLA_sets_CF_correctly() {
        sut.setA((byte) 0x60);

        execute(RLA_opcode, null);
        assertEquals(false, sut.isC());

        execute(RLA_opcode, null);
        assertEquals(true, sut.isC());

        execute(RLA_opcode, null);
        assertEquals(true, sut.isC());

        execute(RLA_opcode, null);
        assertEquals(false, sut.isC());
    }

    @Test
    public void RLA_resets_H_and_N() {
        assertResetsFlags(RLA_opcode, null, "H", "N");
    }

    @Test
    public void RLA_does_not_change_SF_ZF_PF() {
        assertDoesNotChangeFlags(RLA_opcode, null, "S", "Z", "P");
    }

    @Disabled("not implemented")
    @ParameterizedTest
    @ValueSource(ints = {0x00, 0xD7, 0x28, 0xFF})
    public void RLA_sets_bits_3_and_5_from_A(int value) {
        sut.setA((byte) value);
        execute(RLA_opcode, null);
        assertEquals((sut.getA() & 0x80) != 0, sut.is3());
        assertEquals((sut.getA() & 0x10) != 0, sut.is5());
    }

    @Test
    public void RLA_returns_proper_T_states() {
        int states = execute(RLA_opcode, null);
        assertEquals(4, states);
    }
}
