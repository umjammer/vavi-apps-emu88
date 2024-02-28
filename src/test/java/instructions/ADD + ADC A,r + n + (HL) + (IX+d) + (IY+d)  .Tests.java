package instructions;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class ADDC_A_r_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> ADDC_A_A_Source() {
        return Stream.of(
                arguments("A", (byte) 0x87, false, null),
                arguments("A", (byte) 0x8F, false, null),
                arguments("A", (byte) 0x8F, true, null)
        );
    }

    static Stream<Arguments> ADDC_A_r_Source() {
        List<Arguments> combinations = new ArrayList<>();

        // TODO ixy,ihl are not implemented
        String[] registers = new String[] {"B", "C", "D", "E", "H", "L", "(HL)", "n"/*, "IXH", "IXL", "IYH", "IYL"*/, "(IX+n)", "(IY+n)"};
        for (int src = 0; src < registers.length; src++) {
            String reg = registers[src];
            int[] i = new int[] {src};
            Byte[] prefix = new Byte[1];

            modifyTestCaseCreationForIndexRegs(reg, /* ref */i, /* out */prefix);

            byte ADD_opcode = (byte) (i[0] == 7 ? 0xC6 : (i[0] | 0x80));
            byte ADC_opcode = (byte) (i[0] == 7 ? 0xCE : (i[0] | 0x88));
            combinations.add(arguments(reg, ADD_opcode, false, prefix[0]));
            combinations.add(arguments(reg, ADC_opcode, false, prefix[0]));
            combinations.add(arguments(reg, ADC_opcode, true, prefix[0]));
        }

        return combinations.stream();
    }

    @ParameterizedTest
    @MethodSource({"ADDC_A_r_Source", "ADDC_A_A_Source"})
    public void ADDC_A_r_adds_both_registers_with_or_without_carry(String src, byte opcode, boolean cf, Byte prefix) {
        byte oldValue = fixture.create(Byte.TYPE);
        byte valueToAdd = src.equals("A") ? oldValue : fixture.create(Byte.TYPE);

        setup(src, oldValue, valueToAdd, cf);
        execute(opcode, prefix);

        assertEquals(add(oldValue, valueToAdd + (cf ? 1 : 0)) & 0xff, sut.getA());
    }

    private void setup(String src, byte oldValue, byte valueToAdd, boolean cf) {
        sut.setA(oldValue & 0xff);
        sut.setC(cf);

        if (src.equals("n")) {
            setMemoryContentsAt((short) 1, valueToAdd);
        } else if (src.equals("(HL)")) {
            short address = fixture.create(Short.TYPE);
            sut.getBus().pokeb(address & 0xffff, valueToAdd & 0xff);
            sut.setHL(address);
        } else if (src.startsWith("(I")) {
            short address = fixture.create(Short.TYPE);
            byte offset = fixture.create(Byte.TYPE);
            short realAddress = add(address, offset);
            sut.getBus().pokeb(realAddress & 0xffff, valueToAdd & 0xff);
            setMemoryContentsAt((short) 2, offset);
            setReg(src.substring(1, 1 + 2), address);
        } else if (!src.equals("A")) {
            setReg(src, valueToAdd);
        }
    }

    @ParameterizedTest
    @MethodSource("ADDC_A_r_Source")
    public void ADDC_A_r_sets_SF_appropriately(String src, byte opcode, boolean cf, Byte prefix) {
        setup(src, (byte) 0xFD, (byte) 1, false);

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
    @MethodSource("ADDC_A_r_Source")
    public void ADDC_A_r_sets_ZF_appropriately(String src, byte opcode, boolean cf, Byte prefix) {
        setup(src, (byte) 0xFD, (byte) 1, false);

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
    @MethodSource("ADDC_A_r_Source")
    public void ADDC_A_r_sets_HF_appropriately(String src, byte opcode, boolean cf, Byte prefix) {
        for (byte b : new byte[] {0x0E, 0x7E, (byte) 0xFE}) {
            setup(src, b, (byte) 1, false);

            execute(opcode, prefix);
            assertEquals(false, sut.isH());

            execute(opcode, prefix);
            assertEquals(true, sut.isH());

            execute(opcode, prefix);
            assertEquals(false, sut.isH());
        }
    }

    @ParameterizedTest
    @MethodSource("ADDC_A_r_Source")
    public void ADDC_A_r_sets_PF_appropriately(String src, byte opcode, boolean cf, Byte prefix) {
        setup(src, (byte) 0x7E, (byte) 1, false);

        execute(opcode, prefix);
        assertEquals(false, sut.isP());

        execute(opcode, prefix);
        assertEquals(true, sut.isP());

        execute(opcode, prefix);
        assertEquals(false, sut.isP());
    }

    @ParameterizedTest
    @MethodSource({"ADDC_A_r_Source", "ADDC_A_A_Source"})
    public void ADDC_A_r_resets_NF(String src, byte opcode, boolean cf, Byte prefix) {
        assertResetsFlags(opcode, null, "N");
    }

    @ParameterizedTest
    @MethodSource("ADDC_A_r_Source")
    public void ADDC_A_r_sets_CF_appropriately(String src, byte opcode, boolean cf, Byte prefix) {
        setup(src, (byte) 0xFE, (byte) 1, false);

        execute(opcode, prefix);
        assertEquals(false, sut.isC());

        execute(opcode, prefix);
        assertEquals(true, sut.isC());

        execute(opcode, prefix);
        assertEquals(false, sut.isC());
    }

    @Disabled("not implemented")
    @ParameterizedTest
    @MethodSource("ADDC_A_r_Source")
    public void ADDC_A_r_sets_bits_3_and_5_from_result(String src, byte opcode, boolean cf, Byte prefix) {
        setup(src, withBit(withBit((byte) 0, 3, true), 5, false), (byte) 0, false);
        execute(opcode, prefix);
        assertEquals(true, sut.is3());
        assertEquals(false, sut.is5());

        setup(src, withBit(withBit((byte) 0, 3, false), 5, true), (byte) 0, true);
        execute(opcode, prefix);
        assertEquals(false, sut.is3());
        assertEquals(true, sut.is5());
    }

    @ParameterizedTest
    @MethodSource({"ADDC_A_r_Source", "ADDC_A_A_Source"})
    public void ADDC_A_r_returns_proper_T_states(String src, byte opcode, boolean cf, Byte prefix) {
        int states = execute(opcode, prefix);
        assertEquals(
                (src.equals("(HL)") || src.equals("n")) ? 7 :
                        src.startsWith("I") ? 8 :
                                src.startsWith(("(I")) ? 19 :
                                        4, states);
    }
}
