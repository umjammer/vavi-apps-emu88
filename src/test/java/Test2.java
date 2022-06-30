import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import vavi.apps.em88.Bus;
import vavi.apps.em88.Z80;


/**
 * Test2.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-06-27 nsano initial version <br>
 */
public class Test2 {

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "zexall")
    void testZExeAll() throws Exception {
        byte[] program = Files.readAllBytes(Paths.get("src/test/resources/zexall.com"));
        exec(program, 0);
    }

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "zexdoc")
    void testZExeDoc() throws Exception {
        byte[] program = Files.readAllBytes(Paths.get("src/test/resources/zexdoc.com"));
        exec(program, 0);
    }

    public static void exec(byte[] program, int testsToSkip) {
        Z80 z80 = new Z80();
        z80.setBus(new Bus.SimpleBus());

        z80.getBus().pokes(0x100, program, 0, program.length);

        z80.getBus().pokeb(6, 0xFF);
        z80.getBus().pokeb(7, 0xFF);

        z80.addListener(Test2::z80OnBeforeInstructionFetch);

        skipTests(z80, testsToSkip);

        long sw = System.currentTimeMillis();

        z80.execute(0x100);

        System.err.println("\nElapsed time: " + (System.currentTimeMillis() - sw));
    }

    private static void skipTests(Z80 z80, int testsToSkipCount) {
        int loadTestsAddress = 0x120;
        int originalAddress = 0x13A;
        int newTestAddress = originalAddress + testsToSkipCount * 2;
        z80.getBus().pokew(loadTestsAddress, newTestAddress);
    }

    private static void z80OnBeforeInstructionFetch(Z80 z80) {

        // Absolutely minimum implementation of CP/M for ZEXALL and ZEXDOC to work

        if (z80.getPC() == 0) {
            z80.setUserBroken(true);
            return;
        } else if (z80.getPC() != 5)
            return;

        int function = z80.getC();

        if (function == 9) {
            int messageAddress = z80.getDE();
            ByteArrayOutputStream bytesToPrint = new ByteArrayOutputStream();
            int byteToPrint;
            while ((byteToPrint = z80.getBus().peekb(messageAddress)) != '$') {
                bytesToPrint.write(byteToPrint);
                messageAddress++;
            }

            String StringToPrint = new String(bytesToPrint.toByteArray(), StandardCharsets.US_ASCII);
            System.err.print(StringToPrint);
        } else if (function == 2) {
            int byteToPrint = z80.getE();
            char charToPrint = (char) byteToPrint;
            System.err.print(charToPrint);
        }

        // we need to simulate sub routine call return
        int sp = z80.getSP();
        int newPC = z80.getBus().peekw(sp);

        z80.setPC(newPC);
        z80.addSP(2);
    }
}
