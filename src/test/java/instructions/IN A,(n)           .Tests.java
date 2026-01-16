package instructions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class IN_A_n_tests extends InstructionsExecutionTestsBase {

    private static final byte IN_A_n_opcode = (byte) 0xDB;

    @Test
    public void IN_A_n_reads_value_from_port() {
        byte portNumber = fixture.create(Byte.TYPE);
        byte value = fixture.create(Byte.TYPE);
        byte oldValue = fixture.create(Byte.TYPE);

        sut.setA(oldValue);
        setPortValue(portNumber, value);

        execute(IN_A_n_opcode, null, portNumber);

        assertEquals(value, sut.getA());
    }

    @Test
    public void IN_A_n_does_not_modify_flags() {
        assertDoesNotChangeFlags(IN_A_n_opcode, null);
    }

    @Test
    public void IN_A_n_returns_proper_T_states() {
        int states = execute(IN_A_n_opcode, null);
        assertEquals(11, states);
    }
}
