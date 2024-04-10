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
import mekhq.campaign.storyarc.StoryArcStub;
import mekhq.gui.baseComponents.AbstractMHQButtonDialog;
import mekhq.gui.panes.StoryArcSelectionPane;
import mekhq.gui.utilities.MarkdownRenderer;

import javax.swing.*;
import java.awt.*;

public class StoryArcSelectionDialog extends AbstractMHQButtonDialog {
    //region Variable Declarations
    private StoryArcSelectionPane selectionPanel;
    private JTextPane descriptionPane;
    private boolean startNew;
    //endregion Variable Declarations

    //region Constructors
    public StoryArcSelectionDialog(final JFrame parent, boolean startNew) {
        super(parent, "StoryArcSelectionDialog", "StoryArcSelectionDialog.title");
        this.startNew = startNew;
        initialize();
        refreshDescription();
        selectionPanel.getStoryArcStubs().addListSelectionListener(ev -> refreshDescription());
        setMinimumSize(new Dimension(700, 400));
        setPreferredSize(new Dimension(700, 400));
    }
    //endregion Constructors

    //region Getters/Setters
    public StoryArcSelectionPane getSelectionPanel() {
        return selectionPanel;
    }

    public void setSelectionPanel(final StoryArcSelectionPane selectionPanel) {
        this.selectionPanel = selectionPanel;
    }

    /**
     * @return the selected story arc, or null if the dialog was cancelled or no preset was selected
     */
    public @Nullable StoryArcStub getSelectedStoryArc() {
        return getResult().isConfirmed() ? getSelectionPanel().getSelectedStoryArcStub() : null;
    }

    /**
     * @return the currently selected story arc.
     */
    public @Nullable StoryArcStub getCurrentlySelectedStoryArc() {
        return getSelectionPanel().getSelectedStoryArcStub();
    }
    //endregion Getters/Setters

    //region Initialization
    @Override
    protected Container createCenterPane() {
        StoryArcSelectionPane selectionPane = new StoryArcSelectionPane(getFrame(), startNew);
        setSelectionPanel(selectionPane);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.LINE_AXIS));
        mainPanel.add(selectionPane);

        descriptionPane = new JTextPane();
        descriptionPane.setEditable(false);
        descriptionPane.setContentType("text/html");
        descriptionPane.setMinimumSize(new Dimension(400, 400));
        descriptionPane.setPreferredSize(new Dimension(400, 400));
        descriptionPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        mainPanel.add(new JScrollPane(descriptionPane));

        return mainPanel;
    }
    //endregion Initialization

    @Override
    public void setVisible(final boolean visible) {
        // Only show if there are presets to select from
        super.setVisible(visible && (getSelectionPanel().getStoryArcStubs().getModel().getSize() > 0));
    }

    private void refreshDescription() {
        if (null != getCurrentlySelectedStoryArc()) {
            descriptionPane.setText(MarkdownRenderer.getRenderedHtml(getCurrentlySelectedStoryArc().getDescription()));
            descriptionPane.setCaretPosition(0);
        }
    }
}
