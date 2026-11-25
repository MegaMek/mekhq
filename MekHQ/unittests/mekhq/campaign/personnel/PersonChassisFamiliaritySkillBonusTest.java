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
package mekhq.campaign.personnel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static testUtilities.MHQTestUtilities.mockCampaign;

import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.Entity;
import megamek.common.units.LandAirMek;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.familiarity.Familiarity;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Coverage for {@link Person#getChassisFamiliarityCombatBonus(Familiarity, String)}, which the person view uses to
 * show the chassis familiarity modifier against each skill.
 *
 * <p>The claim worth holding is that the modifier lands only on the skills the character uses in their assigned unit.
 * The TOE and MegaMek apply it only there, so a profile that showed it against every combat skill would disagree with
 * them as badly as one that showed it nowhere.</p>
 */
class PersonChassisFamiliaritySkillBonusTest {
    private static final String CHASSIS = "Mirage 6000";

    private Campaign campaign;
    private Person person;

    @BeforeEach
    void setUp() {
        campaign = mockCampaign();
        person = new Person("Given", "Sur", campaign, "MERC");
    }

    /** Assigns the person to a unit built around the given entity, which is eligible for chassis familiarity. */
    private void assignTo(Entity entity) {
        lenient().when(entity.isChassisFamiliarityEligible()).thenReturn(true);
        lenient().when(entity.getChassis()).thenReturn(CHASSIS);
        person.setUnit(new Unit(entity, campaign));
    }

    /**
     * Regression for the reported case: an aerospace pilot new to their fighter under Hard familiarity takes the
     * penalty on aerospace gunnery and piloting, and on nothing else.
     */
    @Test
    void testHardPenaltyAppliesOnlyToTheFighterSkills() {
        assignTo(mock(AeroSpaceFighter.class));

        Familiarity mode = Familiarity.HARD;
        assertEquals(-1, person.getChassisFamiliarityCombatBonus(mode, SkillType.S_GUN_AERO));
        assertEquals(-1, person.getChassisFamiliarityCombatBonus(mode, SkillType.S_PILOT_AERO));
        assertEquals(-1, person.getChassisFamiliarityCombatBonus(mode, SkillType.S_ARTILLERY),
              "artillery is fought with the unit's guns, so it takes the gunnery modifier");
        assertEquals(0, person.getChassisFamiliarityCombatBonus(mode, SkillType.S_PILOT_VTOL),
              "a skill the fighter is not flown with is not affected");
        assertEquals(0, person.getChassisFamiliarityCombatBonus(mode, SkillType.S_SMALL_ARMS));
    }

    /**
     * Normal familiarity at 100 improves piloting before gunnery, so the two skills must be looked up separately.
     */
    @Test
    void testNormalBonusSplitsPilotingFromGunnery() {
        assignTo(mock(AeroSpaceFighter.class));
        person.setChassisFamiliarity(CHASSIS, 100);

        Familiarity mode = Familiarity.NORMAL;
        assertEquals(1, person.getChassisFamiliarityCombatBonus(mode, SkillType.S_PILOT_AERO));
        assertEquals(0, person.getChassisFamiliarityCombatBonus(mode, SkillType.S_GUN_AERO));
    }

    /** A LAM is walked and flown, so both its MekWarrior and aerospace skills take the modifier. */
    @Test
    void testLandAirMekCoversMekAndAerospaceSkills() {
        assignTo(mock(LandAirMek.class));

        Familiarity mode = Familiarity.HARD;
        assertEquals(-1, person.getChassisFamiliarityCombatBonus(mode, SkillType.S_PILOT_MEK));
        assertEquals(-1, person.getChassisFamiliarityCombatBonus(mode, SkillType.S_GUN_MEK));
        assertEquals(-1, person.getChassisFamiliarityCombatBonus(mode, SkillType.S_PILOT_AERO));
        assertEquals(-1, person.getChassisFamiliarityCombatBonus(mode, SkillType.S_GUN_AERO));
    }

    @Test
    void testNoModifierWithoutAUnit() {
        assertEquals(0, person.getChassisFamiliarityCombatBonus(Familiarity.HARD, SkillType.S_GUN_AERO));
    }

    @Test
    void testNoModifierWhenFamiliarityIsDisabled() {
        assignTo(mock(AeroSpaceFighter.class));

        assertEquals(0, person.getChassisFamiliarityCombatBonus(Familiarity.DISABLED, SkillType.S_GUN_AERO));
        assertEquals(0, person.getChassisFamiliarityCombatBonus(null, SkillType.S_GUN_AERO),
              "a campaign with no familiarity mode set is treated as disabled");
    }
}
