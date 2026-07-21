/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import megamek.client.ui.dialogs.buttonDialogs.AbstractButtonDialog;
import megamek.client.ui.util.UIUtil;
import mekhq.MHQOptionsChangedEvent;
import mekhq.MekHQ;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.CampaignOptionsIconLegend;
import mekhq.gui.campaignOptions.MHQOptionsPane;

/**
 * Hosting dialog for {@link MHQOptionsPane}, the MekHQ Client Options screen built on the reusable Campaign Options
 * tree-navigation framework.
 */
public class MHQOptionsTreeDialog extends AbstractButtonDialog {
    private static final int BUTTON_GAP = UIUtil.scaleForGUI(8);

    private MHQOptionsPane optionsPane;

    public MHQOptionsTreeDialog(final JFrame frame) {
        super(frame, true, MekHQ.getDefaultResourceBundle(), "MHQOptionsTreeDialog", "MHQOptionsTreeDialog.title");
        initialize();
    }

    @Override
    protected Container createCenterPane() {
        optionsPane = new MHQOptionsPane(getFrame());
        return optionsPane;
    }

    /**
     * Replaces the base two-button {@code GridLayout} panel (which stretches the buttons across the full footer) with a
     * centred row of fixed-size buttons, matching the Campaign Options dialog. Ok is emphasized as the primary action.
     */
    @Override
    protected JPanel createButtonPanel() {
        JButton okButton = new JButton(resources.getString("Ok.text"));
        okButton.setName("btnOk");
        okButton.setToolTipText(resources.getString("Ok.toolTipText"));
        // Emphasize Ok as the primary action using FlatLaf's accent/default-button colours. This is a visual cue only
        // (not the root-pane default button), so Enter cannot accidentally confirm the dialog while editing a control.
        okButton.putClientProperty("FlatLaf.style",
              "background: $Button.default.background; foreground: $Button.default.foreground");
        okButton.addActionListener(this::okButtonActionPerformed);

        JButton cancelButton = new JButton(resources.getString("Cancel.text"));
        cancelButton.setName("btnCancel");
        cancelButton.setToolTipText(resources.getString("Cancel.toolTipText"));
        cancelButton.addActionListener(this::cancelActionPerformed);

        int buttonWidth = Math.max(UIUtil.scaleForGUI(100),
              Math.max(okButton.getPreferredSize().width, cancelButton.getPreferredSize().width));
        int buttonHeight = Math.max(okButton.getPreferredSize().height, cancelButton.getPreferredSize().height);
        Dimension buttonSize = new Dimension(buttonWidth, buttonHeight);
        okButton.setPreferredSize(buttonSize);
        cancelButton.setPreferredSize(buttonSize);

        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, BUTTON_GAP, BUTTON_GAP));
        actionButtons.add(okButton);
        actionButtons.add(cancelButton);

        // Icons legend on the left, as a reference aid rather than a dialog action (matching the Campaign Options
        // dialog). The right spacer mirrors its width so the Ok/Cancel buttons stay centred on the whole footer.
        List<CampaignOptionsIconLegend.Entry> legendEntries = List.of(
              CampaignOptionsIconLegend.flagEntry(CampaignOptionFlag.IMPORTANT),
              CampaignOptionsIconLegend.flagEntry(CampaignOptionFlag.UNIMPLEMENTED));
        JButton legendButton = CampaignOptionsIconLegend.createLegendButton(legendEntries);
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, BUTTON_GAP, BUTTON_GAP));
        legendPanel.add(legendButton);
        JPanel rightSpacer = new JPanel();
        rightSpacer.setOpaque(false);
        rightSpacer.setPreferredSize(new Dimension(legendPanel.getPreferredSize().width, 0));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(legendPanel, BorderLayout.WEST);
        panel.add(actionButtons, BorderLayout.CENTER);
        panel.add(rightSpacer, BorderLayout.EAST);
        return panel;
    }

    @Override
    protected void okAction() {
        optionsPane.save();
        MekHQ.triggerEvent(new MHQOptionsChangedEvent());
    }

    /**
     * Stores this dialog's window bounds in MekHQ's preference store, matching
     * {@link mekhq.gui.campaignOptions.CampaignOptionsDialog} and the other MekHQ dialogs. Without this override the
     * base class would persist to MegaMek's store instead, splitting this dialog's remembered size away from the rest
     * of MekHQ.
     */
    @Override
    protected void setPreferences() throws Exception {
        setPreferences(MekHQ.getMHQPreferences().forClass(getClass()));
    }
}
