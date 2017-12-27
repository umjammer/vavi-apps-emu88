/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.em88;

import java.util.Timer;
import java.util.TimerTask;
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
    private int[] addresses = new int[3];
    /** */
    private int[] counts = new int[3];
    /** */
    private int[] modes = new int[3];

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
    private Timer[] timers = new Timer[4];

    /** */
    public void setMode(int mode) {
        this.mode = mode;

        for (int i = 0; i < 4; i++) {
            boolean enabled = (mode & (0x01 << i)) != 0;

            if (enabled) {
//                  timers[i] = new Timer();
//                  timers[i].schedule(new DmaTimerTask(i), 0, 333);
Debug.println("channel " + i + " start: " + StringUtil.toHex4(addresses[i]) + ", " + StringUtil.toHex4(counts[i]));
            } else if (timers[i] != null) {
//                  timers[i].cancel();
//                  timers[i] = null;
Debug.println("channel " + i + " stop");
            }
        }
    }

    /** */
    public int getStatus() {
        return status;
    }

    //----

    /** */
    private class DmaTimerTask extends TimerTask {
        int channel;
        DmaTimerTask(int channel) {
            this.channel = channel;
        }
        public void run() {
//Debug.println(StringUtil.toHex4(addresses[channel]) + ", " + StringUtil.toHex4(counts[channel]));
            for (int i = 0; i <= counts[channel]; i++) {
                graphic.pokeb(i, bus.peekb(addresses[channel] + i));
            }
            graphic.repaint();
        }
    };
}

/* */
