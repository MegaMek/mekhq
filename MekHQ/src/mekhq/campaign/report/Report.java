/*
 * Report.java
 *
 * Copyright (c) 2013 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.report;

import javax.swing.JTextPane;

import mekhq.campaign.Campaign;
import mekhq.campaign.Hangar;


/**
 * @author Jay Lawson
 */
public abstract class Report {

    private Campaign campaign;

    public Report(Campaign c) {
        this.campaign = c;
    }

    protected Campaign getCampaign() {
        return campaign;
    }

    protected Hangar getHangar() {
        return getCampaign().getHangar();
    }

    //using JTextPane here allows for more flexibility in terms of
    //what these reports look like
    public abstract JTextPane getReport();

    public abstract String getTitle();

}
