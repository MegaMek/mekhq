/*
 * Copyright (c) 2021-2022 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.divorce;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.event.PersonChangedEvent;
import mekhq.campaign.log.PersonalLogger;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.FormerSpouseReason;
import mekhq.campaign.personnel.enums.RandomDivorceMethod;
import mekhq.campaign.personnel.enums.SplittingSurnameStyle;
import mekhq.campaign.personnel.familyTree.FormerSpouse;

import java.time.LocalDate;
import java.util.ResourceBundle;

/**
 * AbstractDivorce is the baseline class for divorce in MekHQ. It holds all the common logic for
 * divorces, and is implemented by classes defining how to determine if a person will randomly
 * divorce on a given day.
 *
 * TODO : Decouple widowing, which should be part of the death module instead.
 */
public abstract class AbstractDivorce {
    //region Variable Declarations
    private final RandomDivorceMethod method;
    private boolean useClanPersonnelDivorce;
    private boolean usePrisonerDivorce;
    private boolean useRandomOppositeSexDivorce;
    private boolean useRandomSameSexDivorce;
    private boolean useRandomClanPersonnelDivorce;
    private boolean useRandomPrisonerDivorce;

    private static final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    //region Constructors
    protected AbstractDivorce(final RandomDivorceMethod method, final CampaignOptions options) {
        this.method = method;
        setUseClanPersonnelDivorce(options.isUseClanPersonnelDivorce());
        setUsePrisonerDivorce(options.isUsePrisonerDivorce());
        setUseRandomOppositeSexDivorce(options.isUseRandomOppositeSexDivorce());
        setUseRandomSameSexDivorce(options.isUseRandomSameSexDivorce());
        setUseRandomClanPersonnelDivorce(options.isUseRandomClanPersonnelDivorce());
        setUseRandomPrisonerDivorce(options.isUseRandomPrisonerDivorce());
    }
    //endregion Constructors

    //region Getters/Setters
    public RandomDivorceMethod getMethod() {
        return method;
    }

    public boolean isUseClanPersonnelDivorce() {
        return useClanPersonnelDivorce;
    }

    public void setUseClanPersonnelDivorce(final boolean useClanPersonnelDivorce) {
        this.useClanPersonnelDivorce = useClanPersonnelDivorce;
    }

    public boolean isUsePrisonerDivorce() {
        return usePrisonerDivorce;
    }

    public void setUsePrisonerDivorce(final boolean usePrisonerDivorce) {
        this.usePrisonerDivorce = usePrisonerDivorce;
    }

    public boolean isUseRandomOppositeSexDivorce() {
        return useRandomOppositeSexDivorce;
    }

    public void setUseRandomOppositeSexDivorce(final boolean useRandomOppositeSexDivorce) {
        this.useRandomOppositeSexDivorce = useRandomOppositeSexDivorce;
    }

    public boolean isUseRandomSameSexDivorce() {
        return useRandomSameSexDivorce;
    }

    public void setUseRandomSameSexDivorce(final boolean useRandomSameSexDivorce) {
        this.useRandomSameSexDivorce = useRandomSameSexDivorce;
    }

    public boolean isUseRandomClanPersonnelDivorce() {
        return useRandomClanPersonnelDivorce;
    }

    public void setUseRandomClanPersonnelDivorce(final boolean useRandomClanPersonnelDivorce) {
        this.useRandomClanPersonnelDivorce = useRandomClanPersonnelDivorce;
    }

    public boolean isUseRandomPrisonerDivorce() {
        return useRandomPrisonerDivorce;
    }

    public void setUseRandomPrisonerDivorce(final boolean useRandomPrisonerDivorce) {
        this.useRandomPrisonerDivorce = useRandomPrisonerDivorce;
    }
    //endregion Getters/Setters

    /**
     * This is used to determine if a person can divorce.
     * @param person the person to determine for
     * @param randomDivorce if this is for random divorce or manual divorce
     * @return null if they can, otherwise the reason they cannot
     */
    public @Nullable String canDivorce(final Person person, final boolean randomDivorce) {
        if (!person.getGenealogy().hasSpouse()) {
            return resources.getString("cannotDivorce.NotMarried.text");
        } else if (!person.isDivorceable()) {
            return resources.getString("cannotDivorce.NotDivorceable.text");
        } else if (!person.getGenealogy().getSpouse().isDivorceable()) {
            return resources.getString("cannotDivorce.SpouseNotDivorceable.text");
        } else if (!isUseClanPersonnelDivorce() && person.isClanPersonnel()) {
            return resources.getString("cannotDivorce.ClanPersonnel.text");
        } else if (!isUseClanPersonnelDivorce() && person.getGenealogy().getSpouse().isClanPersonnel()) {
            return resources.getString("cannotDivorce.ClanPersonnelSpouse.text");
        } else if (!isUsePrisonerDivorce() && person.getPrisonerStatus().isCurrentPrisoner()) {
            return resources.getString("cannotDivorce.Prisoner.text");
        } else if (!isUsePrisonerDivorce() && person.getGenealogy().getSpouse().getPrisonerStatus().isCurrentPrisoner()) {
            return resources.getString("cannotDivorce.PrisonerSpouse.text");
        } else if (randomDivorce) {
            if (!isUseRandomClanPersonnelDivorce() && person.isClanPersonnel()) {
                return resources.getString("cannotDivorce.RandomClanPersonnel.text");
            } else if (!isUseRandomClanPersonnelDivorce() && person.getGenealogy().getSpouse().isClanPersonnel()) {
                return resources.getString("cannotDivorce.RandomClanPersonnelSpouse.text");
            } else if (!isUseRandomPrisonerDivorce() && person.getPrisonerStatus().isCurrentPrisoner()) {
                return resources.getString("cannotDivorce.RandomPrisoner.text");
            } else if (!isUseRandomPrisonerDivorce() && person.getGenealogy().getSpouse().getPrisonerStatus().isCurrentPrisoner()) {
                return resources.getString("cannotDivorce.RandomPrisonerSpouse.text");
            }
            final boolean sameSex = person.getGenealogy().getSpouse().getGender() == person.getGender();
            if (!isUseRandomOppositeSexDivorce() && !sameSex) {
                return resources.getString("cannotDivorce.OppositeSexDivorceDisabled.text");
            } else if (!isUseRandomSameSexDivorce() && sameSex) {
                return resources.getString("cannotDivorce.SameSexDivorceDisabled.text");
            }
        }

        return null;
    }

    /**
     * This is a standardization method for the divorce surname style to use when a person's spouse
     * dies.
     *
     * TODO : I should be part of AbstractDeath
     *
     * @param campaign the campaign the person is in
     * @param today the current day
     * @param person the person whose spouse has died
     */
    public void widowed(final Campaign campaign, final LocalDate today, final Person person) {
        divorce(campaign, today, person, campaign.getCampaignOptions().isKeepMarriedNameUponSpouseDeath()
                ? SplittingSurnameStyle.BOTH_KEEP_SURNAME : SplittingSurnameStyle.ORIGIN_CHANGES_SURNAME);
    }

    /**
     * This divorces two married people
     * @param campaign the campaign the two people are a part of
     * @param today the current date
     * @param origin the origin person being divorced
     * @param style the style for how the person and their spouse's surnames will change as part of
     *              the divorce
     */
    public void divorce(final Campaign campaign, final LocalDate today, final Person origin,
                        final SplittingSurnameStyle style) {
        final Person spouse = origin.getGenealogy().getSpouse();

        style.apply(campaign, origin, spouse);

        final FormerSpouseReason reason;

        if (spouse.getStatus().isDeadOrMIA() == origin.getStatus().isDeadOrMIA()) {
            reason = FormerSpouseReason.DIVORCE;

            PersonalLogger.divorcedFrom(origin, spouse, today);

            if (origin.getStatus().isDead()) {
                PersonalLogger.widowedBy(spouse, origin, today);

                campaign.addReport(String.format(resources.getString("widowed.report"),
                        origin.getHyperlinkedName(), spouse.getHyperlinkedName()));
            } else {
                PersonalLogger.divorcedFrom(spouse, origin, today);

                campaign.addReport(String.format(resources.getString("divorce.report"),
                        origin.getHyperlinkedName(), spouse.getHyperlinkedName()));
            }

            spouse.setMaidenName(null);
            origin.setMaidenName(null);

            spouse.getGenealogy().setSpouse(null);
            origin.getGenealogy().setSpouse(null);
        } else if (spouse.getStatus().isDeadOrMIA()) {
            reason = FormerSpouseReason.WIDOWED;

            if (spouse.getStatus().isKIA()) {
                PersonalLogger.spouseKia(origin, spouse, today);
            }
            origin.setMaidenName(null);
            origin.getGenealogy().setSpouse(null);
        } else {
            // Origin is Dead or MIA
            reason = FormerSpouseReason.WIDOWED;

            if (origin.getStatus().isKIA()) {
                PersonalLogger.spouseKia(spouse, origin, today);
            }
            spouse.setMaidenName(null);
            spouse.getGenealogy().setSpouse(null);
        }

        // Add to former spouse list
        spouse.getGenealogy().addFormerSpouse(new FormerSpouse(origin, today, reason));
        origin.getGenealogy().addFormerSpouse(new FormerSpouse(spouse, today, reason));

        MekHQ.triggerEvent(new PersonChangedEvent(spouse));
        MekHQ.triggerEvent(new PersonChangedEvent(origin));
    }

    //region New Day
    /**
     * Processes new day random divorce for an individual.
     * @param campaign the campaign to process
     * @param today the current day
     * @param person the person to process
     */
    public void processNewDay(final Campaign campaign, final LocalDate today, final Person person) {
        if (canDivorce(person, true) != null) {
            return;
        }

        if ((person.getGenealogy().getSpouse().getGender() == person.getGender())
                ? randomSameSexDivorce(person) : randomOppositeSexDivorce(person)) {
            divorce(campaign, today, person, SplittingSurnameStyle.WEIGHTED);
        }
    }

    //region Random Divorce
    /**
     * This determines if a person will randomly divorce their opposite sex spouse
     * @param person the person to determine if they are to randomly divorce their opposite sex spouse
     * @return true if the person is to randomly divorce
     */
    protected abstract boolean randomOppositeSexDivorce(final Person person);

    /**
     * This determines if a person will randomly divorce their same-sex spouse.
     * @param person the person who may be randomly divorcing their same-sex spouse
     * @return true if the person is to randomly divorce
     */
    protected abstract boolean randomSameSexDivorce(final Person person);
    //endregion Random Divorce
    //endregion New Day
}
