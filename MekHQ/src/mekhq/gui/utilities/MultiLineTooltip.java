package mekhq.gui.utilities;

import java.util.ArrayList;
import java.util.List;

/**
 * This class splits a tooltip into multiple lines in order to wrap it.
 *  It will enclose a string into HTML format
 *
 * @author Paul Taylor (adapted by Miguel Azevedo)
 * @source https://stackoverflow.com/questions/868651/multi-line-tooltips-in-java
 */
public class MultiLineTooltip
{
    private static int DIALOG_TOOLTIP_MAX_SIZE = 85;
    private static final int SPACE_BUFFER = 10;

    /**
     * Wraps a string with html tags and places breaks where it can split it, defaulting the size of a line to
     * 85 characters.
     * @param tip String to split
     * @return Split string in html format
     */
    public static String splitToolTip(String tip)
    {
        return splitToolTip(tip,DIALOG_TOOLTIP_MAX_SIZE);
    }

    /**
     * Wraps a string with html tags and places breaks where it can split it.
     * @param tip String to split
     * @param length Maximum characters the each line can have
     * @return Split string in html format
     */
    public static String splitToolTip(String tip, int length)
    {
        if(tip.length()<=length + SPACE_BUFFER )
        {
            return tip;
        }

        List<String> parts = new ArrayList<>();

        int maxLength = 0;
        String overLong = tip.substring(0, length + SPACE_BUFFER);
        int lastSpace = overLong.lastIndexOf(' ');
        if(lastSpace >= length)
        {
            parts.add(tip.substring(0,lastSpace));
            maxLength = lastSpace;
        }
        else
        {
            parts.add(tip.substring(0,length));
            maxLength = length;
        }

        while(maxLength < tip.length())
        {
            if(maxLength + length < tip.length())
            {
                parts.add(tip.substring(maxLength, maxLength + length));
                maxLength+=maxLength+length;
            }
            else
            {
                parts.add(tip.substring(maxLength));
                break;
            }
        }

        StringBuilder  sb = new StringBuilder("<html>");
        for(int i=0;i<parts.size() - 1;i++)
        {
            sb.append(parts.get(i)+"<br>");
        }
        sb.append(parts.get(parts.size() - 1));
        sb.append(("</html>"));
        return sb.toString();
    }
}
