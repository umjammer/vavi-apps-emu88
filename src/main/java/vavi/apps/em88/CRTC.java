/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.em88;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import vavi.util.Debug;


/**
 * CRTC.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 031230 nsano initial version <br>
 */
class CRTC implements Device {

    private static final Logger logger = System.getLogger(CRTC.class.getName());

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
        logger.log(Level.DEBUG, "CRTC: setCommand %02x".formatted( command));
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
        case 0x80: // cursor load (off?)
        case 0x81: // cursor load (on?)
            pos = 0;
            // For 0x80/0x81, Bit 0 determines "Load Cursor Position" flag?
            // quasi88: crtc_load_cursor_position = data & 0x01; (where data is command byte?)
            // If command is 0x80 -> 0. If 0x81 -> 1.
            // If 0, it sets cursor to -1,-1 (Hidden).
            // If 1, it reads parameters.
            boolean visible = (command & 0x01) != 0;
            graphic.setCursorDisplaied(visible);
            break;
        }

        sf = 0;
    }

    /**
     * @see "p-84"
     */
    public void setData(int data) {
        logger.log(Level.DEBUG, "CRTC: setData %02x (Command %02x)".formatted(data, command));
        switch (command & 0xE0) { // Check top 3 bits for command group
        case 0x00: // initialize
            switch (sf) {
            case 0: // 1: screen format 1
                boolean c_b = (data & 0x80) != 0;
                int h = data & 0x7f;
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
        case 0x80: // cursor load
            // quasi88:
            // if (crtc_load_cursor_position) {
            //    crtc_cursor[ crtc_param_num++ ] = data;
            // } else {
            //    crtc_cursor[ crtc_param_num++ ] = -1;
            // }
            // Note: crtc_param_num resets on setCommand.
            
            if ((command & 0x01) == 0) {
                // Command 0x80: Clear/Hide Cursor?
                // quasi88 says: if 0, set to -1.
                // But it assumes we READ data.
                // Does 0x80 accept data? Yes, param sequence.
                graphic.setCursor(-1, -1);
                pos++; // Consume param count
            } else {
                // Command 0x81: Set Cursor
                if (pos == 0) {
                    curpos = data; // X
                    pos = 1;
                } else {
                    int x = curpos;
                    int y = data; // Y
                    graphic.setCursor(x, y);
                    pos = 0; // Reset? or 2?
                }
            }
            break;
        case 0x60: // light pen (0x70 in my old code?)
             // 0x60 >> 5 = 3 (CRTC_READ_LIGHT_PEN).
             // Matches quasi88.
             // Do nothing for now.
             break;
        }
        
        // Handle legacy 0x70/0x71 specific if needed, but 0x80 covers it.
        if (command == 0x70 || command == 0x71) {
             if (pos == 0) {
                 curpos = data; 
                 pos = 1;
             } else {
                 graphic.setCursor(curpos, data);
             }
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
        
        switch (command & 0xE0) { // Check Group
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
