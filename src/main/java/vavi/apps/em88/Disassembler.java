/*
 * Copyright (c) 1993-2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.em88;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Properties;

import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * Z80 disassembler.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
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
        case 0xcb:	// CB xx
            o2 = bus.peekb(address++);
            currentMnemonic = mnem1(o2);
            break;
        case 0xdd:	// DD xx
        case 0xfd:	// FD xx
            id = (o1 == 0xdd) ? "IX" : "IY";
            o2 = bus.peekb(address++);
            switch (o2) {
            case 0x21:	// 21: ld ID,nn
                o3 = bus.peekb(address++);
                o4 = bus.peekb(address++);
                ad = (o4 << 8) + o3;
                currentMnemonic = MessageFormat.format(mnem2(o2), id, toHex4(ad));
                break;
            case 0x22:	// 34: ld (nn),ID
                o3 = bus.peekb(address++);
                o4 = bus.peekb(address++);
                ad = (o4 << 8) + o3;
                currentMnemonic = MessageFormat.format(mnem2(o2), toHex4(ad), id);
                currentComment = toName(ad);
                break;
            case 0x29:	// 41: add ID,ID
                currentMnemonic = MessageFormat.format(mnem2(o2), id, id);
                break;
            case 0x2a:	// 42: ld ID,(nn)
                o3 = bus.peekb(address++);
                o4 = bus.peekb(address++);
                ad = (o4 << 8) + o3;
                currentMnemonic = MessageFormat.format(mnem2(o2), id, toHex4(ad));
                currentComment = toName(ad);
                break;
            case 0x36:	// 54: foo (ID + d), n
                o3 = bus.peekb(address++);		// d
                o4 = bus.peekb(address++);		// n
                currentMnemonic = MessageFormat.format(mnem2(o2), id, toHex2(o3), toHex2(o4));
                break;
            case 0xcb:	// ID cb d xx
                o3 = bus.peekb(address++);		// d
                o4 = bus.peekb(address++);		// xx
                currentMnemonic = MessageFormat.format(mnem3(o4), id, toHex2(o3));
                break;
            default:
                switch (type2(o2)) {
                case 2:
                    currentMnemonic = MessageFormat.format(mnem2(o2), id);
                    break;
                case 3:	// ID + d
                    o3 = bus.peekb(address++);
                    currentMnemonic = MessageFormat.format(mnem2(o2), id, toHex2(o3));
                    break;
                }
                break;
            }
            break;
        case 0xed:	// ED xx
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
        default:	// base 0x00 ~ 0xff
            switch (type(o1)) {
            case 1:
                currentMnemonic = mnem(o1);
                break;
            case 2:
                o2 = bus.peekb(address++);
                currentMnemonic = MessageFormat.format(mnem(o1), toHex2(o2));
                break;
            case 202:	// jr, djnz
                o2 = bus.peekb(address++);
                currentMnemonic = MessageFormat.format(mnem(o1), toHex2(o2));
                currentComment = toHex4(address + (byte) o2);
                break;
            case 302:	// in a,n
                o2 = bus.peekb(address++);
                currentMnemonic = MessageFormat.format(mnem(o1), toHex2(o2));
                currentComment = toNameI(o2);
                break;
            case 402:	// out n,a
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
    private final String toHex2(int value) { return StringUtil.toHex2(value); }
    /** */
    private final String toHex4(int value) { return StringUtil.toHex4(value); }

    /** */
    private final String toName(int value) {
        String key = StringUtil.toHex4(value);
        if (names.containsKey(key)) {
            return names.getProperty(key);
        } else {
            return null;
        }
    }

    /** */
    private final String toNameI(int value) {
        String key = StringUtil.toHex2(value);
        if (inportNames.containsKey(key)) {
            return inportNames.getProperty(key);
        } else {
            return null;
        }
    }

    /** */
    private final String toNameO(int value) {
        String key = StringUtil.toHex2(value);
        if (outportNames.containsKey(key)) {
            return outportNames.getProperty(key);
        } else {
            return null;
        }
    }

    /** */
    private final String mnem1(int p) { return table_cb[p].mnemonic; }
    private final String mnem2(int p) { return table_ID[p].mnemonic; }
    private final String mnem3(int p) { return table_IDcb[p].mnemonic; }
    private final String mnem4(int p) { return table_ed[p].mnemonic; }
    private final String mnem(int p) { return table_base[p].mnemonic; }
    private final int type(int p) { return table_base[p].type; }
    /** CB xx */
//  private final int type1(int p) { return table_cb[p].type; }
    /** DD/FD xx */
    private final int type2(int p) { return table_ID[p].type; }
    /** DD/FD CB xx */
//  private final int type3(int p) { return table_IDcb[p].type; }
    /** ED xx */
    private final int type4(int p) { return table_ed[p].type; }
    
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

    /** */
    static {
        try {
            Class<?> clazz = Debugger.class;

            // instructions
            final String path1 = "disassembler.properties";
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
            final String path2 = "address.properties";
            names.load(clazz.getResourceAsStream(path2));

            final String path3 = "inport.properties";
            inportNames.load(clazz.getResourceAsStream(path3));

            final String path4 = "outport.properties";
            outportNames.load(clazz.getResourceAsStream(path4));
        } catch (Exception e) {
Debug.printStackTrace(e);
            System.exit(1);
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
//  } catch (RuntimeException e) {
//   Debug.println(key);
//   throw e;
//  }
    }

    /** */
    public static void main(String[] args) throws IOException {
        final byte[] ram = new byte[0x10000];
        Bus bus = new Bus() {
            protected Mapping getMapping(int a, Direction direction) {
                Mapping address = new Mapping();
                address.base = ram;
                address.pointer = a;
                return address;
            }
            public int inp(int p) { return -1; }
            public void outp(int p, int d) {}
        };

        File file = new File(args[0]);
        int length = (int) file.length();
        InputStream is = new FileInputStream(file);
        int l = 0;
        while (l < length) {
            l += is.read(ram, l, length - l);
        }
        is.close();

        int start = 0;
        if (args.length > 1) {
            start = Integer.parseInt(args[1], 16);
        }
        int bytes = 0x10000;
        if (args.length > 2) {
            bytes = Integer.parseInt(args[2], 16);
        }

        Disassembler da = new Disassembler();
        da.setBus(bus);
Debug.println(StringUtil.toHex4(start) + ", " + StringUtil.toHex8(bytes));
        int pc = start;
        while (pc - start < bytes) {
            int next = da.execute(pc);

            System.out.print(StringUtil.toHex4(pc) + " ");
            for (int i = 0; i < 4; i++) {
                if (i < next - pc) {
                    System.out.print(StringUtil.toHex2(bus.peekb(pc + i)));
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
