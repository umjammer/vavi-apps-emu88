package instructions;

import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class IN_r_C extends InstructionsExecutionTestsBase {

    static Stream<Arguments> IN_r_C_Source() {
        return Stream.of(
                arguments("A", (byte) 0x78),
                arguments("B", (byte) 0x40),
                arguments("C", (byte) 0x48),
                arguments("D", (byte) 0x50),
                arguments("E", (byte) 0x58),
                arguments("H", (byte) 0x60),
                arguments("L", (byte) 0x68)
        );
    }

    static Stream<Arguments> IN_F_C_Source() {
        return Stream.of(
                arguments("F", (byte) 0x70)
        );
    }

    @ParameterizedTest
    @MethodSource("IN_r_C_Source")
    public void IN_r_C_reads_value_from_port(String reg, byte opcode) {
        byte portNumber = fixture.create(Byte.TYPE);
        byte value = fixture.create(Byte.TYPE);
        byte oldValue = fixture.create(Byte.TYPE);

        if (!reg.equals("C"))
            setReg(reg, value);
        sut.setA(oldValue & 0xff);

        executeCase(opcode, portNumber, value);

        assertEquals(value, this.getRegB(reg));
    }

    @ParameterizedTest
    @MethodSource({"IN_r_C_Source", "IN_F_C_Source"})
    public void IN_r_C_sets_SF_appropriately(String reg, byte opcode) {
        byte portNumber = fixture.create(Byte.TYPE);

        executeCase(opcode, portNumber, (byte) 0xFE);
        assertEquals(true, sut.isS());

        executeCase(opcode, portNumber, (byte) 0xFF);
        assertEquals(true, sut.isS());

        executeCase(opcode, portNumber, (byte) 0);
        assertEquals(false, sut.isS());

        executeCase(opcode, portNumber, (byte) 1);
        assertEquals(false, sut.isS());
    }

    @ParameterizedTest
    @MethodSource({"IN_r_C_Source", "IN_F_C_Source"})
    public void IN_r_C_sets_ZF_appropriately(String reg, byte opcode) {
        byte portNumber = fixture.create(Byte.TYPE);

        executeCase(opcode, portNumber, (byte) 0xFF);
        assertEquals(false, sut.isZ());

        executeCase(opcode, portNumber, (byte) 0);
        assertEquals(true, sut.isZ());

        executeCase(opcode, portNumber, (byte) 1);
        assertEquals(false, sut.isZ());
    }

    @ParameterizedTest
    @MethodSource({"IN_r_C_Source", "IN_F_C_Source"})
    public void IN_r_C_resets_HF_NF(String reg, byte opcode) {
        assertResetsFlags(opcode, (byte) 0xED, "H", "N");
    }

    @ParameterizedTest
    @MethodSource({"IN_r_C_Source", "IN_F_C_Source"})
    public void IN_r_C_does_not_change_CF(String reg, byte opcode) {
        byte[] randomValues = fixture.create(byte[].class);
        byte portNumber = fixture.create(Byte.TYPE);

        for (byte value : randomValues) {
            sut.setC(false);
            executeCase(opcode, portNumber, value);
            assertEquals(false, sut.isC());

            sut.setC(true);
            executeCase(opcode, portNumber, value);
            assertEquals(true, sut.isC());
        }
    }

    @ParameterizedTest
    @MethodSource({"IN_r_C_Source", "IN_F_C_Source"})
    public void IN_r_C_sets_PF_as_parity(String reg, byte opcode) {
        byte[] randomValues = fixture.create(byte[].class);
        byte portNumber = fixture.create(Byte.TYPE);

        for (byte value : randomValues) {
            executeCase(opcode, portNumber, value);
            assertEquals(parity[value & 0xff] != 0, sut.isP());
        }
    }

    @Disabled("not implemented")
    @ParameterizedTest
    @MethodSource({"IN_r_C_Source", "IN_F_C_Source"})
    public void IN_r_C_sets_bits_3_and_5_from_result(String reg, byte opcode) {
        byte portNumber = fixture.create(Byte.TYPE);
        byte value = withBit(withBit(((byte) 0), 3, true), 5, false);
        executeCase(opcode, portNumber, value);
        assertEquals(true, sut.is3());
        assertEquals(false, sut.is5());

        value = withBit(withBit(((byte) 0), 3, false), 5, true);
        executeCase(opcode, portNumber, value);
        assertEquals(false, sut.is3());
        assertEquals(true, sut.is5());
    }

    @ParameterizedTest
    @MethodSource({"IN_r_C_Source", "IN_F_C_Source"})
    public void IN_r_C_returns_proper_T_states(String reg, byte opcode) {
        byte portNumber = fixture.create(Byte.TYPE);
        byte value = fixture.create(Byte.TYPE);
        int states = executeCase(opcode, portNumber, value);
        assertEquals(12, states);
    }

    private int executeCase(byte opcode, byte portNumber, byte value) {
        sut.setC(portNumber & 0xff);
        setPortValue(portNumber, value);
        return execute(opcode, (byte) 0xED);
    }
}

