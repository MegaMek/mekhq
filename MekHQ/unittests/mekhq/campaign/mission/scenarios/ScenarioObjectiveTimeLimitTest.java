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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import megamek.Version;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.scenarios.ScenarioObjective.ObjectiveCriterion;
import mekhq.campaign.mission.scenarios.ScenarioObjective.TimeLimitType;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * Covers objective time limits surviving scenario finalization, and generated objective descriptions no longer being
 * written into saves (where they went stale once the time limit changed).
 *
 * @author Illiani
 * @since 0.51.01
 */
class ScenarioObjectiveTimeLimitTest {
    /** A save version from before the time limit fix, so the load-time repair runs. */
    private static final Version VERSION_BEFORE_FIX = new Version("0.51.01");
    /** The first save version written with the fix, so the load-time repair is skipped. */
    private static final Version VERSION_WITH_FIX = new Version("0.51.02");

    /** The Recon Evasion objective: keep 66% of the player's force intact for at least 12 turns. */
    private static ScenarioObjective fixedPreserveObjective(int timeLimit) {
        ScenarioObjective objective = new ScenarioObjective();
        objective.setObjectiveCriterion(ObjectiveCriterion.Preserve);
        objective.setDescription("");
        objective.setPercentage(66);
        objective.setTimeLimitType(TimeLimitType.Fixed);
        objective.setTimeLimitAtMost(false);
        objective.setTimeLimit(timeLimit);
        objective.setTimeLimitScaleFactor(1);
        objective.addForce("Player");
        return objective;
    }

    private static ScenarioObjective scaledObjective(int scaleFactor) {
        ScenarioObjective objective = new ScenarioObjective();
        objective.setObjectiveCriterion(ObjectiveCriterion.Destroy);
        objective.setDescription("");
        objective.setPercentage(50);
        objective.setTimeLimitType(TimeLimitType.ScaledToPrimaryUnitCount);
        objective.setTimeLimitAtMost(true);
        objective.setTimeLimitScaleFactor(scaleFactor);
        return objective;
    }

    @Test
    void finalizationKeepsFixedTimeLimit() {
        AtBDynamicScenario scenario = new AtBDynamicScenario();
        ScenarioObjective objective = fixedPreserveObjective(12);
        scenario.getScenarioObjectives().add(objective);

        AtBDynamicScenarioFactory.scaleObjectiveTimeLimits(scenario, mock(Campaign.class));

        assertEquals(12, objective.getTimeLimit());
    }

    @Test
    void finalizationFillsMissingTimeLimitWithDefault() {
        AtBDynamicScenario scenario = new AtBDynamicScenario();
        ScenarioObjective objective = new ScenarioObjective();
        objective.setObjectiveCriterion(ObjectiveCriterion.Destroy);
        scenario.getScenarioObjectives().add(objective);

        AtBDynamicScenarioFactory.scaleObjectiveTimeLimits(scenario, mock(Campaign.class));

        assertEquals(6, objective.getTimeLimit());
    }

    @Test
    void scaledTimeLimitUsesAtLeastSixUnits() {
        AtBDynamicScenario scenario = new AtBDynamicScenario();
        ScenarioObjective objective = scaledObjective(2);
        scenario.getScenarioObjectives().add(objective);

        AtBDynamicScenarioFactory.scaleObjectiveTimeLimits(scenario, mock(Campaign.class));

        assertEquals(12, objective.getTimeLimit());
    }

    @Test
    void scaledTimeLimitUsesActualUnitCountAboveSix() {
        Campaign campaign = mock(Campaign.class);
        AtBDynamicScenario scenario = new AtBDynamicScenario();
        ScenarioObjective objective = scaledObjective(2);
        scenario.getScenarioObjectives().add(objective);

        List<Entity> botEntities = new ArrayList<>();
        for (int entityIndex = 0; entityIndex < 8; entityIndex++) {
            botEntities.add(mock(Entity.class));
        }
        BotForce botForce = mock(BotForce.class);
        when(botForce.getFullEntityList(any())).thenReturn(botEntities);
        ScenarioForceTemplate forceTemplate = mock(ScenarioForceTemplate.class);
        when(forceTemplate.getContributesToUnitCount()).thenReturn(true);
        scenario.getBotForceTemplates().put(botForce, forceTemplate);

        AtBDynamicScenarioFactory.scaleObjectiveTimeLimits(scenario, campaign);

        assertEquals(16, objective.getTimeLimit());
    }

    @Test
    void savedObjectiveDoesNotContainGeneratedDescription() {
        ScenarioObjective objective = fixedPreserveObjective(12);

        String xml = serialize(objective);

        assertFalse(xml.contains("Keep at least"), "generated description should not be saved: " + xml);
    }

    @Test
    void authoredDescriptionSurvivesSaveAndLoad() throws Exception {
        ScenarioObjective objective = fixedPreserveObjective(12);
        objective.setDescription("Stay hidden until the patrols pass.");

        ScenarioObjective loaded = deserialize(serialize(objective));

        assertEquals("Stay hidden until the patrols pass.", loaded.getDescription());
    }

    @Test
    void staleGeneratedDescriptionIsClearedOnLoad() throws Exception {
        // An older save stored the generated text from when the limit was 12, then the limit became 6
        ScenarioObjective objective = fixedPreserveObjective(12);
        objective.setDescription(objective.getGeneratedDescription());
        objective.setTimeLimit(6);

        ScenarioObjective loaded = deserialize(serialize(objective));

        assertEquals("", loaded.getOverrideDescription());
        assertTrue(loaded.getDescription().contains("for at least 6 turns"), loaded.getDescription());
    }

    @Test
    void staleDescriptionFromUnsetTimeLimitIsCleared() {
        ScenarioObjective objective = scaledObjective(1);
        objective.setDescription(objective.getGeneratedDescription()); // printed "null" as the turn count
        objective.setTimeLimit(9);

        assertTrue(objective.clearStaleGeneratedDescription());
        assertTrue(objective.getDescription().contains("within at most 9 turns"), objective.getDescription());
    }

    @Test
    void authoredDescriptionIsNotCleared() {
        ScenarioObjective objective = fixedPreserveObjective(12);
        objective.setDescription("Keep at least half of your lance alive.");

        assertFalse(objective.clearStaleGeneratedDescription());
        assertEquals("Keep at least half of your lance alive.", objective.getDescription());
    }

    @Test
    void loadRestoresFixedTimeLimitOverwrittenByFinalization() throws Exception {
        ScenarioTemplate template = new ScenarioTemplate();
        template.scenarioObjectives.add(fixedPreserveObjective(12));

        AtBDynamicScenario scenario = new AtBDynamicScenario();
        scenario.setTemplate(template);
        scenario.getScenarioObjectives().add(fixedPreserveObjective(6));

        AtBDynamicScenario loaded = loadScenario(writeScenario(scenario), VERSION_BEFORE_FIX);

        assertEquals(12, loaded.getScenarioObjectives().get(0).getTimeLimit());
    }

    @Test
    void loadKeepsDeliberateSixTurnLimitInSavesWrittenWithTheFix() throws Exception {
        ScenarioTemplate template = new ScenarioTemplate();
        template.scenarioObjectives.add(fixedPreserveObjective(12));

        AtBDynamicScenario scenario = new AtBDynamicScenario();
        scenario.setTemplate(template);
        scenario.getScenarioObjectives().add(fixedPreserveObjective(6));

        AtBDynamicScenario loaded = loadScenario(writeScenario(scenario), VERSION_WITH_FIX);

        assertEquals(6, loaded.getScenarioObjectives().get(0).getTimeLimit());
    }

    @Test
    void loadKeepsTimeLimitThatDiffersFromTheDefault() throws Exception {
        ScenarioTemplate template = new ScenarioTemplate();
        template.scenarioObjectives.add(fixedPreserveObjective(12));

        AtBDynamicScenario scenario = new AtBDynamicScenario();
        scenario.setTemplate(template);
        scenario.getScenarioObjectives().add(fixedPreserveObjective(10));

        AtBDynamicScenario loaded = loadScenario(writeScenario(scenario), VERSION_BEFORE_FIX);

        assertEquals(10, loaded.getScenarioObjectives().get(0).getTimeLimit());
    }

    private static String serialize(ScenarioObjective objective) {
        StringWriter stringWriter = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
            objective.Serialize(printWriter);
        }
        return stringWriter.toString();
    }

    private static ScenarioObjective deserialize(String xml) throws Exception {
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Element root = documentBuilder.parse(new InputSource(new StringReader(xml))).getDocumentElement();
        ScenarioObjective objective = ScenarioObjective.Deserialize(root);
        assertNotNull(objective);
        return objective;
    }

    private static String writeScenario(AtBDynamicScenario scenario) {
        StringWriter stringWriter = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
            scenario.writeToXML(printWriter, 0);
        }
        return stringWriter.toString();
    }

    private static AtBDynamicScenario loadScenario(String xml, Version saveVersion) throws Exception {
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Element root = documentBuilder.parse(new InputSource(new StringReader(xml))).getDocumentElement();
        Scenario loaded = Scenario.generateInstanceFromXML(root, mock(Campaign.class), saveVersion);
        return assertInstanceOf(AtBDynamicScenario.class, loaded);
    }
}
