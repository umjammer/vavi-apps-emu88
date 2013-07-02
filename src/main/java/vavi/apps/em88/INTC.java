/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.em88;

import java.util.Timer;
import java.util.TimerTask;


/**
 * INTC.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 031230 nsano initial version <br>
 */
class INTC implements Device {

    /** bus emulation */
    private Z80 z80;

    /** bus emulation */
    private Bus bus;

    /** emulation connect bus */
    public void setBus(Bus bus) {
        this.bus = bus;

        this.z80 = (Z80) bus.getDevice(Z80.class.getName());

        // vrtc
        timers[0] = new Timer();
        timers[0].schedule(new VrtcTimerTask(), 0, 60);

        timers[1] = new Timer();
        timers[1].schedule(new IntcTimerTask(1), 0, 160);
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
        // Debug.println("sgs_: " + sgs_ + ", level: " + level);
    }

    /** */
    private int mask = 0x00;

    /** 0xe6 */
    public void setMask(int data) {
        if ((data & 0x01) == 0) {
            mask &= ~(0x01 << 2);
        } else {
            mask |= (0x01 << 2);
        }
        if ((data & 0x02) == 0) {
            mask &= ~(0x01 << 1);
        } else {
            mask |= (0x01 << 1);
        }
        if ((data & 0x04) == 0) {
            mask &= ~(0x01 << 0);
        } else {
            mask |= (0x01 << 0);
        }
        // Debug.println("mask: " + StringUtil.toHex2(mask) + " " + StringUtil.toBits(mask));
    }

    /** */
    private int channel;

    /** */
    private int irff = 0xff;

    /** */
    public void requestInterrupt(int channel) {
        if ((mask & (0x01 << channel)) == 0) {
            return;
        }

        if (sgs_) { // 優先順位のみによる割り込み発生
            for (int i = 0; i < channel; i++) {
                if ((irff & (0x01 << i)) == 0) {
                    return;
                }
            }
        } else { // インタラプトレベルと比較し割り込み発生
            if (channel > level) {
                return;
            }
        }

        if ((irff & (0x01 << channel)) != 0) {
            this.channel = channel & 0x07;
            irff &= ~(0x01 << channel);
            z80.requestInterrupt();
        }
    }

    /** */
    public void acknowledgeInterrupt() {
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
        }
    };

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
    };
}

/* */
