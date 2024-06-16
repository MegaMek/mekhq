/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.personnel.education;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.education.AcademyType;
import mekhq.campaign.personnel.enums.education.EducationLevel;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class AcademyTests {
    @Test
    void testSetName() {
        Academy academy = new Academy();
        academy.setName("Military Academy");
        assertEquals("Military Academy", academy.getName());
    }

    @Test
    void testSetTuition() {
        Academy academy = new Academy();
        academy.setTuition(5000);
        assertEquals(5000, academy.getTuition());
    }

    @Test
    void testIsMilitary() {
        Academy academy = new Academy();
        academy.setIsMilitary(true);
        assertTrue(academy.isMilitary());
    }

    @Test
    void testAcademyCreationAllFields() {
        Academy academy = new Academy("MechWarrior", "MekWarrior Academy", AcademyType.COLLEGE, true,
                false, "Colonel", true,
                "Top level MechWarrior Training", 20, true,
                "FWL", Arrays.asList("Sol", "Terra"), false, 3045,
                3089, 3099, 2000, 365, 10,
                EducationLevel.EARLY_CHILDHOOD, EducationLevel.DOCTORATE, 18, 35, Arrays.asList("MechWarrior", "Leadership"),
                Arrays.asList("Combat", "Strategy"), Arrays.asList(3050, 3055), 5, 101);

        assertEquals("MekWarrior Academy", academy.getName());
        assertEquals(AcademyType.COLLEGE, academy.getType());
        assertTrue(academy.isMilitary());
        assertEquals("Colonel", academy.getPromotion());
        assertEquals(20, academy.getFactionDiscount());
        assertEquals("FWL", academy.getFaction());
        assertEquals(2000, academy.getTuition());
        assertEquals(Integer.valueOf(3089), academy.getDestructionYear());
    }

    @Test void testCompareToSameID() {
        Academy academy1 = new Academy();
        Academy academy2 = new Academy();
        academy1.setId(100);
        academy2.setId(100);
        assertEquals(0, academy1.compareTo(academy2));
    }

    @Test
    void testCompareToDifferentID() {
        Academy academy1 = new Academy();
        Academy academy2 = new Academy();
        academy1.setId(100);
        academy2.setId(200);
        assertTrue(academy1.compareTo(academy2) < 0);
    }

    @Test void testGetTuitionAdjustedLowEducationLevel() {
        Academy academy = new Academy();
        academy.setTuition(1000);
        academy.setEducationLevelMin(EducationLevel.EARLY_CHILDHOOD);
        academy.setEducationLevelMax(EducationLevel.HIGH_SCHOOL);
        Person person = Mockito.mock(Person.class);
        when(person.getEduHighestEducation()).thenReturn(EducationLevel.HIGH_SCHOOL);
        assertEquals(1000, academy.getTuitionAdjusted(person));
    }

    @Test void testGetTuitionAdjustedHighEducationLevel() {
        Academy academy = new Academy();
        academy.setTuition(1000);
        academy.setEducationLevelMin(EducationLevel.HIGH_SCHOOL);
        academy.setEducationLevelMax(EducationLevel.POST_GRADUATE);
        Person person = Mockito.mock(Person.class);
        when(person.getEduHighestEducation()).thenReturn(EducationLevel.COLLEGE);
        assertEquals(3000, academy.getTuitionAdjusted(person));
    }

    @Test void testIsQualifiedTrue() {
        Academy academy = new Academy();
        academy.setEducationLevelMin(EducationLevel.COLLEGE);
        Person person = Mockito.mock(Person.class);
        when(person.getEduHighestEducation()).thenReturn(EducationLevel.POST_GRADUATE);
        assertTrue(academy.isQualified(person));
    }

    @Test
    void testIsQualifiedFalse() {
        Academy academy = new Academy();
        academy.setEducationLevelMin(EducationLevel.COLLEGE);
        Person person = Mockito.mock(Person.class);
        when(person.getEduHighestEducation()).thenReturn(EducationLevel.EARLY_CHILDHOOD);
        assertFalse(academy.isQualified(person));
    }

    @Test void testGetFactionDiscountAdjustedNotPresentInLocationSystems() {
        Academy academy = new Academy();
        academy.setLocationSystems(Arrays.asList("Sol"));
        academy.setFactionDiscount(10);
        Person person = Mockito.mock(Person.class);
        Campaign campaign = Mockito.mock(Campaign.class);
        PlanetarySystem system = Mockito.mock(PlanetarySystem.class);
        when(campaign.getSystemById("Sol")).thenReturn(system);
        when(system.getFactions(Mockito.any())).thenReturn(Arrays.asList("Lyr"));
        when(person.getOriginFaction()).thenReturn(new Faction("FWL", ""));
        assertEquals(1.0, academy.getFactionDiscountAdjusted(campaign, person));
    }

    @Test
    public void testSkillParser_ValidSkill() {
        assertEquals(SkillType.S_PILOT_MECH, Academy.skillParser("piloting/mech"));
        assertEquals(SkillType.S_GUN_MECH, Academy.skillParser("gunnery/mech"));
        assertEquals(SkillType.S_GUN_MECH, Academy.skillParser("gunnery/mech"));
        assertEquals(SkillType.S_PILOT_AERO, Academy.skillParser("piloting/aerospace"));
        assertEquals(SkillType.S_GUN_AERO, Academy.skillParser("gunnery/aerospace"));
        assertEquals(SkillType.S_PILOT_GVEE, Academy.skillParser("piloting/ground vehicle"));
        assertEquals(SkillType.S_PILOT_VTOL, Academy.skillParser("piloting/vtol"));
        assertEquals(SkillType.S_PILOT_NVEE, Academy.skillParser("piloting/naval"));
        assertEquals(SkillType.S_GUN_VEE, Academy.skillParser("gunnery/vehicle"));
        assertEquals(SkillType.S_PILOT_JET, Academy.skillParser("piloting/aircraft"));
        assertEquals(SkillType.S_GUN_JET, Academy.skillParser("gunnery/aircraft"));
        assertEquals(SkillType.S_PILOT_SPACE, Academy.skillParser("piloting/spacecraft"));
        assertEquals(SkillType.S_GUN_SPACE, Academy.skillParser("gunnery/spacecraft"));
        assertEquals(SkillType.S_ARTILLERY, Academy.skillParser("artillery"));
        assertEquals(SkillType.S_GUN_BA, Academy.skillParser("gunnery/battlesuit"));
        assertEquals(SkillType.S_GUN_PROTO, Academy.skillParser("gunnery/protomech"));
        assertEquals(SkillType.S_SMALL_ARMS, Academy.skillParser("small arms"));
        assertEquals(SkillType.S_ANTI_MECH, Academy.skillParser("anti-mech"));
        assertEquals(SkillType.S_TECH_MECH, Academy.skillParser("tech/mech"));
        assertEquals(SkillType.S_TECH_MECHANIC, Academy.skillParser("tech/mechanic"));
        assertEquals(SkillType.S_TECH_AERO, Academy.skillParser("tech/aero"));
        assertEquals(SkillType.S_TECH_BA, Academy.skillParser("tech/ba"));
        assertEquals(SkillType.S_TECH_VESSEL, Academy.skillParser("tech/vessel"));
        assertEquals(SkillType.S_ASTECH, Academy.skillParser("astech"));
        assertEquals(SkillType.S_DOCTOR, Academy.skillParser("doctor"));
        assertEquals(SkillType.S_MEDTECH, Academy.skillParser("medtech"));
        assertEquals(SkillType.S_NAV, Academy.skillParser("hyperspace navigation"));
        assertEquals(SkillType.S_ADMIN, Academy.skillParser("administration"));
        assertEquals(SkillType.S_TACTICS, Academy.skillParser("tactics"));
        assertEquals(SkillType.S_STRATEGY, Academy.skillParser("strategy"));
        assertEquals(SkillType.S_NEG, Academy.skillParser("negotiation"));
        assertEquals(SkillType.S_LEADER, Academy.skillParser("leadership"));
        assertEquals(SkillType.S_SCROUNGE, Academy.skillParser("scrounge"));
    }

    @Test
    public void testSkillParser_InvalidSkill() {
        assertThrows(IllegalStateException.class, () -> Academy.skillParser("invalid_skill"));
    }
}
