package mekhq.gui.sorter;

import java.util.Comparator;

/**
     * A comparator for target numbers written as strings
     * @author Jay Lawson
     *
     */
    public class TargetSorter implements Comparator<String> {

        @Override
        public int compare(String s0, String s1) {
            s0 = s0.replaceAll("\\+", "");
            s1 = s1.replaceAll("\\+", "");
            int r0 = 0;
            int r1 = 0;
            if(s0.equals("Impossible")) {
                r0 = Integer.MAX_VALUE;
            }
            else if(s0.equals("Automatic Failure")) {
                r0 = Integer.MAX_VALUE-1;
            }
            else if(s0.equals("Automatic Success")) {
                r0 = Integer.MIN_VALUE;
            } else {
                r0 = Integer.parseInt(s0);
            }
            if(s1.equals("Impossible")) {
                r1 = Integer.MAX_VALUE;
            }
            else if(s1.equals("Automatic Failure")) {
                r1 = Integer.MAX_VALUE-1;
            }
            else if(s1.equals("Automatic Success")) {
                r1 = Integer.MIN_VALUE;
            } else {
                r1 = Integer.parseInt(s1);
            }
            return ((Comparable<Integer>)r0).compareTo(r1);

        }
    }
