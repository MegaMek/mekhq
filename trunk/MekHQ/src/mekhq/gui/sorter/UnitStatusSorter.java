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
            if(s0.contains("Mothballed")) {
                l0 = 1;
            }
            if(s1.contains("Mothballed")) {
                l1 = 1;
            }
            if(s0.contains("Mothballing")) {
                l0 = 2;
            }
            if(s1.contains("Mothballing")) {
                l1 = 2;
            }
            if(s0.contains("Activating")) {
                l0 = 3;
            }
            if(s1.contains("Activating")) {
                l1 = 3;
            }
            if(s0.contains("In Transit")) {
                l0 = 4;
            }
            if(s1.contains("In Transit")) {
                l1 = 4;
            }
            if(s0.contains("Refitting")) {
                l0 = 5;
            }
            if(s1.contains("Refitting")) {
                l1 = 5;
            }
            if(s0.contains("Deployed")) {
                l0 = 6;
            }
            if(s1.contains("Deployed")) {
                l1 = 6;
            }
            if(s0.contains("Salvage")) {
                l0 = 7;
            }
            if(s1.contains("Salvage")) {
                l1 = 7;
            }
            if(s0.contains("Inoperable")) {
                l0 = 8;
            }
            if(s1.contains("Inoperable")) {
                l1 = 8;
            }
            if(s0.contains("Crippled")) {
                l0 = 9;
            }
            if(s1.contains("Crippled")) {
                l1 = 9;
            }
            if(s0.contains("Heavy")) {
                l0 = 10;
            }
            if(s1.contains("Heavy")) {
                l1 = 10;
            }
            if(s0.contains("Light")) {
                l0 = 11;
            }
            if(s1.contains("Light")) {
                l1 = 11;
            }
            if(s0.contains("Undamaged")) {
                l0 = 12;
            }
            if(s1.contains("Undamaged")) {
                l1 = 12;
            }
            return ((Comparable<Integer>)l0).compareTo(l1);
        }
    }