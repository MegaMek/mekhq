package mekhq.campaign.universe;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;

import org.junit.Test;
import org.w3c.dom.DOMException;

public class SystemsIntegrationTest {
    @Test
    public void loadDefaultTest()
            throws DOMException, FileNotFoundException, IOException, ParseException {
        Systems systems = Systems.loadDefault();

        assertNotNull(systems);

        PlanetarySystem terra = systems.getSystemById("Terra");
        assertNotNull(terra);
        assertEquals(0.0, terra.getX(), 0.001);
        assertEquals(0.0, terra.getY(), 0.001);

        Planet thirdRock = terra.getPlanetById("Terra");
        assertNotNull(thirdRock);
        assertEquals(thirdRock, terra.getPlanet(3));
    }
}
