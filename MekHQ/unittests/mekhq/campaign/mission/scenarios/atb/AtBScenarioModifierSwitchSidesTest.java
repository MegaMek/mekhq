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
package mekhq.campaign.mission.scenarios.atb;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.scenarios.AtBDynamicScenario;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.mission.scenarios.atb.AtBScenarioModifier.EventTiming;
import org.junit.jupiter.api.Test;

/**
 * Guards the {@code switchSides} check in {@link AtBScenarioModifier#processModifier}: only an explicit {@code true}
 * switches sides. The scenario modifier editor used to save an unchecked box as {@code false}, which the old
 * presence-only check treated as "switch".
 */
class AtBScenarioModifierSwitchSidesTest {

    private static AtBScenarioModifier alliedModifier(Boolean switchSides) {
        AtBScenarioModifier modifier = new AtBScenarioModifier();
        modifier.setEventTiming(EventTiming.PreForceGeneration);
        modifier.setEventRecipient(ForceAlignment.Allied);
        modifier.setSwitchSides(switchSides);
        return modifier;
    }

    /** Switching sides walks the scenario's bot forces, so whether it ran shows in whether the bots were read. */
    private static AtBDynamicScenario process(AtBScenarioModifier modifier) {
        AtBDynamicScenario scenario = mock(AtBDynamicScenario.class);
        modifier.processModifier(scenario, mock(Campaign.class), EventTiming.PreForceGeneration);
        return scenario;
    }

    @Test
    void explicitFalseDoesNotSwitchSides() {
        verify(process(alliedModifier(false)), never()).getNumBots();
    }

    @Test
    void nullDoesNotSwitchSides() {
        verify(process(alliedModifier(null)), never()).getNumBots();
    }

    @Test
    void trueSwitchesSides() {
        verify(process(alliedModifier(true))).getNumBots();
    }
}
