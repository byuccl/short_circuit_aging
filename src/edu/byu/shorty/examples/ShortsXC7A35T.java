package edu.byu.shorty.examples.ShortsXC7A35T;

import edu.byu.shorty.shorts.ShortedDesign;

import com.xilinx.rapidwright.device.Site;
import com.xilinx.rapidwright.design.Design;

import java.io.File;

public class SsXC7A35T
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

        // place short circuits on the top half of the FPGA.
        for (int y = Y_MAX; y >= Y_MIN; y--) {
            for (int x = X_MIN; x < Y_MIN; x+=2) {
                Site site = d.getDevice().getSite(String.format("SLICE_X%dY%d", x, y));
                if (site != null)                    
                    sd.placeShortedTile(x, y);
            }
        }
    }

    sd2.routeShorts(SHORT_CIRCUIT_DENSITY);

    new File(CHECKPOINT_DIR).mkdir();
    d.writeCheckpoint(CHECKPOINT_DIR + "/" + FILE_NAME);
}