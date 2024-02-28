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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.CropImageFilter;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.klab.commons.cli.Option;
import org.klab.commons.cli.Options;
import vavi.apps.em88.PC88.Controller;
import vavi.apps.em88.PC88.RomDao;
import vavi.apps.em88.PC88.View;
import vavi.net.www.protocol.URLStreamHandlerUtil;
import vavi.util.Debug;
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

    static {
        URLStreamHandlerUtil.loadService();
    }

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
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setTitle("Emu88");
        dialog.setLocation(650, 0);
        dialog.pack();
        dialog.setVisible(true);

        //
        JButton button = new JButton();
        button.setAction(new AbstractAction("Break") {
            public void actionPerformed(ActionEvent ev) {
                pc88.getCpu().setUserBroken(true);
            }
        });
        button.setPreferredSize(new Dimension(60, 20));

        JDialog controller = new JDialog();
        controller.getContentPane().add(button);
        controller.setTitle("Controller");
        controller.setLocation(860, 0);
        controller.pack();
        controller.setVisible(true);

        System.err.println("PC-8801 emulator Copyright (c) 1993-2003 by vavi");

        SwingView view = new SwingView();
        view.mainWindowActivated = dialog::toFront;
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
Debug.println("set font correctly: " + path);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        Runnable mainWindowActivated;

        /** TODO */
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
            frame.addWindowListener(new WindowAdapter() {
                @Override public void windowActivated(WindowEvent e) {
                    mainWindowActivated.run();
                }
            });
            frame.setTitle("Emu88");
            frame.pack();
            frame.setVisible(true);
        }

        /** */
        private void drawText(Graphics g) {
            for (int l = 0; l < 25; l++) {
                for (int c = 0; c < 80; c++) {
                    Image image = textCharacters[tvram[l][c]];
                    g.drawImage(image, c * W, l * H, null);
//if (Character.isLetterOrDigit((char) tvram[l][c])) Debug.println((char) tvram[l][c]);
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
        static final Map<String, String> roms = new HashMap<String, String>() {{
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
Debug.println("Illegal EOF: " + l + "/" + length);
                        break;
                    }
                    l += r;
                }
            } catch (NullPointerException e) {
Debug.println(Level.SEVERE, "set roms correctly: " + roms.get(tag));
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

/* */
