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
package mekhq;

import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import mekhq.campaign.Campaign;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.personnel.skills.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Utilities#applyPhenotypeSkillBonus(Person, Phenotype)}, which restores the Clan Trueborn {@code +1}
 * "Misc bonus" for personnel converted from a crew (for example captured enemy pilots) — see MekHQ issue 9833.
 */
class UtilitiesTest {
    private Campaign mockCampaign;

    @BeforeEach
    void setUp() {
        SkillType.initializeTypes();
        mockCampaign = mock(Campaign.class);
        // Person construction reads campaign.getPlayerForce().getRankSystem(); the force just needs to be non-null.
        when(mockCampaign.getPlayerForce()).thenReturn(mock(PlayerForce.class));
    }

    private Person newPersonWithSkills(String... skillNames) {
        Person person = new Person(mockCampaign, "MERC");
        for (String skillName : skillNames) {
            person.addSkill(skillName, EXP_REGULAR, 0);
        }
        return person;
    }

    @Test
    void appliesBonusToAllMappedSkillsForMekWarriorPhenotype() {
        Person person = newPersonWithSkills(SkillType.S_GUN_MEK, SkillType.S_PILOT_MEK);

        Utilities.applyPhenotypeSkillBonus(person, Phenotype.MEKWARRIOR);

        assertEquals(1, person.getSkill(SkillType.S_GUN_MEK).getBonus());
        assertEquals(1, person.getSkill(SkillType.S_PILOT_MEK).getBonus());
    }

    @Test
    void appliesBonusToAllMappedSkillsForNavalPhenotype() {
        Person person = newPersonWithSkills(SkillType.S_TECH_VESSEL,
              SkillType.S_GUN_SPACE,
              SkillType.S_PILOT_SPACE,
              SkillType.S_NAVIGATION);

        Utilities.applyPhenotypeSkillBonus(person, Phenotype.NAVAL);

        assertEquals(1, person.getSkill(SkillType.S_TECH_VESSEL).getBonus());
        assertEquals(1, person.getSkill(SkillType.S_GUN_SPACE).getBonus());
        assertEquals(1, person.getSkill(SkillType.S_PILOT_SPACE).getBonus());
        assertEquals(1, person.getSkill(SkillType.S_NAVIGATION).getBonus());
    }

    @Test
    void doesNotTouchSkillsOutsideThePhenotypeMapping() {
        Person person = newPersonWithSkills(SkillType.S_GUN_MEK, SkillType.S_SMALL_ARMS);

        Utilities.applyPhenotypeSkillBonus(person, Phenotype.MEKWARRIOR);

        assertEquals(1, person.getSkill(SkillType.S_GUN_MEK).getBonus());
        assertEquals(0, person.getSkill(SkillType.S_SMALL_ARMS).getBonus());
    }

    @Test
    void addsToAnyExistingBonusRatherThanOverwriting() {
        Person person = new Person(mockCampaign, "MERC");
        person.addSkill(SkillType.S_GUN_MEK, EXP_REGULAR, 2);

        Utilities.applyPhenotypeSkillBonus(person, Phenotype.MEKWARRIOR);

        assertEquals(3, person.getSkill(SkillType.S_GUN_MEK).getBonus());
    }

    @Test
    void ignoresMappedSkillsThePersonDoesNotHave() {
        // A MekWarrior phenotype maps to both gunnery and piloting; a person with only one should not gain the other.
        Person person = newPersonWithSkills(SkillType.S_GUN_MEK);

        Utilities.applyPhenotypeSkillBonus(person, Phenotype.MEKWARRIOR);

        assertEquals(1, person.getSkill(SkillType.S_GUN_MEK).getBonus());
        assertFalse(person.hasSkill(SkillType.S_PILOT_MEK));
    }

    @Test
    void internalPhenotypesApplyNoBonus() {
        Person person = newPersonWithSkills(SkillType.S_GUN_MEK, SkillType.S_PILOT_MEK);

        Utilities.applyPhenotypeSkillBonus(person, Phenotype.NONE);
        Utilities.applyPhenotypeSkillBonus(person, Phenotype.GENERAL);

        assertEquals(0, person.getSkill(SkillType.S_GUN_MEK).getBonus());
        assertEquals(0, person.getSkill(SkillType.S_PILOT_MEK).getBonus());
    }
}
