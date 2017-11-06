/*
 * Copyright (c) 2008 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import vavi.apps.em88.Bus;
import vavi.apps.em88.Debugger;
import vavi.apps.em88.Z80;


/**
 * Test1. 
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 080414 nsano initial version <br>
 */
public class Test1 {

    /** */
    private static Bus testBus = new Bus() {

        byte[] ram = new byte[0x10000];

        /** */
        protected final Mapping getMapping(int address, Direction direction) {
            Mapping mapping = new Mapping();
            mapping.base = ram;
            mapping.pointer = address;
            return mapping;
        }

        /** */
        public int inp(int p) {
            return 0;
        }

        /** */
        public void outp(int p, int d) {
        }

        /* */ {
            try {
                int start = 0x0000;
                String file = "/asm.out";
                InputStream is = new BufferedInputStream(new FileInputStream(file));
                byte[] b = new byte[8192];
                int o = 0;
                is.skip(10);
                while (is.available() > 0) {
                    int l = is.read(b, 0, b.length);
                    System.arraycopy(b, 0, ram, start + o, l);
                    o += l;
                }
                is.close();
System.err.printf("%X - %X read\n", start, o);
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    };

    /** */
    void test2() {
//        Debug.println(xor8bit(0, 0));
    }

    /** */
    void test() {
        for (int i = -512; i < 512; i++) {
//          Debug.println(i + ", " + (byte) i + ", " + index(0, i));
        }
    }

    /** */
    public static void main(String[] args) {
        Z80 z80 = new Z80();
//      test();

        testBus.addDevice(z80);
        testBus.reset();
        Debugger debugger = new Debugger();
        debugger.setBus(testBus);
        debugger.execute();

        System.exit(0);
    }
}

/* */
