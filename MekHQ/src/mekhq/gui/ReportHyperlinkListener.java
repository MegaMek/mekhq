/*
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 */
package mekhq.gui;

import megamek.logging.MMLogger;
import mekhq.campaign.unit.Unit;
import mekhq.gui.dialog.VocationalExperienceAwardDialog;
import mekhq.gui.dialog.reportDialogs.MaintenanceReportDialog;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkListener;
import java.util.UUID;

public class ReportHyperlinkListener implements HyperlinkListener {
    private static final MMLogger logger = MMLogger.create(ReportHyperlinkListener.class);

    // region Variable Declarations
    private final CampaignGUI campaignGUI;

    public static final String UNIT = "UNIT";
    public static final String PERSON = "PERSON";
    public static final String NEWS = "NEWS";
    public static final String MAINTENANCE = "MAINTENANCE";
    public static final String PERSONNEL_MARKET = "PERSONNEL_MARKET";
    public static final String REPAIR = "REPAIR";
    public static final String CONTRACT_MARKET = "CONTRACT_MARKET";
    public static final String UNIT_MARKET = "UNIT_MARKET";
    public static final String PERSONNEL_ADVANCEMENT = "PERSONNEL_ADVANCEMENT";
    // endregion Variable Declarations

    // region Constructors
    public ReportHyperlinkListener(final CampaignGUI campaignGUI) {
        this.campaignGUI = campaignGUI;
    }
    // endregion Constructors

    @Override
    public void hyperlinkUpdate(final HyperlinkEvent evt) {
        if (evt.getEventType() == EventType.ACTIVATED) {
            if (evt.getDescription().startsWith(UNIT_MARKET)) { // Must come before UNIT since it starts with UNIT as
                                                                // well
                campaignGUI.showUnitMarket();
            } else if (evt.getDescription().startsWith(UNIT)) {
                try {
                    final UUID id = UUID.fromString(evt.getDescription().split(":")[1]);
                    campaignGUI.focusOnUnit(id);
                } catch (Exception e) {
                    logger.error("", e);
                }
            } else if (evt.getDescription().startsWith(PERSONNEL_MARKET)) { // Must come before PERSON since it starts
                                                                            // with PERSON as well
                campaignGUI.hirePersonMarket();
            } else if (evt.getDescription().startsWith(NEWS)) {
                try {
                    final int id = Integer.parseInt(evt.getDescription().split("\\|")[1]);
                    campaignGUI.showNews(id);
                } catch (Exception e) {
                    logger.error("", e);
                }
            } else if (evt.getDescription().startsWith(MAINTENANCE)) {
                try {
                    final UUID id = UUID.fromString(evt.getDescription().split("\\|")[1]);
                    final Unit unit = campaignGUI.getCampaign().getUnit(id);
                    if (unit == null) {
                        logger.error("Unit id determination failure for " + id);
                        return;
                    }
                    new MaintenanceReportDialog(campaignGUI.getFrame(), unit).setVisible(true);
                } catch (Exception e) {
                    logger.error("", e);
                }
            } else if (evt.getDescription().startsWith(REPAIR)) {
                try {
                    final UUID id = UUID.fromString(evt.getDescription().split("\\|")[1]);
                    campaignGUI.focusOnUnitInRepairBay(id);
                } catch (Exception e) {
                    logger.error("", e);
                }
            } else if (evt.getDescription().startsWith(CONTRACT_MARKET)) {
                campaignGUI.showContractMarket();
            } else if (evt.getDescription().startsWith(PERSONNEL_ADVANCEMENT)) { // Must come before PERSON since it starts
                                                                                 // with PERSON as well
                try {
                    new VocationalExperienceAwardDialog(campaignGUI.getCampaign());
                } catch (Exception e) {
                    logger.error("", e);
                }
            } else if (evt.getDescription().startsWith(PERSON)) {
                try {
                    final UUID id = UUID.fromString(evt.getDescription().split(":")[1]);
                    campaignGUI.focusOnPerson(id);
                } catch (Exception e) {
                    logger.error("", e);
                }
            }
        }
    }
}
