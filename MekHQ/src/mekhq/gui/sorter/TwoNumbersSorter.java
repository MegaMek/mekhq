package mekhq.gui.sorter;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A comparator for two numbers formatted like: "123 [54]"
 * <p>
 * Sorts by first number, then if available second number.
 * Numbers without a second number are sorted before all those with them,
 * the same way FormattedNumberSorter would do so.
 */
public final class TwoNumbersSorter implements Comparator<String>{
    private static final Comparator<String> NUM_SORTER = new FormattedNumberSorter();
    private static final Pattern NUM_PATTERN =
        Pattern.compile("^([+-]?\\d*)\\s+\\[([+-]?\\d*)\\]\\s*$"); //$NON-NLS-1$

    @Override public int compare(String s1, String s2) {
        Matcher match1 = NUM_PATTERN.matcher(s1);
        Matcher match2 = NUM_PATTERN.matcher(s2);
        boolean hasSecondNumber1 = match1.matches();
        boolean hasSecondNumber2 = match2.matches();
        if(!hasSecondNumber1 && !hasSecondNumber2) {
            return NUM_SORTER.compare(s1, s2);
        }
        
        String firstNum1 = s1;
        String firstNum2 = s2;
        if(hasSecondNumber1) {
            firstNum1 = match1.group(1);
        }
        if(hasSecondNumber2) {
            firstNum2 = match2.group(1);
        }
        
        int result = NUM_SORTER.compare(firstNum1, firstNum2);
        if(result != 0) {
            return result;
        }
        
        // Sort numbers without a second number before those with
        if(hasSecondNumber1 && !hasSecondNumber2) {
            return -1;
        }
        if(!hasSecondNumber1 && hasSecondNumber2) {
            return 1;
        }
        // Else, sort by second number
        return NUM_SORTER.compare(match1.group(2), match2.group(2));
    }
}
