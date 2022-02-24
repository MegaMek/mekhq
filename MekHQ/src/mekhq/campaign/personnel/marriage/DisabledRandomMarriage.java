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

import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.RandomMarriageMethod;

public class DisabledRandomMarriage extends AbstractMarriage {
    //region Constructors
    public DisabledRandomMarriage(final CampaignOptions options) {
        super(RandomMarriageMethod.NONE, options);
    }
    //endregion Constructors

    @Override
    protected boolean randomOppositeSexMarriage(final Person person) {
        return false;
    }

    @Override
    protected boolean randomSameSexMarriage(final Person person) {
        return false;
    }
}
