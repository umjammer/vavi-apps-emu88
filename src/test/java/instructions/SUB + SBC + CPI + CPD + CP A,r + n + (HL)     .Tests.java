package instructions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vavi.util.Debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static vavi.apps.em88.Z80.dec16bitInternal;


class SUB_SBC_CPI_CPD_CP_r_tests extends InstructionsExecutionTestsBase {

    private static final byte cpidrPrefix = (byte) 0xED;
    private static final byte[] cpidrOpcodes = new byte[] {(byte) 0xA1, (byte) 0xA9, (byte) 0xB1, (byte) 0xFF};

    static Stream<Arguments> SUB_SBC_A_A_Source() {
        return Stream.of(
                arguments("A", (byte) 0x97, false, null),
                arguments("A", (byte) 0x9F, false, null),
                arguments("A", (byte) 0x9F, true, null)
        );
    }

    static Stream<Arguments> CP_A_Source() {
        return Stream.of(
                arguments("A", (byte) 0xBF, false, null)
        );
    }

    static Stream<Arguments> CPI_Source() {
        return Stream.of(
                arguments("CPI", (byte) 0xA1, false, null)
        );
    }

    static Stream<Arguments> CPD_Source() {
        return Stream.of(
                arguments("CPD", (byte) 0xA9, false, null)
        );
    }

    static Stream<Arguments> CPIR_Source() {
        return Stream.of(
                arguments("CPIR", (byte) 0xB1, false, null)
        );
    }

    static Stream<Arguments> CPDR_Source() {
        return Stream.of(
                arguments("CPDR", (byte) 0xFF, true, null) // can't use B9 because it's "CP C" without prefix
        );
    }

    public static List<Arguments> SUB_SBC_A_r;
    public static List<Arguments> CP_r;
    public static List<Arguments> CPID_R;

    static Stream<Arguments> SUB_SBC_A_r_Source() {
        return SUB_SBC_A_r.stream();
    }
    static Stream<Arguments> CP_r_Source() {
        return CP_r.stream();
    }
    static Stream<Arguments> CPID_R_Source() {
        return CPID_R.stream();
    }

    static {
        List<Arguments> combinations = new ArrayList<>();
        List<Arguments> CP_combinations = new ArrayList<>();

        // TODO ixy,ihl are not implemented
        String[] registers = new String[] {"B", "C", "D", "E", "H", "L", "(HL)", "n"/*, "IXH", "IXL", "IYH", "IYL"*/, "(IX+n)", "(IY+n)"};
        for (int src = 0; src < registers.length; src++) {
            String reg = registers[src];
            int[] i = new int[] {src};
            Byte[] prefix = new Byte[1];

            modifyTestCaseCreationForIndexRegs(reg, /* ref */i, /* out */prefix);

            byte SUB_opcode = (byte) (i[0] == 7 ? 0xD6 : (i[0] | 0x90));
            byte SBC_opcode = (byte) (i[0] == 7 ? 0xDE : (i[0] | 0x98));
            byte CP_opcode = (byte) (i[0] == 7 ? 0xFE : (i[0] | 0xB8));
            combinations.add(arguments(reg, SUB_opcode, false, prefix[0]));
            combinations.add(arguments(reg, SBC_opcode, false, prefix[0]));
            combinations.add(arguments(reg, SBC_opcode, true, prefix[0]));
            CP_combinations.add(arguments(reg, CP_opcode, false, prefix[0]));
        }

        SUB_SBC_A_r = combinations;
        CP_r = CP_combinations;

        CPID_R = Stream.of(CPI_Source(), CPD_Source(), CPIR_Source(), CPDR_Source())
                .flatMap(Function.identity()).collect(Collectors.toList());
    }

    @ParameterizedTest
    @MethodSource({"SUB_SBC_A_r_Source", "SUB_SBC_A_A_Source"})
    public void SUB_SBC_A_r_subtracts_both_registers_with_or_without_carry(String src, byte opcode, boolean cf, Byte prefix) {
        byte oldValue = fixture.create(Byte.TYPE);
        byte valueToSubtract = src.equals("A") ? oldValue : fixture.create(Byte.TYPE);

        setup(src, oldValue, valueToSubtract, cf);
        execute(opcode, prefix);

        assertEquals(sub(oldValue, (valueToSubtract & 0xff) + (cf ? 1 : 0)) & 0xff, sut.getA());
    }

    @ParameterizedTest
    @MethodSource({"CP_r_Source", "CP_A_Source", "CPID_R_Source"})
    public void CPs_do_not_change_A(String src, byte opcode, boolean cf, Byte prefix) {
        byte oldValue = fixture.create(Byte.TYPE);
        byte argument = fixture.create(Byte.TYPE);

        setup(src, oldValue, argument, cf);
        execute(opcode, prefix);

        assertEquals(oldValue & 0xff, sut.getA());
    }

    private void setup(String src, byte oldValue, byte valueToSubtract, boolean cf/* = 0*/) {
        sut.setA(oldValue & 0xff);
        sut.setC(cf);

        if (src.equals("n")) {
            setMemoryContentsAt((short) 1, valueToSubtract);
        } else if (src.equals("(HL)") || src.startsWith("CP")) {
            // TODO got error when 1 at "set_HF_appropriately" (CPDR)
            short address = createAddressFixture();
            sut.getBus().pokeb(address & 0xffff, valueToSubtract & 0xff);
            sut.setHL(address & 0xffff);
        } else if (src.startsWith("(I")) {
            // TODO got error when 1 at "*" (CPI)
            short address = createAddressFixture();
            byte offset = fixture.create(Byte.TYPE);
            short realAddress = add(address, offset);
            sut.getBus().pokeb(realAddress & 0xffff, valueToSubtract & 0xff);
            setMemoryContentsAt((short) 2, offset);
            setReg(src.substring(1, 1 + 2), address);
        } else if (!src.equals("A")) {
            setReg(src, valueToSubtract);
        }
    }

    @ParameterizedTest
    @MethodSource({"SUB_SBC_A_r_Source", "CP_r_Source", "CPID_R_Source"})
    public void SUB_SBC_CPs_set_SF_appropriately(String src, byte opcode, boolean cf, Byte prefix) {
        setup(src, (byte) 0x02, (byte) 1, false);
        execute(opcode, prefix);
        assertEquals(false, sut.isS());

        setup(src, (byte) 0x01, (byte) 1, false);
        execute(opcode, prefix);
        assertEquals(false, sut.isS());

        setup(src, (byte) 0x00, (byte) 1, false);
        execute(opcode, prefix);
        assertEquals(true, sut.isS());

        setup(src, (byte) 0xFF, (byte) 1, false);
        execute(opcode, prefix);
        assertEquals(true, sut.isS());
    }

    @ParameterizedTest
    @MethodSource({"SUB_SBC_A_r_Source", "CP_r_Source", "CPID_R_Source"})
    public void SUB_SBC_CPs_set_ZF_appropriately(String src, byte opcode, boolean cf, Byte prefix) {
        setup(src, (byte) 0x03, (byte) 1, false);
        execute(opcode, prefix);
        assertEquals(false, sut.isZ());

        setup(src, (byte) 0x02, (byte) 1, false);
        execute(opcode, prefix);
        assertEquals(false, sut.isZ());

        setup(src, (byte) 0x01, (byte) 1, false);
        execute(opcode, prefix);
        assertEquals(true, sut.isZ());

        setup(src, (byte) 0x00, (byte) 1, false);
        execute(opcode, prefix);
        assertEquals(false, sut.isZ());
    }

    @ParameterizedTest
    @MethodSource({"SUB_SBC_A_r_Source", "CP_r_Source", "CPID_R_Source"})
    public void SUB_SBC_CPs_set_HF_appropriately(String src, byte opcode, boolean cf, Byte prefix) {
        for (byte b : new byte[] {0x11, (byte) 0x81, (byte) 0xF1}) {
            setup(src, b, (byte) 1, false);
            execute(opcode, prefix);
            assertEquals(false, sut.isH());

            setup(src, (byte) (b - 1), (byte) 1, false);
            execute(opcode, prefix);
            assertEquals(true, sut.isH());

            setup(src, (byte) (b - 2), (byte) 1, false);
            execute(opcode, prefix);
            assertEquals(false, sut.isH());
        }
    }

    @ParameterizedTest
    @MethodSource({"SUB_SBC_A_r_Source", "CP_r_Source"})
    public void SUB_SBC_CP_r_sets_PF_appropriately(String src, byte opcode, boolean cf, Byte prefix) {
        //http://stackoverflow.com/a/8037485/4574

        testPF(src, opcode, 127, 0, false, prefix);
        testPF(src, opcode, 127, 1, false, prefix);
        testPF(src, opcode, 127, 127, false, prefix);
        testPF(src, opcode, 127, 128, true, prefix);
        testPF(src, opcode, 127, 129, true, prefix);
        testPF(src, opcode, 127, 255, true, prefix);
        testPF(src, opcode, 128, 0, false, prefix);
        testPF(src, opcode, 128, 1, true, prefix);
        testPF(src, opcode, 128, 127, true, prefix);
        testPF(src, opcode, 128, 128, false, prefix);
        testPF(src, opcode, 128, 129, false, prefix);
        testPF(src, opcode, 128, 255, false, prefix);
        testPF(src, opcode, 129, 0, false, prefix);
        testPF(src, opcode, 129, 1, false, prefix);
        testPF(src, opcode, 129, 127, true, prefix);
        testPF(src, opcode, 129, 128, false, prefix);
        testPF(src, opcode, 129, 129, false, prefix);
        testPF(src, opcode, 129, 255, false, prefix);
    }

    void testPF(String src, byte opcode, int oldValue, int subtractedValue, boolean expectedPF, Byte prefix) {
        setup(src, (byte) oldValue, (byte) subtractedValue, false);

        execute(opcode, prefix);
        assertEquals(expectedPF, sut.isP());
    }

    @ParameterizedTest
    @MethodSource({"SUB_SBC_A_r_Source", "SUB_SBC_A_A_Source", "CP_r_Source", "CPID_R_Source"})
    public void SUB_SBC_CPs_sets_NF(String src, byte opcode, boolean cf, Byte prefix) {
        assertSetsFlags(opcode, null, "N");
    }

    @ParameterizedTest
    @MethodSource({"SUB_SBC_A_r_Source", "CP_r_Source"})
    public void SUB_SBC_CP_r_sets_CF_appropriately(String src, byte opcode, boolean cf, Byte prefix) {
        setup(src, (byte) 0x01, (byte) 1, false);
        execute(opcode, prefix);
        assertEquals(false, sut.isC());

        setup(src, (byte) 0x00, (byte) 1, false);
        execute(opcode, prefix);
        assertEquals(true, sut.isC());

        setup(src, (byte) 0xFF, (byte) 1, false);
        execute(opcode, prefix);
        assertEquals(false, sut.isC());
    }

    @Disabled("not implemented")
    @ParameterizedTest
    @MethodSource("SUB_SBC_A_r_Source")
    public void SUB_SBC_r_sets_bits_3_and_5_from_result(String src, byte opcode, boolean cf, Byte prefix) {
        setup(src, (byte) (withBit(withBit((byte) 0, 3, true), 5, false) + 1), (byte) 1, false);
        execute(opcode, prefix);
        assertEquals(true, sut.is3());
        assertEquals(false, sut.is5());

        setup(src, (byte) (withBit(withBit((byte) 0, 3, false), 5, true) + 1), (byte) 1, false);
        execute(opcode, prefix);
        assertEquals(false, sut.is3());
        assertEquals(true, sut.is5());
    }

    @ParameterizedTest
    @MethodSource({"SUB_SBC_A_r_Source", "SUB_SBC_A_A_Source", "CP_r_Source"})
    public void SUB_SBC_CP_r_returns_proper_T_states(String src, byte opcode, boolean cf, Byte prefix) {
        int states = execute(opcode, prefix);
        assertEquals(
                (src.equals("(HL)") || src.equals("n")) ? 7 :
                        src.startsWith("I") ? 8 :
                                src.startsWith(("(I")) ? 19 :
                                        4, states);
    }

    @ParameterizedTest
    @MethodSource("CPID_R_Source")
    public void CPI_CPD_CPIR_CPRD_do_not_change_C(String src, byte opcode, boolean cf, Byte prefix) {
        assertDoesNotChangeFlags(opcode, cpidrPrefix, "C");
    }

    @ParameterizedTest
    @MethodSource("CPID_R_Source")
    public void CPI_CPD_CPIR_CPDR_reset_PF_if_BC_reaches_zero(String src, byte opcode, boolean cf, Byte prefix) {
        sut.setBC(128);
        for (int i = 0; i <= 256; i++) {
            int oldBC = sut.getBC();
            execute(opcode, prefix);
            assertEquals(dec16bitInternal(oldBC), sut.getBC());
            assertEquals(sut.getBC() != 0, sut.isP());
        }
    }

    @ParameterizedTest
    @MethodSource({"CPI_Source", "CPIR_Source"})
    public void CPI_CPIR_increase_HL(String src, byte opcode, boolean cf, Byte prefix) {
        short address = fixture.create(Short.TYPE);

        sut.setHL(address & 0xffff);

        execute(opcode, prefix);

        assertEquals(inc(address) & 0xffff, sut.getHL());
    }

    @ParameterizedTest
    @MethodSource({"CPD_Source", "CPDR_Source"})
    public void CPD_CPDR_decrease_HL(String src, byte opcode, boolean cf, Byte prefix) {
        short address = fixture.create(Short.TYPE);

        sut.setHL(address & 0xffff);

        execute(opcode, prefix);

        assertEquals(dec(address) & 0xffff, sut.getHL());
    }

    @ParameterizedTest
    @MethodSource("CPID_R_Source")
    public void CPI_CPD_CPIR_CPDR_decrease_BC(String src, byte opcode, boolean cf, Byte prefix) {
        short count = fixture.create(Short.TYPE);

        sut.setBC(count & 0xffff);

        execute(opcode, prefix);

        assertEquals(dec(count) & 0xffff, sut.getBC());
    }

    @ParameterizedTest
    @MethodSource({"CPI_Source", "CPD_Source"})
    public void CPI_CPD_return_proper_T_states(String src, byte opcode, boolean cf, Byte prefix) {
        int states = execute(opcode, prefix);
        assertEquals(16, states);
    }

    @Disabled("not implemented")
    @ParameterizedTest
    @MethodSource("CPID_R_Source")
    public void CPI_CPD_set_Flag3_from_bit_3_of_A_minus_aHL_minus_HF_and_Flag5_from_bit_1(String src, byte opcode, boolean cf, Byte prefix) {
        byte valueInMemory = fixture.create(Byte.TYPE);
        // TODO got error when 1
        short srcAddress = createAddressFixture();

        for (int i = 0; i < 256; i++) {
            byte valueOfA = (byte) i;
            sut.setA(valueOfA & 0xff);
            sut.setHL(srcAddress & 0xffff);
            sut.getBus().pokeb(srcAddress & 0xffff, valueInMemory & 0xff);

            execute(opcode, prefix);

            byte expected = getLowByte(sub(sub(valueOfA, valueInMemory & 0xff), sut.isH() ? 1 : 0));
            assertEquals(getBit(expected, 3), sut.is3());
            assertEquals(getBit(expected, 1), sut.is5());
        }
    }

    @ParameterizedTest
    @MethodSource({"CPIR_Source", "CPDR_Source"})
    public void CPIR_CPDR_decrease_PC_if_Z_is_1_but_BC_is_not_0(String src, byte opcode, boolean cf, Byte prefix) {
        // TODO got error
        // when 88, 50, 85 at (HL)
        // when 6, 125, 35 at (HL)
        short dataAddress = fixture.create(Short.TYPE);
        short runAddress = fixture.create(Short.TYPE);
        byte data = fixture.create(Byte.TYPE);

Debug.printf("%d, %d, %d", dataAddress, runAddress, data);

        sut.getBus().pokeb(dataAddress & 0xffff, data & 0xff);
        sut.setA(data & 0xff);
        sut.setHL(dataAddress & 0xffff);
        sut.setBC(fixture.create(Short.TYPE) & 0xffff);

        executeAt(runAddress, opcode, cpidrPrefix);

        assertEquals(add(runAddress, 2) & 0xffff, sut.getPC());
    }

    @ParameterizedTest
    @MethodSource({"CPIR_Source", "CPDR_Source"})
    public void CPIR_CPDR_decrease_PC_if_BC_is_0_but_Z_is_not_0(String src, byte opcode, boolean cf, Byte prefix) {
        if ((opcode & 0xff) == 0xFF) opcode = (byte) 0xB9;

        short dataAddress = fixture.create(Short.TYPE);
        short runAddress = fixture.create(Short.TYPE);
        byte data1 = fixture.create(Byte.TYPE);
        byte data2 = fixture.create(Byte.TYPE);

        sut.getBus().pokeb(dataAddress & 0xffff, data1 & 0xff);
        sut.setA(data2 & 0xff);
        sut.setHL(dataAddress & 0xffff);
        sut.setBC(1);

        executeAt(runAddress, opcode, cpidrPrefix);

        assertEquals(add(runAddress, 2) & 0xffff, sut.getPC());
    }

    @ParameterizedTest
    @MethodSource({"CPIR_Source", "CPDR_Source"})
    public void CPIR_CPDR_do_not_decrease_PC_if_BC_is_not_0_and_Z_is_0(String src, byte opcode, boolean cf, Byte prefix) {
        if ((opcode & 0xff) == 0xFF) opcode = (byte) 0xB9;

        short dataAddress = fixture.create(Short.TYPE);
        short runAddress = fixture.create(Short.TYPE);
        byte data1 = fixture.create(Byte.TYPE);
        byte data2 = fixture.create(Byte.TYPE);

        sut.getBus().pokeb(dataAddress & 0xffff, data1 & 0xff);
        sut.setA(data2 & 0xff);
        sut.setHL(dataAddress & 0xffff);
        sut.setBC(1000);

        executeAt(runAddress, opcode, cpidrPrefix);

        assertEquals(runAddress & 0xffff, sut.getPC());
    }

    @ParameterizedTest
    @MethodSource({"CPIR_Source", "CPDR_Source"})
    public void CPIR_CPDR_return_proper_T_states_depending_on_PC_being_decreased_or_not(String src, byte opcode, boolean cf, Byte prefix) {
        if ((opcode & 0xff) == 0xFF) opcode = (byte) 0xB9;

        short dataAddress = fixture.create(Short.TYPE);
        short runAddress = fixture.create(Short.TYPE);
        byte data1 = fixture.create(Byte.TYPE);
        byte data2 = fixture.create(Byte.TYPE);

        sut.getBus().pokeb(dataAddress & 0xffff, data1 & 0xff);
        sut.setA(data2 & 0xff);
        sut.setHL(dataAddress & 0xffff);
        sut.setBC(2);

        int states = executeAt(runAddress, opcode, cpidrPrefix);
        assertEquals(21, states);

        sut.setHL(dataAddress & 0xffff);
        states = executeAt(runAddress, opcode, cpidrPrefix);
        assertEquals(16, states);
    }

    @Override protected int execute(byte opcode, Byte prefix/*= null*/, byte... nextFetches) {
        if (Arrays.binarySearch(cpidrOpcodes, opcode) >= 0)
            return super.execute((opcode & 0xff) == 0xFF ? (byte) 0xB9 : opcode, cpidrPrefix, nextFetches);
        else
            return super.execute(opcode, prefix, nextFetches);
    }
}
