package mekhq.gui.sorter;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Comparator;

import megamek.logging.MMLogger;

/**
 * A comparator for numbers that have been formatted with DecimalFormat
 * 
 * @author Jay Lawson
 */
public class FormattedNumberSorter implements Comparator<String> {
    private static final MMLogger logger = MMLogger.create(FormattedNumberSorter.class);

    private static final String PLUS_SIGN = "+";
    private static final DecimalFormat FORMAT = new DecimalFormat();

    @Override
    public int compare(String s0, String s1) {
        // Cut off leading "+" sign if there
        if (s0.startsWith(PLUS_SIGN)) {
            s0 = s0.substring(1);
        }

        if (s1.startsWith(PLUS_SIGN)) {
            s1 = s1.substring(1);
        }
        // Empty cells are smaller than all numbers
        if (s0.isBlank() && s1.isBlank()) {
            return 0;
        } else if (s0.isBlank()) {
            return -1;
        } else if (s1.isBlank()) {
            return 1;
        }
        // lets find the weight class integer for each name
        long l0 = 0;
        try {
            l0 = FORMAT.parse(s0).longValue();
        } catch (ParseException e) {
            logger.error("", e);
        }
        long l1 = 0;
        try {
            l1 = FORMAT.parse(s1).longValue();
        } catch (ParseException e) {
            logger.error("", e);
        }
        return Long.compare(l0, l1);
    }
}
