/*
 * Copyright (c) 2021-2024 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.procreation;

import megamek.codeUtilities.MathUtility;
import megamek.common.Compute;
import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import megamek.common.options.IOption;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.ExtraData.IntKey;
import mekhq.campaign.ExtraData.StringKey;
import mekhq.campaign.log.MedicalLogger;
import mekhq.campaign.log.PersonalLogger;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.enums.*;
import mekhq.campaign.personnel.enums.education.EducationLevel;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static mekhq.campaign.personnel.education.EducationController.setInitialEducationLevel;

/**
 * AbstractProcreation is the baseline class for procreation and birth in MekHQ. It holds all the
 * common logic for procreation, and is implemented by classes defining how to determine if a female
 * person will randomly procreate on a given day.
 */
public abstract class AbstractProcreation {
    //region Variable Declarations
    private final RandomProcreationMethod method;
    private boolean useClanPersonnelProcreation;
    private boolean usePrisonerProcreation;
    private boolean useRelationshiplessProcreation;
    private boolean useRandomClanPersonnelProcreation;
    private boolean useRandomPrisonerProcreation;

    public static final IntKey PREGNANCY_CHILDREN_DATA = new IntKey("procreation:children");
    public static final StringKey PREGNANCY_FATHER_DATA = new StringKey("procreation:father");

    private static final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    //region Constructors
    protected AbstractProcreation(final RandomProcreationMethod method, final CampaignOptions options) {
        this.method = method;
        setUseClanPersonnelProcreation(options.isUseClanPersonnelProcreation());
        setUsePrisonerProcreation(options.isUsePrisonerProcreation());
        setUseRelationshiplessProcreation(options.isUseRelationshiplessRandomProcreation());
        setUseRandomClanPersonnelProcreation(options.isUseRandomClanPersonnelProcreation());
        setUseRandomPrisonerProcreation(options.isUseRandomPrisonerProcreation());
    }
    //endregion Constructors

    //region Getters/Setters
    public RandomProcreationMethod getMethod() {
        return method;
    }

    public boolean isUseClanPersonnelProcreation() {
        return useClanPersonnelProcreation;
    }

    public void setUseClanPersonnelProcreation(final boolean useClanPersonnelProcreation) {
        this.useClanPersonnelProcreation = useClanPersonnelProcreation;
    }

    public boolean isUsePrisonerProcreation() {
        return usePrisonerProcreation;
    }

    public void setUsePrisonerProcreation(final boolean usePrisonerProcreation) {
        this.usePrisonerProcreation = usePrisonerProcreation;
    }

    public boolean isUseRelationshiplessProcreation() {
        return useRelationshiplessProcreation;
    }

    public void setUseRelationshiplessProcreation(final boolean useRelationshiplessProcreation) {
        this.useRelationshiplessProcreation = useRelationshiplessProcreation;
    }

    public boolean isUseRandomClanPersonnelProcreation() {
        return useRandomClanPersonnelProcreation;
    }

    public void setUseRandomClanPersonnelProcreation(final boolean useRandomClanPersonnelProcreation) {
        this.useRandomClanPersonnelProcreation = useRandomClanPersonnelProcreation;
    }

    public boolean isUseRandomPrisonerProcreation() {
        return useRandomPrisonerProcreation;
    }

    public void setUseRandomPrisonerProcreation(final boolean useRandomPrisonerProcreation) {
        this.useRandomPrisonerProcreation = useRandomPrisonerProcreation;
    }
    //endregion Getters/Setters

    //region Determination Methods
    /**
     * This method determines the number of babies a person will give birth to.
     * @param multiplePregnancyOccurrences the X occurrences for there to be a single multiple
     *                                    child occurrence (i.e., 1 in X)
     * @return the number of babies the person will give birth to, limited to decuplets
     */
    protected int determineNumberOfBabies(final int multiplePregnancyOccurrences) {
        int children = 1;
        while ((Compute.randomInt(multiplePregnancyOccurrences) == 0) && (children < 10)) {
            children++;
        }
        return children;
    }

    /**
     * This method determines the duration for a pregnancy, with a variance determined through a
     * Gaussian distribution with a maximum spread of approximately six weeks.
     * <p>
     * TODO : Swap me to instead use a distribution function that generates an overall length,
     * TODO : Including pre-term and post-term births
     *
     * @return the pregnancy duration
     */
    private int determinePregnancyDuration() {
        // This creates a random range of approximately six weeks with which to modify the standard
        // pregnancy duration to create a randomized pregnancy duration
        final double gaussian = Math.sqrt(-2.0 * Math.log(Math.random()))
                * Math.cos(2.0 * Math.PI * Math.random());
        // To not get unusual results, we limit the variance to +/- 4.0 (almost 6 weeks).
        // A base length of 268 creates a solid enough duration for now.
        return 268 + (int) Math.round(MathUtility.clamp(gaussian, -4d, 4d) * 10.0);
    }

    /**
     * This determines the current week of the pregnancy
     * @param today the current date
     * @param person the pregnant person
     * @return the current week of their pregnancy
     */
    public int determinePregnancyWeek(final LocalDate today, final Person person) {
        return (int) Math.max(Math.ceil(ChronoUnit.DAYS.between(
                person.getExpectedDueDate().minusDays(MHQConstants.PREGNANCY_STANDARD_DURATION),
                today) / 7f), 1);
    }

    /**
     * This determines the father of the baby.
     *
     * @param campaign the campaign the baby is part of
     * @param mother the mother of the baby
     */
    protected @Nullable Person determineFather(final Campaign campaign, final Person mother) {
        return (campaign.getCampaignOptions().isDetermineFatherAtBirth() && mother.getGenealogy().hasSpouse())
                ? mother.getGenealogy().getSpouse()
                : ((mother.getExtraData().get(PREGNANCY_FATHER_DATA) != null)
                        ? campaign.getPerson(UUID.fromString(mother.getExtraData().get(PREGNANCY_FATHER_DATA)))
                        : null);
    }
    //endregion Determination Methods

    /**
     * This is used to determine if a person can procreate
     * @param today the current date
     * @param person the person to determine for
     * @param randomProcreation if this is for random procreation or manual procreation
     * @return null if they can, otherwise the reason why they cannot
     */
    public @Nullable String canProcreate(final LocalDate today, final Person person,
                                         final boolean randomProcreation) {
        if (person.getGender().isMale()) {
            return resources.getString("cannotProcreate.Gender.text");
        } else if (!person.isTryingToConceive()) {
            return resources.getString("cannotProcreate.NotTryingForABaby.text");
        } else if (person.isPregnant()) {
            return resources.getString("cannotProcreate.AlreadyPregnant.text");
        } else if (!person.getStatus().isActive()) {
            return resources.getString("cannotProcreate.Inactive.text");
        } else if (person.isDeployed()) {
            return resources.getString("cannotProcreate.Deployed.text");
        } else if (person.isChild(today)) {
            return resources.getString("cannotProcreate.Child.text");
        } else if (person.getAge(today) >= 51) {
            return resources.getString("cannotProcreate.TooOld.text");
        } else if (!isUseClanPersonnelProcreation() && person.isClanPersonnel()) {
            return resources.getString("cannotProcreate.ClanPersonnel.text");
        } else if (!isUsePrisonerProcreation() && person.getPrisonerStatus().isCurrentPrisoner()) {
            return resources.getString("cannotProcreate.Prisoner.text");
        } else if (randomProcreation) {
            if (!isUseRelationshiplessProcreation() && !person.getGenealogy().hasSpouse()) {
                return resources.getString("cannotProcreate.NoSpouse.text");
            } else if (!isUseRandomClanPersonnelProcreation() && person.isClanPersonnel()) {
                return resources.getString("cannotProcreate.RandomClanPersonnel.text");
            } else if (!isUseRandomPrisonerProcreation() && person.getPrisonerStatus().isCurrentPrisoner()) {
                return resources.getString("cannotProcreate.RandomPrisoner.text");
            } else if (person.getGenealogy().hasSpouse()) {
                if (person.getGenealogy().getSpouse().getGender().isFemale()) {
                    return resources.getString("cannotProcreate.FemaleSpouse.text");
                } else if (!person.getGenealogy().getSpouse().isTryingToConceive()) {
                    return resources.getString("cannotProcreate.SpouseNotTryingForABaby.text");
                } else if (!person.getGenealogy().getSpouse().getStatus().isActive()) {
                    return resources.getString("cannotProcreate.InactiveSpouse.text");
                } else if (person.getGenealogy().getSpouse().isDeployed()) {
                    return resources.getString("cannotProcreate.DeployedSpouse.text");
                } else if (person.getGenealogy().getSpouse().isChild(today)) {
                    return resources.getString("cannotProcreate.ChildSpouse.text");
                } else if (!isUseRandomClanPersonnelProcreation() && person.getGenealogy().getSpouse().isClanPersonnel()) {
                    return resources.getString("cannotProcreate.ClanPersonnelSpouse.text");
                } else if (!isUseRandomPrisonerProcreation()
                        && person.getGenealogy().getSpouse().getPrisonerStatus().isCurrentPrisoner()) {
                    return resources.getString("cannotProcreate.PrisonerSpouse.text");
                }
            }
        }

        return null;
    }

    /**
     * This method is how a person becomes pregnant.
     * @param campaign the campaign the person is a part of
     * @param today the current date
     * @param mother the newly pregnant mother
     * @param isNoReport true if no message should be posted to the daily report
     */
    public void addPregnancy(final Campaign campaign, final LocalDate today, final Person mother, boolean isNoReport) {
        addPregnancy(
                campaign,
                today,
                mother,
                determineNumberOfBabies(campaign.getCampaignOptions().getMultiplePregnancyOccurrences()),
                isNoReport
        );
    }

    /**
     * This method is how a person becomes pregnant with the specified number of children. They have
     * their due date set, and the parentage of the pregnancy is determined.
     *
     * @param campaign the campaign the person is a part of
     * @param today the current date
     * @param mother the newly pregnant mother
     * @param size the number of children the mother is having
     * @param isNoReport true if no message should be posted to the daily report
     */
    public void addPregnancy(final Campaign campaign, final LocalDate today, final Person mother,
                             final int size, boolean isNoReport) {
        if (size < 1) {
            return;
        }

        mother.setExpectedDueDate(today.plusDays(MHQConstants.PREGNANCY_STANDARD_DURATION));

        mother.setDueDate(today.plusDays(determinePregnancyDuration()));

        mother.getExtraData().set(PREGNANCY_CHILDREN_DATA, size);

        mother.getExtraData().set(PREGNANCY_FATHER_DATA, mother.getGenealogy().hasSpouse()
                ? mother.getGenealogy().getSpouse().getId().toString() : null);

        final String babyAmount = resources.getString("babyAmount.text").split(",")[size - 1];

        if (!isNoReport) {
            campaign.addReport(String.format(resources.getString("babyConceived.report"),
                    mother.getHyperlinkedName(),
                    babyAmount).trim());
        }

        if (campaign.getCampaignOptions().isLogProcreation()) {
            MedicalLogger.hasConceived(mother, today, babyAmount);

            if (mother.getGenealogy().hasSpouse()) {
                PersonalLogger.spouseConceived(mother.getGenealogy().getSpouse(),
                        mother.getFullName(), today, babyAmount);
            }
        }
    }

    /**
     * Removes a pregnancy and clears all related data from the provided person
     * @param person the person to clear the pregnancy data for
     */
    public void removePregnancy(final Person person) {
        person.setDueDate(null);
        person.setExpectedDueDate(null);
        person.getExtraData().set(PREGNANCY_CHILDREN_DATA, null);
        person.getExtraData().set(PREGNANCY_FATHER_DATA, null);
    }

    /**
     * This method is how a mother gives birth to a number of babies and has them added to the
     * campaign.
     *
     * @param campaign the campaign to add the baby in question to
     * @param today today's date
     * @param mother the mother giving birth
     */
    public void birth(final Campaign campaign, final LocalDate today, final Person mother) {
        // Determine the number of children
        final int size = mother.getExtraData().get(PREGNANCY_CHILDREN_DATA, 1);

        // Determine father information
        final Person father = determineFather(campaign, mother);

        // Determine Prisoner Status
        final PrisonerStatus prisonerStatus = campaign.getCampaignOptions().isPrisonerBabyStatus()
                ? mother.getPrisonerStatus() : PrisonerStatus.FREE;

        // Output a specific report to the campaign if they are giving birth to multiple children
        if (size > 1) {
            campaign.addReport(String.format(resources.getString("multipleBabiesBorn.report"),
                    mother.getHyperlinkedName(),
                    resources.getString("babyAmount.text").split(",")[size - 1]));
        }

        // Create Babies
        for (int i = 0; i < size; i++) {
            // Create a baby
            final Person baby = campaign.newDependent(
                Gender.RANDOMIZE, mother.getOriginFaction(), campaign.getLocation().getPlanet());
            baby.setSurname(campaign.getCampaignOptions().getBabySurnameStyle()
                    .generateBabySurname(mother, father, baby.getGender()));
            baby.setDateOfBirth(today);

            // Create reports and log the birth
            campaign.addReport(String.format(resources.getString("babyBorn.report"),
                    mother.getHyperlinkedName(), baby.getHyperlinkedName(),
                    GenderDescriptors.BOY_GIRL.getDescriptor(baby.getGender())));
            logAndUpdateFamily(campaign, today, mother, baby, father);

            // Founder Tag Assignment
            if (campaign.getCampaignOptions().isAssignNonPrisonerBabiesFounderTag()
                    && !prisonerStatus.isCurrentPrisoner()) {
                baby.setFounder(true);
            } else if (campaign.getCampaignOptions().isAssignChildrenOfFoundersFounderTag()) {
                baby.setFounder(baby.getGenealogy().getParents().stream().anyMatch(Person::isFounder));
            }

            // set education
            baby.setEduHighestEducation(EducationLevel.EARLY_CHILDHOOD);

            // set loyalty
            baby.setLoyalty(Compute.d6(4, 3));

            // Recruit the baby
            campaign.recruitPerson(baby, prisonerStatus, true, true);

            // if the mother is at school, add the baby to the list of tag alongs
            if (mother.getStatus().isStudent()) {
                mother.addEduTagAlong(baby.getId());
                baby.changeStatus(campaign, today, PersonnelStatus.ON_LEAVE);
            }
        }

        // adjust parents' loyalty
        if (father != null) {
            father.performRandomizedLoyaltyChange(campaign, false, true);
        }

        mother.performRandomizedLoyaltyChange(campaign, false, true);

        // check desire for children
        if (Compute.d6(1) <= 2) {
            mother.setTryingToConceive(false);
        }

        // Cleanup Data
        removePregnancy(mother);

        // Return from Maternity leave
        if (mother.getStatus().isOnMaternityLeave()) {
            mother.changeStatus(campaign, today, PersonnelStatus.ACTIVE);
        }
    }

    /**
     * Logs the birth of a baby and updates the genealogy information of the family.
     *
     * @param campaign the ongoing campaign
     * @param today the current date
     * @param mother the mother of the baby
     * @param baby the newborn baby
     * @param father the father of the baby, null if unknown
     */
    private static void logAndUpdateFamily(Campaign campaign, LocalDate today, Person mother, Person baby, Person father) {
        if (campaign.getCampaignOptions().isLogProcreation()) {
            MedicalLogger.deliveredBaby(mother, baby, today);
            if (father != null) {
                PersonalLogger.ourChildBorn(father, baby, mother.getFullName(), today);
            }
        }

        // Create genealogy information
        baby.getGenealogy().addFamilyMember(FamilialRelationshipType.PARENT, mother);
        mother.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, baby);

        if (father != null) {
            baby.getGenealogy().addFamilyMember(FamilialRelationshipType.PARENT, father);
            father.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, baby);
        }
    }

    /**
     * Creates baby/babies and performs any necessary operations such as setting birthdate, creating reports,
     * updating genealogy, setting education, loyalty, personality, and recruiting the baby.
     * This version is for historic births that occur as part of a character's background.
     *
     * @param campaign the campaign object
     * @param today the current date
     * @param mother the mother person object
     * @param father the father person object, can be null if the father is unknown
     * @return the babies
     */
    public List<Person> birthHistoric(final Campaign campaign, final LocalDate today, final Person mother, @Nullable final Person father) {
        List<Person> babies = new ArrayList<>();

        // Determine the number of children
        final int size = mother.getExtraData().get(PREGNANCY_CHILDREN_DATA, 1);
        // Create Babies
        for (int i = 0; i < size; i++) {
            // Create the babies
            Faction originFaction = mother.getOriginFaction();
            Planet originPlanet = mother.getOriginPlanet();

            if (father != null && Compute.randomInt(1) == 0) {
                originFaction = father.getOriginFaction();
                originPlanet = father.getOriginPlanet();
            }

            final Person baby = campaign.newDependent(Gender.RANDOMIZE, originFaction, originPlanet);

            baby.setSurname(campaign.getCampaignOptions().getBabySurnameStyle()
                    .generateBabySurname(mother, father, baby.getGender()));

            baby.setDateOfBirth(mother.getDueDate());

            baby.removeAllSkills();// Limit skills by age for children and adolescents

            // re-roll SPAs to include in any age and skill adjustments
            Enumeration<IOption> options = new PersonnelOptions().getOptions(PersonnelOptions.LVL3_ADVANTAGES);

            for (IOption option : Collections.list(options)) {
                baby.getOptions().getOption(option.getName()).clearValue();
            }

            baby.setLoyalty(Compute.d6(3) + 2);

            // set education based on age
            setInitialEducationLevel(campaign, baby);

            // Create reports and log the birth
            logAndUpdateFamily(campaign, today, mother, baby, father);

            // add to the list of babies
            babies.add(baby);
        }

        if (Compute.d6(1) <= 2) {
            mother.setTryingToConceive(false);
        }

        // Cleanup Data
        removePregnancy(mother);

        return babies;
    }

    /**
     * This is used to process procreation when a person dies with the Pregnancy Complications status
     * @param campaign the campaign to add the baby to
     * @param today the current date
     * @param person the person to process
     */
    public void processPregnancyComplications(final Campaign campaign, final LocalDate today,
                                              final Person person) {
        // The child might be able to be born, albeit into a world without their mother.
        // The status, however, can be manually set for males and for those who are not pregnant.
        // This is purposeful to allow for player customization, and thus we first check if they
        // are pregnant before checking if the birth occurs
        if (!person.isPregnant()) {
            return;
        }

        final int pregnancyWeek = determinePregnancyWeek(today, person);
        final double babyBornChance = getBabyBornChance(pregnancyWeek);

        if (Compute.randomFloat() < babyBornChance) {
            birth(campaign, today, person);
        }
    }

    /**
     * Calculates the chance of a baby being born based on the pregnancy week.
     *
     * @param pregnancyWeek the week of the pregnancy
     * @return the chance of a baby being born, ranging from 0.0 to 1.0
     */
    private static double getBabyBornChance(int pregnancyWeek) {
        int range = switch (pregnancyWeek) {
            case 23 -> 1;
            case 24 -> 2;
            case 25 -> 3;
            default -> (pregnancyWeek > 25 && pregnancyWeek <= 29) ? 4 :
                    (pregnancyWeek > 29 && pregnancyWeek <=35) ? 5 :
                            (pregnancyWeek > 35) ? 6 : 0;
        };

        return switch (range) {
            case 1 -> 0.25;
            case 2 -> 0.5;
            case 3 -> 0.8;
            case 4 -> 0.9;
            case 5 -> 0.95;
            case 6 -> 0.99;
            default -> 0.0;
        };
    }

    //region New Day
    /**
     * Process new day procreation for an individual
     * @param campaign the campaign to process
     * @param today the current day
     * @param person the person to process
     */
    public void processNewWeek(final Campaign campaign, final LocalDate today, final Person person) {
        // Instantly return for male personnel
        if (person.getGender().isMale()) {
            return;
        }

        // Check if they are already pregnant
        if (person.isPregnant()) {
            // They give birth if the due date has passed
            if ((today.isAfter(person.getDueDate())) || (today.isEqual(person.getDueDate()))) {
                birth(campaign, today, person);

                return;
            }

            if (campaign.getCampaignOptions().isUseMaternityLeave()) {
                if (person.getStatus().isActive()
                    && (person.getDueDate().minusWeeks(20).isAfter(today.minusDays(1)))) {
                    person.changeStatus(campaign, today, PersonnelStatus.ON_MATERNITY_LEAVE);
                }
            }

            return;
        }

        // Make the required checks for random procreation
        processRandomProcreationCheck(campaign, today, person, false);
    }

    /**
     * Checks if a person randomly procreates on the given day in the campaign.
     * If the person does procreate, add a pregnancy to the campaign for the person.
     *
     * @param campaign The campaign to check procreation for.
     * @param today The current date.
     * @param person The person to check for procreation.
     * @param isNoReport true, if the player shouldn't be informed, otherwise false
     */
    public void processRandomProcreationCheck(final Campaign campaign, final LocalDate today,
                                              final Person person, boolean isNoReport) {
        if (randomlyProcreates(today, person)) {
            addPregnancy(campaign, today, person, isNoReport);
        }
    }

    //region Random Procreation
    /**
     * Determines if a non-pregnant woman procreates on a given day
     * @param today the current day
     * @param person the person in question
     * @return true if they do, otherwise false
     */
    protected boolean randomlyProcreates(final LocalDate today, final Person person) {
        if (canProcreate(today, person, true) != null) {
            return false;
        } else {
            return procreation(person);
        }
    }

    /**
     * Determines if a person with an eligible partner procreates
     * @param person the person to determine for
     * @return true if they do, otherwise false
     */
    protected abstract boolean procreation(final Person person);
}
