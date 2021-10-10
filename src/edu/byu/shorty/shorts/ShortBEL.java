package edu.byu.shorty.shorts;

import com.xilinx.rapidwright.design.*;
import com.xilinx.rapidwright.device.*;
import edu.byu.shorty.rapidwrighttools.RapidWrightTools;
import edu.byu.shorty.rapidwrighttools.BELID;
import edu.byu.shorty.rapidwrighttools.BELConfig;

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
public class ShortBEL {

  private Cell cell; // Cell that contains the shortable BEL
  private BEL bel;   // The shortable BEL
  private Site site; // The site that contains the shortable BEL
  private ShortBELType
      type;         // The type of shortable BEL (LUT, REG_INIT, or FF_INIT)
  private BELID ID; // The ID of the shorable BEL (A, B, C, or D)
  private BELConfig
          belConfig;  // The logic  the shortable BEL (HIGH or LOW)
  private Wire tileWire; // The tile wire that the BEL will be connected to.
                         // This is the starting wire that will be used to find
                         // all of the PIPs that need to be turned on in order
                         // to short this BEL to Another BEL
  private Design design; // The design that this shortable BEL is in.
  private String
      belOutputPin; // The Name of the output Pin of this BEL. This is used in
                    // order to hook up nets to two shortable PIPs

  /**
   * Creates a ShortBel object.
   * @param d The design that this shortable BEL is in
   * @param site The site that contains the shortable BEL
   * @param type The type of shortable BEL
   * @param ID The ID of the shorable BEL
   * @param belConfig the logic level of the shortable BEL
   */
  public ShortBEL(Design d, Site site, ShortBELType type, BELID ID,
                  BELConfig belConfig) {
    this(d, site, site.getBEL(ID.toString() + type.getBELPostfix()), belConfig); //calls second constructor
  }

  /**
   * Creates a ShortBel object.
   * @param d The design that this shortable BEL is in
   * @param site The site that contains the shortable BEL
   * @param bel The shortable BEL
   * @param belConfig the logic level of the shortable BEL
   */
  public ShortBEL(Design d, Site site, BEL bel, BELConfig belConfig) {
    // makes sure that the site is a logic site (only sites that are currently
    // supported for shorts)
    checkIfLogicSite(site);

    this.site = site;
    this.ID = BELID.valueOf(bel.getName().substring(0, 1));
    this.type = ShortBELType.BELTypetoShortBELType(bel.getBELType());
    this.belConfig = belConfig;
    this.design = d;
    this.bel = bel;

    // finds tile wire and stores it as a member variable
    findTileWire();
  }



  public ShortBEL(Design d, Site site, BEL bel, BELConfig belConfig, Net belInputNet) {
    // makes sure that the site is a logic site (only sites that are currently
    // supported for shorts)
    checkIfLogicSite(site);

    this.site = site;
    this.ID = BELID.valueOf(bel.getName().substring(0, 1));
    this.type = ShortBELType.BELTypetoShortBELType(bel.getBELType());
    this.belConfig = belConfig;
    this.design = d;
    this.bel = bel;

    // finds tile wire and stores it as a member variable
    findTileWire();
  }



  /**
   * Creates a ShortBel object.
   * @param d The design that this shortable BEL is in
   * @param site The site that contains the shortable BEL
   * @param bel The shortable BEL
   */
  public ShortBEL(Design d, Site site, BEL bel) {
    // makes sure that the site is a logic site (only sites that are currently
    // supported for shorts)
    checkIfLogicSite(site);

    this.site = site;
    this.ID = BELID.valueOf(bel.getName().substring(0, 1));
    this.type = ShortBELType.BELTypetoShortBELType(bel.getBELType());
    //this.belConfig = belConfig;
    this.design = d;
    this.bel = bel;

    // finds tile wire and stores it as a member variable
    findTileWire();
  }



  /**
   * This function creates a cell that contains the shortable bel and places it
   * within the design. This method relies on data members and must be called
   * manually if one would like to use the BEL to make a short (as opposed to
   * using the BEL just to find a short).
   */
  public void createAndPlaceCell() {
    String belName =
            site.getName() + "/" + ID.toString() + type.getBELPostfix();
    String cellName =
            site.getName() + "_" + ID.toString() + type.getBELPostfix();
    if (type == ShortBELType.LUT) // places a lut1 if the ShortBELType has been set to LUT
    {
      cell = RapidWrightTools.createAndPlaceLUT1(
              design, cellName + "_inst", belName,
              "O=" + belConfig.toString());

      this.belOutputPin = "O";
    }
    else // places a FDSE if the ShortBELType has been set to FF_INIT or FF_REG
    {
      cell = design.createAndPlaceCell(cellName + "_inst", Unisim.FDSE, belName);
      int ffConfig = belConfig.toInt();
//      if (ffConfig != BELConfig.INV.toInt() || ffConfig != BELConfig.BUF.toInt())
//      {
//        String errorConfig = ffConfig == BELConfig.INV.toInt() ? "INV" : "BUF";
//        throw new RuntimeException("ERROR! BEL Configuration, " + errorConfig + ", is not compitable with FFs!");
//      }
      cell.getEDIFCellInst().addProperty("INIT", ffConfig);

      this.belOutputPin = "Q";
    }
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

    // These if-else statements correlate the LUT to a string that vivado uses
    // to identify the site pin they are connected to
    if (type == ShortBELType.LUT) {
      sitePinName = ID.toString();
    } else if (type == ShortBELType.REG_INIT) {
      sitePinName = ID.toString() + "Q";
    } else {
      sitePinName = ID.toString() + "MUX";
    }

    // gets the tile wire from the constructed sitePin string up above
    SitePin sitePin = new SitePin(site, sitePinName);
    this.tileWire =
        new Wire(site.getTile(), site.getTile().getWireFromSitePin(sitePin));
  }

  /**
   * A convenience function that unplaces the Cell.
   */
  public void unplace() { this.cell.unplace(); }

  public BELConfig getConfig() { return belConfig; }

  public Cell getCell() { return cell; }

  public BEL getBel() { return bel; }

  public Site getSite() { return site; }

  public ShortBELType getType() { return type; }

  public BELID getID() { return ID; }

  public Wire getTileWire() { return tileWire; }

  public Design getDesign() { return design; }

  public String getBelOutputPin() { return belOutputPin; }
}
