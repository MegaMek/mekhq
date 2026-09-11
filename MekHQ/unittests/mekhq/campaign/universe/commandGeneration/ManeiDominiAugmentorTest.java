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
package mekhq.campaign.universe.commandGeneration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.enums.ManeiDominiAugmentationRank;
import mekhq.campaign.personnel.enums.ManeiDominiRank;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Covers what stays on the MekHQ side of Manei Domini augmentation: which commands are Manei Domini at
 * all, and bridging MegaMek's augmentation rank to the campaign rank a {@code Person} carries.
 *
 * <p>The availability rules themselves - counts, level ceiling, superseded pairs, the neural interface
 * requirement - belong to MegaMek and are covered by {@code ManeiDominiImplantsTest}.</p>
 */
class ManeiDominiAugmentorTest {

    @Test
    void onlyTheShadowDivisionsAreManeiDomini() {
        assertTrue(ManeiDominiAugmentor.isShadowDivision("WOB.SD"));
        assertTrue(ManeiDominiAugmentor.isShadowDivision("wob.sd"), "the key is not case sensitive");
        assertFalse(ManeiDominiAugmentor.isShadowDivision("WOB"),
              "the Militia proper are not Manei Domini");
        assertFalse(ManeiDominiAugmentor.isShadowDivision("CS"));
        assertFalse(ManeiDominiAugmentor.isShadowDivision(null));
    }

    /**
     * The bridge is by name, so a rank added to one enum and not the other would silently become
     * {@code NONE} and strip the warrior's standing off the roster.
     */
    @ParameterizedTest
    @EnumSource(ManeiDominiAugmentationRank.class)
    void everyAugmentationRankHasACampaignRankOfTheSameName(
          ManeiDominiAugmentationRank augmentationRank) {
        ManeiDominiRank campaignRank = ManeiDominiAugmentor.toCampaignRank(augmentationRank);
        assertNotEquals(ManeiDominiRank.NONE, campaignRank,
              augmentationRank + " has no campaign rank of the same name");
        assertEquals(augmentationRank.name(), campaignRank.name());
    }
}
