package instructions;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class RRA_tests extends InstructionsExecutionTestsBase {

    private static final byte RRA_opcode = 0x1F;

    @Test
    public void RRA_rotates_byte_correctly() {
        final byte[] values = new byte[] {0x60, 0x30, 0x18, 0xC, 0x6, 0x3, 0x1, 0x0};
        sut.setA(0xC0);

        for (byte value : values) {
            execute(RRA_opcode, null);
            assertEquals(value & 0xff, sut.getA() & 0x7F);
        }
    }

    @Test
    public void RLA_sets_bit_7_from_CF() {
        sut.setA(fixture.create(Byte.TYPE) | 0x80);
        sut.setC(false);
        execute(RRA_opcode, null);
        assertEquals(false, getBit((byte) (sut.getA() & 0xff), 7));

        sut.setA(fixture.create(Byte.TYPE) & 0x7F);
        sut.setC(true);
        execute(RRA_opcode, null);
        assertEquals(true, getBit((byte) (sut.getA() & 0xff), 7));
    }

    @Test
    public void RRA_sets_CF_correctly() {
        sut.setA(0x06);

        execute(RRA_opcode, null);
        assertEquals(false, sut.isC());

        execute(RRA_opcode, null);
        assertEquals(true, sut.isC());

        execute(RRA_opcode, null);
        assertEquals(true, sut.isC());

        execute(RRA_opcode, null);
        assertEquals(false, sut.isC());
    }

    @Test
    public void RRA_resets_H_and_N() {
        assertResetsFlags(RRA_opcode, null, "H", "N");
    }

    @Test
    public void RRA_does_not_change_SF_ZF_PF() {
        assertDoesNotChangeFlags(RRA_opcode, null, "S", "Z", "P");
    }

    @Disabled("not implemented")
    @ParameterizedTest
    @ValueSource(ints = {0x00, 0xD7, 0x28, 0xFF})
    public void RRA_sets_bits_3_and_5_from_A(int value) {
        sut.setA(value);
        execute(RRA_opcode, null);
        assertEquals(getBit((byte) (sut.getA() & 0xff), 3), sut.is3());
        assertEquals(getBit((byte) (sut.getA() & 0xff), 5), sut.is5());
    }

    @Test
    public void RRA_returns_proper_T_states() {
        int states = execute(RRA_opcode, null);
        assertEquals(4, states);
    }
}
