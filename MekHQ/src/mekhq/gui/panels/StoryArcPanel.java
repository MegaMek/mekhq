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
package mekhq.gui.panels;

import java.awt.Color;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.storyArc.StoryArcStub;
import mekhq.gui.baseComponents.AbstractMHQPanel;

/**
 * This class displays a Story Arc.
 */
public class StoryArcPanel extends AbstractMHQPanel {
    //region Variable Declarations
    private final Campaign campaign;
    private StoryArcStub storyArcStub;
    private JLabel lblTitle;
    private JTextArea txtDetails;
    //endregion Variable Declarations

    //region Constructors
    public StoryArcPanel(final JFrame frame, final @Nullable Campaign campaign,
          final @Nullable StoryArcStub stub) {
        super(frame, "StoryArcPanel");
        this.campaign = campaign;
        setStoryArcStub(stub);
        initialize();
    }
    //endregion Constructors

    //region Getters/Setters
    public @Nullable Campaign getCampaign() {
        return campaign;
    }

    public void setStoryArcStub(final @Nullable StoryArcStub stub) {
        this.storyArcStub = stub;
    }

    public JLabel getLblTitle() {
        return lblTitle;
    }

    public void setLblTitle(final JLabel lblTitle) {
        this.lblTitle = lblTitle;
    }

    public JTextArea getTxtDetails() {
        return txtDetails;
    }

    public void setTxtDetails(final JTextArea txtDetails) {
        this.txtDetails = txtDetails;
    }
    //endregion Getters/Setters

    //region Initialization
    @Override
    protected void initialize() {
        // Set up the Panel
        setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createEmptyBorder(5, 5, 5, 5),
              BorderFactory.createLineBorder(Color.BLACK, 2)));
        setName("storyArcPanel");
        setLayout(new GridLayout(2, 1));

        // Create Components and Layout
        setLblTitle(new JLabel(""));
        getLblTitle().setName("lblTitle");
        getLblTitle().setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        add(getLblTitle());

        setTxtDetails(new JTextArea(""));
        getTxtDetails().setName("txtDetails");
        getTxtDetails().setEditable(false);
        getTxtDetails().setLineWrap(true);
        getTxtDetails().setWrapStyleWord(true);
        getTxtDetails().setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        add(getTxtDetails());
    }
    //endregion Initialization

    protected void updateFromStoryArcStub(final StoryArcStub stub) {
        getLblTitle().setText("<html><b>" + stub.getTitle() + "</b></html>");
        getTxtDetails().setText(stub.getDetails());
    }
}
