package instructions;

import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class DEC_aHL_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> DEC_Source() {
        return Stream.of(
                arguments("HL", (byte) 0x35, null),
                arguments("IX", (byte) 0x35, (byte) 0xDD),
                arguments("IY", (byte) 0x35, (byte) 0xFD)
        );
    }

    @ParameterizedTest
    @MethodSource("DEC_Source")
    public void DEC_aHL_IX_IY_plus_n_increases_value_appropriately(String reg, byte opcode, Byte prefix) {
        byte oldValue = fixture.create(Byte.TYPE);
        byte offset = reg.equals("HL") ? (byte) 0 : fixture.create(Byte.TYPE);
        short address = setup(reg, oldValue, offset);

        if (reg.equals("HL"))
            execute(opcode, prefix);
        else
            execute(opcode, prefix, offset);

        assertMemoryContents(address, dec(oldValue));
    }

    private short setup(String reg, byte value, byte offset/* = 0*/) {
        // TODO got error when 1 at (IX|IY)
        short address = createAddressFixture();
        short actualAddress = add(address, offset);
        sut.getBus().pokeb(actualAddress & 0xffff, value & 0xff);
        setReg(reg, address);
//Debug.printf("reg: %s, value: %d, offset: %d, address: %d", reg, value, offset, address);
        return actualAddress;
    }

    private void assertMemoryContents(short address, byte expected) {
        assertEquals(expected & 0xff, sut.getBus().peekb(address & 0xffff));
    }

    @ParameterizedTest
    @MethodSource("DEC_Source")
    public void DEC_aHL_IX_IY_plus_n_sets_SF_appropriately(String reg, byte opcode, Byte prefix) {
        setup(reg, (byte) 0x02, (byte) 0);

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
    @MethodSource("DEC_Source")
    public void DEC_aHL_IX_IY_plus_n_sets_ZF_appropriately(String reg, byte opcode, Byte prefix) {
        setup(reg, (byte) 0x03, (byte) 0);

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
    @MethodSource("DEC_Source")
    public void DEC_aHL_IX_IY_plus_n_sets_HF_appropriately(String reg, byte opcode, Byte prefix) {
        for (byte b : new byte[] {0x11, (byte) 0x81, (byte) 0xF1}) {
            setup(reg, b, (byte) 0);

            execute(opcode, prefix);
            assertEquals(false, sut.isH());

            execute(opcode, prefix);
            assertEquals(true, sut.isH());

            execute(opcode, prefix);
            assertEquals(false, sut.isH());
        }
    }

    @ParameterizedTest
    @MethodSource("DEC_Source")
    public void DEC_aHL_IX_IY_plus_n_sets_PF_appropriately(String reg, byte opcode, Byte prefix) {
        setup(reg, (byte) 0x81, (byte) 0);

        execute(opcode, prefix);
        assertEquals(false, sut.isP());

        execute(opcode, prefix);
        assertEquals(true, sut.isP());

        execute(opcode, prefix);
        assertEquals(false, sut.isP());
    }

    @ParameterizedTest
    @MethodSource("DEC_Source")
    public void DEC_aHL_IX_IY_plus_n_sets_NF(String reg, byte opcode, Byte prefix) {
        assertSetsFlags(opcode, prefix, "N");
    }

    @ParameterizedTest
    @MethodSource("DEC_Source")
    public void DEC_aHL_IX_IY_plus_n_does_not_change_CF(String reg, byte opcode, Byte prefix) {
        byte[] randomValues = fixture.create(byte[].class);

        for (byte value : randomValues) {
            setup(reg, value, (byte) 0);

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
    @MethodSource("DEC_Source")
    public void DEC_aHL_IX_IY_plus_n_sets_bits_3_and_5_from_result(String reg, byte opcode, Byte prefix) {
        setup(reg, withBit(withBit((byte) 1, 3, true), 5, false), (byte) 0);
        execute(opcode, prefix);
        assertEquals(true, sut.is3());
        assertEquals(false, sut.is5());

        setup(reg, withBit(withBit((byte) 1, 3, false), 5, true), (byte) 0);
        execute(opcode, prefix);
        assertEquals(false, sut.is3());
        assertEquals(true, sut.is5());
    }

    @ParameterizedTest
    @MethodSource("DEC_Source")
    public void DEC_aHL_IX_IY_plus_n_returns_proper_T_states(String reg, byte opcode, Byte prefix) {
        int states = execute(opcode, prefix);
        assertEquals(reg.equals("HL") ? 11 : 23, states);
    }
}

