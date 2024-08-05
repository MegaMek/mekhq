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
package mekhq.campaign.personnel.marriage;

import megamek.client.generator.RandomGenderGenerator;
import megamek.common.Compute;
import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.event.PersonChangedEvent;
import mekhq.campaign.log.PersonalLogger;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.MergingSurnameStyle;
import mekhq.campaign.personnel.enums.PrisonerStatus;
import mekhq.campaign.personnel.enums.RandomMarriageMethod;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * AbstractMarriage is the baseline class for marriage in MekHQ. It holds all the common logic for
 * marriages, and is implemented by classes defining how to determine if a person will randomly
 * marry on a given day.
 */
public abstract class AbstractMarriage {
    //region Variable Declarations
    private final RandomMarriageMethod method;
    private boolean useClanPersonnelMarriages;
    private boolean usePrisonerMarriages;
    private boolean useRandomClanPersonnelMarriages;
    private boolean useRandomPrisonerMarriages;

    private static final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    //region Constructors
    protected AbstractMarriage(final RandomMarriageMethod method, final CampaignOptions options) {
        this.method = method;
        setUseClanPersonnelMarriages(options.isUseClanPersonnelMarriages());
        setUsePrisonerMarriages(options.isUsePrisonerMarriages());
        setUseRandomClanPersonnelMarriages(options.isUseRandomClanPersonnelMarriages());
        setUseRandomPrisonerMarriages(options.isUseRandomPrisonerMarriages());
    }
    //endregion Constructors

    //region Getters/Setters
    @SuppressWarnings(value = "unused")
    public RandomMarriageMethod getMethod() {
        return method;
    }

    @SuppressWarnings(value = "unused")
    public boolean isUseClanPersonnelMarriages() {
        return useClanPersonnelMarriages;
    }

    @SuppressWarnings(value = "unused")
    public void setUseClanPersonnelMarriages(final boolean useClanPersonnelMarriages) {
        this.useClanPersonnelMarriages = useClanPersonnelMarriages;
    }

    @SuppressWarnings(value = "unused")
    public boolean isUsePrisonerMarriages() {
        return usePrisonerMarriages;
    }

    @SuppressWarnings(value = "unused")
    public void setUsePrisonerMarriages(final boolean usePrisonerMarriages) {
        this.usePrisonerMarriages = usePrisonerMarriages;
    }

    @SuppressWarnings(value = "unused")
    public boolean isUseRandomClanPersonnelMarriages() {
        return useRandomClanPersonnelMarriages;
    }

    @SuppressWarnings(value = "unused")
    public void setUseRandomClanPersonnelMarriages(final boolean useRandomClanPersonnelMarriages) {
        this.useRandomClanPersonnelMarriages = useRandomClanPersonnelMarriages;
    }

    @SuppressWarnings(value = "unused")
    public boolean isUseRandomPrisonerMarriages() {
        return useRandomPrisonerMarriages;
    }

    @SuppressWarnings(value = "unused")
    public void setUseRandomPrisonerMarriages(final boolean useRandomPrisonerMarriages) {
        this.useRandomPrisonerMarriages = useRandomPrisonerMarriages;
    }
    //endregion Getters/Setters

    /**
     * This is used to determine if a person can marry
     * @param campaign the campaign the person is a part of
     * @param today the current date
     * @param person the person to determine for
     * @param randomMarriage if this is for random marriage or manual marriage
     * @return null if they can, otherwise the reason why they cannot
     */
    public @Nullable String canMarry(final LocalDate today, final Person person, final boolean randomMarriage) {
        if (!person.isMarriageable()) {
            return resources.getString("cannotMarry.NotMarriageable.text");
        } else if (person.getGenealogy().hasSpouse()) {
            return resources.getString("cannotMarry.AlreadyMarried.text");
        } else if (!person.getStatus().isActive()) {
            return resources.getString("cannotMarry.Inactive.text");
        } else if (person.isDeployed()) {
            return resources.getString("cannotMarry.Deployed.text");
        } else if (person.isChild(today)) {
            return resources.getString("cannotMarry.TooYoung.text");
        } else if (!isUseClanPersonnelMarriages() && person.isClanPersonnel()) {
            return resources.getString("cannotMarry.ClanPersonnel.text");
        } else if (!isUsePrisonerMarriages() && person.getPrisonerStatus().isCurrentPrisoner()) {
            return resources.getString("cannotMarry.Prisoner.text");
        } else if (randomMarriage) {
            if (!isUseRandomClanPersonnelMarriages() && person.isClanPersonnel()) {
                return resources.getString("cannotMarry.RandomClanPersonnel.text");
            } else if (!isUseRandomPrisonerMarriages() && person.getPrisonerStatus().isCurrentPrisoner()) {
                return resources.getString("cannotMarry.RandomPrisoner.text");
            }
        }

        return null;
    }

    /**
     * Determines if the potential spouse is a safe spouse for a person.
     * @param campaign the campaign to check using
     * @param today the current day
     * @param person the person trying to marry
     * @param potentialSpouse the person to determine if they are a safe spouse
     * @return true if the potential spouse is a safe spouse for the provided person
     */
    public boolean safeSpouse(final Campaign campaign, final LocalDate today, final Person person,
                              final Person potentialSpouse, final boolean randomMarriage) {
        // Can't marry yourself
        // Can't marry someone who can't currently marry
        // Can't marry a close relative
        // TODO : GitHub #1672 : can't marry anyone who is not located at the same planet as the person
        // Prisoners are based on whether it is for random marriage or not. You can manually marry
        // a prisoner to any member of the force, but cannot the opposite way. However, random
        // marriages are limited to non-prisoner to non-prisoner marriages and prisoner to prisoner
        // marriages.

        if (person.equals(potentialSpouse)
                || (canMarry(today, potentialSpouse, randomMarriage) != null)
                || person.getGenealogy().checkMutualAncestors(potentialSpouse,
                        campaign.getCampaignOptions().getCheckMutualAncestorsDepth())) {
            return false;
        } else if (randomMarriage) {
            return person.getPrisonerStatus().isCurrentPrisoner() == potentialSpouse.getPrisonerStatus().isCurrentPrisoner();
        } else {
            return !potentialSpouse.getPrisonerStatus().isCurrentPrisoner() || person.getPrisonerStatus().isCurrentPrisoner();
        }
    }

    /**
     * This marries two people that are part of the same campaign together on the given date.
     * @param campaign the campaign the two people are a part of
     * @param today the current date
     * @param origin the origin person being married
     * @param spouse the person's spouse, which can be null if no marriage is to occur
     * @param surnameStyle the style for how the two people's surnames will change as part of the marriage
     * @param isBackground whether the marriage occurred as part of a character's background
     */
    public void marry(final Campaign campaign, final LocalDate today, final Person origin,
                      final @Nullable Person spouse, final MergingSurnameStyle surnameStyle,
                      boolean isBackground) {
        if (spouse == null) {
            return;
        }

        performMarriageChanges(campaign, today, origin, spouse, surnameStyle, isBackground);

        // And finally, we trigger person changed events
        MekHQ.triggerEvent(new PersonChangedEvent(origin));
        MekHQ.triggerEvent(new PersonChangedEvent(spouse));
    }

    /**
     * Updates the necessary information to perform a marriage between two individuals.
     *
     * @param campaign the campaign in which the marriage is taking place
     * @param today the current date of the marriage
     * @param origin the first person getting married
     * @param spouse the second person getting married
     * @param surnameStyle the style of surname changes to be applied
     * @param isBackground whether the marriage occurred as part of a character's background
     */
    public static void performMarriageChanges(Campaign campaign, LocalDate today, Person origin,
                                              Person spouse, MergingSurnameStyle surnameStyle,
                                              boolean isBackground) {
        // Immediately set both Maiden Names, to avoid any divorce bugs (as the default is now an empty string)
        origin.setMaidenName(origin.getSurname());
        spouse.setMaidenName(spouse.getSurname());

        // Then add them as spouses
        origin.getGenealogy().setSpouse(spouse);
        spouse.getGenealogy().setSpouse(origin);

        // Apply the surname style changes
        surnameStyle.apply(campaign, today, origin, spouse);

        // Do the logging
        PersonalLogger.marriage(origin, spouse, today);
        PersonalLogger.marriage(spouse, origin, today);

        if (!isBackground) {
            campaign.addReport(String.format(resources.getString("marriage.report"),
                    origin.getHyperlinkedName(),
                    spouse.getHyperlinkedName()));

            // Process the loyalty change
            if (campaign.getCampaignOptions().isUseLoyaltyModifiers()) {
                origin.performRandomizedLoyaltyChange(campaign, false, true);
                spouse.performRandomizedLoyaltyChange(campaign, false, true);
            }
        }

        // log the origin spouse for both partners
        origin.getGenealogy().setOriginSpouse(origin);
        spouse.getGenealogy().setOriginSpouse(origin);

        // recruit the spouse if they're not already in the unit
        if ((!isBackground) && (spouse.getJoinedCampaign() == null)) {
            campaign.recruitPerson(spouse, PrisonerStatus.FREE, true, false);

            ResourceBundle recruitmentResources = ResourceBundle.getBundle("mekhq.resources.Campaign",
                    MekHQ.getMHQOptions().getLocale());

            campaign.addReport(String.format(recruitmentResources.getString("dependentJoinsForce.text"),
                    spouse.getHyperlinkedFullTitle()));
        }

        // And finally, we trigger person changed events
        MekHQ.triggerEvent(new PersonChangedEvent(origin));
        MekHQ.triggerEvent(new PersonChangedEvent(spouse));
    }

    //region New Day
    /**
     * Processes new day random marriage for an individual.
     * @param campaign the campaign to process
     * @param today the current day
     * @param person the person to process
     */
    public void processNewWeek(final Campaign campaign, final LocalDate today, final Person person) {
        if (canMarry(today, person, true) != null) {
            return;
        }

        if (randomMarriage(person)) {
            boolean isSameSex = false;

            int sameSexDiceSize = campaign.getCampaignOptions().getRandomSameSexMarriageDiceSize();

            if (sameSexDiceSize == 1) {
                isSameSex = true;
            } else if ((sameSexDiceSize != 0) && (Compute.randomInt(sameSexDiceSize) == 0)) {
                isSameSex = true;
            }

            boolean isInterUnit = false;

            int interUnitDiceSize = campaign.getCampaignOptions().getRandomNewDependentMarriage();

            if (interUnitDiceSize == 1) {
                isInterUnit = true;
            } else if ((interUnitDiceSize != 0) && (Compute.randomInt(interUnitDiceSize) == 0)) {
                isInterUnit = true;
            }

            marryRandomSpouse(campaign, today, person, isSameSex, isInterUnit, true);
        }
    }

    /**
     * This method is used to check for marriages that occurred in a character's background
     *
     * @param campaign the campaign for which to process the marriage rolls
     * @param today the current date
     * @param person the person for whom to process the marriage rolls
     */
    public void processBackgroundMarriageRolls(final Campaign campaign, final LocalDate today, final Person person) {
        if (canMarry(today, person, true) != null) {
            return;
        }

        if (randomMarriage(person)) {
            boolean isSameSex = false;

            int sameSexDiceSize = campaign.getCampaignOptions().getRandomSameSexMarriageDiceSize();

            if (sameSexDiceSize == 1) {
                isSameSex = true;
            } else if ((sameSexDiceSize != 0) && (Compute.randomInt(sameSexDiceSize) == 0)) {
                isSameSex = true;
            }

            marryRandomSpouse(campaign, today, person, isSameSex, false, true);
        }
    }

    //region Random Marriage
    /**
     * This determines if a person will randomly marry an opposite sex spouse.
     * @param person the person to determine if they are getting randomly married
     * @return true if the person is to randomly marry
     */
    protected abstract boolean randomMarriage(final Person person);

    /**
     * This finds a random spouse and marries them to the provided person.
     *
     * @param campaign    the campaign the person is a part of
     * @param today       the current date
     * @param person      the person who is getting randomly married
     * @param sameSex     whether the marriage is between same-sex partners
     * @param isInterUnit whether the marriage is to another character chosen from among potential partners already in the campaign unit.
     * @param isBackground whether the marriage occurred in a character's background
     */
    protected void marryRandomSpouse(final Campaign campaign, final LocalDate today,
                                     final Person person, final boolean sameSex,
                                     boolean isInterUnit, boolean isBackground) {
        Gender personGender = person.getGender();

        boolean isNonBinary = (campaign.getCampaignOptions().getNonBinaryDiceSize() > 0)
                && (Compute.randomInt(campaign.getCampaignOptions().getNonBinaryDiceSize()) == 0);

        Gender spouseGender = switch (personGender) {
            case MALE, OTHER_MALE -> sameSex ? (isNonBinary ?
                    Gender.OTHER_MALE : Gender.MALE) : (isNonBinary ? Gender.OTHER_FEMALE : Gender.FEMALE);
            case FEMALE, OTHER_FEMALE -> sameSex ? (isNonBinary ?
                    Gender.OTHER_FEMALE : Gender.FEMALE) : (isNonBinary ? Gender.OTHER_MALE : Gender.MALE);
            case RANDOMIZE -> RandomGenderGenerator.generate();
        };

        List<Person> potentialSpouses = new ArrayList<>();
        Person spouse = null;

        if (isInterUnit) {
            potentialSpouses = campaign.getActivePersonnel().stream()
                    .filter(potentialSpouse -> isPotentialRandomSpouse(campaign, today, person, potentialSpouse, spouseGender))
                    .toList();

            if (!potentialSpouses.isEmpty()) {
                spouse = potentialSpouses.get(Compute.randomInt(potentialSpouses.size()));
            }
        }

        if ((!isInterUnit) || (potentialSpouses.isEmpty())) {
            spouse = createExternalSpouse(campaign, today, person, spouseGender);
        }

        marry(campaign, today, person, spouse, MergingSurnameStyle.WEIGHTED, isBackground);
    }

    /**
     * Creates a spouse for the given person.
     *
     * @param campaign the campaign the person is a part of
     * @param today the current date
     * @param person the person for whom the external spouse is being created
     * @param gender the gender of the external spouse
     * @return the created external spouse
     */
    Person createExternalSpouse(final Campaign campaign, final LocalDate today, final Person person, Gender gender) {
        Person externalSpouse = campaign.newDependent(false, gender);


        // Calculate person's age and the maximum and minimum allowable spouse ages
        int personAge = person.getAge(today);
        int externalSpouseAge = externalSpouse.getAge(today);
        int maximumAgeDifference = campaign.getCampaignOptions().getRandomMarriageAgeRange();
        int externalSpouseMinAge = Math.max (18, personAge - maximumAgeDifference);
        int externalSpouseMaxAge = personAge + maximumAgeDifference;

        if (externalSpouseAge < externalSpouseMinAge) {
            int difference = externalSpouseMinAge - externalSpouseAge;

            externalSpouse.setBirthday(externalSpouse.getBirthday().minusYears(difference));
        } else if (externalSpouseAge > externalSpouseMaxAge) {
            int difference = externalSpouseMaxAge - externalSpouseAge;

            externalSpouse.setBirthday(externalSpouse.getBirthday().plusYears(difference));
        }

        return externalSpouse;
    }

    /**
     * Determines if a person is a valid potential random spouse for the person being randomly
     * married.
     *
     * @param campaign the campaign the two people are a part of
     * @param today the current day
     * @param person the person who is trying to find a random spouse
     * @param potentialSpouse the person to determine if they are a valid potential random spouse
     * @param gender the desired gender to be married to
     * @return true if they are a valid potential random spouse
     */
    protected boolean isPotentialRandomSpouse(final Campaign campaign, final LocalDate today,
                                              final Person person, final Person potentialSpouse,
                                              final Gender gender) {
        // A Potential Spouse must:
        // 1. Be the specified gender
        // 2. Be a safe spouse for the current person
        // 3. Be within the random marriage age range
        if ((potentialSpouse.getGender() != gender)
                || !safeSpouse(campaign, today, person, potentialSpouse, true)) {
            return false;
        }

        final int ageDifference = Math.abs(potentialSpouse.getAge(today) - person.getAge(today));
        return ageDifference <= campaign.getCampaignOptions().getRandomMarriageAgeRange();
    }
    //endregion Random Marriage
    //endregion New Day
}
