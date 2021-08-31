package mekhq.gui.sorter;

import mekhq.MekHQ;

import java.io.Serializable;
import java.util.Comparator;

/**
 * A comparator for bonuses written as strings with "-" sorted to the bottom always
 * @author Jay Lawson
 */
public class BonusSorter implements Comparator<String>, Serializable {
    private static final long serialVersionUID = -2023622114026113846L;

    @Override
    public int compare(String s0, String s1) {
        int i0, i1;

        if (s0.contains("/")) {
            String[] temp = s0.split("/");
            if (temp[0].contains("-") && temp[1].contains("-")) {
                i0 = 99;
            } else {
                int t0;
                try {
                    t0 = temp[0].contains("-") ? 0 : Integer.parseInt(temp[0]);
                } catch (Exception e) {
                    MekHQ.getLogger().error(e);
                    t0 = 0;
                }

                int t1;
                try {
                    t1 = temp[1].contains("-") ? 0 : Integer.parseInt(temp[1]);
                } catch (Exception e) {
                    MekHQ.getLogger().error(e);
                    t1 = 0;
                }
                i0 = t0 + t1;
            }
        } else {
            try {
                i0 = s0.equals("-") ? 90 : Integer.parseInt(s0);
            } catch (Exception e) {
                MekHQ.getLogger().error(e);
                i0 = 90;
            }
        }

        if (s1.contains("/")) {
            String[] temp = s1.split("/");
            if (temp[0].contains("-") && temp[1].contains("-")) {
                i1 = 99;
            } else {
                int t0;
                try {
                    t0 = temp[0].contains("-") ? 0 : Integer.parseInt(temp[0]);
                } catch (Exception e) {
                    MekHQ.getLogger().error(e);
                    t0 = 0;
                }

                int t1;
                try {
                    t1 = temp[1].contains("-") ? 0 : Integer.parseInt(temp[1]);
                } catch (Exception e) {
                    MekHQ.getLogger().error(e);
                    t1 = 0;
                }
                i1 = t0 + t1;
            }
        } else {
            try {
                i1 = s1.equals("-") ? 90 : Integer.parseInt(s1);
            } catch (Exception e) {
                MekHQ.getLogger().error(e);
                i1 = 90;
            }
        }

        return Integer.compare(i0, i1);
    }
}
