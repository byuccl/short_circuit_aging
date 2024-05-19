package edu.byu.shortCircuits.examples.ManualShortedDesign;

import edu.byu.shortCircuits.shorts.ShortedDesign;

import com.xilinx.rapidwright.design.Design;

import java.io.File;

/**
 * Example that shows how to manually place shorts in a designated area and route them.
 */
public class ManualShortedDesign
{
    private final static String EXPERIMENT_NAME = "shorts_xc7a35t";
    private final static String FILE_NAME = EXPERIMENT_NAME + ".dcp";
    private final static String CHECKPOINT_DIR = "checkpoints/" + EXPERIMENT_NAME;

    private final static String PART = "xc7a35ticsg324-1L";

    private final static int X_MIN = 0;
    private final static int X_MAX = 65;
    private final static int Y_MIN = 75;
    private final static int Y_MAX = 149;

    // How many short circuits to create for each LUT-FF pair.
    private final static int SHORT_CIRCUIT_DENSITY = 2;

    public static void main(String[] args) {
        Design d = new Design("Shorts", PART);
        ShortedDesign sd = new ShortedDesign(d);

        // Place short circuits on the top half of the FPGA.
        // The last argument designates how many shorts are created per LUT-FF pair.
        sd.createShortedRegion(X_MIN, X_MAX, Y_MIN, Y_MAX, SHORT_CIRCUIT_DENSITY);

        new File(CHECKPOINT_DIR).mkdir();
        d.writeCheckpoint(CHECKPOINT_DIR + "/" + FILE_NAME);
    }
}