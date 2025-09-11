/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder;

import static java.lang.Math.round;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder.createRoundedLineBorder;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.EnhancedTabbedPane;
import megamek.common.universe.FactionTag;
import megamek.logging.MMLogger;
import megamek.utilities.FastJScrollPane;
import mekhq.MekHQ;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathBuilderTabType;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;

class LifePathFactionPicker extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(LifePathFactionPicker.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.LifePathFactionPicker";

    private static final int MINIMUM_INSTRUCTIONS_WIDTH = scaleForGUI(250);
    private static final int MINIMUM_MAIN_WIDTH = scaleForGUI(575);
    private static final int MINIMUM_COMPONENT_HEIGHT = scaleForGUI(525);

    private static final int TEXT_PANEL_WIDTH = (int) round(MINIMUM_INSTRUCTIONS_WIDTH * 0.75);
    private static final String PANEL_HTML_FORMAT = "<html><div style='width:%dpx;'>%s</div></html>";

    private static final int PADDING = scaleForGUI(10);

    private final Factions factions = Factions.getInstance();
    private final Set<String> storedFactions;
    private Set<String> selectedFactions;

    Set<String> getSelectedFactions() {
        return selectedFactions;
    }

    LifePathFactionPicker(Set<String> selectedFactions, int gameYear, LifePathBuilderTabType tabType) {
        super();

        // Defensive copies to avoid external modification
        this.selectedFactions = new HashSet<>(selectedFactions);
        storedFactions = new HashSet<>(selectedFactions);

        setTitle(getTextAt(RESOURCE_BUNDLE, "LifePathFactionPicker.title"));

        JPanel pnlInstructions = initializeInstructionsPanel(tabType);
        JPanel pnlOptions = buildOptionsPanel(gameYear);
        JPanel pnlControls = buildControlPanel();

        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gbc.gridy = 0;

        gbc.gridx = 0;
        gbc.weightx = 0.2;
        gbc.insets = new Insets(PADDING, PADDING, PADDING, PADDING);
        mainPanel.add(pnlInstructions, gbc);

        JPanel pnlMain = new JPanel();
        pnlMain.setLayout(new BorderLayout());

        pnlMain.add(pnlOptions, BorderLayout.NORTH);
        pnlMain.add(pnlControls, BorderLayout.SOUTH);

        gbc.gridx = 1;
        gbc.weightx = 8;
        mainPanel.add(pnlMain, gbc);

        setContentPane(mainPanel);
        setMinimumSize(new Dimension((int) round((MINIMUM_INSTRUCTIONS_WIDTH + MINIMUM_MAIN_WIDTH) * 1.25),
              MINIMUM_COMPONENT_HEIGHT));
        setLocationRelativeTo(null);
        setModal(true);
        setPreferences(); // Must be before setVisible
        setVisible(true);
    }

    private JPanel buildControlPanel() {
        JPanel pnlControls = new JPanel();
        pnlControls.setLayout(new BoxLayout(pnlControls, BoxLayout.Y_AXIS));
        pnlControls.setBorder(createRoundedLineBorder());

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        String titleCancel = getTextAt(RESOURCE_BUNDLE, "LifePathFactionPicker.button.cancel");
        RoundedJButton btnCancel = new RoundedJButton(titleCancel);
        btnCancel.addActionListener(e -> {
            selectedFactions = storedFactions;
            dispose();
        });

        String titleConfirm = getTextAt(RESOURCE_BUNDLE, "LifePathFactionPicker.button.confirm");
        RoundedJButton btnConfirm = new RoundedJButton(titleConfirm);
        btnConfirm.addActionListener(e -> dispose());

        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(btnCancel);
        buttonPanel.add(Box.createHorizontalStrut(PADDING));
        buttonPanel.add(btnConfirm);
        buttonPanel.add(Box.createHorizontalGlue());

        pnlControls.add(Box.createVerticalStrut(PADDING));
        pnlControls.add(buttonPanel);

        return pnlControls;
    }

    private JPanel buildOptionsPanel(int gameYear) {
        JPanel pnlOptions = new JPanel();
        pnlOptions.setLayout(new BoxLayout(pnlOptions, BoxLayout.Y_AXIS));

        String titleOptions = getTextAt(RESOURCE_BUNDLE, "LifePathFactionPicker.options.label");
        pnlOptions.setBorder(createRoundedLineBorder(titleOptions));

        List<String> superFactions = new ArrayList<>();
        List<String> factions0 = new ArrayList<>();
        List<String> factions1 = new ArrayList<>();
        List<String> factions2 = new ArrayList<>();
        List<String> factions3 = new ArrayList<>();
        List<String> factions4 = new ArrayList<>();
        List<String> factions5 = new ArrayList<>();
        List<String> factions6 = new ArrayList<>();
        List<String> factions7 = new ArrayList<>();
        List<String> factions8 = new ArrayList<>();
        List<String> factions9 = new ArrayList<>();

        List<Faction> allFactions = new ArrayList<>(factions.getFactions(false));
        List<Faction> allFactionsCopy = new ArrayList<>(allFactions);

        List<String> superFactionsNames = List.of("IS", "CLAN", "Periphery");
        for (Faction faction : allFactionsCopy) {
            if (superFactionsNames.contains(faction.getShortName())) {
                allFactions.remove(faction);
                superFactions.add(faction.getShortName());
                continue;
            }

            if (faction.is(FactionTag.SPECIAL)) {
                allFactions.remove(faction);
            }
        }

        allFactions.sort(Comparator.comparing(f -> f.getFullName(gameYear)));
        // Change 'groups' if the number of factions per tab gets too much.
        // The code supports up to 'groups = 10' before additional changes will be needed.
        int groups = 4;
        int n = allFactions.size();
        for (int i = 0; i < n; i++) {
            Faction faction = allFactions.get(i);
            int groupIdx = (int) Math.floor(i * groups / (double) n);
            String factionCode = faction.getShortName();
            switch (groupIdx) {
                case 0 -> factions0.add(factionCode);
                case 1 -> factions1.add(factionCode);
                case 2 -> factions2.add(factionCode);
                case 3 -> factions3.add(factionCode);
                case 4 -> factions4.add(factionCode);
                case 5 -> factions5.add(factionCode);
                case 6 -> factions6.add(factionCode);
                case 7 -> factions7.add(factionCode);
                case 8 -> factions8.add(factionCode);
                default -> factions9.add(factionCode);
            }
        }

        EnhancedTabbedPane optionPane = new EnhancedTabbedPane();

        if (!factions0.isEmpty()) {
            buildTab(gameYear, factions0, optionPane, getFactionOptions(factions0, gameYear));
        }

        if (!factions1.isEmpty()) {
            buildTab(gameYear, factions1, optionPane, getFactionOptions(factions1, gameYear));
        }

        if (!factions2.isEmpty()) {
            buildTab(gameYear, factions2, optionPane, getFactionOptions(factions2, gameYear));
        }

        if (!factions3.isEmpty()) {
            buildTab(gameYear, factions3, optionPane, getFactionOptions(factions3, gameYear));
        }

        if (!factions4.isEmpty()) {
            buildTab(gameYear, factions4, optionPane, getFactionOptions(factions4, gameYear));
        }

        if (!factions5.isEmpty()) {
            buildTab(gameYear, factions5, optionPane, getFactionOptions(factions5, gameYear));
        }

        if (!factions6.isEmpty()) {
            buildTab(gameYear, factions6, optionPane, getFactionOptions(factions6, gameYear));
        }

        if (!factions7.isEmpty()) {
            buildTab(gameYear, factions7, optionPane, getFactionOptions(factions7, gameYear));
        }

        if (!factions8.isEmpty()) {
            buildTab(gameYear, factions8, optionPane, getFactionOptions(factions8, gameYear));
        }

        if (!factions9.isEmpty()) {
            buildTab(gameYear, factions9, optionPane, getFactionOptions(factions9, gameYear));
        }

        if (!superFactions.isEmpty()) {
            FastJScrollPane pnlSuperFactions = getFactionOptions(superFactions, gameYear);
            optionPane.addTab(getTextAt(RESOURCE_BUNDLE, "LifePathFactionPicker.options.tab.special"),
                  pnlSuperFactions);
        }

        pnlOptions.add(optionPane);
        return pnlOptions;
    }

    private void buildTab(int gameYear, List<String> relevantFactions, EnhancedTabbedPane optionPane,
          FastJScrollPane pnlOptions) {
        Faction firstFaction = factions.getFaction(relevantFactions.get(0));
        if (firstFaction == null) {
            LOGGER.error("First Faction not found (build tab): {}", relevantFactions.get(0));
            return;
        }
        Faction lastFaction = factions.getFaction(relevantFactions.get(relevantFactions.size() - 1));
        if (lastFaction == null) {
            LOGGER.error("Last Faction not found (build tab): {}", relevantFactions.get(0));
            return;
        }
        String firstName = firstFaction.getFullName(gameYear);
        String lastName = lastFaction.getFullName(gameYear);

        char firstLetter = firstName.isEmpty() ? '\0' : firstName.charAt(0);
        char lastLetter = lastName.isEmpty() ? '\0' : lastName.charAt(0);

        optionPane.addTab(getFormattedTextAt(RESOURCE_BUNDLE, "LifePathFactionPicker.options.tab", firstLetter,
              lastLetter), pnlOptions);
    }

    private FastJScrollPane getFactionOptions(List<String> factionOptions, int gameYear) {
        JPanel pnlFactions = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int columns = 3;
        for (int i = 0; i < factionOptions.size(); i++) {
            String factionCode = factionOptions.get(i);
            Faction faction = factions.getFaction(factionCode);
            if (faction == null) {
                LOGGER.error("Faction not found: {}", factionCode);
                continue;
            }
            String label = faction.getFullName(gameYear);
            JCheckBox chkFaction = new JCheckBox(label);
            chkFaction.setSelected(selectedFactions.contains(factionCode));
            chkFaction.addActionListener(evt -> {
                if (chkFaction.isSelected()) {
                    selectedFactions.add(factionCode);
                } else {
                    selectedFactions.remove(factionCode);
                }
            });

            gbc.gridx = i % columns;
            gbc.gridy = i / columns;

            JPanel pnlRows = new JPanel();
            pnlRows.setLayout(new BoxLayout(pnlRows, BoxLayout.X_AXIS));
            pnlRows.add(chkFaction);
            pnlRows.setAlignmentX(Component.LEFT_ALIGNMENT);

            pnlFactions.add(pnlRows, gbc);
        }

        FastJScrollPane scrollSkills = new FastJScrollPane(pnlFactions);
        scrollSkills.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollSkills.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollSkills.setBorder(null);

        return scrollSkills;
    }

    private JPanel initializeInstructionsPanel(LifePathBuilderTabType tabType) {
        JPanel pnlInstructions = new JPanel();

        String titleInstructions = getTextAt(RESOURCE_BUNDLE, "LifePathFactionPicker.instructions.label");
        pnlInstructions.setBorder(createRoundedLineBorder(titleInstructions));

        JEditorPane txtInstructions = new JEditorPane();
        txtInstructions.setContentType("text/html");
        txtInstructions.setEditable(false);
        String instructions = String.format(PANEL_HTML_FORMAT, TEXT_PANEL_WIDTH,
              getTextAt(RESOURCE_BUNDLE, "LifePathFactionPicker.instructions.text." + tabType.getLookupName()));
        txtInstructions.setText(instructions);

        FastJScrollPane scrollInstructions = new FastJScrollPane(txtInstructions);
        scrollInstructions.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollInstructions.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollInstructions.setBorder(null);

        pnlInstructions.add(scrollInstructions);
        pnlInstructions.setMinimumSize(new Dimension(MINIMUM_INSTRUCTIONS_WIDTH, MINIMUM_COMPONENT_HEIGHT));

        return pnlInstructions;
    }

    /**
     * This override forces the preferences for this class to be tracked in MekHQ instead of MegaMek.
     */
    private void setPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(LifePathFactionPicker.class);
            this.setName("LifePathFactionPicker");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
    }
}
