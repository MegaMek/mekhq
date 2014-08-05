package mekhq.gui.sorter;

import java.util.Comparator;

/**
     * A comparator for skills levels (e.g. Regular, Veteran, etc)
     *   * @author Jay Lawson
     *
     */
    public class LevelSorter implements Comparator<String> {

        @Override
        public int compare(String s0, String s1) {
            if(s0.equals("-") && s1.equals("-")) {
                return 0;
            } else if(s0.equals("-")) {
                return -1;
            } else if(s1.equals("-")) {
                return 1;
            } else {
                //probably easiest to turn into numbers and then sort that way
                int l0 = 0;
                int l1 = 0;
                if(s0.contains("Green")) {
                    l0 = 2;
                }
                if(s1.contains("Green")) {
                    l1 = 2;
                }
                // Ultra-Green has to be below Green when using String.contains() because it contains Green
                if(s0.contains("Ultra-Green")) {
                    l0 = 1;
                }
                if(s1.contains("Ultra-Green")) {
                    l1 = 1;
                }
                if(s0.contains("Regular")) {
                    l0 = 3;
                }
                if(s1.contains("Regular")) {
                    l1 = 3;
                }
                if(s0.contains("Veteran")) {
                    l0 = 4;
                }
                if(s1.contains("Veteran")) {
                    l1 = 4;
                }
                if(s0.contains("Elite")) {
                    l0 = 5;
                }
                if(s1.contains("Elite")) {
                    l1 = 5;
                }
                return ((Comparable<Integer>)l0).compareTo(l1);
            }
        }
    }