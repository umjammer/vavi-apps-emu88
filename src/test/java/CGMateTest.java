/*
 * https://chatgpt.com/c/68c188a5-9e2c-8323-994a-e016281851d4
 */

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;


/**
 * @see "https://gemini.google.com/app/3aec2d09cba8b2f9"
 */
public class CGMateTest {

    // PC-8801 Standard Resolution
    private static final int WIDTH = 640;
    private static final int HEIGHT = 200;
    private static final int STRIDE = WIDTH / 8; // 80 bytes per row
    private static final int PLANE_SIZE = STRIDE * HEIGHT; // 16,000 bytes

    @Test
    void test1() throws Exception {
        Path p = Paths.get(System.getProperty("user.home"), "Google Drive/My Drive/Private/Computer/games/nsano/nsano/HOME/TMP/img/PIC/BALL.PIC");

        byte[] fileData = Files.readAllBytes(p);

        // The starting offset based on the assembly analysis (LD HL, 0001)
        int startOffset = 1;

        System.out.println("File size: " + fileData.length + " bytes");
        System.out.println("Starting decode at offset: " + startOffset);

        BufferedImage image = decodeImage(fileData, startOffset);

        Path outputFile = Path.of("tmp", "decoded_image_fixed.png");
        ImageIO.write(image, "png", outputFile.toFile());
        System.out.println("Image decoded successfully to " + outputFile.toAbsolutePath());
    }

    public static BufferedImage decodeImage(byte[] data, int offset) {
        // Buffers named for the data they contain: Blue (Block 1), Green (Block 2), Red (Block 3)
        byte[] bluePlane = new byte[PLANE_SIZE];
        byte[] redPlane = new byte[PLANE_SIZE];
        byte[] greenPlane = new byte[PLANE_SIZE];

        int[] dataPtr = {offset};

        // 1. Decode Blue Plane (Block 1)
        System.out.println("--- Decoding Blue Plane (Block 1) ---");
        decodePlane(data, dataPtr, bluePlane);

        // --- FIX: Skip the 0x00 separator (at index 8025) ---
        if (dataPtr[0] < data.length && (data[dataPtr[0]] & 0xFF) == 0x00) {
            System.out.println("  [FIX] Skipping 0x00 separator byte at index " + dataPtr[0]);
            dataPtr[0]++;
        }

        // 2. Decode Green Plane (Block 2, data for the Green channel)
        System.out.println("--- Decoding Green Plane (Block 2) ---");
        decodePlane(data, dataPtr, greenPlane);

        // --- FIX: Skip the 0x00 separator (at index 16358) ---
        if (dataPtr[0] < data.length && (data[dataPtr[0]] & 0xFF) == 0x00) {
            System.out.println("  [FIX] Skipping 0x00 separator byte at index " + dataPtr[0]);
            dataPtr[0]++;
        }

        // 3. Decode Red Plane (Block 3, data for the Red channel)
        System.out.println("--- Decoding Red Plane (Block 3) ---");
        decodePlane(data, dataPtr, redPlane);

        // --- FINAL FIX: Swap Red and Green in the planarToBuffered call ---
        // The planarToBuffered function expects (Blue, Red, Green)
        // Since the output was visually swapped, we feed it (Blue, GreenData, RedData)
        // This effectively tells the image combiner: "Use the Green buffer as the Red plane, and the Red buffer as the Green plane."
        System.out.println("--- Image Assembly: Applying Red/Green Swap Correction ---");
        return planarToBuffered(bluePlane, greenPlane, redPlane);
    }

    private static void decodePlane(byte[] src, int[] srcPtr, byte[] dest) {
        int destPtr = 0;
        int startSrc = srcPtr[0];

        while (destPtr < dest.length && srcPtr[0] < src.length) {
            // Read Command Byte (c9bc: LD A,(HL))
            int cmd = src[srcPtr[0]++] & 0xFF;

            // Check for End of Stream (0x00) (c9bf: CP 00)
            if (cmd == 0) {
                if (destPtr == 0) {
                    System.out.println("  [Warning] Plane decode started with 0x00. Skipping data for this plane.");
                }
                break;
            }

            // Check MSB (Bit 7) (c9c4: BIT 7,A)
            if ((cmd & 0x80) != 0) {
                // --- LITERAL COPY (Bit 7 is Set) ---
                int count = cmd & 0x7F;

                for (int i = 0; i < count; i++) {
                    if (destPtr >= dest.length || srcPtr[0] >= src.length) break;
                    dest[destPtr++] = src[srcPtr[0]++];
                }
            } else {
                // --- RLE FILL (Bit 7 is Clear) ---
                int count = cmd;

                if (srcPtr[0] >= src.length) break;
                byte fillValue = src[srcPtr[0]++];

                for (int i = 0; i < count; i++) {
                    if (destPtr >= dest.length) break;
                    dest[destPtr++] = fillValue;
                }
            }
        }
        System.out.println("  Decoded " + destPtr + " bytes from source offset " + startSrc + " to " + srcPtr[0]);
    }

    private static BufferedImage planarToBuffered(byte[] b, byte[] r, byte[] g) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

        // Standard PC-88 Palette (G-R-B encoding: G=Bit2, R=Bit1, B=Bit0)
        int[] palette = {
                0x000000, 0x0000FF, 0xFF0000, 0xFF00FF,
                0x00FF00, 0x00FFFF, 0xFFFF00, 0xFFFFFF
        };

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x += 8) {
                int byteOffset = (y * STRIDE) + (x / 8);

                // b: Blue data buffer, r: Red data buffer, g: Green data buffer
                int blueByte = b[byteOffset] & 0xFF;
                int redByte = r[byteOffset] & 0xFF;
                int greenByte = g[byteOffset] & 0xFF;

                for (int bit = 0; bit < 8; bit++) {
                    int mask = 1 << (7 - bit);

                    int bBit = (blueByte & mask) != 0 ? 1 : 0;
                    int rBit = (redByte & mask) != 0 ? 1 : 0;
                    int gBit = (greenByte & mask) != 0 ? 1 : 0;

                    // Combine bits to form color index (Format: GRB)
                    int colorIndex = (gBit << 2) | (rBit << 1) | bBit;

                    image.setRGB(x + bit, y, palette[colorIndex]);
                }
            }
        }
        return image;
    }
}
