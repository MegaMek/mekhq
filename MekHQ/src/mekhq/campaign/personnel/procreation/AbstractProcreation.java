/*
 * Copyright (C) 2021-2026 The MegaMek Team. All Rights Reserved.
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

import static mekhq.campaign.enums.DailyReportType.PERSONNEL;
import static mekhq.campaign.personnel.PersonnelOptions.UNOFFICIAL_DOBROWSKI_SYNDROME;
import static mekhq.campaign.personnel.enums.BloodGroup.getInheritedBloodGroup;
import static mekhq.campaign.personnel.enums.BloodGroup.getRandomBloodGroup;
import static mekhq.campaign.personnel.medical.BodyLocation.GENERIC;
import static mekhq.campaign.personnel.medical.BodyLocation.INTERNAL;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.AlternateInjuries.BIRTH_DEFECT;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.AlternateInjuries.KAER_PATHOGEN;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.annotation.Nullable;
import megamek.common.compute.Compute;
import megamek.common.enums.Gender;
import megamek.common.icons.Portrait;
import megamek.common.options.IOption;
import mekhq.MHQConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.ExtraData.IntKey;
import mekhq.campaign.ExtraData.StringKey;
import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.log.MedicalLogger;
import mekhq.campaign.log.PersonalLogger;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.enums.BabySurnameStyle;
import mekhq.campaign.personnel.enums.FamilialRelationshipType;
import mekhq.campaign.personnel.enums.GenderDescriptors;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.enums.RandomProcreationMethod;
import mekhq.campaign.personnel.enums.education.EducationLevel;
import mekhq.campaign.personnel.lifeEvents.BirthAnnouncement;
import mekhq.campaign.personnel.medical.advancedMedical.InjuryTypes;
import mekhq.campaign.personnel.medical.advancedMedicalAlternate.AdvancedMedicalAlternate;
import mekhq.campaign.personnel.medical.advancedMedicalAlternate.AlternateInjuries;
import mekhq.campaign.randomEvents.prisoners.PrisonerStatus;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;

/**
 * AbstractProcreation is the baseline class for procreation and birth in MekHQ. It holds all the common logic for
 * procreation, and is implemented by classes defining how to determine if a female person will randomly procreate on a
 * given day.
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

    /** The most babies a single pregnancy can have (decuplets), matching the labels in {@code babyAmount.text}. */
    public static final int MAXIMUM_PREGNANCY_SIZE = 10;

    /** The oldest a child who joins a parent's family later in life can be, so they always arrive as a minor. */
    public static final int MAXIMUM_ARRIVING_CHILD_AGE = 17;

    private static final String RESOURCE_BUNDLE = "mekhq.resources.AbstractProcreation";
    //endregion Variable Declarations

    //region Constructors
    protected AbstractProcreation(final RandomProcreationMethod method, final CampaignOptions options) {
        this.method = method;
        setUseClanPersonnelProcreation(options.get(CampaignOption.USE_CLAN_PERSONNEL_PROCREATION));
        setUsePrisonerProcreation(options.get(CampaignOption.USE_PRISONER_PROCREATION));
        setUseRelationshiplessProcreation(options.get(CampaignOption.USE_RELATIONSHIPLESS_RANDOM_PROCREATION));
        setUseRandomClanPersonnelProcreation(options.get(CampaignOption.USE_RANDOM_CLAN_PERSONNEL_PROCREATION));
        setUseRandomPrisonerProcreation(options.get(CampaignOption.USE_RANDOM_PRISONER_PROCREATION));
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
     *
     * @param multiplePregnancyOccurrences the X occurrences for there to be a single multiple child occurrence (i.e., 1
     *                                     in X)
     *
     * @return the number of babies the person will give birth to, limited to decuplets
     */
    protected int determineNumberOfBabies(final int multiplePregnancyOccurrences) {
        int children = 1;
        while ((Compute.randomInt(multiplePregnancyOccurrences) == 0) && (children < MAXIMUM_PREGNANCY_SIZE)) {
            children++;
        }
        return children;
    }

    /**
     * This method determines the duration for a pregnancy, with a variance determined through a Gaussian distribution
     * with a maximum spread of approximately six weeks.
     * <p>
     * TODO : Swap me to instead use a distribution function that generates an overall length,
     * TODO : Including pre-term and post-term births
     *
     * @return the pregnancy duration
     */
    private int determinePregnancyDuration() {
        // This creates a random range of approximately six weeks with which to modify the standard
        // pregnancy duration to create a randomized pregnancy duration
        final double gaussian = Math.sqrt(-2.0 * Math.log(Math.random())) * Math.cos(2.0 * Math.PI * Math.random());
        // To not get unusual results, we limit the variance to +/- 4.0 (almost 6 weeks).
        // A base length of 268 creates a solid enough duration for now.
        return 268 + (int) Math.round(Math.clamp(gaussian, -4d, 4d) * 10.0);
    }

    /**
     * This determines the current week of the pregnancy
     *
     * @param today  the current date
     * @param person the pregnant person
     *
     * @return the current week of their pregnancy
     */
    public int determinePregnancyWeek(final LocalDate today, final Person person) {
        return (int) Math.max(Math.ceil(ChronoUnit.DAYS.between(person.getExpectedDueDate()
                                                                      .minusDays(MHQConstants.PREGNANCY_STANDARD_DURATION),
              today) / 7f), 1);
    }

    /**
     * This determines the father of the baby.
     *
     * @param campaign the campaign the baby is part of
     * @param mother   the mother of the baby
     */
    protected @Nullable Person determineFather(final Campaign campaign, final Person mother) {
        if ((campaign.getCampaignOptions().get(CampaignOption.DETERMINE_FATHER_AT_BIRTH) && mother.getGenealogy().hasSpouse())) {
            return mother.getGenealogy().getSpouse();
        } else {
            if ((mother.getExtraData().get(PREGNANCY_FATHER_DATA) != null)) {
                return (campaign.getPlayerForce().getHumanResources().getPerson(UUID.fromString(mother.getExtraData().get(PREGNANCY_FATHER_DATA))));
            } else {return (null);}
        }
    }
    //endregion Determination Methods

    /**
     * This is used to determine if a person can procreate
     *
     * @param today             the current date
     * @param person            the person to determine for
     * @param randomProcreation if this is for random procreation or manual procreation
     *
     * @return null if they can, otherwise the reason why they cannot
     */
    public @Nullable String canProcreate(final LocalDate today, final Person person, final boolean randomProcreation) {
        if (!person.isWantsChildren()) {
            if (person.getGender().isMale()) {
                return getFormattedTextAt(RESOURCE_BUNDLE, "cannotProcreate.DoesNotWantChildren.text");
            } else {
                return getFormattedTextAt(RESOURCE_BUNDLE, "cannotProcreate.NotTryingForABaby.text");
            }
        }

        if (person.isPregnant()) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "cannotProcreate.AlreadyPregnant.text");
        }

        if (!person.getStatus().isActiveFlexible()) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "cannotProcreate.Inactive.text");
        }

        if (person.isDeployed()) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "cannotProcreate.Deployed.text");
        }

        // Not allowing under-18s to procreate is project policy
        if (person.isChild(today, true)) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "cannotProcreate.Child.text");
        }

        if (person.getAge(today) >= 51) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "cannotProcreate.TooOld.text");
        }

        if (!isUseClanPersonnelProcreation() && person.isClanPersonnel()) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "cannotProcreate.ClanPersonnel.text");
        }

        if (!isUsePrisonerProcreation() && person.getPrisonerStatus().isCurrentPrisoner()) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "cannotProcreate.Prisoner.text");
        }

        if (randomProcreation) {
            if (!isUseRelationshiplessProcreation() && !person.getGenealogy().hasSpouse()) {
                return getFormattedTextAt(RESOURCE_BUNDLE, "cannotProcreate.NoSpouse.text");
            }

            if (!isUseRandomClanPersonnelProcreation() && person.isClanPersonnel()) {
                return getFormattedTextAt(RESOURCE_BUNDLE, "cannotProcreate.RandomClanPersonnel.text");
            }

            if (!isUseRandomPrisonerProcreation() && person.getPrisonerStatus().isCurrentPrisoner()) {
                return getFormattedTextAt(RESOURCE_BUNDLE, "cannotProcreate.RandomPrisoner.text");
            }

            if (person.getGenealogy().hasSpouse()) {
                if (person.getGenealogy().getSpouse().getGender().isFemale()) {
                    return getFormattedTextAt(RESOURCE_BUNDLE, "cannotProcreate.FemaleSpouse.text");
                }

                if (!person.getGenealogy().getSpouse().isWantsChildren()) {
                    return getFormattedTextAt(RESOURCE_BUNDLE, "cannotProcreate.SpouseNotTryingForABaby.text");
                }

                if (!person.getGenealogy().getSpouse().getStatus().isActiveFlexible()) {
                    return getFormattedTextAt(RESOURCE_BUNDLE, "cannotProcreate.InactiveSpouse.text");
                }

                if (person.getGenealogy().getSpouse().isDeployed()) {
                    return getFormattedTextAt(RESOURCE_BUNDLE, "cannotProcreate.DeployedSpouse.text");
                }

                // Not allowing under-18s to procreate is project policy
                // This conditional shouldn't be relevant in 2025, due to changes made in 2024.
                // However, we're keeping it as we don't want campaigns from pre-policy
                // implementation being able to circumnavigate project policy.
                if (person.getGenealogy().getSpouse().isChild(today, true)) {
                    return getFormattedTextAt(RESOURCE_BUNDLE, "cannotProcreate.ChildSpouse.text");
                }

                if (!isUseRandomClanPersonnelProcreation() && person.getGenealogy().getSpouse().isClanPersonnel()) {
                    return getFormattedTextAt(RESOURCE_BUNDLE, "cannotProcreate.ClanPersonnelSpouse.text");
                }

                if (!isUseRandomPrisonerProcreation() &&
                          person.getGenealogy().getSpouse().getPrisonerStatus().isCurrentPrisoner()) {
                    return getFormattedTextAt(RESOURCE_BUNDLE, "cannotProcreate.PrisonerSpouse.text");
                }
            }
        }

        return null;
    }

    /**
     * This method is how a person becomes pregnant.
     *
     * @param campaign   the campaign the person is a part of
     * @param today      the current date
     * @param mother     the newly pregnant mother
     * @param isNoReport true if no message should be posted to the daily report
     */
    public void addPregnancy(final Campaign campaign, final LocalDate today, final Person mother, boolean isNoReport) {
        addPregnancy(campaign,
              today,
              mother,
              determineNumberOfBabies(campaign.getCampaignOptions().get(CampaignOption.MULTIPLE_PREGNANCY_OCCURRENCES)),
              isNoReport);
    }

    /**
     * This method is how a person becomes pregnant with the specified number of children. They have their due date set,
     * and the parentage of the pregnancy is determined.
     *
     * @param campaign   the campaign the person is a part of
     * @param today      the current date
     * @param mother     the newly pregnant mother
     * @param size       the number of children the mother is having
     * @param isNoReport true if no message should be posted to the daily report
     */
    public void addPregnancy(final Campaign campaign, final LocalDate today, final Person mother, final int size,
          boolean isNoReport) {
        if (size < 1) {
            return;
        }

        mother.setExpectedDueDate(today.plusDays(MHQConstants.PREGNANCY_STANDARD_DURATION));

        mother.setDueDate(today.plusDays(determinePregnancyDuration()));

        mother.getExtraData().set(PREGNANCY_CHILDREN_DATA, size);

        mother.getExtraData()
              .set(PREGNANCY_FATHER_DATA,
                    mother.getGenealogy().hasSpouse() ? mother.getGenealogy().getSpouse().getId().toString() : null);

        final String babyAmount = getFormattedTextAt(RESOURCE_BUNDLE, "babyAmount.text").split(",")[size - 1];

        if (!isNoReport) {
            campaign.addReport(PERSONNEL, getFormattedTextAt(RESOURCE_BUNDLE,
                  "babyConceived.report",
                  mother.getHyperlinkedName(),
                  babyAmount).trim());
        }

        if (campaign.getCampaignOptions().get(CampaignOption.LOG_PROCREATION)) {
            MedicalLogger.hasConceived(mother, today, babyAmount);

            if (mother.getGenealogy().hasSpouse()) {
                PersonalLogger.spouseConceived(mother.getGenealogy().getSpouse(),
                      mother.getFullName(),
                      today,
                      babyAmount);
            }
        }
    }

    /**
     * Changes the number of babies an existing pregnancy will have. Does nothing if the person isn't pregnant.
     *
     * @param mother the pregnant person
     * @param size   the new number of babies, limited to between one and {@link #MAXIMUM_PREGNANCY_SIZE}
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setPregnancySize(final Person mother, final int size) {
        if (!mother.isPregnant()) {
            return;
        }

        mother.getExtraData().set(PREGNANCY_CHILDREN_DATA, Math.clamp(size, 1, MAXIMUM_PREGNANCY_SIZE));
    }

    /**
     * Removes a pregnancy and clears all related data from the provided person
     *
     * @param person the person to clear the pregnancy data for
     */
    public void removePregnancy(final Person person) {
        person.setDueDate(null);
        person.setExpectedDueDate(null);
        person.getExtraData().set(PREGNANCY_CHILDREN_DATA, null);
        person.getExtraData().set(PREGNANCY_FATHER_DATA, null);
    }

    /**
     * This method is how a mother gives birth to a number of babies and has them added to the campaign.
     *
     * @param campaign the campaign to add the baby in question to
     * @param today    today's date
     * @param mother   the mother giving birth
     */
    public void birth(final Campaign campaign, final LocalDate today, final Person mother) {
        birth(campaign, today, mother, false);
    }

    /**
     * This method is how a mother gives birth to a number of babies and has them added to the campaign.
     *
     * @param campaign             the campaign to add the baby in question to
     * @param today                today's date
     * @param mother               the mother giving birth
     * @param isStartingSimulation true if the birth is part of a new command's simulated history, in which case the
     *                             mother is spared postpartum recovery and no birth announcement is shown
     */
    public void birth(final Campaign campaign, final LocalDate today, final Person mother,
          final boolean isStartingSimulation) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        // Determine the number of children
        final int size = mother.getExtraData().get(PREGNANCY_CHILDREN_DATA, 1);

        // Determine father information
        final Person father = determineFather(campaign, mother);

        // Determine Prisoner Status
        final PrisonerStatus prisonerStatus = PrisonerStatus.FREE;

        // Output a specific report to the campaign if they are giving birth to multiple children
        if (size > 1) {
            campaign.addReport(PERSONNEL, getFormattedTextAt(RESOURCE_BUNDLE,
                  "multipleBabiesBorn.report",
                  mother.getHyperlinkedName(),
                  getFormattedTextAt(RESOURCE_BUNDLE, "babyAmount.text").split(",")[size - 1]));
        }

        // Create Babies. Taken before the loop, so the mother's postpartum recovery isn't counted as hereditary
        final Set<InjuryType> parentInjuryTypes = getParentInjuryTypes(mother, father);

        for (int i = 0; i < size; i++) {
            // Create a baby
            final Person baby = createChild(campaign,
                  today,
                  mother,
                  father,
                  mother.getOriginFaction(),
                  campaign.getPlayerForce().getForceDetachment().getCurrentLocation().getPlanet());
            baby.setOptions(new PersonnelOptions()); // Stop babies being born with SPAs

            inheritBloodGroup(baby, mother, father);

            // Create reports and log the birth
            campaign.addReport(PERSONNEL, getFormattedTextAt(RESOURCE_BUNDLE,
                  "babyBorn.report",
                  mother.getHyperlinkedName(),
                  baby.getHyperlinkedName(),
                  GenderDescriptors.BOY_GIRL.getDescriptor(baby.getGender())));
            logAndUpdateFamily(campaign, today, mother, baby, father);

            // Founder Tag Assignment
            applyFounderStatus(campaignOptions, baby, !prisonerStatus.isCurrentPrisoner());

            // set loyalty
            baby.setLoyalty(Compute.d6(4, 3));

            // Recruit the baby but do not employ the baby. Babies can't have jobs. They don't have object permanence.
            campaign.getPlayerForce().getHumanResources().recruitPerson(campaign, baby, prisonerStatus, true, true, false);

            // if the mother is at school, add the baby to the list of tag along
            if (mother.getStatus().isStudent()) {
                mother.addEduTagAlong(baby.getId());
                if (!baby.getStatus().isCampFollower()) {
                    baby.changeStatus(campaign, today, PersonnelStatus.ON_LEAVE);
                }
            }

            // Apply postpartum effects (first baby only)
            if ((i == 0) && !isStartingSimulation) {
                if (campaignOptions.isUseAdvancedMedical()) {
                    Injury injury;
                    if (campaignOptions.get(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL) &&
                              // These injury types don't stack
                              !AdvancedMedicalAlternate.hasInjuryOfType(mother.getInjuries(),
                                    AlternateInjuries.POSTPARTUM_RECOVERY)) {
                        injury = AlternateInjuries.POSTPARTUM_RECOVERY.newInjury(campaign, mother, GENERIC, 1);
                    } else {
                        injury = InjuryTypes.POSTPARTUM_RECOVERY.newInjury(campaign, mother, INTERNAL, 1);
                    }
                    mother.addInjury(injury);
                } else {
                    int currentHits = mother.getHits();
                    mother.setHits(currentHits + 1);
                }
            }

            // Check for hereditary diseases
            applyHereditaryConditions(campaign, baby, parentInjuryTypes, mother, father);

            // Alert the player, but only show this dialog for the first child with twins/triplets/etc
            if (campaignOptions.get(CampaignOption.SHOW_LIFE_EVENT_DIALOG_BIRTHS) && (i == 0) && !isStartingSimulation) {
                new BirthAnnouncement(campaign, mother, baby.getGender(), size);
            }
        }

        // adjust parents' loyalty
        if (father != null) {
            father.performRandomizedLoyaltyChange(campaign, false, true);
        }

        mother.performRandomizedLoyaltyChange(campaign, false, true);

        // check desire for children
        if (Compute.d6(1) <= 2) {
            mother.setWantsChildren(false);
        }

        // Cleanup Data
        removePregnancy(mother);

        // Return from Maternity leave
        if (mother.getStatus().isOnMaternityLeave()) {
            mother.changeStatus(campaign, today, PersonnelStatus.ACTIVE);
        }
    }

    /**
     * Creates a child with the common traits shared by every way a child can enter the campaign: no skills, no job,
     * no titles, early-childhood education, and a surname derived from their parents.
     *
     * <p>The child is <b>not</b> recruited, linked to their parents, or given loyalty, blood group, or special
     * abilities, as these differ between a live birth, a historic birth, and a child who arrives later in life.</p>
     *
     * <p>At least one parent must be provided. When only one parent is known, they are used as the sole source of the
     * child's surname, regardless of their gender.</p>
     *
     * @param campaign      the campaign the child is being created in
     * @param dateOfBirth   the child's date of birth
     * @param mother        the child's mother, or {@code null} if unknown
     * @param father        the child's father, or {@code null} if unknown
     * @param originFaction the child's origin faction, or {@code null} to pick one at random
     * @param originPlanet  the child's origin planet, or {@code null} to pick one at random
     *
     * @return the new child, who has not yet been added to the campaign
     *
     * @throws IllegalArgumentException if both {@code mother} and {@code father} are {@code null}
     * @author Illiani
     * @since 0.51.01
     */
    protected Person createChild(final Campaign campaign, final LocalDate dateOfBirth, final @Nullable Person mother,
          final @Nullable Person father, final @Nullable Faction originFaction, final @Nullable Planet originPlanet) {
        if (mother == null && father == null) {
            throw new IllegalArgumentException("A child must have at least one known parent");
        }

        final Person child = campaign.getPlayerForce()
                                   .getHumanResources()
                                   .newDependent(campaign, Gender.RANDOMIZE, originFaction, originPlanet);

        // BabySurnameStyle requires a mother. With only one known parent, every style falls back to the parent in
        // that slot, so we put the sole parent there regardless of their gender.
        final BabySurnameStyle surnameStyle = campaign.getCampaignOptions().get(CampaignOption.BABY_SURNAME_STYLE);
        child.setSurname((mother == null) ?
                               surnameStyle.generateBabySurname(father, null, child.getGender()) :
                               surnameStyle.generateBabySurname(mother, father, child.getGender()));

        // Every one of these lines fixes a bug we've had with babies. Who knew children were so good at
        // breaking things?
        child.setDateOfBirth(dateOfBirth); // Make sure we don't have any past or future babies
        child.removeAllSkills(); // Babies don't have skills beyond making a mess and screaming
        child.setPrimaryRole(campaign.getLocalDate(), PersonnelRole.DEPENDENT); // babies can't have jobs
        child.setPreNominal(""); // Stop babies being born with doctorates
        child.setPostNominal(""); // Stop babies being born with post-nominal titles
        child.setEduHighestEducation(EducationLevel.EARLY_CHILDHOOD);

        if (campaign.getCampaignOptions().get(CampaignOption.NO_RANDOM_PORTRAITS_FOR_CHILDREN) &&
                  child.isChild(campaign.getLocalDate(), false)) {
            child.setPortrait(new Portrait());
        }

        return child;
    }

    /**
     * Logs the birth of a baby and updates the genealogy information of the family.
     *
     * <p>The delivery is only logged when the mother is known, as both log entries describe her giving birth.</p>
     *
     * @param campaign the ongoing campaign
     * @param today    the current date
     * @param mother   the mother of the baby, null if unknown
     * @param baby     the newborn baby
     * @param father   the father of the baby, null if unknown
     */
    private static void logAndUpdateFamily(Campaign campaign, LocalDate today, @Nullable Person mother, Person baby,
          @Nullable Person father) {
        if (mother != null && campaign.getCampaignOptions().get(CampaignOption.LOG_PROCREATION)) {
            MedicalLogger.deliveredBaby(mother, baby, today);
            if (father != null) {
                PersonalLogger.ourChildBorn(father, baby, mother.getFullName(), today);
            }
        }

        // Create genealogy information
        linkParentAndChild(mother, baby);
        linkParentAndChild(father, baby);
    }

    /**
     * Records a parent-child relationship in both people's genealogy. Does nothing if the parent is unknown.
     *
     * @param parent the parent, or {@code null} if unknown
     * @param child  the child
     *
     * @author Illiani
     * @since 0.51.01
     */
    protected static void linkParentAndChild(final @Nullable Person parent, final Person child) {
        if (parent == null) {
            return;
        }

        child.getGenealogy().addFamilyMember(FamilialRelationshipType.PARENT, parent);
        parent.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, child);
    }

    /**
     * Gives a child a blood group inherited from their parents. An unknown parent contributes a random blood group.
     *
     * @param child       the child
     * @param parent      one parent, or {@code null} if unknown
     * @param otherParent the other parent, or {@code null} if unknown
     */
    private static void inheritBloodGroup(final Person child, final @Nullable Person parent,
          final @Nullable Person otherParent) {
        child.setBloodGroup(getInheritedBloodGroup(parent == null ? getRandomBloodGroup() : parent.getBloodGroup(),
              otherParent == null ? getRandomBloodGroup() : otherParent.getBloodGroup()));
    }

    /**
     * Collects the injury types currently affecting either parent, so hereditary conditions can be passed on.
     *
     * @param parent      one parent, or {@code null} if unknown
     * @param otherParent the other parent, or {@code null} if unknown
     *
     * @return the combined injury types
     */
    private static Set<InjuryType> getParentInjuryTypes(final @Nullable Person parent,
          final @Nullable Person otherParent) {
        final Set<InjuryType> injuryTypes = new HashSet<>();
        if (parent != null) {
            injuryTypes.addAll(parent.getActiveInjuryTypes());
        }
        if (otherParent != null) {
            injuryTypes.addAll(otherParent.getActiveInjuryTypes());
        }
        return injuryTypes;
    }

    /**
     * Passes hereditary conditions from the parents on to their child.
     *
     * @param campaign          the campaign the child is part of
     * @param child             the child
     * @param parentInjuryTypes the injury types affecting either parent; see
     *                          {@link #getParentInjuryTypes(Person, Person)}
     * @param parent            one parent, or {@code null} if unknown
     * @param otherParent       the other parent, or {@code null} if unknown
     */
    private static void applyHereditaryConditions(final Campaign campaign, final Person child,
          final Set<InjuryType> parentInjuryTypes, final @Nullable Person parent, final @Nullable Person otherParent) {
        if (parentInjuryTypes.contains(KAER_PATHOGEN)) {
            Injury kaerPathogen = KAER_PATHOGEN.newInjury(campaign, child, INTERNAL, 1);
            child.addInjury(kaerPathogen);

            Injury birthDefect = BIRTH_DEFECT.newInjury(campaign, child, INTERNAL, 1);
            child.addInjury(birthDefect);
        }

        if ((parent != null && parent.getOptions().booleanOption(UNOFFICIAL_DOBROWSKI_SYNDROME)) ||
                  (otherParent != null && otherParent.getOptions().booleanOption(UNOFFICIAL_DOBROWSKI_SYNDROME))) {
            child.getOptions().getOption(UNOFFICIAL_DOBROWSKI_SYNDROME).setValue(true);
        }
    }

    /**
     * Assigns the founder tag to a child, and the founder's plot armor where that is enabled.
     *
     * @param campaignOptions the campaign's options
     * @param child           the child, who must already be linked to their parents
     * @param isFreeBirth     {@code true} if the child was born into the force and isn't a prisoner. Only these
     *                        children are eligible for the tag given to all non-prisoner babies.
     */
    private static void applyFounderStatus(final CampaignOptions campaignOptions, final Person child,
          final boolean isFreeBirth) {
        if (campaignOptions.get(CampaignOption.ASSIGN_NON_PRISONER_BABIES_FOUNDER_TAG) && isFreeBirth) {
            child.setFounder(true);
        } else if (campaignOptions.get(CampaignOption.ASSIGN_CHILDREN_OF_FOUNDERS_FOUNDER_TAG)) {
            child.setFounder(child.getGenealogy().getParents().stream().anyMatch(Person::isFounder));
        }

        final boolean useFoundersHavePlotArmor = campaignOptions.get(CampaignOption.USE_FOUNDER_PLOT_ARMOR);
        if (child.isFounder() && useFoundersHavePlotArmor) {
            child.gainEdge(1, campaignOptions.get(CampaignOption.MAXIMUM_EDGE));
        }
    }

    //region Children Joining Later in Life

    /**
     * Determines the earliest date a biological child of the given parent can have been born. Nobody under eighteen
     * can procreate in MekHQ, so this is a full pregnancy after the parent's eighteenth birthday.
     *
     * @param parent the parent
     *
     * @return the earliest possible date of birth for their biological child
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static LocalDate getEarliestChildDateOfBirth(final Person parent) {
        return parent.getDateOfBirth().plusYears(18).plusDays(MHQConstants.PREGNANCY_STANDARD_DURATION);
    }

    /**
     * Determines the oldest a biological child of the given parent can be, capped at
     * {@link #MAXIMUM_ARRIVING_CHILD_AGE}.
     *
     * @param today  the current date
     * @param parent the parent
     *
     * @return the maximum age in years, or {@code -1} if the parent is too young to have any biological child
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int getMaximumBiologicalChildAge(final LocalDate today, final Person parent) {
        final LocalDate earliestDateOfBirth = getEarliestChildDateOfBirth(parent);
        if (earliestDateOfBirth.isAfter(today)) {
            return -1;
        }

        return (int) Math.min(MAXIMUM_ARRIVING_CHILD_AGE, ChronoUnit.YEARS.between(earliestDateOfBirth, today));
    }

    /**
     * Picks a random date of birth for someone who is the given age today.
     *
     * @param today                 the current date
     * @param age                   the age in years the person should be today
     * @param earliestDateOfBirth   the earliest permitted date of birth, or {@code null} for no limit
     *
     * @return the date of birth, which never falls before {@code earliestDateOfBirth}
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static LocalDate getRandomDateOfBirth(final LocalDate today, final int age,
          final @Nullable LocalDate earliestDateOfBirth) {
        // The latest date makes them exactly 'age' today; the earliest is the day before they'd turn 'age' + 1
        final LocalDate latest = today.minusYears(age);
        LocalDate earliest = today.minusYears(age + 1L).plusDays(1);
        if ((earliestDateOfBirth != null) && earliest.isBefore(earliestDateOfBirth)) {
            earliest = earliestDateOfBirth;
        }

        return getRandomDateBetween(earliest, latest);
    }

    /**
     * Picks a random date between two dates, inclusive.
     *
     * @param earliest the earliest date
     * @param latest   the latest date
     *
     * @return the random date, or {@code latest} if {@code earliest} falls after it
     */
    private static LocalDate getRandomDateBetween(final LocalDate earliest, final LocalDate latest) {
        final long span = ChronoUnit.DAYS.between(earliest, latest);
        if (span <= 0) {
            return latest;
        }

        return latest.minusDays(Compute.randomInt((int) span + 1));
    }

    /**
     * Adds a child who was not born into the force: a biological child the parent already had, a child they have
     * just learned about, or a child they have adopted.
     *
     * <p>The child is generated to suit their age, linked to their parent(s), and recruited. Biological children
     * inherit their blood group and any hereditary conditions; adopted children do not. The arrival is reported and,
     * if procreation logging is enabled, logged for each parent.</p>
     *
     * @param campaign    the campaign the child is joining
     * @param today       the current date
     * @param parent      the parent the child is joining
     * @param coParent    the child's other parent, or {@code null} if there is none or they are unknown
     * @param dateOfBirth the child's date of birth. For a biological child, this must not fall before
     *                    {@link #getEarliestChildDateOfBirth(Person)} for either parent.
     * @param arrivalType how the child came to join the family
     *
     * @return the new child, who has been added to the campaign
     *
     * @author Illiani
     * @since 0.51.01
     */
    public Person addChild(final Campaign campaign, final LocalDate today, final Person parent,
          final @Nullable Person coParent, final LocalDate dateOfBirth, final ChildArrivalType arrivalType) {
        final CampaignOptions campaignOptions = campaign.getCampaignOptions();

        // Surname styles are written around a mother, so a female parent fills that slot where there is one
        Person surnameMother = parent;
        Person surnameFather = coParent;
        if ((coParent != null) && !parent.getGender().isFemale() && coParent.getGender().isFemale()) {
            surnameMother = coParent;
            surnameFather = parent;
        }

        // A biological child comes from their parent's home; an adopted child from wherever the force is now
        final Planet originPlanet = arrivalType.isBiological() ?
                                          parent.getOriginPlanet() :
                                          campaign.getPlayerForce()
                                                .getForceDetachment()
                                                .getCurrentLocation()
                                                .getPlanet();
        final Person child = createChild(campaign,
              dateOfBirth,
              surnameMother,
              surnameFather,
              parent.getOriginFaction(),
              originPlanet);

        // Bring the child's education, special abilities, and loyalty in line with their real age
        ForceHumanResources.prepareNonInfantChild(campaign, child);

        if (arrivalType.isBiological()) {
            inheritBloodGroup(child, parent, coParent);
        }

        linkParentAndChild(parent, child);
        linkParentAndChild(coParent, child);

        applyFounderStatus(campaignOptions, child, false);

        // Recruit the child but do not employ them, the same as a newborn
        campaign.getPlayerForce()
              .getHumanResources()
              .recruitPerson(campaign, child, PrisonerStatus.FREE, true, true, false);

        if (arrivalType.isBiological()) {
            applyHereditaryConditions(campaign, child, getParentInjuryTypes(parent, coParent), parent, coParent);
        }

        reportChildArrival(campaign, today, parent, coParent, child, arrivalType);

        // adjust parents' loyalty
        parent.performRandomizedLoyaltyChange(campaign, false, true);
        if (coParent != null) {
            coParent.performRandomizedLoyaltyChange(campaign, false, true);
        }

        // check desire for children
        if (Compute.d6(1) <= 2) {
            parent.setWantsChildren(false);
        }

        return child;
    }

    /**
     * Reports a child joining the family and, if procreation logging is enabled, logs it for each parent.
     *
     * @param campaign    the campaign the child joined
     * @param today       the current date
     * @param parent      the parent the child joined
     * @param coParent    the child's other parent, or {@code null}
     * @param child       the child
     * @param arrivalType how the child came to join the family
     */
    private static void reportChildArrival(final Campaign campaign, final LocalDate today, final Person parent,
          final @Nullable Person coParent, final Person child, final ChildArrivalType arrivalType) {
        final int age = child.getAge(today);
        final String gender = GenderDescriptors.BOY_GIRL.getDescriptor(child.getGender());
        final String descriptor = (age < 1) ?
                                        getFormattedTextAt(RESOURCE_BUNDLE, "childDescriptor.baby.text", gender) :
                                        getFormattedTextAt(RESOURCE_BUNDLE,
                                              "childDescriptor.child.text",
                                              age,
                                              gender);

        final String reportKey = arrivalType.getReportKey();
        if (coParent == null) {
            campaign.addReport(PERSONNEL, getFormattedTextAt(RESOURCE_BUNDLE,
                  reportKey + ".report",
                  parent.getHyperlinkedName(),
                  child.getHyperlinkedName(),
                  descriptor));
        } else {
            campaign.addReport(PERSONNEL, getFormattedTextAt(RESOURCE_BUNDLE,
                  reportKey + ".coParent.report",
                  parent.getHyperlinkedName(),
                  coParent.getHyperlinkedName(),
                  child.getHyperlinkedName(),
                  descriptor));
        }

        if (campaign.getCampaignOptions().get(CampaignOption.LOG_PROCREATION)) {
            PersonalLogger.childJoinedFamily(parent, child, arrivalType, today);
            if (coParent != null) {
                PersonalLogger.childJoinedFamily(coParent, child, arrivalType, today);
            }
        }
    }

    /**
     * Determines whether a person is eligible to randomly learn of a child they didn't know they had.
     *
     * <p>Only unmarried, non-female personnel are eligible: a married man's children arrive through his spouse's
     * pregnancy, and women's children arrive through pregnancy. Otherwise, the same restrictions as random
     * procreation apply, including requiring relationshipless random procreation to be enabled.</p>
     *
     * @param campaign the campaign the person is part of
     * @param today    the current date
     * @param person   the person to check
     *
     * @return {@code true} if they are eligible
     *
     * @author Illiani
     * @since 0.51.01
     */
    boolean isEligibleForRandomPaternity(final Campaign campaign, final LocalDate today, final Person person) {
        if (!campaign.getCampaignOptions().get(CampaignOption.USE_RANDOM_PATERNITY)) {
            return false;
        }

        if (person.getGender().isFemale() || person.getGenealogy().hasSpouse()) {
            return false;
        }

        if (canProcreate(today, person, true) != null) {
            return false;
        }

        return !getEarliestChildDateOfBirth(person).isAfter(today);
    }

    /**
     * Checks whether an eligible person randomly learns of a child they didn't know they had. If they do, the child
     * joins the force, aged anywhere from a newborn up to the campaign's maximum age for these children.
     *
     * <p>The chance uses the same relationshipless random procreation roll as women not in a relationship.</p>
     *
     * @param campaign the campaign the person is part of
     * @param today    the current date
     * @param person   the person to check
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void processRandomPaternityCheck(final Campaign campaign, final LocalDate today, final Person person) {
        if (!isEligibleForRandomPaternity(campaign, today, person) || !procreation(person)) {
            return;
        }

        final int maximumAge = campaign.getCampaignOptions().get(CampaignOption.RANDOM_PATERNITY_MAXIMUM_CHILD_AGE);

        addChild(campaign,
              today,
              person,
              null,
              getRandomDateOfBirthUpToAge(today, maximumAge, getEarliestChildDateOfBirth(person)),
              ChildArrivalType.PATERNITY);
    }

    /**
     * Picks a random date of birth for someone aged anywhere from a newborn up to the given age today.
     *
     * @param today               the current date
     * @param maximumAge          the oldest they can be, capped at {@link #MAXIMUM_ARRIVING_CHILD_AGE}
     * @param earliestDateOfBirth the earliest permitted date of birth, or {@code null} for no limit
     *
     * @return the date of birth, which never falls before {@code earliestDateOfBirth}
     */
    private static LocalDate getRandomDateOfBirthUpToAge(final LocalDate today, final int maximumAge,
          final @Nullable LocalDate earliestDateOfBirth) {
        final int cappedMaximumAge = Math.clamp(maximumAge, 0, MAXIMUM_ARRIVING_CHILD_AGE);

        // Anywhere from today to the day before they'd turn one year older than the maximum age
        LocalDate earliest = today.minusYears(cappedMaximumAge + 1L).plusDays(1);
        if ((earliestDateOfBirth != null) && earliest.isBefore(earliestDateOfBirth)) {
            earliest = earliestDateOfBirth;
        }

        return getRandomDateBetween(earliest, today);
    }

    /**
     * Determines whether a married same-sex couple is eligible to randomly adopt a child, checked from one spouse's
     * side.
     *
     * <p>Both spouses are processed every week, so only the spouse with the lower ID is eligible. This stops the
     * couple getting two rolls a week. Both spouses must otherwise be able to procreate, using the same restrictions
     * as random procreation, apart from the requirement that they can conceive together.</p>
     *
     * @param campaign the campaign the couple is part of
     * @param today    the current date
     * @param person   the spouse to check from
     *
     * @return {@code true} if the couple is eligible
     *
     * @author Illiani
     * @since 0.51.01
     */
    boolean isEligibleForRandomAdoption(final Campaign campaign, final LocalDate today, final Person person) {
        if (!campaign.getCampaignOptions().get(CampaignOption.USE_RANDOM_SAME_SEX_ADOPTION)) {
            return false;
        }

        final Person spouse = person.getGenealogy().getSpouse();
        if (spouse == null) {
            return false;
        }

        // Opposite-sex couples have children through pregnancy instead
        if (person.getGender().isFemale() != spouse.getGender().isFemale()) {
            return false;
        }

        if (person.getId().compareTo(spouse.getId()) > 0) {
            return false;
        }

        // The non-random checks cover both spouses' own circumstances. The random checks would refuse a woman with a
        // wife outright, so the restrictions specific to random procreation are applied separately.
        if ((canProcreate(today, person, false) != null) || (canProcreate(today, spouse, false) != null)) {
            return false;
        }

        return !isBlockedFromRandomProcreation(person) && !isBlockedFromRandomProcreation(spouse);
    }

    /**
     * Checks the restrictions that only apply to random procreation: those on clan personnel and prisoners.
     *
     * @param person the person to check
     *
     * @return {@code true} if random procreation is disabled for them
     */
    private boolean isBlockedFromRandomProcreation(final Person person) {
        return (!isUseRandomClanPersonnelProcreation() && person.isClanPersonnel()) ||
                     (!isUseRandomPrisonerProcreation() && person.getPrisonerStatus().isCurrentPrisoner());
    }

    /**
     * Checks whether an eligible married same-sex couple randomly adopts a child. If they do, the child joins the
     * force, aged anywhere from a newborn up to the campaign's maximum age for adopted children.
     *
     * <p>The chance uses the same random procreation roll as women in a relationship.</p>
     *
     * @param campaign the campaign the couple is part of
     * @param today    the current date
     * @param person   the spouse to check from
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void processRandomAdoptionCheck(final Campaign campaign, final LocalDate today, final Person person) {
        if (!isEligibleForRandomAdoption(campaign, today, person) || !procreation(person)) {
            return;
        }

        final int maximumAge = campaign.getCampaignOptions().get(CampaignOption.RANDOM_ADOPTION_MAXIMUM_CHILD_AGE);

        addChild(campaign,
              today,
              person,
              person.getGenealogy().getSpouse(),
              getRandomDateOfBirthUpToAge(today, maximumAge, null),
              ChildArrivalType.ADOPTION);
    }
    //endregion Children Joining Later in Life

    /**
     * Creates baby/babies and performs any necessary operations such as setting birthdate, creating reports, updating
     * genealogy, setting education, loyalty, personality, and recruiting the baby. This version is for historic births
     * that occur as part of a character's background.
     *
     * @param campaign the campaign object
     * @param today    the current date
     * @param mother   the mother person object
     * @param father   the father person object, can be null if the father is unknown
     *
     * @return the babies
     */
    public List<Person> birthHistoric(final Campaign campaign, final LocalDate today, final Person mother,
          @Nullable final Person father) {
        List<Person> babies = new ArrayList<>();

        // Determine the number of children
        final int size = mother.getExtraData().get(PREGNANCY_CHILDREN_DATA, 1);
        // Create Babies
        for (int i = 0; i < size; i++) {
            // Create the babies
            Faction originFaction = mother.getOriginFaction();
            Planet originPlanet = mother.getOriginPlanet();

            // Even chance the child takes the father's origin instead of the mother's
            if (father != null && Compute.randomInt(2) == 0) {
                originFaction = father.getOriginFaction();
                originPlanet = father.getOriginPlanet();
            }

            final Person baby = createChild(campaign, mother.getDueDate(), mother, father, originFaction, originPlanet);

            // re-roll SPAs to include in any age and skill adjustments
            Enumeration<IOption> options = new PersonnelOptions().getOptions(PersonnelOptions.LVL3_ADVANTAGES);

            for (IOption option : Collections.list(options)) {
                baby.getOptions().getOption(option.getName()).clearValue();
            }

            baby.setLoyalty(Compute.d6(3) + 2);

            // Create reports and log the birth
            logAndUpdateFamily(campaign, today, mother, baby, father);

            // add to the list of babies
            babies.add(baby);
        }

        if (Compute.d6(1) <= 2) {
            mother.setWantsChildren(false);
        }

        // Cleanup Data
        removePregnancy(mother);

        return babies;
    }

    /**
     * Resolves the pregnancy of a background character, with the babies born on the mother's due date. See
     * {@link #birthHistoric(Campaign, LocalDate, Person, Person)}.
     *
     * @param campaign the campaign object
     * @param mother   the mother giving birth
     *
     * @return the babies, which have not yet been added to the campaign
     *
     * @author Illiani
     * @since 0.51.01
     */
    public List<Person> birthBackground(final Campaign campaign, final Person mother) {
        return birthHistoric(campaign, mother.getDueDate(), mother, determineFather(campaign, mother));
    }

    /**
     * This is used to process procreation when a person dies with the Pregnancy Complications status
     *
     * @param campaign the campaign to add the baby to
     * @param today    the current date
     * @param person   the person to process
     */
    public void processPregnancyComplications(final Campaign campaign, final LocalDate today, final Person person) {
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
     *
     * @return the chance of a baby being born, ranging from 0.0 to 1.0
     */
    private static double getBabyBornChance(int pregnancyWeek) {
        int range = switch (pregnancyWeek) {
            case 23 -> 1;
            case 24 -> 2;
            case 25 -> 3;
            default -> (pregnancyWeek > 25 && pregnancyWeek <= 29) ?
                             4 :
                             (pregnancyWeek > 29 && pregnancyWeek <= 35) ? 5 : (pregnancyWeek > 35) ? 6 : 0;
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
     * Process new week procreation for an individual
     *
     * @param campaign the campaign to process
     * @param today    the current day
     * @param person   the person to process
     */
    public void processNewWeek(final Campaign campaign, final LocalDate today, final Person person) {
        processNewWeek(campaign, today, person, false);
    }

    /**
     * Process new week procreation for an individual
     *
     * @param campaign             the campaign to process
     * @param today                the current day
     * @param person               the person to process
     * @param isStartingSimulation true if this week is part of a new command's simulated history; see
     *                             {@link #birth(Campaign, LocalDate, Person, boolean)}
     */
    public void processNewWeek(final Campaign campaign, final LocalDate today, final Person person,
          final boolean isStartingSimulation) {
        // Male personnel can't become pregnant, but may learn of a child they didn't know they had, or adopt one
        // with their husband
        if (person.getGender().isMale()) {
            processRandomPaternityCheck(campaign, today, person);
            processRandomAdoptionCheck(campaign, today, person);
            return;
        }

        // Check if they are already pregnant
        if (person.isPregnant()) {
            // They give birth if the due date has passed
            if ((today.isAfter(person.getDueDate())) || (today.isEqual(person.getDueDate()))) {
                birth(campaign, today, person, isStartingSimulation);

                return;
            }

            if (campaign.getCampaignOptions().get(CampaignOption.USE_MATERNITY_LEAVE) &&
                      !person.isBlockMaternityLeave() &&
                      !isStartingSimulation) {
                if (!person.isBusy()
                          && person.getStatus().isActive()
                          && person.getNonPermanentInjurySeverity() == 0
                          && (person.getDueDate().minusWeeks(20).isBefore(today))) {
                    person.changeStatus(campaign, today, PersonnelStatus.ON_MATERNITY_LEAVE);
                }
            }

            return;
        }

        // Make the required checks for random procreation
        processRandomProcreationCheck(campaign, today, person, false);

        // Women married to women can't conceive together, but may adopt
        processRandomAdoptionCheck(campaign, today, person);
    }

    /**
     * Checks if a person randomly procreates on the given day in the campaign. If the person does procreate, add a
     * pregnancy to the campaign for the person.
     *
     * @param campaign   The campaign to check procreation for.
     * @param today      The current date.
     * @param person     The person to check for procreation.
     * @param isNoReport true, if the player shouldn't be informed, otherwise false
     */
    public void processRandomProcreationCheck(final Campaign campaign, final LocalDate today, final Person person,
          boolean isNoReport) {
        if (randomlyProcreates(today, person)) {
            addPregnancy(campaign, today, person, isNoReport);
        }
    }

    //region Random Procreation

    /**
     * Determines if a non-pregnant woman procreates on a given day
     *
     * @param today  the current day
     * @param person the person in question
     *
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
     *
     * @param person the person to determine for
     *
     * @return true if they do, otherwise false
     */
    protected abstract boolean procreation(Person person);
}
