package instructions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.flextrade.jfixture.JFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.provider.Arguments;
import vavi.apps.em88.Bus;
import vavi.apps.em88.Z80;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static vavi.apps.em88.Z80.inc16bitInternal;


abstract class InstructionsExecutionTestsBase {

    protected Z80 sut;
    protected JFixture fixture;

    @BeforeEach
    protected void setup() {
        sut = new Z80();
        sut.setBus(new Bus.SimpleBus());

        fixture = new JFixture();
    }

//#region Auxiliary methods

    protected int nextFetchesAddress;

    protected void setPortValue(byte portNumber, byte value) {
        sut.getBus().outp(portNumber & 0xff, value & 0xff);
    }

    protected byte getPortValue(byte portNumber) {
        return (byte) (sut.getBus().inp(portNumber & 0xff) & 0xff);
    }

    protected void setMemoryContents(byte... opcodes) {
        setMemoryContentsAt((byte) 0, opcodes);
    }

    protected void setMemoryContentsAt(short address, byte... opcodes) {
        sut.getBus().pokes(address & 0xffff, opcodes);
        nextFetchesAddress = add(address, opcodes.length);
    }

    protected void continueSettingMemoryContents(byte... opcodes) {
        sut.getBus().pokes(nextFetchesAddress, opcodes);
        nextFetchesAddress += opcodes.length;
    }

    protected short getRegW(String name) {
        try {
            Method method = sut.getClass().getMethod("get" + name);
            return (short) ((int) method.invoke(sut) & 0xffff);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    protected byte getRegB(String name) {
        try {
            Method method = sut.getClass().getMethod("get" + name);
            return (byte) ((int) method.invoke(sut) & 0xff);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    protected boolean getFlag(String name) {
        try {
            Method method = sut.getClass().getMethod("is" + name);
            return (boolean) method.invoke(sut);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void setReg(String regName, byte value) {
        try {
            regProperty("set" + regName, Integer.TYPE).invoke(sut, value & 0xff);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void setReg(String regName, short value) {
        try {
            regProperty("set" + regName, Integer.TYPE).invoke(sut, value & 0xffff);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void setFlag(String flagName, boolean value) {
        try {
            regProperty("set" + flagName, Boolean.TYPE).invoke(sut, value);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    protected Method regProperty(String name, Class<?> arg) {
        try {
            return sut.getClass().getMethod(name, arg);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    protected int execute(byte opcode, Byte prefix/*= null*/, byte... nextFetches) {
        return executeAt((short) 0, opcode, prefix, nextFetches);
    }

    protected int executeAt(short address, byte opcode, Byte prefix/*= null*/, byte... nextFetches) {
        sut.setPC(inc16bitInternal(address & 0xffff)); // Inc needed to simulate the first fetch made by the enclosing Z80Processor
        if (prefix == null) {
            setMemoryContentsAt(inc(address), nextFetches);
            return sut.exec(opcode & 0xff);
        } else {
            setMemoryContentsAt(inc(address), opcode);
            continueSettingMemoryContents(nextFetches);

            return sut.exec(prefix & 0xff);
        }
    }

    protected Object ifIndexRegister(String regName, Object value, Object else_) {
        return regName.startsWith("IX") || regName.startsWith("IY") ? value : else_;
    }

    protected void assertNoFlagsAreModified(byte opcode, Byte prefix/*= null*/) {
        int value = fixture.create(Byte.TYPE);
        sut.setF(value);
        execute(opcode, prefix);

        assertEquals(value, sut.getF());
    }

    protected void assertSetsFlags(byte opcode, Byte prefix/*= null*/, String... flagNames) {
        assertSetsFlags(null, opcode, prefix, flagNames);
    }

    protected void assertSetsFlags(Runnable executor, byte opcode, Byte prefix/*= null*/, String... flagNames) {
        assertSetsOrResetsFlags(opcode, true, prefix, executor, flagNames);
    }

    protected void assertResetsFlags(byte opcode, Byte prefix/*= null*/, String... flagNames) {
        assertResetsFlags(null, opcode, prefix, flagNames);
    }

    protected void assertResetsFlags(Runnable executor, byte opcode, Byte prefix/*= null*/, String... flagNames) {
        assertSetsOrResetsFlags(opcode, false, prefix, executor, flagNames);
    }

    protected void assertSetsOrResetsFlags(byte opcode, boolean expected, Byte prefix/*= null*/, Runnable executor/*= null*/, String... flagNames) {
        if (executor == null)
            executor = () -> execute(opcode, prefix);

        Collection<Byte> randomValues = fixture.collections().createCollection(Byte.TYPE, 3);

        for (byte value : randomValues) {
            for (String flag : flagNames)
                setFlag(flag, !expected);

            sut.setA(value);

            executor.run();

            for (String flag : flagNames)
                assertEquals(expected, getFlag(flag));
        }
    }

    protected void assertDoesNotChangeFlags(byte opcode, Byte prefix/*= null*/, String... flagNames) {
        assertDoesNotChangeFlags(null, opcode, prefix, flagNames);
    }

    protected void assertDoesNotChangeFlags(Runnable executor, byte opcode, Byte prefix/*= null*/, String... flagNames) {
        if (executor == null)
            executor = () -> execute(opcode, prefix);

        if (flagNames.length == 0)
            flagNames = new String[] {"C", "H", "S", "Z", "P", "N", "3", "5"};

        Map<String, Boolean> randomFlags = toMap(flagNames, x -> x, x -> fixture.create(Boolean.TYPE));

        for (String flag : flagNames)
            setFlag(flag, randomFlags.get(flag));

        for (int i = 0; i <= fixture.create(Byte.TYPE); i++) {
            executor.run();

            for (String flag : flagNames)
                assertEquals(randomFlags.get(flag), getFlag(flag));
        }
    }

    protected void writeShortToMemory(short address, short value) {
        sut.getBus().pokew(address & 0xffff, value & 0xffff);
    }

    protected short readShortFromMemory(int address) {
        return readShortFromMemory((short) (address & 0xffff));
    }

    protected short readShortFromMemory(short address) {
        return (short) (sut.getBus().peekw(address & 0xffff) & 0xffff);
    }

    protected void setupRegOrMem(String reg, byte value, byte offset/* = 0*/) {
        if (reg.equals("(HL)")) {
            short address = createAddressFixture();
            sut.getBus().pokeb(address & 0xffff, value & 0xff);
            sut.setHL(address & 0xffff);
        } else if (reg.startsWith(("(I"))) {
            String regName = reg.substring(1, 1 + 2);
            short address = createAddressFixture((short) 1, (short) 2, (short) 3);
            short realAddress = add(address, offset);
            sut.getBus().pokeb(realAddress & 0xffff, value & 0xff);
            setReg(regName, address);
        } else {
            setReg(reg, value);
        }
    }

    protected byte valueOfRegOrMem(String reg, byte offset/* = 0*/) {
        if (reg.equals("(HL)")) {
            return (byte) (sut.getBus().peekb(sut.getHL()) & 0xff);
        } else if (reg.startsWith(("(I"))) {
            String regName = reg.substring(1, 1 + 2);
            short address = add(this.getRegW(regName), offset);
            return (byte) (sut.getBus().peekb(address & 0xffff) & 0xff);
        } else {
            return this.getRegB(reg);
        }
    }

    protected static List<Arguments> getBitInstructionsSource(byte baseOpcode, boolean includeLoadReg/* = true*/, boolean loopSevenBits/* = false*/) {
        final Object[][] bases = new Object[][] {
                new Object[] {"A", 7},
                new Object[] {"B", 0},
                new Object[] {"C", 1},
                new Object[] {"D", 2},
                new Object[] {"E", 3},
                new Object[] {"H", 4},
                new Object[] {"L", 5},
                new Object[] {"(HL)", 6}
        };

        List<Arguments> sources = new ArrayList<>();
        int bitsCount = loopSevenBits ? 7 : 0;
        for (int bit = 0; bit <= bitsCount; bit++) {
            for (Object[] instr : bases) {
                String reg = (String) instr[0];
                int regCode = (int) instr[1];
                int opcode = baseOpcode | (bit << 3) | regCode;
                //srcReg, dest, opcode, prefix, bit
                sources.add(arguments(reg, null, (byte) opcode, null, bit));
            }

            for (Object[] instr : bases) {
                String destReg = (String) instr[0];
                if (destReg.equals("(HL)")) destReg = "";
                if (!destReg.isEmpty() && !includeLoadReg) continue;
                int regCode = baseOpcode | (bit << 3) | (int) instr[1];
                for (String reg : new String[] {"(IX+n)", "(IY+n)"}) {
if (!destReg.isEmpty()) continue; // TODO not implemented "xxx i[xy], r"
                    //srcReg, dest, opcode, prefix, bit
                    sources.add(arguments(
                            reg, destReg, (byte) regCode,
                            reg.charAt(2) == 'X' ? (byte) 0xDD :(byte) 0xFD,
                            bit));
                }
            }
        }

        return sources;
    }

    /**
     * @param regNamesArrayIndex OUT
     * @param prefix OUT
     */
    protected static void modifyTestCaseCreationForIndexRegs(String regName, /* ref */int[] regNamesArrayIndex, /* out */Byte[] prefix) {
//            prefix = null;

        switch (regName) {
        case "IXH":
            regNamesArrayIndex[0] = 4;
            prefix[0] = (byte) 0xDD;
            break;
        case "IXL":
            regNamesArrayIndex[0] = 5;
            prefix[0] = (byte) 0xDD;
            break;
        case "IYH":
            regNamesArrayIndex[0] = 4;
            prefix[0] = (byte) 0xFD;
            break;
        case "IYL":
            regNamesArrayIndex[0] = 5;
            prefix[0] = (byte) 0xFD;
            break;
        case "(IX+n)":
            regNamesArrayIndex[0] = 6;
            prefix[0] = (byte) 0xDD;
            break;
        case "(IY+n)":
            regNamesArrayIndex[0] = 6;
            prefix[0] = (byte) 0xFD;
            break;
        }
    }

    protected int executeBit(byte opcode, Byte prefix/*= null*/, Byte offset/*= null*/) {
        if (prefix == null)
            return execute(opcode, (byte) 0xCB);
        else
            return execute((byte) 0xCB, prefix, offset, opcode);
    }

//#endregion

//#region ParityTable

    protected static final byte[] parity = new byte[] {
            1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1,
            0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0,
            0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0,
            1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1,
            0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0,
            1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1,
            1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1,
            0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0,
            0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0,
            1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1,
            1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1,
            0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0,
            1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1,
            0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0,
            0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0,
            1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1};

//#endregion

    public static <K, V> Map<K, V> toMap(String[] ss, Function<String, K> k, Function<String, V> v) {
        Map<K, V> map = new HashMap<>();
        for (String s : ss) {
            map.put(k.apply(s), v.apply(s));
        }
        return map;
    }

    /**
     * Generates a short number from two bytes.
     *
     * @param lowByte Low byte of the new number
     * @param highByte High byte of the new number
     * @return  Generated number
     */
    public static short createShort(byte lowByte, byte highByte) {
        return (short) ((highByte << 8) | (lowByte & 0xff));
    }

    /**
     * Gets the value of a certain bit in a byte.
     * The rightmost bit has position 0, the leftmost bit has position 7.
     *
     * @param value Number to get the bit from
     * @param bitPosition the bit position to retrieve
     * @return Retrieved bit value
     */
    public static boolean getBit(byte value, int bitPosition) {
        if (bitPosition < 0 || bitPosition > 7)
            throw new IllegalArgumentException("bit position must be between 0 and 7");

        return (value & (1 << bitPosition)) != 0;
    }

    /**
     * Retuens a copy of the value that has a certain bit set or reset.
     * The rightmost bit has position 0, the leftmost bit has position 7.
     *
     * @param number The original number
     * @param bitPosition The bit position to modify
     * @param value The bit value
     * @return The original number with the bit appropriately modified
     */
    public static byte withBit(byte number, int bitPosition, boolean value) {
        if (bitPosition < 0 || bitPosition > 7)
            throw new IllegalArgumentException("bit position must be between 0 and 7");

        if (value) {
            return (byte) (number | (1 << bitPosition));
        } else {
            return (byte) (number & ~(1 << bitPosition));
        }
    }

    /**
     * Gets the high byte of a short value.
     *
     * @param value Number to get the high byte from
     * @return High byte of the number
     */
    public static byte getHighByte(short value) {
        return (byte) (value >> 8);
    }

    /**
     * Gets the low byte of a short value.
     *
     * @param value Number to get the low byte from
     * @return Loq byte of the number
     */
    public static byte getLowByte(short value) {
        return (byte) (value & 0xFF);
    }

    /**
     * Increases a number and turns it into zero if it has its maximum value.
     *
     * @param value Number to increase
     * @return Increased number, or zero
     */
    public static short inc(short value) {
        return (short) (((value & 0xffff) + 1) & 0xffff);
    }

    /**
     * Decreases a number and turns it into max value if it passes under its minimum value.
     *
     * @param value Number to decrease
     * @return Increased number, or zero
     */
    public static short dec(short value) {
        return (short) (((value & 0xffff) - 1) & 0xffff);
    }

    /**
     * Adds a value to a number and overlaps it from zero if it passes its maximum value.
     *
     * @param value Number to increase (unsigned)
     * @param offset (signed)
     * @return Increased number, or zero
     */
    public static short add(short value, int offset) {
        return (short) (((value & 0xffff) + offset) & 0xffff);
    }

    /**
     * Subtract a value to a number and overlaps it from its max value if it passes below its minimum value.
     *
     * @param value Number to decrease (unsigned)
     * @param amount signed integer
     * @return Increased number, or zero
     */
    public static short sub(short value, int amount) {
        return (short) (((value & 0xffff) - amount) & 0xffff);
    }

    /**
     * Increases a number and turns it into zero if it has its maximum value.
     *
     * @param value Number to increase
     * @return Increased number, or zero
     */
    public static byte inc(byte value) {
        return (byte) (((value & 0xff) + 1) & 0xff);
    }

    /**
     * Decreases a number and turns it into max value if it passes under its minimum value.
     *
     * @param value Number to decrease
     * @return Increased number, or zero
     */
    public static byte dec(byte value) {
        return (byte) (((value & 0xff) - 1) & 0xff);
    }

    /**
     * Adds a value to the number and overlaps it from zero if it passes its maximum value.
     *
     * @param value Number to increase
     * @param amount Amount to add to the number
     * @return Increased number, or zero
     */
    public static byte add(byte value, int amount) {
        return (byte) (value + (byte) amount);
    }

    /**
     * Subtract a value to the number and overlaps it from its max value if it passes below its minimum value.
     *
     * @param value Number to decrease
     * @param amount Amount to subtract to the number
     * @return Increased number, or zero
     */
    public static byte sub(byte value, int amount) {
        return (byte) (value - (byte) amount);
    }

    /**
     * Increments the value using only the lowest seven bits (the most significant bit is unchanged).
     *
     * @param value Number to increment
     * @return Incremented number
     */
    public static byte inc7Bits(byte value) {
        return (byte) ((value & 0x80) == 0 ? (value + 1) & 0x7F : (value + 1) & 0x7F | 0x80);
    }

    /**
     * Checks if the value lies in a specified range.
     *
     * @param value The number to check
     * @param fromInclusive The lower end of the range
     * @param toInclusive The higher end of the range
     * @return True if the value lies within the range, false otherwise
     */
    public static boolean between(byte value, byte fromInclusive, byte toInclusive) {
        return (fromInclusive & 0xff) <= (value & 0xff) && (value & 0xff) <= (toInclusive & 0xff);
    }

    public static byte asBinaryByte(String binaryString) {
        return (byte) Integer.parseInt(binaryString.replace(" ", ""), 2);
    }

    /**
     * Generates a byte array from the low and high byte of the value.
     *
     * @param value Original value
     * @return Generated byte array
     */
    public static byte[] toByteArray(short value) {
        return new byte[] {
                getLowByte(value), getHighByte(value)
        };
    }

    protected short createAddressFixture() {
        return createAddressFixture((short) 1);
    }

    /** TODO some tests doesn't allow address 1 */
    protected short createAddressFixture(short... excepts) {
        short s;
        do {
            s = fixture.create(Short.TYPE);
        } while (Arrays.binarySearch(excepts, s) >= 0);
//Debug.println("address fixture: " + s);
        return s;
    }
}
