/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.em88;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.System.getLogger;


/**
 * Floppy Disk Controller (FDC).
 *
 * Implements a basic state machine to handle FDC commands and
 * satisfy emulator startup checks.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 251227 nsano initial version <br>
 */
class FDC implements Device {

    private static final Logger logger = getLogger(FDC.class.getName());

    private Bus bus;
    private INTC intc;
    private Timer timer;

    // FDC Status Register Bits
    private static final int RQM = 0x80; // Request for Master
    private static final int DIO = 0x40; // Data In/Out (1=Read, 0=Write)
    private static final int NDM = 0x20; // Non-DMA Code
    private static final int CB  = 0x10; // FDC Busy
    private static final int DB0 = 0x01; // Drive Busy 0
    private static final int DB1 = 0x02; // Drive Busy 1
    private static final int DB2 = 0x04; // Drive Busy 2
    private static final int DB3 = 0x08; // Drive Busy 3

    // Phases
    private enum Phase {
        IDLE,
        COMMAND,
        EXECUTION,
        RESULT
    }

    private Phase phase = Phase.IDLE;
    private int status = RQM; // Initial status: Ready, Direction=Write
    private int command;
    private int parameterCount;
    private int parameterIndex;

    private int[] parameters = new int[10];
    private Queue<Integer> resultQueue = new LinkedList<>();
    
    private int currentDriveHead = 0;
    private int[] pcn = new int[4];

    @Override
    public void setBus(Bus bus) {
        this.bus = bus;
        this.intc = (INTC) bus.getDevice(INTC.class.getName());

        if (this.timer != null) {
            this.timer.cancel();
        }
        this.timer = new Timer("FDC-Timer", true);
        this.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (bus != null) {
                    int currentValue = bus.peekb(0xef20);
                    if (currentValue > 0) {
                        bus.pokeb(0xef20, currentValue - 1);
                    }
                }
            }
        }, 1, 1);
    }

    /** FDC Status Register */
    public int getStatus() {
        return status;
    }

    /** FDC Data Register Read */
    public int read() {
        int data = 0;
        if (phase == Phase.RESULT) {
            if (!resultQueue.isEmpty()) {
                data = resultQueue.poll();
            }
            // If all results read, return to IDLE
            if (resultQueue.isEmpty()) {
                phase = Phase.IDLE;
                status = RQM; // Ready, Write direction (waiting for command)
                logger.log(Level.DEBUG, "FDC: Result Phase Complete -> IDLE");
            }
        }
        return data;
    }

    /** FDC Data Register Write */
    public void write(int data) {
        if (phase == Phase.IDLE) {
            // New Command
            command = data;
            phase = Phase.COMMAND;
            status = RQM | CB; // Busy
            parameterIndex = 0;
            
            // Determine parameter count based on command
            switch (command & 0x1F) { // Mask typical command bits
                case 0x07: // RECALIBRATE
                    parameterCount = 1; // 1 parameter (Drive)
                    break;
                case 0x08: // SENSE INTERRUPT STATUS
                    parameterCount = 0; // No parameters
                    // Execute immediately
                    executeCommand();
                    return;
                case 0x0F: // SEEK
                    parameterCount = 2; // Drive, NCN
                    break;
                case 0x05: // WRITE DATA
                case 0x09: // WRITE DELETED DATA
                case 0x06: // READ DATA
                case 0x0C: // READ DELETED DATA
                case 0x02: // READ DIAGNOSTIC
                case 0x11: // SCAN EQUAL
                    parameterCount = 8; // typical 8 params for read/write
                    break;
                case 0x03: // SPECIFY
                    parameterCount = 2;
                    break;
                default: 
                    // Fallback for unknown commands - expect some params?
                    // For now, assume 0 for unknown to avoid locking
                    logger.log(Level.WARNING, "FDC: Unknown command " + String.format("%02X", command));
                    parameterCount = 0;
                    executeCommand();
                    return;
            }
             logger.log(Level.DEBUG, "FDC: Command " + String.format("%02X", command) + " started, expecting " + parameterCount + " params");

        } else if (phase == Phase.COMMAND) {
            if (parameterIndex < parameters.length) {
                parameters[parameterIndex++] = data;
            }
            if (parameterIndex >= parameterCount) {
                executeCommand();
            }
        }
    }

    private void executeCommand() {
        phase = Phase.EXECUTION;
        // status = RQM | CB | NDM; // Busy, Non-DMA

        // Simulate Execution Phase (instant)
        // In a real emulator, this might be async or checking drive status
        
        // Transition to Result Phase
        phase = Phase.RESULT;
        status = RQM | DIO | CB; // Request Master, Data In (Read), Busy

        resultQueue.clear();

        switch (command & 0x1F) {
            case 0x08: // SENSE INTERRUPT STATUS
                // Returns ST0, PCN
                // ST0: Seek End (bit 5) + Unit Select (bits 0-1) + Head (bit 2)
                resultQueue.add(0x20 | (currentDriveHead & 0x07)); 
                // PCN: Present Cylinder Number for the drive
                resultQueue.add(pcn[currentDriveHead & 0x03]);
                break;
                
            case 0x07: // RECALIBRATE
                // Update current Drive/Head for Sense Interrupt Status
                currentDriveHead = parameters[0] & 0x07;
                // Recalibrate -> Cylinder 0
                pcn[currentDriveHead & 0x03] = 0;

                // Fire Interrupt for Recalibrate completion
                if (intc != null) intc.requestInterrupt(1);

                phase = Phase.IDLE;
                status = RQM;
                break;

            case 0x0F: // SEEK
                // Update current Drive/Head for Sense Interrupt Status
                currentDriveHead = parameters[0] & 0x07;
                // Seek -> New Cylinder (NCN)
                pcn[currentDriveHead & 0x03] = parameters[1];

                // Fire Interrupt for Seek completion
                if (intc != null) intc.requestInterrupt(1);

                phase = Phase.IDLE;
                status = RQM;
                break;

            case 0x05: // WRITE DATA
            case 0x06: // READ DATA
            case 0x02: // READ DIAGNOSTIC
                // Fire Interrupt for Data Transfer completion (Normal Termination)
                if (intc != null) intc.requestInterrupt(1);

                // Returns ST0, ST1, ST2, C, H, R, N
                // ST0: Abnormal Termination (0x40) + Drive/Head
                // This forces Boot failure -> Fallback to BASIC (instead of executing NOPs)
                resultQueue.add(0x40 | (parameters[0] & 0x07)); // ST0
                resultQueue.add(0x00); // ST1
                resultQueue.add(0x00); // ST2
                resultQueue.add(0x00); // C
                resultQueue.add(0x00); // H
                resultQueue.add(0x00); // R
                resultQueue.add(0x02); // N (Length code)
                break;
                
            default:
                // Default: Return to Idle
                phase = Phase.IDLE;
                status = RQM;
                break;
        }
    }

    /** legacy getter */
    public int getData() {
        return 0;
    }
}
