package instructions;

import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class INC_r_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> INC_r_Source() {
        return Stream.of(
                arguments("A", (byte) 0x3C, null),
                arguments("B", (byte) 0x04, null),
                arguments("C", (byte) 0x0C, null),
                arguments("D", (byte) 0x14, null),
                arguments("E", (byte) 0x1C, null),
                arguments("H", (byte) 0x24, null),
                arguments("L", (byte) 0x2C, null)//, // TODO ixy,ihl are not implemented
//                arguments("IXH", (byte) 0x24, (byte) 0xDD),
//                arguments("IXL", (byte) 0x2C, (byte) 0xDD),
//                arguments("IYH", (byte) 0x24, (byte) 0xFD),
//                arguments("IYL", (byte) 0x2C, (byte) 0xFD)
        );
    }

    @ParameterizedTest
    @MethodSource("INC_r_Source")
    public void INC_r_increases_value_appropriately(String reg, byte opcode, Byte prefix) {
        setReg(reg, (byte) 0xFE);
        execute(opcode, prefix);
        assertEquals((byte) 0xFF, this.getRegB(reg));

        execute(opcode, prefix);
        assertEquals((byte) 0x00, this.getRegB(reg));

        execute(opcode, prefix);
        assertEquals((byte) 0x01, this.getRegB(reg));
    }

    @ParameterizedTest
    @MethodSource("INC_r_Source")
    public void INC_r_sets_SF_appropriately(String reg, byte opcode, Byte prefix) {
        setReg(reg, (byte) 0xFD);

        execute(opcode, prefix);
        assertEquals(true, sut.isS());

        execute(opcode, prefix);
        assertEquals(true, sut.isS());

        execute(opcode, prefix);
        assertEquals(false, sut.isS());

        execute(opcode, prefix);
        assertEquals(false, sut.isS());
    }

    @ParameterizedTest
    @MethodSource("INC_r_Source")
    public void INC_r_sets_ZF_appropriately(String reg, byte opcode, Byte prefix) {
        setReg(reg, (byte) 0xFD);

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
    @MethodSource("INC_r_Source")
    public void INC_r_sets_HF_appropriately(String reg, byte opcode, Byte prefix) {
        for (byte b : new byte[] {0x0E, 0x7E, (byte) 0xFE}) {
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
    @MethodSource("INC_r_Source")
    public void INC_r_sets_PF_appropriately(String reg, byte opcode, Byte prefix) {
        setReg(reg, (byte) 0x7E);

        execute(opcode, prefix);
        assertEquals(false, sut.isP());

        execute(opcode, prefix);
        assertEquals(true, sut.isP());

        execute(opcode, prefix);
        assertEquals(false, sut.isP());
    }

    @ParameterizedTest
    @MethodSource("INC_r_Source")
    public void INC_r_resets_NF(String reg, byte opcode, Byte prefix) {
        assertResetsFlags(opcode, prefix, "N");
    }

    @ParameterizedTest
    @MethodSource("INC_r_Source")
    public void INC_r_does_not_change_CF(String reg, byte opcode, Byte prefix) {
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
    @MethodSource("INC_r_Source")
    public void INC_r_sets_bits_3_and_5_from_result(String reg, byte opcode, Byte prefix) {
        setReg(reg, withBit(withBit(((byte) 0), 3, true), 5, false));
        execute(opcode, prefix);
        assertEquals(true, sut.is3());
        assertEquals(false, sut.is5());

        setReg(reg, withBit(withBit(((byte) 0), 3, false), 5, true));
        execute(opcode, prefix);
        assertEquals(false, sut.is3());
        assertEquals(true, sut.is5());
    }

    @ParameterizedTest
    @MethodSource("INC_r_Source")
    public void INC_r_returns_proper_T_states(String reg, byte opcode, Byte prefix) {
        int states = execute(opcode, prefix);
        assertEquals(ifIndexRegister(reg, 8, 4), states);
    }
}
