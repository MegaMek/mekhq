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
import mekhq.campaign.personnel.enums.RandomMarriageMethod;

import java.time.LocalDate;
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
    public @Nullable String canMarry(final Campaign campaign, final LocalDate today,
                                     final Person person, final boolean randomMarriage) {
        if (!person.isMarriageable()) {
            return resources.getString("cannotMarry.NotMarriageable.text");
        } else if (person.getGenealogy().hasSpouse()) {
            return resources.getString("cannotMarry.AlreadyMarried.text");
        } else if (!person.getStatus().isActive()) {
            return resources.getString("cannotMarry.Inactive.text");
        } else if (person.isDeployed()) {
            return resources.getString("cannotMarry.Deployed.text");
        } else if (person.getAge(today) < campaign.getCampaignOptions().getMinimumMarriageAge()) {
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
                || (canMarry(campaign, today, potentialSpouse, randomMarriage) != null)
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
     */
    public void marry(final Campaign campaign, final LocalDate today, final Person origin,
                      final @Nullable Person spouse, final MergingSurnameStyle surnameStyle) {
        if (spouse == null) {
            return;
        }

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

        campaign.addReport(String.format(resources.getString("marriage.report"), origin.getHyperlinkedName(),
                spouse.getHyperlinkedName()));

        // Process the loyalty change
        if (campaign.getCampaignOptions().isUseLoyaltyModifiers()) {
            origin.performRandomizedLoyaltyChange(campaign, false, true);
            spouse.performRandomizedLoyaltyChange(campaign, false, true);
        }

        // log the origin spouse for both partners
        origin.getGenealogy().setOriginSpouse(spouse);
        spouse.getGenealogy().setOriginSpouse(origin);

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
        if (canMarry(campaign, today, person, true) != null) {
            return;
        }

        if (randomMarriage(person)) {
            marryRandomSpouse(campaign, today, person, false);
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
     * @param campaign the campaign the person is a part of
     * @param today the current date
     * @param person the person who is getting randomly married
     * @param sameSex whether the marriage is between same-sex partners
     */
    protected void marryRandomSpouse(final Campaign campaign, final LocalDate today,
                                     final Person person, final boolean sameSex) {
        final Gender gender = sameSex ? person.getGender() : (person.getGender().isMale() ? Gender.FEMALE : Gender.MALE);
        final List<Person> potentials = campaign.getActivePersonnel().stream()
                .filter(potentialSpouse -> isPotentialRandomSpouse(campaign, today, person, potentialSpouse, gender))
                .toList();
        if (!potentials.isEmpty()) {
            marry(campaign, today, person, potentials.get(Compute.randomInt(potentials.size())),
                    MergingSurnameStyle.WEIGHTED);
        }
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
