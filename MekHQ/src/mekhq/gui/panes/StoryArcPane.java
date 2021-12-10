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
package mekhq.gui.panes;

import megamek.client.ui.preferences.JListPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.annotations.Nullable;
import mekhq.gui.baseComponents.AbstractMHQScrollPane;
import mekhq.gui.baseComponents.JScrollablePanel;
import mekhq.gui.renderers.StoryArcRenderer;
import mekhq.campaign.storyarc.StoryArc;

import javax.swing.*;
import java.awt.*;

public class StoryArcPane extends AbstractMHQScrollPane {
    //region Variable Declarations
    private JList<StoryArc> storyArcs;
    //endregion Variable Declarations

    //region Constructors
    public StoryArcPane(final JFrame frame) {
        super(frame, "StoryArcPane", JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        initialize();
    }
    //endregion Constructors

    //region Getters/Setters
    public JList<StoryArc> getPresets() {
        return storyArcs;
    }

    public void setPresets(final JList<StoryArc> storyArcs) {
        this.storyArcs = storyArcs;
    }

    public @Nullable StoryArc getSelectedStoryArc() {
        return getPresets().getSelectedValue();
    }
    //endregion Getters/Setters

    //region Initialization
    @Override
    protected void initialize() {
        final DefaultListModel<StoryArc> listModel = new DefaultListModel<>();
        listModel.addAll(StoryArc.getStoryArcs());
        setPresets(new JList<>(listModel));
        getPresets().setName("storyArcsList");
        getPresets().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        getPresets().setSelectedIndex(0);
        getPresets().setLayoutOrientation(JList.VERTICAL);
        getPresets().setCellRenderer(new StoryArcRenderer(getFrame()));

        final JPanel panel = new JScrollablePanel(new GridLayout(1, 1));
        panel.setName("storyArcPanel");
        panel.add(getPresets());

        setViewportView(panel);
        setMinimumSize(new Dimension(350, 150));
        setPreferredSize(new Dimension(500, 400));

        setPreferences();
    }

    @Override
    protected void setCustomPreferences(final PreferencesNode preferences) {
        super.setCustomPreferences(preferences);
        preferences.manage(new JListPreference(getPresets()));
    }
    //endregion Initialization
}
