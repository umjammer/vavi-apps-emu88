/*
 * Copyright (c) 1993-2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.em88;

import java.util.HashMap;
import java.util.Map;


/**
 * Bus.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.10 931205 nsano initial version <br>
 *          0.20 931208 nsano add Z80 instruction <br>
 *          1.00 031228 nsano java porting <br>
 *          1.01 040108 nsano add device related <br>
 */
public abstract class Bus {

    /** ある状況で実際どのメモリにマッピングされているかを現すクラスです。 */
    public final class Mapping {
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

    /** */
    public final int peekb(int address) {
        Mapping mapping = getMapping(address, Direction.READ);
        return mapping.base[mapping.pointer] & 0xff;
    }

    /** */
    public final int peekw(int address) {
        int l = peekb(address);
        int h = peekb(address + 1);

        return (h << 8) | l;
    }

    /** */
    public void pokeb(int address, int value) {
        Mapping mapping = getMapping(address, Direction.WRITE);
        mapping.base[mapping.pointer] = (byte) (value & 0xff);
// Debug.println(StringUtil.toHex4(a) + ": " + StringUtil.toHex2(d));
    }

    /** */
    public final void pokew(int address, int value) {
        pokeb(address, value);
        pokeb(address + 1, value >> 8);
// Debug.println(StringUtil.toHex4(a) + ": " + StringUtil.toHex2(d >> 8) + StringUtil.toHex2(d & 0xff));
    }

    /** */
    public final void pokew(int address, int h, int l) {
        pokeb(address, h);
        pokeb(address + 1, l);
// Debug.println(StringUtil.toHex4(a) + ": " + StringUtil.toHex2(d >> 8) + StringUtil.toHex2(d & 0xff));
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

/* */
