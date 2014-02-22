package mekhq.gui.sorter;

import java.util.Comparator;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Ranks;

 /**
     * A comparator for ranks written as strings with "-" sorted to the bottom always
     * @author Jay Lawson
     *
     */
    public class RankSorter implements Comparator<String> {

        private Ranks ranks;
        
        public RankSorter(Campaign c) {
            ranks = c.getRanks();
        }
        
        @Override
        public int compare(String s0, String s1) {
            if(s0.equals("-") && s1.equals("-")) {
                return 0;
            } else if(s0.equals("-")) {
                return 1;
            } else if(s1.equals("-")) {
                return -1;
            } else {
                //get the numbers associated with each rank string
                int r0 = ranks.getRankOrder(s0);
                int r1 = ranks.getRankOrder(s1);
                return ((Comparable<Integer>)r0).compareTo(r1);
            }
        }
    }