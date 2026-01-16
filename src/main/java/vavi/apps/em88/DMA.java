/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.em88;

import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * DMA Controller.
 *
 * <pre>
 *  ch 0:    5 inch DMA type disk unit
 *  ch 1:    8 inch disk unit
 *  ch 2:    CRTC
 *  ch 3:
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 040102 nsano initial version <br>
 */
class DMA implements Device {

    /** */
    private Graphic graphic;

    /** */
    private Bus bus;

    /** */
    public void setBus(Bus bus) {
        this.bus = bus;

        this.graphic = (Graphic) bus.getDevice(Graphic.class.getName());
    }

    //----

    /** */
    private int mode;

    /** */
    private int status;

    /** */
    private final int[] addresses = new int[4];
    /** */
    private final int[] counts = new int[4];
    /** */
    private final int[] modes = new int[4];

    private static final int MODE_VERIFY = 0;
    private static final int MODE_READ = 2;
    private static final int MODE_WRITE = 1;

    /** DMAC F/L */
    private boolean fl = false;

    /** */
    public void setAddress(int channel, int address) {
        if (!fl) {
            addresses[channel] = address;
            fl = true;
        } else {
            addresses[channel] |= (address << 8);
            fl = false;
        }
    }

    /** */
    public int getAddress(int channel) {
        return addresses[channel];
    }

    /** */
    public void setTerminalCount(int channel, int count) {
        if (!fl) {
            counts[channel] = count;
            fl = true;
        } else {
            counts[channel] |= ((count & 0x3f) << 8);
            modes [channel]  = ((count & 0xc0) >> 2);
            fl = false;
        }
    }

    /** */
    public int getTerminalCount(int channel) {
        return counts[channel];
    }

    /** */
    public void setMode(int mode) {
        this.mode = mode;

        for (int i = 0; i < 4; i++) {
            boolean enabled = (mode & (0x01 << i)) != 0;

            if (enabled) {
                if (i == 2) { // CRTC
                    for (int j = 0; j <= counts[i]; j++) {
                        graphic.pokeb(j, bus.peekb(addresses[i] + j));
                    }
                    graphic.repaint();
                }
//logger.log(Level.TRACE, "channel " + i + " start: " + StringUtil.toHex4(addresses[i]) + ", " + StringUtil.toHex4(counts[i]));
            }
        }
    }

    /** */
    public int getStatus() {
        return status;
    }
}
