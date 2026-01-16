/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.em88;

/**
 * vavi.apps.em88.SimpleBus.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-02-18 nsano initial version <br>
 */
public class SimpleBus extends Bus {

    final byte[] ram = new byte[0x10000];
    final byte[] io = new byte[0x100];
    Mapping address = new Mapping();

    {
        address.base = ram;
    }

    @Override
    protected Mapping getMapping(int a, Direction direction) {
        address.pointer = a;
        return address;
    }

    @Override
    public int inp(int p) {
        return io[p & 0xff] & 0xff;
    }

    @Override
    public void outp(int p, int d) {
        io[p & 0xff] = (byte) (d & 0xff);
    }
}
