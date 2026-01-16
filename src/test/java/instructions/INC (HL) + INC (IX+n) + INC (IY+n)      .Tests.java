package instructions;

import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class INC_aHL_IX_IY_plus_n_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> INC_Source() {
        return Stream.of(
                arguments("HL", (byte) 0x34, null),
                arguments("IX", (byte) 0x34, (byte) 0xDD),
                arguments("IY", (byte) 0x34, (byte) 0xFD)
        );
    }

    @ParameterizedTest
    @MethodSource("INC_Source")
    public void INC_aHL_IX_IY_plus_n_increases_value_appropriately(String reg, byte opcode, Byte prefix) {
        byte oldValue = fixture.create(Byte.TYPE);
        byte offset = reg.equals("HL") ? (byte) 0 : fixture.create(Byte.TYPE);
        short address = setup(reg, oldValue, offset);

        if (reg.equals("HL"))
            execute(opcode, prefix);
        else
            execute(opcode, prefix, offset);

        assertMemoryContents(address, inc(oldValue));
    }

    private short setup(String reg, byte value, byte offset /*= 0*/) {
        // TODO got error when 1 at "increases_value_appropriately" (IX|IY)
        // offset is always 0, then when address is 0, actualAddress becomes 1, so excepts 0 also
        // 2 ???
        short address = createAddressFixture((short) 0, (short) 1, (short) 2);
        short actualAddress = add(address, offset);
        sut.getBus().pokeb(actualAddress & 0xffff, value & 0xff);
        setReg(reg, address);
        return actualAddress;
    }

    private void assertMemoryContents(short address, byte expected) {
        assertEquals(expected & 0xff, sut.getBus().peekb(address & 0xffff));
    }

    @ParameterizedTest
    @MethodSource("INC_Source")
    public void INC_aHL_IX_IY_plus_n_sets_SF_appropriately(String reg, byte opcode, Byte prefix) {
        setup(reg, (byte) 0xFD, (byte) 0);

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
    @MethodSource("INC_Source")
    public void INC_aHL_IX_IY_plus_n_sets_ZF_appropriately(String reg, byte opcode, Byte prefix) {
        setup(reg, (byte) 0xFD, (byte) 0);

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
    @MethodSource("INC_Source")
    public void INC_aHL_IX_IY_plus_n_sets_HF_appropriately(String reg, byte opcode, Byte prefix) {
        for (byte b : new byte[] {0x0E, 0x7E, (byte) 0xFE}) {
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
    @MethodSource("INC_Source")
    public void INC_aHL_IX_IY_plus_n_sets_PF_appropriately(String reg, byte opcode, Byte prefix) {
        setup(reg, (byte) 0x7E, (byte) 0);

        execute(opcode, prefix);
        assertEquals(false, sut.isP());

        execute(opcode, prefix);
        assertEquals(true, sut.isP());

        execute(opcode, prefix);
        assertEquals(false, sut.isP());
    }

    @ParameterizedTest
    @MethodSource("INC_Source")
    public void INC_aHL_IX_IY_plus_n_resets_NF(String reg, byte opcode, Byte prefix) {
        assertResetsFlags(opcode, prefix, "N");
    }

    @ParameterizedTest
    @MethodSource("INC_Source")
    public void INC_aHL_IX_IY_plus_n_does_not_change_CF(String reg, byte opcode, Byte prefix) {
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
    @MethodSource("INC_Source")
    public void INC_aHL_IX_IY_plus_n_sets_bits_3_and_5_from_result(String reg, byte opcode, Byte prefix) {
        setup(reg, withBit(withBit(((byte) 0), 3, true), 5, false), (byte) 0);
        execute(opcode, prefix);
        assertEquals(true, sut.is3());
        assertEquals(false, sut.is5());

        setup(reg, withBit(withBit(((byte) 0), 3, false), 5, true), (byte) 0);
        execute(opcode, prefix);
        assertEquals(false, sut.is3());
        assertEquals(true, sut.is5());
    }

    @ParameterizedTest
    @MethodSource("INC_Source")
    public void INC_aHL_IX_IY_plus_n_returns_proper_T_states(String reg, byte opcode, Byte prefix) {
        int states = execute(opcode, prefix);
        assertEquals(reg.equals("HL") ? 11 : 23, states);
    }
}
