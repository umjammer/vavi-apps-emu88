package instructions;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


/*partial*/ class LD_r_IX_IY_plus_d_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> LD_Source() {
        return Stream.of(
                arguments("A", "IX", (byte) 0x7E, (byte) 0xDD),
                arguments("B", "IX", (byte) 0x46, (byte) 0xDD),
                arguments("C", "IX", (byte) 0x4E, (byte) 0xDD),
                arguments("D", "IX", (byte) 0x56, (byte) 0xDD),
                arguments("E", "IX", (byte) 0x5E, (byte) 0xDD),
                arguments("H", "IX", (byte) 0x66, (byte) 0xDD),
                arguments("L", "IX", (byte) 0x6E, (byte) 0xDD),
                arguments("A", "IY", (byte) 0x7E, (byte) 0xFD),
                arguments("B", "IY", (byte) 0x46, (byte) 0xFD),
                arguments("C", "IY", (byte) 0x4E, (byte) 0xFD),
                arguments("D", "IY", (byte) 0x56, (byte) 0xFD),
                arguments("E", "IY", (byte) 0x5E, (byte) 0xFD),
                arguments("H", "IY", (byte) 0x66, (byte) 0xFD),
                arguments("L", "IY", (byte) 0x6E, (byte) 0xFD)
        );
    }

    @ParameterizedTest
    @MethodSource("LD_Source")
    public void LD_r_IX_IY_plus_d_loads_value_from_memory(String destReg, String srcPointerReg, byte opcode, byte prefix) {
        short address = fixture.create(Short.TYPE);
        byte offset = fixture.create(Byte.TYPE);
        byte oldValue = fixture.create(Byte.TYPE);
        byte newValue = fixture.create(Byte.TYPE);
        short actualAddress = add(address, offset);

        setReg(srcPointerReg, address);
        setReg(destReg, oldValue);
        sut.getBus().pokeb(actualAddress & 0xffff, newValue & 0xff);

        execute(opcode, prefix, offset);

        assertEquals(newValue, this.getRegW(destReg));
    }

    @ParameterizedTest
    @MethodSource("LD_Source")
    public void LD_r_IX_IY_plus_d_do_not_modify_flags(String destReg, String srcPointerReg, byte opcode, byte prefix) {
        assertNoFlagsAreModified(opcode, prefix);
    }

    @ParameterizedTest
    @MethodSource("LD_Source")
    public void LD_r_IX_IY_plus_d_return_proper_T_states(String destReg, String srcPointerReg, byte opcode, byte prefix) {
        int states = execute(opcode, prefix);
        assertEquals(19, states);
    }
}
