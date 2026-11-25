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
package mekhq.campaign.personnel.procreation;

import static mekhq.campaign.personnel.PersonnelOptions.UNOFFICIAL_DOBROWSKI_SYNDROME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import megamek.common.enums.Gender;
import megamek.common.equipment.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.BabySurnameStyle;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.education.EducationLevel;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.randomEvents.prisoners.PrisonerStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

/**
 * Exercises children joining a family later in life (paternity and adoption), multiple pregnancies, and births brought
 * forward, against a real test campaign rather than mocks.
 *
 * <p>Every random procreation die is set to one side, so each roll succeeds, and the birth announcement dialog is
 * turned off so births can run headless.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
class ChildArrivalIntegrationTest {
    private Campaign campaign;
    private ForceHumanResources humanResources;
    private LocalDate today;
    private AbstractProcreation procreation;

    @BeforeAll
    static void globalSetup() {
        EquipmentType.initializeTypes();
        SkillType.initializeTypes();
    }

    @BeforeEach
    void setup() {
        campaign = MHQTestUtilities.getTestCampaign();
        humanResources = campaign.getPlayerForce().getHumanResources();
        today = campaign.getLocalDate();

        final CampaignOptions options = campaign.getCampaignOptions();
        options.set(CampaignOption.SHOW_LIFE_EVENT_DIALOG_BIRTHS, false);
        options.set(CampaignOption.LOG_PROCREATION, true);
        options.set(CampaignOption.BABY_SURNAME_STYLE, BabySurnameStyle.MOTHERS);
        options.set(CampaignOption.USE_RELATIONSHIPLESS_RANDOM_PROCREATION, true);
        options.set(CampaignOption.RANDOM_PROCREATION_RELATIONSHIP_DICE_SIZE, 1);
        options.set(CampaignOption.RANDOM_PROCREATION_RELATIONSHIPLESS_DICE_SIZE, 1);
        // A randomly generated origin could make someone clan personnel, so don't let that block anything
        options.set(CampaignOption.USE_CLAN_PERSONNEL_PROCREATION, true);
        options.set(CampaignOption.USE_RANDOM_CLAN_PERSONNEL_PROCREATION, true);

        // Built after the options are set, as it reads them on construction
        procreation = new RandomProcreation(options);
    }

    /**
     * Creates and recruits an adult who wants children, without simulating any relationship history.
     */
    private Person adult(final Gender gender, final int age, final String surname) {
        final Person person = humanResources.newPerson(campaign, PersonnelRole.ADMINISTRATOR, PersonnelRole.NONE);
        person.setGender(gender);
        person.setDateOfBirth(today.minusYears(age));
        person.setSurname(surname);
        person.setWantsChildren(true);
        humanResources.recruitPerson(campaign, person, PrisonerStatus.FREE, true, false, true, true);
        return person;
    }

    private static void marry(final Person first, final Person second) {
        first.getGenealogy().setSpouse(second);
        second.getGenealogy().setSpouse(first);
    }

    //region addChild

    @Test
    void biologicalChildOfASingleParentJoinsTheForceLinkedToThem() {
        final Person father = adult(Gender.MALE, 30, "Kerensky");
        final LocalDate dateOfBirth = today.minusYears(4).minusDays(10);

        final Person child = procreation.addChild(campaign, today, father, null, dateOfBirth,
              ChildArrivalType.BIOLOGICAL);

        assertNotNull(humanResources.getPerson(child.getId()), "the child is recruited into the force");
        assertEquals(dateOfBirth, child.getDateOfBirth());
        assertEquals(List.of(father), child.getGenealogy().getParents());
        assertTrue(father.getGenealogy().getChildren().contains(child));
        assertTrue(child.getPrimaryRole().isDependent(), "children can't have jobs");
        assertEquals(EducationLevel.EARLY_CHILDHOOD, child.getEduHighestEducation());
        assertEquals(0, child.getSkills().size(), "young children have no skills");
        assertEquals("Kerensky", child.getSurname(), "the sole parent's surname is used, whatever their gender");
        assertTrue(father.getPersonalLog().stream().anyMatch(entry -> entry.getDesc().contains(child.getFullName())),
              "the arrival is logged for the parent");
    }

    @Test
    void adoptedChildIsLinkedToBothParentsAndTakesTheMothersSurname() {
        final Person father = adult(Gender.MALE, 35, "Davion");
        final Person mother = adult(Gender.FEMALE, 33, "Steiner");

        // The father is the 'parent' the child joins, but the surname style still uses the mother's name
        final Person child = procreation.addChild(campaign, today, father, mother, today.minusYears(6),
              ChildArrivalType.ADOPTION);

        assertEquals(2, child.getGenealogy().getParents().size());
        assertTrue(child.getGenealogy().getParents().contains(father));
        assertTrue(child.getGenealogy().getParents().contains(mother));
        assertTrue(father.getGenealogy().getChildren().contains(child));
        assertTrue(mother.getGenealogy().getChildren().contains(child));
        assertEquals("Steiner", child.getSurname());
    }

    @Test
    void onlyBiologicalChildrenInheritHereditaryConditions() {
        final Person parent = adult(Gender.MALE, 40, "Liao");
        parent.getOptions().getOption(UNOFFICIAL_DOBROWSKI_SYNDROME).setValue(true);

        final Person biologicalChild = procreation.addChild(campaign, today, parent, null, today.minusYears(3),
              ChildArrivalType.BIOLOGICAL);
        final Person adoptedChild = procreation.addChild(campaign, today, parent, null, today.minusYears(3),
              ChildArrivalType.ADOPTION);

        assertTrue(biologicalChild.getOptions().booleanOption(UNOFFICIAL_DOBROWSKI_SYNDROME));
        assertFalse(adoptedChild.getOptions().booleanOption(UNOFFICIAL_DOBROWSKI_SYNDROME));
    }

    @Test
    void prepareNonInfantChildTrimsAnAdultGeneratedPersonToTheirRealAge() {
        // Generated people start as adults with skills, as background and arriving children do
        final Person child = humanResources.newPerson(campaign, PersonnelRole.ADMINISTRATOR, PersonnelRole.NONE);
        child.setDateOfBirth(today.minusYears(10));

        ForceHumanResources.prepareNonInfantChild(campaign, child);

        assertEquals(0, child.getSkills().size());
        assertEquals(EducationLevel.EARLY_CHILDHOOD, child.getEduHighestEducation());
        assertEquals("", child.getPreNominal());
        assertEquals("", child.getPostNominal());
    }
    //endregion addChild

    //region Random Paternity

    @Test
    void unmarriedManLearnsOfAChildNoOlderThanTheMaximumAge() {
        campaign.getCampaignOptions().set(CampaignOption.USE_RANDOM_PATERNITY, true);
        campaign.getCampaignOptions().set(CampaignOption.RANDOM_PATERNITY_MAXIMUM_CHILD_AGE, 3);
        final Person father = adult(Gender.MALE, 30, "Kurita");

        procreation.processNewWeek(campaign, today, father);

        assertEquals(1, father.getGenealogy().getChildren().size());
        final Person child = father.getGenealogy().getChildren().get(0);
        assertEquals(List.of(father), child.getGenealogy().getParents(), "the mother is unknown");
        assertTrue(child.getAge(today) <= 3);
        assertFalse(child.getDateOfBirth().isAfter(today));
    }

    @Test
    void paternityNeverGivesAChildBornBeforeTheFatherCouldHaveOne() {
        campaign.getCampaignOptions().set(CampaignOption.USE_RANDOM_PATERNITY, true);
        campaign.getCampaignOptions().set(CampaignOption.RANDOM_PATERNITY_MAXIMUM_CHILD_AGE,
              AbstractProcreation.MAXIMUM_ARRIVING_CHILD_AGE);
        // Twenty, so any child is at most about a year old despite the high maximum age
        final Person father = adult(Gender.MALE, 20, "Marik");

        procreation.processRandomPaternityCheck(campaign, today, father);

        assertEquals(1, father.getGenealogy().getChildren().size());
        final Person child = father.getGenealogy().getChildren().get(0);
        assertFalse(child.getDateOfBirth().isBefore(AbstractProcreation.getEarliestChildDateOfBirth(father)));
    }

    @Test
    void paternityNeedsTheOptionAndAnUnmarriedMan() {
        final Person single = adult(Gender.MALE, 30, "Steiner");
        final Person husband = adult(Gender.MALE, 30, "Davion");
        marry(husband, adult(Gender.FEMALE, 30, "Davion"));

        procreation.processRandomPaternityCheck(campaign, today, single);
        assertTrue(single.getGenealogy().getChildren().isEmpty(), "the option is off by default");

        campaign.getCampaignOptions().set(CampaignOption.USE_RANDOM_PATERNITY, true);
        procreation.processRandomPaternityCheck(campaign, today, husband);
        assertTrue(husband.getGenealogy().getChildren().isEmpty(), "married men have children through pregnancy");
    }
    //endregion Random Paternity

    //region Random Adoption

    @Test
    void maleCoupleAdoptsOneChildAWeekBetweenThem() {
        campaign.getCampaignOptions().set(CampaignOption.USE_RANDOM_SAME_SEX_ADOPTION, true);
        campaign.getCampaignOptions().set(CampaignOption.RANDOM_ADOPTION_MAXIMUM_CHILD_AGE, 10);
        final Person first = adult(Gender.MALE, 32, "Hasek");
        final Person second = adult(Gender.MALE, 34, "Hasek");
        marry(first, second);

        procreation.processNewWeek(campaign, today, first);
        procreation.processNewWeek(campaign, today, second);

        assertEquals(1, first.getGenealogy().getChildren().size(), "only one spouse rolls each week");
        final Person child = first.getGenealogy().getChildren().get(0);
        assertTrue(child.getGenealogy().getParents().contains(first));
        assertTrue(child.getGenealogy().getParents().contains(second));
        assertTrue(child.getAge(today) <= 10);
    }

    @Test
    void femaleCoupleAdoptsInsteadOfConceiving() {
        campaign.getCampaignOptions().set(CampaignOption.USE_RANDOM_SAME_SEX_ADOPTION, true);
        final Person first = adult(Gender.FEMALE, 28, "Cameron");
        final Person second = adult(Gender.FEMALE, 29, "Cameron");
        marry(first, second);

        procreation.processNewWeek(campaign, today, first);
        procreation.processNewWeek(campaign, today, second);

        assertFalse(first.isPregnant());
        assertFalse(second.isPregnant());
        assertEquals(1, first.getGenealogy().getChildren().size());
        assertEquals(first.getGenealogy().getChildren(), second.getGenealogy().getChildren());
    }

    @Test
    void oppositeSexCoupleConceivesRatherThanAdopting() {
        campaign.getCampaignOptions().set(CampaignOption.USE_RANDOM_SAME_SEX_ADOPTION, true);
        final Person husband = adult(Gender.MALE, 30, "Allard");
        final Person wife = adult(Gender.FEMALE, 30, "Allard");
        marry(husband, wife);

        procreation.processNewWeek(campaign, today, husband);
        procreation.processNewWeek(campaign, today, wife);

        assertTrue(wife.isPregnant());
        assertTrue(husband.getGenealogy().getChildren().isEmpty());
        assertTrue(wife.getGenealogy().getChildren().isEmpty());
    }

    @Test
    void sameSexAdoptionIsOffByDefault() {
        final Person first = adult(Gender.MALE, 32, "Brewer");
        final Person second = adult(Gender.MALE, 34, "Brewer");
        marry(first, second);

        procreation.processNewWeek(campaign, today, first);
        procreation.processNewWeek(campaign, today, second);

        assertTrue(first.getGenealogy().getChildren().isEmpty());
    }
    //endregion Random Adoption

    //region Pregnancy Size and Early Birth

    @Test
    void manualMultiplePregnancyCanBeResizedAndBornEarly() {
        final Person mother = adult(Gender.FEMALE, 27, "Avellar");

        procreation.addPregnancy(campaign, today, mother, 3, true);
        assertTrue(mother.isPregnant());
        assertTrue(mother.getDueDate().isAfter(today), "the birth is still months away");
        assertEquals(3, mother.getExtraData().get(AbstractProcreation.PREGNANCY_CHILDREN_DATA));

        procreation.setPregnancySize(mother, 2);
        assertEquals(2, mother.getExtraData().get(AbstractProcreation.PREGNANCY_CHILDREN_DATA));

        // Give Birth Now: an ordinary birth, just today
        procreation.birth(campaign, today, mother);

        assertFalse(mother.isPregnant());
        assertNull(mother.getExtraData().get(AbstractProcreation.PREGNANCY_CHILDREN_DATA));
        assertEquals(2, mother.getGenealogy().getChildren().size());
        for (Person baby : mother.getGenealogy().getChildren()) {
            assertEquals(today, baby.getDateOfBirth());
            assertNotNull(humanResources.getPerson(baby.getId()));
        }
    }
    //endregion Pregnancy Size and Early Birth
}
