/*
 * ReportHyperlinkListener.java
 *
 * Copyright (c) 2009 - Jay Lawson <jaylawson39 at yahoo.com>. All Rights Reserved.
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
package mekhq.gui;

import mekhq.MekHQ;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.util.UUID;

public class ReportHyperlinkListener implements HyperlinkListener {
    //region Variable Declarations
    private final CampaignGUI campaignGUI;

    public static final String UNIT = "UNIT";
    public static final String PERSON = "PERSON";
    public static final String NEWS = "NEWS";
    public static final String MAINTENANCE = "MAINTENANCE";
    public static final String PERSONNEL_MARKET = "PERSONNEL_MARKET";
    public static final String REPAIR = "REPAIR";
    public static final String CONTRACT_MARKET = "CONTRACT_MARKET";
    public static final String UNIT_MARKET = "UNIT_MARKET";
    //endregion Variable Declarations

    //region Constructors
    public ReportHyperlinkListener(final CampaignGUI campaignGUI) {
        this.campaignGUI = campaignGUI;
    }
    //endregion Constructors

    @Override
    public void hyperlinkUpdate(final HyperlinkEvent evt) {
        if (evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            if (evt .getDescription().startsWith(UNIT_MARKET)) { // Must come before UNIT since it starts with UNIT as well
                campaignGUI.showUnitMarket();
            } else if (evt.getDescription().startsWith(UNIT)) {
                try {
                    final UUID id = UUID.fromString(evt.getDescription().split(":")[1]);
                    campaignGUI.focusOnUnit(id);
                } catch (Exception e) {
                    MekHQ.getLogger().error(e);
                }
            } else if (evt.getDescription().startsWith(PERSONNEL_MARKET)) { // Must come before PERSON since it starts with PERSON as well
                campaignGUI.hirePersonMarket();
            } else if (evt.getDescription().startsWith(PERSON)) {
                try {
                    final UUID id = UUID.fromString(evt.getDescription().split(":")[1]);
                    campaignGUI.focusOnPerson(id);
                } catch (Exception e) {
                    MekHQ.getLogger().error(e);
                }
            } else if (evt.getDescription().startsWith(NEWS)) {
                try {
                    final int id = Integer.parseInt(evt.getDescription().split("\\|")[1]);
                    campaignGUI.showNews(id);
                } catch (Exception e) {
                    MekHQ.getLogger().error(e);
                }
            } else if (evt.getDescription().startsWith(MAINTENANCE)) {
                try {
                    final UUID id = UUID.fromString(evt.getDescription().split("\\|")[1]);
                    campaignGUI.showMaintenanceReport(id);
                } catch (Exception e) {
                    MekHQ.getLogger().error(e);
                }
            } else if (evt.getDescription().startsWith(REPAIR)) {
                try {
                    final UUID id = UUID.fromString(evt.getDescription().split("\\|")[1]);
                    campaignGUI.focusOnUnitInRepairBay(id);
                } catch (Exception e) {
                    MekHQ.getLogger().error(e);
                }
            } else if (evt.getDescription().startsWith(CONTRACT_MARKET)) {
                campaignGUI.showContractMarket();
            }
        }
    }
}
