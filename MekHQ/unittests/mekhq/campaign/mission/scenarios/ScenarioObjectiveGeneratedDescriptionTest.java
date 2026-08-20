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
package mekhq.campaign.mission.scenarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.mission.scenarios.ObjectiveEffect.EffectScalingType;
import mekhq.campaign.mission.scenarios.ObjectiveEffect.ObjectiveEffectType;
import mekhq.campaign.mission.scenarios.ScenarioObjective.ObjectiveCriterion;
import org.junit.jupiter.api.Test;

/**
 * Covers {@link ScenarioObjective#getGeneratedDescription()} and the override-aware
 * {@link ScenarioObjective#getDescription()}: standard criteria derive their text from the structured fields, an
 * authored description overrides the generated text, and {@code Custom} falls back to the authored text.
 */
class ScenarioObjectiveGeneratedDescriptionTest {

    private static ObjectiveEffect effect(ObjectiveEffectType type, int howMuch) {
        ObjectiveEffect e = new ObjectiveEffect();
        e.effectType = type;
        e.effectScaling = EffectScalingType.Fixed;
        e.howMuch = howMuch;
        return e;
    }

    /** The Convoy Ambush objective, rebuilt from its structured fields. */
    private static ScenarioObjective convoyAmbushObjective() {
        ScenarioObjective objective = new ScenarioObjective();
        objective.setObjectiveCriterion(ObjectiveCriterion.PreventReachMapEdge);
        objective.setDescription(""); // no authored override -> generated
        objective.setPercentage(50);
        objective.addForce("Convoy");
        objective.addSuccessEffect(effect(ObjectiveEffectType.ScenarioVictory, 1));
        objective.addFailureEffect(effect(ObjectiveEffectType.ScenarioDefeat, 1));
        return objective;
    }

    @Test
    void generatesPreventReachMapEdgeWithEffects() {
        String generated = convoyAmbushObjective().getGeneratedDescription();
        assertEquals("Prevent at least 50% of the marked force(s) from reaching the designated map edge. "
                           + "(+1 Scenario VP if completed; -1 Scenario VP if failed)", generated);
    }

    @Test
    void blankDescriptionFallsBackToGenerated() {
        ScenarioObjective objective = convoyAmbushObjective();
        assertEquals(objective.getGeneratedDescription(), objective.getDescription(),
              "a blank description should return the generated text");
    }

    @Test
    void authoredDescriptionOverridesGenerated() {
        ScenarioObjective objective = convoyAmbushObjective();
        objective.setDescription("Hold the line at the pass.");
        assertEquals("Hold the line at the pass.", objective.getDescription());
    }

    @Test
    void destroyUsesFixedCountWhenSet() {
        ScenarioObjective objective = new ScenarioObjective();
        objective.setObjectiveCriterion(ObjectiveCriterion.Destroy);
        objective.setDescription("");
        objective.setFixedAmount(1);
        assertEquals("Destroy at least 1 unit of the marked force(s).", objective.getGeneratedDescription());
    }

    @Test
    void customFallsBackToAuthoredText() {
        ScenarioObjective objective = new ScenarioObjective();
        objective.setObjectiveCriterion(ObjectiveCriterion.Custom);
        objective.setDescription("Do the unusual thing.");
        assertEquals("Do the unusual thing.", objective.getGeneratedDescription());
        assertEquals("Do the unusual thing.", objective.getDescription());
    }

    @Test
    void samplesForEyeballing() {
        for (ObjectiveCriterion criterion : ObjectiveCriterion.values()) {
            if (criterion == ObjectiveCriterion.Custom) {
                continue;
            }
            ScenarioObjective objective = new ScenarioObjective();
            objective.setObjectiveCriterion(criterion);
            objective.setDescription("");
            objective.setPercentage(75);
            objective.addSuccessEffect(effect(ObjectiveEffectType.ScenarioVictory, 1));
            String generated = objective.getGeneratedDescription();
            assertTrue(generated != null && !generated.isBlank());
            System.out.println(criterion + " -> " + generated);
        }
    }
}
