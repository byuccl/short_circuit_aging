package edu.byu.shortCircuits.shorts;

import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.design.Net;
import com.xilinx.rapidwright.device.*;
import edu.byu.shortCircuits.rapidWrightTools.RapidWrightTools;
import edu.byu.shortCircuits.rapidWrightTools.BELID;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static edu.byu.shortCircuits.rapidWrightTools.RapidWrightTools.countLut6;

/*
 * This class contains tools to find and create shorts. It also contains a list of all shorts created through the
 * createShort method of this class
 */
public class ShortTools
{

    @Deprecated
    public static void createShort(Design d, Site site0, Site site1, BEL bel0, BEL bel1, String shortedNode) {
        Short s = new Short(d, site0, site1, bel0, bel1);
        s.routeShort(d, shortedNode);
    }

    @Deprecated
    public static void createShort(Design d, Site site0, Site site1, String bel0, String bel1, String shortedNode) {
        Short s = new Short(d, site0, site1, site0.getBEL(bel0), site1.getBEL(bel1));
        s.routeShort(d, shortedNode);
    }

    /**
     * Finds all of the PIPs/nodes that can be used to create a short between the two provided ShortBEL objects. Note
     * that the two BELs must be within the same tile.
     * @param bel0 The first BEL that will be shorted.
     * @param bel1 The second BEL that will be shorted.
     * @param justNodes Designates whether the returning list contains the string of PIPs or just the Node
     *                  True: The returning list contains the strings of just the nodes
     *                  False: The returning list contains the strings of the whole PIP (which also contain the node)
     *                  Note that the createShort methods require the string of just the node, so this parameter
     *                  should be true if using it with the createShort methods.
     * @return A list of all of the PIPs or all of the nodes that can be used to create a short between the
     *        two BELs.
     */
    public static ArrayList<String> findShorts(ShortCell bel0, ShortCell bel1, boolean justNodes)
    {
        //These are the wires that connect the smaller switchbox to the site pins of the two shortBels
        Wire wire0 = bel0.getTileWire();
        Wire wire1 = bel1.getTileWire();

        //This will contain the two pips that connect need to be turned on in order to connect the two BELs to the
        //larger interconnect tile (switchbox)
        ArrayList<PIP> pips = new ArrayList<>();

        //this gets all of the pips connected to wire0
        for (PIP pip : wire0.getForwardPIPs())
        {
            //this if statement gets the PIP that will eventually connect us to the larger switchbox
            if(pip.getPIPType() == PIPType.DIRECTIONAL_NOT_BUFFERED21)
            {
                pips.add(pip);
                break;
            }
        }

        //this gets all of the pips connected to wire1
        for (PIP pip : wire1.getForwardPIPs())
        {
            //this if statement gets the PIP that will eventually connect us to the larger switchbox
            if(pip.getPIPType() == PIPType.DIRECTIONAL_NOT_BUFFERED21)
            {
                pips.add(pip);
                break;
            }
        }

        //the list that will hold all possible shorts found
        ArrayList<String> potentialShorts = new ArrayList<>();

        //finds all of the pips in the larger switch box that can be connected to BEL0
        for (PIP pip0 : pips.get(0).getEndWire().getNode().getAllDownhillPIPs())
        {
            //finds all of the pips in the larger switch box that can be connected to BEL0
            for (PIP pip1 : pips.get(1).getEndWire().getNode().getAllDownhillPIPs())
            {
                // adds the pips to the net if the two end nodes of the two pips are the same as each other
                if (pip0.getEndWire().equals(pip1.getEndWire()))
                {
                    if(justNodes == true) //only adds the name of the end node if the justNodes parameter is true
                    {
                        potentialShorts.add(pip0.getEndWireName());
                    }
                    else //otherwise we will add the entire pips to the list
                    {
                        potentialShorts.add(pip0.toString());
                        potentialShorts.add(pip1.toString());
                    }
                }
            }
        }

        //returns the list of the possible shorts. Note that if no shorts are found this is empty
        return potentialShorts;
    }

    /**
     * Finds all of the PIPs/nodes that can be used to create a short between the two provided BEL objects. Note that
     * the two BELs must be within the same tile.
     * @param d The design that will contain the short
     * @param site0 The site that contains bel0
     * @param site1 The site that contains bel1
     * @param bel0 The first BEL that will be shorted.
     * @param bel1 The second BEL that will be shorted.
     * @param justNodes Designates whether the returning list contains the string of PIPs or just the Node
     *                  True: The returning list contains the strings of just the nodes
     *                  False: The returning list contains the strings of the whole PIP (which also contain the node)
     *                  Note that the createShort methods require the string of just the node, so this parameter
     *                  should be true if using it with the createShort methods.
     * @return A list of all of the PIPs or all of the nodes that can be used to create a short between the
     *        two BELs.
     */
    public static ArrayList<String> findShorts(Design d, Site site0, Site site1, BEL bel0, BEL bel1, boolean justNodes)
    {
        //makes sure that the sites are in the same tile
        if(!site0.getTile().equals(site1.getTile()))
        {
            throw new RuntimeException("ERROR: " + site0.getName() + " and " + site1.getName()
                    + " are not within the same tile");
        }

        //creates unplaced shortBel objects to be passed to the first createShort method
        ShortCell shortCell0 = new ShortCell(d, site0, bel0, LogicValue.LOW);
        ShortCell shortCell1 = new ShortCell(d, site1, bel1, LogicValue.HIGH);

        //passes the newly created shortBEL objects to the first findShort method
        return findShorts(shortCell0, shortCell1, justNodes);
    }

    /**
     * Finds all of the PIPs/nodes that can be used to create a short between the BELs designated by the two Strings.
     * Note that the two BELs must be within the same tile.
     * @param d The design that will contain the short
     * @param site0 The site that contains bel0
     * @param site1 The site that contains bel1
     * @param bel0 The string of the first BEL that will be shorted.
     * @param bel1 The string of the second BEL that will be shorted.
     * @param justNodes Designates whether the returning list contains the string of PIPs or just the Node
     *                  True: The returning list contains the strings of just the nodes
     *                  False: The returning list contains the strings of the whole PIP (which also contain the node)
     *                  Note that the createShort methods require the string of just the node, so this parameter
     *                  should be true if using it with the createShort methods.
     * @return A list of all of the PIPs or all of the nodes that can be used to create a short between the
     *        two BELs.
     */
    public static ArrayList<String> findShorts(Design d, Site site0, Site site1, String bel0, String bel1, boolean justNodes)
    {
        //creates bel objects based on the bel strings and passes it to the second findShort method
        return findShorts(d, site0, site1, site0.getBEL(bel0), site1.getBEL(bel1), justNodes);
    }
}
