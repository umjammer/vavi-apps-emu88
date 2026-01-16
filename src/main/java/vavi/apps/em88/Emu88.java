/*
 * Copyright (c) 1993-2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.em88;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyListener;
import java.awt.image.CropImageFilter;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.klab.commons.cli.Option;
import org.klab.commons.cli.Options;
import vavi.apps.em88.PC88.Controller;
import vavi.apps.em88.PC88.RomDao;
import vavi.apps.em88.PC88.View;
import vavi.util.StringUtil;


/**
 * PC-8801 emulator.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.10 931205 nsano original (CP/M emulator) <br>
 *          0.20 931208 nsano PC-8801 emulation version <br>
 *          1.00 031228 nsano java porting <br>
 */
@Options
class Emu88 {

    private static final Logger logger = System.getLogger(Emu88.class.getName());

    @Option(option = "d", argName = "debug mode")
    boolean debugMode;
    @Option(option = "1", argName = "dip switch 1", args = 1)
    Integer dipSwitch1; // TODO hex?
    @Option(option = "2", argName = "dip switch 2", args = 1)
    Integer dipSwitch2; // TODO hex?

    /** run emulator */
    public static void main(String[] args) throws Exception {

        Emu88 emu = new Emu88();
        Options.Util.bind(args, emu);

        PC88 pc88 = new PC88();

        if (emu.dipSwitch1 != null) {
            pc88.setDipSwitch1(emu.dipSwitch1);
        }
        if (emu.dipSwitch2 != null) {
            pc88.setDipSwitch2(emu.dipSwitch2);
        }

        DebugPanel debugPanel = new DebugPanel(pc88.getCpu());
        JDialog dialog = new JDialog();
        dialog.getContentPane().add(debugPanel);
        // Debug Dialog configuration
        dialog.setFocusableWindowState(false); // Prevent stealing focus
        dialog.setTitle("Emu88 Debug");
        dialog.setLocation(650, 0);
        dialog.pack();
        dialog.setVisible(true);

        //
        JButton button = new JButton();
        button.setAction(new AbstractAction("Break") {
            ExecutorService es = Executors.newSingleThreadExecutor();
            public void actionPerformed(ActionEvent ev) {
                if (getValue(Action.NAME).equals("Break")) {
                    pc88.getCpu().setUserBroken(true);
                    putValue(Action.NAME, "Start");
                } else if (getValue(Action.NAME).equals("Start")) {
                    es.execute(() -> pc88.getCpu().execute(pc88.getCpu().getPC(), 0));
                    putValue(Action.NAME, "Break");
                }
            }
        });
        button.setPreferredSize(new Dimension(60, 20));

        JDialog controller = new JDialog();
        controller.getContentPane().add(button);
        controller.setTitle("Controller");
        controller.setLocation(860, 0);
        controller.pack();
        controller.setVisible(true);

        logger.log(Level.DEBUG, "PC-8801 emulator Copyright (c) 1993-2003 by vavi");

        SwingView view = new SwingView();
        // view.mainWindowActivated = dialog::toFront; // Disable auto-to-front to avoid focus issues
        pc88.setView(view);
        pc88.setRomDao(new MyRomDao());
        pc88.reset();
        pc88.setDebugMode(emu.debugMode);
        pc88.exec(0);

        System.exit(0);
    }

    /**
     * Graphics
     */
    static class SwingView implements View {
        /** */
        JFrame frame = new JFrame();
        /** */
        private Image[] textCharacters = new Image[256];
        /** */
        private JPanel screen;

        /** */
        private int[][] tvram = new int[26][120];

        /* */
        public void setTextVram(int c, int l, int value) {
            tvram[l][c] = value;
        }

        /* */
        public int getTextVram(int c, int l) {
            return tvram[l][c];
        }

        /** Cursor Position */
        private int cursorX = -1, cursorY = -1;

        public void setCursor(int c, int l) {
            logger.log(Level.DEBUG, "SwingView: setCursor(%d, %d)".formatted(c, l));
            this.cursorX = c;
            this.cursorY = l;
        }

        /** */
        private int W = 8;
        /** */
        private int H = 16;

        public void set40(boolean _40) {
            W = _40 ? 16 : 8;
        }

        public void set25Line(boolean _25Line) {
            H = _25Line ? 16 : 20;
        }

        /** */
        SwingView() {
            String path = "/font2.png";
            try {
                Toolkit t = Toolkit.getDefaultToolkit();

                Image image = ImageIO.read(getClass().getResourceAsStream(path));

                for (int i = 0; i < 16; i++) {
                    for (int j = 0; j < 16; j++) {
                        ImageFilter cif = new CropImageFilter(j * 8, i * 16, 8, 16);
                        FilteredImageSource fis = new FilteredImageSource(image.getSource(), cif);
                        textCharacters[i * 16 + j] = t.createImage(fis);
                    }
                }
            } catch (NullPointerException e) {
logger.log(Level.ERROR, "set font correctly: " + path);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        /** */
        Runnable mainWindowActivated;

        public void reset() {
            screen = new JPanel() {
                public void paint(Graphics g) {
                    super.paint(g);
                    drawText(g);
                }
            };
            screen.setPreferredSize(new Dimension(640, 400));
            screen.setOpaque(true);
            screen.setBackground(Color.black);

            //----

            frame.getContentPane().add(screen);

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            // frame.addWindowListener(new WindowAdapter() {
            //     @Override public void windowActivated(WindowEvent e) {
            //         if (mainWindowActivated != null) mainWindowActivated.run();
            //     }
            // });
            frame.setTitle("Emu88 Main"); // Rename Main Window
            frame.pack();
            frame.setVisible(true);
            frame.requestFocus(); // Request focus for Main Window

            new javax.swing.Timer(33, e -> {
                screen.repaint();
            }).start();
        }

        /** */
        private void drawText(Graphics g) {
            // Blink Counter
            long blink = (System.currentTimeMillis() / 500) % 2; // 0 or 1

            for (int l = 0; l < 25; l++) {
                // Parse Attributes for this line (20 pairs at offset 80)
                int attrPtr = 80;
                int[] attrEvents = new int[81]; // 0..80.
                
                for (int i=0; i<81; i++) attrEvents[i] = 0; 
                
                int attrRest = 0; // "Rest" attribute (Right Side)

                // Decode Attributes from VRAM pairs
                for (int i = 0; i < 20; i++) {
                    int col = tvram[l][attrPtr + i*2] & 0xff;
                    int atr = tvram[l][attrPtr + i*2 + 1] & 0xff;
                    
                    int encodedAttr = atr | 0x100; // Mark as present

                    if (col < 80) { // Valid Column 0-79
                        if (attrEvents[col] == 0) { // If Empty, set it
                            attrEvents[col] = encodedAttr;
                        }
                    } else if (col == 0x80) { // End Marker
                        if (attrRest == 0) {
                            attrRest = encodedAttr;
                        }
                    }
                }
                
                // Attribute Swap Logic (Right-to-Left Propagation)
                // If there is a "Rest" attribute at the end, it applies to everything 
                // from the previous attribute marker up to the end.
                // quasi88 logic:
                // If attrEvents[0] is empty, it means the FIRST segment hasn't been defined.
                // But in "Valid Until", the attribute at Col X applies to 0...X.
                // So we need to propagate RIGHT TO LEFT.
                
                // If we have an End Marker (attrRest), we use it to fill gaps from Right.
                int currentFill = attrRest;
                for (int c = 80; c >= 0; c--) {
                    if (attrEvents[c] != 0) {
                        // Found a marker.
                        // The marker at 'c' is valid for 0...c.
                        // But wait, "Valid Until" means Attr at C applies to pixels < C.
                        // So the region C...NextMarker gets "NextMarker's Attr". No.
                        // "Valid Until C" means: From PreviousMarker to C, use THIS Attr.
                        
                        // quasi88:
                        // for (j=80; j>0; j--) {
                        //   if (text_attr[j]) {
                        //     tmp = text_attr[j];
                        //     text_attr[j] = attr_rest;
                        //     attr_rest = tmp;
                        //   }
                        // }
                        // text_attr[0] = attr_rest;
                        
                        // Let's implement EXACTLY this swap logic.
                        // It essentially shifts attributes to the RIGHT (Start of Next Segment).
                        int tmp = attrEvents[c];
                        attrEvents[c] = currentFill;
                        currentFill = tmp;
                    }
                }
                // Finally, set Column 0 to the remaining attribute (which applies to start)
                attrEvents[0] = currentFill;

                // Scan Line
                int activeAttr = 0xE0; // Default White
                
                // State variables
                boolean activeReverse = false;
                boolean activeBlink = false;
                boolean activeSecret = false;
                int activeColor = 7; // White

                for (int c = 0; c < 80; c++) {
                    // Update Attribute State
                    if (attrEvents[c] != 0) {
                        int attrByte = attrEvents[c] & 0xff; // Strip 0x100
                        boolean switchBit = (attrByte & 0x08) != 0;
                        if (switchBit) {
                            // Color Change
                            int b = (attrByte & 0x20) >> 5;
                            int r = (attrByte & 0x40) >> 6;
                            int g_ = (attrByte & 0x80) >> 7;
                            activeColor = (g_ << 2) | (r << 1) | b;
                        } else {
                            // Attribute Change
                            activeReverse = (attrByte & 0x04) != 0;
                            activeBlink   = (attrByte & 0x02) != 0;
                            activeSecret  = (attrByte & 0x01) != 0;
                        }
                    }

                    int charCode = tvram[l][c] & 0xff;
                    
                    // Apply current state
                    boolean reverse = activeReverse;
                    boolean blinkOn = activeBlink;
                    boolean secret  = activeSecret;
                    int colorVal    = activeColor;

                    if (secret || (blinkOn && blink == 0)) {
                        charCode = 0; // Space
                    }

                    Color fg = colors[colorVal];
                    Color bg = Color.black;

                    // Cursor Overlay (Blink Block)
                    if (cursorX != -1 && l == cursorY && c == cursorX) {
                        if (blink == 1) { 
                             reverse = !reverse;
                        }
                        // Force Debug Log once
                        if (c == 0 && l == 0 && blink == 1) { /* logger.log(Level.DEBUG, "Cursor Blink On"); */ }
                    }

                    if (reverse) {
                        Color tmp = fg;
                        fg = bg;
                        bg = tmp;
                    }

                    // Background Draw
                    if (!bg.equals(Color.black)) { 
                        g.setColor(bg);
                        g.fillRect(c * W, l * H, W, H);
                    }

                    if (charCode != 0 && charCode != 32) {
                        if (reverse) {
                             g.setXORMode(Color.black);
                             g.drawImage(textCharacters[charCode], c * W, l * H, null);
                             g.setPaintMode();
                        } else {
                             g.drawImage(textCharacters[charCode], c * W, l * H, null);
                        }
                    }
                }
            }
        }

        /**
         * @param controller should be an instance of KeyListener
         */
        public void setController(Controller controller) {
            frame.addKeyListener((KeyListener) controller);
        }

        /** */
        private static final Color[] colors = {
            Color.black, Color.red, Color.blue, Color.magenta,
            Color.green, Color.cyan, Color.yellow, Color.white
        };

        /* */
        public void setBackground(int color) {
            screen.setBackground(colors[color]);
        }

        /* */
        public void repaint() {
            screen.repaint();
        }
    }

    static class MyRomDao implements RomDao {
        static final Map<String, String> roms = new HashMap<>() {{
            put("N88", "classpath:roms/romn88.bin");
            put("N80", "classpath:roms/romn.bin");
            put("4TH", "classpath:roms/rom4th.bin");
//            put("N88", "file:///Users/nsano/.config/quasi88/rom/N88.ROM");
//            put("N80", "file:///Users/nsano/.config/quasi88/rom/N80.ROM");
//            put("4TH", "file:///Users/nsano/.config/quasi88/rom/N88EXT0.ROM");
        }};

        /** */
        public void read(String tag, byte[] buf, int length) {
            try (InputStream is = URI.create(roms.get(tag)).toURL().openStream()) {
                int l = 0;
                while (l < length) {
                    int r = is.read(buf, l, length - l);
                    if (r < 0) {
//logger.log(Level.TRACE, "Illegal EOF: " + l + "/" + length);
                        break;
                    }
                    l += r;
                }
            } catch (NullPointerException e) {
//logger.log(Level.TRACE, Level.SEVERE, "set roms correctly: " + roms.get(tag));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /** */
    static class DebugPanel extends JPanel {
        Z80 cpu;

        DebugPanel(Z80 cpu) {
            this.cpu = cpu;
            cpu.addListener(c -> repaint());

            this.setPreferredSize(new Dimension(200, 300));
            this.setOpaque(true);
            this.setBackground(Color.black);
        }

        public void paint(Graphics g) {

            super.paint(g);

            int y1 = 10;
            int y2 = 10;
            g.setColor(Color.green);
            g.drawString(String.format("PC=%04x", cpu.getPC()), 10, y1 += 20);
            g.drawString(String.format(" A=%02x", cpu.getA()), 10, y1 += 20);
            g.drawString("IM=" + cpu.getIm(), 100, y2 += 20);
            g.drawString(String.format("BC=%04x", cpu.getBC()), 10, y1 += 20);
            g.drawString("iff1=" + cpu.isIff1(), 100, y2 += 20);
            g.drawString(String.format("DE=%04x", cpu.getDE()), 10, y1 += 20);
            g.drawString("iff2=" + cpu.isIff2(), 100, y2 += 20);
            g.drawString(String.format("HL=%04x", cpu.getHL()), 10, y1 += 20);
            g.drawString("intr=" + cpu.isInterrupted(), 100, y2 += 20);
            g.drawString(String.format("IR=%04x", cpu.getIR()), 10, y1 += 20);
            g.drawString(String.format("SP=%04x", cpu.getSP()), 10, y1 += 20);
            g.drawString(String.format("IX=%04x", cpu.getIX()), 10, y1 += 20);
            g.drawString(String.format("IY=%04x", cpu.getIY()), 10, y1 += 20);
            g.drawString(" C:" + (cpu.isC() ? 1 : 0), 10, y1 += 20);
            g.drawString(" N:" + (cpu.isN() ? 1 : 0), 10, y1 += 20);
            g.drawString(" P:" + (cpu.isP() ? 1 : 0), 10, y1 += 20);
            g.drawString(" H:" + (cpu.isH() ? 1 : 0), 10, y1 += 20);
            g.drawString(" Z:" + (cpu.isZ() ? 1 : 0), 10, y1 += 20);
            g.drawString(" S:" + (cpu.isS() ? 1 : 0), 10, y1 += 20);

            if (cpu.getBus() == null) {
                return;
            }
            for (int i = 0; i < 6 && cpu.getSP() + 2 * i < 0x10000; i++) {
                g.drawString(String.format("%04x: %04x", cpu.getSP() + 2 * i, cpu.getBus().peekw(cpu.getSP() + 2 * i)), 100, y2 += 20);
            }
            for (int i = 0; i < 12; i++) {
                g.drawString(String.format("%02x: %s", i, StringUtil.toBits(cpu.getBus().inp(i))), 10, y1 += 20);
            }
        }
    }
}
