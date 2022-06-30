package instructions;

import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class LD_A_I_R_tests extends InstructionsExecutionTestsBase {

    private static final byte prefix = (byte) 0xED;

    static Stream<Arguments> LD_A_R_I_Source() {
        return Stream.of(
                arguments("I", (byte) 0x57),
                arguments("R", (byte) 0x5F)
        );
    }

    @ParameterizedTest
    @MethodSource("LD_A_R_I_Source")
    public void LD_A_I_R_loads_value_correctly(String reg, byte opcode) {
        byte oldValue = fixture.create(Byte.TYPE);
        byte newValue = fixture.create(Byte.TYPE);
        sut.setA(oldValue);
        setReg(reg, newValue);

        execute(opcode, prefix);

        //Account for R being increased on instruction execution
        if (reg.equals("R"))
            newValue = inc7Bits(inc7Bits(newValue));

        assertEquals(newValue, sut.getA());
    }

    @ParameterizedTest
    @MethodSource("LD_A_R_I_Source")
    public void LD_A_I_R_returns_proper_T_states(String reg, byte opcode) {
        int states = execute(opcode, prefix);
        assertEquals(9, states);
    }

    @ParameterizedTest
    @MethodSource("LD_A_R_I_Source")
    public void LD_A_I_R_sets_SF_properly(String reg, byte opcode) {
        for (int i = 0; i <= 255; i++) {
            byte b = (byte) i;
            setReg(reg, b);
            execute(opcode, prefix);
            assertEquals((b & 0xff) >= 128, sut.isS());
        }
    }

    @ParameterizedTest
    @MethodSource("LD_A_R_I_Source")
    public void LD_A_I_R_sets_ZF_properly(String reg, byte opcode) {
        for (int i = 0; i <= 255; i++) {
            byte b = (byte) i;
            setReg(reg, b);
            execute(opcode, prefix);

            //Account for R being increased on instruction execution
            if (reg.equals("R"))
                b = inc7Bits(inc7Bits(b));

            assertEquals(b == 0, sut.isZ());
        }
    }

    @ParameterizedTest
    @MethodSource("LD_A_R_I_Source")
    public void LD_A_I_R_sets_PF_from_IFF2(String reg, byte opcode) {
        setReg(reg, fixture.create(Byte.TYPE));

        sut.setIff2(false);
        execute(opcode, prefix);
        assertEquals(false, sut.isP());

        sut.setIff2(true);
        execute(opcode, prefix);
        assertEquals(true, sut.isP());
    }

    @ParameterizedTest
    @MethodSource("LD_A_R_I_Source")
    public void LD_A_I_R_resets_HF_and_NF_properly(String reg, byte opcode) {
        assertResetsFlags(opcode, prefix, "H", "N");
    }

    @ParameterizedTest
    @MethodSource("LD_A_R_I_Source")
    public void LD_A_I_R_does_not_change_CF(String reg, byte opcode) {
        assertDoesNotChangeFlags(opcode, prefix, "C");
    }

    @Disabled("not implemented")
    @ParameterizedTest
    @MethodSource("LD_A_R_I_Source")
    public void LD_A_I_R_sets_flags_3_5_from_I(String reg, byte opcode) {
        setReg(reg, withBit(withBit(((byte) 1), 3, true), 5, false));
        execute(opcode, prefix);
        assertEquals(true, sut.is3());
        assertEquals(false, sut.is5());

        setReg(reg, withBit(withBit(((byte) 1), 3, false), 5, true));
        execute(opcode, prefix);
        assertEquals(false, sut.is3());
        assertEquals(true, sut.is5());
    }
}
