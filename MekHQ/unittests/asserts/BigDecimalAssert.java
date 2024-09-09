package asserts;

import org.junit.jupiter.api.Assertions;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Deric Page (deric.page@nisc.coop) (ext 2335)
 */
public class BigDecimalAssert {
    public static void assertEquals(BigDecimal expected, Object actual, int scale) {
        assertNotNull(actual);

        assertInstanceOf(BigDecimal.class, actual, "actual: " + actual.getClass().getName());
        BigDecimal scaledExpected = expected.setScale(scale, RoundingMode.FLOOR);
        BigDecimal scaledActual = ((BigDecimal) actual).setScale(scale, RoundingMode.FLOOR);
        Assertions.assertEquals(0, scaledExpected.compareTo(scaledActual),
                "\n\texpected: " + scaledExpected + "\n\tactual: " + scaledActual);
    }
}
