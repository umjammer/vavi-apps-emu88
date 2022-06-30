package instructions;

import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


@Timeout(1)
class LD_arr_r_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> LD_rr_r_Source() {
        return Stream.of(
                arguments("BC", "A", (byte) 0x02),
                arguments("DE", "A", (byte) 0x12),
                arguments("HL", "A", (byte) 0x77),
                arguments("HL", "B", (byte) 0x70),
                arguments("HL", "C", (byte) 0x71),
                arguments("HL", "D", (byte) 0x72),
                arguments("HL", "E", (byte) 0x73),
                arguments("HL", "H", (byte) 0x74),
                arguments("HL", "L", (byte) 0x75)
        );
    }

    @Disabled("infinit loop")
    @ParameterizedTest
    @MethodSource("LD_rr_r_Source")
    public void LD_arr_r_loads_value_in_memory(String destPointerReg, String srcReg, byte opcode) {
        boolean isHorL = (srcReg.equals("H") || srcReg.equals("L"));

        short address = fixture.create(Short.TYPE);
        byte oldValue = fixture.create(Byte.TYPE);
        byte newValue = fixture.create(Byte.TYPE);

        setReg(destPointerReg, address);
        sut.getBus().pokeb(address & 0xffff, oldValue & 0xff);
        if (!isHorL)
            setReg(srcReg, newValue);

        sut.execute(opcode);

        byte expected = isHorL ? this.getRegB(srcReg) : newValue;
        assertEquals(expected & 0xff, sut.getBus().peekb(address & 0xffff));
    }

    @ParameterizedTest
    @MethodSource("LD_rr_r_Source")
    public void LD_rr_r_do_not_modify_flags(String destPointerReg, String srcReg, byte opcode) {
        assertNoFlagsAreModified(opcode, null);
    }

    @ParameterizedTest
    @MethodSource("LD_rr_r_Source")
    public void LD_rr_r_returns_proper_T_states(String destPointerReg, String srcReg, byte opcode) {
        int states = execute(opcode, null);
        assertEquals(7, states);
    }
}
