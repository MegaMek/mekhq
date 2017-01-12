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

import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;

/**
 * Base class for any GUI tab that uses <code>TechTableModel</code> or
 * <code>TaskTableMode</code> to give each access to the selected tech or task.
 *
 * @author Neoancient
 *
 */
public abstract class TechWorkGuiTab extends CampaignGuiTab {

    private static final long serialVersionUID = -713421158605886893L;

    TechWorkGuiTab(CampaignGUI gui, String tabName) {
        super(gui, tabName);
    }

    public abstract Person getSelectedTech();

    public abstract Part getSelectedTask();
}
