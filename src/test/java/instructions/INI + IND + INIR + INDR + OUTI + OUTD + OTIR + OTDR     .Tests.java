package instructions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vavi.util.Debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static vavi.apps.em88.Z80.dec16bitInternal;
import static vavi.apps.em88.Z80.dec8bitInternal;
import static vavi.apps.em88.Z80.inc16bitInternal;


class INI_IND_INIR_INDR_tests extends InstructionsExecutionTestsBase {

    private static final byte prefix = (byte) 0xED;

    public static final byte INI_Source = (byte) 0xA2;
    public static final byte IND_Source = (byte) 0xAA;
    public static final byte INIR_Source = (byte) 0xB2;
    public static final byte INDR_Source = (byte) 0xBA;
    public static final byte OUTI_Source = (byte) 0xA3;
    public static final byte OUTD_Source = (byte) 0xAB;
    public static final byte OTIR_Source = (byte) 0xB3;
    public static final byte OTDR_Source = (byte) 0xBB;

    @BeforeEach
    protected void setup() {
        super.setup();
    }

    @ParameterizedTest
    @ValueSource(bytes = {INI_Source, IND_Source, INIR_Source, INDR_Source})
    public void INI_IND_INIR_INDR_read_value_from_port_aC_into_aHL(byte opcode) {
        byte portNumber = fixture.create(Byte.TYPE);
        byte value = fixture.create(Byte.TYPE);
        byte oldValue = fixture.create(Byte.TYPE);
        short address = fixture.create(Short.TYPE);

        sut.setHL(address & 0xffff);
        sut.getBus().pokeb(address & 0xffff, oldValue & 0xff);

        executeWithPortSetup(opcode, portNumber, value);

        int actual = sut.getBus().peekb(address & 0xffff);
        assertEquals(value, actual);
    }

    @ParameterizedTest
    @ValueSource(bytes = {OUTI_Source, OUTD_Source, OTIR_Source, OTDR_Source})
    public void OUTI_OUTD_OTIR_OTDR_write_value_from_aHL_into_port_aC(byte opcode) {
        byte portNumber = fixture.create(Byte.TYPE);
        byte value = fixture.create(Byte.TYPE);
        byte oldValue = fixture.create(Byte.TYPE);
        short address = createAddressFixture();

        sut.setHL(address & 0xffff);
        sut.getBus().pokeb(address & 0xffff, value & 0xff);

        executeWithPortSetup(opcode, portNumber, oldValue);

        int actual = sut.getBus().inp(portNumber & 0xff);
        assertEquals(value & 0xff, actual);
    }

    @ParameterizedTest
    @ValueSource(bytes = {INI_Source, INIR_Source, OUTI_Source, OTIR_Source})
    public void INI_INIR_OUTI_OTIR_increase_HL(byte opcode) {
        short address = fixture.create(Short.TYPE);

        sut.setHL(address & 0xffff);

        executeWithPortSetup(opcode, (byte) 0, (byte) 0);

        short expected = inc(address);
        assertEquals(expected & 0xffff, sut.getHL());
    }

    @ParameterizedTest
    @ValueSource(bytes = {IND_Source, INDR_Source, OUTD_Source, OTDR_Source})
    public void IND_INDR_OUTD_OTDR_decrease_HL(byte opcode) {
        short address = fixture.create(Short.TYPE);

        sut.setHL(address & 0xffff);

        executeWithPortSetup(opcode, (byte) 0, (byte) 0);

        short expected = dec(address);
        assertEquals(expected & 0xffff, sut.getHL());
    }

    @ParameterizedTest
    @ValueSource(bytes = {INI_Source, IND_Source, INIR_Source, INDR_Source, OUTI_Source, OUTD_Source, OTIR_Source, OTDR_Source})
    public void INI_IND_INIR_INDR_OUTI_OUTD_OTIR_OTDR_decrease_B(byte opcode) {
        byte counter = fixture.create(Byte.TYPE);

        sut.setB(counter & 0xff);

        executeWithPortSetup(opcode, (byte) 0, (byte) 0);

        byte expected = dec(counter);
        assertEquals(expected & 0xff, sut.getB());
    }

    @ParameterizedTest
    @ValueSource(bytes = {INI_Source, IND_Source, INIR_Source, INDR_Source, OUTI_Source, OUTD_Source, OTIR_Source, OTDR_Source})
    public void INI_IND_INIR_INDR_OUTI_OUTD_OTIR_OTDR_set_Z_if_B_reaches_zero(byte opcode) {
        for (int i = 0; i < 256; i++) {
            byte b = (byte) i;
            sut.setB(b & 0xff);

            executeWithPortSetup(opcode, (byte) 0, (byte) 0);

            assertEquals(dec(b) == 0, sut.isZ());
        }
    }

    @ParameterizedTest
    @ValueSource(bytes = {INI_Source, IND_Source, INIR_Source, INDR_Source, OUTI_Source, OUTD_Source, OTIR_Source, OTDR_Source})
    public void INI_IND_INIR_INDR_OUTI_OUTD_OTIR_OTDR_set_NF(byte opcode) {
        assertSetsFlags(opcode, prefix, "N");
    }

    @ParameterizedTest
    @ValueSource(bytes = {INI_Source, IND_Source, INIR_Source, INDR_Source, OUTI_Source, OUTD_Source, OTIR_Source, OTDR_Source})
    public void INI_IND_INIR_INDR_OUTI_OUTD_OTIR_OTDR_do_not_change_CF(byte opcode) {
        assertDoesNotChangeFlags(opcode, prefix, "C");
    }

    @Disabled("not implemented")
    @ParameterizedTest
    @ValueSource(bytes = {INI_Source, IND_Source, INIR_Source, INDR_Source, OUTI_Source, OUTD_Source, OTIR_Source, OTDR_Source})
    public void INI_IND_INIR_INDR_OUTI_OUTD_OTIR_OTDR_set_bits_3_and_5_from_result_of_decrementing_B(byte opcode) {
        sut.setB(withBit(withBit((byte) 1, 3, true), 5, false));
        execute(opcode, prefix);
        assertEquals(true, sut.is3());
        assertEquals(false, sut.is5());

        sut.setB(withBit(withBit((byte) 1, 3, false), 5, true));
        execute(opcode, prefix);
        assertEquals(false, sut.is3());
        assertEquals(true, sut.is5());
    }

    @ParameterizedTest
    @ValueSource(bytes = {INI_Source, IND_Source, INIR_Source, INDR_Source, OUTI_Source, OUTD_Source, OTIR_Source, OTDR_Source})
    public void INI_IND_INIR_INDR_OUTI_OUTD_OTIR_OTDR_set_SF_appropriately(byte opcode) {
        sut.setB(0x02);

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
    @ValueSource(bytes = {INI_Source, IND_Source, OUTI_Source, OUTD_Source})
    public void INI_IND_OUTI_OUTD_return_proper_T_states(byte opcode) {
        int states = execute(opcode, prefix);
        assertEquals(16, states);
    }

    @ParameterizedTest
    @ValueSource(bytes = {INIR_Source, INDR_Source, OTIR_Source, OTDR_Source})
    public void INIR_INDR_OTIR_OTDR_decrease_PC_by_two_if_counter_does_not_reach_zero(byte opcode) {
        short dataAddress = fixture.create(Short.TYPE);
        short runAddress = fixture.create(Short.TYPE);
        byte portNumber = fixture.create(Byte.TYPE);
        byte oldData = fixture.create(Byte.TYPE);
        byte data = fixture.create(Byte.TYPE);

        sut.getBus().pokeb(dataAddress & 0xffff, oldData & 0xff);
        setPortValue(portNumber, data);
        sut.setHL(dataAddress & 0xffff);
        sut.setB(fixture.create(Byte.TYPE) & 0xff);

        executeAt(runAddress, opcode, prefix);

try {
        assertEquals(runAddress, sut.getPC());
} catch (AssertionError e) {
 Debug.printf("fixture: %d, %d, %d, %d, %d", dataAddress, runAddress, portNumber, oldData, data);
}
    }

    @ParameterizedTest
    @ValueSource(bytes = {INIR_Source, INDR_Source, OTIR_Source, OTDR_Source})
    public void INIR_INDR_OTIR_OTDR_do_not_decrease_PC_by_two_if_counter_reaches_zero(byte opcode) {
        short dataAddress = fixture.create(Short.TYPE);
        short runAddress = fixture.create(Short.TYPE);
        byte portNumber = fixture.create(Byte.TYPE);
        byte oldData = fixture.create(Byte.TYPE);
        byte data = fixture.create(Byte.TYPE);

        sut.getBus().pokeb(dataAddress & 0xffff, oldData & 0xff);
        setPortValue(portNumber, data);
        sut.setHL(dataAddress & 0xffff);
        sut.setB(1);

        executeAt(runAddress, opcode, prefix);

        assertEquals(add(runAddress, 2) & 0xffff, sut.getPC());
    }

    @ParameterizedTest
    @ValueSource(bytes = {INIR_Source, INDR_Source, OTIR_Source, OTDR_Source})
    public void INIR_INDR_OTIR_OTDR_return_proper_T_states_depending_of_value_of_B(byte opcode) {
        sut.setB(128);
        for (int i = 0; i <= 256; i++) {
            int oldB = sut.getB();
            int states = execute(opcode, prefix);
            assertEquals(dec8bitInternal(oldB), sut.getB());
            assertEquals(sut.getB() == 0 ? 16 : 21, states);
        }
    }

    private int executeWithPortSetup(byte opcode, byte portNumber/* = 0*/, byte portValue/* = 0*/) {
        sut.setC(portNumber & 0xff);
        setPortValue(portNumber, portValue);
        return execute(opcode, prefix);
    }
}
