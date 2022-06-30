package instructions;

import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class ADC_HL_rr_tests extends InstructionsExecutionTestsBase {

    private static final byte prefix = (byte) 0xED;

    static Stream<Arguments> ADC_HL_rr_Source() {
        return Stream.of(
                arguments("BC", (byte) 0x4A),
                arguments("DE", (byte) 0x5A),
                arguments("SP", (byte) 0x7A)
        );
    }

    static Stream<Arguments> ADC_HL_HL_Source() {
        return Stream.of(
                arguments("HL", (byte) 0x6A)
        );
    }

    @ParameterizedTest
    @MethodSource({"ADC_HL_rr_Source", "ADC_HL_HL_Source"})
    public void ADC_HL_rr_adds_both_registers_with_and_without_carry(String src, byte opcode) {
        for (int cf = 0; cf <= 1; cf++) {
            short value1 = fixture.create(Short.TYPE);
            short value2 = (src.equals("HL")) ? value1 : fixture.create(Short.TYPE);

            sut.setHL(value1);
            sut.setC(cf != 0);
            if (!src.equals("HL"))
                setReg(src, value2);

            execute(opcode, prefix);

            assertEquals(add(add(value1, value2), cf) & 0xffff, sut.getHL());
            if (!src.equals("HL"))
                assertEquals(value2, this.getRegW(src));
        }
    }

    @ParameterizedTest
    @MethodSource("ADC_HL_rr_Source")
    public void ADC_HL_rr_sets_SF_appropriately(String src, byte opcode) {
        setup(src, (short) 0xFFFD, (short) 1);
        execute(opcode, prefix);
        assertEquals(true, sut.isS());

        setup(src, (short) 0xFFFE, (short) 1);
        execute(opcode, prefix);
        assertEquals(true, sut.isS());

        setup(src, (short) 0xFFFF, (short) 1);
        execute(opcode, prefix);
        assertEquals(false, sut.isS());

        setup(src, (short) 0x7FFE, (short) 1);
        execute(opcode, prefix);
        assertEquals(false, sut.isS());

        setup(src, (short) 0x7FFF, (short) 1);
        execute(opcode, prefix);
        assertEquals(true, sut.isS());
    }

    private void setup(String src, short oldValue, short valueToSubtract) {
        sut.setHL(oldValue & 0xffff);
        sut.setC(false);

        if (!src.equals("HL")) {
            setReg(src, valueToSubtract);
        }
    }

    @ParameterizedTest
    @MethodSource("ADC_HL_rr_Source")
    public void ADC_HL_rr_sets_ZF_appropriately(String src, byte opcode) {
        setup(src, (short) 0xFFFD, (short) 1);
        execute(opcode, prefix);
        assertEquals(false, sut.isZ());

        setup(src, (short) 0xFFFE, (short) 1);
        execute(opcode, prefix);
        assertEquals(false, sut.isZ());

        setup(src, (short) 0xFFFF, (short) 1);
        execute(opcode, prefix);
        assertEquals(true, sut.isZ());

        setup(src, (short) 0x00, (short) 1);
        execute(opcode, prefix);
        assertEquals(false, sut.isZ());
    }

    @ParameterizedTest
    @MethodSource("ADC_HL_rr_Source")
    public void ADC_HL_rr_sets_HF_appropriately(String src, byte opcode) {
        for (int i : new int[] {0x0FFE, 0x7FFE, 0xEFFE}) {
            short s = (short) i;

            setup(src, s, (short) 1);
            execute(opcode, prefix);
            assertEquals(false, sut.isH());

            setup(src, (short) (s + 1), (short) 1);
            execute(opcode, prefix);
            assertEquals(true, sut.isH());

            setup(src, (short) (s + 2), (short) 1);
            execute(opcode, prefix);
            assertEquals(false, sut.isH());
        }
    }

    @ParameterizedTest
    @MethodSource("ADC_HL_rr_Source")
    public void ADC_HL_rr_sets_CF_appropriately(String src, byte opcode) {
        setup(src, (short) 0xFFFE, (short) 1);
        execute(opcode, prefix);
        assertEquals(false, sut.isC());

        setup(src, (short) 0xFFFF, (short) 1);
        execute(opcode, prefix);
        assertEquals(true, sut.isC());

        setup(src, (short) 0, (short) 1);
        execute(opcode, prefix);
        assertEquals(false, sut.isC());
    }

    @ParameterizedTest
    @MethodSource("ADC_HL_rr_Source")
    public void ADC_HL_rr_sets_PF_appropriately(String src, byte opcode) {
        // http://stackoverflow.com/a/8037485/4574

        testPF(src, opcode, 127, 0, false);
        testPF(src, opcode, 127, 1, true);
        testPF(src, opcode, 127, 127, true);
        testPF(src, opcode, 127, 128, false);
        testPF(src, opcode, 127, 129, false);
        testPF(src, opcode, 127, 255, false);
        testPF(src, opcode, 128, 0, false);
        testPF(src, opcode, 128, 1, false);
        testPF(src, opcode, 128, 127, false);
        testPF(src, opcode, 128, 128, true);
        testPF(src, opcode, 128, 129, true);
        testPF(src, opcode, 128, 255, true);
        testPF(src, opcode, 129, 0, false);
        testPF(src, opcode, 129, 1, false);
        testPF(src, opcode, 129, 127, false);
        testPF(src, opcode, 129, 128, true);
        testPF(src, opcode, 129, 129, true);
        testPF(src, opcode, 129, 255, false);
    }

    void testPF(String src, byte opcode, int oldValue, int subtractedValue, boolean expectedPF) {
        setup(src, createShort((byte) 0, (byte) oldValue), createShort((byte) 0, (byte) subtractedValue));

        execute(opcode, prefix);
        assertEquals(expectedPF, sut.isP());
    }

    @ParameterizedTest
    @MethodSource("ADC_HL_rr_Source")
    public void ADC_HL_rr_resets_NF(String src, byte opcode) {
        assertResetsFlags(opcode, prefix, "N");
    }

    @Disabled("not implemented")
    @ParameterizedTest
    @MethodSource("ADC_HL_rr_Source")
    public void ADC_HL_rr_sets_bits_3_and_5_from_high_byte_of_result(String src, byte opcode) {
        sut.setHL(createShort((byte) 0, withBit(withBit(((byte) 0), 3, true), 5, false)));
        setReg(src, (short) 0);
        execute(opcode, prefix);
        assertEquals(true, sut.is3());
        assertEquals(false, sut.is5());

        sut.setHL(createShort((byte) 0, withBit(withBit(((byte) 0), 3, false), 5, true)));
        execute(opcode, prefix);
        assertEquals(false, sut.is3());
        assertEquals(true, sut.is5());
    }

    @ParameterizedTest
    @MethodSource("ADC_HL_rr_Source")
    public void ADC_HL_rr_returns_proper_T_states(String src, byte opcode) {
        int states = execute(opcode, prefix);
        assertEquals(15, states);
    }
}
