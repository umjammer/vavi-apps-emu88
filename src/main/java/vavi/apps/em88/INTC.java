/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.em88;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Timer;
import java.util.TimerTask;


/**
 * INTC.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 031230 nsano initial version <br>
 */
class INTC implements Device {

    private static final Logger logger = System.getLogger(INTC.class.getName());

    /** bus emulation */
    private Z80 z80;

    /** bus emulation */
    private Bus bus;

    /** emulation connect bus */
    public void setBus(Bus bus) {
        this.bus = bus;

        this.z80 = (Z80) bus.getDevice(Z80.class.getName());

        // vrtc - IRQ 1
        if (timers[1] != null) {
            timers[1].cancel();
        }
        timers[1] = new Timer("VRTC-Timer", true);
        // Toggle every 8ms -> 16ms period -> ~62.5Hz
        timers[1].schedule(new VrtcTimerTask(), 0, 8);

        // IRQ 1 is for FDC, it should be triggered by the FDC device, not a fixed timer.
//        timers[1] = new Timer();
//        timers[1].schedule(new IntcTimerTask(1), 0, 160);
    }

    // ----

    /** */
    private boolean sgs_;

    /** */
    private int level;

    /** 0xe4 */
    public void setRegister(int data) {
        this.sgs_ = (data & 0x08) != 0;
        this.level = data & 0x07;
        //logger.log(Level.TRACE, "sgs_: " + sgs_ + ", level: " + level);
    }

    /** */
    private int mask = 0xff; // Default all interrupts to unmasked

    /** 0xe6 */
    /** 0xe6 */
    /** 0xe6 */
    public void setMask(int data) {
        if ((data & 0x01) != 0) {
            mask |= (0x01 << 2);
        } else {
            mask &= ~(0x01 << 2);
        }
        if ((data & 0x02) != 0) {
            mask |= (0x01 << 1);
        } else {
            mask &= ~(0x01 << 1);
        }
        if ((data & 0x04) != 0) {
            mask |= (0x01 << 0);
        } else {
            mask &= ~(0x01 << 0);
        }
//logger.log(Level.TRACE, "mask: %02x".formatted(mask, StringUtil.toBits(mask)));
    }

    /** */
    private int channel;

    /** */
    private int irff = 0xff;

    /** */
    public void requestInterrupt(int channel) {
        if (channel == 2) {
             // System.err.printf("INTC: Req Ch%d Mask=%02x SGS=%b Lvl=%d IRFF=%02x\n", channel, mask, sgs_, level, irff);
        }

        if ((mask & (0x01 << channel)) == 0) {
            if (channel == 2) logger.log(Level.DEBUG, "INTC: Ch2 Blocked by Mask");
            return;
        }

        if (sgs_) { // Interrupts occur based on priority only
            for (int i = 0; i < channel; i++) {
                if ((irff & (0x01 << i)) == 0) {
                    if (channel == 2) logger.log(Level.DEBUG, "INTC: Ch2 Blocked by Higher Priority Ch" + i);
                    return;
                }
            }
        } else { // Compare with interrupt level and generate interrupt
            if (channel > level) {
                if (channel == 2) logger.log(Level.DEBUG, "INTC: Ch2 Blocked by Level (" + level + ")");
                return;
            }
        }

        if ((irff & (0x01 << channel)) != 0) {
            this.channel = channel & 0x07;
            irff &= ~(0x01 << channel);
            if (channel != 0) logger.log(Level.DEBUG, "INTC: Firing Z80 Interrupt for Ch" + channel);
            z80.requestInterrupt();
        } else {
             if (channel == 2) logger.log(Level.DEBUG, "INTC: Ch2 Already Pending (IRFF Bit 0)");
        }
    }

    /** */
    public void acknowledgeInterrupt() {
        if (channel != 0) logger.log(Level.DEBUG, "INTC: Ack Ch" + channel);
        irff |= (0x01 << channel);
    }

    /** */
    public int getOffsetAddress() {
        return channel * 2;
    }

    // ----

    /** */
    private volatile boolean vrtc;

    /** */
    public boolean getVrtc() {
        return vrtc;
    }

    /** */
    private Timer[] timers = new Timer[8];

    /** */
    private class VrtcTimerTask extends TimerTask {
        public void run() {
            vrtc = !vrtc;
            if (vrtc) { // Generate interrupt on the rising edge of the VRTC signal
                // This is the proper way to handle the interrupt wait flag.
                // The real ISR would do this, but we do it here directly
                // to ensure the wait loop is broken.
                if (bus != null) {
                    // int currentValue = bus.peekb(0xef3e);
                    // if (currentValue > 0) {
                    //    bus.pokeb(0xef3e, currentValue - 1);
                    // }
                }
                requestInterrupt(1); // IRQ 1 is the VRTC interrupt
            }
        }
    }

    /** */
    private class IntcTimerTask extends TimerTask {
        int channel;

        IntcTimerTask(int channel) {
            this.channel = channel;
        }

        public void run() {

            // if (channel == 1) { vrtc = true; }

            requestInterrupt(channel);

            // Thread.yield();

            // if (channel == 1) { vrtc = false; }
        }
    }
}
