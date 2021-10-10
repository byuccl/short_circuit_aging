package edu.byu.shorty.shorts;

import com.xilinx.rapidwright.design.Net;
import com.xilinx.rapidwright.edif.EDIFCellInst;

import java.util.ArrayList;

/**
 * This class acts as a convenient container that holds all of the RapidWright objects that make up a short
 */
public class Short
{
    private ArrayList<ShortBEL> shortBELs; // An array list containing to the two ShortBELs that make up the short
    private Net net; // A net that contains all of the PIPs that connect the shorted BELs
    private boolean exists; // A boolean variable that is true if the short exists, and is false once the short has been
                            // deleted

    /**
     * Returns a new Short object
     * @param shortBEL0 The first BEL in the short
     * @param shortBEL1 The second BEL in the short
     * @param net The net that contains all of the PIPs that connect
     */
    public Short(ShortBEL shortBEL0, ShortBEL shortBEL1, Net net)
    {
        // This makes sure that the two shorts only connected to 2 BELs. This maximizes the amount of current per BEL
        // that can occur in a design, and detects errors in the short making process
//        if(net.getLogicalNet().getPortInsts().size() > 2)
//        {
//            throw new RuntimeException("ERROR: Net " + net.getName() + " is connected to more than two BELs!");
//        }

        //gets the (EDIF) nets and bels of the shorts for error checking
        EDIFCellInst EDIFShortBEL0 = shortBEL0.getCell().getEDIFCellInst();
        EDIFCellInst EDIFShortBEL1 = shortBEL1.getCell().getEDIFCellInst();
        EDIFCellInst EDIFNetBEL0 = net.getLogicalNet().getSourcePortInsts(false).get(0).getCellInst();
        EDIFCellInst EDIFNetBEL1 = net.getLogicalNet().getSourcePortInsts(false).get(1).getCellInst();

        //This makes sure that the two provided BELs are shorted together
//        if(!(EDIFShortBEL0.equals(EDIFNetBEL0) && EDIFShortBEL1.equals(EDIFNetBEL1)))
//        {
//            throw new RuntimeException("ERROR: " + shortBEL0.getBel().getName() + " and " + shortBEL1.getBel().getName()
//                    + " are not shorted together in the net " + net.getName() + "!");
//        }

        // This stores the BELs and the net that make up the short
        this.shortBELs = new ArrayList<>();
        this.shortBELs.add(shortBEL0);
        this.shortBELs.add(shortBEL1);
        this.net = net;
        this.exists = true;
    }

    /**
     * Deletes the short by unplacing the BELs and unrouting the PIPs in the net
     */
    public void deleteShort()
    {
        shortBELs.get(0).unplace();
        shortBELs.get(1).unplace();
        this.net.unroute();
        this.exists = false;
    }

    public ArrayList<ShortBEL> getShortBELs()
    {
        return shortBELs;
    }

    public Net getNet()
    {
        return net;
    }

    /**
     * Returns whether or not the short exists in the design (it does unless deleted)
     * @return true if it exists, false if it has been deleted
     */
    public boolean doesExist()
    {
        return exists;
    }

}
