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
package mekhq.campaign.universe.commandGeneration.ratgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.client.ratgenerator.ForceDescriptor;
import mekhq.campaign.universe.commandGeneration.CommandGenerationOptions;
import org.junit.jupiter.api.Test;

/**
 * Verifies how the calibre of a generated force shifts its warriors' Bloodname chances.
 *
 * <p>The roll is 2d6 against a target, so a negative modifier makes a Bloodname likelier. Only the
 * top two experience tiers get one, on the reasoning that the Clans post their Bloodnamed warriors to
 * their better formations.</p>
 */
class BloodnameTargetModifierTest {

    private static CommandGenerationOptions optionsWithExperience(Integer experience) {
        CommandGenerationOptions options = mock(CommandGenerationOptions.class);
        ForceDescriptorSnapshot snapshot = mock(ForceDescriptorSnapshot.class);
        when(snapshot.getExperience()).thenReturn(experience);
        when(options.getForceDescriptorSnapshot()).thenReturn(snapshot);
        return options;
    }

    @Test
    void greenAndRegularForcesRollExactlyAsBefore() {
        assertEquals(0, CommandGenerator.bloodnameTargetModifier(
              optionsWithExperience(ForceDescriptor.EXP_GREEN)));
        assertEquals(0, CommandGenerator.bloodnameTargetModifier(
              optionsWithExperience(ForceDescriptor.EXP_REGULAR)));
    }

    @Test
    void veteranAndEliteForcesCarryMoreBloodnamedWarriors() {
        int veteran = CommandGenerator.bloodnameTargetModifier(
              optionsWithExperience(ForceDescriptor.EXP_VETERAN));
        int elite = CommandGenerator.bloodnameTargetModifier(
              optionsWithExperience(ForceDescriptor.EXP_ELITE));

        assertTrue(veteran < 0, "a veteran force should lower the target, got " + veteran);
        assertTrue(elite < veteran, "an elite force should beat a veteran one, got " + elite);
    }

    @Test
    void theShiftStaysModest() {
        // The warrior's own skills already weigh on the same roll, so this must not swamp them.
        assertTrue(CommandGenerator.bloodnameTargetModifier(
              optionsWithExperience(ForceDescriptor.EXP_ELITE)) >= -2,
              "the calibre shift should stay a thumb on the scale");
    }

    @Test
    void anUnsetExperienceLevelShiftsNothing() {
        // The experience level can be left to chance, in which case there is no force calibre to read.
        assertEquals(0, CommandGenerator.bloodnameTargetModifier(optionsWithExperience(null)));
    }

    @Test
    void aMissingSnapshotShiftsNothing() {
        CommandGenerationOptions options = mock(CommandGenerationOptions.class);
        when(options.getForceDescriptorSnapshot()).thenReturn(null);
        assertEquals(0, CommandGenerator.bloodnameTargetModifier(options));
    }
}
