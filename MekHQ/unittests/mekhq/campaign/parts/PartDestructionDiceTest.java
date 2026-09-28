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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import megamek.common.units.Aero;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;

/**
 * Checks that the campaign's {@link Dice} decides whether a damaged part is destroyed after battle. A part is
 * destroyed when the 2d6 roll is below the Destroy Part target from the campaign options.
 */
class PartDestructionDiceTest {
    private static final int DESTROY_PART_TARGET = 10;

    /** Dice that always roll the same total, and count how often they were asked. */
    private static final class FixedDice implements Dice {
        private final int fixedRoll;
        private int numberOfD6Rolls;

        private FixedDice(int fixedRoll) {
            this.fixedRoll = fixedRoll;
        }

        @Override
        public int d6(int numberOfDice) {
            numberOfD6Rolls++;
            return fixedRoll;
        }

        @Override
        public int randomInt(int maximumValue) {
            return 0;
        }
    }

    @Test
    void rollBelowTargetDestroysTheDamagedPart() {
        FixedDice fixedDice = new FixedDice(DESTROY_PART_TARGET - 1);
        AeroSensor sensor = createSensorWithOneNewHit(fixedDice);

        sensor.updateConditionFromEntity(true);

        assertEquals(1, fixedDice.numberOfD6Rolls);
        verify(sensor).remove(false);
    }

    @Test
    void rollAtTargetLeavesTheDamagedPartInPlace() {
        FixedDice fixedDice = new FixedDice(DESTROY_PART_TARGET);
        AeroSensor sensor = createSensorWithOneNewHit(fixedDice);

        sensor.updateConditionFromEntity(true);

        assertEquals(1, fixedDice.numberOfD6Rolls);
        assertEquals(1, sensor.getHits());
        verify(sensor, never()).remove(anyBoolean());
    }

    @Test
    void noRollIsMadeWhenNotCheckingForDestruction() {
        FixedDice fixedDice = new FixedDice(DESTROY_PART_TARGET - 1);
        AeroSensor sensor = createSensorWithOneNewHit(fixedDice);

        sensor.updateConditionFromEntity(false);

        assertEquals(0, fixedDice.numberOfD6Rolls);
        verify(sensor, never()).remove(anyBoolean());
    }

    /**
     * Builds an undamaged aerospace sensor on a fighter whose sensors have just taken one hit, in a campaign that
     * rolls with the given dice.
     */
    private static AeroSensor createSensorWithOneNewHit(Dice dice) {
        Campaign campaign = mockCampaign();
        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaignOptions.get(CampaignOption.USE_AERO_SYSTEM_HITS)).thenReturn(false);
        when(campaignOptions.get(CampaignOption.DESTROY_PART_TARGET)).thenReturn(DESTROY_PART_TARGET);
        when(campaign.getDice()).thenReturn(dice);

        Aero fighter = mock(Aero.class);
        when(fighter.getSensorHits()).thenReturn(1);
        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(fighter);

        AeroSensor sensor = spy(new AeroSensor(50, false, campaign));
        doNothing().when(sensor).remove(anyBoolean());
        sensor.setUnit(unit);
        return sensor;
    }
}
