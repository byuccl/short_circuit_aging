package edu.byu.shorty.rapidwrighttools;

import com.xilinx.rapidwright.design.*;
import com.xilinx.rapidwright.design.tools.LUTTools;
import com.xilinx.rapidwright.device.*;
import com.xilinx.rapidwright.edif.EDIFNet;
import com.xilinx.rapidwright.edif.EDIFTools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.stream.Stream;

import static edu.byu.shorty.tcl.ExecuteTclScript.executeScript;

/**
 * Convenient methods for RapidWright that may be helpful when creating designs that have RO or shorts
 */
public class RapidWrightTools
{
    /**
     *
     * @param d The design that the returned EDIFNet will belong to
     * @return the EDIFNet of the universal GND wire
     */
    static public EDIFNet getGndNet(Design d){
        return EDIFTools.getStaticNet(NetType.GND, d.getNetlist().getTopCell(), d.getNetlist());
    }

    /**
     *
     * @param d The design that the returned EDIFNet will belong to
     * @return the EDIFNet of the universal VCC wire
     */
    static public EDIFNet getVccNet(Design d){
        return EDIFTools.getStaticNet(NetType.VCC, d.getNetlist().getTopCell(), d.getNetlist());
    }

    /**
     * A function that places a LUT1 cell in the desired location. This function was created for two reason. One was to
     * Overcome a bug that was caused by a discrepancy between pin names between the EDIFCell and Cell objects of a
     * LUT1 Cell. The second was to allow a convenient way to add an initialization to the LUT that skips the
     * computation heavy LUTTools.configureLUT(cell, init); function and thus dramatically increases performance (as
     * much as 8x).
     * @param d The design that will contain the LUT1
     * @param lutLocation location of the lut in the following format: site_name/lut_name
     *                    ex: "SLICE_X30Y100/B6LUT"
     * @param name The name of the LUT1. Is arbitrary.
     * @param init The String of the init function. Look at the if else statement in the function for examples.
     * @return A cell of the created lut.
     */
    public static Cell createAndPlaceLUT1(Design d, String name, String lutLocation, String init)
    {
        // makes sure that the cell is actually a lut
        if(!lutLocation.contains("LUT"))
        {
            throw new RuntimeException("ERROR: " + lutLocation + " is not a valid lut!");
        }

        Cell cell = d.createAndPlaceCell(name, Unisim.LUT1, lutLocation);

        if(init.equals("O=1")) // const 1 configuration
        {
            cell.addProperty("INIT", "2'h3");
        }
        else if(init.equals("O=0")) // const 0 configuration
        {
            cell.addProperty("INIT", "2'h0");
        }
        else if(init.equals("O=!I0")) // inverter configuration
        {
            cell.addProperty("INIT", "2'h1");
        }
        else if(init.equals("O=I0")) // buffer configuration
        {
            cell.addProperty("INIT", "2'h2");
        }
        else //only calculates init equation, if none of the above four configurations are detected
        {
            LUTTools.configureLUT(cell, init);
        }

        //this part is what overcomes the bug with the physical/logical pin discrepancy.
        cell.removePinMapping("A6");
        cell.addPinMapping("A6", "I0");
//        System.out.println(cell);

//        System.out.println(d.getCells());
        return cell;
    }

    /**
     * This function is much like createAndPlaceLUT1 but the init string for an inverter is already provided
     * @param d The design that will contain the LUT1
     * @param lutLocation location of the lut in the following format: site_name/lut_name
     *      *              ex: "SLICE_X30Y100/B6LUT"
     * @param name The name of the LUT1. Is arbitrary.
     * @return A cell of the created inverter.
     */
    public static Cell createAndPlaceInverter(Design d, String name, String lutLocation)
    {
        return createAndPlaceLUT1(d, name, lutLocation,"O=!I0");
    }

    /**
     * This function returns a list of logic sites (SLICEL and/or SLICEM) within the specified range
     * @param device The device to be searched for sites
     * @param xMin the minimum x-coordinate for the sites
     * @param xMax the maximum x-coordinate for the sites
     * @param yMin the minimum y-coordinate for the sites
     * @param yMax the maximum y-coordinate for the sites
     * @return a list of logic sites (SLICEL and/or SLICEM) within the specified range
     */
    public static ArrayList<Site> getLogicSitesInRange(Device device, int xMin, int xMax, int yMin, int yMax)
    {
        ArrayList<Site> sites = new ArrayList<>();

        // Concats two arrays by using the Stream class to create a single Stream that contains all SLICE_L and SLICE_M
        // sites and then iterates through the string using the forEachOrdered method
        Stream.concat(Arrays.stream(device.getAllSitesOfType(SiteTypeEnum.SLICEL)),
                Arrays.stream(device.getAllSitesOfType(SiteTypeEnum.SLICEM))).forEachOrdered(site -> {
            //checks to see if the site is within the specified range
            if(site.getInstanceX() >= xMin && site.getInstanceX() <= xMax
                    && site.getInstanceY() >= yMin && site.getInstanceY() <= yMax )
            {
                sites.add(site);
            }
        });
        return sites;
    }

    /**
     * gets the tile that is the upper left corner of the clock region
     * @param cr clock region
     * @return upper left tile
     */
    public static Tile getLogicalUpperLeft(ClockRegion cr)
    {
        Tile upperLeft = cr.getUpperLeft();
        Tile lowerRight = cr.getLowerRight();

        for(int y = upperLeft.getRow(); y <= lowerRight.getRow(); y++)
        {
            for(int x = upperLeft.getColumn(); x <= lowerRight.getColumn(); x++)
            {
                Tile tempTile = cr.getDevice().getTile(y, x);
                if(tempTile.getTileTypeEnum().name().contains("CLBL")
                        || tempTile.getTileTypeEnum().name().contains("CLE"))
                {
                    return tempTile;
                }
            }
        }

        return null;
    }

    /**
     * gets the tile that is the lower right corner of the clock region
     * @param cr clock region
     * @return lower right tile
     */
    public static Tile getLogicalLowerRight(ClockRegion cr)
    {
        Tile upperLeft = cr.getUpperLeft();
        Tile lowerRight = cr.getLowerRight();

        for(int y = lowerRight.getRow(); y >= upperLeft.getRow(); y--)
        {
            for(int x = lowerRight.getColumn(); x >= upperLeft.getColumn(); x--)
            {
                Tile tempTile = cr.getDevice().getTile(y, x);
                if(tempTile.getTileTypeEnum().name().contains("CLBL")
                    || tempTile.getTileTypeEnum().name().contains("CLE"))
                {
                    return tempTile;
                }
            }
        }

        return null;
    }

    /**
     * returns the logical center of a the box created by the coordinates. Largely unused
     * @param dev device to get center from
     * @param xMin min x coordinate
     * @param xMax max x coordinate
     * @param yMin min y coordinate
     * @param yMax max y coordinate
     * @return Logic tile that is the logical center
     */
    public static Tile getLogicalCenter(Device dev, int xMin, int xMax, int yMin, int yMax)
    {

        int xCenter = (xMax + xMin)/2;
        int yCenter = (yMax + yMin)/2;

        for(int x = 0; x < (xMax - xMin)/2; x++)
        {
            for (int y = 0; y < (yMax - yMin)/2; y++)
            {
                Tile centerLogicTile = dev.getTile(yCenter - y, xCenter + x);
                if (centerLogicTile.getTileTypeEnum().name().contains("CLBL")
                        || centerLogicTile.getTileTypeEnum().name().contains("CLE"))
                {
                    return centerLogicTile;
                }

                centerLogicTile = dev.getTile(yCenter + y, xCenter + x);
                if (centerLogicTile.getTileTypeEnum().name().contains("CLBL")
                        || centerLogicTile.getTileTypeEnum().name().contains("CLE"))
                {
                    return centerLogicTile;
                }
            }

            for (int y = 0; y < (yMax - yMin) / 2; y++)
            {
                Tile centerLogicTile = dev.getTile(yCenter - y, xCenter - x);
                if (centerLogicTile.getTileTypeEnum().name().contains("CLBL")
                        || centerLogicTile.getTileTypeEnum().name().contains("CLE"))
                {
                    return centerLogicTile;
                }

                centerLogicTile = dev.getTile(yCenter + y, xCenter - x);
                if (centerLogicTile.getTileTypeEnum().name().contains("CLBL")
                        || centerLogicTile.getTileTypeEnum().name().contains("CLE"))
                {
                    return centerLogicTile;
                }
            }
        }

        return null;
    }

    /**
     * This method returns the approximate centered logic tile in the clock region. This method ignores the tiles on the
     * edges of the clock region when picking the center logic tile.
     * @param cr The clock region that the desired center is in.
     * @return The logical tile that is in the center of all other logical tiles in the clock region
     */
    public static Tile getLogicalCenter(ClockRegion cr)
    {
        Device dev = cr.getDevice();
        int xMin = getLogicalUpperLeft(cr).getColumn();
        int xMax = getLogicalLowerRight(cr).getColumn();
        int yMin = getLogicalUpperLeft(cr).getRow();
        int yMax = getLogicalLowerRight(cr).getRow();

        int xCenter = (xMax + xMin)/2;
        int yCenter = (yMax + yMin)/2;

        for(int x = 0; x < (xMax - xMin)/2; x++)
        {
            for (int y = 0; y < (yMax - yMin)/2; y++)
            {
                Tile centerLogicTile = dev.getTile(yCenter - y, xCenter + x);
                if (centerLogicTile.getTileTypeEnum().name().contains("CLBL"))
                {
                    return centerLogicTile;
                }

                centerLogicTile = dev.getTile(yCenter + y, xCenter + x);
                if (centerLogicTile.getTileTypeEnum().name().contains("CLBL"))
                {
                    return centerLogicTile;
                }
            }

            for (int y = 0; y < (yMax - yMin) / 2; y++)
            {
                Tile centerLogicTile = dev.getTile(yCenter - y, xCenter - x);
                if (centerLogicTile.getTileTypeEnum().name().contains("CLBL"))
                {
                    return centerLogicTile;
                }

                centerLogicTile = dev.getTile(yCenter + y, xCenter - x);
                if (centerLogicTile.getTileTypeEnum().name().contains("CLBL"))
                {
                    return centerLogicTile;
                }
            }
        }
        return null;
    }

    /**
     * gets a site from the tile based on the index of the site, with the index 0 being the site with a smaller x
     * coordinate.
     * @param tile tile to get site from
     * @param index index of site
     * @return the site
     */
    public static Site getSiteFromTile(Tile tile, int index)
    {
        Site[] sites = tile.getSites();
        if(index >= sites.length)
        {
            throw new RuntimeException("ERROR: The provided site index (" + index + ") doesn't correspond to any site" +
                    "within the tile " + tile.getName());
        }

        for (Site site : sites)
        {
            int siteIndex = site.getInstanceX() % sites.length;
            if(siteIndex == index)
            {
                return site;
            }
        }

        return null;
    }


    /**
     * Counts the number of 6LUTs in a design. Useful to prevent the number of LUTs go above the artificial limit of
     * 20,800 that vivado imposes for the ARTY A7-35t
     * @param d design to count the 6LUTs from
     * @return the number of LUTs currently used in the design
     */
    public static int countLut6(Design d)
    {
        int currentLuts = 0;
        for(Cell cell : d.getCells())
        {
            if (cell.getName().equals("<LOCKED>"))
                continue;

//            System.out.println(cell.getName());

            if (cell.getBEL().getBELType().equals("LUT6") || (cell.getBEL().getBELType().equals("LUT_OR_MEM6")))
            {
                currentLuts++;
            }
        }

        return currentLuts;
    }

    /**
     * Runs a tcl script that runs route_design on a checkpoint
     * @param fileName name of the dcp file
     * @param expName experiment name of the dcp file (used to locate the dcp file)
     */
    public static void routeCheckpoint(String fileName, String expName)
    {
        executeScript("route_checkpoint.tcl", fileName, expName);
    }

    /**
     * Checks for any non-logical (null) sites in the rectangular region made by the x and y constraints.
     * @param dev The device to check
     * @param xMin minimum x coordinate of the region
     * @param xMax maximum x coordinate of the region
     * @param yMin minimum y coordinate of the region
     * @param yMax maximum y coordinate of the region
     * @return false if no Null sites found, otherwise true.
     */
    public static boolean containsNullSites(Device dev, int xMin, int xMax, int yMin, int yMax)
    {
        for(int y = yMin; y <= yMax; y++)
        {
            for(int x = xMin; x <= xMax; x++)
            {
                String siteString = String.format("SLICE_X%dY%d", x, y);
                Site site = dev.getSite(siteString);

                if(site == null)
                {
                    return true;
                }
            }
        }

        return false;
    }

}
