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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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
package mekhq.gui.scenarioTemplateEditor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import megamek.common.units.UnitType;
import mekhq.campaign.mission.scenarios.ObjectiveEffect;
import mekhq.campaign.mission.scenarios.ObjectiveEffect.EffectScalingType;
import mekhq.campaign.mission.scenarios.ObjectiveEffect.ObjectiveEffectType;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceGenerationMethod;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.SynchronizedDeploymentType;
import mekhq.campaign.mission.scenarios.ScenarioMapParameters.MapLocation;
import mekhq.campaign.mission.scenarios.ScenarioObjective;
import mekhq.campaign.mission.scenarios.ScenarioObjective.ObjectiveCriterion;
import mekhq.campaign.mission.scenarios.ScenarioTemplate;
import mekhq.campaign.mission.scenarios.ScenarioTemplate.BattlefieldControlType;
import org.junit.jupiter.api.Test;

/**
 * Phase 4.5 - the capstone integration test. Rebuilds a fully-populated template by round-tripping it through the
 * extracted editor panels as a set (the property/map/modifier {@code writeInto} panels plus a per-force
 * {@link ForceEditorPanel}), then asserts the reconstructed template serializes byte-identically to the original. This
 * is the closest headless analogue to loading a template into the dialog and saving it back.
 */
class ScenarioTemplateEditorPanelsIntegrationTest {

    private static final List<String> TERRAIN = List.of("Woods", "Rough", "Urban");
    private static final List<String> MODIFIER_KEYS = List.of("ModifierA", "ModifierB", "ModifierC");

    @Test
    void panelsRebuildTemplateIdentically() {
        ScenarioTemplate original = buildMaximalTemplate();

        ScenarioTemplate rebuilt = new ScenarioTemplate();

        // property / map / modifier panels: load from original, write into the rebuilt template
        TemplatePropertiesPanel propertiesPanel = new TemplatePropertiesPanel();
        propertiesPanel.load(original);
        propertiesPanel.writeInto(rebuilt);

        MapParametersPanel mapPanel = new MapParametersPanel(TERRAIN);
        mapPanel.load(original.mapParameters);
        mapPanel.writeInto(rebuilt.mapParameters);

        ModifiersPanel modifiersPanel = new ModifiersPanel(MODIFIER_KEYS);
        modifiersPanel.load(original.scenarioModifiers);
        modifiersPanel.writeInto(rebuilt.scenarioModifiers);

        // forces: round-trip each through the force editor
        ForceEditorPanel forcePanel = new ForceEditorPanel(List.of());
        forcePanel.setAvailableForceIds(original.getScenarioForces().keySet());
        for (ScenarioForceTemplate force : original.getAllScenarioForces()) {
            forcePanel.loadForce(force);
            ScenarioForceTemplate rebuiltForce = forcePanel.buildForceTemplate();
            rebuilt.getScenarioForces().put(rebuiltForce.getForceName(), rebuiltForce);
        }

        // objectives are edited live on the template (ObjectivesPanel does not transform them)
        rebuilt.scenarioObjectives.addAll(original.scenarioObjectives);

        assertEquals(toXml(original), toXml(rebuilt),
              "The assembled panels should rebuild the template without losing or reshaping any field");
    }

    private static ScenarioTemplate buildMaximalTemplate() {
        ScenarioTemplate template = new ScenarioTemplate();
        template.name = "Integration Template";
        template.shortBriefing = "Short.";
        template.detailedBriefing = "Detailed.";
        template.setStratConScenarioType("CONVOY");
        template.battlefieldControl = BattlefieldControlType.ENEMY;
        template.isHostileFacility = true;
        template.isAlliedFacility = true;
        template.isSuitedForAmbushes = true;
        template.isSuitedForBungledPatrols = true;

        template.mapParameters.setBaseWidth(35);
        template.mapParameters.setBaseHeight(45);
        template.mapParameters.setWidthScalingIncrement(6);
        template.mapParameters.setHeightScalingIncrement(7);
        template.mapParameters.setAdditionalMapSheetWide(2);
        template.mapParameters.setAdditionalMapSheetTall(3);
        template.mapParameters.setAllowRotation(true);
        template.mapParameters.setUseStandardAtBSizing(true);
        template.mapParameters.setMapLocation(MapLocation.SpecificGroundTerrain);
        template.mapParameters.getAllowedTerrainType().add("Woods");
        template.mapParameters.getAllowedTerrainType().add("Rough");

        template.scenarioModifiers.add("ModifierA");
        template.scenarioModifiers.add("ModifierC");

        ScenarioForceTemplate force = new ScenarioForceTemplate(ForceAlignment.Opposing.ordinal(),
              ForceGenerationMethod.BVScaled.ordinal(), 1.25, List.of(2, 4), 5, 40, UnitType.TANK);
        force.setForceName("OpFor");
        force.setGenerationOrder(2);
        force.setArrivalTurn(3);
        force.setContributesToBV(false);
        force.setContributesToUnitCount(false);
        force.setContributesToMapSize(false);
        force.setSyncDeploymentType(SynchronizedDeploymentType.None);
        force.getRoleCollections().add("RAIDER");
        template.getScenarioForces().put(force.getForceName(), force);

        ScenarioObjective objective = new ScenarioObjective();
        objective.setObjectiveCriterion(ObjectiveCriterion.Destroy);
        objective.setDescription("Destroy them");
        objective.setPercentage(50);
        ObjectiveEffect effect = new ObjectiveEffect();
        effect.effectType = ObjectiveEffectType.ScenarioVictory;
        effect.effectScaling = EffectScalingType.Fixed;
        effect.howMuch = 1;
        objective.addSuccessEffect(effect);
        template.scenarioObjectives.add(objective);

        return template;
    }

    private static String toXml(ScenarioTemplate template) {
        StringWriter stringWriter = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
            template.Serialize(printWriter);
        }
        return stringWriter.toString();
    }
}
