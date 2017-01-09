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

import java.awt.Frame;

import javax.swing.JPanel;

import mekhq.IconPackage;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;

/**
 * Abstract base class for CampaignGUI tab components
 * 
 * @author Neoancient
 *
 */
public abstract class CampaignGuiTab extends JPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6091435251932963385L;

	private CampaignGUI gui;
	
	protected String name;
	
	public CampaignGuiTab(CampaignGUI gui, String name) {
		this.gui = gui;
		this.name = name;
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
	
	public Frame getFrame() {
		return gui.getFrame();
	}
	
	public IconPackage getIconPackage() {
		return gui.getIconPackage();
	}
	
	public String getName() {
		return name;
	}
	
	abstract public void initTab();
	
	abstract public void refreshAll();

}
