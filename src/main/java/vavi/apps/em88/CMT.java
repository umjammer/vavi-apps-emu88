/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.em88;


/**
 * CMT.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 031230 nsano initial version <br>
 */
class CMT implements Device {

    /** */
    private Bus bus;

    /** */
    public void setBus(Bus bus) {
        this.bus = bus;
    }

    // ----

    // CMT carrier control
    public static final boolean MARK = true;

    public static final boolean SPACE = false;

    /** CMT carrier control 0:spase, 1:mark */
    private boolean cds;

    /** */
    public void setCDS(boolean cds) {
        this.cds = cds;
    }

    /** CMT motor control 0:OFF, 1:ON */
    private boolean mton;

    /** */
    public void setMTON(boolean mton) {
        this.mton = mton;
    }
}

/* */
