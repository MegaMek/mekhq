/*
 * Copyright (c) 2020 - The MegaMek Team. All rights reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

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
