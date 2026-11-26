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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import megamek.Version;
import megamek.common.board.Board;
import mekhq.campaign.Campaign;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * Verifies that the friendly instant and delayed reinforcement lists on an {@link AtBDynamicScenario} survive a save
 * and load. Auxiliary (leadership) and utility units rely on the instant list to arrive on turn 1; if it is lost on
 * load they fall back to reinforcement arrival turns.
 *
 * <p>Also covers the template link of individually deployed units, which sets their deployment zone. If it is lost on
 * load those units arrive in the "Any" zone instead of the zone shown in the lobby.</p>
 */
class AtBDynamicScenarioReinforcementListsTest {
    private static final Version VERSION = new Version();

    @Test
    void instantReinforcementsSurviveAnXmlRoundTrip() throws Exception {
        UUID firstUnitId = UUID.randomUUID();
        UUID secondUnitId = UUID.randomUUID();
        AtBDynamicScenario scenario = scenarioWithTemplate();
        scenario.getFriendlyInstantReinforcements().add(firstUnitId);
        scenario.getFriendlyInstantReinforcements().add(secondUnitId);

        AtBDynamicScenario loaded = load(writeToXml(scenario));

        assertEquals(List.of(firstUnitId, secondUnitId), loaded.getFriendlyInstantReinforcements());
    }

    @Test
    void delayedReinforcementsSurviveAnXmlRoundTrip() throws Exception {
        UUID unitId = UUID.randomUUID();
        AtBDynamicScenario scenario = scenarioWithTemplate();
        scenario.getFriendlyDelayedReinforcements().add(unitId);

        AtBDynamicScenario loaded = load(writeToXml(scenario));

        assertEquals(List.of(unitId), loaded.getFriendlyDelayedReinforcements());
        assertTrue(loaded.getFriendlyInstantReinforcements().isEmpty());
    }

    @Test
    void individualUnitTemplateLinksSurviveAnXmlRoundTrip() throws Exception {
        UUID unitId = UUID.randomUUID();
        AtBDynamicScenario scenario = scenarioWithTemplate();
        ScenarioForceTemplate playerTemplate = new ScenarioForceTemplate();
        playerTemplate.setForceName(ScenarioForceTemplate.PRIMARY_FORCE_TEMPLATE_ID);
        playerTemplate.setActualDeploymentZone(Board.START_CENTER);
        scenario.getTemplate().getScenarioForces().put(playerTemplate.getForceName(), playerTemplate);
        scenario.addUnit(unitId);
        scenario.getPlayerUnitTemplates().put(unitId, playerTemplate);

        AtBDynamicScenario loaded = load(writeToXml(scenario));

        ScenarioForceTemplate loadedTemplate = loaded.getPlayerUnitTemplates().get(unitId);
        assertNotNull(loadedTemplate, "an individually deployed unit must keep its template link after a reload");
        assertEquals(ScenarioForceTemplate.PRIMARY_FORCE_TEMPLATE_ID, loadedTemplate.getForceName());
        assertEquals(Board.START_CENTER, loadedTemplate.getActualDeploymentZone());
    }

    @Test
    void unitTemplateLinksForUnitsNoLongerInTheScenarioAreNotWritten() throws Exception {
        UUID unitId = UUID.randomUUID();
        AtBDynamicScenario scenario = scenarioWithTemplate();
        ScenarioForceTemplate playerTemplate = new ScenarioForceTemplate();
        playerTemplate.setForceName(ScenarioForceTemplate.PRIMARY_FORCE_TEMPLATE_ID);
        scenario.getTemplate().getScenarioForces().put(playerTemplate.getForceName(), playerTemplate);
        scenario.getPlayerUnitTemplates().put(unitId, playerTemplate);

        AtBDynamicScenario loaded = load(writeToXml(scenario));

        assertTrue(loaded.getPlayerUnitTemplates().isEmpty());
    }

    /** The reinforcement lists are only written for a current scenario that still carries its template. */
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
