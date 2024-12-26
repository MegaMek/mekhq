/*
 * Copyright (c) 2017-2022 - The MegaMek Team. All Rights Reserved.
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

import mekhq.IconPackage;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.gui.enums.MHQTabType;

import javax.swing.*;

/**
 * Abstract base class for CampaignGUI tab components. Custom tabs should extend
 * CustomCampaignGuiTab instead of this one.
 *
 * @author Neoancient
 */
public abstract class CampaignGuiTab extends JPanel {
    private CampaignGUI gui;

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
