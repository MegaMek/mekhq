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

public class StoryArcSelectionPane extends AbstractMHQScrollPane {
    //region Variable Declarations
    private JList<StoryArc> storyArcs;
    //endregion Variable Declarations

    //region Constructors
    public StoryArcSelectionPane(final JFrame frame) {
        super(frame, "StoryArcSelectionPane", JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        initialize();
    }
    //endregion Constructors

    //region Getters/Setters
    public JList<StoryArc> getStoryArcs() {
        return storyArcs;
    }

    public void setStoryArcs(final JList<StoryArc> storyArcs) {
        this.storyArcs = storyArcs;
    }

    public @Nullable StoryArc getSelectedStoryArc() {
        return getStoryArcs().getSelectedValue();
    }

    //endregion Getters/Setters

    //region Initialization
    @Override
    protected void initialize() {
        DefaultListModel<StoryArc> listModel = new DefaultListModel<>();
        listModel.addAll(StoryArc.getStoryArcs());
        setStoryArcs(new JList<>(listModel));
        getStoryArcs().setName("storyArcsList");
        getStoryArcs().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        getStoryArcs().setSelectedIndex(0);
        getStoryArcs().setLayoutOrientation(JList.VERTICAL);
        getStoryArcs().setCellRenderer(new StoryArcRenderer(getFrame()));

        final JPanel panel = new JScrollablePanel(new GridLayout(1, 1));
        panel.setName("storyArcPanel");
        panel.add(getStoryArcs());

        setViewportView(panel);

        setPreferences();
    }

    @Override
    protected void setCustomPreferences(final PreferencesNode preferences) {
        super.setCustomPreferences(preferences);
        preferences.manage(new JListPreference(getStoryArcs()));
    }
    //endregion Initialization
}
