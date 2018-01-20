package mekhq.gui.sorter;

import java.util.Comparator;

/**
     * A comparator for bonuses written as strings with "-" sorted to the bottom always
     * @author Jay Lawson
     *
     */
public class BonusSorter implements Comparator<String> {

    @Override
    public int compare(String s0, String s1) {
        int i0 = 0, i1 = 0;
        if (s0.contains("/")) {
            String[] temp = s0.split("/");
            if(temp[0].contains("-") && temp[1].contains("-")) {
                i0 = 99;
            } else {
                int t0 = temp[0].contains("-") ? 0 : Integer.parseInt(temp[0]);
                int t1 = temp[1].contains("-") ? 0 : Integer.parseInt(temp[1]);
                i0 = t0 + t1;
            }
        } else {
            i0 = s0.equals("-") ? 90 : Integer.parseInt(s0);
        }
        if (s1.contains("/")) {
            String[] temp = s1.split("/");
            if(temp[0].contains("-") && temp[1].contains("-")) {
                i1 = 99;
            } else {
                int t0 = temp[0].contains("-") ? 0 : Integer.parseInt(temp[0]);
                int t1 = temp[1].contains("-") ? 0 : Integer.parseInt(temp[1]);
                i1 = t0 + t1;
            }
        } else {
            i1 = s1.equals("-") ? 90 : Integer.parseInt(s1);
        }

        return ((Comparable<Integer>)i1).compareTo(i0);
    }
}
