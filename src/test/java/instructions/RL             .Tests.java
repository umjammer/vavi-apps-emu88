package instructions;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class RL_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> RL_Source() {
        return getBitInstructionsSource((byte) 0x10, true, false).stream();
    }

    private byte offset;

    @BeforeEach
    protected void setup() {
        super.setup();
        offset = fixture.create(Byte.TYPE);
    }

    @ParameterizedTest
    @MethodSource("RL_Source")
    public void RL_rotates_byte_correctly(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        final byte[] values = new byte[] {0x6, 0xC, 0x18, 0x30, 0x60, (byte) 0xC0, (byte) 0x80, 0};
        setupRegOrMem(reg, (byte) 0x03, offset);

        for (byte value : values) {
            executeBit(opcode, prefix, offset);
            assertEquals(value & 0xff, valueOfRegOrMem(reg, offset) & 0xFE);
        }
    }

    @ParameterizedTest
    @MethodSource("RL_Source")
    public void RL_sets_bit_0_from_CF(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        setupRegOrMem(reg, (byte) (fixture.create(Byte.TYPE) | 1), offset);
        sut.setC(false);
        executeBit(opcode, prefix, offset);
        assertFalse(getBit(valueOfRegOrMem(reg, offset), 0));

        setupRegOrMem(reg, (byte) (fixture.create(Byte.TYPE) & 0xFE), offset);
        sut.setC(true);
        executeBit(opcode, prefix, offset);
        assertTrue(getBit(valueOfRegOrMem(reg, offset), 0));
    }

    @ParameterizedTest
    @MethodSource("RL_Source")
    public void RL_sets_CF_correctly(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        setupRegOrMem(reg, (byte) 0x60, offset);

        executeBit(opcode, prefix, offset);
        assertEquals(false, sut.isC());

        executeBit(opcode, prefix, offset);
        assertEquals(true, sut.isC());

        executeBit(opcode, prefix, offset);
        assertEquals(true, sut.isC());

        executeBit(opcode, prefix, offset);
        assertEquals(false, sut.isC());
    }

    @ParameterizedTest
    @MethodSource("RL_Source")
    public void RL_resets_H_and_N(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        assertResetsFlags(() -> executeBit(opcode, prefix, offset), opcode, prefix, "H", "N");
    }

    @ParameterizedTest
    @MethodSource("RL_Source")
    public void RL_sets_SF_appropriately(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        setupRegOrMem(reg, (byte) 0x20, offset);

        executeBit(opcode, prefix, offset);
        assertEquals(false, sut.isS());

        executeBit(opcode, prefix, offset);
        assertEquals(true, sut.isS());

        executeBit(opcode, prefix, offset);
        assertEquals(false, sut.isS());
    }

    @ParameterizedTest
    @MethodSource("RL_Source")
    public void RL_sets_ZF_appropriately(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        for (int i = 0; i < 256; i++) {
            setupRegOrMem(reg, (byte) i, offset);
            executeBit(opcode, prefix, offset);
            assertEquals(valueOfRegOrMem(reg, offset) == 0, sut.isZ());
        }
    }

    @ParameterizedTest
    @MethodSource("RL_Source")
    public void RL_sets_PV_appropriately(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        for (int i = 0; i < 256; i++) {
            setupRegOrMem(reg, (byte) i, offset);
            executeBit(opcode, prefix, offset);
            assertEquals(parity[valueOfRegOrMem(reg, offset) & 0xff] != 0, sut.isP());
        }
    }

    @Disabled("not implemented")
    @ParameterizedTest
    @MethodSource("RL_Source")
    public void RL_sets_bits_3_and_5_from_result(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        for (byte b : new byte[] {0x00, (byte) 0xD7, 0x28, (byte) 0xFF}) {
            setupRegOrMem(reg, b, offset);
            executeBit(opcode, prefix, offset);
            byte value = valueOfRegOrMem(reg, offset);
            assertEquals(getBit(value, 3), sut.is3());
            assertEquals(getBit(value, 5), sut.is5());
        }
    }

    @ParameterizedTest
    @MethodSource("RL_Source")
    public void RL_returns_proper_T_states(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        int states = executeBit(opcode, prefix, offset);
        assertEquals(reg.equals("(HL)") ? 15 : reg.startsWith("(I") ? 23 : 8, states);
    }
}
