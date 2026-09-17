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
package mekhq.campaign.mission.scenarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import megamek.Version;
import mekhq.campaign.Campaign;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * Verifies that the player's off-board deployment choices on an {@link AtBDynamicScenario} survive a save and load,
 * and that the loader copes with a damaged value instead of aborting or misreading it.
 */
class AtBDynamicScenarioOffBoardForceIdsTest {
    private static final Version VERSION = new Version();
    private static final String OFF_BOARD_TAG = "offBoardForceIDs";

    @Test
    void offBoardForceIdsSurviveAnXmlRoundTrip() throws Exception {
        AtBDynamicScenario scenario = scenarioWithTemplate();
        scenario.setForceDeployingOffBoard(3, true);
        scenario.setForceDeployingOffBoard(7, true);

        AtBDynamicScenario loaded = load(writeToXml(scenario));

        assertEquals(Set.of(3, 7), loaded.getOffBoardForceIDs());
        assertTrue(loaded.isForceDeployingOffBoard(3));
        assertFalse(loaded.isForceDeployingOffBoard(5));
    }

    @Test
    void unmarkedForceIsNotSaved() throws Exception {
        AtBDynamicScenario scenario = scenarioWithTemplate();
        scenario.setForceDeployingOffBoard(3, true);
        scenario.setForceDeployingOffBoard(7, true);
        scenario.setForceDeployingOffBoard(3, false);

        AtBDynamicScenario loaded = load(writeToXml(scenario));

        assertEquals(Set.of(7), loaded.getOffBoardForceIDs());
    }

    @Test
    void noOffBoardForcesWritesNoTagAndLoadsEmpty() throws Exception {
        String xml = writeToXml(scenarioWithTemplate());

        assertFalse(xml.contains(OFF_BOARD_TAG), "an empty off-board set should not be written at all");
        assertTrue(load(xml).getOffBoardForceIDs().isEmpty());
    }

    @Test
    void unreadableIdIsSkippedWithoutAbortingTheLoad() throws Exception {
        String xml = "<scenario id=\"1\" type=\"" + AtBDynamicScenario.class.getName() + "\">"
                           + "<" + OFF_BOARD_TAG + ">3, seven ,7,</" + OFF_BOARD_TAG + ">"
                           + "</scenario>";

        AtBDynamicScenario loaded = load(xml);

        assertEquals(Set.of(3, 7), loaded.getOffBoardForceIDs());
        assertFalse(loaded.isForceDeployingOffBoard(0), "a damaged value must not be read as force 0");
    }

    @Test
    void removingAFormationClearsItsOffBoardFlag() {
        AtBDynamicScenario scenario = new AtBDynamicScenario();
        scenario.setForceDeployingOffBoard(3, true);

        scenario.removeFormation(3);

        assertFalse(scenario.isForceDeployingOffBoard(3));
    }

    /** The off-board tag is only written for a current scenario that still carries its template. */
    private static AtBDynamicScenario scenarioWithTemplate() {
        AtBDynamicScenario scenario = new AtBDynamicScenario();
        scenario.setTemplate(new ScenarioTemplate());
        return scenario;
    }

    private static String writeToXml(AtBDynamicScenario scenario) {
        StringWriter stringWriter = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
            scenario.writeToXML(printWriter, 0);
        }
        return stringWriter.toString();
    }

    /** Loads the XML through the same factory entry point a campaign save uses. */
    private static AtBDynamicScenario load(String xml) throws Exception {
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Element root = documentBuilder.parse(new InputSource(new StringReader(xml))).getDocumentElement();

        Scenario loaded = Scenario.generateInstanceFromXML(root, mock(Campaign.class), VERSION);

        return assertInstanceOf(AtBDynamicScenario.class, loaded);
    }
}
