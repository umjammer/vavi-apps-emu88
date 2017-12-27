/*
 * Copyright (c) 1993-2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.em88;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import vavi.util.StringUtil;


/**
 * Z80 Debugger.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.10 931205 original (CP/M debugger) <br>
 *          0.20 931207 Z80 debugger <br>
 *          1.00 031228 nsano java porting <br>
 */
public class Debugger implements Device {

    /** */
    private Z80 z80;

    /** */
    private Disassembler disassembler = new Disassembler();

    /** */
    private Bus bus;

    /** */
    public void setBus(Bus bus) {
        this.bus = bus;

        this.z80 = (Z80) bus.getDevice(Z80.class.getName());
        this.currentAddress = z80.getPC();

        disassembler.setBus(bus);
    }

    //----

    /** */
    private int currentAddress = 0;
    /** */
    private int brakeAddress = -1;

    /** */
    private void displayRegs() {
        System.out.println(
            "PC=" + StringUtil.toHex4(z80.getPC()) +
            " A=" + StringUtil.toHex2(z80.getA()) +
            " BC=" + StringUtil.toHex4(z80.getBC()) +
            " DE=" + StringUtil.toHex4(z80.getDE()) +
            " HL=" + StringUtil.toHex4(z80.getHL()) +
            " SP=" + StringUtil.toHex4(z80.getSP()) +
            " IX=" + StringUtil.toHex4(z80.getIX()) +
            " IY=" + StringUtil.toHex4(z80.getIY()) +
            " C:" + (z80.isC() ? 1 : 0) +
            " N:" + (z80.isN() ? 1 : 0) +
            " P:" + (z80.isP() ? 1 : 0) +
            " H:" + (z80.isH() ? 1 : 0) +
            " Z:" + (z80.isZ() ? 1 : 0) +
            " S:" + (z80.isS() ? 1 : 0));
    }

    private void help() {
        System.err.println(
                "x[rr=nn]\tchange register\n" +
                "d[nn]\t\tdump memory\n" +
                "e[nn]\t\twrite memory\n" +
                "l[nn]\t\tdisassemble\n" +
                "g[nn]\t\tgo\n" +
                "t[nn]\t\ttrace nn times (visible)\n" +
                "u[nn]\t\ttrace nn times (invisible)\n" +
                "p\t\tpassing trace\n" +
                "RET\t\ttrace once\n" +
                "b[nn]\t\tset/display break points\n" +
                "ip\t\tinput from port p\n" +
                "op,n\t\toutput n to port p\n" +
                "r,filename,nn\tread file to address nn\n" +
                "!\t\tshell command\n" +
                "h/?\t\thelp\n" +
                "q\t\tquit\n" +
                "SHFT+GRPH\tbreak\n\n" +
                "n is 8 bit hex number\n" +
                "r is register (a,bc,de,hl)\n");
    }

    /** */
    private void hexDump(int address, int length) {

        for (int y = 0; y < (length + 15) / 16; y++) {
            System.out.print(StringUtil.toHex4(address + y * 16) + " ");
            for (int x = 0; x < 16; x++) {
                if (address + y * 16 + x > 0xffff) {
                    break;
                }
                System.out.print(
                    StringUtil.toHex2(bus.peekb(address + y * 16 + x)) + " ");
            }
            System.out.print("  ");
            for (int x = 0; x < 16; x++) {
                if (address + y * 16 + x > 0xffff) {
                    break;
                }
                int c = bus.peekb(address + y * 16 + x);
                System.out.print(
                    !Character.isISOControl((char) c) ? (char) c : '.');
            }
            System.out.println();
        }
    }

    /** */
    private int list(int address) {

        System.out.print(StringUtil.toHex4(address) + " ");
        int movedAddress = disassembler.execute(address);
        for (int i = 0; i < 4; i++) {
            if (i < movedAddress - address) {
                System.out.print(StringUtil.toHex2(bus.peekb(address + i)));
            } else {
                System.out.print("  ");
            }
        }
        System.out.print("\t" + disassembler.getCurrentMnemonic());
        if (disassembler.getCurrentComment() != null) {
            System.out.println("\t\t; " + disassembler.getCurrentComment());
        } else {
            System.out.println();
        }

        return movedAddress;
    }

    /** */
    private void setRegs(String buf) {
        buf = buf.substring(1).toLowerCase();

        if (buf.startsWith("a=")) {
            z80.setA(Integer.parseInt(buf.substring(2), 16));
        } else if (buf.startsWith("bc=")) {
            z80.setBC(Integer.parseInt(buf.substring(3), 16));
        } else if (buf.startsWith("de=")) {
            z80.setDE(Integer.parseInt(buf.substring(3), 16));
        } else if (buf.startsWith("hl=")) {
            z80.setHL(Integer.parseInt(buf.substring(3), 16));
        } else if (buf.startsWith("pc=")) {
            currentAddress = Integer.parseInt(buf.substring(3), 16);
            z80.setPC(currentAddress);
        } else if (buf.startsWith("sp=")) {
            z80.setSP(Integer.parseInt(buf.substring(3), 16));
        } else {
            System.err.println("unknown reg: " + buf);
        }

        displayRegs();
    }

    /** */
    private int listedAddress = 0;

    /** */
    private void listSource(String buf) {

        if (buf.length() > 1) {
            listedAddress = Integer.parseInt(buf.substring(1), 16);
        }

        for (int i = 0; i < 8; i++) {
            listedAddress = list(listedAddress);
        }
    }

    /** */
    private void readFile(String buf) throws IOException {

        StringTokenizer st = new StringTokenizer(buf, ",\t ");

        if (st.countTokens() < 3) {
            return;
        }

        String filename = st.nextToken();
        int address = Integer.parseInt(st.nextToken(), 16);

System.out.println("read file " + filename + " address " + StringUtil.toHex4(address) + "(" + address + ")");

        FileInputStream fp = new FileInputStream(filename);

        for (int p = address;; p++) {
            int c = fp.read();
            if (c == -1) {
                break;
            }
//System.out.println("address " + StringUtil.toHex4(address) + "=" + StringUtil.toHex2(k));
            bus.pokeb(p, c);
        }

        fp.close();
//System.out.println("read file " + filename + " address " + StringUtil.toHex4(address) + ", " + (p - address) + " bytes");
    }

    /** */
    private void outputPort(String buf) {

        StringTokenizer st = new StringTokenizer(buf, ",\t ");

        if (st.countTokens() < 2) {
            return;
        }

        int port = Integer.parseInt(st.nextToken(), 16);
        int data = Integer.parseInt(st.nextToken(), 16);

        bus.outp(port, data);
    }

    /** */
    private void inputPort(String buf) {

        int port = Integer.parseInt(buf.substring(1), 16);

        int data = bus.inp(port);

        System.out.println("in " + StringUtil.toHex2(port) +
                           " = " + StringUtil.toHex2(data));
    }

    /** */
    private int editedAddress = 0;

    /** */
    private void editMemory(String buf) throws IOException {

        if (buf.length() > 1) {
            editedAddress = Integer.parseInt(buf.substring(1), 16);
        }
        while (true) {
            System.out.print(
                StringUtil.toHex4(editedAddress) + " " +
                StringUtil.toHex2(bus.peekb(editedAddress)) + " = ");
            buf = reader.readLine();
            if (buf.charAt(0) == '.') {
                break;
            }
            if (buf.length() > 0) {
                bus.pokeb(editedAddress, Integer.parseInt(buf, 16));
            }
            editedAddress++;
        }
    }

    /** */
    private int dumpedAddress = 0;

    /** */
    private void dumpMemory(String buf) {

        if (buf.length() > 1) {
            dumpedAddress = Integer.parseInt(buf.substring(1), 16);
        }
        hexDump(dumpedAddress, 256);
        dumpedAddress += 256;

        if (dumpedAddress > 0xffff) {    // TODO
            dumpedAddress = 0;
        }
    }

    /**
     * @return current address is brake point or not.
     */
    private boolean step(boolean verbose) {
        currentAddress = z80.execute(currentAddress, 1);

        if (currentAddress == brakeAddress) {
            System.err.println("break point");
            return true;
        }
        if (z80.isUserBroken()) {
            System.err.println("user break");
            return true;
        }
        if (verbose) {
            displayRegs();
            list(currentAddress);
        }

        return false;
    }

    /** */
    private void go(int length, boolean verbose) {

        if (length != 0) {
            for (int i = 0; i < length; i++) {
                if (step(verbose)) {
                    break;
                }
            }
        } else {
            while (true) {
                if (step(verbose)) {
                    break;
                }
            }
        }
    }

    /** */
    private void pass(int address) {
        while (true) {
            if (step(false)) {
                break;
            }
            if (currentAddress == address) {
                break;
            }
        }

        displayRegs();
        list(currentAddress);
    }

    /** */
    private BufferedReader reader =
            new BufferedReader(new InputStreamReader(System.in));

    /**
     * PC-8801 emulator debug mode
     */
    public void execute() {

        System.err.println("Z80 Debugger Copyright (c) 1993-2003 by vavi");

        while (true) {
            try {
                System.out.print(">");
                String line = reader.readLine();
                if (line == null) {
                    break;
                }

                if (line.length() == 0) {
                    step(true);
                    continue;
                }

                int k = line.toLowerCase().charAt(0);
                switch (k) {
                case 'x':
                    setRegs(line);
                    break;
                case 'l':
                    listSource(line);
                    break;
                case 'd':
                    dumpMemory(line);
                    break;
                case 'e':
                    editMemory(line);
                    break;
                case 'i':
                    inputPort(line);
                    break;
                case 'o':
                    outputPort(line);
                    break;
                case 'g':
                    if (line.length() > 1) {
                      currentAddress = Integer.parseInt(line.substring(1), 16);
                    }
                    go(0, false);
                    break;
                case 't':
                case 'u':
                    int p = Integer.parseInt(line.substring(1), 16);
                    if (p == 0) {
                        p = 1;
                    }
                    go(p, k != 'u');
                    break;
                case 'p':
                    pass(disassembler.execute(currentAddress));
                    break;
                case 'b':
                    if (line.length() > 1) {
                        brakeAddress = Integer.parseInt(line.substring(1), 16);
                    }
System.err.println("break point = " + StringUtil.toHex4(brakeAddress));
                    break;
                case 'r':
                    readFile(line);
                    break;
                case 'h':
                case '?':
                    help();
                    break;
                case '!':
                    Runtime.getRuntime().exec(line.substring(1));
                    break;
                case 'q':
                    return;
                }
            } catch (Exception e) {
e.printStackTrace(System.err);
            }
        }
    }
}

/* */
