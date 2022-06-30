package instructions;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class NEG_tests extends InstructionsExecutionTestsBase {

    private static final byte opcode = 0x44;
    private static final byte prefix = (byte) 0xED;

    @Test
    public void NEG_subtracts_A_from_zero() {
        byte oldValue = fixture.create(Byte.TYPE);

        sut.setA(oldValue & 0xff);
        execute();

        byte expected = sub((byte) 0, oldValue);
        assertEquals(expected & 0xff, sut.getA());
    }

    @Test
    public void NEG_sets_SF_appropriately() {
        sut.setA(2);
        execute();
        assertEquals(true, sut.isS());

        sut.setA(1);
        execute();
        assertEquals(true, sut.isS());

        sut.setA(0);
        execute();
        assertEquals(false, sut.isS());

        sut.setA(0xFF);
        execute();
        assertEquals(false, sut.isS());

        sut.setA(0x80);
        execute();
        assertEquals(true, sut.isS());
    }

    @Test
    public void NEG_sets_ZF_appropriately() {
        sut.setA(2);
        execute();
        assertEquals(false, sut.isZ());

        sut.setA(1);
        execute();
        assertEquals(false, sut.isZ());

        sut.setA(0);
        execute();
        assertEquals(true, sut.isZ());

        sut.setA(0xFF);
        execute();
        assertEquals(false, sut.isZ());

        sut.setA(0x80);
        execute();
        assertEquals(false, sut.isZ());
    }

    @Test
    public void NEG_sets_HF_appropriately() {
        for (int i = 0; i <= 255; i++) {
            byte b = (byte) i;
            sut.setA(b & 0xff);
            execute();
            boolean expected = (((b & 0xff) ^ sut.getA()) & 0x10) != 0;
            assertEquals(expected, sut.isH());
        }
    }

    @Test
    public void NEG_sets_PF_appropriately() {
        for (int i = 0; i <= 255; i++) {
            byte b = (byte) i;
            sut.setA(b & 0xff);
            execute();
            boolean expected = (b & 0xff) == 0x80;
            assertEquals(expected, sut.isP());
        }
    }

    @Test
    public void NEG_sets_NF() {
        assertSetsFlags(opcode, prefix, "N");
    }

    @Test
    public void NEG_sets_CF_appropriately() {
        for (int i = 0; i <= 255; i++) {
            byte b = (byte) i;
            sut.setA(b & 0xff);
            execute();
            boolean expected = (b != 0);
            assertEquals(expected, sut.isC());
        }
    }

    @Disabled("not implemented")
    @Test
    public void NEG_sets_bits_3_and_5_from_result() {
        sut.setA(0x0F);
        execute();
        assertEquals(false, sut.is3());
        assertEquals(true, sut.is5());

        sut.setA(0xF1);
        execute();
        assertEquals(true, sut.is3());
        assertEquals(false, sut.is5());
    }

    @Test
    public void NEG_returns_proper_T_states() {
        int states = execute();
        assertEquals(8, states);
    }

    private int execute() {
        return execute(opcode, prefix);
    }
}
