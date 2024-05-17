package edu.byu.shorty.shorts;

import com.xilinx.rapidwright.design.Cell;
import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.design.Net;
import com.xilinx.rapidwright.device.*;
import com.xilinx.rapidwright.edif.*;
import com.xilinx.rapidwright.util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * This class acts as a convenient container that holds all of the RapidWright objects that make up a short
 */
public class Short
{
    private ShortCell cell0;
    private ShortCell cell1;
    private Net net; // A net that contains all of the PIPs that connect the shorted BELs
//    private List<Net> configNets = new LinkedList<>();


    public Short(Design d, ShortCell cell0, ShortCell cell1, Net net) {
        this.cell0 = cell0;
        this.cell1 = cell1;

        Cell c0 = d.getCell(cell0.getCellName());
        if (c0 == null) {
            throw new RuntimeException("Error! Cell " + c0 + "does not exist in Design!");
        }
        cell0.setCell(c0);

        Cell c1 = d.getCell(cell1.getCellName());
        if (c1 == null) {
            throw new RuntimeException("Error! Cell " + c1 + "does not exist in Design!");
        }
        cell1.setCell(c1);

        this.net = d.getNet(net.getName());
        if (this.net == null) {
            throw new RuntimeException("Error! Net " + net + "does not exist in Design!");
        }

//        for (Net configNet : configNets) {
//            Net n = d.getNet(configNet.getName());
//            if (n == null) {
//                throw new RuntimeException("Error! Net " + configNet + "does not exist in Design!");
//            }
//            this.configNets.add(n);
//        }
    }


    public Short(Design d, String site0, String site1, String bel0, String bel1) {
        Site s0 = d.getDevice().getSite(site0);
        Site s1 = d.getDevice().getSite(site1);
        BEL b0 = s0.getBEL(bel0);
        BEL b1 = s1.getBEL(bel1);
        placeShort(d, s0, s1, b0, b1);
        createShortedNet(d);
    }

    public Short(Design d, Site site0, Site site1, BEL bel0, BEL bel1) {
        placeShort(d, site0, site1, bel0, bel1);
        createShortedNet(d);
    }

    private void placeShort(Design d, Site site0, Site site1, BEL bel0, BEL bel1) {
        //makes sure that the sites are in the same tile
        if(!site0.getTile().equals(site1.getTile()))
        {
            throw new RuntimeException("ERROR: " + site0.getName() + " and " + site1.getName()
                    + " are not within the same tile");
        }

        LogicValue config0 = LogicValue.LOW;
        LogicValue config1 = LogicValue.HIGH;

        //creates placed shortBel objects to be passed to the first createShort method
        cell0 = new ShortCell(d, site0, bel0, config0);
        cell1 = new ShortCell(d, site1, bel1, config1);
    }

    public Net routeShort(Design d, Collection<Wire> usedWires, int numOfShorts) {
        PIP pip0 = routeToSwitchbox(cell0.getTileWire());
        PIP pip1 = routeToSwitchbox(cell1.getTileWire());

        int shortCount = 0;
        //finds all of the pips in the larger switch box that can be connected to BEL0
        for (PIP shortPip0 : pip0.getEndNode().getAllDownhillPIPs())
        {
            //finds all of the pips in the larger switch box that can be connected to BEL0
            for (PIP shortPip1 : pip1.getEndNode().getAllDownhillPIPs())
            {
                // adds the pips to the net if the two end nodes of the two pips are the same as each other
                if (shortPip0.getEndWire().equals(shortPip1.getEndWire()) && !usedWires.contains(shortPip0.getEndWire()))
                {
                    net.addPIP(shortPip0);
                    net.addPIP(shortPip1);
                    shortCount++;
                    if (shortCount == numOfShorts) {
                        net.lockRouting();
                        return net;
                    }
                }
            }
        }

        return null;
    }

    public Net routeShort(Design d, String shortedNode) {

        PIP pip0 = routeToSwitchbox(cell0.getTileWire());
        PIP pip1 = routeToSwitchbox(cell1.getTileWire());

        //finds all of the pips in the larger switch box that can be connected to BEL0
        for (PIP shortPip0 : pip0.getEndNode().getAllDownhillPIPs())
        {
            //finds all of the pips in the larger switch box that can be connected to BEL0
            for (PIP shortPip1 : pip1.getEndNode().getAllDownhillPIPs())
            {
                // adds the pips to the net if the two end nodes of the two pips are the same as each other
                if (shortPip0.getEndWire().equals(shortPip1.getEndWire()) && shortPip0.getEndWire().getWireName().equals(shortedNode))
                {
                    net.addPIP(shortPip0);
                    net.addPIP(shortPip1);
                    net.lockRouting();
                    return net;
                }
            }
        }

        return null;
    }

    public void connectConfigNet(Net configNet) {
        for(int i = 0; i < 6; i++)
        {
            connectConfigNet(configNet, i);
        }
    }

    public void connectConfigNet(Net configNet, int pinIdx) {
        String inputPin = "I" + pinIdx;
        Cell lut = cell0.getBel().isLUT() ? cell0.getCell() : cell1.getCell();
        configNet.connect(lut, inputPin);
    }

    private void createShortedNet(Design d) {
        net = d.createNet(cell0.getSite() + "_" + cell0.getBel() + "-" + cell1.getSite() + "_" + cell1.getBel().getName() + "-shorted_net");
        net.connect(cell0.getCell(), cell0.getBelOutputPin());
        net.connect(cell1.getCell(), cell1.getBelOutputPin());
    }

    private PIP routeToSwitchbox(Wire wire) {
        for (PIP pip : wire.getForwardPIPs())
        {
            //this if statement gets the PIP that will eventually connect us to the larger switchbox
            if(pip.getPIPType() == PIPType.DIRECTIONAL_NOT_BUFFERED21)
            {
                net.addPIP(pip);
                return pip;
            }
        }
        return null;
    }

    /**
     * Deletes the short by unplacing the BELs and unrouting the PIPs in the net
     */
    public void deleteShort()
    {
        cell0.unplace();
        cell1.unplace();
        this.net.unroute();
    }

    public ShortCell getFirstCell() {return cell0;}
    public ShortCell getSecondCell() {return cell1;}

    public Net getShortNet() { return net; }
    public void setShortNet(Net net) {this.net = net;}

//    public List<Net> getConfigNets() {return configNets;}

    public static String  getLUTPostfix() {return "6LUT";}
    public static String  getRegInitPostfix() {return "FF";}
    public static String  getFFInitPostfix() {return "5FF";}


}
