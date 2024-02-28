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


class AND_r_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> AND_r_Source() {
        List<Arguments> combinations = new ArrayList<>();

        // TODO ixy,ihl are not implemented
        final String[] registers = new String[] {"B", "C", "D", "E", "H", "L", "(HL)", "n"/*, "IXH", "IXL", "IYH", "IYL"*/, "(IX+n)", "(IY+n)"};
        for (int src = 0; src < registers.length; src++) {
            String reg = registers[src];
            int[] i = new int[] {src};
            Byte[] prefix = new Byte[1];

            modifyTestCaseCreationForIndexRegs(reg, /* ref */i, /* out */prefix);

            byte opcode = (byte) (i[0] == 7 ? 0xE6 : (i[0] | 0xA0));
            combinations.add(arguments(reg, opcode, prefix[0]));
        }

        return combinations.stream();
    }

    static Stream<Arguments> AND_A_Source() {
        return Stream.of(
                arguments("A", (byte) 0xA7, null)
        );
    }

    @ParameterizedTest
    @MethodSource("AND_r_Source")
    public void AND_r_ands_both_registers(String src, byte opcode, Byte prefix) {
        byte oldValue = fixture.create(Byte.TYPE);
        byte valueToAnd = fixture.create(Byte.TYPE);

        setup(src, oldValue, valueToAnd);
        execute(opcode, prefix);

        assertEquals(oldValue & valueToAnd, sut.getA());
    }

    @ParameterizedTest
    @MethodSource("AND_A_Source")
    public void AND_A_does_not_change_A(String src, byte opcode, Byte prefix) {
        byte value = fixture.create(Byte.TYPE);

        sut.setA(value);
        execute(opcode, prefix);

        assertEquals(value, sut.getA());
    }

    private void setup(String src, byte oldValue, byte valueToAnd) {
        sut.setA(oldValue);

        if (src.equals("n")) {
            setMemoryContentsAt((short) 1, valueToAnd);
        } else if (src.equals("(HL)")) {
            short address = fixture.create(Short.TYPE);
            sut.getBus().pokeb(address & 0xffff, valueToAnd & 0xff);
            sut.setHL(address);
        } else if (src.startsWith("(I")) {
            short address = fixture.create(Short.TYPE);
            byte offset = fixture.create(Byte.TYPE);
            short realAddress = add(address, offset);
            sut.getBus().pokeb(realAddress & 0xffff, valueToAnd & 0xff);
            setMemoryContentsAt((short) 2, offset);
            setReg(src.substring(1, 1 + 2), address);
        } else if (!src.equals("A")) {
            setReg(src, valueToAnd);
        }
    }

    @ParameterizedTest
    @MethodSource("AND_r_Source")
    public void AND_r_sets_SF_appropriately(String src, byte opcode, Byte prefix) {
        executeCase(src, opcode, 0xFF, 0xFF, prefix);
        assertEquals(true, sut.isS());

        executeCase(src, opcode, 0xFF, 0x80, prefix);
        assertEquals(true, sut.isS());

        executeCase(src, opcode, 0xFF, 0, prefix);
        assertEquals(false, sut.isS());
    }

    private void executeCase(String src, byte opcode, int oldValue, int valueToAnd, Byte prefix) {
        setup(src, (byte) oldValue, (byte) valueToAnd);
        execute(opcode, prefix);
    }

    @ParameterizedTest
    @MethodSource("AND_r_Source")
    public void AND_r_sets_ZF_appropriately(String src, byte opcode, Byte prefix) {
        executeCase(src, opcode, 0xFF, 0xFF, prefix);
        assertEquals(false, sut.isZ());

        executeCase(src, opcode, 0xFF, 0x80, prefix);
        assertEquals(false, sut.isZ());

        executeCase(src, opcode, 0xFF, 0, prefix);
        assertEquals(true, sut.isZ());
    }

    @ParameterizedTest
    @MethodSource("AND_r_Source")
    public void AND_r_sets_HF(String src, byte opcode, Byte prefix) {
        assertSetsFlags(opcode, null, "H");
    }

    @ParameterizedTest
    @MethodSource("AND_r_Source")
    public void AND_r_sets_PF_appropriately(String src, byte opcode, Byte prefix) {
        executeCase(src, opcode, 0xFF, 0x7E, prefix);
        assertEquals(true, sut.isP());

        executeCase(src, opcode, 0xFF, 0x7F, prefix);
        assertEquals(false, sut.isP());

        executeCase(src, opcode, 0xFF, 0x80, prefix);
        assertEquals(false, sut.isP());

        executeCase(src, opcode, 0xFF, 0x81, prefix);
        assertEquals(true, sut.isP());
    }

    @ParameterizedTest
    @MethodSource({"AND_r_Source", "AND_A_Source"})
    public void AND_r_resets_NF_and_CF(String src, byte opcode, Byte prefix) {
        assertResetsFlags(opcode, null, "N", "C");
    }

    @Disabled("not implemented")
    @ParameterizedTest
    @MethodSource("AND_r_Source")
    public void AND_r_sets_bits_3_and_5_from_result(String src, byte opcode, Byte prefix) {
        byte value = withBit(withBit((byte) 0, 3, true), 5, false);
        setup(src, value, value);
        execute(opcode, prefix);
        assertEquals(true, sut.is3());
        assertEquals(false, sut.is5());

        value = withBit(withBit((byte) 0, 3, false), 5, true);
        setup(src, value, value);
        execute(opcode, prefix);
        assertEquals(false, sut.is3());
        assertEquals(true, sut.is5());
    }

    @ParameterizedTest
    @MethodSource({"AND_r_Source", "AND_A_Source"})
    public void AND_r_returns_proper_T_states(String src, byte opcode, Byte prefix) {
        int states = execute(opcode, prefix);
        assertEquals(
                (src.equals("(HL)") || src.equals("n")) ? 7 :
                src.startsWith("I") ? 8 :
                src.startsWith(("(I")) ? 19 :
                4, states);
    }
}
