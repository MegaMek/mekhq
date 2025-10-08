/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui;

import javax.swing.JFrame;
import javax.swing.JPanel;

import mekhq.IconPackage;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.gui.enums.MHQTabType;

/**
 * Abstract base class for CampaignGUI tab components. Custom tabs should extend CustomCampaignGuiTab instead of this
 * one.
 *
 * @author Neoancient
 */
public abstract class CampaignGuiTab extends JPanel {
    private final CampaignGUI gui;

    protected String tabName;

    CampaignGuiTab(CampaignGUI gui, String tabName) {
        this.gui = gui;
        this.tabName = tabName;
        initTab();
    }

    public CampaignGUI getCampaignGui() {
        return gui;
    }

    /* Some convenience methods */
    public Campaign getCampaign() {
        return gui.getCampaign();
    }

    public CampaignOptions getCampaignOptions() {
        return gui.getCampaign().getCampaignOptions();
    }

    public JFrame getFrame() {
        return gui.getFrame();
    }

    public IconPackage getIconPackage() {
        return gui.getIconPackage();
    }

    public String getTabName() {
        return tabName;
    }

    abstract public void initTab();

    abstract public void refreshAll();

    abstract public MHQTabType tabType();

    /**
     * Called when tab is removed from gui.
     */
    public void disposeTab() {
        MekHQ.unregisterHandler(this);
    }
}
