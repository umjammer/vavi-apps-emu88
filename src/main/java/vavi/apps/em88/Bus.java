/*
 * Copyright (c) 1993-2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.em88;

import java.util.HashMap;
import java.util.Map;

import static vavi.apps.em88.Z80.add16bitInternal;
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

    /** ある状況で実際どのメモリにマッピングされているかを現すクラスです。 */
    public static final class Mapping {
        /** */
        public byte[] base;
        /** */
        public int pointer;
    }

    /** メモリの読み書きの方向を表す列挙です。 */
    public enum Direction {
        READ,
        WRITE
    }

    /**
     * @param address 16bit のアドレス
     * @param direction {@link Direction} で {@link Mapping} が変わる場合がある
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
        Mapping mapping = getMapping(address, Direction.WRITE);
        mapping.base[mapping.pointer] = (byte) (value & 0xff);
// Debug.println(StringUtil.toHex4(a) + ": " + StringUtil.toHex2(d));
    }

    /** */
    public final void pokew(int address, int value) {
        pokeb(address, value);
        pokeb(inc16bitInternal(address), value >> 8);
// Debug.println(StringUtil.toHex4(a) + ": " + StringUtil.toHex2(d >> 8) + StringUtil.toHex2(d & 0xff));
    }

    /** */
    public final void pokew(int address, int h, int l) {
        pokeb(address, h);
        pokeb(inc16bitInternal(address), l);
// Debug.println(StringUtil.toHex4(a) + ": " + StringUtil.toHex2(d >> 8) + StringUtil.toHex2(d & 0xff));
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

    /** for test */
    public static class SimpleBus extends Bus {
        final byte[] ram = new byte[0x10000];
        final byte[] io = new byte[0x100];
        Mapping address = new Mapping();

        { address.base = ram; }

        @Override protected Mapping getMapping(int a, Direction direction) {
            address.pointer = a;
            return address;
        }

        @Override public int inp(int p) {
            return io[p & 0xff] & 0xff;
        }

        @Override public void outp(int p, int d) {
            io[p & 0xff] = (byte) (d & 0xff);
        }
    }
}

/* */
