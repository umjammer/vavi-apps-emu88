/*
 * Copyright (c) 1988-2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.em88;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import vavi.apps.em88.PC88.Controller;

import vavi.util.Debug;


/**
 * PC-8801 のキーボードをエミュレーションします．
 * 
 * TODO 三つ押されたら四つ目が反応する件
 * <pre>
 * 
 *  -X--X-
 *   |  |
 *  -O--X-
 *   ↑ ここ
 * </pre>
 * 
 * @author <a href=mailto:vavivavi@yahoo.co.jp>Naohide Sano</a> (nsano)
 * @version 0.00 031229 nsano initial version <br>
 *          0.10 040102 nsano complete <br>
 *          0.11 040104 nsano clean up <br>
 */
public final class Keyboard extends KeyAdapter implements Device, Controller {

    /** */
    private Bus bus;

    /** */
    public void setBus(Bus bus) {
        this.bus = bus;
    }

    //----

    /** key port 0x00 ~ 0x0b */
    private int[] keyPort = new int[12];

    /** 指定したポートの値を取得します． */
    public int getPort(int port) {
        return keyPort[port];
    }

    /** */ {
        for (int i = 0; i < 12; i++) {
            keyPort[i] = 0xff;
        }
    }

    // 00
    public static final int RK_NUMPAD0 = 0x01;
    public static final int RK_NUMPAD1 = 0x02;
    public static final int RK_NUMPAD2 = 0x04;
    public static final int RK_NUMPAD3 = 0x08;
    public static final int RK_NUMPAD4 = 0x10;
    public static final int RK_NUMPAD5 = 0x20;
    public static final int RK_NUMPAD6 = 0x40;
    public static final int RK_NUMPAD7 = 0x80;

    // 01
    public static final int RK_NUMPAD8 = 0x01;
    public static final int RK_NUMPAD9 = 0x02;
    public static final int RK_ASTERISK = 0x04;
    public static final int RK_PLUS = 0x08;
    public static final int RK_EQUALS = 0x10;
    public static final int RK_NUMPAD_COMMA = 0x20;
    public static final int RK_NUMPAD_PERIOD = 0x40;
    public static final int RK_ENTER = 0x80;

    // 02
    public static final int RK_AT = 0x01;
    public static final int RK_A = 0x02;
    public static final int RK_B = 0x04;
    public static final int RK_C = 0x08;
    public static final int RK_D = 0x10;
    public static final int RK_E = 0x20;
    public static final int RK_F = 0x40;
    public static final int RK_G = 0x80;

    // 03
    public static final int RK_H = 0x01;
    public static final int RK_I = 0x02;
    public static final int RK_J = 0x04;
    public static final int RK_K = 0x08;
    public static final int RK_L = 0x10;
    public static final int RK_M = 0x20;
    public static final int RK_N = 0x40;
    public static final int RK_O = 0x80;

    // 04
    public static final int RK_P = 0x01;
    public static final int RK_Q = 0x02;
    public static final int RK_R = 0x04;
    public static final int RK_S = 0x08;
    public static final int RK_T = 0x10;
    public static final int RK_U = 0x20;
    public static final int RK_V = 0x40;
    public static final int RK_W = 0x80;

    // 05
    public static final int RK_X = 0x01;
    public static final int RK_Y = 0x02;
    public static final int RK_Z = 0x04;
    public static final int RK_OPEN_BRACKET = 0x08;
    public static final int RK_BACK_SLASH = 0x10;
    public static final int RK_CLOSE_BRACKET = 0x20;
    public static final int RK_CIRCUMFLEX = 0x40;
    public static final int RK_MINUS = 0x80;

    // 06
    public static final int RK_0 = 0x01;
    public static final int RK_1 = 0x02;
    public static final int RK_2 = 0x04;
    public static final int RK_3 = 0x08;
    public static final int RK_4 = 0x10;
    public static final int RK_5 = 0x20;
    public static final int RK_6 = 0x40;
    public static final int RK_7 = 0x80;

    // 07
    public static final int RK_8 = 0x01;
    public static final int RK_9 = 0x02;
    public static final int RK_COLON = 0x04;
    public static final int RK_SEMICOLON = 0x08;
    public static final int RK_COMMA = 0x10;
    public static final int RK_PERIOD = 0x20;
    public static final int RK_SLASH = 0x40;
    public static final int RK_UNDERSCORE = 0x80;

    // 08
    public static final int RK_HOME = 0x01;
    public static final int RK_UP = 0x02;
    public static final int RK_RIGHT = 0x04;
    public static final int RK_INSERT = 0x08;
    public static final int RK_GRPH = 0x10;
    public static final int RK_KANA = 0x20;
    public static final int RK_SHIFT = 0x40;
    public static final int RK_CONTROL = 0x80;

    // 09
    public static final int RK_STOP = 0x01;
    public static final int RK_F1 = 0x02;
    public static final int RK_F2 = 0x04;
    public static final int RK_F3 = 0x08;
    public static final int RK_F4 = 0x10;
    public static final int RK_F5 = 0x20;
    public static final int RK_SPACE = 0x40;
    public static final int RK_ESCAPE = 0x80;

    // 10
    public static final int RK_TAB = 0x01;
    public static final int RK_DOWN = 0x02;
    public static final int RK_LEFT = 0x04;
    public static final int RK_HELP = 0x08;
    public static final int RK_COPY = 0x10;
    public static final int RK_NUMPAD_MINUS = 0x20;
    public static final int RK_NUMPAD_SLASH = 0x40;
    public static final int RK_CAPS_LOCK = 0x80;

    // 11
    public static final int RK_PAGE_UP = 0x01;
    public static final int RK_PAGE_DOWN = 0x02;

    // implements

    /** */
    public void keyPressed(KeyEvent ev) {
        switch (ev.getKeyCode()) {
        // 00
        case KeyEvent.VK_NUMPAD0:
            keyPort[0] &= ~RK_NUMPAD0;
            break;
        case KeyEvent.VK_NUMPAD1:
            keyPort[0] &= ~RK_NUMPAD1;
            break;
        case KeyEvent.VK_NUMPAD2:
            keyPort[0] &= ~RK_NUMPAD2;
            break;
        case KeyEvent.VK_NUMPAD3:
            keyPort[0] &= ~RK_NUMPAD3;
            break;
        case KeyEvent.VK_NUMPAD4:
            keyPort[0] &= ~RK_NUMPAD4;
            break;
        case KeyEvent.VK_NUMPAD5:
            keyPort[0] &= ~RK_NUMPAD5;
            break;
        case KeyEvent.VK_NUMPAD6:
            keyPort[0] &= ~RK_NUMPAD6;
            break;
        case KeyEvent.VK_NUMPAD7:
            keyPort[0] &= ~RK_NUMPAD7;
            break;
        // 01
        case KeyEvent.VK_NUMPAD8:
            keyPort[1] &= ~RK_NUMPAD8;
            break;
        case KeyEvent.VK_NUMPAD9:
            keyPort[1] &= ~RK_NUMPAD9;
            break;
        case KeyEvent.VK_ASTERISK:
            keyPort[1] &= ~RK_ASTERISK;
            break;
        case KeyEvent.VK_PLUS:
            keyPort[1] &= ~RK_PLUS;
            break;
        case KeyEvent.VK_EQUALS:
            keyPort[1] &= ~RK_EQUALS;
            break;
//      case KeyEvent.: keyPort[1] &= ~RK_NUMPAD_COMMA; break;
//      case KeyEvent.: keyPort[1] &= ~RK_NUMPAD_PERIOD; break;
        case KeyEvent.VK_ENTER:
            keyPort[1] &= ~RK_ENTER;
            break;
        // 02
        case KeyEvent.VK_AT:
            keyPort[2] &= ~RK_AT;
            break;
        case KeyEvent.VK_A:
            keyPort[2] &= ~RK_A;
            break;
        case KeyEvent.VK_B:
            keyPort[2] &= ~RK_B;
            break;
        case KeyEvent.VK_C:
            keyPort[2] &= ~RK_C;
            break;
        case KeyEvent.VK_D:
            keyPort[2] &= ~RK_D;
            break;
        case KeyEvent.VK_E:
            keyPort[2] &= ~RK_E;
            break;
        case KeyEvent.VK_F:
            keyPort[2] &= ~RK_F;
            break;
        case KeyEvent.VK_G:
            keyPort[2] &= ~RK_G;
            break;
        // 03
        case KeyEvent.VK_H:
            keyPort[3] &= ~RK_H;
            break;
        case KeyEvent.VK_I:
            keyPort[3] &= ~RK_I;
            break;
        case KeyEvent.VK_J:
            keyPort[3] &= ~RK_J;
            break;
        case KeyEvent.VK_K:
            keyPort[3] &= ~RK_K;
            break;
        case KeyEvent.VK_L:
            keyPort[3] &= ~RK_L;
            break;
        case KeyEvent.VK_M:
            keyPort[3] &= ~RK_M;
            break;
        case KeyEvent.VK_N:
            keyPort[3] &= ~RK_N;
            break;
        case KeyEvent.VK_O:
            keyPort[3] &= ~RK_O;
            break;
        // 04
        case KeyEvent.VK_P:
            keyPort[4] &= ~RK_P;
            break;
        case KeyEvent.VK_Q:
            keyPort[4] &= ~RK_Q;
            break;
        case KeyEvent.VK_R:
            keyPort[4] &= ~RK_R;
            break;
        case KeyEvent.VK_S:
            keyPort[4] &= ~RK_S;
            break;
        case KeyEvent.VK_T:
            keyPort[4] &= ~RK_T;
            break;
        case KeyEvent.VK_U:
            keyPort[4] &= ~RK_U;
            break;
        case KeyEvent.VK_V:
            keyPort[4] &= ~RK_V;
            break;
        case KeyEvent.VK_W:
            keyPort[4] &= ~RK_W;
            break;
        // 05
        case KeyEvent.VK_X:
            keyPort[5] &= ~RK_X;
            break;
        case KeyEvent.VK_Y:
            keyPort[5] &= ~RK_Y;
            break;
        case KeyEvent.VK_Z:
            keyPort[5] &= ~RK_Z;
            break;
        case KeyEvent.VK_OPEN_BRACKET:
            keyPort[5] &= ~RK_OPEN_BRACKET;
            break;
        case KeyEvent.VK_BACK_SLASH:
            keyPort[5] &= ~RK_BACK_SLASH;
            break;
        case KeyEvent.VK_CLOSE_BRACKET:
            keyPort[5] &= ~RK_CLOSE_BRACKET;
            break;
        case KeyEvent.VK_CIRCUMFLEX:
            keyPort[5] &= ~RK_CIRCUMFLEX;
            break;
        case KeyEvent.VK_MINUS:
            keyPort[5] &= ~RK_MINUS;
            break;
        // 06
        case KeyEvent.VK_0:
            keyPort[6] &= ~RK_0;
            break;
        case KeyEvent.VK_1:
            keyPort[6] &= ~RK_1;
            break;
        case KeyEvent.VK_2:
            keyPort[6] &= ~RK_2;
            break;
        case KeyEvent.VK_3:
            keyPort[6] &= ~RK_3;
            break;
        case KeyEvent.VK_4:
            keyPort[6] &= ~RK_4;
            break;
        case KeyEvent.VK_5:
            keyPort[6] &= ~RK_5;
            break;
        case KeyEvent.VK_6:
            keyPort[6] &= ~RK_6;
            break;
        case KeyEvent.VK_7:
            keyPort[6] &= ~RK_7;
            break;
        // 07
        case KeyEvent.VK_8:
            keyPort[7] &= ~RK_8;
            break;
        case KeyEvent.VK_9:
            keyPort[7] &= ~RK_9;
            break;
        case KeyEvent.VK_COLON:
            keyPort[7] &= ~RK_COLON;
            break;
        case KeyEvent.VK_SEMICOLON:
            keyPort[7] &= ~RK_SEMICOLON;
            break;
        case KeyEvent.VK_COMMA:
            keyPort[7] &= ~RK_COMMA;
            break;
        case KeyEvent.VK_PERIOD:
            keyPort[7] &= ~RK_PERIOD;
            break;
        case KeyEvent.VK_SLASH:
            keyPort[7] &= ~RK_SLASH;
            break;
        case KeyEvent.VK_UNDERSCORE:
            keyPort[7] &= ~RK_UNDERSCORE;
            break;
        // 08
        case KeyEvent.VK_HOME:
            keyPort[8] &= ~RK_HOME;
            break;
        case KeyEvent.VK_UP:
            keyPort[8] &= ~RK_UP;
            break;
        case KeyEvent.VK_RIGHT:
            keyPort[8] &= ~RK_RIGHT;
            break;
        case KeyEvent.VK_INSERT:
            keyPort[8] &= ~RK_INSERT;
            break;
        case KeyEvent.VK_ALT:
            keyPort[8] &= ~RK_GRPH;
            break;
        case KeyEvent.VK_SCROLL_LOCK:
            keyPort[8] &= ~RK_KANA;
            break;
        case KeyEvent.VK_SHIFT:
            keyPort[8] &= ~RK_SHIFT;
            break;
        case KeyEvent.VK_CONTROL:
            keyPort[8] &= ~RK_CONTROL;
            break;
        // 09
        case KeyEvent.VK_F11:
            keyPort[9] &= ~RK_STOP;
            break;
        case KeyEvent.VK_F1:
            keyPort[9] &= ~RK_F1;
            break;
        case KeyEvent.VK_F2:
            keyPort[9] &= ~RK_F2;
            break;
        case KeyEvent.VK_F3:
            keyPort[9] &= ~RK_F3;
            break;
        case KeyEvent.VK_F4:
            keyPort[9] &= ~RK_F4;
            break;
        case KeyEvent.VK_F5:
            keyPort[9] &= ~RK_F5;
            break;
        case KeyEvent.VK_SPACE:
            keyPort[9] &= ~RK_SPACE;
            break;
        case KeyEvent.VK_ESCAPE:
            keyPort[9] &= ~RK_ESCAPE;
            break;
        // 0A
        case KeyEvent.VK_TAB:
            keyPort[10] &= ~RK_TAB;
            break;
        case KeyEvent.VK_DOWN:
            keyPort[10] &= ~RK_DOWN;
            break;
        case KeyEvent.VK_LEFT:
            keyPort[10] &= ~RK_LEFT;
            break;
        case KeyEvent.VK_END:
            keyPort[10] &= ~RK_HELP;
            break;
        case KeyEvent.VK_F12:
            keyPort[10] &= ~RK_COPY;
            break;
        // case KeyEvent.: keyPort[10] &= ~RK_NUMPAD_MINUS; break;
        // case KeyEvent.: keyPort[10] &= ~RK_NUMPAD_SLASH; break;
        case KeyEvent.VK_CAPS_LOCK:
            keyPort[10] &= ~RK_CAPS_LOCK;
            break;
        // 0B
        case KeyEvent.VK_PAGE_UP:
            keyPort[11] &= ~RK_PAGE_UP;
            break;
        case KeyEvent.VK_PAGE_DOWN:
            keyPort[11] &= ~RK_PAGE_DOWN;
            break;
        }
    }

    /** */
    public void keyReleased(KeyEvent ev) {
        // Debug.println(ev.getKeyCode());
        switch (ev.getKeyCode()) {
        // 00
        case KeyEvent.VK_NUMPAD0:
            keyPort[0] |= RK_NUMPAD0;
            break;
        case KeyEvent.VK_NUMPAD1:
            keyPort[0] |= RK_NUMPAD1;
            break;
        case KeyEvent.VK_NUMPAD2:
            keyPort[0] |= RK_NUMPAD2;
            break;
        case KeyEvent.VK_NUMPAD3:
            keyPort[0] |= RK_NUMPAD3;
            break;
        case KeyEvent.VK_NUMPAD4:
            keyPort[0] |= RK_NUMPAD4;
            break;
        case KeyEvent.VK_NUMPAD5:
            keyPort[0] |= RK_NUMPAD5;
            break;
        case KeyEvent.VK_NUMPAD6:
            keyPort[0] |= RK_NUMPAD6;
            break;
        case KeyEvent.VK_NUMPAD7:
            keyPort[0] |= RK_NUMPAD7;
            break;
        // 01
        case KeyEvent.VK_NUMPAD8:
            keyPort[1] |= RK_NUMPAD8;
            break;
        case KeyEvent.VK_NUMPAD9:
            keyPort[1] |= RK_NUMPAD9;
            break;
        case KeyEvent.VK_ASTERISK:
            keyPort[1] |= RK_ASTERISK;
            break;
        case KeyEvent.VK_PLUS:
            keyPort[1] |= RK_PLUS;
            break;
        case KeyEvent.VK_EQUALS:
            keyPort[1] |= RK_EQUALS;
            break;
        // case KeyEvent.: keyPort[1] |= RK_NUMPAD_COMMA; break;
        // case KeyEvent.: keyPort[1] |= RK_NUMPAD_PERIOD; break;
        case KeyEvent.VK_ENTER:
            keyPort[1] |= RK_ENTER;
            break;
        // 02
        case KeyEvent.VK_AT:
            keyPort[2] |= RK_AT;
            break;
        case KeyEvent.VK_A:
            keyPort[2] |= RK_A;
            break;
        case KeyEvent.VK_B:
            keyPort[2] |= RK_B;
            break;
        case KeyEvent.VK_C:
            keyPort[2] |= RK_C;
            break;
        case KeyEvent.VK_D:
            keyPort[2] |= RK_D;
            break;
        case KeyEvent.VK_E:
            keyPort[2] |= RK_E;
            break;
        case KeyEvent.VK_F:
            keyPort[2] |= RK_F;
            break;
        case KeyEvent.VK_G:
            keyPort[2] |= RK_G;
            break;
        // 03
        case KeyEvent.VK_H:
            keyPort[3] |= RK_H;
            break;
        case KeyEvent.VK_I:
            keyPort[3] |= RK_I;
            break;
        case KeyEvent.VK_J:
            keyPort[3] |= RK_J;
            break;
        case KeyEvent.VK_K:
            keyPort[3] |= RK_K;
            break;
        case KeyEvent.VK_L:
            keyPort[3] |= RK_L;
            break;
        case KeyEvent.VK_M:
            keyPort[3] |= RK_M;
            break;
        case KeyEvent.VK_N:
            keyPort[3] |= RK_N;
            break;
        case KeyEvent.VK_O:
            keyPort[3] |= RK_O;
            break;
        // 04
        case KeyEvent.VK_P:
            keyPort[4] |= RK_P;
            break;
        case KeyEvent.VK_Q:
            keyPort[4] |= RK_Q;
            break;
        case KeyEvent.VK_R:
            keyPort[4] |= RK_R;
            break;
        case KeyEvent.VK_S:
            keyPort[4] |= RK_S;
            break;
        case KeyEvent.VK_T:
            keyPort[4] |= RK_T;
            break;
        case KeyEvent.VK_U:
            keyPort[4] |= RK_U;
            break;
        case KeyEvent.VK_V:
            keyPort[4] |= RK_V;
            break;
        case KeyEvent.VK_W:
            keyPort[4] |= RK_W;
            break;
        // 05
        case KeyEvent.VK_X:
            keyPort[5] |= RK_X;
            break;
        case KeyEvent.VK_Y:
            keyPort[5] |= RK_Y;
            break;
        case KeyEvent.VK_Z:
            keyPort[5] |= RK_Z;
            break;
        case KeyEvent.VK_OPEN_BRACKET:
            keyPort[5] |= RK_OPEN_BRACKET;
            break;
        case KeyEvent.VK_BACK_SLASH:
            keyPort[5] |= RK_BACK_SLASH;
            break;
        case KeyEvent.VK_CLOSE_BRACKET:
            keyPort[5] |= RK_CLOSE_BRACKET;
            break;
        case KeyEvent.VK_CIRCUMFLEX:
            keyPort[5] |= RK_CIRCUMFLEX;
            break;
        case KeyEvent.VK_MINUS:
            keyPort[5] |= RK_MINUS;
            break;
        // 06
        case KeyEvent.VK_0:
            keyPort[6] |= RK_0;
            break;
        case KeyEvent.VK_1:
            keyPort[6] |= RK_1;
            break;
        case KeyEvent.VK_2:
            keyPort[6] |= RK_2;
            break;
        case KeyEvent.VK_3:
            keyPort[6] |= RK_3;
            break;
        case KeyEvent.VK_4:
            keyPort[6] |= RK_4;
            break;
        case KeyEvent.VK_5:
            keyPort[6] |= RK_5;
            break;
        case KeyEvent.VK_6:
            keyPort[6] |= RK_6;
            break;
        case KeyEvent.VK_7:
            keyPort[6] |= RK_7;
            break;
        // 07
        case KeyEvent.VK_8:
            keyPort[7] |= RK_8;
            break;
        case KeyEvent.VK_9:
            keyPort[7] |= RK_9;
            break;
        case KeyEvent.VK_COLON:
            keyPort[7] |= RK_COLON;
            break;
        case KeyEvent.VK_SEMICOLON:
            keyPort[7] |= RK_SEMICOLON;
            break;
        case KeyEvent.VK_COMMA:
            keyPort[7] |= RK_COMMA;
            break;
        case KeyEvent.VK_PERIOD:
            keyPort[7] |= RK_PERIOD;
            break;
        case KeyEvent.VK_SLASH:
            keyPort[7] |= RK_SLASH;
            break;
        case KeyEvent.VK_UNDERSCORE:
            keyPort[7] |= RK_UNDERSCORE;
            break;
        // 08
        case KeyEvent.VK_HOME:
            keyPort[8] |= RK_HOME;
            break;
        case KeyEvent.VK_UP:
            keyPort[8] |= RK_UP;
            break;
        case KeyEvent.VK_RIGHT:
            keyPort[8] |= RK_RIGHT;
            break;
        case KeyEvent.VK_INSERT:
            keyPort[8] |= RK_INSERT;
            break;
        case KeyEvent.VK_ALT:
            keyPort[8] |= RK_GRPH;
            break;
        case KeyEvent.VK_SCROLL_LOCK:
            keyPort[8] |= RK_KANA;
            break;
        case KeyEvent.VK_SHIFT:
            keyPort[8] |= RK_SHIFT;
            break;
        case KeyEvent.VK_CONTROL:
            keyPort[8] |= RK_CONTROL;
            break;
        // 09
        case KeyEvent.VK_F11:
            keyPort[9] |= RK_STOP;
            break;
        case KeyEvent.VK_F1:
            keyPort[9] |= RK_F1;
            break;
        case KeyEvent.VK_F2:
            keyPort[9] |= RK_F2;
            break;
        case KeyEvent.VK_F3:
            keyPort[9] |= RK_F3;
            break;
        case KeyEvent.VK_F4:
            keyPort[9] |= RK_F4;
            break;
        case KeyEvent.VK_F5:
            keyPort[9] |= RK_F5;
            break;
        case KeyEvent.VK_SPACE:
            keyPort[9] |= RK_SPACE;
            break;
        case KeyEvent.VK_ESCAPE:
            keyPort[9] |= RK_ESCAPE;
            break;
        // 0A
        case KeyEvent.VK_TAB:
            keyPort[10] |= RK_TAB;
            break;
        case KeyEvent.VK_DOWN:
            keyPort[10] |= RK_DOWN;
            break;
        case KeyEvent.VK_LEFT:
            keyPort[10] |= RK_LEFT;
            break;
        case KeyEvent.VK_END:
            keyPort[10] |= RK_HELP;
            break;
        case KeyEvent.VK_F12:
            keyPort[10] |= RK_COPY;
            break;
//      case KeyEvent.:	keyPort[10] |= RK_NUMPAD_MINUS;	break;
//      case KeyEvent.:	keyPort[10] |= RK_NUMPAD_SLASH;	break;
        case KeyEvent.VK_CAPS_LOCK:
            keyPort[10] |= RK_CAPS_LOCK;
            break;
        // 0B
        case KeyEvent.VK_PAGE_UP:
            keyPort[11] |= RK_PAGE_UP;
            break;
        case KeyEvent.VK_PAGE_DOWN:
            keyPort[11] |= RK_PAGE_DOWN;
            break;
        //
        case KeyEvent.VK_PAUSE:
            Debug.println("reset");
            break;
        }
    }
}

/* */
