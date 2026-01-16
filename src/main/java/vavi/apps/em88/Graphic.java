/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.em88;

import vavi.apps.em88.PC88.View;


/**
 * PC-8801 emulator Graphics Section.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 031230 nsano initial version <br>
 */
final class Graphic implements Device {

    /** */
    private Bus bus;

    /** */
    public void setBus(Bus bus) {
        this.bus = bus;
    }

    // ----

    private View view;

    public void setView(View view) {
        this.view = view;
        view.reset();
    }

    // ----

    /** */
    public void changePalette(int palette, int color) {
    }

    /** crt mode false:B&W true:color */
    private boolean color;

    /** */
    public void setColorMode(boolean color) {
        this.color = color;
    }

    /** */
    public boolean isColorMode() {
        return color;
    }

    /** */
    private boolean hColor;

    /** */
    public void setHColorMode(boolean hColor) {
        this.hColor = hColor;
    }

    /** */
    private boolean graphicDisplayed;

    /** */
    public void setGraphicDisplayed(boolean graphicDisplayed) {
        this.graphicDisplayed = graphicDisplayed;
    }

    /** CRTC mode text 0:OFF, 1:ON */
    private boolean textDisplayed;

    /** */
    public void setTextDisplayed(boolean textDisplayed) {
        this.textDisplayed = textDisplayed;
    }

    /** */
    public boolean isTextDisplayed() {
        return textDisplayed;
    }

    /** */
    private boolean cursorDisplayed;

    /** */
    public void setCursorDisplaied(boolean cursorDisplayed) {
        this.cursorDisplayed = cursorDisplayed;
    }

    /** */
    public void setCursor(int c, int l) {
        if (view != null) {
            view.setCursor(c, l);
        }
    }

    /** */
    private boolean _40;

    /** */
    public void set40(boolean _40) {
        this._40 = _40;
//logger.log(Level.TRACE, _40);
        view.set40(_40);
    }

    /** */
    private boolean _25Line;

    /** */
    public void set25Line(boolean _25Line) {
        this._25Line = _25Line;
//logger.log(Level.TRACE, _25Line);
        view.set25Line(_25Line);
    }

    /** */
    private boolean _200Line;

    /** */
    public void set200Line(boolean _200Line) {
        this._200Line = _200Line;
    }

    /** */
    public void setBorderColor(int color) {
    }

    /** */
    public void setBackground(int color) {
        view.setBackground(color);
    }

    public void repaint() {
        view.repaint();
    }

    /** */
    public void pokeb(int offset, int data) {

        int c = offset % 120;
        int l = offset / 120;

// if (Character.isLetterOrDigit((char) data)) {
//  logger.log(Level.TRACE, (char) data + ": " + c + ", " + l);
// }
// if (Character.isLetterOrDigit((char) data)) {
//  System.err.print(Level.TRACE, "%4x ".formatted(data));
// }
        // if (c >= 80) { // Attribute Area logic was broken/unused
            view.setTextVram(c, l, data);
        // } else {
        //    view.setTextVram(c, l, data);
        // }
    }
}
