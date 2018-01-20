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
            int l0 = getDamageStateIndex(s0);
            int l1 = getDamageStateIndex(s1);

            return ((Comparable<Integer>)l0).compareTo(l1);
        }

        public static int getDamageStateIndex(String damageState) {
        	int idx = 0;

            if(damageState.contains("Mothballed")) {
                idx = 1;
            }
            if(damageState.contains("Mothballing")) {
                idx = 2;
            }
            if(damageState.contains("Activating")) {
                idx = 3;
            }
            if(damageState.contains("In Transit")) {
                idx = 4;
            }
            if(damageState.contains("Refitting")) {
                idx = 5;
            }
            if(damageState.contains("Deployed")) {
                idx = 6;
            }
            if(damageState.contains("Salvage")) {
                idx = 7;
            }
            if(damageState.contains("Inoperable")) {
                idx = 8;
            }
            if(damageState.contains("Crippled")) {
                idx = 9;
            }
            if(damageState.contains("Heavy")) {
                idx = 10;
            }
            if(damageState.contains("Moderate")) {
                idx = 11;
            }
            if(damageState.contains("Light")) {
                idx = 12;
            }
            if(damageState.contains("Undamaged")) {
                idx = 13;
            }

            return idx;
        }
    }
