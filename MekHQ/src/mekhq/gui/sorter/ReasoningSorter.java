package mekhq.gui.sorter;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReasoningSorter implements Comparator<String> {
    private static final Pattern NUM_PATTERN = Pattern.compile("\\((\\d+)\\)\\s*$");

    private static int extractNumber(String s) {
        Matcher m = NUM_PATTERN.matcher(s);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        // Put malformed strings last
        return Integer.MAX_VALUE;
    }

    @Override
    public int compare(String a, String b) {
        return Integer.compare(extractNumber(a), extractNumber(b));
    }
}
