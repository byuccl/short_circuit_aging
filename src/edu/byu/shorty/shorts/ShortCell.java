package edu.byu.shorty.shorts;

import com.xilinx.rapidwright.design.*;
import com.xilinx.rapidwright.device.*;
import edu.byu.shorty.rapidWrightTools.BELID;

/**
 * This class contains everything needed to short two BELs together. It is made
 * up of a number of RapidWright Objects. This class is heavily used by methods
 * in ShortTools. It does not have to ever be used by users (as each method in
 * ShortTools has versions that take BEL objects or Strings instead of ShortBEL
 * objects), but it can be helpful for those who are unsure on how to pass in a
 * BEL that is shortable. One of the constructors for this class contains
 * helpful enumerations so that it is almost impossible to provide a BEL that
 * isn't shortable.
 */
public class ShortCell {

  private Cell cell; // Cell that contains the shortable BEL
  private LogicValue outputValue; // The logic  the shortable BEL (HIGH or LOW)
  private Wire tileWire; // The tile wire that the BEL will be connected to.
                         // This is the starting wire that will be used to find
                         // all of the PIPs that need to be turned on in order
                         // to short this BEL to Another BEL
  private String belOutputPin; // The Name of the output Pin of this BEL. This is used in
                    // order to hook up nets to two shortable PIPs


  /**
   * Creates a ShortBel object.
   * @param d The design that this shortable BEL is in
   * @param site The site that contains the shortable BEL
   * @param bel The shortable BEL
   */
  public ShortCell(Design d, Site site, BEL bel, LogicValue logicValue) {
    // makes sure that the site is a logic site (only sites that are currently
    // supported for shorts)
    checkIfLogicSite(site);

    this.outputValue = logicValue;
    createAndPlaceCell(d, site, bel);

    // finds tile wire and stores it as a member variable
    findTileWire();
  }

  /**
   * This function creates a cell that contains the shortable bel and places it
   * within the design. This method relies on data members and must be called
   * manually if one would like to use the BEL to make a short (as opposed to
   * using the BEL just to find a short).
   */
  private void createAndPlaceCell(Design d, Site site, BEL bel) {

    String loc = site.toString() + "/" + bel.getName();
    String cellName =
            "shortCell_" + site.getName() + "_" + bel.getName();
    if (bel.getBELType().contains("LUT")) // places a lut1 if the ShortBELType has been set to LUT
    {
      cell = d.createAndPlaceCell( cellName + "_inst", Unisim.LUT6, loc);
      String initString = outputValue == LogicValue.LOW ? "64'h0" : "64'hffffffffffffffff";
      cell.addProperty("INIT", initString);

      this.belOutputPin = "O";
      fixPins();
    }
    else // places a FDSE if the ShortBELType has been set to FF_INIT or FF_REG
    {
      cell = d.createAndPlaceCell(cellName + "_inst", Unisim.FDSE, loc);
      int ffConfig = outputValue.toInt();
//      if (ffConfig != BELConfig.INV.toInt() || ffConfig != BELConfig.BUF.toInt())
//      {
//        String errorConfig = ffConfig == BELConfig.INV.toInt() ? "INV" : "BUF";
//        throw new RuntimeException("ERROR! BEL Configuration, " + errorConfig + ", is not compitable with FFs!");
//      }
      cell.getEDIFCellInst().addProperty("INIT", ffConfig);

      this.belOutputPin = "Q";
    }
  }

  private void fixPins() {
    cell.setBELFixed(true);
    for (String pin : cell.getBEL().getPinMap().keySet())
      cell.fixPin(pin);
  }

  /**
   * makes sure that the site is a logic site (only sites that are currently
   * supported for shorts)
   * @param site site that is to be checked
   */
  private void checkIfLogicSite(Site site) {
    // checks if it's a slicel or slicem site (the two kind of logic sites in
    // many devices).
    // If not it throws an exception
    if (!(site.getSiteTypeEnum() == SiteTypeEnum.SLICEL ||
          site.getSiteTypeEnum() == SiteTypeEnum.SLICEM)) {
      throw new RuntimeException("ERROR: Site " + site.getName() +
                                 " must either be of site type SLICEL or"
                                 + " SLICEM!");
    }
  }

  /**
   * Finds the tile wire and stores it as a member variable
   */
  private void findTileWire() {
    String sitePinName;
    String type = getType();
    String id = getID();
    // These if-else statements correlate the LUT to a string that vivado uses
    // to identify the site pin they are connected to
    if (type.contains("LUT")) {
      sitePinName = id;
    } else if (type.equals("REG_INIT")) {
      sitePinName = id + "Q";
    } else if (type.equals("FF_INIT")) {
      sitePinName = id + "MUX";
    }
    else {
      throw new RuntimeException("Error! Could not find tile wire for shortCell " + cell.toString());
    }

    Site site = getSite();
    // gets the tile wire from the constructed sitePin string up above
    SitePin sitePin = new SitePin(site, sitePinName);
    this.tileWire =
        new Wire(site.getTile(), site.getTile().getWireFromSitePin(sitePin));
  }

  /**
   * A convenience function that unplaces the Cell.
   */
  public void unplace() { this.cell.unplace(); }

  public LogicValue getOutputValue() { return outputValue; }

  public Cell getCell() { return cell; }

  public void setCell(Cell cell) {this.cell = cell;}

  public String getCellName() { return cell.getName(); }

  public BEL getBel() { return cell.getBEL(); }

  public Site getSite() { return cell.getSite(); }

  public String getType() { return getBel().getBELType(); }

  public String getID() { return getBel().toString().substring(0, 1); }

  public Wire getTileWire() { return tileWire; }

  public String getBelOutputPin() { return belOutputPin; }
}
