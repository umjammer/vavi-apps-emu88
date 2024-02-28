/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.em88;

import vavi.util.Debug;


/**
 * CRTC.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 031230 nsano initial version <br>
 */
class CRTC implements Device {

    /** */
    private Graphic graphic;

    /** bus emulation */
    private Bus bus;

    /** emulation connect bus */
    public void setBus(Bus bus) {
        this.bus = bus;

        this.graphic = (Graphic) bus.getDevice(Graphic.class.getName());
    }

    //----

    /** CRTC command No. */
    private int command;

    /** CRTC screen format No. */
    private int sf;
    /** cursor/light pen position x/y 0:x, 1:y */
    private int pos;

    // cursor/light pen position offset address
    private int curpos;

    /** */
    public void setCommand(int command) {
        this.command = command;

        switch (command) {
        case 0x00: // initialize, 0:reset
            graphic.setTextDisplayed(false); // TODO DMA off ???
            break;
        case 0x20: // normal display
        case 0x21: // reverse display (now ignore)
            graphic.setTextDisplayed(true); // TODO DMA on ???
            break;
        case 0x43: // interrupt mask
            break;
        case 0x60: // light pen get position
            pos = 0;
//        peninit();
            break;
        case 0x70: // cursor on, set position
        case 0x71: // cursor off, set position
            pos = 0;
            graphic.setCursorDisplaied((command - 0x70) != 0);
            break;
        }

        sf = 0;
    }

    /**
     * @see "p-84"
     */
    public void setData(int data) {
        switch (command) {
        case 0x00: // initialize
            switch (sf) {
            case 0: // 1: screen format 1
                boolean c_b = (data & 0x80) != 0;
                int h = data & 0x7f;
if (h + 2 > 80) {
 Debug.println("h > 80");
}
                sf = 1;
                break;
            case 1: // 2: screen format 2
                int b = (data & 0xc0) >> 6;
                sf = 2;
                break;
            case 2: // 3: screen format 3
                sf = 3;
                break;
            case 3: // 4: screen format 4
                sf = 4;
                break;
            case 4: // 5: screen format 5
                sf = 5;
                break;
            case 5: // 6: screen format 6
                sf = 0;
                break;
            }
            break;
        case 0x70: // corsor on, set position
        case 0x71: // corsor off, set position
            if (pos == 0) {
                curpos = data * 2;
                pos = 1;
            } else {
                curpos += data * 160;
//        locate(curpos % 160, curpos / 160);
            }
            break;
        }
    }

    /** */
    public int getStatus() {
        int data = 0;
        data |= graphic.isTextDisplayed() ? 0x10 : 0x00; // TODO DMA ???
//      data |= penhit() ? 0x01 : 0x00;
        return data;
    }

    /** */
    public int getData() {
        int data = 0;
        switch (command) {
        case 0x00: // reset
            break;
        case 0x60: // light pen get position
//            CURPOS = getpen();
            if (pos == 0) {
                data = curpos % 160;
                pos = 1;
            } else {
                data = curpos / 160;
            }
            break;
        }

        return data;
    }
}

/* */
