/*
 * Copyright (c) 1993-2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.em88;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Properties;

import vavi.util.Debug;


/**
 * Z80 disassembler.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 931209 nsano first <br>
 *          1.00 031228 nsano java porting <br>
 *          2.00 040111 nsano outsource instructions <br>
 */
class Disassembler implements Device {

    /** */
    private Bus bus;

    /** */
    public void setBus(Bus bus) {
        this.bus = bus;
    }

    //----

    /** */
    private String currentMnemonic;

    /** */
    public String getCurrentMnemonic() {
        return currentMnemonic;
    }

    /** */
    private String currentComment;

    /** */
    public String getCurrentComment() {
        return currentComment;
    }

    /**
     * disassemble
     * @return next address
     */
    public int execute(int address) {
        int o1 = bus.peekb(address++);
        int o2 = 0, o3, o4, ad;
        String id;

        currentMnemonic = null;
        currentComment = null;

        switch (o1) {
        case 0xcb: // CB xx
            o2 = bus.peekb(address++);
            currentMnemonic = mnem1(o2);
            break;
        case 0xdd: // DD xx
        case 0xfd: // FD xx
            id = (o1 == 0xdd) ? "IX" : "IY";
            o2 = bus.peekb(address++);
            switch (o2) {
            case 0x21: // 21: ld ID,nn
                o3 = bus.peekb(address++);
                o4 = bus.peekb(address++);
                ad = (o4 << 8) + o3;
                currentMnemonic = MessageFormat.format(mnem2(o2), id, toHex4(ad));
                break;
            case 0x22: // 34: ld (nn),ID
                o3 = bus.peekb(address++);
                o4 = bus.peekb(address++);
                ad = (o4 << 8) + o3;
                currentMnemonic = MessageFormat.format(mnem2(o2), toHex4(ad), id);
                currentComment = toName(ad);
                break;
            case 0x29: // 41: add ID,ID
                currentMnemonic = MessageFormat.format(mnem2(o2), id, id);
                break;
            case 0x2a: // 42: ld ID,(nn)
                o3 = bus.peekb(address++);
                o4 = bus.peekb(address++);
                ad = (o4 << 8) + o3;
                currentMnemonic = MessageFormat.format(mnem2(o2), id, toHex4(ad));
                currentComment = toName(ad);
                break;
            case 0x36: // 54: foo (ID + d), n
                o3 = bus.peekb(address++); // d
                o4 = bus.peekb(address++); // n
                currentMnemonic = MessageFormat.format(mnem2(o2), id, toHex2(o3), toHex2(o4));
                break;
            case 0xcb: // ID cb d xx
                o3 = bus.peekb(address++); // d
                o4 = bus.peekb(address++); // xx
                currentMnemonic = MessageFormat.format(mnem3(o4), id, toHex2(o3));
                break;
            default:
                switch (type2(o2)) {
                case 2:
                    currentMnemonic = MessageFormat.format(mnem2(o2), id);
                    break;
                case 3: // ID + d
                    o3 = bus.peekb(address++);
                    currentMnemonic = MessageFormat.format(mnem2(o2), id, toHex2(o3));
                    break;
                }
                break;
            }
            break;
        case 0xed: // ED xx
            o2 = bus.peekb(address++);
            switch (type4(o2)) {
            case 2:
                currentMnemonic = mnem4(o2);
                break;
            case 104:
                o3 = bus.peekb(address++);
                o4 = bus.peekb(address++);
                ad = (o4 << 8) + o3;
                currentMnemonic = MessageFormat.format(mnem4(o2), toHex4(ad));
                currentComment = toName(ad);
                break;
            }
            break;
        default: // base 0x00 ~ 0xff
            switch (type(o1)) {
            case 1:
                currentMnemonic = mnem(o1);
                break;
            case 2:
                o2 = bus.peekb(address++);
                currentMnemonic = MessageFormat.format(mnem(o1), toHex2(o2));
                break;
            case 202: // jr, djnz
                o2 = bus.peekb(address++);
                currentMnemonic = MessageFormat.format(mnem(o1), toHex2(o2));
                currentComment = toHex4(address + (byte) o2);
                break;
            case 302: // in a,n
                o2 = bus.peekb(address++);
                currentMnemonic = MessageFormat.format(mnem(o1), toHex2(o2));
                currentComment = toNameI(o2);
                break;
            case 402: // out n,a
                o2 = bus.peekb(address++);
                currentMnemonic = MessageFormat.format(mnem(o1), toHex2(o2));
                currentComment = toNameO(o2);
                break;
            case 3:
                o2 = bus.peekb(address++);
                o3 = bus.peekb(address++);
                ad = (o3 << 8) + o2;
                currentMnemonic = MessageFormat.format(mnem(o1), toHex4(ad));
                break;
            case 103:
                o2 = bus.peekb(address++);
                o3 = bus.peekb(address++);
                ad = (o3 << 8) + o2;
                currentMnemonic = MessageFormat.format(mnem(o1), toHex4(ad));
                currentComment = toName(ad);
                break;
            }
        }

if (currentMnemonic == null) {
 Debug.println("???: " + toHex2(o1) + ", " + toHex2(o2));
}
        return address;
    }

    /** */
    private static String toHex2(int value) { return String.format("%02x", value); }
    /** */
    private static String toHex4(int value) { return String.format("%04x", value); }

    /** */
    private static String toName(int value) {
        String key = toHex4(value);
        if (names.containsKey(key)) {
            return names.getProperty(key);
        } else {
            return null;
        }
    }

    /** */
    private static String toNameI(int value) {
        String key = toHex2(value);
        if (inportNames.containsKey(key)) {
            return inportNames.getProperty(key);
        } else {
            return null;
        }
    }

    /** */
    private static String toNameO(int value) {
        String key = toHex2(value);
        if (outportNames.containsKey(key)) {
            return outportNames.getProperty(key);
        } else {
            return null;
        }
    }

    /** */
    private static String mnem1(int p) { return table_cb[p].mnemonic; }
    private static String mnem2(int p) { return table_ID[p].mnemonic; }
    private static String mnem3(int p) { return table_IDcb[p].mnemonic; }
    private static String mnem4(int p) { return table_ed[p].mnemonic; }
    private static String mnem(int p) { return table_base[p].mnemonic; }
    private static int type(int p) { return table_base[p].type; }
    /** CB xx */
    private static int type1(int p) { return table_cb[p].type; }
    /** DD/FD xx */
    private static int type2(int p) { return table_ID[p].type; }
    /** DD/FD CB xx */
    private static int type3(int p) { return table_IDcb[p].type; }
    /** ED xx */
    private static int type4(int p) { return table_ed[p].type; }

    /** */
    private static class Entry {
        Entry(int type, int length, String mnemonic) {
            this.type = type;
            this.length = length;
            this.mnemonic = mnemonic;
        }
        int type;
        int length;
        String mnemonic;
    }

    /** 00 ~ ff */
    private static final Entry[] table_base = new Entry[256];
    /** CB xx */
    private static final Entry[] table_cb = new Entry[256];
    /** DD/FD xx */
    private static final Entry[] table_ID = new Entry[256];
    /** DD/FD CB b xx */
    private static final Entry[] table_IDcb = new Entry[256];
    /** ED xx */
    private static final Entry[] table_ed = new Entry[256];

    //-------------------------------------------------------------------------

    /** */
    private static Properties names = new Properties();

    /** */
    private static Properties outportNames = new Properties();

    /** */
    private static Properties inportNames = new Properties();

    /* load properties */
    static {
        try {
            Class<?> clazz = Debugger.class;

            // instructions
            final String path1 = "/disassembler.properties";
            Properties props = new Properties();
            props.load(clazz.getResourceAsStream(path1));

            for (int i = 0; i < 256; i++) {
                // base
                setEntry(i, "base", table_base, props);
                // cb
                setEntry(i, "cb", table_cb, props);
                // ID
                setEntry(i, "ID", table_ID, props);
                // ID cb
                setEntry(i, "IDcb", table_IDcb, props);
                // ed
                setEntry(i, "ed", table_ed, props);
            }

            // names
            final String path2 = "/address.properties";
            names.load(clazz.getResourceAsStream(path2));

            final String path3 = "/inport.properties";
            inportNames.load(clazz.getResourceAsStream(path3));

            final String path4 = "/outport.properties";
            outportNames.load(clazz.getResourceAsStream(path4));
        } catch (Exception e) {
Debug.printStackTrace(e);
            throw new IllegalStateException(e);
        }
    }

    /** */
    private static void setEntry(int i,
                                 String category,
                                 Entry[] table,
                                 Properties props) {

        String keyBase = "instruction." + category + "." + i + ".";
        String key = keyBase + "mnemonic";
//try {
        String mnemonic = props.getProperty(key);
        key = keyBase + "length";
        int length = Integer.parseInt(props.getProperty(key));
        key = keyBase + "type";
        int type = Integer.parseInt(props.getProperty(key));

        Entry entry = new Entry(type, length, mnemonic);
        table[i] = entry;
//} catch (RuntimeException e) {
// Debug.println(key);
// throw e;
//}
    }

    /**
     * run disassembler
     *
     * @param args 0: file, 1: start address, 2: bytes, 3: offset
     */
    public static void main(String[] args) throws IOException {
        Bus bus = new Bus.SimpleBus();

        int offset = 0;
        if (args.length > 3) {
            offset = Integer.parseInt(args[3], 16);
        }

        Path file = Paths.get(args[0]);

        int start = 0;
        if (args.length > 1) {
            start = Integer.parseInt(args[1], 16);
        }
        int bytes = 0x10000;
        if (args.length > 2) {
            bytes = Integer.parseInt(args[2], 16);
        }

        byte[] data = Files.readAllBytes(file);
        bus.pokes(start, Arrays.copyOfRange(data, offset, offset + bytes));

        Disassembler da = new Disassembler();
        da.setBus(bus);
Debug.printf("%04x, %08x", start, bytes);
        int pc = start;
        while (pc - start < bytes) {
            int next = da.execute(pc);

            System.out.printf("%04x ", pc);
            for (int i = 0; i < 4; i++) {
                if (i < next - pc) {
                    System.out.printf("%02x", bus.peekb(pc + i));
                } else {
                    System.out.print("  ");
                }
            }
            System.out.print("\t" + da.getCurrentMnemonic());
            if (da.getCurrentComment() != null) {
                System.out.println("\t\t; " + da.getCurrentComment());
            } else {
                System.out.println();
            }

            pc = next;
        }
    }
}

/* */
