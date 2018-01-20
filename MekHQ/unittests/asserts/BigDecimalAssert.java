package asserts;

import junit.framework.TestCase;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author Deric Page (deric.page@nisc.coop) (ext 2335)
 */
public class BigDecimalAssert {
    public static void assertEquals(BigDecimal expected, Object actual, int scale) {
        TestCase.assertNotNull(actual);
        TestCase.assertTrue("actual: " + actual.getClass().getName(), actual instanceof BigDecimal);
        BigDecimal scaledExpected = expected.setScale(scale, RoundingMode.FLOOR);
        BigDecimal scaledActual = ((BigDecimal) actual).setScale(scale, RoundingMode.FLOOR);
        TestCase.assertTrue("\n\texpected: " + scaledExpected + "\n\tactual: " + scaledActual,
                            scaledExpected.compareTo(scaledActual) == 0);
    }
}
