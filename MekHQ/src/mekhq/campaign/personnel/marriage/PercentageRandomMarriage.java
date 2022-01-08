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
package mekhq.campaign.personnel.marriage;

import megamek.common.Compute;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.RandomMarriageMethod;

public class PercentageRandomMarriage extends AbstractMarriage {
    //region Variable Declarations
    private double oppositeSexPercentage;
    private double sameSexPercentage;
    //endregion Variable Declarations

    //region Constructors
    public PercentageRandomMarriage(final CampaignOptions options) {
        super(RandomMarriageMethod.PERCENTAGE, options);
        setOppositeSexPercentage(options.getPercentageRandomMarriageOppositeSexChance());
        setSameSexPercentage(options.getPercentageRandomMarriageSameSexChance());
    }
    //endregion Constructors

    //region Getters/Setters
    public double getOppositeSexPercentage() {
        return oppositeSexPercentage;
    }

    public void setOppositeSexPercentage(final double oppositeSexPercentage) {
        this.oppositeSexPercentage = oppositeSexPercentage;
    }

    public double getSameSexPercentage() {
        return sameSexPercentage;
    }

    public void setSameSexPercentage(final double sameSexPercentage) {
        this.sameSexPercentage = sameSexPercentage;
    }
    //endregion Getters/Setters

    @Override
    protected boolean randomOppositeSexMarriage(final Person person) {
        return Compute.randomFloat() < getOppositeSexPercentage();
    }

    @Override
    protected boolean randomSameSexMarriage(final Person person) {
        return Compute.randomFloat() < getSameSexPercentage();
    }
}
