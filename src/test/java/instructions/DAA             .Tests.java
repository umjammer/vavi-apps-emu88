package instructions;

import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class DAA_tests extends InstructionsExecutionTestsBase {

    private static final byte DAA_opcode = 0x27;

    static Stream<Arguments> DAA_cases_Source() {
        return Stream.of(
                //        N      C      H      A     added C after
                arguments(false, false, false, 0x09, 0x00, false),
                arguments(false, false, false, 0x90, 0x00, false),

                arguments(false, false, false, 0x0F, 0x06, false),
                arguments(false, false, false, 0x8A, 0x06, false),

                arguments(false, false, true, 0x03, 0x06, false),
                arguments(false, false, true, 0x90, 0x06, false),

                arguments(false, false, false, 0xA9, 0x60, true),
                arguments(false, false, false, 0xF0, 0x60, true),

                arguments(false, false, false, 0x9F, 0x66, true),
                arguments(false, false, false, 0xFA, 0x66, true),

                arguments(false, false, true, 0xA3, 0x66, true),
                arguments(false, false, true, 0xF0, 0x66, true),

                arguments(false, true, false, 0x09, 0x60, true),
                arguments(false, true, false, 0x20, 0x60, true),

                arguments(false, true, false, 0x0F, 0x66, true),
                arguments(false, true, false, 0x2A, 0x66, true),

                arguments(false, true, true, 0x03, 0x66, true),
                arguments(false, true, true, 0x30, 0x66, true),

                arguments(true, false, false, 0x09, 0x00, false),
                arguments(true, false, false, 0x90, 0x00, false),

                arguments(true, false, true, 0x0F, 0xFA, false),
                arguments(true, false, true, 0x86, 0xFA, false),

                arguments(true, true, false, 0x79, 0xA0, true),
                arguments(true, true, false, 0xF0, 0xA0, true),

                arguments(true, true, true, 0x6F, 0x9A, true),
                arguments(true, true, true, 0x76, 0x9A, true)
        );
    }

    @ParameterizedTest
    @MethodSource("DAA_cases_Source")
    public void DAA_generates_A_and_CF_correctly_based_on_input(boolean inputNF, boolean inputCF, boolean inputHF, int inputA, int addedValue, boolean outputC) {
        setup(inputNF, inputCF, inputHF, inputA);

        execute(DAA_opcode, null);

        assertEquals(add((byte) inputA, addedValue) & 0xff, sut.getA());
        assertEquals(outputC, sut.isC());
    }

    @ParameterizedTest
    @MethodSource("DAA_cases_Source")
    public void DAA_returns_proper_T_states(boolean inputNF, boolean inputCF, boolean inputHF, int inputA, int addedValue, boolean outputC) {
        setup(inputNF, inputCF, inputHF, inputA);

        int states = execute(DAA_opcode, null);
        assertEquals(4, states);
    }

    @Test
    public void DAA_covers_all_possible_combinations_of_flags_and_A() {
        for (int flagN = 0; flagN <= 1; flagN++)
            for (int flagC = 0; flagC <= 1; flagC++)
                for (int flagH = 0; flagH <= 1; flagH++)
                    for (int valueOfA = 0; valueOfA <= 255; valueOfA++) {
                        setup(flagN != 0, flagC != 0, flagH != 0, valueOfA);
                        execute(DAA_opcode, null);
                    }
    }

    @ParameterizedTest
    @MethodSource("DAA_cases_Source")
    public void DAA_generates_PF_properly(boolean inputNF, boolean inputCF, boolean inputHF, int inputA, int addedValue, boolean outputC) {
        setup(inputNF, inputCF, inputHF, inputA);

        execute(DAA_opcode, null);

        assertEquals(parity[sut.getA() & 0xff] != 0, sut.isP());
    }

    @ParameterizedTest
    @MethodSource("DAA_cases_Source")
    public void DAA_generates_SF_properly(boolean inputNF, boolean inputCF, boolean inputHF, int inputA, int addedValue, boolean outputC) {
        setup(inputNF, inputCF, inputHF, inputA);

        execute(DAA_opcode, null);

        assertEquals(getBit((byte) (sut.getA() & 0xff), 7), sut.isS());
    }

    @ParameterizedTest
    @MethodSource("DAA_cases_Source")
    public void DAA_generates_ZF_properly(boolean inputNF, boolean inputCF, boolean inputHF, int inputA, int addedValue, boolean outputC) {
        setup(inputNF, inputCF, inputHF, inputA);

        execute(DAA_opcode, null);

        assertEquals(sut.getA() == 0, sut.isZ());
    }

    @ParameterizedTest
    @MethodSource("DAA_cases_Source")
    public void DAA_does_not_modify_NF(boolean inputNF, boolean inputCF, boolean inputHF, int inputA, int addedValue, boolean outputC) {
        setup(inputNF, inputCF, inputHF, inputA);

        assertDoesNotChangeFlags(DAA_opcode, null, "N");
    }

    @Disabled("not implemented")
    @ParameterizedTest
    @MethodSource("DAA_cases_Source")
    public void DAA_sets_bits_3_and_5_from_of_result(boolean inputNF, boolean inputCF, boolean inputHF, int inputA, int addedValue, boolean outputC) {
        setup(inputNF, inputCF, inputHF, inputA);

        execute(DAA_opcode, null);

        assertEquals(sut.is3(), getBit((byte) (sut.getA() & 0xff), 3));
        assertEquals(sut.is5(), getBit((byte) (sut.getA() & 0xff), 5));
    }

    private void setup(boolean inputNF, boolean inputCF, boolean inputHF, int inputA) {
        sut.setN(inputNF);
        sut.setC(inputCF);
        sut.setH(inputHF);
        sut.setA(inputA & 0xff);
    }
}
