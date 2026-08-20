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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.units.EntityWeightClass;
import megamek.common.units.UnitType;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceGenerationMethod;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.SynchronizedDeploymentType;
import org.junit.jupiter.api.Test;

/**
 * Phase 4.1 panel test for {@link ForceEditorPanel}: a full loadForce -> buildForceTemplate round-trip, exercised
 * headlessly. Uses an Opposing/BV-scaled force so the alignment listener does not override the loaded contributes-to
 * flags.
 */
class ForceEditorPanelTest {

    @Test
    void loadForceThenBuildRoundTripsTheForce() {
        ScenarioForceTemplate source = new ScenarioForceTemplate(ForceAlignment.Opposing.ordinal(),
              ForceGenerationMethod.BVScaled.ordinal(),
              1.25,
              List.of(2, 4, 6),
              ScenarioForceTemplate.DESTINATION_EDGE_RANDOM,
              40,
              UnitType.TANK);
        source.setForceName("Bravo");
        source.setArrivalTurn(3);
        source.setMaxWeightClass(EntityWeightClass.WEIGHT_ASSAULT);
        source.setMinWeightClass(EntityWeightClass.WEIGHT_MEDIUM);
        source.setGenerationOrder(2);
        source.setStartingAltitude(6);
        source.setUseArtillery(true);
        source.setDeployOffboard(true);
        source.setSubjectToRandomRemoval(false);
        source.setSyncRetreatThreshold(true);
        source.setSyncDeploymentType(SynchronizedDeploymentType.None);
        source.setFixedMul("Some.mul");
        source.setObjectiveLinkedForces(List.of("Alpha"));
        source.getRoleCollections().add("RAIDER,CAVALRY");

        ForceEditorPanel panel = new ForceEditorPanel(List.of("Some.mul", "Other.mul"));
        panel.setAvailableForceIds(List.of("Alpha"));
        panel.loadForce(source);

        ScenarioForceTemplate result = panel.buildForceTemplate();

        assertEquals("Bravo", result.getForceName());
        assertEquals(ForceAlignment.Opposing.ordinal(), result.getForceAlignment());
        assertEquals(ForceGenerationMethod.BVScaled.ordinal(), result.getGenerationMethod());
        assertEquals(1.25, result.getForceMultiplier());
        assertEquals(ScenarioForceTemplate.DESTINATION_EDGE_RANDOM, result.getDestinationZone());
        assertEquals(40, result.getRetreatThreshold());
        assertEquals(UnitType.TANK, result.getAllowedUnitType());
        assertEquals(3, result.getArrivalTurn());
        assertEquals(EntityWeightClass.WEIGHT_ASSAULT, result.getMaxWeightClass());
        assertEquals(EntityWeightClass.WEIGHT_MEDIUM, result.getMinWeightClass());
        assertEquals(2, result.getGenerationOrder());
        assertEquals(6, result.getStartingAltitude());
        assertTrue(result.getUseArtillery());
        assertTrue(result.getDeployOffboard());
        assertEquals(false, result.isSubjectToRandomRemoval());
        assertTrue(result.getSyncRetreatThreshold());
        assertEquals(SynchronizedDeploymentType.None, result.getSyncDeploymentType());
        assertEquals(List.of(2, 4, 6), result.getDeploymentZones());
        assertEquals("Some.mul", result.getFixedMul());
        assertEquals(List.of("Alpha"), result.getObjectiveLinkedForces());
        assertEquals(List.of("RAIDER,CAVALRY"), result.getRoleCollections());
    }

    @Test
    void resetReturnsToAddNewDefaults() {
        ScenarioForceTemplate source = new ScenarioForceTemplate(ForceAlignment.Opposing.ordinal(),
              ForceGenerationMethod.BVScaled.ordinal(), 1.5, List.of(2), 5, 40, UnitType.TANK);
        source.setForceName("Bravo");
        source.setSyncDeploymentType(SynchronizedDeploymentType.None);

        ForceEditorPanel panel = new ForceEditorPanel(List.of());
        panel.loadForce(source);
        assertEquals("Bravo", panel.getForceName());

        panel.reset();

        assertEquals("", panel.getForceName());
        ScenarioForceTemplate afterReset = panel.buildForceTemplate();
        assertEquals(ForceAlignment.Player.ordinal(), afterReset.getForceAlignment());
        assertEquals(1, afterReset.getGenerationOrder());
        assertEquals(50, afterReset.getRetreatThreshold());
    }

    @Test
    void getForceNameReflectsLoadedForce() {
        ScenarioForceTemplate source = new ScenarioForceTemplate(ForceAlignment.Player.ordinal(),
              ForceGenerationMethod.PlayerSupplied.ordinal(), 1.0, List.of(1), 5, 50,
              ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_MIX);
        source.setForceName("Alpha");
        source.setSyncDeploymentType(SynchronizedDeploymentType.None);

        ForceEditorPanel panel = new ForceEditorPanel(List.of());
        panel.loadForce(source);

        assertEquals("Alpha", panel.getForceName());
    }
}
