/*
 * Copyright (C) 2021 - The MegaMek Team. All Rights Reserved.
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

import megamek.common.Compute;
import megamek.common.annotations.Nullable;
import megamek.common.util.EncodeControl;
import mekhq.MekHqConstants;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.ExtraData;
import mekhq.campaign.log.MedicalLogger;
import mekhq.campaign.log.PersonalLogger;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.FamilialRelationshipType;
import mekhq.campaign.personnel.enums.GenderDescriptors;
import mekhq.campaign.personnel.enums.PrisonerStatus;
import mekhq.campaign.personnel.enums.RandomProcreationMethod;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ResourceBundle;
import java.util.UUID;

/**
 * AbstractProcreation is the baseline class for procreation and birth in MekHQ. It holds all the
 * common logic for procreation, and is implemented by classes defining how to determine if a female
 * person will randomly procreate on a given day.
 */
public abstract class AbstractProcreation {
    //region Variable Declarations
    private final RandomProcreationMethod method;
    private boolean useClannerProcreation;
    private boolean usePrisonerProcreation;
    private boolean useRelationshiplessProcreation;
    private boolean useRandomClannerProcreation;
    private boolean useRandomPrisonerProcreation;

    public static final ExtraData.IntKey PREGNANCY_CHILDREN_DATA = new ExtraData.IntKey("procreation:children");
    public static final ExtraData.StringKey PREGNANCY_FATHER_DATA = new ExtraData.StringKey("procreation:father");

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel", new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    protected AbstractProcreation(final RandomProcreationMethod method, final CampaignOptions options) {
        this.method = method;
        setUseClannerProcreation(options.isUseClannerProcreation());
        setUsePrisonerProcreation(options.isUsePrisonerProcreation());
        setUseRelationshiplessProcreation(options.isUseRelationshiplessRandomProcreation());
        setUseRandomClannerProcreation(options.isUseRandomClannerProcreation());
        setUseRandomPrisonerProcreation(options.isUseRandomPrisonerProcreation());
    }
    //endregion Constructors

    //region Getters/Setters
    public RandomProcreationMethod getMethod() {
        return method;
    }

    public boolean isUseClannerProcreation() {
        return useClannerProcreation;
    }

    public void setUseClannerProcreation(final boolean useClannerProcreation) {
        this.useClannerProcreation = useClannerProcreation;
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

    public boolean isUseRandomClannerProcreation() {
        return useRandomClannerProcreation;
    }

    public void setUseRandomClannerProcreation(final boolean useRandomClannerProcreation) {
        this.useRandomClannerProcreation = useRandomClannerProcreation;
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
     * @param multiplePregnancyChance the chance to have each baby after the first
     * @return the number of babies the person will give birth to, limited to decuplets
     */
    private int determineNumberOfBabies(final int multiplePregnancyChance) {
        int children = 1;
        while ((Compute.randomInt(multiplePregnancyChance) == 0) && (children < 10)) {
            children++;
        }
        return children;
    }

    /**
     * This method determines the duration for a pregnancy, with a variance determined through a
     * Gaussian distribution with a maximum spread of approximately six weeks.
     *
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
        // To not get weird results, we limit the variance to +/- 4.0 (almost 6 weeks). A base
        // length of 268 creates a solid enough duration for now.
        return 268 + (int) Math.round(Math.max(-4.0, Math.min(4.0, gaussian)) * 10.0);
    }

    /**
     * This determines the current week of the pregnancy
     * @param today the current date
     * @param person the pregnant person
     * @return the current week of their pregnancy
     */
    public int determinePregnancyWeek(final LocalDate today, final Person person) {
        return Math.toIntExact(ChronoUnit.WEEKS.between(person.getExpectedDueDate()
                .minus(MekHqConstants.PREGNANCY_STANDARD_DURATION, ChronoUnit.DAYS)
                .plus(1, ChronoUnit.DAYS), today));
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
        } else if (!isUseClannerProcreation() && person.isClanner()) {
            return resources.getString("cannotProcreate.Clanner.text");
        } else if (!isUsePrisonerProcreation() && person.getPrisonerStatus().isPrisoner()) {
            return resources.getString("cannotProcreate.Prisoner.text");
        } else if (randomProcreation) {
            if (!isUseRelationshiplessProcreation() && !person.getGenealogy().hasSpouse()) {
                return resources.getString("cannotProcreate.NoSpouse.text");
            } else if (!isUseRandomClannerProcreation() && person.isClanner()) {
                return resources.getString("cannotProcreate.RandomClanner.text");
            } else if (!isUseRandomPrisonerProcreation() && person.getPrisonerStatus().isPrisoner()) {
                return resources.getString("cannotProcreate.RandomPrisoner.text");
            } else if (person.getGenealogy().hasSpouse()) {
                if (person.getGenealogy().getSpouse().getGender().isFemale()) {
                    return resources.getString("cannotProcreate.FemaleSpouse.text");
                } else if (!person.getGenealogy().getSpouse().isTryingToConceive()) {
                    return resources.getString("cannotProcreate.SpouseNotTryingForABaby.text");
                } else if (!person.getGenealogy().getSpouse().getStatus().isActive()) {
                    return resources.getString("cannotProcreate.InactiveSpouse.text");
                } else if (!isUseRandomClannerProcreation() && person.getGenealogy().getSpouse().isClanner()) {
                    return resources.getString("cannotProcreate.ClannerSpouse.text");
                } else if (!isUseRandomPrisonerProcreation()
                        && person.getGenealogy().getSpouse().getPrisonerStatus().isPrisoner()) {
                    return resources.getString("cannotProcreate.PrisonerSpouse.text");
                } else if (person.getGenealogy().getSpouse().getStatus().isMIA()) {
                    return resources.getString("cannotProcreate.MIASpouse.text");
                } else if (person.getGenealogy().getSpouse().isDeployed()) {
                    return resources.getString("cannotProcreate.DeployedSpouse.text");
                } else if (person.getGenealogy().getSpouse().isChild(today)) {
                    return resources.getString("cannotProcreate.ChildSpouse.text");
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
     */
    public void addPregnancy(final Campaign campaign, final LocalDate today, final Person mother) {
        addPregnancy(campaign, today, mother, determineNumberOfBabies(
                campaign.getCampaignOptions().getMultiplePregnancyOccurrences()));
    }

    /**
     * This method is how a person becomes pregnant with the specified number of children. They have
     * their due date set and the parentage of the pregnancy determined.
     *
     * @param campaign the campaign the person is a part of
     * @param today the current date
     * @param mother the newly pregnant mother
     * @param size the number of children the mother is having
     */
    public void addPregnancy(final Campaign campaign, final LocalDate today, final Person mother,
                             final int size) {
        if (size < 1) {
            return;
        }

        mother.setExpectedDueDate(today.plus(MekHqConstants.PREGNANCY_STANDARD_DURATION, ChronoUnit.DAYS));
        mother.setDueDate(today.plus(determinePregnancyDuration(), ChronoUnit.DAYS));
        mother.getExtraData().set(PREGNANCY_CHILDREN_DATA, size);
        mother.getExtraData().set(PREGNANCY_FATHER_DATA, mother.getGenealogy().hasSpouse()
                ? mother.getGenealogy().getSpouse().getId().toString() : null);

        final String babyAmount = resources.getString("babyAmount.text").split(",")[size - 1];
        campaign.addReport(String.format(resources.getString("babyConceived.report"),
                mother.getHyperlinkedName(), babyAmount).trim());
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
        Person father = (mother.getExtraData().get(PREGNANCY_FATHER_DATA) != null)
                ? campaign.getPerson(UUID.fromString(mother.getExtraData().get(PREGNANCY_FATHER_DATA))) : null;
        father = campaign.getCampaignOptions().isDetermineFatherAtBirth()
                ? Utilities.nonNull(mother.getGenealogy().getSpouse(), father) : father;

        // Determine Prisoner Status
        final PrisonerStatus prisonerStatus = campaign.getCampaignOptions().getPrisonerBabyStatus()
                ? mother.getPrisonerStatus() : PrisonerStatus.FREE;

        // Output a specific report to the campaign if they are giving birth to multiple children
        if (size > 1) {
            campaign.addReport(String.format(resources.getString("multipleBabiesBorn.report"),
                    mother.getHyperlinkedName(),
                    resources.getString("babyAmount.text").split(",")[size - 1]));
        }

        // Create Babies
        for (int i = 0; i < size; i++) {
            // Create the specific baby
            final Person baby = campaign.newDependent(true);
            baby.setSurname(campaign.getCampaignOptions().getBabySurnameStyle()
                    .generateBabySurname(mother, father, baby.getGender()));
            baby.setBirthday(today);

            // Create reports and log the birth
            campaign.addReport(String.format(resources.getString("babyBorn.report"),
                    mother.getHyperlinkedName(), baby.getHyperlinkedName(),
                    GenderDescriptors.BOY_GIRL.getDescriptor(baby.getGender())));
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

            // Founder Tag Assignment
            if (campaign.getCampaignOptions().isAssignNonPrisonerBabiesFounderTag()
                    && !prisonerStatus.isPrisoner()) {
                baby.setFounder(true);
            } else if (campaign.getCampaignOptions().isAssignChildrenOfFoundersFounderTag()) {
                baby.setFounder(baby.getGenealogy().getParents().stream().anyMatch(Person::isFounder));
            }

            // Recruit the baby
            campaign.recruitPerson(baby, prisonerStatus, true, true);
        }

        // Cleanup Data
        removePregnancy(mother);
    }

    //region New Day
    /**
     * Process new day procreation for an individual
     * @param campaign the campaign to process
     * @param today the current day
     * @param person the person to process
     */
    public void processNewDay(final Campaign campaign, final LocalDate today, final Person person) {
        // Instantly return for male personnel
        if (person.getGender().isMale()) {
            return;
        }

        // Check if they are already pregnant
        if (person.isPregnant()) {
            // They give birth if the due date is the current day
            if (today.isEqual(person.getDueDate())) {
                birth(campaign, today, person);
            }
            return;
        }

        // Make the required checks for random procreation
        if (randomlyProcreates(today, person)) {
            addPregnancy(campaign, today, person);
        }
    }

    //region Random Procreation
    /**
     * Determines if a non-pregnant female person procreates on a given day
     * @param today the current day
     * @param person the person in question
     * @return true if they do, otherwise false
     */
    protected boolean randomlyProcreates(final LocalDate today, final Person person) {
        if (canProcreate(today, person, true) != null) {
            return false;
        } else if (person.getGenealogy().hasSpouse()) {
            return relationshipProcreation(person);
        } else if (isUseRelationshiplessProcreation()) {
            return relationshiplessProcreation(person);
        } else {
            return false;
        }
    }

    /**
     * Determines if a person with an eligible partner procreates
     * @param person the person to determine for
     * @return true if they do, otherwise false
     */
    protected abstract boolean relationshipProcreation(final Person person);

    /**
     * Determines if a person without a partner procreates
     * @param person the person to determine for
     * @return true if they do, otherwise false
     */
    protected abstract boolean relationshiplessProcreation(final Person person);
    //endregion Random Procreation
    //endregion New Day

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
        // This is purposeful, to allow for player customization, and thus we first check if they
        // are pregnant before checking if the birth occurs
        if (person.isPregnant()) {
            final int pregnancyWeek = determinePregnancyWeek(today, person);
            final double babyBornChance;
            if (pregnancyWeek > 35) {
                babyBornChance = 0.99;
            } else if (pregnancyWeek > 29) {
                babyBornChance = 0.95;
            } else if (pregnancyWeek > 25) {
                babyBornChance = 0.9;
            } else if (pregnancyWeek == 25) {
                babyBornChance = 0.8;
            } else if (pregnancyWeek == 24) {
                babyBornChance = 0.5;
            } else if (pregnancyWeek == 23) {
                babyBornChance = 0.25;
            } else {
                babyBornChance = 0.0;
            }

            if (Compute.randomFloat() < babyBornChance) {
                birth(campaign, today, person);
            }
        }
    }
}
