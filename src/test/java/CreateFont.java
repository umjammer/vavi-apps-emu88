import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import javax.imageio.ImageIO;

import vavi.util.StringUtil;


/**
 * CreateFont.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-06-27 nsano initial version <br>
 */
public class CreateFont {

    /** */
    public static void main(String[] args) throws Exception {
        int W = 8;
        int H = 16;
        BufferedImage image = new BufferedImage(16 * 8, 16 * H, BufferedImage.TYPE_BYTE_BINARY);
        Graphics g = image.getGraphics();
        InputStream is = CreateFont.class.getResourceAsStream("/font.rom");
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                for (int l = 0; l < 8; l++) {
                    int r = is.read();
                    System.err.println(StringUtil.toBits(r));
                    for (int d = 0; d < 2; d++) { // doubling a line to make H 8 -> 16
                        for (int c = 0; c < 8; c++) {
                            int x = j * W + c;
                            int y = i * H + l * 2 + d;
                            boolean b = (r & (0x80 >>> c)) != 0;
                            g.setColor(b ? Color.white : Color.black);
                            g.drawRect(x, y, x, y);
                        }
                    }
                }
                System.err.println();
            }
        }
        ImageIO.write(image, "PNG", new File("src/main/resources/font2.png"));
    }
}
