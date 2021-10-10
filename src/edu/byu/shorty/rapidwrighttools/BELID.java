package edu.byu.shorty.rapidwrighttools;

/**
 * A enumeration for all of the prefixes a shortable BEL can have. These refer to the first Letter of the BEL name,
 * i.e. The C in C6LUT
 */
public enum BELID
{
    A("A"),
    B("B"),
    C("C"),
    D("D");

    private final String ID;
    private final int ASCII_A = 65;

    BELID(String ID)
    {
        this.ID = ID;
    }

    /**
     * Returns the string equivalent of the enumeration
     * @return the string equivalent of the enumeration
     */
    @Override
    public String toString()
    {
        return ID;
    }

    /**
     * Returns the integer equivalent of the enumeration, starting at 0 with A and incrementing by one. This is helpful
     * when mapping BELs with the node. For example, a BEL with ID A can connect to NW2BEG0, while a BEL with ID C can
     * connect to NW2BEG2.
     * @return the int equivalent of the enumeration
     */
    public int toInt() {return ((char) ID.charAt(0)) - ASCII_A;}

}
