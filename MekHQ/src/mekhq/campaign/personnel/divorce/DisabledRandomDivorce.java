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

import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.RandomDivorceMethod;

public class DisabledRandomDivorce extends AbstractDivorce {
    //region Constructors
    public DisabledRandomDivorce(final CampaignOptions options) {
        super(RandomDivorceMethod.NONE, options);
    }
    //endregion Constructors

    @Override
    protected boolean randomOppositeSexDivorce(final Person person) {
        return false;
    }

    @Override
    protected boolean randomSameSexDivorce(final Person person) {
        return false;
    }
}
