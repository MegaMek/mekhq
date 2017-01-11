/*
 * Copyright (c) 2017 The MegaMek Team. All rights reserved.
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
package mekhq.gui;

import java.util.ResourceBundle;

import javax.swing.JFrame;
import javax.swing.JPanel;

import megamek.common.util.EncodeControl;
import mekhq.IconPackage;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;

/**
 * Abstract base class for CampaignGUI tab components. Custom tabs should extend CustomCampaignGuiTab
 * instead of this one.
 * 
 * @author Neoancient
 *
 */
public abstract class CampaignGuiTab extends JPanel {
	
	private static final long serialVersionUID = 6091435251932963385L;

    public enum TabType {
    	TOE (0, "panOrganization.TabConstraints.tabTitle"),  //$NON-NLS-1$
    	BRIEFING (1, "panBriefing.TabConstraints.tabTitle"),  //$NON-NLS-1$
    	MAP (2, "panMap.TabConstraints.tabTitle"),  //$NON-NLS-1$
    	PERSONNEL (3, "panPersonnel.TabConstraints.tabTitle"),  //$NON-NLS-1$
    	HANGAR (4, "panHangar.TabConstraints.tabTitle"),  //$NON-NLS-1$
    	WAREHOUSE (5, "panSupplies.TabConstraints.tabTitle"),  //$NON-NLS-1$
    	REPAIR (6, "panRepairBay.TabConstraints.tabTitle"),  //$NON-NLS-1$
    	INFIRMARY (7, "panInfirmary.TabConstraints.tabTitle"),  //$NON-NLS-1$
    	MEKLAB (8, "panMekLab.TabConstraints.tabTitle"),  //$NON-NLS-1$
    	FINANCES (9, "panFinances.TabConstraints.tabTitle"),  //$NON-NLS-1$
    	OVERVIEW (10, "panOverview.TabConstraints.tabTitle"),  //$NON-NLS-1$
    	CUSTOM (11, null);
    	
    	private int defaultPos;
    	private String name;
    	
    	public int getDefaultPos() {
    		return defaultPos;
    	}
    	
    	public String getTabName() {
    		return name;
    	}
    	
    	TabType(int defaultPos, String resKey) {
    		this.defaultPos = defaultPos;
    		if (resKey == null) {
    			name = "Custom";
    		} else {
    			name = ResourceBundle.getBundle("mekhq.resources.CampaignGUI", new EncodeControl()) //$NON-NLS-1$;
    				.getString(resKey);
    		}
    	}
    	
    	public CampaignGuiTab createTab(CampaignGUI gui) {
    		switch (this) {
			case TOE:
				return new TOETab(gui, name);
			case BRIEFING:
				return new BriefingTab(gui, name);
			case MAP:
				return new MapTab(gui, name);
			case PERSONNEL:
				return new PersonnelTab(gui, name);
			case HANGAR:
				return new HangarTab(gui, name);
			case WAREHOUSE:
				return new WarehouseTab(gui, name);
			case REPAIR:
				return new RepairTab(gui, name);
			case INFIRMARY:
				return new InfirmaryTab(gui, name);
			case MEKLAB:
				return new MekLabTab(gui, name);
			case FINANCES:
			case OVERVIEW:
			default:
				return null;
    		
    		}
    	}
    }

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

	abstract public TabType tabType();
}
