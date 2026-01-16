package instructions;

import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static vavi.apps.em88.Z80.dec16bitInternal;


class LDI_tests extends InstructionsExecutionTestsBase {

    private static final byte prefix = (byte) 0xED;

    static Stream<Arguments> LDI_Source() {
        return Stream.of(arguments("LDI", (byte) 0xA0));
    }

    static Stream<Arguments> LDD_Source() {
        return Stream.of(arguments("LDD", (byte) 0xA8));
    }

    static Stream<Arguments> LDIR_Source() {
        return Stream.of(arguments("LDI", (byte) 0xB0));
    }

    static Stream<Arguments> LDDR_Source() {
        return Stream.of(arguments("LDD", (byte) 0xB8));
    }

    @ParameterizedTest
    @MethodSource({"LDI_Source", "LDD_Source", "LDIR_Source", "LDDR_Source"})
    public void LDI_LDD_LDIR_LDDR_copy_value_correctly(String instr, byte opcode) {
        byte oldValue = fixture.create(Byte.TYPE);
        byte value = fixture.create(Byte.TYPE);
        // TODO got error when 1 at (LDI|LDD)
        short srcAddress = createAddressFixture();
        short destAddress = fixture.create(Short.TYPE);

        sut.setHL(srcAddress & 0xffff);
        sut.setDE(destAddress & 0xffff);

        sut.getBus().pokeb(srcAddress & 0xffff, value & 0xff);
        sut.getBus().pokeb(destAddress & 0xffff, oldValue & 0xff);

        execute(opcode, prefix);

        int newValue = sut.getBus().peekb(destAddress & 0xffff);
        assertEquals(value & 0xff, newValue);
    }

    @ParameterizedTest
    @MethodSource({"LDI_Source", "LDIR_Source"})
    public void LDI_LDIR_increase_DE_and_HL(String instr, byte opcode) {
        short srcAddress = fixture.create(Short.TYPE);
        short destAddress = fixture.create(Short.TYPE);

        sut.setHL(srcAddress & 0xffff);
        sut.setDE(destAddress & 0xffff);

        execute(opcode, prefix);

        assertEquals(inc(srcAddress) & 0xffff, sut.getHL());
        assertEquals(inc(destAddress) & 0xffff, sut.getDE());
    }

    @ParameterizedTest
    @MethodSource({"LDD_Source", "LDDR_Source"})
    public void LDD_LDDR_decreases_DE_and_HL(String instr, byte opcode) {
        short srcAddress = fixture.create(Short.TYPE);
        short destAddress = fixture.create(Short.TYPE);

        sut.setHL(srcAddress & 0xffff);
        sut.setDE(destAddress & 0xffff);

        execute(opcode, prefix);

        assertEquals(dec(srcAddress) & 0xffff, sut.getHL());
        assertEquals(dec(destAddress) & 0xffff, sut.getDE());
    }

    @ParameterizedTest
    @MethodSource({"LDI_Source", "LDD_Source", "LDIR_Source", "LDDR_Source"})
    public void LDI_LDD_LDIR_LDDR_decrease_BC(String instr, byte opcode) {
        short counter = fixture.create(Short.TYPE);
        sut.setBC(counter & 0xffff);

        execute(opcode, prefix);

        assertEquals(dec(counter) & 0xffff, sut.getBC());
    }

    @ParameterizedTest
    @MethodSource({"LDI_Source", "LDD_Source", "LDIR_Source", "LDDR_Source"})
    public void LDI_LDD_LDIR_LDDR_do_not_change_S_Z_C(String instr, byte opcode) {
        assertDoesNotChangeFlags(opcode, prefix, "S", "Z", "C");
    }

    @ParameterizedTest
    @MethodSource({"LDI_Source", "LDD_Source", "LDIR_Source", "LDDR_Source"})
    public void LDI_LDD_LDIR_LDDR_reset_H_N(String instr, byte opcode) {
        assertResetsFlags(opcode, prefix, "H", "N");
    }

    @ParameterizedTest
    @MethodSource({"LDI_Source", "LDD_Source", "LDIR_Source", "LDDR_Source"})
    public void LDI_LDD_LDIR_LDDR_resets_PF_if_BC_reaches_zero(String instr, byte opcode) {
        sut.setBC((short) 128);
        for (int i = 0; i <= 256; i++) {
            int oldBC = sut.getBC();
            execute(opcode, prefix);
            assertEquals(dec16bitInternal(oldBC), sut.getBC());
            assertEquals(sut.getBC() != 0, sut.isP());
        }
    }

    @Disabled("not implemented")
    @ParameterizedTest
    @MethodSource({"LDI_Source", "LDD_Source", "LDIR_Source", "LDDR_Source"})
    public void LDI_LDD_LDIR_LDDR_set_Flag3_from_bit_3_of_value_plus_A_and_Flag5_from_bit_1(String instr, byte opcode) {
        byte value = fixture.create(Byte.TYPE);
        short srcAddress = fixture.create(Short.TYPE);

        for (int i = 0; i < 256; i++) {
            byte valueOfA = (byte) i;
            sut.setA(valueOfA & 0xff);
            sut.setHL(srcAddress & 0xffff);
            sut.getBus().pokeb(srcAddress & 0xffff, value & 0xff);

            execute(opcode, prefix);

            byte valuePlusA = getLowByte(add(value, valueOfA));
            assertEquals((valuePlusA & 0x80) != 0, sut.is3());
            assertEquals((valuePlusA & 0x01) != 0, sut.is5());
        }
    }

    @ParameterizedTest
    @MethodSource({"LDIR_Source", "LDDR_Source"})
    public void LDIR_LDDR_decrease_PC_by_two_if_counter_does_not_reach_zero(String instr, byte opcode) {
        sut.setBC(128);
        for (int i = 0; i <= 256; i++) {
            short address = fixture.create(Short.TYPE);
            int oldPC = sut.getPC();
            int oldBC = sut.getBC();
            executeAt(address, opcode, prefix);
            assertEquals(dec16bitInternal(oldBC), sut.getBC());
            assertEquals((sut.getBC() == 0 ? add(address, 2) : address) & 0xffff, sut.getPC());
        }
    }

    @ParameterizedTest
    @MethodSource({"LDI_Source", "LDD_Source"})
    public void LDI_LDD_return_proper_T_states(String instr, byte opcode) {
        int states = execute(opcode, prefix);
        assertEquals(16, states);
    }

    @ParameterizedTest
    @MethodSource({"LDIR_Source", "LDDR_Source"})
    public void LDIR_LDDR_return_proper_T_states_depending_of_value_of_BC(String instr, byte opcode) {
        sut.setBC(128);
        for (int i = 0; i <= 256; i++) {
            int oldBC = sut.getBC();
            int states = execute(opcode, prefix);
            assertEquals(dec16bitInternal(oldBC), sut.getBC());
            assertEquals(sut.getBC() == 0 ? 16 : 21, states);
        }
    }
}