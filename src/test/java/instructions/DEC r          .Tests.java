package instructions;

import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class DEC_r_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> DEC_r_Source() {
        return Stream.of(
                arguments("A", (byte) 0x3D, null),
                arguments("B", (byte) 0x05, null),
                arguments("C", (byte) 0x0D, null),
                arguments("D", (byte) 0x15, null),
                arguments("E", (byte) 0x1D, null),
                arguments("H", (byte) 0x25, null),
                arguments("L", (byte) 0x2D, null)//, // TODO ixy,ihl are not implemented
//                arguments("IXH", (byte) 0x25, (byte) 0xDD),
//                arguments("IXL", (byte) 0x2D, (byte) 0xDD),
//                arguments("IYH", (byte) 0x25, (byte) 0xFD),
//                arguments("IYL", (byte) 0x2D, (byte) 0xFD)
        );
    }

    @ParameterizedTest
    @MethodSource("DEC_r_Source")
    public void DEC_r_decreases_value_appropriately(String reg, byte opcode, Byte prefix) {
        setReg(reg, (byte) 0x01);
        execute(opcode, prefix);
        assertEquals((byte) 0x00, this.getRegB(reg));

        execute(opcode, prefix);
        assertEquals((byte) 0xFF, this.getRegB(reg));

        execute(opcode, prefix);
        assertEquals((byte) 0xFE, this.getRegB(reg));
    }

    @ParameterizedTest
    @MethodSource("DEC_r_Source")
    public void DEC_r_sets_SF_appropriately(String reg, byte opcode, Byte prefix) {
        setReg(reg, (byte) 0x02);

        execute(opcode, prefix);
        assertEquals(false, sut.isS());

        execute(opcode, prefix);
        assertEquals(false, sut.isS());

        execute(opcode, prefix);
        assertEquals(true, sut.isS());

        execute(opcode, prefix);
        assertEquals(true, sut.isS());
    }

    @ParameterizedTest
    @MethodSource("DEC_r_Source")
    public void DEC_r_sets_ZF_appropriately(String reg, byte opcode, Byte prefix) {
        setReg(reg, (byte) 0x03);

        execute(opcode, prefix);
        assertEquals(false, sut.isZ());

        execute(opcode, prefix);
        assertEquals(false, sut.isZ());

        execute(opcode, prefix);
        assertEquals(true, sut.isZ());

        execute(opcode, prefix);
        assertEquals(false, sut.isZ());
    }

    @ParameterizedTest
    @MethodSource("DEC_r_Source")
    public void DEC_r_sets_HF_appropriately(String reg, byte opcode, Byte prefix) {
        for (byte b : new byte[] {0x11, (byte) 0x81, (byte) 0xF1}) {
            setReg(reg, b);

            execute(opcode, prefix);
            assertEquals(false, sut.isH());

            execute(opcode, prefix);
            assertEquals(true, sut.isH());

            execute(opcode, prefix);
            assertEquals(false, sut.isH());
        }
    }

    @ParameterizedTest
    @MethodSource("DEC_r_Source")
    public void DEC_r_sets_PF_appropriately(String reg, byte opcode, Byte prefix) {
        setReg(reg, (byte) 0x81);

        execute(opcode, prefix);
        assertEquals(false, sut.isP());

        execute(opcode, prefix);
        assertEquals(true, sut.isP());

        execute(opcode, prefix);
        assertEquals(false, sut.isP());
    }

    @ParameterizedTest
    @MethodSource("DEC_r_Source")
    public void DEC_r_sets_NF(String reg, byte opcode, Byte prefix) {
        byte[] randomValues = fixture.create(byte[].class);

        for (byte value : randomValues) {
            setReg(reg, value);
            sut.setN(false);

            execute(opcode, prefix);
            assertEquals(true, sut.isN());
        }
    }

    @ParameterizedTest
    @MethodSource("DEC_r_Source")
    public void DEC_r_does_not_chance_CF(String reg, byte opcode, Byte prefix) {
        byte[] randomValues = fixture.create(byte[].class);

        for (byte value : randomValues) {
            setReg(reg, value);

            sut.setC(false);
            execute(opcode, prefix);
            assertEquals(false, sut.isC());

            sut.setC(true);
            execute(opcode, prefix);
            assertEquals(true, sut.isC());
        }
    }

    @Disabled("not implemented")
    @ParameterizedTest
    @MethodSource("DEC_r_Source")
    public void DEC_r_sets_bits_3_and_5_from_result(String reg, byte opcode, Byte prefix) {
        setReg(reg, withBit(withBit(((byte) 1), 3, true), 5, false));
        execute(opcode, prefix);
        assertEquals(true, sut.is3());
        assertEquals(false, sut.is5());

        setReg(reg, withBit(withBit(((byte) 1), 3, false), 5, true));
        execute(opcode, prefix);
        assertEquals(false, sut.is3());
        assertEquals(true, sut.is5());
    }

    @ParameterizedTest
    @MethodSource("DEC_r_Source")
    public void DEC_r_returns_proper_T_states(String reg, byte opcode, Byte prefix) {
        int states = execute(opcode, prefix);
        assertEquals(ifIndexRegister(reg, 8, 4), states);
    }
}

