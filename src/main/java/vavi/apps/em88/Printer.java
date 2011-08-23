/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.em88;


/**
 * Printer.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 031230 nsano initial version <br>
 */
class Printer implements Device {

    /** */
    private Bus bus;

    /** */
    public void setBus(Bus bus) {
        this.bus = bus;
    }

    //----

    /** */
    public boolean isBusy() {
        return false;
    }

    public void setPSTB(boolean pstb) {
    }
}

/* */
