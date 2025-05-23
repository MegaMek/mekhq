/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.dialog.reportDialogs.FactionStanding;

import java.time.LocalDate;
import javax.swing.ImageIcon;
import javax.swing.JFrame;

import megamek.client.ui.baseComponents.MMComboBox;
import megamek.common.annotations.Nullable;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.universe.Faction;

/**
 * Dialog window for manually reporting a mission result with a specific mission name in a campaign. Extends
 * {@link SimulateMissionDialog} to customize the user interface for manual entry.
 *
 * <p>This dialog disables the mission status combo box and sets the display name for the mission.</p>
 */
public class ManualMissionDialog extends SimulateMissionDialog {
    private final String missionName;

    /**
     * Creates a new ManualMissionDialog for entering a mission result manually.
     *
     * @param parent          the parent {@link JFrame} for modality.
     * @param campaignIcon    icon representing the campaign.
     * @param campaignFaction the faction representing the campaign.
     * @param today           the current date of the campaign.
     * @param missionStatus   the mission status to apply.
     * @param missionName     the display name of the mission.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public ManualMissionDialog(JFrame parent, ImageIcon campaignIcon, Faction campaignFaction, LocalDate today,
          MissionStatus missionStatus, String missionName) {
        super(campaignIcon, campaignFaction, today, missionStatus);
        this.missionName = missionName;

        populateFactionsList();
        populateStatusList();
        populateDialog();
        initializeDialog(parent);
    }

    @Override
    protected String getMissionName() {
        return "<b>" + missionName + "</b>";
    }

    @Override
    protected @Nullable MMComboBox<String> getComboMissionStatus() {
        return null; // This will stop the mission status selector from appearing
    }
}
