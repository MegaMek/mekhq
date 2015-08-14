package mekhq.gui.sorter;

import java.text.DecimalFormat;
import java.util.Comparator;



    /**
     * A comparator for numbers that have been formatted with DecimalFormat
     * @author Jay Lawson
     *
     */
    public class FormattedNumberSorter implements Comparator<String> {

        @Override
        public int compare(String s0, String s1) {
            //lets find the weight class integer for each name
            DecimalFormat format = new DecimalFormat();
            long l0 = 0;
            try {
                l0 = format.parse(s0).longValue();
            } catch (java.text.ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            long l1 = 0;
            try {
                l1 = format.parse(s1).longValue();
            } catch (java.text.ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return ((Comparable<Long>)l0).compareTo(l1);
        }
    }
