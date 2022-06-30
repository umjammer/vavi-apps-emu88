/*
 * Copyright (c) 1993-2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.em88;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.function.Consumer;

import vavi.util.Debug;


/**
 * Z80 Emulator.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 931205 nsano original 8080a instructions only <br>
 *          0.10 931208 nsano add Z80 instructions <br>
 *          1.00 031228 nsano java porting <br>
 *          1.10 040101 nsano see jasper Z80 <br>
 */
public class Z80 implements Device {

    // Z80 register emulation

    // 8bit registers
    private int a, f, b, c, d, e, r, i;
    private int af2, bc2, de2, hl2;

    // 16bit registers
    private int hl;
    private int sp;
    private int pc;
    private int ix;
    private int iy;

    // interrupt flip-flops
    private boolean iff1 = true;
    private boolean iff2 = true;

    // interrupt mode 0, 1, 2
    private int im = 2;

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
    private void assembleFlags() {
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
    private void extractFlags() {
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

    public final int getAF2() {
        return af2;
    }

    public final void setAF2(int af2) {
        this.af2 = af2;
    }

    public final int getBC() {
        return (b << 8) | c;
    }

    public final void setBC(int bc) {
        b = (bc >> 8) & 0xff;
        c = bc & 0xff;
    }

    public final int getBC2() {
        return bc2;
    }

    public final void setBC2(int bc2) {
        this.bc2 = bc2;
    }

    public final int getDE() {
        return (d << 8) | e;
    }

    public final void setDE(int de) {
        d = (de >> 8) & 0xff;
        e = de & 0xff;
    }

    public final int getDE2() {
        return de2;
    }

    public final void setDE2(int de2) {
        this.de2 = de2;
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

    /** for test */
    public final void addSP(int i) {
        this.sp = add16bitInternal(this.sp, i);
    }

    public final int getPC() {
        return pc;
    }

    public final void setPC(int pc) {
        this.pc = pc;
    }

    public final int getHL() {
        return hl;
    }

    public final void setHL(int hl) {
        this.hl = hl;
    }

    public final int getHL2() {
        return hl2;
    }

    public final void setHL2(int hl2) {
        this.hl2 = hl2;
    }

    /** for internal */
    public final int getH() {
        return (this.hl >> 8) & 0xff;
    }
    /** for internal */
    public final void setH(int h) {
        this.hl = (this.hl & 0x00ff) | (h << 8);
    }
    /** for internal */
    public final int getL() {
        return this.hl & 0xff;
    }
    /** for internal */
    public final void setL(int l) {
        this.hl = (this.hl & 0xff00) | l;
    }

    public final int getIX() {
        return ix;
    }

    public final void setIX(int ix) {
        this.ix = ix;
    }

    /** for test */
    public final int getIXH() {
        return (this.ix >> 8) & 0xff;
    }
    /** for test */
    public final void setIXH(int ixh) {
        this.ix = (this.ix & 0x00ff) | (ixh << 8);
    }
    /** for test */
    public final int getIXL() {
        return this.ix & 0xff;
    }
    /** for test */
    public final void setIXL(int ixl) {
        this.ix = (this.ix & 0xff00) | ixl;
    }

    public final int getIY() {
        return iy;
    }

    public final void setIY(int iy) {
        this.iy = iy;
    }

    /** for test */
    public final int getIYH() {
        return (this.iy >> 8) & 0xff;
    }
    /** for test */
    public final void setIYH(int iyh) {
        this.iy = (this.iy & 0x00ff) | (iyh << 8);
    }
    /** for test */
    public final int getIYL() {
        return this.iy & 0xff;
    }
    /** for test */
    public final void setIYL(int iyl) {
        this.iy = (this.iy & 0xff00) | iyl;
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

    public int getB() {
        return b;
    }

    public void setB(int b) {
        this.b = b;
    }

    public int getC() {
        return c;
    }

    public void setC(int c) {
        this.c = c;
    }

    public int getD() {
        return d;
    }

    public void setD(int d) {
        this.d = d;
    }

    public int getE() {
        return e;
    }

    public void setE(int e) {
        this.e = e;
    }

    /** Interrupt modes/register */
    public int getI() {
        return i;
    }

    /** for test TODO */
    public void setI(int v) {
        i = v;
    }

    /** Memory refresh register */
    public int getR() {
        return r;
    }

    /** for test */
    public void setR(int v) {
        r = v;
    }

    // flag operations

    public boolean isC() {
        return fc;
    }
    public boolean isN() {
        return fn;
    }
    public boolean isP() {
        return fp;
    }
    public boolean is3() {
        return f3;
    }
    public boolean is5() {
        return f5;
    }
    public boolean isH() {
        return fh;
    }
    public boolean isZ() {
        return fz;
    }
    public boolean isS() {
        return fs;
    }

    /** for test */
    public void setN(boolean v) {
        fn = v;
        assembleFlags();
    }
    /** for test */
    public void setP(boolean v) {
        fp = v;
        assembleFlags();
    }
    /** for test */
    public void setH(boolean v) {
        fh = v;
        assembleFlags();
    }
    /** for test */
    public void setZ(boolean v) {
        fz = v;
        assembleFlags();
    }
    /** for test */
    public void setS(boolean v) {
        fs = v;
        assembleFlags();
    }
    /** for test */
    public void setC(boolean v) {
        fc = v;
        assembleFlags();
    }
    /** for test */
    public void set3(boolean v) {
        f3 = v;
        assembleFlags();
    }
    /** for test */
    public void set5(boolean v) {
        f5 = v;
        assembleFlags();
    }

    /** */
    public int getIm() {
        return im;
    }

    /** for test TODO */
    public void setIm(int v) {
        im = v;
    }

    /** */
    public boolean isIff1() {
        return iff1;
    }

    /** */
    public boolean isIff2() {
        return iff2;
    }

    /** for test TODO */
    public void setIff1(boolean v) {
        iff1 = v;
    }

    /** for test TODO */
    public void setIff2(boolean v) {
        iff2 = v;
    }

    // -------------------------------------------------------------------------

    /** μPD82xx */
    private INTC intc;

    /** bus emulation */
    private Bus bus;

    /** */
    public Bus getBus() {
        return bus;
    }

    /** emulation connect bus */
    public void setBus(Bus bus) {
        this.bus = bus;

        this.intc = (INTC) bus.getDevice(INTC.class.getName());
    }

    /** stop running */
    private volatile boolean broken = false;

    /** stop running or not */
    public boolean isUserBroken() {
        return broken;
    }

    /** @param broken true: stop running */
    public void setUserBroken(boolean broken) {
        this.broken = broken;
    }

    /** interrupted */
    private volatile boolean interrupted = false;

    /** interrupted or not */
    public boolean isInterrupted() {
        return interrupted;
    }

    /** non maskable interrupted */
    private volatile boolean nmi = false;

    /** do interruption */
    public void requestInterrupt() {
        interrupted = true;
        nmi = false;
    }

    /** do nmi */
    public void requestNonMaskableInterrupt() {
        interrupted = true;
        nmi = true;
    }

    /** before instruction listener */
    private Consumer<Z80> listener;

    /** add before instruction listener */
    public void addListener(Consumer<Z80> listener) {
        this.listener = listener;
    }

    /** exec instruction listener */
    private void fireListener(Z80 arg) {
        if (listener != null)
            listener.accept(arg);
    }

    /** used cycles */
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
                fireListener(this);
                exec();
                processInterrupt();
            }
        } else {
            while (!broken) {
                fireListener(this);
                exec();
                processInterrupt();
            }
        }

        return pc;
    }

    /** */
    private void processInterrupt() {
        if (interrupted) {
            intc.acknowledgeInterrupt();
            if (iff1) {
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
    private int fetchB() {
        int v = bus.peekb(pc);
        pc = add16bitInternal(pc, 1);
        return v;
    }

    /**
     * fetch a word indicated by pc address.
     * pc will be incremented two.
     */
    private int fetchW() {
        int v = bus.peekw(pc);
        pc = add16bitInternal(pc, 2);
        return v;
    }

    // -------------------------------------------------------------------------

    private static boolean isZero(int v) {
        return v == 0;
    }

    private static boolean isSign(int v) {
        return (v & 0x80) != 0;
    }

    private static boolean isSign16(int v) {
        return (v & 0x8000) != 0;
    }

    private static boolean isCarry(int v) {
        return (v & 0x100) != 0;
    }

    private static boolean isHalfC(int v) {
        return (v & 0x10) != 0;
    }

    private static boolean isCarry16(int v) {
        return (v & 0x10000) != 0;
    }

    private static boolean isParity(int v) {
        return parityTable[v];
    }

    /** */
    private static final boolean[] parityTable = {
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

    /** overlap */
    public static int add16bitInternal(int v, int o) {
        return (v + o) & 0xffff;
    }

    /** overlap */
    public static int inc16bitInternal(int v) {
        return add16bitInternal(v, 1);
    }

    /** overlap */
    public static int sub16bitInternal(int v, int o) {
        return (v - o) & 0xffff;
    }

    /** overlap */
    public static int dec16bitInternal(int v) {
        return sub16bitInternal(v, 1);
    }

    /** overlap */
    public static int add8bitInternal(int v, int o) {
        return (v + o) & 0xff;
    }

    /** overlap */
    private static int inc8bitInternal(int v) {
        return add8bitInternal(v, 1);
    }

    /** overlap */
    private static int sub8bitInternal(int v, int o) {
        return (v - o) & 0xff;
    }

    /** overlap */
    public static int dec8bitInternal(int v) {
        return sub8bitInternal(v, 1);
    }

    /** for R */
    private static int inc7Bits(int value) {
        return (value & 0x80) == 0 ? (value + 1) & 0x7F : (value + 1) & 0x7F | 0x80;
    }

    /** */
    private void push(int v) {
        sp = sub16bitInternal(sp, 2);
        bus.pokew(sp, v);
// debug1();
    }

@SuppressWarnings("unused")
private void debug1() {
    String key = String.format("%04x", bus.peekw(pc - 2));
    if (names.containsKey(key)) {
        Debug.println("call: " + key + ":" + names.getProperty(key));
    } else {
        Debug.println("push: " + key + ": ???");
    }
}

    /** */
    private int pop() {
        int v = bus.peekw(sp);
        sp = add16bitInternal(sp, 2);
        return v;
    }

    /**
     * @param v used only lower 8bits (signed)
     */
    private int index(int o, int v) {
        return add16bitInternal(o, (byte) v);
    }

    /** */
    private void call() {
        int w = fetchW();
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
    private int add8bit(int o1, int o2) {
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
    private int adc8bit(int o1, int o2) {
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
    private int sub8bit(int o1, int o2) {
        int w = o1 - o2;
        int v = w & 0xff;

        fp = ((o1 ^ o2) & (o1 ^ v) & 0x80) != 0;
        fh = isHalfC(o1 ^ v ^ o2);
        fc = isCarry(w);
        fz = isZero(v);
        fs = isSign(v);
        fn = true;

        return v;
    }

    /** 8 bit sbc */
    private int sbc8bit(int o1, int o2) {
        int cy = fc ? 1 : 0;
        int w = o1 - o2 - cy;
        int v = w & 0xff;

        fp = ((o1 ^ o2) & (o1 ^ v) & 0x80) != 0;
        fh = isHalfC(o1 ^ v ^ o2);
        fc = isCarry(w);
        fz = isZero(v);
        fs = isSign(v);
        fn = true;

        return v;
    }

    /** */
    private int cmp8bit(int x, int y) {
        return sub8bit(x, y);
    }

    /** 8 bit increment */
    private int inc8bit(int o) {
        int v = inc8bitInternal(o);

        fh = isHalfC(inc8bitInternal(o & 0x0f));
        fp = o == 0x7f;
        fz = isZero(v);
        fs = isSign(v);
        fn = false;

        return v;
    }

    /** 8 bit decrement */
    private int dec8bit(int o) {
        int v = dec8bitInternal(o);

        fh = isHalfC(dec8bitInternal(o & 0x0f));
        fp = o == 0x80;
        fz = isZero(v);
        fs = isSign(v);
        fn = true;

        return v;
    }

    /** 8 bit and */
    private int and8bit(int o1, int o2) {
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
    private int or8bit(int o1, int o2) {
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
    private int xor8bit(int o1, int o2) {
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
    private int add16bit(int o1, int o2) {
        int w = o1 + o2;
        int v = w & 0xffff;

        fc = isCarry16(w);
        fh = (((o1 & 0x0fff) + (o2 & 0x0fff)) & 0x1000) != 0;
        fn = false;

        return v;
    }

    /** 16 bit adc */
    private int adc16bit(int o1, int o2) {
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
    private int sbc16bit(int o1, int o2) {
        int cy = fc ? 1 : 0;
        int w = o1 - o2 - cy;
        int v = w & 0xffff;

        fp = ((o1 ^ o2) & (o1 ^ v) & 0x8000) != 0;
        fh = ((o1 ^ v ^ o2) & 0x1000) != 0;
        fc = isCarry16(w);
        fz = isZero(v);
        fs = isSign16(v);
        fn = true;

        return v;
    }

    /** rotate left circler */
    private int rlc(int o, boolean na) {

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
    private int rrc(int o, boolean na) {

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
    private int rl(int o, boolean na) {

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
    private int rr(int o, boolean na) {

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
    private int sla(int o) {

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
    private int sra(int o) {

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
    private int srl(int o) {

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
    private void bit(int n, int o) {
        fp = (o & bit_tbl[n]) == 0;
        fz = fp;
        fs = n != 7 ? false : !fp;
        fh = true;
        fn = false;
    }

    /** set bit */
    private int setBit(int n, int o) {
        return o | bit_tbl[n];
    }

    /** reset bit */
    private int resetBit(int n, int o) {
        return o & ~bit_tbl[n];
    }

    /** bus.input port c */
    private int inpc() {
        int v = bus.inp(getBC());

        fn = false;
        fp = isParity(v);
        fs = isSign(v);
        fz = isZero(v);
        fh = isHalfC(v);

        return v;
    }

    // ----

    /** execute at pc */
    private void exec() {

        int o = fetchB();

        exec(o);
    }

    /**
     * for test
     * @param o unsigned byte
     */
    public int exec(int o) {
        r = inc7Bits(r);

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
            b = getH();
            cost = 4;
            break;
        case 0x45: // ld b,l
            b = getL();
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
            c = getH();
            cost = 4;
            break;
        case 0x4d: // ld c,l
            c = getL();
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
            d = getH();
            cost = 4;
            break;
        case 0x55: // ld d,l
            d = getL();
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
            e = getH();
            cost = 4;
            break;
        case 0x5d: // ld e,l
            e = getL();
            cost = 4;
            break;
        case 0x5f: // ld e,a
            e = a;
            cost = 4;
            break;
        case 0x60: // ld h,b
            setH(b);
            cost = 4;
            break;
        case 0x61: // ld h,c
            setH(c);
            cost = 4;
            break;
        case 0x62: // ld h,d
            setH(d);
            cost = 4;
            break;
        case 0x63: // ld h,e
            setH(e);
            cost = 4;
            break;
        case 0x64: // ld h,h
//          setH(h)
            cost = 4;
            break;
        case 0x65: // ld h,l
            setH(getL());
            cost = 4;
            break;
        case 0x67: // ld h,a
            setH(a);
            cost = 4;
            break;
        case 0x68: // ld l,b
            setL(b);
            cost = 4;
            break;
        case 0x69: // ld l,c
            setL(c);
            cost = 4;
            break;
        case 0x6a: // ld l,d
            setL(d);
            cost = 4;
            break;
        case 0x6b: // ld l,e
            setL(e);
            cost = 4;
            break;
        case 0x6c: // ld l,h
            setL(getH());
            cost = 4;
            break;
        case 0x6d: // ld l,l
//          setL(getL());
            cost = 4;
            break;
        case 0x6f: // ld l,a
            setL(a);
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
            a = getH();
            cost = 4;
            break;
        case 0x7d: // ld a,l
            a = getL();
            cost = 4;
            break;
        case 0x7f: // ld a,a
//          a = a;
            cost = 4;
            break;

        case 0x77: // ld (hl),a
            bus.pokeb(hl, a);
            cost = 7;
            break;
        case 0x70: // ld (hl),b
            bus.pokeb(hl, b);
            cost = 7;
            break;
        case 0x71: // ld (hl),c
            bus.pokeb(hl, c);
            cost = 7;
            break;
        case 0x72: // ld (hl),d
            bus.pokeb(hl, d);
            cost = 7;
            break;
        case 0x73: // ld (hl),e
            bus.pokeb(hl, e);
            cost = 7;
            break;
        case 0x74: // ld (hl),h
            bus.pokeb(hl, getH());
            cost = 7;
            break;
        case 0x75: // ld (hl),l
            bus.pokeb(hl, getL());
            cost = 7;
            break;

        case 0x7e: // ld a,(hl)
            a = bus.peekb(hl);
            cost = 7;
            break;
        case 0x46: // ld b,(hl)
            b = bus.peekb(hl);
            cost = 7;
            break;
        case 0x4e: // ld c,(hl)
            c = bus.peekb(hl);
            cost = 7;
            break;
        case 0x56: // ld d,(hl)
            d = bus.peekb(hl);
            cost = 7;
            break;
        case 0x5e: // ld e,(hl)
            e = bus.peekb(hl);
            cost = 7;
            break;
        case 0x66: // ld h,(hl)
            setH(bus.peekb(hl));
            cost = 7;
            break;
        case 0x6e: // ld l,(hl)
            setL(bus.peekb(hl));
            cost = 7;
            break;

        case 0x3e: // ld a,n
            a = fetchB();
            cost = 7;
            break;
        case 0x06: // ld b,n
            b = fetchB();
            cost = 7;
            break;
        case 0x0e: // ld c,n
            c = fetchB();
            cost = 7;
            break;
        case 0x16: // ld d,n
            d = fetchB();
            cost = 7;
            break;
        case 0x1e: // ld e,n
            e = fetchB();
            cost = 7;
            break;
        case 0x26: // ld h,n
            setH(fetchB());
            cost = 7;
            break;
        case 0x2e: // ld l,n
            setL(fetchB());
            cost = 7;
            break;

        case 0x36: // ld (hl),n
            bus.pokeb(hl, fetchB());
            cost = 10;
            break;
        case 0x01: // ld bc,nn
            setBC(fetchW());
            cost = 10;
            break;
        case 0x11: // ld de,nn
            setDE(fetchW());
            cost = 10;
            break;
        case 0x21: // ld hl,nn
            setHL(fetchW());
            cost = 10;
            break;
        case 0x31: // ld sp,nn
            sp = fetchW();
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
            bus.pokew(fetchW(), hl);
            cost = 16;
            break;
        case 0x2a: // ld hl,(nn)
            setHL(bus.peekw(fetchW()));
            cost = 16;
            break;

        case 0x32: // ld (nn),a
            bus.pokeb(fetchW(), a);
            cost = 13;
            break;
        case 0x3a: // ld a,(nn)
            a = bus.peekb(fetchW());
            cost = 13;
            break;

        case 0xe3: { // ex (sp),hl
            int t = bus.peekw(sp);
            bus.pokew(sp, hl);
            hl = t;
            cost = 19;
        }
            break;
        case 0xeb: { // ex de,hl
            int t = getDE();
            setDE(hl);
            hl = t;
            cost = 4;
        }
            break;

        case 0xf9: // ld sp,hl
            sp = hl;
            cost = 6;
            break;
        case 0xe9: // jp (hl)
            pc = hl;
            cost = 4;
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
            setH(inc8bit(getH()));
            cost = 4;
            break;
        case 0x2c: // inc l
            setL(inc8bit(getL()));
            cost = 4;
            break;
        case 0x34: // inc (hl)
            bus.pokeb(hl, inc8bit(bus.peekb(hl)));
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
            setH(dec8bit(getH()));
            cost = 4;
            break;
        case 0x2d: // dec l
            setL(dec8bit(getL()));
            cost = 4;
            break;
        case 0x35: // dec (hl)
            bus.pokeb(hl, dec8bit(bus.peekb(hl)));
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
            hl += 1;
            cost = 6;
            break;
        case 0x33: // inc sp
            sp = inc16bitInternal(sp);
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
            hl -= 1;
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
            a = add8bit(a, getH());
            cost = 4;
            break;
        case 0x85: // add a,l
            a = add8bit(a, getL());
            cost = 4;
            break;
        case 0x86: // add a,(hl)
            a = add8bit(a, bus.peekb(hl));
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
            a = adc8bit(a, getH());
            cost = 4;
            break;
        case 0x8d: // adc a,l
            a = adc8bit(a, getL());
            cost = 4;
            break;
        case 0x8e: // adc a,(hl)
            a = adc8bit(a, bus.peekb(hl));
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
            a = sub8bit(a, getH());
            cost = 4;
            break;
        case 0x95: // sub l
            a = sub8bit(a, getL());
            cost = 4;
            break;
        case 0x96: // sub (hl)
            a = sub8bit(a, bus.peekb(hl));
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
            a = sbc8bit(a, getH());
            cost = 4;
            break;
        case 0x9d: // sbc a,l
            a = sbc8bit(a, getL());
            cost = 4;
            break;
        case 0x9e: // sbc a,(hl)
            a = sbc8bit(a, bus.peekb(hl));
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
            cmp8bit(a, getH());
            cost = 4;
            break;
        case 0xbd: // cp l
            cmp8bit(a, getL());
            cost = 4;
            break;
        case 0xbe: // cp (hl)
            cmp8bit(a, bus.peekb(hl));
            cost = 7;
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
            a = and8bit(a, getH());
            cost = 4;
            break;
        case 0xa5: // and l
            a = and8bit(a, getL());
            cost = 4;
            break;
        case 0xa6: // and (hl)
            a = and8bit(a, bus.peekb(hl));
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
            a = xor8bit(a, getH());
            cost = 4;
            break;
        case 0xad: // xor l
            a = xor8bit(a, getL());
            cost = 4;
            break;
        case 0xae: // xor (hl)
            a = xor8bit(a, bus.peekb(hl));
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
            a = or8bit(a, getH());
            cost = 4;
            break;
        case 0xb5: // or l
            a = or8bit(a, getL());
            cost = 4;
            break;
        case 0xb6: // or (hl)
            a = or8bit(a, bus.peekb(hl));
            cost = 7;
            break;

        case 0xc6: // add a,n
            a = add8bit(a, fetchB());
            cost = 7;
            break;
        case 0xce: // adc a,n
            a = adc8bit(a, fetchB());
            cost = 7;
            break;
        case 0xd6: // sub n
            a = sub8bit(a, fetchB());
            cost = 7;
            break;
        case 0xde: // sbc a,n
            a = sbc8bit(a, fetchB());
            cost = 7;
            break;
        case 0xe6: // and n
            a = and8bit(a, fetchB());
            cost = 7;
            break;
        case 0xee: // xor n
            a = xor8bit(a, fetchB());
            cost = 7;
            break;
        case 0xf6: // or a,n
            a = or8bit(a, fetchB());
            cost = 7;
            break;
        case 0xfe: // cp a,n
            cmp8bit(a, fetchB());
            cost = 7;
            break;

        case 0x09: // add hl,bc
            hl = add16bit(hl, getBC());
            cost = 11;
            break;
        case 0x19: // add hl,de
            hl = add16bit(hl, getDE());
            cost = 11;
            break;
        case 0x29: // add hl,hl
            hl = add16bit(hl, hl);
            cost = 11;
            break;
        case 0x39: // add hl,sp
            hl = add16bit(hl, sp);
            cost = 11;
            break;

        case 0x27: { // daa
            int v = a;
            if (((v & 0x0f) > 9) || fh) {
                a = add8bitInternal(a, fn ? -6 : 6);
            }
            if ((v > 0x99) || fc) {
                a = add8bitInternal(a, fn ? -0x60 : 0x60);
            }

            fc = fc | (v > 0x99);
            fh = ((v ^ a) & 0x10) != 0;
            fp = isParity(a);
            fz = isZero(a);
            fs = isSign(a);

            cost = 4;
        }
            break;
        case 0x2f: // cpl
            fh = true;
            fn = true;
            a ^= 0xff;
            cost = 4;
            break;
        case 0x3f: // ccf
            fn = false;
            fh = fc;
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
                pc = fetchW();
            } else {
                pc = add16bitInternal(pc, 2);
            }
            cost = 10;
        }
            break;
        case 0xd2: { // jp nc,nn
            if (!fc) {
                pc = fetchW();
            } else {
                pc = add16bitInternal(pc, 2);
            }
            cost = 10;
        }
            break;
        case 0xca: { // jp z,nn
            if (fz) {
                pc = fetchW();
            } else {
                pc = add16bitInternal(pc, 2);
            }
            cost = 10;
        }
            break;
        case 0xc2: { // jp nz,nn
            if (!fz) {
                pc = fetchW();
            } else {
                pc = add16bitInternal(pc, 2);
            }
            cost = 10;
        }
            break;
        case 0xea: { // jp pe,nn
            if (fp) {
                pc = fetchW();
            } else {
                pc = add16bitInternal(pc, 2);
            }
            cost = 10;
        }
            break;
        case 0xe2: { // jp po,nn
            if (!fp) {
                pc = fetchW();
            } else {
                pc = add16bitInternal(pc, 2);
            }
            cost = 10;
        }
            break;
        case 0xfa: { // jp m,nn
            if (fs) {
                pc = fetchW();
            } else {
                pc = add16bitInternal(pc, 2);
            }
            cost = 10;
        }
            break;
        case 0xf2: { // jp p,nn
            if (!fs) {
                pc = fetchW();
            } else {
                pc = add16bitInternal(pc, 2);
            }
            cost = 10;
        }
            break;

        case 0xcd: { // call
            call();
            cost = 17;
        }
            break;
        case 0xdc: { // call c,nn
            if (fc) {
                call();
                cost = 17;
            } else {
                pc = add16bitInternal(pc, 2);
                cost = 10;
            }
        }
            break;
        case 0xd4: { // call nc,nn
            if (!fc) {
                call();
                cost = 17;
            } else {
                pc = add16bitInternal(pc, 2);
                cost = 10;
            }
        }
            break;
        case 0xcc: { // call z,nn
            if (fz) {
                call();
                cost = 17;
            } else {
                pc = add16bitInternal(pc, 2);
                cost = 10;
            }
        }
            break;
        case 0xc4: { // call nz,nn
            if (!fz) {
                call();
                cost = 17;
            } else {
                pc = add16bitInternal(pc, 2);
                cost = 10;
            }
        }
            break;
        case 0xec: { // call pe,nn
            if (fp) {
                call();
                cost = 17;
            } else {
                pc = add16bitInternal(pc, 2);
                cost = 10;
            }
        }
            break;
        case 0xe4: { // call po,nn
            if (!fp) {
                call();
                cost = 17;
            } else {
                pc = add16bitInternal(pc, 2);
                cost = 10;
            }
        }
            break;
        case 0xfc: { // call m,nn
            if (fs) {
                call();
                cost = 17;
            } else {
                pc = add16bitInternal(pc, 2);
                cost = 10;
            }
        }
            break;
        case 0xf4: { // call p,nn
            if (!fs) {
                call();
                cost = 17;
            } else {
                pc = add16bitInternal(pc, 2);
                cost = 10;
            }
        }
            break;

        case 0xc7: // rst 00h
            push(pc);
            pc = 0 * 8;
            cost = 11;
            break;
        case 0xcf: // rst 08h
            push(pc);
            pc = 1 * 8;
            cost = 11;
            break;
        case 0xd7: // rst 10h
            push(pc);
            pc = 2 * 8;
            cost = 11;
            break;
        case 0xdf: // rst 18h
            push(pc);
            pc = 3 * 8;
            cost = 11;
            break;
        case 0xe7: // rst 20h
            push(pc);
            pc = 4 * 8;
            cost = 11;
            break;
        case 0xef: // rst 28h
            push(pc);
            pc = 5 * 8;
            cost = 11;
            break;
        case 0xf7: // rst 30h
            push(pc);
            pc = 6 * 8;
            cost = 11;
            break;
        case 0xff: // rst 38h
            push(pc);
            pc = 7 * 8;
            cost = 11;
            break;

        case 0xc9: // ret
            pc = pop();
            cost = 10;
            break;
        case 0xd8: // ret c
            if (fc) {
                pc = pop();
                cost = 11;
            } else {
                cost = 5;
            }
            break;
        case 0xd0: // ret nc
            if (!fc) {
                pc = pop();
                cost = 11;
            } else {
                cost = 5;
            }
            break;
        case 0xc8: // ret z
            if (fz) {
                pc = pop();
                cost = 11;
            } else {
                cost = 5;
            }
            break;
        case 0xc0: // ret nz
            if (!fz) {
                pc = pop();
                cost = 11;
            } else {
                cost = 5;
            }
            break;
        case 0xe8: // ret pe
            if (fp) {
                pc = pop();
                cost = 11;
            } else {
                cost = 5;
            }
            break;
        case 0xe0: // ret po
            if (!fp) {
                pc = pop();
                cost = 11;
            } else {
                cost = 5;
            }
            break;
        case 0xf8: // ret m
            if (fs) {
                pc = pop();
                cost = 11;
            } else {
                cost = 5;
            }
            break;
        case 0xf0: // ret p
            if (!fs) {
                pc = pop();
                cost = 11;
            } else {
                cost = 5;
            }
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
            push(hl);
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
            hl = pop();
            cost = 10;
            break;
        case 0xf1: // pop af
            setAF(pop());
            cost = 10;
            break;

        case 0xdb: // in a,n
            a = bus.inp(fetchB());
            cost = 11;
            break;
        case 0xd3: // out n,a
            bus.outp(fetchB(), a);
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
            cost = 4;
Debug.printf("halt: %4x", dec16bitInternal(pc));
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
                byte v = (byte) fetchB();
                pc = add16bitInternal(pc, v);
                cost = 13;
            } else {
                pc = add16bitInternal(pc, 1);
                cost = 8;
            }
        }
            break;

        case 0x18: { // jr e
            byte v = (byte) fetchB();
            pc = add16bitInternal(pc, v);
            cost = 12;
        }
            break;
        case 0x20: { // jr nz,e
            if (!fz) {
                byte v = (byte) fetchB();
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
                byte v = (byte) fetchB();
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
                byte v = (byte) fetchB();
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
                byte v = (byte) fetchB();
                pc = add16bitInternal(pc, v);
                cost = 12;
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

            t = hl;
            hl = hl2;
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
Debug.printf("Unknown instruction: %02x", o);
            break;
        }

        return cost;
    }

    /** cb xx */
    private void exec_cb() {
        int v = fetchB();
        r = inc7Bits(r);

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
            setH(rlc(getH(), true));
            cost = 8;
            break;
        case 0x05: // rlc l
            setL(rlc(getL(), true));
            cost = 8;
            break;
        case 0x06: // rlc (hl)
            bus.pokeb(hl, rlc(bus.peekb(hl), true));
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
            setH(rrc(getH(), true));
            cost = 8;
            break;
        case 0x0d: // rrc l
            setL(rrc(getL(), true));
            cost = 8;
            break;
        case 0x0e: // rrc (hl)
            bus.pokeb(hl, rrc(bus.peekb(hl), true));
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
            setH(rl(getH(), true));
            cost = 8;
            break;
        case 0x15: // rl l
            setL(rl(getL(), true));
            cost = 8;
            break;
        case 0x16: // rl (hl)
            bus.pokeb(hl, rl(bus.peekb(hl), true));
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
            setH(rr(getH(), true));
            cost = 8;
            break;
        case 0x1d: // rr l
            setL(rr(getL(), true));
            cost = 8;
            break;
        case 0x1e: // rr (hl)
            bus.pokeb(hl, rr(bus.peekb(hl), true));
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
            setH(sla(getH()));
            cost = 8;
            break;
        case 0x25: // sla l
            setL(sla(getL()));
            cost = 8;
            break;
        case 0x26: // sla (hl)
            bus.pokeb(hl, sla(bus.peekb(hl)));
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
            setH(sra(getH()));
            cost = 8;
            break;
        case 0x2d: // sra l
            setL(sra(getL()));
            cost = 8;
            break;
        case 0x2e: // sra (hl)
            bus.pokeb(hl, sra(bus.peekb(hl)));
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
            setH(srl(getH()));
            cost = 8;
            break;
        case 0x3d: // srl l
            setL(srl(getL()));
            cost = 8;
            break;
        case 0x3e: // srl (hl)
            bus.pokeb(hl, srl(bus.peekb(hl)));
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
            bit(0, getH());
            cost = 8;
            break;
        case 0x45: // bit 0,l
            bit(0, getL());
            cost = 8;
            break;
        case 0x46: // bit 0,(hl)
            bit(0, bus.peekb(hl));
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
            bit(1, getH());
            cost = 8;
            break;
        case 0x4d: // bit 1,l
            bit(1, getL());
            cost = 8;
            break;
        case 0x4e: // bit 1,(hl)
            bit(1, bus.peekb(hl));
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
            bit(2, getH());
            cost = 8;
            break;
        case 0x55: // bit 2,l
            bit(2, getL());
            cost = 8;
            break;
        case 0x56: // bit 2,(hl)
            bit(2, bus.peekb(hl));
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
            bit(3, getH());
            cost = 8;
            break;
        case 0x5d: // bit 3,l
            bit(3, getL());
            cost = 8;
            break;
        case 0x5e: // bit 3,(hl)
            bit(3, bus.peekb(hl));
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
            bit(4, getH());
            cost = 8;
            break;
        case 0x65: // bit 4,l
            bit(4, getL());
            cost = 8;
            break;
        case 0x66: // bit 4,(hl)
            bit(4, bus.peekb(hl));
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
            bit(5, getH());
            cost = 8;
            break;
        case 0x6d: // bit 5,l
            bit(5, getL());
            cost = 8;
            break;
        case 0x6e: // bit 5,(hl)
            bit(5, bus.peekb(hl));
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
            bit(6, getH());
            cost = 8;
            break;
        case 0x75: // bit 6,l
            bit(6, getL());
            cost = 8;
            break;
        case 0x76: // bit 6,(hl)
            bit(6, bus.peekb(hl));
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
            bit(7, getH());
            cost = 8;
            break;
        case 0x7d: // bit 7,l
            bit(7, getL());
            cost = 8;
            break;
        case 0x7e: // bit 7,(hl)
            bit(7, bus.peekb(hl));
            cost = 12;
            break;
        case 0x7f: // bit 7,a
            bit(7, a);
            cost = 8;
            break;

        case 0x80: // res 0,b
            b = resetBit(0, b);
            cost = 8;
            break;
        case 0x81: // res 0,c
            c = resetBit(0, c);
            cost = 8;
            break;
        case 0x82: // res 0,d
            d = resetBit(0, d);
            cost = 8;
            break;
        case 0x83: // res 0,e
            e = resetBit(0, e);
            cost = 8;
            break;
        case 0x84: // res 0,h
            setH(resetBit(0, getH()));
            cost = 8;
            break;
        case 0x85: // res 0,l
            setL(resetBit(0, getL()));
            cost = 8;
            break;
        case 0x86: // res 0,(hl)
            bus.pokeb(hl, resetBit(0, bus.peekb(hl)));
            cost = 15;
            break;
        case 0x87: // res 0,a
            a = resetBit(0, a);
            cost = 8;
            break;

        case 0x88: // res 1,b
            b = resetBit(1, b);
            cost = 8;
            break;
        case 0x89: // res 1,c
            c = resetBit(1, c);
            cost = 8;
            break;
        case 0x8a: // res 1,d
            d = resetBit(1, d);
            cost = 8;
            break;
        case 0x8b: // res 1,e
            e = resetBit(1, e);
            cost = 8;
            break;
        case 0x8c: // res 1,h
            setH(resetBit(1, getH()));
            cost = 8;
            break;
        case 0x8d: // res 1,l
            setL(resetBit(1, getL()));
            cost = 8;
            break;
        case 0x8e: // res 1,(hl)
            bus.pokeb(hl, resetBit(1, bus.peekb(hl)));
            cost = 15;
            break;
        case 0x8f: // res 1,a
            a = resetBit(1, a);
            cost = 8;
            break;

        case 0x90: // res 2,b
            b = resetBit(2, b);
            cost = 8;
            break;
        case 0x91: // res 2,c
            c = resetBit(2, c);
            cost = 8;
            break;
        case 0x92: // res 2,d
            d = resetBit(2, d);
            cost = 8;
            break;
        case 0x93: // res 2,e
            e = resetBit(2, e);
            cost = 8;
            break;
        case 0x94: // res 2,h
            setH(resetBit(2, getH()));
            cost = 8;
            break;
        case 0x95: // res 2,l
            setL(resetBit(2, getL()));
            cost = 8;
            break;
        case 0x96: // res 2,(hl)
            bus.pokeb(hl, resetBit(2, bus.peekb(hl)));
            cost = 15;
            break;
        case 0x97: // res 2,a
            a = resetBit(2, a);
            cost = 8;
            break;

        case 0x98: // res 3,b
            b = resetBit(3, b);
            cost = 8;
            break;
        case 0x99: // res 3,c
            c = resetBit(3, c);
            cost = 8;
            break;
        case 0x9a: // res 3,d
            d = resetBit(3, d);
            cost = 8;
            break;
        case 0x9b: // res 3,e
            e = resetBit(3, e);
            cost = 8;
            break;
        case 0x9c: // res 3,h
            setH(resetBit(3, getH()));
            cost = 8;
            break;
        case 0x9d: // res 3,l
            setL(resetBit(3, getL()));
            cost = 8;
            break;
        case 0x9e: // res 3,(hl)
            bus.pokeb(hl, resetBit(3, bus.peekb(hl)));
            cost = 15;
            break;
        case 0x9f: // res 3,a
            a = resetBit(3, a);
            cost = 8;
            break;

        case 0xa0: // res 4,b
            b = resetBit(4, b);
            cost = 8;
            break;
        case 0xa1: // res 4,c
            c = resetBit(4, c);
            cost = 8;
            break;
        case 0xa2: // res 4,d
            d = resetBit(4, d);
            cost = 8;
            break;
        case 0xa3: // res 4,e
            e = resetBit(4, e);
            cost = 8;
            break;
        case 0xa4: // res 4,h
            setH(resetBit(4, getH()));
            cost = 8;
            break;
        case 0xa5: // res 4,l
            setL(resetBit(4, getL()));
            cost = 8;
            break;
        case 0xa6: // res 4,(hl)
            bus.pokeb(hl, resetBit(4, bus.peekb(hl)));
            cost = 15;
            break;
        case 0xa7: // res 4,a
            a = resetBit(4, a);
            cost = 8;
            break;

        case 0xa8: // res 5,b
            b = resetBit(5, b);
            cost = 8;
            break;
        case 0xa9: // res 5,c
            c = resetBit(5, c);
            cost = 8;
            break;
        case 0xaa: // res 5,d
            d = resetBit(5, d);
            cost = 8;
            break;
        case 0xab: // res 5,e
            e = resetBit(5, e);
            cost = 8;
            break;
        case 0xac: // res 5,h
            setH(resetBit(5, getH()));
            cost = 8;
            break;
        case 0xad: // res 5,l
            setL(resetBit(5, getL()));
            cost = 8;
            break;
        case 0xae: // res 5,(hl)
            bus.pokeb(hl, resetBit(5, bus.peekb(hl)));
            cost = 15;
            break;
        case 0xaf: // res 5,a
            a = resetBit(5, a);
            cost = 8;
            break;

        case 0xb0: // res 6,b
            b = resetBit(6, b);
            cost = 8;
            break;
        case 0xb1: // res 6,c
            c = resetBit(6, c);
            cost = 8;
            break;
        case 0xb2: // res 6,d
            d = resetBit(6, d);
            cost = 8;
            break;
        case 0xb3: // res 6,e
            e = resetBit(6, e);
            cost = 8;
            break;
        case 0xb4: // res 6,h
            setH(resetBit(6, getH()));
            cost = 8;
            break;
        case 0xb5: // res 6,l
            setL(resetBit(6, getL()));
            cost = 8;
            break;
        case 0xb6: // res 6,(hl)
            bus.pokeb(hl, resetBit(6, bus.peekb(hl)));
            cost = 15;
            break;
        case 0xb7: // res 6,a
            a = resetBit(6, a);
            cost = 8;
            break;

        case 0xb8: // res 7,b
            b = resetBit(7, b);
            cost = 8;
            break;
        case 0xb9: // res 7,c
            c = resetBit(7, c);
            cost = 8;
            break;
        case 0xba: // res 7,d
            d = resetBit(7, d);
            cost = 8;
            break;
        case 0xbb: // res 7,e
            e = resetBit(7, e);
            cost = 8;
            break;
        case 0xbc: // res 7,h
            setH(resetBit(7, getH()));
            cost = 8;
            break;
        case 0xbd: // res 7,l
            setL(resetBit(7, getL()));
            cost = 8;
            break;
        case 0xbe: // res 7,(hl)
            bus.pokeb(hl, resetBit(7, bus.peekb(hl)));
            cost = 15;
            break;
        case 0xbf: // res 7,a
            a = resetBit(7, a);
            cost = 8;
            break;

        case 0xc0: // set 0,b
            b = setBit(0, b);
            cost = 8;
            break;
        case 0xc1: // set 0,c
            c = setBit(0, c);
            cost = 8;
            break;
        case 0xc2: // set 0,d
            d = setBit(0, d);
            cost = 8;
            break;
        case 0xc3: // set 0,e
            e = setBit(0, e);
            cost = 8;
            break;
        case 0xc4: // set 0,h
            setH(setBit(0, getH()));
            cost = 8;
            break;
        case 0xc5: // set 0,l
            setL(setBit(0, getL()));
            cost = 8;
            break;
        case 0xc6: // set 0,(hl)
            bus.pokeb(hl, setBit(0, bus.peekb(hl)));
            cost = 15;
            break;
        case 0xc7: // set 0,a
            a = setBit(0, a);
            cost = 8;
            break;

        case 0xc8: // set 1,b
            b = setBit(1, b);
            cost = 8;
            break;
        case 0xc9: // set 1,c
            c = setBit(1, c);
            cost = 8;
            break;
        case 0xca: // set 1,d
            d = setBit(1, d);
            cost = 8;
            break;
        case 0xcb: // set 1,e
            e = setBit(1, e);
            cost = 8;
            break;
        case 0xcc: // set 1,h
            setH(setBit(1, getH()));
            cost = 8;
            break;
        case 0xcd: // set 1,l
            setL(setBit(1, getL()));
            cost = 8;
            break;
        case 0xce: // set 1,(hl)
            bus.pokeb(hl, setBit(1, bus.peekb(hl)));
            cost = 15;
            break;
        case 0xcf: // set 1,a
            a = setBit(1, a);
            cost = 8;
            break;

        case 0xd0: // set 2,b
            b = setBit(2, b);
            cost = 8;
            break;
        case 0xd1: // set 2,c
            c = setBit(2, c);
            cost = 8;
            break;
        case 0xd2: // set 2,d
            d = setBit(2, d);
            cost = 8;
            break;
        case 0xd3: // set 2,e
            e = setBit(2, e);
            cost = 8;
            break;
        case 0xd4: // set 2,h
            setH(setBit(2, getH()));
            cost = 8;
            break;
        case 0xd5: // set 2,l
            setL(setBit(2, getL()));
            cost = 8;
            break;
        case 0xd6: // set 2,(hl)
            bus.pokeb(hl, setBit(2, bus.peekb(hl)));
            cost = 15;
            break;
        case 0xd7: // set 2,a
            a = setBit(2, a);
            cost = 8;
            break;

        case 0xd8: // set 3,b
            b = setBit(3, b);
            cost = 8;
            break;
        case 0xd9: // set 3,c
            c = setBit(3, c);
            cost = 8;
            break;
        case 0xda: // set 3,d
            d = setBit(3, d);
            cost = 8;
            break;
        case 0xdb: // set 3,e
            e = setBit(3, e);
            cost = 8;
            break;
        case 0xdc: // set 3,h
            setH(setBit(3, getH()));
            cost = 8;
            break;
        case 0xdd: // set 3,l
            setL(setBit(3, getL()));
            cost = 8;
            break;
        case 0xde: // set 3,(hl)
            bus.pokeb(hl, setBit(3, bus.peekb(hl)));
            cost = 15;
            break;
        case 0xdf: // set 3,a
            a = setBit(3, a);
            cost = 8;
            break;

        case 0xe0: // set 4,b
            b = setBit(4, b);
            cost = 8;
            break;
        case 0xe1: // set 4,c
            c = setBit(4, c);
            cost = 8;
            break;
        case 0xe2: // set 4,d
            d = setBit(4, d);
            cost = 8;
            break;
        case 0xe3: // set 4,e
            e = setBit(4, e);
            cost = 8;
            break;
        case 0xe4: // set 4,h
            setH(setBit(4, getH()));
            cost = 8;
            break;
        case 0xe5: // set 4,l
            setL(setBit(4, getL()));
            cost = 8;
            break;
        case 0xe6: // set 4,(hl)
            bus.pokeb(hl, setBit(4, bus.peekb(hl)));
            cost = 15;
            break;
        case 0xe7: // set 4,a
            a = setBit(4, a);
            cost = 8;
            break;

        case 0xe8: // set 5,b
            b = setBit(5, b);
            cost = 8;
            break;
        case 0xe9: // set 5,c
            c = setBit(5, c);
            cost = 8;
            break;
        case 0xea: // set 5,d
            d = setBit(5, d);
            cost = 8;
            break;
        case 0xeb: // set 5,e
            e = setBit(5, e);
            cost = 8;
            break;
        case 0xec: // set 5,h
            setH(setBit(5, getH()));
            cost = 8;
            break;
        case 0xed: // set 5,l
            setL(setBit(5, getL()));
            cost = 8;
            break;
        case 0xee: // set 5,(hl)
            bus.pokeb(hl, setBit(5, bus.peekb(hl)));
            cost = 15;
            break;
        case 0xef: // set 5,a
            a = setBit(5, a);
            cost = 8;
            break;

        case 0xf0: // set 6,b
            b = setBit(6, b);
            cost = 8;
            break;
        case 0xf1: // set 6,c
            c = setBit(6, c);
            cost = 8;
            break;
        case 0xf2: // set 6,d
            d = setBit(6, d);
            cost = 8;
            break;
        case 0xf3: // set 6,e
            e = setBit(6, e);
            cost = 8;
            break;
        case 0xf4: // set 6,h
            setH(setBit(6, getH()));
            cost = 8;
            break;
        case 0xf5: // set 6,l
            setL(setBit(6, getL()));
            cost = 8;
            break;
        case 0xf6: // set 6,(hl)
            bus.pokeb(hl, setBit(6, bus.peekb(hl)));
            cost = 15;
            break;
        case 0xf7: // set 6,a
            a = setBit(6, a);
            cost = 8;
            break;

        case 0xf8: // set 7,b
            b = setBit(7, b);
            cost = 8;
            break;
        case 0xf9: // set 7,c
            c = setBit(7, c);
            cost = 8;
            break;
        case 0xfa: // set 7,d
            d = setBit(7, d);
            cost = 8;
            break;
        case 0xfb: // set 7,e
            e = setBit(7, e);
            cost = 8;
            break;
        case 0xfc: // set 7,h
            setH(setBit(7, getH()));
            cost = 8;
            break;
        case 0xfd: // set 7,l
            setL(setBit(7, getL()));
            cost = 8;
            break;
        case 0xfe: // set 7,(hl)
            bus.pokeb(hl, setBit(7, bus.peekb(hl)));
            cost = 15;
            break;
        case 0xff: // set 7,a
            a = setBit(7, a);
            cost = 8;
            break;

        default:
Debug.printf("Unknown instruction: CB %02x", v);
            break;
        }
    }

    /** dd xx [b|ww] */
    private void exec_dd() {
        int v = fetchB();
        r = inc7Bits(r);

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
            ix = fetchW();
            cost = 14;
            break;
        case 0x22: // ld (nn),ix
            bus.pokew(fetchW(), ix);
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
            ix = bus.peekw(fetchW());
            cost = 20;
            break;
        case 0x2b: // dec ix
            ix = sub16bitInternal(ix, 1);
            cost = 10;
            break;
        case 0x34: { // inc (ix+d)
            int i = index(ix, fetchB());
            int b = bus.peekb(i);
            bus.pokeb(i, inc8bit(b));
            cost = 23;
        }
            break;
        case 0x35: { // dec (ix+d)
            int i = index(ix, fetchB());
            int b = bus.peekb(i);
            bus.pokeb(i, dec8bit(b));
            cost = 23;
        }
            break;
        case 0x36: { // ld (ix+d),n
            int i = index(ix, fetchB());
            bus.pokeb(i, fetchB());
            cost = 19;
        }
            break;
        case 0x39: // add ix,sp
            ix = add16bit(ix, sp);
            cost = 15;
            break;

        case 0x46: { // ld b,(ix+d)
            int i = index(ix, fetchB());
            b = bus.peekb(i);
            cost = 19;
        }
            break;
        case 0x4e: { // ld c,(ix+d)
            int i = index(ix, fetchB());
            c = bus.peekb(i);
            cost = 19;
        }
            break;
        case 0x56: { // ld d,(ix+d)
            int i = index(ix, fetchB());
            d = bus.peekb(i);
            cost = 19;
        }
            break;
        case 0x5e: { // ld e,(ix+d)
            int i = index(ix, fetchB());
            e = bus.peekb(i);
            cost = 19;
        }
            break;
        case 0x66: { // ld h,(ix+d)
            int i = index(ix, fetchB());
            setH(bus.peekb(i));
            cost = 19;
        }
            break;
        case 0x6e: { // ld l,(ix+d)
            int i = index(ix, fetchB());
            setL(bus.peekb(i));
            cost = 19;
        }
            break;

        case 0x70: { // ld (ix+d),b
            int i = index(ix, fetchB());
            bus.pokeb(i, b);
            cost = 19;
        }
            break;
        case 0x71: { // ld (ix+d),c
            int i = index(ix, fetchB());
            bus.pokeb(i, c);
            cost = 19;
        }
            break;
        case 0x72: { // ld (ix+d),d
            int i = index(ix, fetchB());
            bus.pokeb(i, d);
            cost = 19;
        }
            break;
        case 0x73: { // ld (ix+d),e
            int i = index(ix, fetchB());
            bus.pokeb(i, e);
            cost = 19;
        }
            break;
        case 0x74: { // ld (ix+d),h
            int i = index(ix, fetchB());
            bus.pokeb(i, getH());
            cost = 19;
        }
            break;
        case 0x75: { // ld (ix+d),l
            int i = index(ix, fetchB());
            bus.pokeb(i, getL());
            cost = 19;
        }
            break;
        case 0x77: { // ld (ix+d),a
            int i = index(ix, fetchB());
            bus.pokeb(i, a);
            cost = 19;
        }
            break;

        case 0x7e: { // ld a,(ix+d)
            int i = index(ix, fetchB());
            a = bus.peekb(i);
            cost = 19;
        }
            break;
        case 0x86: { // add a,(ix+d)
            int i = index(ix, fetchB());
            int b = bus.peekb(i);
            a = add8bit(a, b);
            cost = 19;
        }
            break;
        case 0x8e: { // adc a,(ix+d)
            int i = index(ix, fetchB());
            int b = bus.peekb(i);
            a = adc8bit(a, b);
            cost = 19;
        }
            break;
        case 0x96: { // sub a,(ix+d)
            int i = index(ix, fetchB());
            int b = bus.peekb(i);
            a = sub8bit(a, b);
            cost = 19;
        }
            break;
        case 0x9e: { // sbc a,(ix+d)
            int i = index(ix, fetchB());
            int b = bus.peekb(i);
            a = sbc8bit(a, b);
            cost = 19;
        }
            break;
        case 0xa6: { // and (ix+d)
            int i = index(ix, fetchB());
            int b = bus.peekb(i);
            a = and8bit(a, b);
            cost = 19;
        }
            break;
        case 0xae: { // xor (ix+d)
            int i = index(ix, fetchB());
            int b = bus.peekb(i);
            a = xor8bit(a, b);
            cost = 19;
        }
            break;
        case 0xb6: { // or (ix+d)
            int i = index(ix, fetchB());
            int b = bus.peekb(i);
            a = or8bit(a, b);
            cost = 19;
        }
            break;
        case 0xbe: { // cp (ix+d)
            int i = index(ix, fetchB());
            int b = bus.peekb(i);
            cmp8bit(a, b);
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
Debug.printf("Unknown instruction: DD %02x", v);
            break;
        }
    }

    /**
     * dd cb b xx
     * <p>
     * `pc += 2` at last, no need to do that for 2nd fetch
     * </p>
     */
    private void exec_ddcb() {
        int v = bus.peekb(inc16bitInternal(pc));

        switch (v) {
        case 0x06: { // rlc (ix+d)
            int i = index(ix, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, rlc(b, true));
            cost = 23;
        }
            break;
        case 0x0e: { // rrc (ix+d)
            int i = index(ix, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, rrc(b, true));
            cost = 23;
        }
            break;
        case 0x16: { // rl (ix+d)
            int i = index(ix, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, rl(b, true));
            cost = 23;
        }
            break;
        case 0x1e: { // rr (ix+d)
            int i = index(ix, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, rr(b, true));
            cost = 23;
        }
            break;
        case 0x26: { // sla (ix+d)
            int i = index(ix, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, sla(b));
            cost = 23;
        }
            break;
        case 0x2e: { // sra (ix+d)
            int i = index(ix, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, sra(b));
            cost = 23;
        }
            break;
        case 0x3e: { // srl (ix+d)
            int i = index(ix, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, srl(b));
            cost = 23;
        }
            break;
        case 0x46: { // bit 0,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int b = bus.peekb(i);
            bit(0, b);
            cost = 20;
        }
            break;
        case 0x4e: { // bit 1,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int b = bus.peekb(i);
            bit(1, b);
            cost = 20;
        }
            break;
        case 0x56: { // bit 2,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int b = bus.peekb(i);
            bit(2, b);
            cost = 20;
        }
            break;
        case 0x5e: { // bit 3,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int b = bus.peekb(i);
            bit(3, b);
            cost = 20;
        }
            break;
        case 0x66: { // bit 4,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int b = bus.peekb(i);
            bit(4, b);
            cost = 20;
        }
            break;
        case 0x6e: { // bit 5,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int b = bus.peekb(i);
            bit(5, b);
            cost = 20;
        }
            break;
        case 0x76: { // bit 6,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int b = bus.peekb(i);
            bit(6, b);
            cost = 20;
        }
            break;
        case 0x7e: { // bit 7,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int b = bus.peekb(i);
            bit(7, b);
            cost = 20;
        }
            break;
        case 0x86: { // res 0,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, resetBit(0, b));
            cost = 23;
        }
            break;
        case 0x8e: { // res 1,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, resetBit(1, b));
            cost = 23;
        }
            break;
        case 0x96: { // res 2,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, resetBit(2, b));
            cost = 23;
        }
            break;
        case 0x9e: { // res 3,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, resetBit(3, b));
            cost = 23;
        }
            break;
        case 0xa6: { // res 4,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, resetBit(4, b));
            cost = 23;
        }
            break;
        case 0xae: { // res 5,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, resetBit(5, b));
            cost = 23;
        }
            break;
        case 0xb6: { // res 6,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, resetBit(6, b));
            cost = 23;
        }
            break;
        case 0xbe: { // res 7,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, resetBit(7, b));
            cost = 23;
        }
            break;
        case 0xc6: { // set 0,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, setBit(0, b));
            cost = 23;
        }
            break;
        case 0xce: { // set 1,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, setBit(1, b));
            cost = 23;
        }
            break;
        case 0xd6: { // set 2,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, setBit(2, b));
            cost = 23;
        }
            break;
        case 0xde: { // set 3,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, setBit(3, b));
            cost = 23;
        }
            break;
        case 0xe6: { // set 4,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, setBit(4, b));
            cost = 23;
        }
            break;
        case 0xee: { // set 5,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, setBit(5, b));
            cost = 23;
        }
            break;
        case 0xf6: { // set 6,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, setBit(6, b));
            cost = 23;
        }
            break;
        case 0xfe: { // set 7,(ix+d)
            int i = index(ix, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, setBit(7, b));
            cost = 23;
        }
            break;

        default:
Debug.printf("Unknown instruction: DD CB %02x", v);
            break;
        }

        pc = add16bitInternal(pc, 2);
    }

    /** ed xx [w] */
    private void exec_ed() {
        int o = fetchB();
        r = inc7Bits(r);

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
            setH(inpc());
            cost = 12;
            break;
        case 0x68: // in l,(c)
            setL(inpc());
            cost = 12;
            break;
        case 0x70: // in f,(c)
            inpc();
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
            bus.outp(getBC(), getH());
            cost = 12;
            break;
        case 0x69: // out (c),l
            bus.outp(getBC(), getL());
            cost = 12;
            break;
        case 0x79: // out (c),a
            bus.outp(getBC(), a);
            cost = 12;
            break;
        case 0x71: // out (c),0
            bus.outp(getBC(), 0);
            cost = 12;
            break;

        case 0x42: // sbc hl,bc
            hl = sbc16bit(hl, getBC());
            cost = 15;
            break;
        case 0x52: // sbc hl,de
            hl = sbc16bit(hl, getDE());
            cost = 15;
            break;
        case 0x62: // sbc hl,hl
            hl = sbc16bit(hl, hl);
            cost = 15;
            break;
        case 0x72: // sbc hl,sp
            hl = sbc16bit(hl, sp);
            cost = 15;
            break;

        case 0x4a: // adc hl,bc
            hl = adc16bit(hl, getBC());
            cost = 15;
            break;
        case 0x5a: // adc hl,de
            hl = adc16bit(hl, getDE());
            cost = 15;
            break;
        case 0x6a: // adc hl,hl
            hl = adc16bit(hl, hl);
            cost = 15;
            break;
        case 0x7a: // adc hl,sp
            hl = adc16bit(hl, sp);
            cost = 15;
            break;

        case 0x43: // ld (nn),bc
            bus.pokew(fetchW(), getBC());
            cost = 20;
            break;
        case 0x53: // ld (nn),de
            bus.pokew(fetchW(), getDE());
            cost = 20;
            break;
        case 0x73: // ld (nn),sp
            bus.pokew(fetchW(), sp);
            cost = 20;
            break;

        case 0x4b: // ld bc,(nn)
            setBC(bus.peekw(fetchW()));
            cost = 20;
            break;
        case 0x5b: // ld de,(nn)
            setDE(bus.peekw(fetchW()));
            cost = 20;
            break;
        case 0x7b: // ld sp,(nn)
            sp = bus.peekw(fetchW());
            cost = 20;
            break;

        case 0x44: { // neg
            int v = a;
            a = -a & 0xff;

            fn = true;
            fh = isHalfC(v ^ a);
            fp = a == 0x80;
            fs = isSign(a);
            fz = isZero(a);
            fc = (v != 0);

            cost = 8;
        }
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
            a = r;

            fn = false;
            fp = iff2;
            fs = isSign(a);
            fz = isZero(a);
            fh = false;

            cost = 9;
            break;

        case 0x67: { // rrd
            int v = bus.peekb(hl);

            bus.pokeb(hl, ((v & 0xf0) >> 4) | ((a & 0x0f) << 4));
            a = (a & 0xf0) | (v & 0x0f);

            fp = isParity(a);
            fs = isSign(a);
            fz = isZero(a);
            fh = false;
            fn = false;

            cost = 18;
        }
            break;
        case 0x6f: { // rld
            int v = bus.peekb(hl);

            bus.pokeb(hl, ((v & 0x0f) << 4) | (a & 0x0f));
            a = (a & 0xf0) | ((v & 0xf0) >> 4);

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
            op_eda0();
            if (fp) {
                pc = sub16bitInternal(pc, 2);
                cost = 21;
            } else {
                cost = 16;
            }
            break;
        case 0xb1: // cpir
            op_eda1();
            if (fp && !fz) {
                pc = sub16bitInternal(pc, 2);
                cost = 21;
            } else {
                cost = 16;
            }
            break;
        case 0xb2: // inir
            op_eda2();
            if (!fz) {
                pc = sub16bitInternal(pc, 2);
                cost = 21;
            } else {
                cost = 16;
            }
            break;
        case 0xb3: // otir
            op_eda3();
            if (!fz) {
                pc = sub16bitInternal(pc, 2);
                cost = 21;
            } else {
                cost = 16;
            }
            break;

        case 0xb8: // lddr
            op_eda8();
            if (fp) {
                pc = sub16bitInternal(pc, 2);
                cost = 21;
            } else {
                cost = 16;
            }
            break;
        case 0xb9: // cpdr
            op_eda9();
            if (fp && !fz) {
                pc = sub16bitInternal(pc, 2);
                cost = 21;
            } else {
                cost = 16;
            }
            break;
        case 0xba: // indr
            op_edaa();
            if (!fz) {
                pc = sub16bitInternal(pc, 2);
                cost = 21;
            } else {
                cost = 16;
            }
            break;
        case 0xbb: // otdr
            op_edab();
            if (!fz) {
                pc = sub16bitInternal(pc, 2);
                cost = 21;
            } else {
                cost = 16;
            }
            break;

        default:
Debug.printf("Unknown instruction: ED %02x", o);
            break;
        }
    }

    /** ldi */
    private void op_eda0() {
        bus.pokeb(getDE(), bus.peekb(hl));

        setDE(inc16bitInternal(getDE()));
        hl = inc16bitInternal(hl);
        setBC(dec16bitInternal(getBC()));

        fp = getBC() != 0;
        fh = false;
        fn = false;
    }

    /** cpi */
    private void op_eda1() {
        int o = bus.peekb(hl);
        int w = a - o;
        int v = w & 0xff;

        hl = inc16bitInternal(hl);
        setBC(dec16bitInternal(getBC()));

        fp = getBC() != 0;
        fh = isHalfC(a ^ v ^ o);
        fz = isZero(v);
        fs = isSign(v);
        fn = true;
    }

    /** ini */
    private void op_eda2() {
        bus.pokeb(hl, inpc());

        hl = inc16bitInternal(hl);
        b = dec8bitInternal(b);

        fz = isZero(b);
        fn = true;
        fs = isSign(b);
    }

    /** outi */
    private void op_eda3() {
        bus.outp(c, bus.peekb(hl));

        hl = inc16bitInternal(hl);
        b = dec8bitInternal(b);

        fz = isZero(b);
        fn = true;
        fs = isSign(b);
    }

    /** ldd */
    private void op_eda8() {
        bus.pokeb(getDE(), bus.peekb(hl));

        setDE(dec16bitInternal(getDE()));
        hl = dec16bitInternal(hl);
        setBC(dec16bitInternal(getBC()));

        fp = getBC() != 0;
        fh = false;
        fn = false;
    }

    /** cpd */
    private void op_eda9() {
        int o = bus.peekb(hl);
        int w = a - o;
        int v = w & 0xff;

        hl = dec16bitInternal(hl);
        setBC(dec16bitInternal(getBC()));

        fp = getBC() != 0;
        fh = isHalfC(a ^ v ^ o);
        fz = isZero(v);
        fs = isSign(v);
        fn = true;
    }

    /** ind */
    private void op_edaa() {
        bus.pokeb(hl, inpc());

        hl = dec16bitInternal(hl);
        b = dec8bitInternal(b);

        fz = isZero(b);
        fn = true;
        fs = isSign(b);
    }

    /** outd */
    private void op_edab() {
        bus.outp(c, bus.peekb(hl));

        hl = dec16bitInternal(hl);
        b = dec8bitInternal(b);

        fz = isZero(b);
        fn = true;
        fs = isSign(b);
    }

    /** fd xx [b|ww] */
    private void exec_fd() {
        int v = fetchB();
        r = inc7Bits(r);

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
            iy = fetchW();
            cost = 14;
            break;
        case 0x22: // ld (nn),iy
            bus.pokew(fetchW(), iy);
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
            iy = bus.peekw(fetchW());
            cost = 20;
            break;
        case 0x2b: // dec iy
            iy = sub16bitInternal(iy, 1);
            cost = 10;
            break;
        case 0x34: { // inc (iy+d)
            int i = index(iy, fetchB());
            int b = bus.peekb(i);
            bus.pokeb(i, inc8bit(b));
            cost = 23;
        }
            break;
        case 0x35: { // dec (iy+d)
            int i = index(iy, fetchB());
            int b = bus.peekb(i);
            bus.pokeb(i, dec8bit(b));
            cost = 23;
        }
            break;
        case 0x36: { // ld (iy+d),n
            int i = index(iy, fetchB());
            bus.pokeb(i, fetchB());
            cost = 19;
        }
            break;
        case 0x39: { // add iy,sp
            iy = add16bit(iy, sp);
            cost = 15;
        }
            break;

        case 0x46: { // ld b,(iy+d)
            int i = index(iy, fetchB());
            b = bus.peekb(i);
            cost = 19;
        }
            break;
        case 0x4e: { // ld c,(iy+d)
            int i = index(iy, fetchB());
            c = bus.peekb(i);
            cost = 19;
        }
            break;
        case 0x56: { // ld d,(iy+d)
            int i = index(iy, fetchB());
            d = bus.peekb(i);
            cost = 19;
        }
            break;
        case 0x5e: { // ld e,(iy+d)
            int i = index(iy, fetchB());
            e = bus.peekb(i);
            cost = 19;
        }
            break;
        case 0x66: { // ld h,(iy+d)
            int i = index(iy, fetchB());
            setH(bus.peekb(i));
            cost = 19;
        }
            break;
        case 0x6e: { // ld l,(iy+d)
            int i = index(iy, fetchB());
            setL(bus.peekb(i));
            cost = 19;
        }
            break;

        case 0x70: { // ld (iy+d),b
            int i = index(iy, fetchB());
            bus.pokeb(i, b);
            cost = 19;
        }
            break;
        case 0x71: { // ld (iy+d),c
            int i = index(iy, fetchB());
            bus.pokeb(i, c);
            cost = 19;
        }
            break;
        case 0x72: { // ld (iy+d),d
            int i = index(iy, fetchB());
            bus.pokeb(i, d);
            cost = 19;
        }
            break;
        case 0x73: { // ld (iy+d),e
            int i = index(iy, fetchB());
            bus.pokeb(i, e);
            cost = 19;
        }
            break;
        case 0x74: { // ld (iy+d),h
            int i = index(iy, fetchB());
            bus.pokeb(i, getH());
            cost = 19;
        }
            break;
        case 0x75: { // ld (iy+d),l
            int i = index(iy, fetchB());
            bus.pokeb(i, getL());
            cost = 19;
        }
            break;
        case 0x77: { // ld (iy+d),a
            int i = index(iy, fetchB());
            bus.pokeb(i, a);
            cost = 19;
        }
            break;

        case 0x7e: { // ld a,(iy+d)
            int i = index(iy, fetchB());
            a = bus.peekb(i);
            cost = 19;
        }
            break;
        case 0x86: { // add a,(iy+d)
            int i = index(iy, fetchB());
            int b = bus.peekb(i);
            a = add8bit(a, b);
            cost = 19;
        }
            break;
        case 0x8e: { // adc a,(iy+d)
            int i = index(iy, fetchB());
            int b = bus.peekb(i);
            a = adc8bit(a, b);
            cost = 19;
        }
            break;
        case 0x96: { // sub (iy+d)
            int i = index(iy, fetchB());
            int b = bus.peekb(i);
            a = sub8bit(a, b);
            cost = 19;
        }
            break;
        case 0x9e: { // sbc a,(iy+d)
            int i = index(iy, fetchB());
            int b = bus.peekb(i);
            a = sbc8bit(a, b);
            cost = 19;
        }
            break;
        case 0xa6: { // and (iy+d)
            int i = index(iy, fetchB());
            int b = bus.peekb(i);
            a = and8bit(a, b);
            cost = 19;
        }
            break;
        case 0xae: { // xor (iy+d)
            int i = index(iy, fetchB());
            int b = bus.peekb(i);
            a = xor8bit(a, b);
            cost = 19;
        }
            break;
        case 0xb6: { // or (iy+d)
            int i = index(iy, fetchB());
            int b = bus.peekb(i);
            a = or8bit(a, b);
            cost = 19;
        }
            break;
        case 0xbe: { // cp (iy+d)
            int i = index(iy, fetchB());
            int b = bus.peekb(i);
            cmp8bit(a, b);
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
Debug.printf("Unknown instruction : FD %02x", v);
            break;
        }
    }

    /**
     * fd cb b xx
     * <p>
     * `pc += 2` at last, no need to do that for 2nd fetch
     * </p>
     */
    private void exec_fdcb() {
        int v = bus.peekb(inc16bitInternal(pc));

        switch (v) {
        case 0x06: { // rlc (iy+d)
            int i = index(iy, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, rlc(b, true));
            cost = 23;
        }
            break;
        case 0x0e: { // rrc (iy+d)
            int i = index(iy, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, rrc(b, true));
            cost = 23;
        }
            break;
        case 0x16: { // rl (iy+d)
            int i = index(iy, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, rl(b, true));
            cost = 23;
        }
            break;
        case 0x1e: { // rr (iy+d)
            int i = index(iy, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, rr(b, true));
            cost = 23;
        }
            break;
        case 0x26: { // sla (iy+d)
            int i = index(iy, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, sla(b));
            cost = 23;
        }
            break;
        case 0x2e: { // sra (iy+d)
            int i = index(iy, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, sra(b));
            cost = 23;
        }
            break;
        case 0x3e: { // srl (iy+d)
            int i = index(iy, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, srl(b));
            cost = 23;
        }
            break;
        case 0x46: { // bit 0,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int b = bus.peekb(i);
            bit(0, b);
            cost = 20;
        }
            break;
        case 0x4e: { // bit 1,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int b = bus.peekb(i);
            bit(1, b);
            cost = 20;
        }
            break;
        case 0x56: { // bit 2,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int b = bus.peekb(i);
            bit(2, b);
            cost = 20;
        }
            break;
        case 0x5e: { // bit 3,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int b = bus.peekb(i);
            bit(3, b);
            cost = 20;
        }
            break;
        case 0x66: { // bit 4,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int b = bus.peekb(i);
            bit(4, b);
            cost = 20;
        }
            break;
        case 0x6e: { // bit 5,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int b = bus.peekb(i);
            bit(5, b);
            cost = 20;
        }
            break;
        case 0x76: { // bit 6,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int b = bus.peekb(i);
            bit(6, b);
            cost = 20;
        }
            break;
        case 0x7e: { // bit 7,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int b = bus.peekb(i);
            bit(7, b);
            cost = 20;
        }
            break;
        case 0x86: { // res 0,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, resetBit(0, b));
            cost = 23;
        }
            break;
        case 0x8e: { // res 1,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, resetBit(1, b));
            cost = 23;
        }
            break;
        case 0x96: { // res 2,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, resetBit(2, b));
            cost = 23;
        }
            break;
        case 0x9e: { // res 3,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, resetBit(3, b));
            cost = 23;
        }
            break;
        case 0xa6: { // res 4,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, resetBit(4, b));
            cost = 23;
        }
            break;
        case 0xae: { // res 5,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, resetBit(5, b));
            cost = 23;
        }
            break;
        case 0xb6: { // res 6,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, resetBit(6, b));
            cost = 23;
        }
            break;
        case 0xbe: { // res 7,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, resetBit(7, b));
            cost = 23;
        }
            break;
        case 0xc6: { // set 0,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, setBit(0, b));
            cost = 23;
        }
            break;
        case 0xce: { // set 1,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, setBit(1, b));
            cost = 23;
        }
            break;
        case 0xd6: { // set 2,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, setBit(2, b));
            cost = 23;
        }
            break;
        case 0xde: { // set 3,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, setBit(3, b));
            cost = 23;
        }
            break;
        case 0xe6: { // set 4,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, setBit(4, b));
            cost = 23;
        }
            break;
        case 0xee: { // set 5,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, setBit(5, b));
            cost = 23;
        }
            break;
        case 0xf6: { // set 6,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, setBit(6, b));
            cost = 23;
        }
            break;
        case 0xfe: { // set 7,(iy+d)
            int i = index(iy, bus.peekb(pc));
            int b = bus.peekb(i);
            bus.pokeb(i, setBit(7, b));
            cost = 23;
        }
            break;

        default:
Debug.printf("Unknown instruction : FD CB %02x", v);
            break;
        }

        pc = add16bitInternal(pc, 2);
    }

    // -------------------------------------------------------------------------

    /** */
    private static Properties names = new Properties();

    /* */
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
