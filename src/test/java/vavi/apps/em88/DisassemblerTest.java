/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.em88;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import vavi.util.Debug;


/**
 * ZexallTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2003-12-28 nsano initial version <br>
 */
class DisassemblerTest {

    /**
     * run disassembler
     *
     * @param args 0: file, 1: start address, 2: bytes, 3: offset
     */
    public static void main(String[] args) throws IOException {
        Bus bus = new SimpleBus();

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