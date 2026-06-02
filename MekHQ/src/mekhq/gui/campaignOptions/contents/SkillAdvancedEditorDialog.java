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
package mekhq.gui.campaignOptions.contents;

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;

import megamek.client.ui.util.UIUtil;

/**
 * A compact modal editor for the advanced configuration of a single skill: its per-level XP costs and the experience
 * milestones (the level at which a pilot is considered Green, Regular, Veteran, Elite, Heroic, and Legendary).
 *
 * <p>The dialog edits a working copy and only writes the values back into the supplied {@link SkillConfiguration} when
 * the user confirms with OK, so cancelling leaves the configuration untouched. Milestones are clamped into ascending
 * order on confirmation to guarantee a logical progression.</p>
 */
class SkillAdvancedEditorDialog extends JDialog {
    private static final int SKILL_LEVEL_COUNT = 11;
    private static final int MAX_LEVEL = 10;

    private final SkillConfiguration config;
    private boolean changed;

    private final JSpinner spnTargetNumber;
    private final JSpinner[] spnCosts = new JSpinner[SKILL_LEVEL_COUNT];
    private final JSpinner spnGreen;
    private final JSpinner spnRegular;
    private final JSpinner spnVeteran;
    private final JSpinner spnElite;
    private final JSpinner spnHeroic;
    private final JSpinner spnLegendary;

    /**
     * Creates the advanced editor for one skill.
     *
     * @param parent           the window to center on, may be {@code null}
     * @param skillDisplayName the human-readable skill name shown in the title
     * @param config           the configuration to edit; only mutated when the user confirms
     */
    SkillAdvancedEditorDialog(Window parent, String skillDisplayName, SkillConfiguration config) {
        super(parent, getTextAt(getCampaignOptionsResourceBundle(), "skillAdvancedEditor.title") + skillDisplayName,
              ModalityType.APPLICATION_MODAL);
        this.config = config;

        spnTargetNumber = createSpinner(config.targetNumber, 0, 12);
        for (int i = 0; i < SKILL_LEVEL_COUNT; i++) {
            int cost = (i < config.costs.length && config.costs[i] != null) ? config.costs[i] : -1;
            spnCosts[i] = createSpinner(cost, -1, 9999);
        }
        spnGreen = createSpinner(config.greenLevel, 0, MAX_LEVEL);
        spnRegular = createSpinner(config.regularLevel, 0, MAX_LEVEL);
        spnVeteran = createSpinner(config.veteranLevel, 0, MAX_LEVEL);
        spnElite = createSpinner(config.eliteLevel, 0, MAX_LEVEL);
        spnHeroic = createSpinner(config.heroicLevel, 0, MAX_LEVEL);
        spnLegendary = createSpinner(config.legendaryLevel, 0, MAX_LEVEL);

        initComponents();
        pack();
        setLocationRelativeTo(parent);
    }

    private static JSpinner createSpinner(int value, int min, int max) {
        int clamped = Math.max(min, Math.min(max, value));
        return new JSpinner(new SpinnerNumberModel(clamped, min, max, 1));
    }

    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout());

        // Lay the three logical groups (target number + milestones, low XP levels, high XP levels) out side by side so
        // the dialog uses the available horizontal space instead of becoming a single tall column.
        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(8),
              UIUtil.scaleForGUI(12),
              UIUtil.scaleForGUI(8),
              UIUtil.scaleForGUI(12)));

        GridBagConstraints columnConstraints = new GridBagConstraints();
        columnConstraints.gridx = 0;
        columnConstraints.gridy = 0;
        columnConstraints.anchor = GridBagConstraints.PAGE_START;
        columnConstraints.insets = new Insets(0, 0, 0, UIUtil.scaleForGUI(24));

        content.add(createTargetAndMilestonesColumn(), columnConstraints);

        String costLabelTemplate = getTextAt(getCampaignOptionsResourceBundle(), "skillAdvancedEditor.costLevel");
        columnConstraints.gridx++;
        content.add(createCostsColumn(costLabelTemplate, 0, 5), columnConstraints);

        columnConstraints.gridx++;
        columnConstraints.insets = new Insets(0, 0, 0, 0);
        content.add(createCostsColumn(costLabelTemplate, 6, MAX_LEVEL), columnConstraints);

        getContentPane().add(content, BorderLayout.CENTER);

        JButton btnOK = new JButton(getTextAt(getCampaignOptionsResourceBundle(), "skillAdvancedEditor.ok"));
        btnOK.addActionListener(evt -> confirm());
        JButton btnCancel = new JButton(getTextAt(getCampaignOptionsResourceBundle(), "skillAdvancedEditor.cancel"));
        btnCancel.addActionListener(evt -> dispose());
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(btnOK);
        buttonPanel.add(btnCancel);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createTargetAndMilestonesColumn() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = newColumnConstraints();

        addSectionHeader(panel, gbc, "skillAdvancedEditor.targetNumber");
        addRow(panel, gbc, getTextAt(getCampaignOptionsResourceBundle(), "lblSkillPanelTargetNumber.text"),
              spnTargetNumber);

        addSectionHeader(panel, gbc, "skillAdvancedEditor.milestones");
        addRow(panel, gbc, getTextAt(getCampaignOptionsResourceBundle(), "skillAdvancedEditor.green"), spnGreen);
        addRow(panel, gbc, getTextAt(getCampaignOptionsResourceBundle(), "skillAdvancedEditor.regular"), spnRegular);
        addRow(panel, gbc, getTextAt(getCampaignOptionsResourceBundle(), "skillAdvancedEditor.veteran"), spnVeteran);
        addRow(panel, gbc, getTextAt(getCampaignOptionsResourceBundle(), "skillAdvancedEditor.elite"), spnElite);
        addRow(panel, gbc, getTextAt(getCampaignOptionsResourceBundle(), "skillAdvancedEditor.heroic"), spnHeroic);
        addRow(panel, gbc, getTextAt(getCampaignOptionsResourceBundle(), "skillAdvancedEditor.legendary"),
              spnLegendary);

        return panel;
    }

    private JPanel createCostsColumn(String costLabelTemplate, int firstLevel, int lastLevel) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = newColumnConstraints();

        // Drop the costs columns by one header + one row so the "XP Cost per Level" header lines up with the
        // "Experience Milestones" header (and Level 0 with Green), leaving Base Target Number alone on the first row.
        addSectionHeaderSpacer(panel, gbc);
        addRowSpacer(panel, gbc);

        // Only the first costs column carries the section header so the two halves read as one "XP Cost per Level"
        // group spanning the right of the dialog.
        if (firstLevel == 0) {
            addSectionHeader(panel, gbc, "skillAdvancedEditor.costs");
        } else {
            addSectionHeaderSpacer(panel, gbc);
        }

        for (int level = firstLevel; level <= lastLevel; level++) {
            addRow(panel, gbc, String.format(costLabelTemplate, level), spnCosts[level]);
        }

        return panel;
    }

    private GridBagConstraints newColumnConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.insets = new Insets(UIUtil.scaleForGUI(3), UIUtil.scaleForGUI(3), UIUtil.scaleForGUI(3),
              UIUtil.scaleForGUI(8));
        return gbc;
    }

    private void addSectionHeader(JPanel panel, GridBagConstraints gbc, String resourceKey) {
        JLabel header = new JLabel("<html><b>" + getTextAt(getCampaignOptionsResourceBundle(), resourceKey)
              + "</b></html>");
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(UIUtil.scaleForGUI(2), UIUtil.scaleForGUI(3), UIUtil.scaleForGUI(6),
              UIUtil.scaleForGUI(8));
        panel.add(header, gbc);
        gbc.gridwidth = 1;
        gbc.insets = new Insets(UIUtil.scaleForGUI(3), UIUtil.scaleForGUI(3), UIUtil.scaleForGUI(3),
              UIUtil.scaleForGUI(8));
    }

    private void addSectionHeaderSpacer(JPanel panel, GridBagConstraints gbc) {
        // Reserve the same vertical space a header would take so this column's rows line up with the headed column.
        JLabel spacer = new JLabel("<html><b>&nbsp;</b></html>");
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(UIUtil.scaleForGUI(2), UIUtil.scaleForGUI(3), UIUtil.scaleForGUI(6),
              UIUtil.scaleForGUI(8));
        panel.add(spacer, gbc);
        gbc.gridwidth = 1;
        gbc.insets = new Insets(UIUtil.scaleForGUI(3), UIUtil.scaleForGUI(3), UIUtil.scaleForGUI(3),
              UIUtil.scaleForGUI(8));
    }

    private void addRowSpacer(JPanel panel, GridBagConstraints gbc) {
        // Reserve the vertical space of one label/spinner row so the costs columns drop into alignment with the
        // milestones rows in the adjacent column.
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        panel.add(Box.createVerticalStrut(spnTargetNumber.getPreferredSize().height), gbc);
        gbc.gridwidth = 1;
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, String labelText, Component control) {
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel(labelText), gbc);
        gbc.gridx = 1;
        panel.add(control, gbc);
    }

    private void confirm() {
        config.targetNumber = (int) spnTargetNumber.getValue();
        for (int i = 0; i < SKILL_LEVEL_COUNT; i++) {
            config.costs[i] = (int) spnCosts[i].getValue();
        }

        int green = (int) spnGreen.getValue();
        int regular = Math.max(green, (int) spnRegular.getValue());
        int veteran = Math.max(regular, (int) spnVeteran.getValue());
        int elite = Math.max(veteran, (int) spnElite.getValue());
        int heroic = Math.max(elite, (int) spnHeroic.getValue());
        int legendary = Math.max(heroic, (int) spnLegendary.getValue());

        config.greenLevel = green;
        config.regularLevel = regular;
        config.veteranLevel = veteran;
        config.eliteLevel = elite;
        config.heroicLevel = heroic;
        config.legendaryLevel = legendary;

        changed = true;
        dispose();
    }

    /**
     * @return {@code true} if the user confirmed changes that were written back into the configuration
     */
    boolean wasChanged() {
        return changed;
    }
}
