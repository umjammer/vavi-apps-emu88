/*
 * Copyright (c) 1993-2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.em88;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * Z80 Emulator.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 931205 nsano original 8080a instructions only <br>
 *          0.10 931208 nsano add Z80 instructions <br>
 *          1.00 031228 nsano java porting <br>
 *          1.10 040101 nsano see jasper Z80 <br>
 */
public class Z80 implements Device {

    // Z80 register emulation

    // 8bit registers
    private int a, f, b, c, h, l, d, e, r, i;
    private int af2, bc2, hl2, de2;

    // 16bit registers
    private int sp;
    private int pc;
    private int ix;
    private int iy;

    // interrupt flip-flops
    private boolean iff1 = true;
    private boolean iff2 = true;

    // interrupt mode 0, 1, 2
    private int im = 2;

    /** */
    private int r7;

    // flag position
    private static final int FBIT_C = 0x01;
    private static final int FBIT_N = 0x02;
    private static final int FBIT_P = 0x04;
    private static final int FBIT_3 = 0x08;
    private static final int FBIT_H = 0x10;
    private static final int FBIT_5 = 0x20;
    private static final int FBIT_Z = 0x40;
    private static final int FBIT_S = 0x80;

    // flags
    private boolean fc;
    private boolean fn;
    private boolean fp; // p/v
    private boolean f3;
    private boolean fh;
    private boolean f5;
    private boolean fz;
    private boolean fs;

    /** */
    private final void assembleFlags() {
        f = 0;

        if (fc) {
            f |= FBIT_C;
        }
        if (fn) {
            f |= FBIT_N;
        }
        if (fp) {
            f |= FBIT_P;
        }
        if (f3) {
            f |= FBIT_3;
        }
        if (fh) {
            f |= FBIT_H;
        }
        if (f5) {
            f |= FBIT_5;
        }
        if (fz) {
            f |= FBIT_Z;
        }
        if (fs) {
            f |= FBIT_S;
        }
    }

    /** */
    private final void extractFlags() {
        fc = (f & FBIT_C) != 0;
        fn = (f & FBIT_N) != 0;
        fp = (f & FBIT_P) != 0;
        f3 = (f & FBIT_3) != 0;
        fh = (f & FBIT_H) != 0;
        f5 = (f & FBIT_5) != 0;
        fz = (f & FBIT_Z) != 0;
        fs = (f & FBIT_S) != 0;
    }

    // 16bit operations
    public final int getAF() {
        return (a << 8) | getF();
    }

    public final void setAF(int af) {
        a = (af >> 8) & 0xff;
        setF(af);
    }

    public final int getHL() {
        return (h << 8) | l;
    }

    public final void setHL(int hl) {
        h = (hl >> 8) & 0xff;
        l = hl & 0xff;
    }

    public final int getBC() {
        return (b << 8) | c;
    }

    public final void setBC(int bc) {
        b = (bc >> 8) & 0xff;
        c = bc & 0xff;
    }

    public final int getDE() {
        return (d << 8) | e;
    }

    public final void setDE(int de) {
        d = (de >> 8) & 0xff;
        e = de & 0xff;
    }

    public final int getIR() {
        return (i << 8) | r;
    }

    public final int getSP() {
        return sp;
    }

    public final void setSP(int sp) {
        this.sp = sp;
    }

    public final int getPC() {
        return pc;
    }

    public final void setPC(int pc) {
        this.pc = pc;
    }

    public final int getIX() {
        return ix;
    }

    public final int getIY() {
        return iy;
    }

    // 8bit operations
    public final int getF() {
        assembleFlags();
        return f;
    }

    public final void setF(int f) {
        this.f = f & 0xff;
        extractFlags();
    }

    public int getA() {
        return a;
    }

    public void setA(int a) {
        this.a = a;
    }

    public int getH() {
        return h;
    }

    public int getL() {
        return l;
    }

    public int getB() {
        return b;
    }

    public int getC() {
        return c;
    }

    public int getD() {
        return d;
    }

    public int getE() {
        return e;
    }

    /** Interrupt modes/register */
    public int getI() {
        return i;
    }

    /** Memory refresh register */
    public int getR() {
        return r;
    }

    // flag operations
    boolean isC() {
        return fc;
    }

    boolean isN() {
        return fn;
    }

    boolean isP() {
        return fp;
    }

    boolean isV() {
        return fp;
    }

    boolean isH() {
        return fh;
    }

    boolean isZ() {
        return fz;
    }

    boolean isS() {
        return fs;
    }

    // -------------------------------------------------------------------------

    /** */
    private JPanel panel;

    /** */ {
        //
        panel = new JPanel() {
            public void paint(Graphics g) {

                super.paint(g);

                int y1 = 10;
                int y2 = 10;
                g.setColor(Color.green);
                g.drawString("PC=" + StringUtil.toHex4(getPC()), 10, y1 += 20);
                g.drawString(" A=" + StringUtil.toHex2(a), 10, y1 += 20);
                g.drawString("IM=" + im, 100, y2 += 20);
                g.drawString("BC=" + StringUtil.toHex4(getBC()), 10, y1 += 20);
                g.drawString("iff1=" + iff1, 100, y2 += 20);
                g.drawString("DE=" + StringUtil.toHex4(getDE()), 10, y1 += 20);
                g.drawString("iff2=" + iff2, 100, y2 += 20);
                g.drawString("HL=" + StringUtil.toHex4(getHL()), 10, y1 += 20);
                g.drawString("intr=" + interrupted, 100, y2 += 20);
                g.drawString("IR=" + StringUtil.toHex4(getIR()), 10, y1 += 20);
                g.drawString("SP=" + StringUtil.toHex4(sp), 10, y1 += 20);
                g.drawString("IX=" + StringUtil.toHex4(ix), 10, y1 += 20);
                g.drawString("IY=" + StringUtil.toHex4(iy), 10, y1 += 20);
                g.drawString(" C:" + (fc ? 1 : 0), 10, y1 += 20);
                g.drawString(" N:" + (fn ? 1 : 0), 10, y1 += 20);
                g.drawString(" P:" + (fp ? 1 : 0), 10, y1 += 20);
                g.drawString(" H:" + (fh ? 1 : 0), 10, y1 += 20);
                g.drawString(" Z:" + (fz ? 1 : 0), 10, y1 += 20);
                g.drawString(" S:" + (fs ? 1 : 0), 10, y1 += 20);

                if (bus == null) {
                    return;
                }
                for (int i = 0; i < 6 && sp + 2 * i < 0x10000; i++) {
                    g.drawString(StringUtil.toHex4(sp + 2 * i) + ": " + StringUtil.toHex4(bus.peekw(sp + 2 * i)), 100, y2 += 20);
                }
                for (int i = 0; i < 12; i++) {
                    g.drawString(StringUtil.toHex2(i) + ": " + StringUtil.toBits(bus.inp(i)), 10, y1 += 20);
                }
            }
        };
        panel.setPreferredSize(new Dimension(200, 300));
        panel.setOpaque(true);
        panel.setBackground(Color.black);

        JDialog dialog = new JDialog();
        dialog.getContentPane().add(panel);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setTitle("Emu88");
        dialog.setLocation(650, 0);
        dialog.pack();
        dialog.setVisible(true);

        //
        JButton button = new JButton();
        button.setAction(new AbstractAction("Break") {
            public void actionPerformed(ActionEvent ev) {
                broken = true;
            }
        });
        button.setPreferredSize(new Dimension(60, 20));

        JDialog controller = new JDialog();
        controller.getContentPane().add(button);
        controller.setTitle("Controller");
        controller.setLocation(860, 0);
        controller.pack();
        controller.setVisible(true);
    }

    // -------------------------------------------------------------------------

    /** 鐃緒申PD82xx */
    private INTC intc;

    /** bus emulation */
    private Bus bus;

    /** emulation connect bus */
    public void setBus(Bus bus) {
        this.bus = bus;

        this.intc = (INTC) bus.getDevice(INTC.class.getName());
    }

    /** */
    private volatile boolean broken = false;

    /** */
    public boolean isUserBroken() {
        return broken;
    }

    /** */
    private volatile boolean interrupted = false;

    /** */
    private volatile boolean nmi = false;

    /** */
    public void requestInterrupt() {
        interrupted = true;
        nmi = false;
    }

    /** */
    public void requestNonMaskableInterrupt() {
        interrupted = true;
        nmi = true;
    }

    /** */
    private int cost;

    /** emulation z80 fetch, decode, execute */
    public void execute(int address) {
        execute(address, 0);
    }

    /** emulation z80 fetch, decode, execute, debug mode */
    public int execute(int address, int steps) {

        pc = address;

        broken = false;

        if (steps > 0) {
            for (int c = 0; c < steps && !broken; c++) {
                exec();
                processInterrupt();
panel.repaint();
            }
        } else {
            while (!broken) {
                exec();
                processInterrupt();
panel.repaint();
            }
        }

        return pc;
    }

    /** */
    private void processInterrupt() {
        if (interrupted) {
            intc.acknowledgeInterrupt();
            if (iff1 == true) {
                if (nmi) {
                    interruptNonMaskable();
                } else {
                    interrupt();
                }
            }
            interrupted = false;
            nmi = false;
        }
    }

    /**
     * fetch a byte indicated by pc address.
     * pc will be incremented one.
     */
    private final int fetchb() {
        int v = bus.peekb(pc);
        pc = add16bitInternal(pc, 1);
        return v;
    }

    /**
     * fetch a word indicated by pc address.
     * pc will be incremented two.
     */
    private final int fetchw() {
        int v = bus.peekw(pc);
        pc = add16bitInternal(pc, 2);
        return v;
    }

    // -------------------------------------------------------------------------

    private final boolean isZero(int v) {
        return v == 0;
    }

    private final boolean isSign(int v) {
        return (v & 0x80) != 0;
    }

    private final boolean isSign16(int v) {
        return (v & 0x8000) != 0;
    }

    private final boolean isCarry(int v) {
        return (v & 0x100) != 0;
    }

    private final boolean isHalfC(int v) {
        return (v & 0x10) != 0;
    }

    private final boolean isCarry16(int v) {
        return (v & 0x10000) != 0;
    }

    private final boolean isParity(int v) {
        return parity_tbl[v];
    }

    /** */
    private static final boolean[] parity_tbl = {
        true, false, false, true, false, true, true, false, false, true, true, false, true, false, false, true,
        false, true, true, false, true, false, false, true, true, false, false, true, false, true, true, false,
        false, true, true, false, true, false, false, true, true, false, false, true, false, true, true, false,
        true, false, false, true, false, true, true, false, false, true, true, false, true, false, false, true,
        false, true, true, false, true, false, false, true, true, false, false, true, false, true, true, false,
        true, false, false, true, false, true, true, false, false, true, true, false, true, false, false, true,
        true, false, false, true, false, true, true, false, false, true, true, false, true, false, false, true,
        false, true, true, false, true, false, false, true, true, false, false, true, false, true, true, false,
        false, true, true, false, true, false, false, true, true, false, false, true, false, true, true, false,
        true, false, false, true, false, true, true, false, false, true, true, false, true, false, false, true,
        true, false, false, true, false, true, true, false, false, true, true, false, true, false, false, true,
        false, true, true, false, true, false, false, true, true, false, false, true, false, true, true, false,
        true, false, false, true, false, true, true, false, false, true, true, false, true, false, false, true,
        false, true, true, false, true, false, false, true, true, false, false, true, false, true, true, false,
        false, true, true, false, true, false, false, true, true, false, false, true, false, true, true, false,
        true, false, false, true, false, true, true, false, false, true, true, false, true, false, false, true
    };

    /** */
    private final int add16bitInternal(int v, int o) {
        return (v + o) & 0xffff;
    }

    /** */
    private final int sub16bitInternal(int v, int o) {
        return (v - o) & 0xffff;
    }

    /** */
    private final int add8bitInternal(int v, int o) {
        return (v + o) & 0xff;
    }

    /** */
    private final int inc8bitInternal(int v) {
        return (v + 1) & 0xff;
    }

    /** */
    private final int dec8bitInternal(int v) {
        return (v - 1) & 0xff;
    }

    /** */
    private final void push(int v) {
        sp = sub16bitInternal(sp, 2);
        bus.pokew(sp, v);
// debug1();
    }

@SuppressWarnings("unused")
private void debug1() {
    String key = StringUtil.toHex4(bus.peekw(pc - 2));
    if (names.containsKey(key)) {
        Debug.println("call: " + key + ":" + names.getProperty(key));
    } else {
        Debug.println("push: " + key + ": ???");
    }
}

    /** */
    private final int pop() {
        int v = bus.peekw(sp);
        sp = add16bitInternal(sp, 2);
        return v;
    }

    /**
     * @param v used only lower 8bits (signed)
     */
    private final int index(int o, int v) {
        return add16bitInternal(o, (byte) v);
    }

    /** */
    private final void call() {
        int w = fetchw();
        push(pc);
        pc = w;
    }

    // -------------------------------------------------------------------------

    /** */
    private void interruptNonMaskable() {
        iff2 = iff1;
        iff1 = false;
        push(pc);
        pc = 0x66;

        cost = 11;
    }

    /** */
    private void interrupt() {

        switch (im) {
        case 0:
            exec(intc.getOffsetAddress());

            cost = 13;
            break;
        case 1:
            push(pc);
            iff1 = false;
            iff2 = false;
            pc = 0x38;

            cost = 13;
            break;
        case 2:
            push(pc);
            iff1 = false;
            iff2 = false;
// Debug.println("pc: " + StringUtil.toHex4(pc));
            pc = bus.peekw((i << 8) | intc.getOffsetAddress());
// Debug.println("pc: " + StringUtil.toHex4(pc) + ", " +
// StringUtil.toHex4((i << 8) | intc.getOffsetAddress()));
            cost = 19;
            break;
        }
    }

    // ----

    /** 8 bit add */
    private final int add8bit(int o1, int o2) {
        int w = o1 + o2;
        int v = w & 0xff;

        fp = ((o1 ^ ~o2) & (o1 ^ v) & 0x80) != 0;
        fh = isHalfC((o1 & 0x0f) + (o2 & 0x0f));
        fc = isCarry(w);
        fz = isZero(v);
        fs = isSign(v);
        fn = false;

        return v;
    }

    /** 8 bit adc */
    private final int adc8bit(int o1, int o2) {
        int cy = fc ? 1 : 0;
        int w = o1 + o2 + cy;
        int v = w & 0xff;

        fp = ((o1 ^ ~o2) & (o1 ^ v) & 0x80) != 0;
        fh = isHalfC((o1 & 0x0f) + (o2 & 0x0f) + cy);
        fc = isCarry(w);
        fz = isZero(v);
        fs = isSign(v);
        fn = false;

        return v;
    }

    /** 8 bit sub */
    private final int sub8bit(int o1, int o2) {
        int w = o1 - o2;
        int v = w & 0xff;

        fp = ((o1 ^ ~o2) & (o1 ^ v) & 0x80) != 0;
        fh = isHalfC((o1 & 0x0f) - (o2 & 0x0f));
        fc = isCarry(w);
        fz = isZero(v);
        fs = isSign(v);
        fn = true;

        return v;
    }

    /** 8 bit sbc */
    private final int sbc8bit(int o1, int o2) {
        int cy = fc ? 1 : 0;
        int w = o1 - o2 - cy;
        int v = w & 0xff;

        fp = ((o1 ^ ~o2) & (o1 ^ v) & 0x80) != 0;
        fh = isHalfC((o1 & 0x0f) - (o2 & 0x0f) - cy);
        fc = isCarry(w);
        fz = isZero(v);
        fs = isSign(v);
        fn = true;

        return v;
    }

    /** */
    private final int cmp8bit(int x, int y) {
        return sub8bit(x, y);
    }

    /** 8 bit increment */
    private final int inc8bit(int o) {
        int v = (o + 1) & 0xff;

        fh = isHalfC((o & 0x0f) + 1);
        fp = o == 0x7f;
        fz = isZero(v);
        fs = isSign(v);
        fn = false;

        return v;
    }

    /** 8 bit decrement */
    private final int dec8bit(int o) {
        int v = (o - 1) & 0xff;

        fh = isHalfC((o & 0x0f) - 1);
        fp = o == 0x80;
        fz = isZero(v);
        fs = isSign(v);
        fn = true;

        return v;
    }

    /** 8 bit and */
    private final int and8bit(int o1, int o2) {
        int v = o1 & o2;

        fp = isParity(v);
        fc = false;
        fh = true;
        fz = isZero(v);
        fs = isSign(v);
        fn = false;

        return v;
    }

    /** 8 bit or */
    private final int or8bit(int o1, int o2) {
        int v = o1 | o2;

        fp = isParity(v);
        fc = false;
        fh = false;
        fz = isZero(v);
        fs = isSign(v);
        fn = false;

        return v;
    }

    /** 8 bit xor */
    private final int xor8bit(int o1, int o2) {
        int v = (o1 ^ o2) & 0xff;

        fp = isParity(v);
        fc = false;
        fh = false;
        fz = isZero(v);
        fs = isSign(v);
        fn = false;

        return v;
    }

    /** 16 bit add */
    private final int add16bit(int o1, int o2) {
        int w = o1 + o2;
        int v = w & 0xffff;

        fc = isCarry16(w);
        fh = (((o1 & 0x0fff) + (o2 & 0x0fff)) & 0x1000) != 0;
        fn = false;

        return v;
    }

    /** 16 bit adc */
    private final int adc16bit(int o1, int o2) {
        int cy = fc ? 1 : 0;
        int w = o1 + o2 + cy;
        int v = w & 0xffff;

        fp = ((o1 ^ ~o2) & (o1 ^ v) & 0x8000) != 0;
        fh = (((o1 & 0x0fff) + (o2 & 0x0fff)) & 0x1000) != 0;
        fc = isCarry16(w);
        fz = isZero(v);
        fs = isSign16(v);
        fn = false;

        return v;
    }

    /** 16 bit sbc */
    private final int sbc16bit(int o1, int o2) {
        int cy = fc ? 1 : 0;
        int w = o1 - o2 - cy;
        int v = w & 0xffff;

        fp = ((o1 ^ ~o2) & (o1 ^ v) & 0x8000) != 0;
        fh = (((o1 & 0x0fff) + (o2 & 0x0fff)) & 0x1000) != 0;
        fc = isCarry16(w);
        fz = isZero(v);
        fs = isSign16(v);
        fn = true;

        return v;
    }

    /** rotate left circler */
    private final int rlc(int o, boolean na) {

        fh = false;
        fn = false;
        fc = (o & 0x80) != 0;

        int v = ((o << 1) | (fc ? 0x01 : 0x00)) & 0xff;

        if (na) {
            fp = isParity(v);
            fz = isZero(v);
            fs = isSign(v);
        }

        return v;
    }

    /** rotate right circler */
    private final int rrc(int o, boolean na) {

        fh = false;
        fn = false;
        fc = (o & 0x01) != 0;

        int v = (o >> 1) | (fc ? 0x80 : 0x00);

        if (na) {
            fp = isParity(v);
            fz = isZero(v);
            fs = isSign(v);
        }

        return v;
    }

    /** rotate left */
    private final int rl(int o, boolean na) {

        int cy = fc ? 0x01 : 0x00;

        fh = false;
        fn = false;
        fc = (o & 0x80) != 0;

        int v = ((o << 1) | cy) & 0xff;

        if (na) {
            fp = isParity(v);
            fz = isZero(v);
            fs = isSign(v);
        }

        return v;
    }

    /** rotate right */
    private final int rr(int o, boolean na) {

        int cy = fc ? 0x80 : 0x00;

        fh = false;
        fn = false;
        fc = (o & 0x01) != 0;

        int v = (o >> 1) | cy;

        if (na) {
            fp = isParity(v);
            fz = isZero(v);
            fs = isSign(v);
        }

        return v;
    }

    /** shift left arithmetic */
    private final int sla(int o) {

        fh = false;
        fn = false;
        fc = (o & 0x80) != 0;

        int v = (o << 1) & 0xff;

        fp = isParity(v);
        fz = isZero(v);
        fs = isSign(v);

        return v;
    }

    /** shift right arithmetic */
    private final int sra(int o) {

        int cy = o & 0x80;

        fh = false;
        fn = false;
        fc = (o & 0x01) != 0;

        int v = (o >> 1) | cy;

        fp = isParity(v);
        fz = isZero(v);
        fs = isSign(v);

        return v;
    }

    /** shift right logical */
    private final int srl(int o) {

        fh = false;
        fn = false;
        fc = (o & 0x01) != 0;

        int v = o >> 1;

        fp = isParity(v);
        fz = isZero(v);
        fs = isSign(v);

        return v;
    }

    /** */
    private static final int[] bit_tbl = {
        0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80
    };

    /**
     * test bit.
     * z,h,n flags will be changed
     */
    private final void bit(int n, int o) {
        fz = ((o & bit_tbl[n]) != 0) ? false : true;

        fh = true;
        fn = false;
    }

    /** set bit */
    private final int setbit(int n, int o) {
        return o | bit_tbl[n];
    }

    /** reset bit */
    private final int resetbit(int n, int o) {
        return o & ~bit_tbl[n];
    }

    /** bus.input port c */
    private final int inpc() {
        int v = bus.inp(getBC());

        fn = false;
        fp = isParity(v);
        fs = isSign(v);
        fz = isZero(v);
        fh = isHalfC(v);

        return r;
    }

    // ----

    /** for im 0 */
    private final void exec(int o) {

        switch (o) {
        case 0x00: // nop
            cost = 4;
            break;
        case 0x02: // ld (bc),a
            bus.pokeb(getBC(), a);
            cost = 7;
            break;
        case 0x04: // inc b
            b = inc8bit(b);
            cost = 4;
            break;
        case 0x06: // ld b,n
            b = fetchb();
            cost = 7;
            break;
        case 0x08: { // ex af,af'
            int t = getAF();
            setAF(af2);
            af2 = t;
            cost = 4;
        }
            break;
        case 0x0a: // ld a,(bc)
            a = bus.peekb(getBC());
            cost = 7;
            break;
        case 0x0c: // inc c
            c = inc8bit(c);
            cost = 4;
            break;
        case 0x0e: // ld c,n
            c = fetchb();
            cost = 7;
            break;

        default:
            Debug.println("Unknown instruction: " + StringUtil.toHex2(o));
            break;
        }
    }

    /** */
    private final void exec() {

        int o = fetchb();

        r = inc8bitInternal(r);

        switch (o) {
        case 0x40: // ld b,b
//          b = b;
            cost = 4;
            break;
        case 0x41: // ld b,c
            b = c;
            cost = 4;
            break;
        case 0x42: // ld b,d
            b = d;
            cost = 4;
            break;
        case 0x43: // ld b,e
            b = e;
            cost = 4;
            break;
        case 0x44: // ld b,h
            b = h;
            cost = 4;
            break;
        case 0x45: // ld b,l
            b = l;
            cost = 4;
            break;
        case 0x47: // ld b,a
            b = a;
            cost = 4;
            break;
        case 0x48: // ld c,b
            c = b;
            cost = 4;
            break;
        case 0x49: // ld c,c
//          c = c;
            cost = 4;
            break;
        case 0x4a: // ld c,d
            c = d;
            cost = 4;
            break;
        case 0x4b: // ld c,e
            c = e;
            cost = 4;
            break;
        case 0x4c: // ld c,h
            c = h;
            cost = 4;
            break;
        case 0x4d: // ld c,l
            c = l;
            cost = 4;
            break;
        case 0x4f: // ld c,a
            c = a;
            cost = 4;
            break;
        case 0x50: // ld d,b
            d = b;
            cost = 4;
            break;
        case 0x51: // ld d,c
            d = c;
            cost = 4;
            break;
        case 0x52: // ld d,d
//          d = d;
            cost = 4;
            break;
        case 0x53: // ld d,e
            d = e;
            cost = 4;
            break;
        case 0x54: // ld d,h
            d = h;
            cost = 4;
            break;
        case 0x55: // ld d,l
            d = l;
            cost = 4;
            break;
        case 0x57: // ld d,a
            d = a;
            cost = 4;
            break;
        case 0x58: // ld e,b
            e = b;
            cost = 4;
            break;
        case 0x59: // ld e,c
            e = c;
            cost = 4;
            break;
        case 0x5a: // ld e,d
            e = d;
            cost = 4;
            break;
        case 0x5b: // ld e,e
//          e = e;
            cost = 4;
            break;
        case 0x5c: // ld e,h
            e = h;
            cost = 4;
            break;
        case 0x5d: // ld e,l
            e = l;
            cost = 4;
            break;
        case 0x5f: // ld e,a
            e = a;
            cost = 4;
            break;
        case 0x60: // ld h,b
            h = b;
            cost = 4;
            break;
        case 0x61: // ld h,c
            h = c;
            cost = 4;
            break;
        case 0x62: // ld h,d
            h = d;
            cost = 4;
            break;
        case 0x63: // ld h,e
            h = e;
            cost = 4;
            break;
        case 0x64: // ld h,h
//          h = h;
            cost = 4;
            break;
        case 0x65: // ld h,l
            h = l;
            cost = 4;
            break;
        case 0x67: // ld h,a
            h = a;
            cost = 4;
            break;
        case 0x68: // ld l,b
            l = b;
            cost = 4;
            break;
        case 0x69: // ld l,c
            l = c;
            cost = 4;
            break;
        case 0x6a: // ld l,d
            l = d;
            cost = 4;
            break;
        case 0x6b: // ld l,e
            l = e;
            cost = 4;
            break;
        case 0x6c: // ld l,h
            l = h;
            cost = 4;
            break;
        case 0x6d: // ld l,l
//          l = l;
            cost = 4;
            break;
        case 0x6f: // ld l,a
            l = a;
            cost = 4;
            break;
        case 0x78: // ld a,b
            a = b;
            cost = 4;
            break;
        case 0x79: // ld a,c
            a = c;
            cost = 4;
            break;
        case 0x7a: // ld a,d
            a = d;
            cost = 4;
            break;
        case 0x7b: // ld a,e
            a = e;
            cost = 4;
            break;
        case 0x7c: // ld a,h
            a = h;
            cost = 4;
            break;
        case 0x7d: // ld a,l
            a = l;
            cost = 4;
            break;
        case 0x7f: // ld a,a
//          a = a;
            cost = 4;
            break;

        case 0x77: // ld (hl),a
            bus.pokeb(getHL(), a);
            cost = 7;
            break;
        case 0x70: // ld (hl),b
            bus.pokeb(getHL(), b);
            cost = 7;
            break;
        case 0x71: // ld (hl),c
            bus.pokeb(getHL(), c);
            cost = 7;
            break;
        case 0x72: // ld (hl),d
            bus.pokeb(getHL(), d);
            cost = 7;
            break;
        case 0x73: // ld (hl),e
            bus.pokeb(getHL(), e);
            cost = 7;
            break;
        case 0x74: // ld (hl),h
            bus.pokeb(getHL(), h);
            cost = 7;
            break;
        case 0x75: // ld (hl),l
            bus.pokeb(getHL(), l);
            cost = 7;
            break;

        case 0x7e: // ld a,(hl)
            a = bus.peekb(getHL());
            cost = 7;
            break;
        case 0x46: // ld b,(hl)
            b = bus.peekb(getHL());
            cost = 7;
            break;
        case 0x4e: // ld c,(hl)
            c = bus.peekb(getHL());
            cost = 7;
            break;
        case 0x56: // ld d,(hl)
            d = bus.peekb(getHL());
            cost = 7;
            break;
        case 0x5e: // ld e,(hl)
            e = bus.peekb(getHL());
            cost = 7;
            break;
        case 0x66: // ld h,(hl)
            h = bus.peekb(getHL());
            cost = 7;
            break;
        case 0x6e: // ld l,(hl)
            l = bus.peekb(getHL());
            cost = 7;
            break;

        case 0x3e: // ld a,n
            a = fetchb();
            cost = 7;
            break;
        case 0x06: // ld b,n
            b = fetchb();
            cost = 7;
            break;
        case 0x0e: // ld c,n
            c = fetchb();
            cost = 7;
            break;
        case 0x16: // ld d,n
            d = fetchb();
            cost = 7;
            break;
        case 0x1e: // ld e,n
            e = fetchb();
            cost = 7;
            break;
        case 0x26: // ld h,n
            h = fetchb();
            cost = 7;
            break;
        case 0x2e: // ld l,n
            l = fetchb();
            cost = 7;
            break;

        case 0x36: // ld (hl),n
            bus.pokeb(getHL(), fetchb());
            cost = 10;
            break;
        case 0x01: // ld bc,nn
            setBC(fetchw());
            cost = 10;
            break;
        case 0x11: // ld de,nn
            setDE(fetchw());
            cost = 10;
            break;
        case 0x21: // ld hl,nn
            setHL(fetchw());
            cost = 10;
            break;
        case 0x31: // ld sp,nn
            sp = fetchw();
            cost = 10;
            break;

        case 0x02: // ld (bc),a
            bus.pokeb(getBC(), a);
            cost = 7;
            break;
        case 0x12: // ld (de),a
            bus.pokeb(getDE(), a);
            cost = 7;
            break;
        case 0x0a: // ld a,(bc)
            a = bus.peekb(getBC());
            cost = 7;
            break;
        case 0x1a: // ld a,(de)
            a = bus.peekb(getDE());
            cost = 7;
            break;

        case 0x22: // ld (nn),hl
            bus.pokew(fetchw(), getHL());
            cost = 16;
            break;
        case 0x2a: // ld hl,(nn)
            setHL(bus.peekw(fetchw()));
            cost = 16;
            break;

        case 0x32: // ld (nn),a
            bus.pokeb(fetchw(), a);
            cost = 13;
            break;
        case 0x3a: // ld a,(nn)
            a = bus.peekb(fetchw());
            cost = 13;
            break;

        case 0xe3: { // ex (sp),hl
            int t = bus.peekw(sp);
            bus.pokew(sp, getHL());
            setHL(t);
        }
            break;
        case 0xeb: { // ex de,hl
            int t = getDE();
            setDE(getHL());
            setHL(t);
        }
            break;

        case 0xf9: // ld sp,hl
            sp = getHL();
            break;
        case 0xe9: // jp (hl)
            pc = getHL();
            break;

        case 0x3c: // inc a
            a = inc8bit(a);
            cost = 4;
            break;
        case 0x04: // inc b
            b = inc8bit(b);
            cost = 4;
            break;
        case 0x0c: // inc c
            c = inc8bit(c);
            cost = 4;
            break;
        case 0x14: // inc d
            d = inc8bit(d);
            cost = 4;
            break;
        case 0x1c: // inc e
            e = inc8bit(e);
            cost = 4;
            break;
        case 0x24: // inc h
            h = inc8bit(h);
            cost = 4;
            break;
        case 0x2c: // inc l
            l = inc8bit(l);
            cost = 4;
            break;
        case 0x34: // inc (hl)
            bus.pokeb(getHL(), inc8bit(bus.peekb(getHL())));
            cost = 11;
            break;

        case 0x3d: // dec a
            a = dec8bit(a);
            cost = 4;
            break;
        case 0x05: // dec b
            b = dec8bit(b);
            cost = 4;
            break;
        case 0x0d: // dec c
            c = dec8bit(c);
            cost = 4;
            break;
        case 0x15: // dec d
            d = dec8bit(d);
            cost = 4;
            break;
        case 0x1d: // dec e
            e = dec8bit(e);
            cost = 4;
            break;
        case 0x25: // dec h
            h = dec8bit(h);
            cost = 4;
            break;
        case 0x2d: // dec l
            l = dec8bit(l);
            cost = 4;
            break;
        case 0x35: // dec (hl)
            bus.pokeb(getHL(), dec8bit(bus.peekb(getHL())));
            cost = 11;
            break;

        case 0x03: // inc bc
            setBC(getBC() + 1);
            cost = 6;
            break;
        case 0x13: // inc de
            setDE(getDE() + 1);
            cost = 6;
            break;
        case 0x23: // inc hl
            setHL(getHL() + 1);
            cost = 6;
            break;
        case 0x33: // inc sp
            sp = inc8bitInternal(sp);
            cost = 6;
            break;
        case 0x0b: // dec bc
            setBC(getBC() - 1);
            cost = 6;
            break;
        case 0x1b: // dec de
            setDE(getDE() - 1);
            cost = 6;
            break;
        case 0x2b: // dec hl
            setHL(getHL() - 1);
            cost = 6;
            break;
        case 0x3b: // dec sp
            sp = sub16bitInternal(sp, 1);
            cost = 6;
            break;

        case 0x87: // add a,a
            a = add8bit(a, a);
            cost = 4;
            break;
        case 0x80: // add a,b
            a = add8bit(a, b);
            cost = 4;
            break;
        case 0x81: // add a,c
            a = add8bit(a, c);
            cost = 4;
            break;
        case 0x82: // add a,d
            a = add8bit(a, d);
            cost = 4;
            break;
        case 0x83: // add a,e
            a = add8bit(a, e);
            cost = 4;
            break;
        case 0x84: // add a,h
            a = add8bit(a, h);
            cost = 4;
            break;
        case 0x85: // add a,l
            a = add8bit(a, l);
            cost = 4;
            break;
        case 0x86: // add a,(hl)
            a = add8bit(a, bus.peekb(getHL()));
            cost = 7;
            break;

        case 0x8f: // adc a,a
            a = adc8bit(a, a);
            cost = 4;
            break;
        case 0x88: // adc a,b
            a = adc8bit(a, b);
            cost = 4;
            break;
        case 0x89: // adc a,c
            a = adc8bit(a, c);
            cost = 4;
            break;
        case 0x8a: // adc a,d
            a = adc8bit(a, d);
            cost = 4;
            break;
        case 0x8b: // adc a,e
            a = adc8bit(a, e);
            cost = 4;
            break;
        case 0x8c: // adc a,h
            a = adc8bit(a, h);
            cost = 4;
            break;
        case 0x8d: // adc a,l
            a = adc8bit(a, l);
            cost = 4;
            break;
        case 0x8e: // adc a,(hl)
            a = adc8bit(a, bus.peekb(getHL()));
            cost = 7;
            break;

        case 0x97: // sub a
            a = sub8bit(a, a);
            cost = 4;
            break;
        case 0x90: // sub b
            a = sub8bit(a, b);
            cost = 4;
            break;
        case 0x91: // sub c
            a = sub8bit(a, c);
            cost = 4;
            break;
        case 0x92: // sub d
            a = sub8bit(a, d);
            cost = 4;
            break;
        case 0x93: // sub e
            a = sub8bit(a, e);
            cost = 4;
            break;
        case 0x94: // sub h
            a = sub8bit(a, h);
            cost = 4;
            break;
        case 0x95: // sub l
            a = sub8bit(a, l);
            cost = 4;
            break;
        case 0x96: // sub (hl)
            a = sub8bit(a, bus.peekb(getHL()));
            cost = 7;
            break;

        case 0x9f: // sbc a,a
            a = sbc8bit(a, a);
            cost = 4;
            break;
        case 0x98: // sbc a,b
            a = sbc8bit(a, b);
            cost = 4;
            break;
        case 0x99: // sbc a,c
            a = sbc8bit(a, c);
            cost = 4;
            break;
        case 0x9a: // sbc a,d
            a = sbc8bit(a, d);
            cost = 4;
            break;
        case 0x9b: // sbc a,e
            a = sbc8bit(a, e);
            cost = 4;
            break;
        case 0x9c: // sbc a,h
            a = sbc8bit(a, h);
            cost = 4;
            break;
        case 0x9d: // sbc a,l
            a = sbc8bit(a, l);
            cost = 4;
            break;
        case 0x9e: // sbc a,(hl)
            a = sbc8bit(a, bus.peekb(getHL()));
            cost = 7;
            break;

        case 0xbf: // cp a
            cmp8bit(a, a);
            cost = 4;
            break;
        case 0xb8: // cp b
            cmp8bit(a, b);
            cost = 4;
            break;
        case 0xb9: // cp c
            cmp8bit(a, c);
            cost = 4;
            break;
        case 0xba: // cp d
            cmp8bit(a, d);
            cost = 4;
            break;
        case 0xbb: // cp e
            cmp8bit(a, e);
            cost = 4;
            break;
        case 0xbc: // cp h
            cmp8bit(a, h);
            cost = 4;
            break;
        case 0xbd: // cp l
            cmp8bit(a, l);
            cost = 4;
            break;
        case 0xbe: // cp (hl)
            cmp8bit(a, bus.peekb(getHL()));
            cost = 4;
            break;

        case 0xa7: // and a
            a = and8bit(a, a);
            cost = 4;
            break;
        case 0xa0: // and b
            a = and8bit(a, b);
            cost = 4;
            break;
        case 0xa1: // and c
            a = and8bit(a, c);
            cost = 4;
            break;
        case 0xa2: // and d
            a = and8bit(a, d);
            cost = 4;
            break;
        case 0xa3: // and e
            a = and8bit(a, e);
            cost = 4;
            break;
        case 0xa4: // and h
            a = and8bit(a, h);
            cost = 4;
            break;
        case 0xa5: // and l
            a = and8bit(a, l);
            cost = 4;
            break;
        case 0xa6: // and (hl)
            a = and8bit(a, bus.peekb(getHL()));
            cost = 7;
            break;

        case 0xaf: // xor a
            a = xor8bit(a, a);
            cost = 4;
            break;
        case 0xa8: // xor b
            a = xor8bit(a, b);
            cost = 4;
            break;
        case 0xa9: // xor c
            a = xor8bit(a, c);
            cost = 4;
            break;
        case 0xaa: // xor d
            a = xor8bit(a, d);
            cost = 4;
            break;
        case 0xab: // xor e
            a = xor8bit(a, e);
            cost = 4;
            break;
        case 0xac: // xor h
            a = xor8bit(a, h);
            cost = 4;
            break;
        case 0xad: // xor l
            a = xor8bit(a, l);
            cost = 4;
            break;
        case 0xae: // xor (hl)
            a = xor8bit(a, bus.peekb(getHL()));
            cost = 7;
            break;

        case 0xb7: // or a
            a = or8bit(a, a);
            cost = 4;
            break;
        case 0xb0: // or b
            a = or8bit(a, b);
            cost = 4;
            break;
        case 0xb1: // or c
            a = or8bit(a, c);
            cost = 4;
            break;
        case 0xb2: // or d
            a = or8bit(a, d);
            cost = 4;
            break;
        case 0xb3: // or e
            a = or8bit(a, e);
            cost = 4;
            break;
        case 0xb4: // or h
            a = or8bit(a, h);
            cost = 4;
            break;
        case 0xb5: // or l
            a = or8bit(a, l);
            cost = 4;
            break;
        case 0xb6: // or (hl)
            a = or8bit(a, bus.peekb(getHL()));
            cost = 7;
            break;

        case 0xc6: // add a,n
            a = add8bit(a, fetchb());
            cost = 7;
            break;
        case 0xce: // adc a,n
            a = adc8bit(a, fetchb());
            cost = 7;
            break;
        case 0xd6: // sub n
            a = sub8bit(a, fetchb());
            cost = 7;
            break;
        case 0xde: // sbc a,n
            a = sbc8bit(a, fetchb());
            cost = 7;
            break;
        case 0xe6: // and n
            a = and8bit(a, fetchb());
            cost = 7;
            break;
        case 0xee: // xor n
            a = xor8bit(a, fetchb());
            cost = 7;
            break;
        case 0xf6: // or a,n
            a = or8bit(a, fetchb());
            cost = 7;
            break;
        case 0xfe: // cp a,n
            cmp8bit(a, fetchb());
            cost = 7;
            break;

        case 0x09: // add hl,bc
            setHL(add16bit(getHL(), getBC()));
            cost = 11;
            break;
        case 0x19: // add hl,de
            setHL(add16bit(getHL(), getDE()));
            cost = 11;
            break;
        case 0x29: // add hl,hl
            setHL(add16bit(getHL(), getHL()));
            cost = 11;
            break;
        case 0x39: // add hl,sp
            setHL(add16bit(getHL(), sp));
            cost = 11;
            break;

        case 0x27: // daa
            if (((a & 0x0f) > 9) || fh) {
                a = add8bitInternal(a, 6);
                fc = (fh || fc);
                fh = true;
            }
            if ((a > 0x9f) || fc) {
                a = add8bitInternal(a, 0x60);
                fc = true;
            }

            fp = isParity(a);
            fz = isZero(a);
            fs = isSign(a);

            cost = 4;
            break;
        case 0x2f: // cpl
            fh = true;
            fn = true;
            a ^= 0xff;
            cost = 4;
            break;
        case 0x3f: // ccf
            fn = false;
            fc = !fc;
            cost = 4;
            break;
        case 0x37: // scf
            fh = false;
            fn = false;
            fc = true;
            cost = 4;
            break;

        case 0x07: // rlca
            a = rlc(a, false);
            cost = 4;
            break;
        case 0x0f: // rrca
            a = rrc(a, false);
            cost = 4;
            break;
        case 0x17: // rla
            a = rl(a, false);
            cost = 4;
            break;
        case 0x1f: // rra
            a = rr(a, false);
            cost = 4;
            break;

        case 0xc3: // jp
            pc = bus.peekw(pc);
            cost = 10;
            break;
        case 0xda: { // jp c,nn
            if (fc) {
                pc = fetchw();
            } else {
                pc = add16bitInternal(pc, 2);
            }
            cost = 10;
        }
            break;
        case 0xd2: { // jp nc,nn
            if (!fc) {
                pc = fetchw();
            } else {
                pc = add16bitInternal(pc, 2);
            }
            cost = 10;
        }
            break;
        case 0xca: { // jp z,nn
            if (fz) {
                pc = fetchw();
            } else {
                pc = add16bitInternal(pc, 2);
            }
            cost = 10;
        }
            break;
        case 0xc2: { // jp nz,nn
            if (!fz) {
                pc = fetchw();
            } else {
                pc = add16bitInternal(pc, 2);
            }
            cost = 10;
        }
            break;
        case 0xea: { // jp pe,nn
            if (fp) {
                pc = fetchw();
            } else {
                pc = add16bitInternal(pc, 2);
            }
            cost = 10;
        }
            break;
        case 0xe2: { // jp po,nn
            if (!fp) {
                pc = fetchw();
            } else {
                pc = add16bitInternal(pc, 2);
            }
            cost = 10;
        }
            break;
        case 0xfa: { // jp m,nn
            if (fs) {
                pc = fetchw();
            } else {
                pc = add16bitInternal(pc, 2);
            }
            cost = 10;
        }
            break;
        case 0xf2: { // jp p,nn
            if (!fs) {
                pc = fetchw();
            } else {
                pc = add16bitInternal(pc, 2);
            }
            cost = 10;
        }
            break;

        case 0xcd: { // call
            call();
            cost = 10;
        }
            break;
        case 0xdc: { // call c,nn
            if (fc) {
                call();
            } else {
                pc = add16bitInternal(pc, 2);
            }
            cost = 10;
        }
            break;
        case 0xd4: { // call nc,nn
            if (!fc) {
                call();
            } else {
                pc = add16bitInternal(pc, 2);
            }
            cost = 10;
        }
            break;
        case 0xcc: { // call z,nn
            if (fz) {
                call();
            } else {
                pc = add16bitInternal(pc, 2);
            }
            cost = 10;
        }
            break;
        case 0xc4: { // call nz,nn
            if (!fz) {
                call();
            } else {
                pc = add16bitInternal(pc, 2);
            }
            cost = 10;
        }
            break;
        case 0xec: { // call pe,nn
            if (fp) {
                call();
            } else {
                pc = add16bitInternal(pc, 2);
            }
            cost = 10;
        }
            break;
        case 0xe4: { // call po,nn
            if (!fp) {
                call();
            } else {
                pc = add16bitInternal(pc, 2);
            }
            cost = 10;
        }
            break;
        case 0xfc: { // call m,nn
            if (fs) {
                call();
            } else {
                pc = add16bitInternal(pc, 2);
            }
            cost = 10;
        }
            break;
        case 0xf4: { // call p,nn
            if (!fs) {
                call();
            } else {
                pc = add16bitInternal(pc, 2);
            }
            cost = 10;
        }
            break;

        case 0xc7: // rst 00h
            push(pc);
            pc = 0 * 8;
            cost = 4;
            break;
        case 0xcf: // rst 08h
            push(pc);
            pc = 1 * 8;
            cost = 4;
            break;
        case 0xd7: // rst 10h
            push(pc);
            pc = 2 * 8;
            cost = 4;
            break;
        case 0xdf: // rst 18h
            push(pc);
            pc = 3 * 8;
            cost = 4;
            break;
        case 0xe7: // rst 20h
            push(pc);
            pc = 4 * 8;
            cost = 4;
            break;
        case 0xef: // rst 28h
            push(pc);
            pc = 5 * 8;
            cost = 4;
            break;
        case 0xf7: // rst 30h
            push(pc);
            pc = 6 * 8;
            cost = 4;
            break;
        case 0xff: // rst 38h
            push(pc);
            pc = 7 * 8;
            cost = 4;
            break;

        case 0xc9: // ret
            pc = pop();
            cost = 4;
            break;
        case 0xd8: // ret c
            if (fc) {
                pc = pop();
            }
            cost = 4;
            break;
        case 0xd0: // ret nc
            if (!fc) {
                pc = pop();
            }
            cost = 4;
            break;
        case 0xc8: // ret z
            if (fz) {
                pc = pop();
            }
            cost = 4;
            break;
        case 0xc0: // ret nz
            if (!fz) {
                pc = pop();
            }
            cost = 4;
            break;
        case 0xe8: // ret pe
            if (fp) {
                pc = pop();
            }
            cost = 4;
            break;
        case 0xe0: // ret po
            if (!fp) {
                pc = pop();
            }
            cost = 4;
            break;
        case 0xf8: // ret m
            if (fs) {
                pc = pop();
            }
            cost = 4;
            break;
        case 0xf0: // ret p
            if (!fs) {
                pc = pop();
            }
            cost = 4;
            break;

        case 0xc5: // push bc
            push(getBC());
            cost = 11;
            break;
        case 0xd5: // push de
            push(getDE());
            cost = 11;
            break;
        case 0xe5: // push hl
            push(getHL());
            cost = 11;
            break;
        case 0xf5: // push af
            push(getAF());
            cost = 11;
            break;

        case 0xc1: // pop bc
            setBC(pop());
            cost = 10;
            break;
        case 0xd1: // pop de
            setDE(pop());
            cost = 10;
            break;
        case 0xe1: // pop hl
            setHL(pop());
            cost = 10;
            break;
        case 0xf1: // pop af
            setAF(pop());
            cost = 10;
            break;

        case 0xdb: // in a,n
            a = bus.inp(fetchb());
            cost = 11;
            break;
        case 0xd3: // out n,a
            bus.outp(fetchb(), a);
            cost = 11;
            break;

        case 0xf3: // di
            iff1 = false;
            iff2 = false;
// Debug.println("DI");
            cost = 4;
            break;
        case 0xfb: // ei
            iff1 = true;
            iff2 = true;
// Debug.println("EI");
            cost = 4;
            break;
        case 0x00: // nop
            cost = 4;
            break;
        case 0x76: // halt
Debug.println("halt: " + StringUtil.toHex4(pc - 1));
            while (!broken) {
//              r = add8bitInternal(xxx); // TODO check
                Thread.yield();
            }
            break;

        /*
         * Z80 instruction set
         */

        case 0x08: { // ex af,af'
            int t = getAF();
            setAF(af2);
            af2 = t;
            cost = 4;
        }
            break;

        case 0x10: { // djnz e
            b = dec8bitInternal(b);
            if (b != 0) {
                byte v = (byte) fetchb();
                pc = add16bitInternal(pc, v);
                cost = 13;
            } else {
                pc = add16bitInternal(pc, 1);
                cost = 8;
            }
        }
            break;

        case 0x18: { // jr e
            byte v = (byte) fetchb();
            pc = add16bitInternal(pc, v);
            cost = 12;
        }
            break;
        case 0x20: { // jr nz,e
            if (!fz) {
                byte v = (byte) fetchb();
                pc = add16bitInternal(pc, v);
                cost = 12;
            } else {
                pc = add16bitInternal(pc, 1);
                cost = 7;
            }
        }
            break;
        case 0x28: { // jr z,e
            if (fz) {
                byte v = (byte) fetchb();
                pc = add16bitInternal(pc, v);
                cost = 12;
            } else {
                pc = add16bitInternal(pc, 1);
                cost = 7;
            }
        }
            break;
        case 0x30: { // jr nc,e
            if (!fc) {
                byte v = (byte) fetchb();
                pc = add16bitInternal(pc, v);
                cost = 12;
            } else {
                pc = add16bitInternal(pc, 1);
                cost = 7;
            }
        }
            break;
        case 0x38: { // jr c,e
            if (fc) {
                byte v = (byte) fetchb();
                pc = add16bitInternal(pc, v);
            } else {
                pc = add16bitInternal(pc, 1);
                cost = 7;
            }
        }
            break;

        case 0xcb: // cb xx
            exec_cb();
            break;

        case 0xd9: { // exx
            int t = getBC();
            setBC(bc2);
            bc2 = t;

            t = getDE();
            setDE(de2);
            de2 = t;

            t = getHL();
            setHL(hl2);
            hl2 = t;

            cost = 4;
        }
            break;

        case 0xdd: // dd xx
            exec_dd();
            break;

        case 0xed: // ed xx
            exec_ed();
            break;

        case 0xfd: // fd xx
            exec_fd();
            break;

        default:
            Debug.println("Unknown instruction: " + StringUtil.toHex2(o));
            break;
        }
    }

    /** cb xx */
    private final void exec_cb() {

        int v = fetchb();

        r = inc8bitInternal(r);

        switch (v) {
        case 0x00: // rlc b
            b = rlc(b, true);
            cost = 8;
            break;
        case 0x01: // rlc c
            c = rlc(c, true);
            cost = 8;
            break;
        case 0x02: // rlc d
            d = rlc(d, true);
            cost = 8;
            break;
        case 0x03: // rlc e
            e = rlc(e, true);
            cost = 8;
            break;
        case 0x04: // rlc h
            h = rlc(h, true);
            cost = 8;
            break;
        case 0x05: // rlc l
            l = rlc(l, true);
            cost = 8;
            break;
        case 0x06: // rlc (hl)
            bus.pokeb(getHL(), rlc(bus.peekb(getHL()), true));
            cost = 15;
            break;
        case 0x07: // rlc a
            a = rlc(a, true);
            cost = 8;
            break;

        case 0x08: // rrc b
            b = rrc(b, true);
            cost = 8;
            break;
        case 0x09: // rrc c
            c = rrc(c, true);
            cost = 8;
            break;
        case 0x0a: // rrc d
            d = rrc(d, true);
            cost = 8;
            break;
        case 0x0b: // rrc e
            e = rrc(e, true);
            cost = 8;
            break;
        case 0x0c: // rrc h
            h = rrc(h, true);
            cost = 8;
            break;
        case 0x0d: // rrc l
            l = rrc(l, true);
            cost = 8;
            break;
        case 0x0e: // rrc (hl)
            bus.pokeb(getHL(), rrc(bus.peekb(getHL()), true));
            cost = 15;
            break;
        case 0x0f: // rrc a
            a = rrc(a, true);
            cost = 8;
            break;

        case 0x10: // rl b
            b = rl(b, true);
            cost = 8;
            break;
        case 0x11: // rl c
            c = rl(c, true);
            cost = 8;
            break;
        case 0x12: // rl d
            d = rl(d, true);
            cost = 8;
            break;
        case 0x13: // rl e
            e = rl(e, true);
            cost = 8;
            break;
        case 0x14: // rl h
            h = rl(h, true);
            cost = 8;
            break;
        case 0x15: // rl l
            l = rl(l, true);
            cost = 8;
            break;
        case 0x16: // rl (hl)
            bus.pokeb(getHL(), rl(bus.peekb(getHL()), true));
            cost = 15;
            break;
        case 0x17: // rl a
            a = rl(a, true);
            cost = 8;
            break;

        case 0x18: // rr b
            b = rr(b, true);
            cost = 8;
            break;
        case 0x19: // rr c
            c = rr(c, true);
            cost = 8;
            break;
        case 0x1a: // rr d
            d = rr(d, true);
            cost = 8;
            break;
        case 0x1b: // rr e
            e = rr(e, true);
            cost = 8;
            break;
        case 0x1c: // rr h
            h = rr(h, true);
            cost = 8;
            break;
        case 0x1d: // rr l
            l = rr(l, true);
            cost = 8;
            break;
        case 0x1e: // rr (hl)
            bus.pokeb(getHL(), rr(bus.peekb(getHL()), true));
            cost = 15;
            break;
        case 0x1f: // rr a
            a = rr(a, true);
            cost = 8;
            break;

        case 0x20: // sla b
            b = sla(b);
            cost = 8;
            break;
        case 0x21: // sla c
            c = sla(c);
            cost = 8;
            break;
        case 0x22: // sla d
            d = sla(d);
            cost = 8;
            break;
        case 0x23: // sla e
            e = sla(e);
            cost = 8;
            break;
        case 0x24: // sla h
            h = sla(h);
            cost = 8;
            break;
        case 0x25: // sla l
            l = sla(l);
            cost = 8;
            break;
        case 0x26: // sla (hl)
            bus.pokeb(getHL(), sla(bus.peekb(getHL())));
            cost = 15;
            break;
        case 0x27: // sla a
            a = sla(a);
            cost = 8;
            break;

        case 0x28: // sra b
            b = sra(b);
            cost = 8;
            break;
        case 0x29: // sra c
            c = sra(c);
            cost = 8;
            break;
        case 0x2a: // sra d
            d = sra(d);
            cost = 8;
            break;
        case 0x2b: // sra e
            e = sra(e);
            cost = 8;
            break;
        case 0x2c: // sra h
            h = sra(h);
            cost = 8;
            break;
        case 0x2d: // sra l
            l = sra(l);
            cost = 8;
            break;
        case 0x2e: // sra (hl)
            bus.pokeb(getHL(), sra(bus.peekb(getHL())));
            cost = 15;
            break;
        case 0x2f: // sra a
            a = sra(a);
            cost = 8;
            break;

        case 0x38: // srl b
            b = srl(b);
            cost = 8;
            break;
        case 0x39: // srl c
            c = srl(c);
            cost = 8;
            break;
        case 0x3a: // srl d
            d = srl(d);
            cost = 8;
            break;
        case 0x3b: // srl e
            e = srl(e);
            cost = 8;
            break;
        case 0x3c: // srl h
            h = srl(h);
            cost = 8;
            break;
        case 0x3d: // srl l
            l = srl(l);
            cost = 8;
            break;
        case 0x3e: // srl (hl)
            bus.pokeb(getHL(), srl(bus.peekb(getHL())));
            cost = 15;
            break;
        case 0x3f: // srl a
            a = srl(a);
            cost = 8;
            break;

        case 0x40: // bit 0,b
            bit(0, b);
            cost = 8;
            break;
        case 0x41: // bit 0,c
            bit(0, c);
            cost = 8;
            break;
        case 0x42: // bit 0,d
            bit(0, d);
            cost = 8;
            break;
        case 0x43: // bit 0,e
            bit(0, e);
            cost = 8;
            break;
        case 0x44: // bit 0,h
            bit(0, h);
            cost = 8;
            break;
        case 0x45: // bit 0,l
            bit(0, l);
            cost = 8;
            break;
        case 0x46: // bit 0,(hl)
            bit(0, bus.peekb(getHL()));
            cost = 12;
            break;
        case 0x47: // bit 0,a
            bit(0, a);
            cost = 8;
            break;

        case 0x48: // bit 1,b
            bit(1, b);
            cost = 8;
            break;
        case 0x49: // bit 1,c
            bit(1, c);
            cost = 8;
            break;
        case 0x4a: // bit 1,d
            bit(1, d);
            cost = 8;
            break;
        case 0x4b: // bit 1,e
            bit(1, e);
            cost = 8;
            break;
        case 0x4c: // bit 1,h
            bit(1, h);
            cost = 8;
            break;
        case 0x4d: // bit 1,l
            bit(1, l);
            cost = 8;
            break;
        case 0x4e: // bit 1,(hl)
            bit(1, bus.peekb(getHL()));
            cost = 12;
            break;
        case 0x4f: // bit 1,a
            bit(1, a);
            cost = 8;
            break;

        case 0x50: // bit 2,b
            bit(2, b);
            cost = 8;
            break;
        case 0x51: // bit 2,c
            bit(2, c);
            cost = 8;
            break;
        case 0x52: // bit 2,d
            bit(2, d);
            cost = 8;
            break;
        case 0x53: // bit 2,e
            bit(2, e);
            cost = 8;
            break;
        case 0x54: // bit 2,h
            bit(2, h);
            cost = 8;
            break;
        case 0x55: // bit 2,l
            bit(2, l);
            cost = 8;
            break;
        case 0x56: // bit 2,(hl)
            bit(2, bus.peekb(getHL()));
            cost = 12;
            break;
        case 0x57: // bit 2,a
            bit(2, a);
            cost = 8;
            break;

        case 0x58: // bit 3,b
            bit(3, b);
            cost = 8;
            break;
        case 0x59: // bit 3,c
            bit(3, c);
            cost = 8;
            break;
        case 0x5a: // bit 3,d
            bit(3, d);
            cost = 8;
            break;
        case 0x5b: // bit 3,e
            bit(3, e);
            cost = 8;
            break;
        case 0x5c: // bit 3,h
            bit(3, h);
            cost = 8;
            break;
        case 0x5d: // bit 3,l
            bit(3, l);
            cost = 8;
            break;
        case 0x5e: // bit 3,(hl)
            bit(3, bus.peekb(getHL()));
            cost = 12;
            break;
        case 0x5f: // bit 3,a
            bit(3, a);
            cost = 8;
            break;

        case 0x60: // bit 4,b
            bit(4, b);
            cost = 8;
            break;
        case 0x61: // bit 4,c
            bit(4, c);
            cost = 8;
            break;
        case 0x62: // bit 4,d
            bit(4, d);
            cost = 8;
            break;
        case 0x63: // bit 4,e
            bit(4, e);
            cost = 8;
            break;
        case 0x64: // bit 4,h
            bit(4, h);
            cost = 8;
            break;
        case 0x65: // bit 4,l
            bit(4, l);
            cost = 8;
            break;
        case 0x66: // bit 4,(hl)
            bit(4, bus.peekb(getHL()));
            cost = 12;
            break;
        case 0x67: // bit 4,a
            bit(4, a);
            cost = 8;
            break;

        case 0x68: // bit 5,b
            bit(5, b);
            cost = 8;
            break;
        case 0x69: // bit 5,c
            bit(5, c);
            cost = 8;
            break;
        case 0x6a: // bit 5,d
            bit(5, d);
            cost = 8;
            break;
        case 0x6b: // bit 5,e
            bit(5, e);
            cost = 8;
            break;
        case 0x6c: // bit 5,h
            bit(5, h);
            cost = 8;
            break;
        case 0x6d: // bit 5,l
            bit(5, l);
            cost = 8;
            break;
        case 0x6e: // bit 5,(hl)
            bit(5, bus.peekb(getHL()));
            cost = 12;
            break;
        case 0x6f: // bit 5,a
            bit(5, a);
            cost = 8;
            break;

        case 0x70: // bit 6,b
            bit(6, b);
            cost = 8;
            break;
        case 0x71: // bit 6,c
            bit(6, c);
            cost = 8;
            break;
        case 0x72: // bit 6,d
            bit(6, d);
            cost = 8;
            break;
        case 0x73: // bit 6,e
            bit(6, e);
            cost = 8;
            break;
        case 0x74: // bit 6,h
            bit(6, h);
            cost = 8;
            break;
        case 0x75: // bit 6,l
            bit(6, l);
            cost = 8;
            break;
        case 0x76: // bit 6,(hl)
            bit(6, bus.peekb(getHL()));
            cost = 12;
            break;
        case 0x77: // bit 6,a
            bit(6, a);
            cost = 8;
            break;

        case 0x78: // bit 7,b
            bit(7, b);
            cost = 8;
            break;
        case 0x79: // bit 7,c
            bit(7, c);
            cost = 8;
            break;
        case 0x7a: // bit 7,d
            bit(7, d);
            cost = 8;
            break;
        case 0x7b: // bit 7,e
            bit(7, e);
            cost = 8;
            break;
        case 0x7c: // bit 7,h
            bit(7, h);
            cost = 8;
            break;
        case 0x7d: // bit 7,l
            bit(7, l);
            cost = 8;
            break;
        case 0x7e: // bit 7,(hl)
            bit(7, bus.peekb(getHL()));
            cost = 12;
            break;
        case 0x7f: // bit 7,a
            bit(7, a);
            cost = 8;
            break;

        case 0x80: // res 0,b
            b = resetbit(0, b);
            cost = 8;
            break;
        case 0x81: // res 0,c
            c = resetbit(0, c);
            cost = 8;
            break;
        case 0x82: // res 0,d
            d = resetbit(0, d);
            cost = 8;
            break;
        case 0x83: // res 0,e
            e = resetbit(0, e);
            cost = 8;
            break;
        case 0x84: // res 0,h
            h = resetbit(0, h);
            cost = 8;
            break;
        case 0x85: // res 0,l
            l = resetbit(0, l);
            cost = 8;
            break;
        case 0x86: // res 0,(hl)
            bus.pokeb(getHL(), resetbit(0, bus.peekb(getHL())));
            cost = 15;
            break;
        case 0x87: // res 0,a
            a = resetbit(0, a);
            cost = 8;
            break;

        case 0x88: // res 1,b
            b = resetbit(1, b);
            cost = 8;
            break;
        case 0x89: // res 1,c
            c = resetbit(1, c);
            cost = 8;
            break;
        case 0x8a: // res 1,d
            d = resetbit(1, d);
            cost = 8;
            break;
        case 0x8b: // res 1,e
            e = resetbit(1, e);
            cost = 8;
            break;
        case 0x8c: // res 1,h
            h = resetbit(1, h);
            cost = 8;
            break;
        case 0x8d: // res 1,l
            l = resetbit(1, l);
            cost = 8;
            break;
        case 0x8e: // res 1,(hl)
            bus.pokeb(getHL(), resetbit(1, bus.peekb(getHL())));
            cost = 15;
            break;
        case 0x8f: // res 1,a
            a = resetbit(1, a);
            cost = 8;
            break;

        case 0x90: // res 2,b
            b = resetbit(2, b);
            cost = 8;
            break;
        case 0x91: // res 2,c
            c = resetbit(2, c);
            cost = 8;
            break;
        case 0x92: // res 2,d
            d = resetbit(2, d);
            cost = 8;
            break;
        case 0x93: // res 2,e
            e = resetbit(2, e);
            cost = 8;
            break;
        case 0x94: // res 2,h
            h = resetbit(2, h);
            cost = 8;
            break;
        case 0x95: // res 2,l
            l = resetbit(2, l);
            cost = 8;
            break;
        case 0x96: // res 2,(hl)
            bus.pokeb(getHL(), resetbit(2, bus.peekb(getHL())));
            cost = 15;
            break;
        case 0x97: // res 2,a
            a = resetbit(2, a);
            cost = 8;
            break;

        case 0x98: // res 3,b
            b = resetbit(3, b);
            cost = 8;
            break;
        case 0x99: // res 3,c
            c = resetbit(3, c);
            cost = 8;
            break;
        case 0x9a: // res 3,d
            d = resetbit(3, d);
            cost = 8;
            break;
        case 0x9b: // res 3,e
            e = resetbit(3, e);
            cost = 8;
            break;
        case 0x9c: // res 3,h
            h = resetbit(3, h);
            cost = 8;
            break;
        case 0x9d: // res 3,l
            l = resetbit(3, l);
            cost = 8;
            break;
        case 0x9e: // res 3,(hl)
            bus.pokeb(getHL(), resetbit(3, bus.peekb(getHL())));
            cost = 15;
            break;
        case 0x9f: // res 3,a
            a = resetbit(3, a);
            cost = 8;
            break;

        case 0xa0: // res 4,b
            b = resetbit(4, b);
            cost = 8;
            break;
        case 0xa1: // res 4,c
            c = resetbit(4, c);
            cost = 8;
            break;
        case 0xa2: // res 4,d
            d = resetbit(4, d);
            cost = 8;
            break;
        case 0xa3: // res 4,e
            e = resetbit(4, e);
            cost = 8;
            break;
        case 0xa4: // res 4,h
            h = resetbit(4, h);
            cost = 8;
            break;
        case 0xa5: // res 4,l
            l = resetbit(4, l);
            cost = 8;
            break;
        case 0xa6: // res 4,(hl)
            bus.pokeb(getHL(), resetbit(4, bus.peekb(getHL())));
            cost = 15;
            break;
        case 0xa7: // res 4,a
            a = resetbit(4, a);
            cost = 8;
            break;

        case 0xa8: // res 5,b
            b = resetbit(5, b);
            cost = 8;
            break;
        case 0xa9: // res 5,c
            c = resetbit(5, c);
            cost = 8;
            break;
        case 0xaa: // res 5,d
            d = resetbit(5, d);
            cost = 8;
            break;
        case 0xab: // res 5,e
            e = resetbit(5, e);
            cost = 8;
            break;
        case 0xac: // res 5,h
            h = resetbit(5, h);
            cost = 8;
            break;
        case 0xad: // res 5,l
            l = resetbit(5, l);
            cost = 8;
            break;
        case 0xae: // res 5,(hl)
            bus.pokeb(getHL(), resetbit(5, bus.peekb(getHL())));
            cost = 15;
            break;
        case 0xaf: // res 5,a
            a = resetbit(5, a);
            cost = 8;
            break;

        case 0xb0: // res 6,b
            b = resetbit(6, b);
            cost = 8;
            break;
        case 0xb1: // res 6,c
            c = resetbit(6, c);
            cost = 8;
            break;
        case 0xb2: // res 6,d
            d = resetbit(6, d);
            cost = 8;
            break;
        case 0xb3: // res 6,e
            e = resetbit(6, e);
            cost = 8;
            break;
        case 0xb4: // res 6,h
            h = resetbit(6, h);
            cost = 8;
            break;
        case 0xb5: // res 6,l
            l = resetbit(6, l);
            cost = 8;
            break;
        case 0xb6: // res 6,(hl)
            bus.pokeb(getHL(), resetbit(6, bus.peekb(getHL())));
            cost = 15;
            break;
        case 0xb7: // res 6,a
            a = resetbit(6, a);
            cost = 8;
            break;

        case 0xb8: // res 7,b
            b = resetbit(7, b);
            cost = 8;
            break;
        case 0xb9: // res 7,c
            c = resetbit(7, c);
            cost = 8;
            break;
        case 0xba: // res 7,d
            d = resetbit(7, d);
            cost = 8;
            break;
        case 0xbb: // res 7,e
            e = resetbit(7, e);
            cost = 8;
            break;
        case 0xbc: // res 7,h
            h = resetbit(7, h);
            cost = 8;
            break;
        case 0xbd: // res 7,l
            l = resetbit(7, l);
            cost = 8;
            break;
        case 0xbe: // res 7,(hl)
            bus.pokeb(getHL(), resetbit(7, bus.peekb(getHL())));
            cost = 15;
            break;
        case 0xbf: // res 7,a
            a = resetbit(7, a);
            cost = 8;
            break;

        case 0xc0: // set 0,b
            b = setbit(0, b);
            cost = 8;
            break;
        case 0xc1: // set 0,c
            c = setbit(0, c);
            cost = 8;
            break;
        case 0xc2: // set 0,d
            d = setbit(0, d);
            cost = 8;
            break;
        case 0xc3: // set 0,e
            e = setbit(0, e);
            cost = 8;
            break;
        case 0xc4: // set 0,h
            h = setbit(0, h);
            cost = 8;
            break;
        case 0xc5: // set 0,l
            l = setbit(0, l);
            cost = 8;
            break;
        case 0xc6: // set 0,(hl)
            bus.pokeb(getHL(), setbit(0, bus.peekb(getHL())));
            cost = 15;
            break;
        case 0xc7: // set 0,a
            a = setbit(0, a);
            cost = 8;
            break;

        case 0xc8: // set 1,b
            b = setbit(1, b);
            cost = 8;
            break;
        case 0xc9: // set 1,c
            c = setbit(1, c);
            cost = 8;
            break;
        case 0xca: // set 1,d
            d = setbit(1, d);
            cost = 8;
            break;
        case 0xcb: // set 1,e
            e = setbit(1, e);
            cost = 8;
            break;
        case 0xcc: // set 1,h
            h = setbit(1, h);
            cost = 8;
            break;
        case 0xcd: // set 1,l
            l = setbit(1, l);
            cost = 8;
            break;
        case 0xce: // set 1,(hl)
            bus.pokeb(getHL(), setbit(1, bus.peekb(getHL())));
            cost = 15;
            break;
        case 0xcf: // set 1,a
            a = setbit(1, a);
            cost = 8;
            break;

        case 0xd0: // set 2,b
            b = setbit(2, b);
            cost = 8;
            break;
        case 0xd1: // set 2,c
            c = setbit(2, c);
            cost = 8;
            break;
        case 0xd2: // set 2,d
            d = setbit(2, d);
            cost = 8;
            break;
        case 0xd3: // set 2,e
            e = setbit(2, e);
            cost = 8;
            break;
        case 0xd4: // set 2,h
            h = setbit(2, h);
            cost = 8;
            break;
        case 0xd5: // set 2,l
            l = setbit(2, l);
            cost = 8;
            break;
        case 0xd6: // set 2,(hl)
            bus.pokeb(getHL(), setbit(2, bus.peekb(getHL())));
            cost = 15;
            break;
        case 0xd7: // set 2,a
            a = setbit(2, a);
            cost = 8;
            break;

        case 0xd8: // set 3,b
            b = setbit(3, b);
            cost = 8;
            break;
        case 0xd9: // set 3,c
            c = setbit(3, c);
            cost = 8;
            break;
        case 0xda: // set 3,d
            d = setbit(3, d);
            cost = 8;
            break;
        case 0xdb: // set 3,e
            e = setbit(3, e);
            cost = 8;
            break;
        case 0xdc: // set 3,h
            h = setbit(3, h);
            cost = 8;
            break;
        case 0xdd: // set 3,l
            l = setbit(3, l);
            cost = 8;
            break;
        case 0xde: // set 3,(hl)
            bus.pokeb(getHL(), setbit(3, bus.peekb(getHL())));
            cost = 15;
            break;
        case 0xdf: // set 3,a
            a = setbit(3, a);
            cost = 8;
            break;

        case 0xe0: // set 4,b
            b = setbit(4, b);
            cost = 8;
            break;
        case 0xe1: // set 4,c
            c = setbit(4, c);
            cost = 8;
            break;
        case 0xe2: // set 4,d
            d = setbit(4, d);
            cost = 8;
            break;
        case 0xe3: // set 4,e
            e = setbit(4, e);
            cost = 8;
            break;
        case 0xe4: // set 4,h
            h = setbit(4, h);
            cost = 8;
            break;
        case 0xe5: // set 4,l
            l = setbit(4, l);
            cost = 8;
            break;
        case 0xe6: // set 4,(hl)
            bus.pokeb(getHL(), setbit(4, bus.peekb(getHL())));
            cost = 15;
            break;
        case 0xe7: // set 4,a
            a = setbit(4, a);
            cost = 8;
            break;

        case 0xe8: // set 5,b
            b = setbit(5, b);
            cost = 8;
            break;
        case 0xe9: // set 5,c
            c = setbit(5, c);
            cost = 8;
            break;
        case 0xea: // set 5,d
            d = setbit(5, d);
            cost = 8;
            break;
        case 0xeb: // set 5,e
            e = setbit(5, e);
            cost = 8;
            break;
        case 0xec: // set 5,h
            h = setbit(5, h);
            cost = 8;
            break;
        case 0xed: // set 5,l
            l = setbit(5, l);
            cost = 8;
            break;
        case 0xee: // set 5,(hl)
            bus.pokeb(getHL(), setbit(5, bus.peekb(getHL())));
            cost = 15;
            break;
        case 0xef: // set 5,a
            a = setbit(5, a);
            cost = 8;
            break;

        case 0xf0: // set 6,b
            b = setbit(6, b);
            cost = 8;
            break;
        case 0xf1: // set 6,c
            c = setbit(6, c);
            cost = 8;
            break;
        case 0xf2: // set 6,d
            d = setbit(6, d);
            cost = 8;
            break;
        case 0xf3: // set 6,e
            e = setbit(6, e);
            cost = 8;
            break;
        case 0xf4: // set 6,h
            h = setbit(6, h);
            cost = 8;
            break;
        case 0xf5: // set 6,l
            l = setbit(6, l);
            cost = 8;
            break;
        case 0xf6: // set 6,(hl)
            bus.pokeb(getHL(), setbit(6, bus.peekb(getHL())));
            cost = 15;
            break;
        case 0xf7: // set 6,a
            a = setbit(6, a);
            cost = 8;
            break;

        case 0xf8: // set 7,b
            b = setbit(7, b);
            cost = 8;
            break;
        case 0xf9: // set 7,c
            c = setbit(7, c);
            cost = 8;
            break;
        case 0xfa: // set 7,d
            d = setbit(7, d);
            cost = 8;
            break;
        case 0xfb: // set 7,e
            e = setbit(7, e);
            cost = 8;
            break;
        case 0xfc: // set 7,h
            h = setbit(7, h);
            cost = 8;
            break;
        case 0xfd: // set 7,l
            l = setbit(7, l);
            cost = 8;
            break;
        case 0xfe: // set 7,(hl)
            bus.pokeb(getHL(), setbit(7, bus.peekb(getHL())));
            cost = 15;
            break;
        case 0xff: // set 7,a
            a = setbit(7, a);
            cost = 8;
            break;

        default:
            Debug.println("Unknown instruction : " + StringUtil.toHex2(v));
            break;
        }
    }

    /** dd xx [b|ww] */
    private final void exec_dd() {
        int v = fetchb();

        switch (v) {
        case 0x09: // add ix,bc
            ix = add16bit(ix, getBC());
            cost = 15;
            break;
        case 0x19: // add ix,de
            ix = add16bit(ix, getDE());
            cost = 15;
            break;
        case 0x21: // ld ix,nn
            ix = fetchw();
            cost = 14;
            break;
        case 0x22: // ld (nn),ix
            bus.pokew(fetchw(), ix);
            cost = 20;
            break;
        case 0x23: // inc ix
            ix = add16bitInternal(ix, 1);
            cost = 10;
            break;
        case 0x29: // add ix,ix
            ix = add16bit(ix, ix);
            cost = 15;
            break;
        case 0x2a: // ld ix,(nn)
            ix = bus.peekw(fetchw());
            cost = 20;
            break;
        case 0x2b: // dec ix
            ix = sub16bitInternal(ix, 1);
            cost = 10;
            break;
        case 0x34: { // inc (ix+d)
            int i = index(ix, fetchb());
            int w = bus.peekb(i);
            bus.pokeb(i, inc8bit(w));
            cost = 23;
        }
            break;
        case 0x35: { // dec (ix+d)
            int i = index(ix, fetchb());
            int w = bus.peekb(i);
            bus.pokeb(i, dec8bit(w));
            cost = 23;
        }
            break;
        case 0x36: { // ld (ix+d),n
            int i = index(ix, fetchb());
            bus.pokeb(i, fetchb());
            cost = 19;
        }
            break;
        case 0x39: // add ix,sp
            ix = add16bit(ix, sp);
            cost = 15;
            break;

        case 0x46: { // ld b,(ix+d)
            int i = index(ix, fetchb());
            b = bus.peekb(i);
            cost = 19;
        }
            break;
        case 0x4e: { // ld c,(ix+d)
            int i = index(ix, fetchb());
            c = bus.peekb(i);
            cost = 19;
        }
            break;
        case 0x56: { // ld d,(ix+d)
            int i = index(ix, fetchb());
            d = bus.peekb(i);
            cost = 19;
        }
            break;
        case 0x5e: { // ld e,(ix+d)
            int i = index(ix, fetchb());
            e = bus.peekb(i);
            cost = 19;
        }
            break;
        case 0x66: { // ld h,(ix+d)
            int i = index(ix, fetchb());
            h = bus.peekb(i);
            cost = 19;
        }
            break;
        case 0x6e: { // ld l,(ix+d)
            int i = index(ix, fetchb());
            l = bus.peekb(i);
            cost = 19;
        }
            break;

        case 0x70: { // ld (ix+d),b
            int i = index(ix, fetchb());
            bus.pokeb(i, b);
            cost = 19;
        }
            break;
        case 0x71: { // ld (ix+d),c
            int i = index(ix, fetchb());
            bus.pokeb(i, c);
            cost = 19;
        }
            break;
        case 0x72: { // ld (ix+d),d
            int i = index(ix, fetchb());
            bus.pokeb(i, d);
            cost = 19;
        }
            break;
        case 0x73: { // ld (ix+d),e
            int i = index(ix, fetchb());
            bus.pokeb(i, e);
            cost = 19;
        }
            break;
        case 0x74: { // ld (ix+d),h
            int i = index(ix, fetchb());
            bus.pokeb(i, h);
            cost = 19;
        }
            break;
        case 0x75: { // ld (ix+d),l
            int i = index(ix, fetchb());
            bus.pokeb(i, l);
            cost = 19;
        }
            break;
        case 0x77: { // ld (ix+d),a
            int i = index(ix, fetchb());
            bus.pokeb(i, a);
            cost = 19;
        }
            break;

        case 0x7e: { // ld a,(ix+d)
            int i = index(ix, fetchb());
            a = bus.peekb(i);
            cost = 19;
        }
            break;
        case 0x86: { // add a,(ix+d)
            int i = index(ix, fetchb());
            int w = bus.peekb(i);
            a = add8bit(a, w);
            cost = 19;
        }
            break;
        case 0x8e: { // adc a,(ix+d)
            int i = index(ix, fetchb());
            int w = bus.peekb(i);
            a = adc8bit(a, w);
            cost = 19;
        }
            break;
        case 0x96: { // sub a,(ix+d)
            int i = index(ix, fetchb());
            int w = bus.peekb(i);
            a = sub8bit(a, w);
            cost = 19;
        }
            break;
        case 0x9e: { // sbc a,(ix+d)
            int i = index(ix, fetchb());
            int w = bus.peekb(i);
            a = sbc8bit(a, w);
            cost = 19;
        }
            break;
        case 0xa6: { // and (ix+d)
            int i = index(ix, fetchb());
            int w = bus.peekb(i);
            a = and8bit(a, w);
            cost = 19;
        }
            break;
        case 0xae: { // xor (ix+d)
            int i = index(ix, fetchb());
            int w = bus.peekb(i);
            a = xor8bit(a, w);
            cost = 19;
        }
            break;
        case 0xb6: { // or (ix+d)
            int i = index(ix, fetchb());
            int w = bus.peekb(i);
            a = or8bit(a, w);
            cost = 19;
        }
            break;
        case 0xbe: { // cp (ix+d)
            int i = index(ix, fetchb());
            int w = bus.peekb(i);
            cmp8bit(a, w);
            cost = 19;
        }
            break;

        case 0xcb: // dd cb xx
            exec_ddcb();
            break;

        case 0xe1: // pop ix
            ix = pop();
            cost = 14;
            break;
        case 0xe3: { // ex (sp),ix
            int t = bus.peekw(sp);
            bus.pokew(sp, ix);
            ix = t;
            cost = 23;
        }
            break;
        case 0xe5: // push ix
            push(ix);
            cost = 15;
            break;
        case 0xe9: // jp (ix)
            pc = ix;
            cost = 8;
            break;
        case 0xf9: // ld sp,ix
            sp = ix;
            cost = 10;
            break;

        default:
            Debug.println("Unknown instruction : " + StringUtil.toHex2(v));
            break;
        }
    }

    /**
     * dd cb b xx
     * <p>
     * 鐃重� pc += 2 ����̂� fetch �p���Ȃ� (�ȉ����l)
     * </p>
     */
    private final void exec_ddcb() {
        int v = bus.peekb(pc + 1);

        switch (v) {
        case 0x06: { // rlc (ix+d)
            int i = index(ix, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, rlc(w, true));
        }
            break;
        case 0x0e: { // rrc (ix+d)
            int i = index(ix, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, rrc(w, true));
        }
            break;
        case 0x16: { // rl (ix+d)
            int i = index(ix, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, rl(w, true));
        }
            break;
        case 0x1e: { // rr (ix+d)
            int i = index(ix, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, rr(w, true));
        }
            break;
        case 0x26: { // sla (ix+d)
            int i = index(ix, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, sla(w));
        }
            break;
        case 0x2e: { // sra (ix+d)
            int i = index(ix, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, sra(w));
        }
            break;
        case 0x3e: { // srl (ix+d)
            int i = index(ix, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, srl(w));
        }
            break;
        case 0x46: { // bit 0,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int w = bus.peekb(i);
            bit(0, w);
        }
            break;
        case 0x4e: { // bit 1,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int w = bus.peekb(i);
            bit(1, w);
        }
            break;
        case 0x56: { // bit 2,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int w = bus.peekb(i);
            bit(2, w);
        }
            break;
        case 0x5e: { // bit 3,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int w = bus.peekb(i);
            bit(3, w);
        }
            break;
        case 0x66: { // bit 4,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int w = bus.peekb(i);
            bit(4, w);
        }
            break;
        case 0x6e: { // bit 5,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int w = bus.peekb(i);
            bit(5, w);
        }
            break;
        case 0x76: { // bit 6,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int w = bus.peekb(i);
            bit(6, w);
        }
            break;
        case 0x7e: { // bit 7,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int w = bus.peekb(i);
            bit(7, w);
        }
            break;
        case 0x86: { // res 0,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, resetbit(0, w));
        }
            break;
        case 0x8e: { // res 1,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, resetbit(1, w));
        }
            break;
        case 0x96: { // res 2,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, resetbit(2, w));
        }
            break;
        case 0x9e: { // res 3,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, resetbit(3, w));
        }
            break;
        case 0xa6: { // res 4,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, resetbit(4, w));
        }
            break;
        case 0xae: { // res 5,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, resetbit(5, w));
        }
            break;
        case 0xb6: { // res 6,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, resetbit(6, w));
        }
            break;
        case 0xbe: { // res 7,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, resetbit(7, w));
        }
            break;
        case 0xc6: { // set 0,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, setbit(0, w));
        }
            break;
        case 0xce: { // set 1,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, setbit(1, w));
        }
            break;
        case 0xd6: { // set 2,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, setbit(2, w));
        }
            break;
        case 0xde: { // set 3,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, setbit(3, w));
        }
            break;
        case 0xe6: { // set 4,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, setbit(4, w));
        }
            break;
        case 0xee: { // set 5,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, setbit(5, w));
        }
            break;
        case 0xf6: { // set 6,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, setbit(6, w));
        }
            break;
        case 0xfe: { // set 7,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, setbit(7, w));
        }
            break;

        default:
            Debug.println("Unknown instruction : " + StringUtil.toHex2(v));
            break;
        }

        pc = add16bitInternal(pc, 2);
    }

    /** ed xx [w] */
    private final void exec_ed() {

        int o = fetchb();

        r = inc8bitInternal(r);

        switch (o) {
        case 0x40: // in b,(c)
            b = inpc();
            cost = 12;
            break;
        case 0x48: // in c,(c)
            c = inpc();
            cost = 12;
            break;
        case 0x50: // in d,(c)
            d = inpc();
            cost = 12;
            break;
        case 0x58: // in e,(c)
            e = inpc();
            cost = 12;
            break;
        case 0x60: // in h,(c)
            h = inpc();
            cost = 12;
            break;
        case 0x68: // in l,(c)
            l = inpc();
            cost = 12;
            break;
        case 0x78: // in a,(c)
            a = inpc();
            cost = 12;
            break;

        case 0x41: // out (c),b
            bus.outp(getBC(), b);
            cost = 12;
            break;
        case 0x49: // out (c),c
            bus.outp(getBC(), c);
            cost = 12;
            break;
        case 0x51: // out (c),d
            bus.outp(getBC(), d);
            cost = 12;
            break;
        case 0x59: // out (c),e
            bus.outp(getBC(), e);
            cost = 12;
            break;
        case 0x61: // out (c),h
            bus.outp(getBC(), h);
            cost = 12;
            break;
        case 0x69: // out (c),l
            bus.outp(getBC(), l);
            cost = 12;
            break;
        case 0x79: // out (c),a
            bus.outp(getBC(), a);
            cost = 12;
            break;

        case 0x42: // sbc hl,bc
            setHL(sbc16bit(getHL(), getBC()));
            cost = 15;
            break;
        case 0x52: // sbc hl,de
            setHL(sbc16bit(getHL(), getDE()));
            cost = 15;
            break;
        case 0x62: // sbc hl,hl
            setHL(sbc16bit(getHL(), getHL()));
            cost = 15;
            break;
        case 0x72: // sbc hl,sp
            setHL(sbc16bit(getHL(), sp));
            cost = 15;
            break;

        case 0x4a: // adc hl,bc
            setHL(adc16bit(getHL(), getBC()));
            cost = 15;
            break;
        case 0x5a: // adc hl,de
            setHL(adc16bit(getHL(), getDE()));
            cost = 15;
            break;
        case 0x6a: // adc hl,hl
            setHL(adc16bit(getHL(), getHL()));
            cost = 15;
            break;
        case 0x7a: // adc hl,sp
            setHL(adc16bit(getHL(), sp));
            cost = 15;
            break;

        case 0x43: // ld (nn),bc
            bus.pokew(fetchw(), getBC());
            cost = 20;
            break;
        case 0x53: // ld (nn),de
            bus.pokew(fetchw(), getDE());
            cost = 20;
            break;
        case 0x73: // ld (nn),sp
            bus.pokew(fetchw(), sp);
            cost = 20;
            break;

        case 0x4b: // ld bc,(nn)
            setBC(bus.peekw(fetchw()));
            cost = 20;
            break;
        case 0x5b: // ld de,(nn)
            setDE(bus.peekw(fetchw()));
            cost = 20;
            break;
        case 0x7b: // ld sp,(nn)
            sp = bus.peekw(fetchw());
            cost = 20;
            break;

        case 0x44: // neg
            fn = true;
            fh = isHalfC(0 - (a & 0x0f));

            a = -a;

            fp = isParity(a);
            fs = isSign(a);
            fz = isZero(a);

            cost = 8;
            break;

        case 0x45: // retn
            iff1 = iff2;
            pc = pop();
            cost = 14;
            break;
        case 0x4d: // reti
            iff1 = iff2;
            pc = pop();
            cost = 14;
            break;

        case 0x46: // im 0
            im = 0;
Debug.println("im 0");
            cost = 8;
            break;
        case 0x56: // im 1
            im = 1;
Debug.println("im 1");
            cost = 8;
            break;
        case 0x5e: // im 2
            im = 2;
Debug.println("im 2");
            cost = 8;
            break;

        case 0x47: // ld i,a
            i = a;
            cost = 9;
            break;
        case 0x4f: // ld r,a
            r = a;
            r7 = r;
            cost = 9;
            break;

        case 0x57: // ld a,i
            a = i;

            fn = false;
            fp = iff2;
            fs = isSign(a);
            fz = isZero(a);
            fh = false;

            cost = 9;
            break;
        case 0x5f: // ld a,r
            a = (r & 0x7f) | (r7 & 0x80);

            fn = false;
            fp = iff2;
            fs = isSign(a);
            fz = isZero(a);
            fh = false;

            cost = 9;
            break;

        case 0x67: { // rrd
            int v = bus.peekb(getHL());

            a = (a & 0xf0) | (v & 0x0f);
            bus.pokeb(getHL(), ((v & 0xf0) >> 4) | ((v & 0x0f) << 4));

            fp = isParity(a);
            fs = isSign(a);
            fz = isZero(a);
            fh = false;
            fn = false;

            cost = 18;
        }
            break;
        case 0x6f: { // rld
            int v = bus.peekb(getHL());

            a = (a & 0xf0) | ((v & 0xf0) >> 4);
            bus.pokeb(getHL(), ((v & 0x0f) << 4) | ((v & 0xf0) >> 4));

            fp = isParity(a);
            fs = isSign(a);
            fz = isZero(a);
            fh = false;
            fn = false;

            cost = 18;
        }
            break;

        case 0xa0: // ldi
            op_eda0();
            cost = 16;
            break;
        case 0xa1: // cpi
            op_eda1();
            cost = 16;
            break;
        case 0xa2: // ini
            op_eda2();
            cost = 16;
            break;
        case 0xa3: // outi
            op_eda3();
            cost = 16;
            break;

        case 0xa8: // ldd
            op_eda8();
            cost = 16;
            break;
        case 0xa9: // cpd
            op_eda9();
            cost = 16;
            break;
        case 0xaa: // ind
            op_edaa();
            cost = 16;
            break;
        case 0xab: // outd
            op_edab();
            cost = 16;
            break;

        case 0xb0: // ldir
            do {
                op_eda0();
                cost += 20;
            } while (fp);
            cost -= (cost == 0) ? 4 : -16;
            break;
        case 0xb1: // cpir
            do {
                op_eda1();
                cost = +20;
            } while (fp && !fz);
            cost -= (cost == 0) ? 4 : -16;
            break;
        case 0xb2: // inir
            do {
                op_eda2();
                cost = +20;
            } while (!fz);
            cost -= (cost == 0) ? 4 : -16;
            break;
        case 0xb3: // otir
            do {
                op_eda3();
                cost = +20;
            } while (!fz);
            cost -= (cost == 0) ? 4 : -16;
            break;

        case 0xb8: // lddr
            do {
                op_eda8();
                cost = +20;
            } while (fp);
            cost -= (cost == 0) ? 4 : -16;
            break;
        case 0xb9: // cpdr
            do {
                op_eda9();
                cost = +20;
            } while (fp && !fz);
            cost -= (cost == 0) ? 4 : -16;
            break;
        case 0xba: // indr
            do {
                op_edaa();
                cost = +20;
            } while (!fz);
            cost -= (cost == 0) ? 4 : -16;
            break;
        case 0xbb: // otdr
            do {
                op_edab();
                cost = +20;
            } while (!fz);
            cost -= (cost == 0) ? 4 : -16;
            break;

        default:
            Debug.println("Unknown instruction : " + StringUtil.toHex2(o));
            break;
        }
    }

    /** ldi */
    private final void op_eda0() {
        bus.pokeb(getDE(), bus.peekb(getHL()));

        setDE(getDE() + 1);
        setHL(getHL() + 1);
        setBC(getBC() - 1);

        fp = getBC() != 0;
        fh = false;
        fn = false;
    }

    /** cpi */
    private final void op_eda1() {
        cmp8bit(a, bus.peekb(getHL()));

        setHL(getHL() + 1);
        setBC(getBC() - 1);

        fp = getBC() != 0;
    }

    /** ini */
    private final void op_eda2() {
        bus.pokeb(getHL(), inpc());

        setHL(getHL() + 1);
        b = dec8bitInternal(b);

        fz = isZero(b);
        fn = true;
    }

    /** outi */
    private final void op_eda3() {
        bus.outp(c, bus.peekb(getHL()));

        setHL(getHL() + 1);
        b = dec8bitInternal(b);

        fz = isZero(b);
        fn = true;
    }

    /** ldd */
    private final void op_eda8() {
        bus.pokeb(getDE(), bus.peekb(getHL()));

        setDE(getDE() - 1);
        setHL(getHL() - 1);
        setBC(getBC() - 1);

        fp = getBC() != 0;
        fh = false;
        fn = false;
    }

    /** cpd */
    private final void op_eda9() {
        cmp8bit(a, bus.peekb(getHL()));

        setHL(getHL() - 1);
        setBC(getBC() - 1);

        fp = getBC() != 0;
    }

    /** ind */
    private final void op_edaa() {
        bus.pokeb(getHL(), inpc());

        setHL(getHL() - 1);
        b = dec8bitInternal(b);

        fz = isZero(b);
        fn = true;
    }

    /** outd */
    private final void op_edab() {
        bus.outp(c, bus.peekb(getHL()));

        setHL(getHL() - 1);
        b = dec8bitInternal(b);

        fz = isZero(b);
        fn = true;
    }

    /** fd xx [b|ww] */
    private final void exec_fd() {
        int v = fetchb();

        switch (v) {
        case 0x09: // add iy,bc
            iy = add16bit(iy, getBC());
            cost = 15;
            break;
        case 0x19: // add iy,de
            iy = add16bit(iy, getDE());
            cost = 15;
            break;
        case 0x21: // ld iy,nn
            iy = fetchw();
            cost = 14;
            break;
        case 0x22: // ld (nn),iy
            bus.pokew(fetchw(), iy);
            cost = 20;
            break;
        case 0x23: // inc iy
            iy = add16bitInternal(iy, 1);
            cost = 10;
            break;
        case 0x29: // add iy,iy
            iy = add16bit(iy, iy);
            cost = 15;
            break;
        case 0x2a: // ld iy,(nn)
            iy = bus.peekw(fetchw());
            cost = 20;
            break;
        case 0x2b: // dec iy
            iy = sub16bitInternal(iy, 1);
            cost = 10;
            break;
        case 0x34: { // inc (iy+d)
            int i = index(iy, fetchb());
            int w = bus.peekb(i);
            bus.pokeb(i, inc8bit(w));
            cost = 23;
        }
            break;
        case 0x35: { // dec (iy+d)
            int i = index(iy, fetchb());
            int w = bus.peekb(i);
            bus.pokeb(i, dec8bit(w));
            cost = 23;
        }
            break;
        case 0x36: { // ld (iy+d),n
            int i = index(iy, fetchb());
            bus.pokeb(i, fetchb());
            cost = 19;
        }
            break;
        case 0x39: { // add iy,sp
            iy = add16bit(iy, sp);
            cost = 15;
        }
            break;

        case 0x46: { // ld b,(iy+d)
            int i = index(iy, fetchb());
            b = bus.peekb(i);
            cost = 19;
        }
            break;
        case 0x4e: { // ld c,(iy+d)
            int i = index(iy, fetchb());
            c = bus.peekb(i);
            cost = 19;
        }
            break;
        case 0x56: { // ld d,(iy+d)
            int i = index(iy, fetchb());
            d = bus.peekb(i);
            cost = 19;
        }
            break;
        case 0x5e: { // ld e,(iy+d)
            int i = index(iy, fetchb());
            e = bus.peekb(i);
            cost = 19;
        }
            break;
        case 0x66: { // ld h,(iy+d)
            int i = index(iy, fetchb());
            h = bus.peekb(i);
            cost = 19;
        }
            break;
        case 0x6e: { // ld l,(iy+d)
            int i = index(iy, fetchb());
            l = bus.peekb(i);
            cost = 19;
        }
            break;

        case 0x70: { // ld (iy+d),b
            int i = index(iy, fetchb());
            bus.pokeb(i, b);
            cost = 19;
        }
            break;
        case 0x71: { // ld (iy+d),c
            int i = index(iy, fetchb());
            bus.pokeb(i, c);
            cost = 19;
        }
            break;
        case 0x72: { // ld (iy+d),d
            int i = index(iy, fetchb());
            bus.pokeb(i, d);
            cost = 19;
        }
            break;
        case 0x73: { // ld (iy+d),e
            int i = index(iy, fetchb());
            bus.pokeb(i, e);
            cost = 19;
        }
            break;
        case 0x74: { // ld (iy+d),h
            int i = index(iy, fetchb());
            bus.pokeb(i, h);
            cost = 19;
        }
            break;
        case 0x75: { // ld (iy+d),l
            int i = index(iy, fetchb());
            bus.pokeb(i, l);
            cost = 19;
        }
            break;
        case 0x77: { // ld (iy+d),a
            int i = index(iy, fetchb());
            bus.pokeb(i, a);
            cost = 19;
        }
            break;

        case 0x7e: { // ld a,(iy+d)
            int i = index(iy, fetchb());
            a = bus.peekb(i);
            cost = 19;
        }
            break;
        case 0x86: { // add a,(iy+d)
            int i = index(iy, fetchb());
            int w = bus.peekb(i);
            a = add8bit(a, w);
            cost = 19;
        }
            break;
        case 0x8e: { // adc a,(iy+d)
            int i = index(iy, fetchb());
            int w = bus.peekb(i);
            a = adc8bit(a, w);
            cost = 19;
        }
            break;
        case 0x96: { // sub (iy+d)
            int i = index(iy, fetchb());
            int w = bus.peekb(i);
            a = sub8bit(a, w);
            cost = 19;
        }
            break;
        case 0x9e: { // sbc a,(iy+d)
            int i = index(iy, fetchb());
            int w = bus.peekb(i);
            a = sbc8bit(a, w);
            cost = 19;
        }
            break;
        case 0xa6: { // and (iy+d)
            int i = index(iy, fetchb());
            int w = bus.peekb(i);
            a = and8bit(a, w);
            cost = 19;
        }
            break;
        case 0xae: { // xor (iy+d)
            int i = index(iy, fetchb());
            int w = bus.peekb(i);
            a = xor8bit(a, w);
            cost = 19;
        }
            break;
        case 0xb6: { // or (iy+d)
            int i = index(iy, fetchb());
            int w = bus.peekb(i);
            a = or8bit(a, w);
            cost = 19;
        }
            break;
        case 0xbe: { // cp (iy+d)
            int i = index(iy, fetchb());
            int w = bus.peekb(i);
            cmp8bit(a, w);
            cost = 19;
        }
            break;

        case 0xcb: // fd cb d xx
            exec_fdcb();
            break;

        case 0xe1: // pop iy
            iy = pop();
            cost = 14;
            break;
        case 0xe3: { // ex (sp),iy
            int t = bus.peekw(sp);
            bus.pokew(sp, iy);
            iy = t;
            cost = 23;
        }
            break;
        case 0xe5: // push iy
            push(iy);
            cost = 15;
            break;
        case 0xe9: // jp (iy)
            pc = iy;
            cost = 8;
            break;
        case 0xf9: // ld sp,iy
            sp = iy;
            cost = 10;
            break;

        default:
            Debug.println("Unknown instruction : " + StringUtil.toHex2(v));
            break;
        }
    }

    /**
     * fd cb b xx
     * <p>
     * �Ō�� pc += 2 ����̂� fetch �p���Ȃ� (�ȉ����l)
     * </p>
     */
    private final void exec_fdcb() {
        int v = bus.peekb(pc + 1);

        switch (v) {
        case 0x06: { // rlc (iy+d)
            int i = index(iy, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, rlc(w, true));
        }
            break;
        case 0x0e: { // rrc (iy+d)
            int i = index(iy, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, rrc(w, true));
        }
            break;
        case 0x16: { // rl (iy+d)
            int i = index(iy, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, rl(w, true));
        }
            break;
        case 0x1e: { // rr (iy+d)
            int i = index(iy, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, rr(w, true));
        }
            break;
        case 0x26: { // sla (iy+d)
            int i = index(iy, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, sla(w));
        }
            break;
        case 0x2e: { // sra (iy+d)
            int i = index(iy, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, sra(w));
        }
            break;
        case 0x3e: { // srl (iy+d)
            int i = index(iy, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, srl(w));
        }
            break;
        case 0x46: { // bit 0,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int w = bus.peekb(i);
            bit(0, w);
        }
            break;
        case 0x4e: { // bit 1,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int w = bus.peekb(i);
            bit(1, w);
        }
            break;
        case 0x56: { // bit 2,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int w = bus.peekb(i);
            bit(2, w);
        }
            break;
        case 0x5e: { // bit 3,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int w = bus.peekb(i);
            bit(3, w);
        }
            break;
        case 0x66: { // bit 4,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int w = bus.peekb(i);
            bit(4, w);
        }
            break;
        case 0x6e: { // bit 5,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int w = bus.peekb(i);
            bit(5, w);
        }
            break;
        case 0x76: { // bit 6,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int w = bus.peekb(i);
            bit(6, w);
        }
            break;
        case 0x7e: { // bit 7,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int w = bus.peekb(i);
            bit(7, w);
        }
            break;
        case 0x86: { // res 0,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, resetbit(0, w));
        }
            break;
        case 0x8e: { // res 1,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, resetbit(1, w));
        }
            break;
        case 0x96: { // res 2,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, resetbit(2, w));
        }
            break;
        case 0x9e: { // res 3,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, resetbit(3, w));
        }
            break;
        case 0xa6: { // res 4,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, resetbit(4, w));
        }
            break;
        case 0xae: { // res 5,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, resetbit(5, w));
        }
            break;
        case 0xb6: { // res 6,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, resetbit(6, w));
        }
            break;
        case 0xbe: { // res 7,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, resetbit(7, w));
        }
            break;
        case 0xc6: { // set 0,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, setbit(0, w));
        }
            break;
        case 0xce: { // set 1,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, setbit(1, w));
        }
            break;
        case 0xd6: { // set 2,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, setbit(2, w));
        }
            break;
        case 0xde: { // set 3,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, setbit(3, w));
        }
            break;
        case 0xe6: { // set 4,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, setbit(4, w));
        }
            break;
        case 0xee: { // set 5,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, setbit(5, w));
        }
            break;
        case 0xf6: { // set 6,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, setbit(6, w));
        }
            break;
        case 0xfe: { // set 7,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int w = bus.peekb(i);
            bus.pokeb(i, setbit(7, w));
        }
            break;

        default:
            Debug.println("Unknown instruction : " + StringUtil.toHex2(v));
            break;
        }

        pc = add16bitInternal(pc, 2);
    }

    // -------------------------------------------------------------------------

    /** */
    private static Properties names = new Properties();

    /** */
    static {
        try {
            Class<?> clazz = Debugger.class;
            InputStream is = clazz.getResourceAsStream("/address.properties");
            names.load(is);
        } catch (IOException e) {
            Debug.printStackTrace(e);
            System.exit(1);
        }
    }
}

/* */
