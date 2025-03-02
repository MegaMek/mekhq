/*
 * Copyright (C) 2021-2025 The MegaMek Team
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community. BattleMech,
 * BattleTech, and MechWarrior are trademarks of The Topps Company, Inc.
 * The MegaMek organization is not affiliated with The Topps Company, Inc.
 * or Catalyst Game Labs.
 */
package mekhq.campaign.personnel.procreation;

import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.RandomProcreationMethod;

import java.time.LocalDate;

public class DisabledRandomProcreation extends AbstractProcreation {
    //region Constructors
    public DisabledRandomProcreation(final CampaignOptions options) {
        super(RandomProcreationMethod.NONE, options);
    }
    //endregion Constructors

    @Override
    protected boolean randomlyProcreates(final LocalDate today, final Person person) {
        return false;
    }

    @Override
    protected boolean procreation(final Person person) {
        return false;
    }
}
