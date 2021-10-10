package edu.byu.shorty.shorts;

/**
 * An enumeration that contains each type of shortable BEL.
 * The LUT is for LUT6 or LUT_OR_MEM6 BELs, while the REG_INIT and FF_INIT are the two types of FF within a logic site
 */
public enum ShortBELType
{
    LUT("LUT"), // correlate to the site pins that don't contain a postfix
    REG_INIT("REG_INIT"), //correlate to the site pins that contain the 'Q' prefix
    FF_INIT("FF_INIT"); //can correlate to the site pins that contain the 'MUX' prefix

    private final String type;

    ShortBELType(String type)
    {
        this.type = type;
    }

    /**
     * Returns the string equivalent of the enumeration
     * @return the string equivalent of the enumeration
     */
    @Override
    public String toString()
    {
        return this.type;
    }

    /**
     * Returns the postfix of the BEL type (the prefix being the BELID). For example, for a LUT6 named D6LUT, the
     * postfix would be 6LUT, and for a REG_INIT BFF the postfix would be BFF. This allows one to recreate the name of
     * any shortable BEL using both the BELID and ShortBELType enumerations
     * @return the postfix of the BEL type
     */
    public String getBELPostfix()
    {
        return  type.equals("LUT") ? "6LUT" :
                type.equals("REG_INIT") ? "FF" :
                "5FF";
    }

    /**
     * This functions is meant to replace the enumeration's ofValue() method. This takes a String returns the
     * enumeration.
     * @param BELType
     * @return the enumeration equivalent of the string. If the string does not contain LUT and is not REG_INIT or
     * FF_INIT it will return an invalid enumeration
     */
    public static ShortBELType BELTypetoShortBELType(String BELType)
    {
        //if the BELType string contains anything with 'LUT' in the name, the function returns the LUT type
        if(BELType.contains("LUT"))
        {
            return LUT;
        }
        return valueOf(BELType);
    }
}
