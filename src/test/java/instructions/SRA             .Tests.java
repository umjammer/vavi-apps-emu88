package instructions;

import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;


class SRA_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> SRA_Source() {
        return getBitInstructionsSource((byte) 0x28, true, false).stream();
    }

    private byte offset;

    @ParameterizedTest
    @MethodSource("SRA_Source")
    public void SRA_shifts_negative_byte_and_loads_register_correctly(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        final byte[] values = new byte[] {(byte) 0xC0, (byte) 0xE0, (byte) 0xF0, (byte) 0xF8, (byte) 0xFC, (byte) 0xFE, (byte) 0xFF};
        setupRegOrMem(reg, (byte) 0x80, offset);

        for (byte value : values) {
            sut.setC(true);
            executeBit(opcode, prefix, offset);
            assertEquals(value, valueOfRegOrMem(reg, offset));
            if (destReg != null && !destReg.isEmpty())
                assertEquals(value, valueOfRegOrMem(destReg, offset));
        }
    }

    @ParameterizedTest
    @MethodSource("SRA_Source")
    public void SRA_shifts_positive_byte_and_loads_register_correctly(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        final byte[] values = new byte[] {0x20, 0x10, 0x08, 0x04, 0x02, 0x01, 0};
        setupRegOrMem(reg, (byte) 0x40, offset);

        for (byte value : values) {
            sut.setC(true);
            executeBit(opcode, prefix, offset);
            assertEquals(value, valueOfRegOrMem(reg, offset));
        }
    }

    @ParameterizedTest
    @MethodSource("SRA_Source")
    public void SRA_sets_CF_from_bit_0(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        setupRegOrMem(reg, (byte) (fixture.create(Byte.TYPE) | 0x01), offset);
        sut.setC(false);
        executeBit(opcode, prefix, offset);
        assertEquals(true, sut.isC());

        setupRegOrMem(reg, (byte) (fixture.create(Byte.TYPE) & 0xFE), offset);
        sut.setC(true);
        executeBit(opcode, prefix, offset);
        assertEquals(false, sut.isC());
    }

    @ParameterizedTest
    @MethodSource("SRA_Source")
    public void SRA_resets_H_and_N(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        assertResetsFlags(() -> executeBit(opcode, prefix, offset), opcode, prefix, "H", "N");
    }

    @ParameterizedTest
    @MethodSource("SRA_Source")
    public void SRA_sets_SF_appropriately(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        setupRegOrMem(reg, (byte) (fixture.create(Byte.TYPE) | 0x80), offset);
        executeBit(opcode, prefix, offset);
        assertEquals(true, sut.isS());

        setupRegOrMem(reg, (byte) (fixture.create(Byte.TYPE) & 0x7F), offset);
        executeBit(opcode, prefix, offset);
        assertEquals(false, sut.isS());
    }

    @ParameterizedTest
    @MethodSource("SRA_Source")
    public void SRA_sets_ZF_appropriately(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        for (int i = 0; i < 256; i++) {
            setupRegOrMem(reg, (byte) i, offset);
            executeBit(opcode, prefix, offset);
            assertEquals(valueOfRegOrMem(reg, offset) == 0, sut.isZ());
        }
    }

    @ParameterizedTest
    @MethodSource("SRA_Source")
    public void SRA_sets_PV_appropriately(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        for (int i = 0; i < 256; i++) {
            setupRegOrMem(reg, (byte) i, offset);
            executeBit(opcode, prefix, offset);
            assertEquals(parity[valueOfRegOrMem(reg, offset) & 0xff] != 0, sut.isP());
        }
    }

    @Disabled("not implemented")
    @ParameterizedTest
    @MethodSource("SRA_Source")
    public void SRA_sets_bits_3_and_5_from_result(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        for (byte b : new byte[] {0x00, (byte) 0xD7, 0x28, (byte) 0xFF}) {
            setupRegOrMem(reg, b, offset);
            executeBit(opcode, prefix, offset);
            byte value = valueOfRegOrMem(reg, offset);
            assertEquals(getBit(value, 3), sut.is3());
            assertEquals(getBit(value, 5), sut.is5());
        }
    }

    @ParameterizedTest
    @MethodSource("SRA_Source")
    public void SRA_returns_proper_T_states(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        int states = executeBit(opcode, prefix, offset);
        assertEquals(reg.equals("(HL)") ? 15 : reg.startsWith("(I") ? 23 : 8, states);
    }
}