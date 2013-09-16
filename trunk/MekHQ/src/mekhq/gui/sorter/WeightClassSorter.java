package mekhq.gui.sorter;

import java.util.Comparator;

import megamek.common.EntityWeightClass;



    /**
     * A comparator for unit weight classes
     * @author Jay Lawson
     *
     */
    public class WeightClassSorter implements Comparator<String> {

        @Override
        public int compare(String s0, String s1) {
            //lets find the weight class integer for each name
            int l0 = 0;
            int l1 = 0;
            for(int i = 0; i < EntityWeightClass.SIZE; i++) {
                if(EntityWeightClass.getClassName(i).equals(s0)) {
                    l0 = i;
                }
                if(EntityWeightClass.getClassName(i).equals(s1)) {
                    l1 = i;
                }
            }
            return ((Comparable<Integer>)l0).compareTo(l1);
        }
    }