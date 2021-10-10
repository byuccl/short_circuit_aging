package edu.byu.shorty.shorts;

import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.design.Net;
import com.xilinx.rapidwright.device.*;
import edu.byu.shorty.rapidwrighttools.RapidWrightTools;
import edu.byu.shorty.rapidwrighttools.BELID;
import edu.byu.shorty.rapidwrighttools.BELConfig;

import java.util.ArrayList;
import java.util.List;

import static edu.byu.shorty.rapidwrighttools.RapidWrightTools.countLut6;

/*
 * This class contains tools to find and create shorts. It also contains a list of all shorts created through the
 * createShort method of this class
 */
public class ShortTools {
    private final static int MAX_35T_BELS = 20800;
    private static ArrayList<Short> shorts = new ArrayList<>(); // A List containing all shorts made through one of the
                                                                // createShort methods.

    // Runs the test function of this class
    public static void main(String[] args) {
        // runTest();
    }

    /**
     * wrapper for the createShortedRegion that maintains backwards compatibility.
     * Creates a region of shorts in the region bounded by the parameters. This
     * function has been optimized for the ARTY A7-35t to produce the most
     * efficiently damaging shorts and may not work as well or at all for other
     * FPGAs.
     * 
     * @param xMin minimum x bound for short region
     * @param xMax maximum x bound for short region
     * @param yMin minimum y bound for short region
     * @param yMax maximum y bound for short region
     */
    public static void createShortedRegion(Design d, int xMin, int xMax, int yMin, int yMax) {
        createShortedRegion(d, xMin, xMax, yMin, yMax, null);
    }

    /**
     * Creates a region of shorts in the region bounded by the parameters. This
     * function has been optimized for the ARTY A7-35t to produce the most
     * efficiently damaging shorts and may not work as well or at all for other
     * FPGAs.
     * 
     * @param d         Design to put the shorts on
     * @param xMin      minimum x bound for short region
     * @param xMax      maximum x bound for short region
     * @param yMin      minimum y bound for short region
     * @param yMax      maximum y bound for short region
     * @param enableNet maximum y bound for short region
     * @param enableNet If not null, then one of the BELs will be an inverter or
     *                  buffer with the input attached to this net. This net can be
     *                  used as an enable or disable signal for the shorts.
     */
    public static void createShortedRegion(Design d, int xMin, int xMax, int yMin, int yMax, Net enableNet) {
        int maxLuts = Integer.MAX_VALUE;
        if (d.getDevice().equals("xc7a35ticsg324-1L"))
            maxLuts = MAX_35T_BELS;

        int lutCount = countLut6(d);

        outerLoop: for (int y = yMin; y <= yMax; y++) {
            for (int x = xMin; x <= xMax; x++) {
                String siteString = String.format("SLICE_X%dY%d", x, y);
                Site site = d.getDevice().getSite(siteString);

                if (site == null) {
                    continue;
                }

                for (BELID id : BELID.values()) {
                    if (lutCount >= maxLuts)
                        break outerLoop;
                    // System.out.printf("\tSLICE_X%dY%d/%s\n", x, y, id.toString());

                    String bel0 = id.toString() + ShortBELType.LUT.getBELPostfix();
                    String bel1 = id.toString() + ShortBELType.REG_INIT.getBELPostfix();

                    String shortedNode = site.getSiteIndexInTile() == 0 ? "NE2BEG" + String.valueOf(id.toInt())
                            : "SW2BEG" + String.valueOf(id.toInt());

                    createShort(d, site, site, bel0, bel1, shortedNode, enableNet);

                    lutCount++;
                }
            }
        }
    }

    /**
     * Creates a short between the two provided ShortBEL objects. The short is
     * connected at the provided shortedNode node. Note that the two BELs must be
     * within the same tile.
     * 
     * @param bel0 The first BEL that will be shorted.
     * @param bel1 The second BEL that will be shorted.
     */
    public static void createMultiShort(ShortBEL bel0, ShortBEL bel1) {
        // Makes sure that the two provided BELs have opposing output levels so a short
        // can actually be created
        if (bel0.getConfig() == bel1.getConfig()
                && (bel0.getConfig() == BELConfig.LOW || bel0.getConfig() == BELConfig.HIGH)) {
            String logicLevel = bel0.getConfig() == BELConfig.LOW ? "LOW" : "HIGH";
            throw new RuntimeException("ERROR: The two BELs, " + bel0.getBel().getName() + " and "
                    + bel1.getBel().getName() + " have the same output logic level (" + logicLevel + ") and therefore "
                    + "won't result in a short when tied together!");
        }

        // creates a net that will short the two bels
        Net net = bel0.getDesign().createNet(bel0.getSite().getName() + "_" + bel0.getBel().getName() + "_"
                + bel1.getBel().getName() + "_multishorted_net");

        // logically connects the outputs of the two bels
        net.connect(bel0.getCell(), bel0.getBelOutputPin());
        net.connect(bel1.getCell(), bel1.getBelOutputPin());

        // This will contain the two pips that connect need to be turned on in order to
        // connect the two BELs to the
        // larger interconnect tile (switchbox)
        ArrayList<PIP> pips = new ArrayList<>();

        // These are the wires that connect the smaller switchbox to the site pins of
        // the two shortBels
        Wire wire0 = bel0.getTileWire();
        Wire wire1 = bel1.getTileWire();

        // this gets all of the pips connected to wire0
        for (PIP pip : wire0.getForwardPIPs()) {
            // this if statement gets the PIP that will eventually connect us to the larger
            // switchbox
            if (pip.getPIPType() == PIPType.DIRECTIONAL_NOT_BUFFERED21) {
                pips.add(pip);
                net.addPIP(pip);
                break;
            }
        }

        // this gets all of the pips connected to wire1
        for (PIP pip : wire1.getForwardPIPs()) {
            // this if statement gets the PIP that will eventually connect us to the larger
            // switchbox
            if (pip.getPIPType() == PIPType.DIRECTIONAL_NOT_BUFFERED21) {
                pips.add(pip);
                net.addPIP(pip);
                break;
            }
        }

        boolean shortSuccess = false;// used to report if creating a short was successful

        // the indexes of the two pips found in the two for loops above
        int pip0Index = 0;
        int pip1Index = 1;

        ArrayList<String> potentialShorts = new ArrayList<>();

        // finds all of the pips in the larger switch box that can be connected to BEL0
        for (PIP pip0 : pips.get(0).getEndWire().getNode().getAllDownhillPIPs()) {
            // finds all of the pips in the larger switch box that can be connected to BEL0
            for (PIP pip1 : pips.get(1).getEndWire().getNode().getAllDownhillPIPs()) {
                // adds the pips to the net if the two end nodes of the two pips are the same as
                // each other
                if (pip0.getEndWire().equals(pip1.getEndWire())) {
                    net.addPIP(pip0);
                    net.addPIP(pip1);
                }
            }
        }

        net.lockRouting();
        Short short_inst = new Short(bel0, bel1, net);
        shorts.add(short_inst);
    }

    /**
     * Creates a short between the two provided ShortBEL objects. The short is
     * connected at the provided shortedNode node. Note that the two BELs must be
     * within the same tile.
     * 
     * @param bel0        The first BEL that will be shorted.
     * @param bel1        The second BEL that will be shorted.
     * @param shortedNode The node that will be the connecting point of the two
     *                    BELs. I.e. NE2BEG0
     */
    public static void createShort(ShortBEL bel0, ShortBEL bel1, String shortedNode) {
        // Makes sure that the two provided BELs have opposing output levels so a short
        // can actually be created
        if (bel0.getConfig() == bel1.getConfig()
                && (bel0.getConfig() == BELConfig.LOW || bel0.getConfig() == BELConfig.HIGH)) {
            String logicLevel = bel0.getConfig() == BELConfig.LOW ? "LOW" : "HIGH";
            throw new RuntimeException("ERROR: The two BELs, " + bel0.getBel().getName() + " and "
                    + bel1.getBel().getName() + " have the same output logic level (" + logicLevel + ") and therefore "
                    + "won't result in a short when tied together!");
        }

        // creates a net that will short the two bels
        Net net = bel0.getDesign().createNet(bel0.getSite().getTile().getName() + "_" + shortedNode + "_net");

        // logically connects the outputs of the two bels
        net.connect(bel0.getCell(), bel0.getBelOutputPin());
        net.connect(bel1.getCell(), bel1.getBelOutputPin());

        // This will contain the two pips that connect need to be turned on in order to
        // connect the two BELs to the
        // larger interconnect tile (switchbox)
        ArrayList<PIP> pips = new ArrayList<>();

        // These are the wires that connect the smaller switchbox to the site pins of
        // the two shortBels
        Wire wire0 = bel0.getTileWire();
        Wire wire1 = bel1.getTileWire();

        // this gets all of the pips connected to wire0
        for (PIP pip : wire0.getForwardPIPs()) {
            // this if statement gets the PIP that will eventually connect us to the larger
            // switchbox
            if (pip.getPIPType() == PIPType.DIRECTIONAL_NOT_BUFFERED21) {
                pips.add(pip);
                net.addPIP(pip);
                break;
            }
        }

        // this gets all of the pips connected to wire1
        for (PIP pip : wire1.getForwardPIPs()) {
            // this if statement gets the PIP that will eventually connect us to the larger
            // switchbox
            if (pip.getPIPType() == PIPType.DIRECTIONAL_NOT_BUFFERED21) {
                pips.add(pip);
                net.addPIP(pip);
                break;
            }
        }

        boolean shortSuccess = false;// used to report if creating a short was successful

        // the indexes of the two pips found in the two for loops above
        int pip0Index = 0;
        int pip1Index = 1;

        // finds all of the pips in the larger switch box that can be connected to BEL0
        for (PIP pip0 : pips.get(pip0Index).getEndWire().getNode().getAllDownhillPIPs()) {
            // finds all of the pips in the larger switch box that can be connected to BEL1
            for (PIP pip1 : pips.get(pip1Index).getEndWire().getNode().getAllDownhillPIPs()) {
                // adds the pips to the net if the two end nodes of the two pips are the same as
                // each other and the
                // shortedNode parameter
                if (pip0.getEndWire().equals(pip1.getEndWire())
                        && pip0.getEndWire().getWireName().equals(shortedNode)) {
                    net.addPIP(pip0);
                    net.addPIP(pip1);
                    shortSuccess = true;
                }
            }
        }

        // throws an exception if the short cannot be created with the provided
        // shortedNode parameter
        if (shortSuccess == false) {
            throw new RuntimeException("ERROR: The node " + shortedNode + " either does not exist or cannot be "
                    + "directly connected to BEL " + bel0.getSite().getName() + "/" + bel0.getBel().getName()
                    + " and/or BEL " + bel1.getSite().getName() + "/" + bel1.getBel().getName() + "!");
        } else// if the short was created, the routing is locked, and saved as a Short object
              // in the shorts list.
        {
            net.lockRouting();
            Short short_inst = new Short(bel0, bel1, net);
            shorts.add(short_inst);
        }
    }

    /**
     * Creates a short between the two provided BEL objects. The short is connected
     * at the provided shortedNode node. Note that the two BELs must be within the
     * same tile.
     * 
     * @param d         The design that will contain the short
     * @param site0     The site that contains bel0 (will be configured low
     * @param site1     The site that contains bel1
     * @param bel0      The first BEL that will be shorted.
     * @param bel1      The second BEL that will be shorted.
     * @param enableNet If not null, then one of the BELs will be an inverter or
     *                  buffer with the input attached to this net. This net can be
     *                  used as an enable or disable signal for the shorts.
     */
    public static void createShort(Design d, Site site0, Site site1, BEL bel0, BEL bel1, String shortedNode,
            Net enableNet) {
        // makes sure that the sites are in the same tile
        if (!site0.getTile().equals(site1.getTile())) {
            throw new RuntimeException(
                    "ERROR: " + site0.getName() + " and " + site1.getName() + " are not within the same tile");
        }

        BELConfig config0;
        BELConfig config1;
        if (enableNet != null && bel0.getBELType().contains("LUT")) {
            config0 = BELConfig.INV;
            config1 = BELConfig.HIGH;

        } else if (enableNet != null && bel1.getBELType().contains("LUT")) {
            config0 = BELConfig.LOW;
            config1 = BELConfig.BUF;
        } else {
            config0 = BELConfig.LOW;
            config1 = BELConfig.HIGH;
        }

        // creates placed shortBel objects to be passed to the first createShort method
        ShortBEL shortBEL0 = new ShortBEL(d, site0, bel0, config0);
        shortBEL0.createAndPlaceCell();

        ShortBEL shortBEL1 = new ShortBEL(d, site1, bel1, config1);
        shortBEL1.createAndPlaceCell();

        if (shortBEL0.getConfig() == BELConfig.INV) {
            enableNet.connect(shortBEL0.getCell(), "I0");
        } else if (shortBEL1.getConfig() == BELConfig.BUF) {
            enableNet.connect(shortBEL1.getCell(), "I0");
        }

        // passes the newly created shortBEL objects to the first createShort method
        createShort(shortBEL0, shortBEL1, shortedNode);
    }

    /**
     * Creates a short between the two BELs designated by the two provided Strings.
     * This short may be configurable via an inverter or buffer that has a input
     * hooked up to the enableNet parameter. The short is connected at the provided
     * shortedNode node. Note that the two BELs must be within the same tile.
     * 
     * @param d           The design that will contain the short
     * @param site0       The site that contains bel0
     * @param site1       The site that contains bel1
     * @param bel0        The string of the first BEL that will be shorted.
     * @param bel1        The string of the second BEL that will be shorted.
     * @param shortedNode The node that will be the connecting point of the two
     *                    BELs. I.e. NE2BEG0
     * @param enableNet   If not null, then one of the BELs will be an inverter or
     *                    buffer with the input attached to this net. This net can
     *                    be used as an enable or disable signal for the shorts.
     */
    public static void createShort(Design d, Site site0, Site site1, String bel0, String bel1, String shortedNode,
            Net enableNet) {
        // creates bel objects based on the bel strings and passes it to the second
        // createShort method
        createShort(d, site0, site1, site0.getBEL(bel0), site1.getBEL(bel1), shortedNode, enableNet);
    }

    /**
     * Creates a short between the two BELs designated by the two provided Strings.
     * The short is connected at the provided shortedNode node. Note that the two
     * BELs must be within the same tile.
     * 
     * @param d           The design that will contain the short
     * @param site0       The site that contains bel0
     * @param site1       The site that contains bel1
     * @param bel0        The string of the first BEL that will be shorted.
     * @param bel1        The string of the second BEL that will be shorted.
     * @param shortedNode The node that will be the connecting point of the two
     *                    BELs. I.e. NE2BEG0
     */
    public static void createShort(Design d, Site site0, Site site1, String bel0, String bel1, String shortedNode) {
        // creates bel objects based on the bel strings and passes it to the second
        // createShort method
        createShort(d, site0, site1, site0.getBEL(bel0), site1.getBEL(bel1), shortedNode, null);
    }

    /**
     * Finds all of the PIPs/nodes that can be used to create a short between the
     * two provided ShortBEL objects. Note that the two BELs must be within the same
     * tile.
     * 
     * @param bel0      The first BEL that will be shorted.
     * @param bel1      The second BEL that will be shorted.
     * @param justNodes Designates whether the returning list contains the string of
     *                  PIPs or just the Node True: The returning list contains the
     *                  strings of just the nodes False: The returning list contains
     *                  the strings of the whole PIP (which also contain the node)
     *                  Note that the createShort methods require the string of just
     *                  the node, so this parameter should be true if using it with
     *                  the createShort methods.
     * @return A list of all of the PIPs or all of the nodes that can be used to
     *         create a short between the two BELs.
     */
    public static ArrayList<String> findShorts(ShortBEL bel0, ShortBEL bel1, boolean justNodes) {
        // These are the wires that connect the smaller switchbox to the site pins of
        // the two shortBels
        Wire wire0 = bel0.getTileWire();
        Wire wire1 = bel1.getTileWire();

        // This will contain the two pips that connect need to be turned on in order to
        // connect the two BELs to the
        // larger interconnect tile (switchbox)
        ArrayList<PIP> pips = new ArrayList<>();

        // this gets all of the pips connected to wire0
        for (PIP pip : wire0.getForwardPIPs()) {
            // this if statement gets the PIP that will eventually connect us to the larger
            // switchbox
            if (pip.getPIPType() == PIPType.DIRECTIONAL_NOT_BUFFERED21) {
                pips.add(pip);
                break;
            }
        }

        // this gets all of the pips connected to wire1
        for (PIP pip : wire1.getForwardPIPs()) {
            // this if statement gets the PIP that will eventually connect us to the larger
            // switchbox
            if (pip.getPIPType() == PIPType.DIRECTIONAL_NOT_BUFFERED21) {
                pips.add(pip);
                break;
            }
        }

        // the list that will hold all possible shorts found
        ArrayList<String> potentialShorts = new ArrayList<>();

        // finds all of the pips in the larger switch box that can be connected to BEL0
        for (PIP pip0 : pips.get(0).getEndWire().getNode().getAllDownhillPIPs()) {
            // finds all of the pips in the larger switch box that can be connected to BEL0
            for (PIP pip1 : pips.get(1).getEndWire().getNode().getAllDownhillPIPs()) {
                // adds the pips to the net if the two end nodes of the two pips are the same as
                // each other
                if (pip0.getEndWire().equals(pip1.getEndWire())) {
                    if (justNodes == true) // only adds the name of the end node if the justNodes parameter is true
                    {
                        potentialShorts.add(pip0.getEndWireName());
                    } else // otherwise we will add the entire pips to the list
                    {
                        potentialShorts.add(pip0.toString());
                        potentialShorts.add(pip1.toString());
                    }
                }
            }
        }

        // returns the list of the possible shorts. Note that if no shorts are found
        // this is empty
        return potentialShorts;
    }

    /**
     * Finds all of the PIPs/nodes that can be used to create a short between the
     * two provided BEL objects. Note that the two BELs must be within the same
     * tile.
     * 
     * @param d         The design that will contain the short
     * @param site0     The site that contains bel0
     * @param site1     The site that contains bel1
     * @param bel0      The first BEL that will be shorted.
     * @param bel1      The second BEL that will be shorted.
     * @param justNodes Designates whether the returning list contains the string of
     *                  PIPs or just the Node True: The returning list contains the
     *                  strings of just the nodes False: The returning list contains
     *                  the strings of the whole PIP (which also contain the node)
     *                  Note that the createShort methods require the string of just
     *                  the node, so this parameter should be true if using it with
     *                  the createShort methods.
     * @return A list of all of the PIPs or all of the nodes that can be used to
     *         create a short between the two BELs.
     */
    public static ArrayList<String> findShorts(Design d, Site site0, Site site1, BEL bel0, BEL bel1,
            boolean justNodes) {
        // makes sure that the sites are in the same tile
        if (!site0.getTile().equals(site1.getTile())) {
            throw new RuntimeException(
                    "ERROR: " + site0.getName() + " and " + site1.getName() + " are not within the same tile");
        }

        // creates unplaced shortBel objects to be passed to the first createShort
        // method
        ShortBEL shortBEL0 = new ShortBEL(d, site0, bel0, BELConfig.LOW);
        ShortBEL shortBEL1 = new ShortBEL(d, site1, bel1, BELConfig.HIGH);

        // passes the newly created shortBEL objects to the first findShort method
        return findShorts(shortBEL0, shortBEL1, justNodes);
    }

    /**
     * Finds all of the PIPs/nodes that can be used to create a short between the
     * BELs designated by the two Strings. Note that the two BELs must be within the
     * same tile.
     * 
     * @param d         The design that will contain the short
     * @param site0     The site that contains bel0
     * @param site1     The site that contains bel1
     * @param bel0      The string of the first BEL that will be shorted.
     * @param bel1      The string of the second BEL that will be shorted.
     * @param justNodes Designates whether the returning list contains the string of
     *                  PIPs or just the Node True: The returning list contains the
     *                  strings of just the nodes False: The returning list contains
     *                  the strings of the whole PIP (which also contain the node)
     *                  Note that the createShort methods require the string of just
     *                  the node, so this parameter should be true if using it with
     *                  the createShort methods.
     * @return A list of all of the PIPs or all of the nodes that can be used to
     *         create a short between the two BELs.
     */
    public static ArrayList<String> findShorts(Design d, Site site0, Site site1, String bel0, String bel1,
            boolean justNodes) {
        // creates bel objects based on the bel strings and passes it to the second
        // findShort method
        return findShorts(d, site0, site1, site0.getBEL(bel0), site1.getBEL(bel1), justNodes);
    }

    public static ArrayList<Short> getShorts() {
        return shorts;
    }

    /**
     * Runs a test that tests the most of the methods and classes found in the
     * edu.byu.ecen.shorts package as well as a few found in the RapidWrightTools
     * class. This test/demo fills the Arty-A7 board with shorts using every LUT6
     * and REG_INIT flip-flop on the chip and outputs a checkpoint with the results.
     * Note that if one were to create a bitstream from the resulting checkpoint and
     * tried to program the Arty board with it, it would immediately turn off the
     * board do to an excess current draw ( > 2.5-3 Amps). However, editing the
     * ranges contained in the final int variables at the beginning of the function
     * makes it easy to create shorts within whatever rectangular region of the chip
     * that one desires, and thus this test can be scaled down to a size that
     * creates a bitstream that doesn't draw more current than the regulators on the
     * Arty board can handle.
     */
    private static void runTest() {
        Design d = new Design("ShortTools_test", "xc7a35ticsg324-1L");

        // the range of the sites to be filled. This should be set up so that the range
        // is the range of sites on the
        // board
        final int X_MIN = 0;
        final int X_MAX = d.getDevice().getColumns();
        final int Y_MIN = 0;
        final int Y_MAX = d.getDevice().getRows();

        // gets all of the logic sites within the range defined above and stores them in
        // the list 'sites'
        ArrayList<Site> sites = RapidWrightTools.getLogicSitesInRange(d.getDevice(), X_MIN, X_MAX, Y_MIN, Y_MAX);

        // The series of for loops below go through every type of LUT and FF in every
        // site and creates a short between
        // them.
        // This first for loop goes through each of the BELID values: A, B, C, and D
        for (BELID ID : BELID.values()) {
            // uses findShorts to create a list of all potential shorts. Since all logic
            // sites in the Arty-A7 have the
            // same general layout, we use an iterator to just get the first arbitrary site
            // out of the 'sites' list.
            // We then use the BEL ID and the BELPostfix for LUTs and REG_INIT flip-flops to
            // generate the string of the
            // two bels we want to find shorts for (for example, if the current BELID was B,
            // this list would contain all
            // of the nodes that connect B6LUT and BFF for any logic site). Uses the sort
            // method to sort the potential
            // shorts so that all of the Nodes will match up to each other by index.
            String bel0 = ID.toString() + ShortBELType.LUT.getBELPostfix(); // the first bel to be shorted
            String bel1 = ID.toString() + ShortBELType.REG_INIT.getBELPostfix(); // the second bel to be shorted
            Site arbitrarySite = sites.iterator().next(); // the arbitrary logic site that will be used to find shorts
            ArrayList<String> potentialShorts = findShorts(d, arbitrarySite, sites.iterator().next(), bel0, bel1, true); // The
                                                                                                                         // list
                                                                                                                         // of
                                                                                                                         // potential
                                                                                                                         // shorts
                                                                                                                         // (just
                                                                                                                         // the
                                                                                                                         // nodes)
            potentialShorts.sort(null); // sorting the nodes for

            // goes through all of the logic sites and creates shorts between all of the
            // 6LUT and FF that are prefixed
            // with the current BELID
            for (Site site : sites) {
                // get the short node used for this site. Since the two sites in a tile share an
                // interconnect tile, we
                // have to use two types of nodes based on the index of the site within the
                // tile. The 7 and 16 are fairly
                // arbitrary and can be replaced with any index for shorting different nodes. Do
                // note that we have seen
                // different shorts give output different amounts of current (see the shorts
                // profiler experiment for
                // more details.
                String shortedNode = site.getSiteIndexInTile() == 0 ? potentialShorts.get(7) : potentialShorts.get(16);

                // uses the createShort function to create a short from the two bels defined
                // above in the current site
                createShort(d, site, site, bel0, bel1, shortedNode);
            }
        }

        // At the end, we must route all sites. We can then output a checkpoint which
        // will be stored in check_output and
        // can be opened in vivado
        d.routeSites();
        d.writeCheckpoint("checkpoint_output/" + "test.dcp");
    }
}
