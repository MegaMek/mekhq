package mekhq.gui.sorter;

import java.util.Comparator;

/**
     * A comparator for unit status strings
     * @author Jay Lawson
     *
     */
    public class UnitStatusSorter implements Comparator<String> {

        @Override
        public int compare(String s0, String s1) {
            //probably easiest to turn into numbers and then sort that way
            int l0 = 0;
            int l1 = 0;
            if(s0.contains("Salvage")) {
                l0 = 1;
            }
            if(s1.contains("Salvage")) {
                l1 = 1;
            }
            if(s0.contains("Inoperable")) {
                l0 = 2;
            }
            if(s1.contains("Inoperable")) {
                l1 = 2;
            }
            if(s0.contains("Crippled")) {
                l0 = 3;
            }
            if(s1.contains("Crippled")) {
                l1 = 3;
            }
            if(s0.contains("Heavy")) {
                l0 = 4;
            }
            if(s1.contains("Heavy")) {
                l1 = 4;
            }
            if(s0.contains("Light")) {
                l0 = 5;
            }
            if(s1.contains("Light")) {
                l1 = 5;
            }
            if(s0.contains("Undamaged")) {
                l0 = 6;
            }
            if(s1.contains("Undamaged")) {
                l1 = 6;
            }
            return ((Comparable<Integer>)l0).compareTo(l1);
        }
    }