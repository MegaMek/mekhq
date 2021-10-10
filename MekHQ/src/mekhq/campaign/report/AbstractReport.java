/*
 * Report.java
 *
 * Copyright (c) 2013 - Jay Lawson <jaylawson39 at yahoo.com>. All Rights Reserved.
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
package mekhq.campaign.report;

import megamek.common.util.EncodeControl;
import mekhq.campaign.Campaign;

import java.util.ResourceBundle;

/**
 * @author Jay Lawson
 */
public abstract class AbstractReport {
    //region Variable Declarations
    private final Campaign campaign;

    protected final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Reports", new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    public AbstractReport(final Campaign campaign) {
        this.campaign = campaign;
    }
    //endregion Constructors

    //region Getters
    protected Campaign getCampaign() {
        return campaign;
    }
    //endregion Getters
}
