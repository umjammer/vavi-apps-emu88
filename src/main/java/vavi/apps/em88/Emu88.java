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
import java.awt.event.KeyListener;
import java.awt.image.CropImageFilter;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import vavi.apps.em88.PC88.Controller;
import vavi.apps.em88.PC88.RomDao;
import vavi.apps.em88.PC88.View;


/**
 * PC-8801 emulator.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.10 931205 nsano original (CP/M emulator) <br>
 *          0.20 931208 nsano PC-8801 emulation version <br>
 *          1.00 031228 nsano java porting <br>
 */
class Emu88 {

    /** */
    @SuppressWarnings("static-access")
    public static void main(String[] args) throws Exception {

        boolean debug_flg = false;

        PC88 pc88 = new PC88();

        Options options = new Options();
        options.addOption("d", false, "debug mode");
        options.addOption(OptionBuilder.withArgName("sw1")
                          .hasArg()
                          .withDescription("dip switch 1" )
                          .create("1") );
        options.addOption(OptionBuilder.withArgName("sw2")
                          .hasArg()
                          .withDescription("dip switch 2" )
                          .create("2") );
        options.addOption("h", false, "display help");

        CommandLineParser parser = new BasicParser();
        CommandLine cl = parser.parse(options, args);

        if (cl.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Emu88", options, true);
            return;
        }

        if (cl.hasOption("d")) {
            debug_flg = true;
        }
        if (cl.hasOption("1")) {
            pc88.setDipSwitch1(~Integer.parseInt(cl.getOptionValue("1"), 16));
        }
        if (cl.hasOption("2")) {
            pc88.setDipSwitch1(~Integer.parseInt(cl.getOptionValue("2"), 16));
        }
        
        System.err.println("PC-8801 emulator Copyright (c) 1993-2003 by vavi");
        
        pc88.setView(new SwingView());
        pc88.setRomDao(new MyRomDao());
        pc88.reset();
        pc88.setDebugMode(debug_flg);
        pc88.exec(0);

        System.exit(0);
    }

    /**
     * �µ������ Graphics ���̂�̂���H�H�H
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
            try {
                Toolkit t = Toolkit.getDefaultToolkit();

                String path = "/font2.png";
                Image image = ImageIO.read(getClass().getResourceAsStream(path));

                for (int i = 0; i < 16; i++) {
                    for (int j = 0; j < 16; j++) {
                        ImageFilter cif = new CropImageFilter(j * 8, i * 16, 8, 16);
                        FilteredImageSource fis = new FilteredImageSource(image.getSource(), cif);
                        textCharacters[i * 16 + j] = t.createImage(fis);
                    }
                }
            } catch (IOException e) {
                throw (RuntimeException) new IllegalStateException().initCause(e);
            }
        }

        /* TODO �킩��Â炢 */
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
            frame.setTitle("Emu88");
            frame.pack();
            frame.setVisible(true);
        }

        /** */
        private final void drawText(Graphics g) {
            for (int l = 0; l < 25; l++) {
                for (int c = 0; c < 80; c++) {
                    Image image = textCharacters[tvram[l][c]];
                    g.drawImage(image, c * W, l * H, null);
                }
            }
        }

        /**
         * @param controller should be instance of KeyListener
         */
        public void setController(Controller controller) {
            frame.addKeyListener((KeyListener) controller);
        }

        /** */
        private Color[] colors = {
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
        /** */
        public void read(String filename, byte[] buf, int length) {
            try {
    
                InputStream is = new FileInputStream(filename);
                int l = 0;
                while (l < length) {
                    int r = is.read(buf, l, length - l);
                    if (r < 0) {
System.err.println("Illegal EOF: " + l + "/" + length);
                        break;
                    }
                    l += r;
                }
                is.close();

            } catch (IOException e) {
                throw (RuntimeException) new IllegalStateException().initCause(e);
            }
        }
    }
}

/* */
