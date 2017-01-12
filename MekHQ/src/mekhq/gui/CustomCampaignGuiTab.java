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

/**
 * Abstract base class for custom gui tabs
 *
 * @author Neoancient
 */
public abstract class CustomCampaignGuiTab extends CampaignGuiTab {

    private static final long serialVersionUID = 1312671796115618117L;

    public CustomCampaignGuiTab(CampaignGUI gui, String tabName) {
        super(gui, tabName);
    }

    /*
     * (non-Javadoc)
     *
     * @see mekhq.gui.CampaignGuiTab#tabType()
     */
    @Override
    final public GuiTabType tabType() {
        return GuiTabType.CUSTOM;
    }

}
