/*
 * Copyright (c) 1993-2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.em88;

import java.util.HashMap;
import java.util.Map;

import static vavi.apps.em88.Z80.inc16bitInternal;


/**
 * Bus.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.10 931205 nsano initial version <br>
 *          0.20 931208 nsano add Z80 instruction <br>
 *          1.00 031228 nsano java porting <br>
 *          1.01 040108 nsano add device related <br>
 */
public abstract class Bus {

    /** A class that represents what memory is actually mapped in a given situation. */
    public static final class Mapping {
        /** */
        public byte[] base;
        /** */
        public int pointer;
    }

    /** An enumeration that describes the direction of memory reads and writes. */
    public enum Direction {
        READ,
        WRITE
    }

    /**
     * @param address 16bit address
     * @param direction {@link Direction} may change {@link Mapping}
     */
    protected abstract Mapping getMapping(int address, Direction direction);

    /** @return unsigned byte */
    public final int peekb(int address) {
        Mapping mapping = getMapping(address, Direction.READ);
        return mapping.base[mapping.pointer] & 0xff;
    }

    /** @return unsigned short */
    public final int peekw(int address) {
        int l = peekb(address);
        int h = peekb(inc16bitInternal(address));

        return (h << 8) | l;
    }

    /** @param value unsigned byte */
    public void pokeb(int address, int value) {
        if (address == 0xEF54) {
             System.err.printf("Writing to EF54: %02x\n", value);
        }
        Mapping mapping = getMapping(address, Direction.WRITE);
        mapping.base[mapping.pointer] = (byte) (value & 0xff);
//logger.log(Level.TRACE, StringUtil.toHex4(a) + ": " + StringUtil.toHex2(d));
    }

    /** */
    public final void pokew(int address, int value) {
        pokeb(address, value);
        pokeb(inc16bitInternal(address), value >> 8);
//logger.log(Level.TRACE, StringUtil.toHex4(a) + ": " + StringUtil.toHex2(d >> 8) + StringUtil.toHex2(d & 0xff));
    }

    /** */
    public final void pokew(int address, int h, int l) {
        pokeb(address, h);
        pokeb(inc16bitInternal(address), l);
//logger.log(Level.TRACE, StringUtil.toHex4(a) + ": " + StringUtil.toHex2(d >> 8) + StringUtil.toHex2(d & 0xff));
    }

    /** */
    public void pokes(int address, byte[] b, int ofs, int len) {
        Mapping mapping = getMapping(address, Direction.WRITE);
        System.arraycopy(b, ofs, mapping.base, mapping.pointer, len);
    }

    /** */
    public void pokes(int address, byte[] b) {
        pokes(address, b, 0, b.length);
    }

    /** */
    public abstract int inp(int port);

    /** */
    public abstract void outp(int port, int data);

    // ----

    /** */
    private final Map<String, Device> devices = new HashMap<>();

    /** */
    public void addDevice(Device device) {
        devices.put(device.getClass().getName(), device);
    }

    /** */
    public Device getDevice(String name) {
        return devices.get(name);
    }

    /** */
    public void reset() {
        for (Device device : devices.values()) {
            device.setBus(this);
        }
    }
}
