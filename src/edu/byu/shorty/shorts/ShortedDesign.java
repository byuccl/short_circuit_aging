package edu.byu.shorty.shorts;

import com.xilinx.rapidwright.design.Cell;
import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.design.Net;
import com.xilinx.rapidwright.device.*;
import edu.byu.shorty.rapidWrightTools.BELID;
import edu.byu.shorty.rapidWrightTools.RapidWrightTools;

import java.util.*;

import static edu.byu.shorty.rapidWrightTools.RapidWrightTools.countLut6;

/*
 * This class contains tools to find and create shorts. It also contains a list of all shorts created through the
 * createShort method of this class
 */
public class ShortedDesign
{
    private final static int MAX_35T_LUTS = 20800;
    private Design d;
    private int maxLuts;
    private Set<Wire> usedWires = new HashSet<>();
//    private static ArrayList<Short> shorts = new ArrayList<>(); // A List containing all shorts made through one of the
//                                                                // createShort methods.

    private List<Short> shorts = new ArrayList<>();

    /**
     * creates a new shorted design and adds the finds the used wires from the design.
     * @param d design the shorts will be apart of.
     */
    public ShortedDesign(Design d) {
        this.d = d;

        usedWires = new HashSet<>();
        for (Net net : d.getNets())
        {
            for(PIP pip : net.getPIPs())
            {
                usedWires.add(pip.getEndWire());
            }
        }

        if (d.getDevice().toString().contains("xc7a35t"))
            maxLuts = MAX_35T_LUTS;
        else
            maxLuts = Integer.MAX_VALUE;
    }

    public ShortedDesign(Design d, Collection<Short> shorts) {
        this.d = d;

        usedWires = new HashSet<>();
        for (Net net : d.getNets())
        {
            for(PIP pip : net.getPIPs())
            {
                usedWires.add(pip.getEndWire());
            }
        }

        if (d.getDevice().toString().contains("xc7a35t"))
            maxLuts = MAX_35T_LUTS;
        else
            maxLuts = Integer.MAX_VALUE;

        addShorts(shorts);
    }

    /**
     * Recreates short objects in current design from the collection of shorts. Useful if reloading a shorted design
     * from Vivado
     * @param shorts collection of shorts to recreate in current design.
     */
    public void addShorts(Collection<Short> shorts) {
        for (Short s : shorts) {
            ShortCell c0 = s.getFirstCell();
            ShortCell c1 = s.getSecondCell();
            Net net = s.getShortNet();
            this.shorts.add(new Short(d, c0, c1, net));
        }
    }

    /**
     * Updates set of wires this design currently uses.
     */
    public void updateUsedWires() {
        updateUsedWires(this.d);
    }

    /**
     * Updates wires from a separate design
     * @param otherDesign separate design to update wires from.
     */
    public void updateUsedWires(Design otherDesign) {
        for (Net net : d.getNets())
        {
            for(PIP pip : net.getPIPs())
            {
                usedWires.add(pip.getEndWire());
            }
        }
    }

    /**
     * updates the wires used in the design with the wires from the provided collection.
     * @param wires a collection of wires
     */
    public void updateUsedWires(Collection<Wire> wires) {
        for (Wire wire : wires) {
            this.usedWires.add(wire);
        }
    }

    /**
     * Clears the wires used for the design.
     */
    public void resetWires() {
        usedWires.clear();
    }

    public Short placeShort(int x, int y, BELID id) {
        Site site = d.getDevice().getSite(String.format("SLICE_X%dY%d", x, y));
        return placeShort(site, id);
    }

    public Short placeShort(Site site, BELID id) {
        String belName0 = id + Short.getLUTPostfix(); //6LUT
        String belName1 = id + Short.getRegInitPostfix(); //FF

        return new Short(d, site, site, site.getBEL(belName0), site.getBEL(belName1));
    }

    /**
     * places shorts on a site
     * @param x x coordinate of a site
     * @param y y coordinate of a site
     * @return list of shorts that were created.
     */
    public List<Short> placeShortedSite(int x, int y) {
        List<Short> shorts = new ArrayList<>();

        Site site = d.getDevice().getSite(String.format("SLICE_X%dY%d", x, y));
        for (BELID id : BELID.values()) {
            Short s = placeShort(site, id);
            shorts.add(s);
        }

        this.shorts.addAll(shorts);
        return shorts;
    }

    /**
     * places shorts on a tile
     * @param x x coordinate of a site in the tile
     * @param y y coordinate of a site in the tile
     * @return list of shorts that were created.
     */
    public List<Short> placeShortedTile(int x, int y) {
        if (x % 2 == 1)
            x = x - 1;

        List<Short> shorts = placeShortedSite(x, y);
        shorts.addAll(placeShortedSite(x+1, y));

        return shorts;
    }

    public void createShortConfig() {
        Net configNet = d.getNet("lut_config");
        if (configNet == null)
            configNet = d.createNet("lut_config");
        for (Short s : this.shorts) {
            s.connectConfigNet(configNet);
        }
    }

    public Net routeShort(Short s, int numOfShorts) {
        Net net = s.routeShort(d, usedWires, numOfShorts);
        for(PIP pip : net.getPIPs())
        {
            usedWires.add(pip.getEndWire());
        }
        return net;
    }

    public void routeShorts(int numOfShorts) {
        for (Short s : shorts) {
            routeShort(s, numOfShorts);
        }
    }


    public List<Short> shortSite(int x, int y)
    {
        return shortTile(x, y, 1);
    }

    public List<Short> shortSite(int x, int y, int numOfShorts)
    {
        List<Short> shorts = placeShortedSite(x, y);
        for (Short s : shorts) {
            routeShort(s, numOfShorts);
        }

        return shorts;
    }

    public List<Short> shortTile(int x, int y)
    {
        return shortTile(x, y, 1);
    }

    public List<Short> shortTile(int x, int y, int numOfShorts)
    {
        List<Short> shorts = placeShortedTile(x, y);
        for (Short s : shorts) {
            routeShort(s, numOfShorts);
        }

        return shorts;
    }

    /**
     * Creates a region of shorts in the region bounded by the parameters. This function has been optimized for the ARTY
     * A7-35t to produce the most efficiently damaging shorts and may not work as well or at all for other FPGAs.
     * @param xMin minimum x bound for short region
     * @param xMax maximum x bound for short region
     * @param yMin minimum y bound for short region
     * @param yMax maximum y bound for short region
     */
    public List<Short> createShortedRegion(int xMin, int xMax, int yMin, int yMax)
    {
        return createShortedRegion(xMin, xMax, yMin, yMax, 1);
    }

    /**
     * Creates a region of shorts in the region bounded by the parameters. This function has been optimized for the ARTY
     * A7-35t to produce the most efficiently damaging shorts and may not work as well or at all for other FPGAs.
     * @param xMin minimum x bound for short region
     * @param xMax maximum x bound for short region
     * @param yMin minimum y bound for short region
     * @param yMax maximum y bound for short region
     */
    public List<Short> createShortedRegion(int xMin, int xMax, int yMin, int yMax, int numOfShorts)
    {

        int lutCount = countLut6(d);
        List<Short> shorts = new ArrayList<>();

        outerLoop:
        for(int y = yMin; y <= yMax; y++)
        {
            for (int x = xMin; x <= xMax; x++)
            {
                shorts.addAll(shortSite(x, y, numOfShorts));
                lutCount+=4;
                if (lutCount > (maxLuts-4))
                    return shorts;

            }
        }

        return shorts;
    }

    public List<Short> getShorts()
    {
        return shorts;
    }
}
