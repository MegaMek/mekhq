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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * The tech's briefing before a refit: the class, the changes that set it, and the rules the player should know.
 */
class RefitBriefingTest {
    private static final boolean REFIT_KIT = false;
    private static final boolean CUSTOM_JOB = true;

    private final PartsScenario scenario = PartsScenario.create();
    private final Unit wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);

    private Refit planWolverineRefit(boolean isCustomJob) {
        return new Refit(wolverine, UnitFixture.WOLVERINE_WVR_6M.loadEntity(), isCustomJob, false, false);
    }

    @Test
    void theTechNamesTheClassAndListsTheChanges() {
        Refit refit = planWolverineRefit(REFIT_KIT);

        String briefing = RefitBriefing.describeInCharacter(scenario.getCampaign(), refit);

        assertTrue(briefing.contains("Class C (Maintenance)"), briefing);
        assertTrue(briefing.contains(wolverine.getName()), briefing);
        assertTrue(briefing.contains("<li>Class C (Maintenance): Moving Jump Jet from the Left Leg to the Left Torso"
              + "</li>"), briefing);
        assertFalse(briefing.contains("{"), "No placeholder is left unfilled: " + briefing);
    }

    @Test
    void workWithoutAKitWarnsOfTheQualityLoss() {
        String notes = RefitBriefing.describeOutOfCharacter(scenario.getCampaign(), planWolverineRefit(CUSTOM_JOB));

        assertNotNull(notes);
        assertTrue(notes.contains("lowers the unit's quality"), notes);
    }

    @Test
    void aKitRefitHasNoQualityWarning() {
        String notes = RefitBriefing.describeOutOfCharacter(scenario.getCampaign(), planWolverineRefit(REFIT_KIT));

        boolean isWarningOfQuality = (notes != null) && notes.contains("quality");
        assertFalse(isWarningOfQuality, String.valueOf(notes));
    }

    @Test
    void aFailedCheckSaysHowMuchLongerTheRefitTakes() {
        Refit refit = planWolverineRefit(REFIT_KIT);
        refit.fail(0);

        String report = RefitBriefing.describeFailedCheck(scenario.getCampaign(), refit);

        // 2,750 minutes of work again at a plain 8-hour day runs into a sixth day
        assertTrue(report.contains(wolverine.getName()), report);
        assertTrue(report.contains("6 days"), report);
        assertTrue(RefitBriefing.describeFailedCheckRules(refit).contains("time doubles"));
        assertFalse(report.contains("{"), report);
    }

    @Test
    void aFailedRefurbishmentIsLost() {
        Refit refurbishment = new Refit(wolverine, wolverine.getEntity(), false, true, false);

        String report = RefitBriefing.describeFailedCheck(scenario.getCampaign(), refurbishment);

        assertTrue(report.contains("has failed"), report);
        assertFalse(report.contains("{"), report);
    }

    @Test
    void refurbishmentIsBriefedAsAFactoryJob() {
        Refit refurbishment = new Refit(wolverine, wolverine.getEntity(), false, true, false);

        String briefing = RefitBriefing.describeInCharacter(scenario.getCampaign(), refurbishment);

        assertTrue(briefing.contains("refurbishing"), briefing);
        assertTrue(briefing.contains("Class E (Factory)"), briefing);
        assertFalse(briefing.contains("{"), briefing);
    }
}
