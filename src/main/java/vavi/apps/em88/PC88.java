/*
 * Copyright (c) 1993-2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.em88;

import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * PC-8801 mk2 emulator.
 * <p>
 * TODO �������� {@link Device} ��
 * </p>
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 931205 nsano first version <br>
 *          0.10 931206 nsano add sub system <br>
 *          1.00 031228 nsano java porting <br>
 */
public class PC88 {

    public interface View { 
        void setController(Controller controller);

        void reset();

        void setTextVram(int c, int l, int value);
        int getTextVram(int c, int l);

        void set40(boolean _40);
        void set25Line(boolean _25Line);

        void repaint();
        void setBackground(int c); // TODO
    }

    private View view;

    public void setView(View view) {
        this.view = view;
    }

    public interface Controller { 
    }

    public interface RomDao {
        void read(String filename, byte[] buf, int length);
    }

    private RomDao romDao;

    public void setRomDao(RomDao romDao) {
        this.romDao = romDao;
    }

    /** the CPU */
    private Z80 z80 = new Z80();
    /** */
    private INTC intc = new INTC();
    /** the Keyboard */
    private Keyboard keyboard = new Keyboard();
    /** Graphics */
    private Graphic graphic = new Graphic();
    /** */
    private DMA dma = new DMA();
    /** */
    private CRTC crtc = new CRTC();
    /** */
    private Printer printer = new Printer();
    /** */
    private USART usart = new USART();
    /** */
    private CMT cmt = new CMT();
    /** */
    private UIOP uiop = new UIOP();

    /** */
    public Bus getBus() {
        return mainBus;
    }

    /** */
    public void reset() {
        romDao.read("/roms/romn88.bin", ROM_N88, 0x8000);
        romDao.read("/roms/romn.bin", ROM_N, 0x8000);
        romDao.read("/roms/rom4th.bin", ROM_4TH[0], 0x2000);
//      romDao.read("/roms/romsub.bin", ROM_SUB, 0x800);

        mainBus.addDevice(graphic);
        mainBus.addDevice(intc);
        mainBus.addDevice(keyboard);
        mainBus.addDevice(dma);
        mainBus.addDevice(crtc);
        mainBus.addDevice(printer);
        mainBus.addDevice(usart);
        mainBus.addDevice(cmt);
        mainBus.addDevice(uiop);
        mainBus.addDevice(z80);

        mainBus.reset();

Debug.println("sw1: " + StringUtil.toHex2(sw1));
Debug.println("sw2: " + StringUtil.toHex2(sw2));
    }

    /** */
    private boolean debugMode;

    /** */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    /**
     * @before {@link #setView(View)}
     */
    public void exec(int address) {
        // TODO ���܂���
        Graphic graphic = (Graphic) mainBus.getDevice(Graphic.class.getName());
        graphic.setView(view);
        // TODO ���܂���
        Keyboard keyboard = (Keyboard) mainBus.getDevice(Keyboard.class.getName());
        view.setController(keyboard);

        if (!debugMode) {
            z80.execute(address);
        }
        if (debugMode || z80.isUserBroken()) {
            Debugger debugger = new Debugger();
            debugger.setBus(mainBus);
            debugger.execute();
        }
    }

    //-------------------------------------------------------------------------

    // memory mode
    private static final boolean RAM = true;
    private static final boolean ROM = false;
    // ROM mode	(ROM 3, 4)
    private static final boolean N = true;
    //          (ROM 1, 2)
    private static final boolean N88 = false;

    /* 88 system */

    /**
     * dip switch 1
     * �ȉ��� ON/OFF �Ńr�b�g��쐬�����]���Đݒ肷��B
     * <pre>
     * SW1                      ON                 OFF
     *
     * BASIC mode on boot       N-BASIC            N88-BASIC
     * boot mode (N88-BASIC)    terminal mode      BASIC
     * column                   80 char/line       40 char/line
     * line                     25 line/screen     20 line/screen
     * S parameter              no ignore S        ignore S
     * DEL code process         process DEL        ignore DEL
     *
     * memory wait              1 wait             normal
     * select BCR/CS            Bar Code Reader    CMD SING
     * </pre>
     */
    private int sw1 = ~0x34 & 0xff;

    /** */
    public void setDipSwitch1(int sw1) {
        this.sw1 = sw1 & 0xff;
    }

    /**
     * dip switch 2
     * �ȉ��� ON/OFF �Ńr�b�g��쐬�����]���Đݒ肷��B
     * <pre>
     * SW2                      ON              OFF
     *
     * parity check             use             nouse
     * parity                   even            odd
     * data bit length          8               7
     * stop bit length          2               1
     * X parameter              no ignore X     ignore X
     * communication            half            full
     *
     * floppy disk drives       exist           no exist
     * reserved
     * </pre>
     */
    private int sw2 = ~0xc6 & 0xff;

    /** */
    public void setDipSwitch2(int sw2) {
        this.sw2 = sw2 & 0xff;
    }

    /** jumper switch RS-232C boud rate */
    private int jp1 = 8;
    /** jumper switch CRT display dot 0:S, 1:H */
    private int jp2 = 0;
    /** jumper switch CRT B&W output 0:M, 1:T */
    private int jp3 = 0;

    /** text window offset address register */
    private int oar = 0x80;
    /** ~ROMKILL signal */
    private int romkill = 0xff;

    /** C000 - FFFF RAM mode 01:B, 02:R, 04:G, 00:RAM */
    private int vram = 0;

    /** RAM mode 0:ROM,RAM mode, 1:64KRAM mode */
    private boolean mmode = ROM;
    /** ROM mode 0:N88-BASIC mode, 1:N-BASIC mode */
    private boolean rmode = N88;

    /** ROM 2 */
    private byte[] ROM_N88 = new byte[0x8000];
    /** ROM 1 */
    private byte[] ROM_N = new byte[0x8000];
    /** 4th ROM 1 (ROM 3) */
    private byte[][] ROM_4TH = new byte[8][];
    /** main RAM */
    private byte[] RAM_64K = new byte[0x10000];

    /** */
    private byte[] VRAM_R = new byte[0x4000];
    /** */
    private byte[] VRAM_G = new byte[0x4000];
    /** */
    private byte[] VRAM_B = new byte[0x4000];

    /** ROM sub system */
    private byte[] ROM_SUB = new byte[0x800];
    /** RAM sub system */
    private byte[] RAM_SUB = new byte[0x4000];

    /** */
    private byte[] ROM_DUMMY = new byte[] { (byte) 0xff };

    /** initialize RAM, pattern */ {
        ROM_4TH[0] = new byte[0x2000];

//      RAM_64K[0xe6c4] = (byte) 0xc8;    // TODO
//      RAM_64K[0xe6c5] = (byte) 0xf3;

        int d;
        for (int a = 0; a < 0x10000; a += 0x100) {
            if ((a & 0x0d00) == 0x0100 ||
                (a & 0x0f00) == 0x0500 ||
                (a & 0x0f00) == 0x0a00 ||
                (a & 0x0d00) == 0x0c00) {
                d = 0xff;
            } else {
                d = 0x00;
            }

            if ((a & 0x4000) != 0) {
                d ^= 0xff;
            }
            if ((a & 0x8000) != 0) {
                d ^= 0xff;
            }
            if ((a & 0xf000) == 0xb000) {
                d ^= 0xff;
            }

            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 16; j++) {
                    RAM_64K[a + i * 64      + j] = (byte) d;
                }
                for (int j = 0; j < 16; j++) {
                    RAM_64K[a + i * 64 + 16 + j] = (byte) (d ^ 0xff);
                }
                for (int j = 0; j < 16; j++) {
                    RAM_64K[a + i * 64 + 32 + j] = (byte) d;
                }
                for (int j = 0; j < 16; j++) {
                    RAM_64K[a + i * 64 + 48 + j] = (byte) (d ^ 0xff);
                }
                d ^= 0xff;
            }
        }
    }

    /** */
    private final Bus mainBus = new Bus() {

        /** */
        private int rom4th = 0;
        /** */
        private int tvrams;
        /** */
        private int tvrame;

        /** */
        protected final Mapping getMapping(int address, Direction direction) {
            Mapping mapping = new Mapping();

            if (address < 0x6000) {                     // 0000 - 6000 : RAM N88 N
                if (direction == Direction.WRITE || mmode == RAM) {
                    mapping.base = RAM_64K;
                } else {
//                  if ((~romkill & 0xfe) != 0) {       // TODO ROM1 �����UʁH
//                      address.base = null;
//  Debug.println("4throm: " + rom4th + ", illegal access: " + StringUtil.toHex4(a));
//                  } else {
                        if (rmode == N) {
                            mapping.base = ROM_N;
                        } else {
                            mapping.base = ROM_N88;
                        }
//                  }
                }
                mapping.pointer = address;
            } else if (address < 0x8000) {              // 6000 - 8000 : RAM 4th
                if (direction == Direction.WRITE || mmode == RAM) {
                    mapping.base = RAM_64K;
                    mapping.pointer = address;
                } else {
                    if ((~romkill & 0xff) != 0) {
                        mapping.base = ROM_4TH[rom4th];
                        mapping.pointer = address - 0x6000;
                        if (mapping.base == null) {     // TODO
Debug.println("use default: rom " + (rom4th + 1) + " does not exists");
                            mapping.base = ROM_N88;
                            mapping.pointer = address;
                        }
                    } else {
                        if (rmode == N) {
                            mapping.base = ROM_N;
                            mapping.pointer = address;
                        } else {
                            mapping.base = ROM_N88;
                            mapping.pointer = address;
                        }
                    }
                }
            } else if (address < 0x8400) {              // 8000 - 8400 : RAM WIN
                if (mmode == ROM && rmode == N88) {     // mode 1
                    address = (oar << 8) + (address & 0x03ff);
                }
                mapping.base = RAM_64K;
                mapping.pointer = address;
            } else if (address < 0xc000) {              // 8400 - C000 : RAM
                mapping.base = RAM_64K;
                mapping.pointer = address;
            } else {                                    // C000 - FFFF : RAM GVB GVR GVG
                switch (vram) {
                case 0x00:
                    mapping.base = RAM_64K;
                    mapping.pointer = address;
                    break;
                case 0x01:
                    mapping.base = VRAM_B;
                    mapping.pointer = address - 0xc000;
                    break;
                case 0x02:
                    mapping.base = VRAM_R;
                    mapping.pointer = address - 0xc000;
                    break;
                case 0x04:
                    mapping.base = VRAM_G;
                    mapping.pointer = address - 0xc000;
                    break;
                }
            }

//  if (direction == WRITE &&
//      address.base == ROM_N88 && address.pointer < 0x8000) {
//   Debug.println("ROM write: " + StringUtil.toHex4(address.pointer));
//  }
            return mapping;
        }

        /** TODO */
        public void pokeb(int address, int value) {
            Mapping mapping = getMapping(address, Direction.WRITE);
            mapping.base[mapping.pointer] = (byte) (value & 0xff);

            if (mapping.base == RAM_64K) {
                if (address >= tvrams && address <= tvrame) {
                    graphic.pokeb(address - tvrams, value);
                    graphic.repaint();
                }
            }
        }

        /** */
        public int inp(int port) {
            int data = 0;

            switch (port) {
            case 0:                     // key board
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 0x0a:
            case 0x0b:
                data = keyboard.getPort(port);
                break;
            case 0x20:                  // USART data port
                data = usart.getData();
                break;
            case 0x21:                  // USART control port
                data = usart.getControl();
                break;
            case 0x30:                  // dip switch 1, uip 2, 3
                data = (sw1 & 0x3f) | uip1() | uip2();
                break;
            case 0x31:                  // dip switch 2
                data = sw2 & 0x3f;
                break;
            case 0x40:                  // BUSY,SHG,DCD,EXTON,CDI,VRTC
                data = vrtc() | exton() | shg() | busy();
                break;
            case 0x50:                  // CRTC read data
                data = crtc.getData();
                break;
            case 0x51:                  // CRTC read status
                data = crtc.getStatus();
                break;
            case 0x5c:                  // GVRAM status
                data = vram;
                break;
            case 0x68:                  // DMAC status port
                break;
            case 0x70:                  // TEXT WINDOW offset address
                data = oar;
                break;
            case 0x71:                  // 4th ROM control
                data = romkill;
                break;
            case 0xe2:                  // PC-8012-02 bank status
                break;
            case 0xe3:                  // PC-8012-02 bank control
                break;
            case 0xe8:                  // KANJI font L
                break;
            case 0xe9:                  // KANJI font H
                break;
            case 0xf4:                  // DMA 8 inch disk control
                break;
            case 0xf6:                  // DMA 8 inch disk control
                break;
            case 0xf7:                  // DMA 8 inch disk control
                break;
            case 0xf8:                  // DMA 8 inch disk control
                break;
            case 0xfa:                  // DMA 8 inch disk control
                break;
            case 0xfb:                  // DMA 8 inch disk control
                break;
            case 0xfc:                  // mini disk control
                break;
            case 0xfe:                  // mini disk control
                break;
            }

//Debug.println(" pc: " + StringUtil.toHex4(z80.getPC()) + ": " + StringUtil.toHex2(port) + " <- " + StringUtil.toHex2(data) + ", " + inportNames.getProperty(StringUtil.toHex2(port)));
            return data;
        }

        /** */
        public void outp(int port, int data) {
//Debug.println("pc: " + StringUtil.toHex4(z80.getPC()) + ": " + StringUtil.toHex2(port) + " -> " + StringUtil.toHex2(data) + ", " + outportNames.getProperty(StringUtil.toHex2(port)));
            switch (port) {
            case 0x10:                  //
                break;
            case 0x20:                  // USART data port
                usart.setData(data);
                break;
            case 0x21:                  // USART control port
                usart.setControl(data);
                break;
            case 0x30:                  // system control port 1
                graphic.set40((data & 0x01) == 0);
                graphic.setColorMode((data & 0x02) == 0);   // B&W / color
                cmt.setCDS((data & 0x04) != 0 ? CMT.MARK : CMT.SPACE);
                cmt.setMTON((data & 0x08) != 0);
                usart.setBS1((data & 0x10) != 0);
                usart.setBS2((data & 0x20) != 0);
                break;
            case 0x31:                  // system control port 2
                graphic.set200Line((data & 0x01) != 0);
                graphic.setHColorMode((data & 0x10) != 0);
                mmode = (data & 0x02) != 0 ? RAM : ROM;
//Debug.println("mmode: " + (mmode == ROM ? "ROM" : "RAM"));
                rmode = (data & 0x04) != 0 ? N : N88;
//Debug.println("rmode: " + (rmode == N88 ? "N88" : "N"));
                graphic.setGraphicDisplayed((data & 0x08) != 0);
                graphic.set25Line((data & 0x20) != 0);
                break;
            case 0x40:                  // strobe port
                printer.setPSTB((data & 0x01) == 0);
//              beep((data & 0x20) != 0);
        uiop.setPort1((data & 0x40) != 0);
        uiop.setPort2((data & 0x80) != 0);                      // CMD SING
                break;
            case 0x50:                  // CRTC write data
                crtc.setData(data);
                break;
            case 0x51:                  // CRTC write command
                crtc.setCommand(data);
                break;
            case 0x52:                  // border, background color
                graphic.setBackground(data & 0x07);             // select BGC
                graphic.setBorderColor(data & 0x70);            // border
                break;
            case 0x53:                  // screen control
                graphic.setTextDisplayed((data & 0x01) != 0);   // TVRAM
                break;
            case 0x54:                  // control color palette
            case 0x55:
            case 0x56:
            case 0x57:
            case 0x58:
            case 0x59:
            case 0x5a:
            case 0x5b:
                graphic.changePalette(port - 0x54, data);
                break;
            case 0x5c:                  // select GVRAM (B,R,G,RAM)
                vram = 0x01;
                break;
            case 0x5d:
                vram = 0x02;
                break;
            case 0x5e:
                vram = 0x04;
                break;
            case 0x5f:
                vram = 0;
                break;
            case 0x60:                  // CH-n DMA address
            case 0x62:
            case 0x64:
            case 0x66: {
                int channel = (port - 0x60) / 2;
                dma.setAddress(channel, data);
tvrams = dma.getAddress(2);
              } break;
            case 0x61:                  // CH-n DMA terminal count
            case 0x63:
            case 0x65:
            case 0x67: {
                int channel = (port - 0x61) / 2;
                dma.setTerminalCount(channel, data);
tvrame = tvrams + dma.getTerminalCount(2);
              } break;
            case 0x68:                  // DMAC control port
                dma.setMode(data);
                break;
            case 0x70:                  // set TEXT WINDOW address
                oar = data;
                break;
            case 0x71:                  // 4th ROM control
                romkill = data;
                if ((~romkill & 0xff) != 0) {
                    rom4th = (int) (Math.log(~romkill & 0xff) / Math.log(2));
Debug.println("4th rom: " + (rom4th + 1));
                }
                break;
            case 0x78:                  // inc TEXT WINDOW address
                oar++;
                break;
            case 0xe2:                  // PC-8012-02 bank control
                break;
            case 0xe3:                  // PC-8012-02 bank control
                break;
            case 0xe4:                  // interrupt controller out
                intc.setRegister(data);
                break;
            case 0xe6:                  // interrupt musk flag
                intc.setMask(data);
                break;
            case 0xe8:                  // KANJI ROM address L
                break;
            case 0xe9:                  // KANJI ROM address H
                break;
            case 0xea:                  // KANJI ROM read start
                break;
            case 0xeb:                  // KANJI ROM read stop
                break;
            case 0xf3:                  // DMA 8 inch disk control
                break;
            case 0xf4:                  // DMA 8 inch disk control
                break;
            case 0xf5:                  // DMA 8 inch disk control
                break;
            case 0xf7:                  // DMA 8 inch disk control
                break;
            case 0xf8:                  // DMA 8 inch disk control
                break;
            case 0xf9:                  // DMA 8 inch disk control
                break;
            case 0xfb:                  // DMA 8 inch disk control
                break;
            case 0xfd:                  // mini disk control
                break;
            case 0xff:                  // mini disk control
                break;
            }
        }

        /** 31h */
        private int uip1() { return !uiop.getPort1() ? 0x40 : 0x00; }
        /** 31h */
        private int uip2() { return !uiop.getPort2() ? 0x80 : 0x00; }
        /** 40h */
        private int exton() { return (sw2 & 0x40) != 0  ? 0x00 : 0x08; }
        /** 40h */
        private int vrtc() { return intc.getVrtc() ? 0x20 : 0x00; }

        /** 40h */
        private int dcd() { return false  ? 0x00 : 0x40; }
        /** 40h */
        private int busy() { return printer.isBusy()  ? 0x01 : 0x00; }
        /** 40h */
        private int shg() { return true ? 0x00 : 0x02; }

        /** 40h out */
        private boolean pstb() { return false ? false : true; }
    };

    /** */
    private final Bus subBus = new Bus() {

        /** */
        protected final Mapping getMapping(int address, Direction direction) {
            Mapping mapping = new Mapping();
            if (address < 0x800) {
                mapping.base = ROM_SUB;
                mapping.pointer = address;
            } else if (address < 0x4000) {
                mapping.base = null;
                mapping.pointer = 0;
            } else if (address < 0x8000) {
                mapping.base = RAM_SUB;
                mapping.pointer = address;
            } else {
                mapping.base = null;
                mapping.pointer = 0;
            }
            return mapping;
        }

        /** */
        public int inp(int port) {
            int data = 0;

            switch (port) {
            case 0xf7:              // BUSY
//              data = busy();
                break;
            case 0xfa:              // FDC765 read status register
                break;
            case 0xfb:              // FDC765 read data
                break;
            case 0xfc:              // 8255 port A input data
                break;
            case 0xfe:              // 8255 port C hand shake
                break;
            }

            return data;
        }

        /** */
        public void outp(int port, int data) {
            switch (port) {
            case 0xf6:              // out PSTB (PC-8801 mk2 ignore)
                break;
            case 0xf7:              // VFO window (lower 4 bit)
                break;
            case 0xf8:              // motor ON/OFF
                break;
            case 0xfb:              // FDC765 write data
                break;
            case 0xfd:              // 8255 port B output data
                break;
            case 0xfe:              // 8255 port C hand shake
                break;
            case 0xff:              // 8255 control port
                break;
            }
        }
    };
}

/* */
