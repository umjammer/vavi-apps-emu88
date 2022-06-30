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


class RR_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> RR_Source() {
         return getBitInstructionsSource((byte) 0x18, true, false).stream();
    }

    private byte offset;

    @BeforeEach
    protected void setup() {
        super.setup();
        offset = fixture.create(Byte.TYPE);
    }

    @ParameterizedTest
    @MethodSource("RR_Source")
    public void RR_rotates_byte_and_loads_register_correctly(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        byte[] values = new byte[] {0x60, 0x30, 0x18, 0xC, 0x6, 0x3, 0x1, 0x0};
        setupRegOrMem(reg, (byte) 0xC0, offset);

        for (byte value : values) {
            executeBit(opcode, prefix, offset);
            assertEquals(value, valueOfRegOrMem(reg, offset) & 0x7F);
            if (destReg != null && !destReg.isEmpty())
                assertEquals(value, valueOfRegOrMem(destReg, offset) & 0x7F);
        }
    }

    @ParameterizedTest
    @MethodSource("RR_Source")
    public void RR_sets_bit_7_from_CF(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        setupRegOrMem(reg, (byte) (fixture.create(Byte.TYPE) | 0X80), offset);
        sut.setC(false);
        executeBit(opcode, prefix, offset);
        assertFalse(getBit(valueOfRegOrMem(reg, offset), 7));

        setupRegOrMem(reg, (byte) (fixture.create(Byte.TYPE) & 0x7f), offset);
        sut.setC(true);
        executeBit(opcode, prefix, offset);
        assertTrue(getBit(valueOfRegOrMem(reg, offset), 7));
    }

    @ParameterizedTest
    @MethodSource("RR_Source")
    public void RR_sets_CF_correctly(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        setupRegOrMem(reg, (byte) 0x06, offset);

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
    @MethodSource("RR_Source")
    public void RR_resets_H_and_N(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        assertResetsFlags(() -> executeBit(opcode, prefix, offset), opcode, prefix, "H", "N");
    }

    @ParameterizedTest
    @MethodSource("RR_Source")
    public void RR_sets_SF_appropriately(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        sut.setC(true);
        executeBit(opcode, prefix, offset);
        assertEquals(true, sut.isS());

        sut.setC(false);
        executeBit(opcode, prefix, offset);
        assertEquals(false, sut.isS());
    }

    @ParameterizedTest
    @MethodSource("RR_Source")
    public void RR_sets_ZF_appropriately(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        for (int i = 0; i < 256; i++) {
            setupRegOrMem(reg, (byte) i, offset);
            executeBit(opcode, prefix, offset);
            assertEquals(valueOfRegOrMem(reg, offset) == 0, sut.isZ());
        }
    }

    @ParameterizedTest
    @MethodSource("RR_Source")
    public void RR_sets_PV_appropriately(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        for (int i = 0; i < 256; i++) {
            setupRegOrMem(reg, (byte) i, offset);
            executeBit(opcode, prefix, offset);
            assertEquals(parity[valueOfRegOrMem(reg, offset) & 0xff] != 0, sut.isP());
        }
    }

    @Disabled("not implemented")
    @ParameterizedTest
    @MethodSource("RR_Source")
    public void RR_sets_bits_3_and_5_from_result(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        for (byte b : new byte[] {0x00, (byte) 0xD7, 0x28, (byte) 0xFF}) {
            setupRegOrMem(reg, b, offset);
            executeBit(opcode, prefix, offset);
            byte value = valueOfRegOrMem(reg, offset);
            assertEquals(getBit(value, 3), sut.is3());
            assertEquals(getBit(value, 5), sut.is5());
        }
    }

    @ParameterizedTest
    @MethodSource("RR_Source")
    public void RR_returns_proper_T_states(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        int states = executeBit(opcode, prefix, offset);
        assertEquals(reg.equals("(HL)") ? 15 : reg.startsWith("(I") ? 23 : 8, states);
    }
}
