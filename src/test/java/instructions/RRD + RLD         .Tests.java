package instructions;

import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class RRD_RLD_tests extends InstructionsExecutionTestsBase {

    private static final byte prefix = (byte) 0xED;

    static Stream<Arguments> RRD_RLD_Source() {
        return Stream.of(
                arguments((byte) 0x67, "R"),
                arguments((byte) 0x6F, "L")
        );
    }

    @ParameterizedTest
    @MethodSource("RRD_RLD_Source")
    public void RRD_RLD_moves_data_appropriately(byte opcode, String direction) {
        byte oldHLContents = asBinaryByte("1001 0110");
        byte oldAValue = asBinaryByte("0011 1010");
        short address = setup(oldHLContents, oldAValue);

        execute(opcode, prefix);

        byte expectedHLContents, expectedAValue;
        if (direction.equals("R")) {
            expectedHLContents = asBinaryByte("1010 1001");
            expectedAValue = asBinaryByte("0011 0110");
        } else {
            expectedHLContents = asBinaryByte("0110 1010");
            expectedAValue = asBinaryByte("0011 1001");
        }

        assertMemoryContents(address, expectedHLContents);
        assertEquals(expectedAValue & 0xff, sut.getA());
    }

    private short setup(byte HLcontents, byte Avalue) {
        sut.setA(Avalue & 0xff);
        // TODO got error when 1 at "moves_data_appropriately" (L)
        short address = createAddressFixture();
        sut.getBus().pokeb(address & 0xffff, HLcontents & 0xff);
        sut.setHL(address & 0xffff);
        return address;
    }

    private void assertMemoryContents(short address, byte expected) {
        assertEquals(expected & 0xff, sut.getBus().peekb(address & 0xffff));
    }

    @ParameterizedTest
    @MethodSource("RRD_RLD_Source")
    public void RRD_RLD_sets_SF_appropriately(byte opcode, String direction) {
        for (int i = 0; i <= 255; i++) {
            byte b = (byte) i;
            sut.setA(b & 0xff);
            execute(opcode, prefix);
            assertEquals((b & 0xff) >= 128, sut.isS());
        }
    }

    @ParameterizedTest
    @MethodSource("RRD_RLD_Source")
    public void RRD_RLD_sets_ZF_appropriately(byte opcode, String direction) {
        for (int i = 0; i <= 255; i++) {
            byte b = (byte) i;
            setup(b, (byte) 0);
            execute(opcode, prefix);
            assertEquals(sut.getA() == 0, sut.isZ());
        }
    }

    @ParameterizedTest
    @MethodSource("RRD_RLD_Source")
    public void RRD_RLD_resets_HF(byte opcode, String direction) {
        assertResetsFlags(opcode, prefix, "H");
    }

    @ParameterizedTest
    @MethodSource("RRD_RLD_Source")
    public void RRD_RLD_sets_PF_as_parity(byte opcode, String direction) {
        for (int i = 0; i <= 255; i++) {
            byte b = (byte) i;
            setup(fixture.create(Byte.TYPE), b);
            execute(opcode, prefix);
            assertEquals(parity[sut.getA() & 0xff] != 0, sut.isP());
        }
    }

    @ParameterizedTest
    @MethodSource("RRD_RLD_Source")
    public void RRD_RLD_resets_NF(byte opcode, String direction) {
        assertResetsFlags(opcode, prefix, "N");
    }

    @ParameterizedTest
    @MethodSource("RRD_RLD_Source")
    public void RRD_RLD_does_not_chance_CF(byte opcode, String direction) {
        assertDoesNotChangeFlags(opcode, prefix, "C");
    }

    @Disabled("not implemented")
    @ParameterizedTest
    @MethodSource("RRD_RLD_Source")
    public void RRD_RLD_sets_bits_3_and_5_from_result(byte opcode, String direction) {
        for (int i = 0; i <= 255; i++) {
            byte b = (byte) i;
            setup(fixture.create(Byte.TYPE), b);
            execute(opcode, prefix);
            assertEquals(getBit((byte) (sut.getA() & 0xff), 3), sut.is3());
            assertEquals(getBit((byte) (sut.getA() & 0xff), 5), sut.is5());
        }
    }

    @ParameterizedTest
    @MethodSource("RRD_RLD_Source")
    public void RRD_RLD_returns_proper_T_states(byte opcode, String direction) {
        int states = execute(opcode, prefix);
        assertEquals(18, states);
    }
}

