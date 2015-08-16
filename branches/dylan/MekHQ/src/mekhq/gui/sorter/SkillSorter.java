package mekhq.gui.sorter;

import java.util.Comparator;

/**
 * A comparator for skills written as strings with "-" sorted to the bottom always
 * @author Jay Lawson
 *
 */
public class SkillSorter implements Comparator<String> {

    @Override
    public int compare(String s0, String s1) {
        if(s0.equals("-") && s1.equals("-")) {
            return 0;
        } else if(s0.equals("-")) {
            return 1;
        } else if(s1.equals("-")) {
            return -1;
        } else {
            return ((Comparable<String>)s0).compareTo(s1);
        }
    }
}
