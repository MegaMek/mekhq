package mekhq.gui.sorter;

import java.util.Comparator;

/**
 *
 * @author Dylan Myers
 * Comparator for comparing details in the warehouse and parts store
 */
public class PartsDetailSorter implements Comparator<String> {

    @Override
    public int compare(String s0, String s1) {
    	double l0 = -1;
    	double l1 = -1;
        String[] ss0 = s0.replace("<html>", "").replace("</html>", "").replace("<nobr>", "").replace("</nobr>", "").split(" ");
        String[] ss1 = s1.replace("<html>", "").replace("</html>", "").replace("<nobr>", "").replace("</nobr>", "").split(" ");
        if(!ss0[0].isEmpty()) {
            l0 = Double.parseDouble(ss0[0]);
        }
        if(!ss1[0].isEmpty()) {
        	l1 = Double.parseDouble(ss1[0]);
        }
        s0 = "";
        s1 = "";
        if(ss0.length > 1) {
        	s0 = ss0[1];
        }
        if(ss1.length > 1) {
        	s1 = ss1[1];
        }
        int sComp = s0.compareTo(s1);
        if (sComp == 0) {
            return ((Comparable<Double>)l0).compareTo(l1);
        } else {
            return sComp;
        }
    }

}
