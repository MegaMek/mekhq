/*
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
package mekhq.gui.dialog;

import megamek.common.annotations.Nullable;
import mekhq.gui.baseComponents.AbstractMHQButtonDialog;
import mekhq.gui.panes.StoryArcPane;
import mekhq.campaign.storyarc.StoryArc;

import javax.swing.*;
import java.awt.*;

public class StoryArcSelectionDialog extends AbstractMHQButtonDialog {
    //region Variable Declarations
    private StoryArcPane selectionPanel;
    //endregion Variable Declarations

    //region Constructors
    public StoryArcSelectionDialog(final JFrame parent) {
        super(parent, "StoryArcSelectionDialog", "StoryArcSelectionDialog.title");
        initialize();
    }
    //endregion Constructors

    //region Getters/Setters
    public StoryArcPane getSelectionPanel() {
        return selectionPanel;
    }

    public void setSelectionPanel(final StoryArcPane selectionPanel) {
        this.selectionPanel = selectionPanel;
    }

    /**
     * @return the selected preset, or null if the dialog was cancelled or no preset was selected
     */
    public @Nullable StoryArc getSelectedStoryArc() {
        return getResult().isConfirmed() ? getSelectionPanel().getSelectedStoryArc() : null;
    }
    //endregion Getters/Setters

    //region Initialization
    @Override
    protected Container createCenterPane() {
        setSelectionPanel(new StoryArcPane(getFrame()));
        return getSelectionPanel();
    }
    //endregion Initialization

    @Override
    public void setVisible(final boolean visible) {
        // Only show if there are presets to select from
        super.setVisible(visible && (getSelectionPanel().getPresets().getModel().getSize() > 0));
    }
}
