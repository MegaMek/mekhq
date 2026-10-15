/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import megamek.Version;
import megamek.common.bays.BayType;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.missing.MissingCubicle;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

/**
 * Cubicles from saves made before MegaMek renamed its Mek bay types load as Mek bays with their unit and bay, instead
 * of loading broken and stopping the campaign from being saved (issue #3912).
 */
class CubicleOlderSaveTest {
    private static final UUID DROPSHIP_ID = UUID.randomUUID();

    /** A cubicle written the way a 0.49 save wrote it, with the given stored bay type. */
    private static String savedCubicle(String partClass, String storedBayType) {
        return "<part id=\"9643\" type=\"" + partClass + "\">"
                     + "<id>9643</id><name>Cubicle</name><unitTonnage>1900</unitTonnage>"
                     + "<unitId>" + DROPSHIP_ID + "</unitId><quantity>1</quantity><quality>3</quality>"
                     + "<parentPartId>9641</parentPartId>"
                     + "<bayType>" + storedBayType + "</bayType></part>";
    }

    private static Part loaded(String partXml) throws Exception {
        Element partElement = MHQXMLUtility.newSafeDocumentBuilder()
                                    .parse(new ByteArrayInputStream(partXml.getBytes(StandardCharsets.UTF_8)))
                                    .getDocumentElement();
        return Part.generateInstanceFromXML(partElement, new Version("0.49.18"));
    }

    private static String olderMekBayName() {
        // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
        return "MECH";
    }

    @Test
    void aCubicleWithTheOlderMekBayNameLoadsAsAMekBayOnItsUnit() throws Exception {
        Cubicle cubicle = (Cubicle) loaded(savedCubicle(Cubicle.class.getName(), olderMekBayName()));

        assertEquals(BayType.MEK, cubicle.getBayType());
        assertNotNull(cubicle.getUnit(), "The rest of the cubicle loads too, including its unit");
        assertEquals(DROPSHIP_ID, cubicle.getUnit().getId());
        assertNotNull(cubicle.getParentPart(), "and its bay");
    }

    @Test
    void aMissingCubicleWithTheOlderMekBayNameLoadsAsAMekBay() throws Exception {
        MissingCubicle missingCubicle = (MissingCubicle) loaded(savedCubicle(MissingCubicle.class.getName(),
              olderMekBayName()));

        Part replacement = missingCubicle.getNewPart();

        assertTrue(replacement instanceof Cubicle);
        assertEquals(BayType.MEK, ((Cubicle) replacement).getBayType());
    }

    @Test
    void currentBayTypeNamesStillLoad() throws Exception {
        Cubicle cubicle = (Cubicle) loaded(savedCubicle(Cubicle.class.getName(), BayType.VEHICLE_HEAVY.toString()));

        assertEquals(BayType.VEHICLE_HEAVY, cubicle.getBayType());
    }

    @Test
    void anUnreadableBayTypeStillLoadsTheCubicle() throws Exception {
        Cubicle cubicle = (Cubicle) loaded(savedCubicle(Cubicle.class.getName(), "NOT_A_BAY"));

        assertEquals(BayType.MEK, cubicle.getBayType());
        assertNotNull(cubicle.getUnit());
    }

    @Test
    void aCubicleWithoutABayTypeCanStillBeSavedAndPriced() {
        Cubicle cubicle = new Cubicle(1900, null, null);

        StringWriter savedXml = new StringWriter();
        PrintWriter printWriter = new PrintWriter(savedXml);
        cubicle.writeToXML(printWriter, 0);
        printWriter.flush();

        assertFalse(savedXml.toString().contains("bayType"));
        assertEquals(0, cubicle.getTonnage());
        assertEquals(Money.zero(), cubicle.getStickerPrice());
    }
}
