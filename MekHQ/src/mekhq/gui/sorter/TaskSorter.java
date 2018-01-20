package mekhq.gui.sorter;

import java.util.Comparator;

/**
     * A comparator for skills levels (e.g. Regular, Veteran, etc)
     *   * @author Dylan Myers
     *
     */
    public class TaskSorter implements Comparator<String> {

    	// Nothing compare to make the filtering work
        @Override
        public int compare(String s0, String s1) {
            return 0;
        }
    }
