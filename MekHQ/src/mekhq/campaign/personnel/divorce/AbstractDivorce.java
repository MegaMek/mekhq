/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.event.PersonChangedEvent;
import mekhq.campaign.log.PersonalLogger;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.DivorceSurnameStyle;
import mekhq.campaign.personnel.enums.FormerSpouseReason;
import mekhq.campaign.personnel.enums.RandomDivorceMethod;
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
    private boolean useRandomOppositeSexDivorce;
    private boolean useRandomSameSexDivorce;

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel", new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    protected AbstractDivorce(final RandomDivorceMethod method, final CampaignOptions options) {
        this.method = method;
        setUseRandomOppositeSexDivorce(options.isUseRandomOppositeSexDivorce());
        setUseRandomSameSexDivorce(options.isUseRandomSameSexDivorce());
    }
    //endregion Constructors

    //region Getters/Setters
    public RandomDivorceMethod getMethod() {
        return method;
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
        } else if (randomDivorce) {
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
        divorce(campaign, today, person, campaign.getCampaignOptions().getKeepMarriedNameUponSpouseDeath()
                ? DivorceSurnameStyle.BOTH_KEEP_SURNAME : DivorceSurnameStyle.ORIGIN_CHANGES_SURNAME);
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
                        final DivorceSurnameStyle style) {
        final Person spouse = origin.getGenealogy().getSpouse();

        style.apply(campaign, origin, spouse);

        final FormerSpouseReason reason;

        if (spouse.getStatus().isDeadOrMIA() == origin.getStatus().isDeadOrMIA()) {
            reason = FormerSpouseReason.DIVORCE;

            PersonalLogger.divorcedFrom(origin, spouse, today);
            PersonalLogger.divorcedFrom(spouse, origin, today);

            campaign.addReport(String.format(resources.getString("divorce.report"),
                    origin.getHyperlinkedName(), spouse.getHyperlinkedName()));

            spouse.setMaidenName(null);
            origin.setMaidenName(null);

            spouse.getGenealogy().setSpouse(null);
            origin.getGenealogy().setSpouse(null);
        } else if (spouse.getStatus().isDeadOrMIA()) {
            reason = FormerSpouseReason.WIDOWED;

            if (spouse.getStatus().isKIA()) {
                PersonalLogger.spouseKia(spouse, origin, today);
            }
            origin.setMaidenName(null);
            origin.getGenealogy().setSpouse(null);
        } else { // Origin is Dead or MIA
            reason = FormerSpouseReason.WIDOWED;

            if (origin.getStatus().isKIA()) {
                PersonalLogger.spouseKia(origin, spouse, today);
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

        final boolean sameSex = person.getGenealogy().getSpouse().getGender() == person.getGender();
        if ((!sameSex && randomOppositeSexDivorce(person)) || (sameSex && randomSameSexDivorce(person))) {
            divorce(campaign, today, person, DivorceSurnameStyle.WEIGHTED);
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
