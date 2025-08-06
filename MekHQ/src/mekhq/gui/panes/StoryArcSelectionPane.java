/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.panes;

import java.awt.GridLayout;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import megamek.client.ui.preferences.JListPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.annotations.Nullable;
import mekhq.campaign.storyarc.StoryArcStub;
import mekhq.gui.baseComponents.AbstractMHQScrollPane;
import mekhq.gui.baseComponents.DefaultMHQScrollablePanel;
import mekhq.gui.renderers.StoryArcRenderer;

public class StoryArcSelectionPane extends AbstractMHQScrollPane {
    //region Variable Declarations
    private JList<StoryArcStub> storyArcStubs;

    //should this be loading story arcs that require starting a new campaign?
    private boolean startNew;
    //endregion Variable Declarations

    //region Constructors
    public StoryArcSelectionPane(final JFrame frame, boolean startNew) {
        super(frame, "StoryArcSelectionPane", JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
              JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        this.startNew = startNew;
        initialize();
    }
    //endregion Constructors

    //region Getters/Setters
    public JList<StoryArcStub> getStoryArcStubs() {
        return storyArcStubs;
    }

    public void setStoryArcs(final JList<StoryArcStub> stubs) {
        this.storyArcStubs = stubs;
    }

    public @Nullable StoryArcStub getSelectedStoryArcStub() {
        return getStoryArcStubs().getSelectedValue();
    }

    //endregion Getters/Setters

    //region Initialization
    @Override
    protected void initialize() {
        DefaultListModel<StoryArcStub> listModel = new DefaultListModel<>();
        listModel.addAll(StoryArcStub.getStoryArcStubs(startNew));
        setStoryArcs(new JList<>(listModel));
        getStoryArcStubs().setName("storyArcsList");
        getStoryArcStubs().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        getStoryArcStubs().setSelectedIndex(0);
        getStoryArcStubs().setLayoutOrientation(JList.VERTICAL);
        getStoryArcStubs().setCellRenderer(new StoryArcRenderer(getFrame()));

        final JPanel panel = new DefaultMHQScrollablePanel(this.getFrame(), "storyArcPanel", new GridLayout(1, 1));
        panel.setName("storyArcPanel");
        panel.add(getStoryArcStubs());

        setViewportView(panel);

        try {
            setPreferences();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setCustomPreferences(final PreferencesNode preferences) throws Exception {
        super.setCustomPreferences(preferences);
        preferences.manage(new JListPreference(getStoryArcStubs()));
    }
    //endregion Initialization
}
