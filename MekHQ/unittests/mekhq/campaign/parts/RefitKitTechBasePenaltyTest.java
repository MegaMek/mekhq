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

import megamek.common.equipment.EquipmentType;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A refit kit takes the Clan and Inner Sphere acquisition penalties from the parts it holds, the same penalty each
 * part would get if bought on its own (issue #10544). The kit used to check its own tech base, which is always "all",
 * so it only ever took the lower of the two penalties.
 */
class RefitKitTechBasePenaltyTest {
    private PartsScenario scenario;
    private Campaign campaign;

    @BeforeAll
    static void initializeSkills() {
        SkillType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
    }

    /** A kit that refits a Wolverine WVR-6R with one more laser in the centre torso. */
    private Refit planAKitAddingALaser(String laserName, boolean isMixedTech) {
        Unit wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
        Entity design = UnitFixture.WOLVERINE_WVR_6R.loadEntity();
        design.setMixedTech(isMixedTech);
        try {
            design.addEquipment(EquipmentType.get(laserName), Mek.LOC_CENTER_TORSO);
        } catch (Exception exception) {
            throw new IllegalStateException("The WVR-6R has room for one more laser", exception);
        }
        return new Refit(wolverine, design, false, false, false);
    }

    /** How much the Clan and Inner Sphere penalties add to the kit's acquisition target. */
    private int techPenaltyOf(Refit refitKit, int clanPenalty, int innerSpherePenalty) {
        setPenalties(0, 0);
        int targetWithoutPenalties = refitKit.getAllAcquisitionMods().getValue();
        setPenalties(clanPenalty, innerSpherePenalty);
        return refitKit.getAllAcquisitionMods().getValue() - targetWithoutPenalties;
    }

    private void setPenalties(int clanPenalty, int innerSpherePenalty) {
        campaign.getCampaignOptions().set(CampaignOption.CLAN_ACQUISITION_PENALTY, clanPenalty);
        campaign.getCampaignOptions().set(CampaignOption.IS_ACQUISITION_PENALTY, innerSpherePenalty);
    }

    @Test
    void aKitHoldingAClanPartTakesTheClanPenalty() {
        Refit refitKit = planAKitAddingALaser("CLERMediumLaser", true);

        assertEquals(3, techPenaltyOf(refitKit, 3, 0));
    }

    @Test
    void aKitHoldingAClanPartTakesTheClanPenaltyEvenWhenTheInnerSpherePenaltyIsLower() {
        Refit refitKit = planAKitAddingALaser("CLERMediumLaser", true);

        assertEquals(3, techPenaltyOf(refitKit, 3, 1));
    }

    @Test
    void aKitOfInnerSpherePartsIgnoresTheClanPenalty() {
        Refit refitKit = planAKitAddingALaser("ISMediumLaser", false);

        assertEquals(0, techPenaltyOf(refitKit, 3, 0));
    }

    @Test
    void aKitOfInnerSpherePartsTakesTheInnerSpherePenalty() {
        Refit refitKit = planAKitAddingALaser("ISMediumLaser", false);

        assertEquals(2, techPenaltyOf(refitKit, 0, 2));
    }
}
