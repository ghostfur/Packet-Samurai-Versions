package org.openaion.packetsamurai.filter.assertionoperator;


import org.openaion.packetsamurai.filter.value.string.StringValue;
import org.openaion.packetsamurai.parser.DataStructure;

/**
 * 
 * @author Gilles Duboscq
 *
 */
public class StringDifferantOperator implements StringAssertionOperator
{

    public boolean execute(StringValue value1, StringValue value2, DataStructure dp)
    {
        if(value1.getStringValue(dp) == null)
            return (value2.getStringValue(dp) != null);
        return !value1.getStringValue(dp).equals(value2.getStringValue(dp));
    }
}