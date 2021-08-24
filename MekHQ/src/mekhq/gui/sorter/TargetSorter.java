package mekhq.gui.sorter;

import mekhq.MekHQ;

import java.io.Serializable;
import java.util.Comparator;

/**
 * A comparator for target numbers written as strings
 * @author Jay Lawson
 */
public class TargetSorter implements Comparator<String>, Serializable {
    private static final long serialVersionUID = 2890151997236948679L;

    @Override
    public int compare(String s0, String s1) {
        s0 = s0.replaceAll("\\+", "");
        s1 = s1.replaceAll("\\+", "");
        int r0;
        int r1;

        switch (s0) {
            case "Impossible":
                r0 = Integer.MAX_VALUE;
                break;
            case "Automatic Failure":
                r0 = Integer.MAX_VALUE - 1;
                break;
            case "Automatic Success":
                r0 = Integer.MIN_VALUE;
                break;
            default:
                try {
                    r0 = Integer.parseInt(s0);
                } catch (Exception e) {
                    MekHQ.getLogger().error(e);
                    r0 = Integer.MAX_VALUE - 1;
                }
                break;
        }

        switch (s1) {
            case "Impossible":
                r1 = Integer.MAX_VALUE;
                break;
            case "Automatic Failure":
                r1 = Integer.MAX_VALUE - 1;
                break;
            case "Automatic Success":
                r1 = Integer.MIN_VALUE;
                break;
            default:
                try {
                    r1 = Integer.parseInt(s1);
                } catch (Exception e) {
                    MekHQ.getLogger().error(e);
                    r1 = Integer.MAX_VALUE - 1;
                }
                break;
        }

        return Integer.compare(r0, r1);
    }
}
