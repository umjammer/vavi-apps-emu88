package instructions;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;


class BIT_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> BIT_Source() {
        return getBitInstructionsSource((byte) 0x40, false, true).stream();
    }

    private byte offset;

    @BeforeEach
    protected void setup() {
        super.setup();
        offset = fixture.create(Byte.TYPE);
    }

    @ParameterizedTest
    @MethodSource("BIT_Source")
    public void BIT_gets_bit_correctly(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        byte value = withBit(((byte) 0), bit, true);
        setupRegOrMem(reg, value, offset);
        executeBit(opcode, prefix, offset);
        assertEquals(false, sut.isZ());

        value = withBit(((byte) 0xFF), bit, false);
        setupRegOrMem(reg, value, offset);
        executeBit(opcode, prefix, offset);
        assertEquals(true, sut.isZ());
    }

    @ParameterizedTest
    @MethodSource("BIT_Source")
    public void BIT_sets_PF_as_ZF(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        for (int i = 0; i < 256; i++) {
            setupRegOrMem(reg, (byte) i, offset);
            executeBit(opcode, prefix, offset);
            assertEquals(sut.isZ(), sut.isP());
        }
    }

    @ParameterizedTest
    @MethodSource("BIT_Source")
    public void BIT_sets_SF_if_bit_is_7_and_is_set(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        for (int i = 0; i < 256; i++) {
            byte b = (byte) i;
            setupRegOrMem(reg, b, offset);
            executeBit(opcode, prefix, offset);
            boolean expected = (bit == 7) && getBit(b, 7);
            assertEquals(expected, sut.isS());
        }
    }

    @ParameterizedTest
    @MethodSource("BIT_Source")
    public void BIT_resets_N(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        assertResetsFlags(() -> executeBit(opcode, prefix, offset), opcode, prefix, "N");
    }

    @ParameterizedTest
    @MethodSource("BIT_Source")
    public void BIT_sets_H(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        assertSetsFlags(() -> executeBit(opcode, prefix, offset), opcode, prefix, "H");
    }

    @ParameterizedTest
    @MethodSource("BIT_Source")
    public void BIT_does_not_modify_CF(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        assertDoesNotChangeFlags(() -> executeBit(opcode, prefix, offset), opcode, prefix, "C");
    }

    @ParameterizedTest
    @MethodSource("BIT_Source")
    public void BIT_returns_proper_T_states(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        int states = executeBit(opcode, prefix, offset);
        assertEquals(reg.equals("(HL)") ? 12 : reg.startsWith("(") ? 20 : 8, states);
    }
}
