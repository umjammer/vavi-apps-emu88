/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.em88;


/**
 * USART.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 031230 nsano initial version <br>
 */
class USART implements Device {

    /** */
    private Bus bus;

    /** */
    public void setBus(Bus bus) {
        this.bus = bus;
    }

    // ----

    /** USART channel 0:CMT 1:CMT 0:RS-232C 1:RS-232C */
    private boolean bs1;

    /** */
    public void setBS1(boolean bs1) {
        this.bs1 = bs1;
    }

    /** control 0:600bps 0:1200bps 1:ASYNC 1:SYNC */
    private boolean bs2;

    /** */
    public void setBS2(boolean bs2) {
        this.bs2 = bs2;
    }

    /** */
    public int getControl() {
        return 0;
    }

    /** */
    public void setControl(int control) {
    }

    /** */
    public int getData() {
        return 0;
    }

    /** */
    public void setData(int data) {
    }
}

/* */
