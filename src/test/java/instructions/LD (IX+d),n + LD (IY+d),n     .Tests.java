package instructions;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static vavi.apps.em88.Z80.add16bitInternal;


class LD_IX_IY_plus_n_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> LD_Source() {
        return Stream.of(
                arguments("IX", (byte) 0x36, (byte) 0xDD),
                arguments("IY", (byte) 0x36, (byte) 0xFD)
        );
    }

    @ParameterizedTest
    @MethodSource("LD_Source")
    public void LD_IX_IY_plus_n_loads_value_in_memory(String reg, byte opcode, byte prefix) {
        short address = fixture.create(Short.TYPE);
        byte offset = fixture.create(Byte.TYPE);
        byte oldValue = fixture.create(Byte.TYPE);
        byte newValue = fixture.create(Byte.TYPE);
        short actualAddress = add(address, offset);

        sut.getBus().pokeb(actualAddress & 0xffff, oldValue & 0xff);
        setReg(reg, address);

        execute(opcode, prefix, offset, newValue);

        assertEquals(newValue & 0xff, sut.getBus().peekb(actualAddress & 0xffff));
    }

    @ParameterizedTest
    @MethodSource("LD_Source")
    public void LD_IX_IY_plus_n_does_not_modify_flags(String reg, byte opcode, byte prefix) {
        assertNoFlagsAreModified(opcode, prefix);
    }

    @ParameterizedTest
    @MethodSource("LD_Source")
    public void LD_IX_IY_plus_n_returns_proper_T_states(String reg, byte opcode, byte prefix) {
        int states = execute(opcode, prefix);
        assertEquals(19, states);
    }
}
