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

import static mekhq.campaign.personnel.skills.SkillType.S_TECH_AERO;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_BA;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_MECHANICAL;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_MEK;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_VEHICLE;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_VESSEL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.HandheldWeapon;
import megamek.common.units.*;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IPartWork;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests the single source of truth mapping a unit type to the whole-unit "global" technician skill used by maintenance
 * and refit routing, and the campaign option that forces part repairs onto that same global skill.
 */
class PersonTechSkillRoutingTest {

    @BeforeAll
    static void beforeAll() {
        SkillType.initializeTypes();
    }

    private static Unit unitOf(Entity entity) {
        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(entity);
        return unit;
    }

    @Test
    void meksProtoMeksAndHandheldWeaponsAreMaintainedByMekTechs() {
        assertEquals(S_TECH_MEK, Person.getGlobalTechSkillNameFor(unitOf(new BipedMek())));
        assertEquals(S_TECH_MEK, Person.getGlobalTechSkillNameFor(unitOf(new ProtoMek())));
        assertEquals(S_TECH_MEK, Person.getGlobalTechSkillNameFor(unitOf(new HandheldWeapon())));
    }

    @Test
    void battleArmorIsMaintainedByBattleArmorTechs() {
        assertEquals(S_TECH_BA, Person.getGlobalTechSkillNameFor(unitOf(new BattleArmor())));
    }

    @Test
    void aerospaceFightersAreMaintainedByAeroTechs() {
        assertEquals(S_TECH_AERO, Person.getGlobalTechSkillNameFor(unitOf(new AeroSpaceFighter())));
    }

    @Test
    void conventionalFightersAndSmallCraftAreMaintainedByAeroTechs() {
        assertEquals(S_TECH_AERO, Person.getGlobalTechSkillNameFor(unitOf(new ConvFighter())));
        assertEquals(S_TECH_AERO, Person.getGlobalTechSkillNameFor(unitOf(new SmallCraft())));
    }

    @Test
    void largeCraftAreMaintainedByVesselCrews() {
        assertEquals(S_TECH_VESSEL, Person.getGlobalTechSkillNameFor(unitOf(new Dropship())));
        assertEquals(S_TECH_VESSEL, Person.getGlobalTechSkillNameFor(unitOf(new Jumpship())));
    }

    @Test
    void tanksAndInfantryAreMaintainedByMechanics() {
        assertEquals(S_TECH_VEHICLE, Person.getGlobalTechSkillNameFor(unitOf(new Tank())));
        assertEquals(S_TECH_VEHICLE, Person.getGlobalTechSkillNameFor(unitOf(new ConvInfantry())));
    }

    @Test
    void aNullUnitOrEntityMapsToNoSkill() {
        assertNull(Person.getGlobalTechSkillNameFor(null));
        assertNull(Person.getGlobalTechSkillNameFor(unitOf(null)));
    }

    /** A Mek tech who also holds the granular Tech/Mechanical specialist skill. */
    private static Person mekTechWithSpecialist() {
        Person person = new Person("Test", "Tech", null, "MERC");
        person.addSkill(S_TECH_MEK, 5, 0);
        person.addSkill(S_TECH_MECHANICAL, 5, 0);
        return person;
    }

    /** A Mek unit whose campaign reports the given "use global tech skills only" setting. */
    private static Unit mekUnitWithGlobalOnly(boolean globalOnly) {
        Campaign campaign = mock(Campaign.class);
        CampaignOptions options = new CampaignOptions();
        options.set(CampaignOption.USE_GLOBAL_TECH_SKILLS_ONLY, globalOnly);
        when(campaign.getCampaignOptions()).thenReturn(options);

        Unit unit = unitOf(new BipedMek());
        when(unit.getCampaign()).thenReturn(campaign);
        return unit;
    }

    /** A part on the given unit that accepts only the granular Tech/Mechanical specialist skill. */
    private static IPartWork mechanicalPartOn(Unit unit) {
        IPartWork part = mock(IPartWork.class);
        when(part.getUnit()).thenReturn(unit);
        when(part.isRightTechType(S_TECH_MECHANICAL)).thenReturn(true);
        return part;
    }

    @Test
    void byDefaultPartRepairUsesTheGranularSpecialistSkill() {
        Person tech = mekTechWithSpecialist();
        IPartWork part = mechanicalPartOn(mekUnitWithGlobalOnly(false));

        Skill skill = tech.getSkillForWorkingOn(part);
        assertNotNull(skill);
        assertEquals(S_TECH_MECHANICAL, skill.getType().getName(),
              "with the option off, the granular specialist skill the part accepts should be used");
    }

    @Test
    void withGlobalTechSkillsOnlyPartRepairUsesTheGlobalSkill() {
        Person tech = mekTechWithSpecialist();
        IPartWork part = mechanicalPartOn(mekUnitWithGlobalOnly(true));

        Skill skill = tech.getSkillForWorkingOn(part);
        assertNotNull(skill);
        assertEquals(S_TECH_MEK, skill.getType().getName(),
              "with the option on, the whole-unit global tech skill should be used instead of the specialist skill");
    }
}
