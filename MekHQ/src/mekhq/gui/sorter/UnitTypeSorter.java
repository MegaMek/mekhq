package mekhq.gui.sorter;

import java.util.Comparator;

import megamek.common.UnitType;


    /**
     * A comparator for unit types
     * @author Jay Lawson
     *
     */
    public class UnitTypeSorter implements Comparator<String> {

        @Override
        public int compare(String s0, String s1) {
            //lets find the weight class integer for each name
            int l0 = 0;
            int l1 = 0;
            for(int i = 0; i <= UnitType.SPACE_STATION; i++) {
                if(UnitType.getTypeDisplayableName(i).equals(s0)) {
                    l0 = i;
                }
                if(UnitType.getTypeDisplayableName(i).equals(s1)) {
                    l1 = i;
                }
            }
            return ((Comparable<Integer>)l1).compareTo(l0);
        }
    }
