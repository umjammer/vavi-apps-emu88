/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.em88;

/**
 * UIOP.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 031230 nsano initial version <br>
 */
class UIOP implements Device {

    /** */
    private Bus bus;

    /** */
    public void setBus(Bus bus) {
        this.bus = bus;
    }

    // ----

    /** universal input port 1 */
    private boolean port1 = false;

    /** */
    public boolean getPort1() {
        return port1;
    }

    /** */
    public void setPort1(boolean port1) {
        this.port1 = port1;
    }

    /** universal input port 2 */
    private boolean port2 = false;

    /** */
    public boolean getPort2() {
        return port2;
    }

    /** */
    public void setPort2(boolean port2) {
        this.port2 = port2;
    }
}

/* */
