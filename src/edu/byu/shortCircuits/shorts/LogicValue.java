package edu.byu.shortCircuits.shorts;

/*
 * Indicates the a logic level of either high or low.
 */
public enum LogicValue
{
    LOW("0"),
    HIGH("1");
//    INV("!I0"),
//    BUF("I0");

    private final String config;

    LogicValue(String config)
    {
        this.config = config;
    }

    /**
     * Returns the string equivalent of the logicLevel
     * @return the string equivalent of the logicLevel
     */
    @Override
    public String toString() { return config; }

    /**
     * Returns the integer equivalent of the logicLevel
     * @return the integer equivalent of the logicLevel
     */
    public int toInt()
    {
        return Integer.valueOf(config);
    }
}