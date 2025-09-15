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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.EnhancedTabbedPane;
import megamek.logging.MMLogger;
import megamek.utilities.FastJScrollPane;
import mekhq.MekHQ;
import mekhq.campaign.personnel.advancedCharacterBuilder.ATOWLifeStage;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePath;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathBuilderTabType;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;

public class LifePathLifePathPicker extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(LifePathLifePathPicker.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.LifePathLifePathPicker";

    private static final int MINIMUM_INSTRUCTIONS_WIDTH = scaleForGUI(250);
    private static final int MINIMUM_MAIN_WIDTH = scaleForGUI(575);
    private static final int MINIMUM_COMPONENT_HEIGHT = scaleForGUI(575);

    private static final int TEXT_PANEL_WIDTH = (int) round(MINIMUM_INSTRUCTIONS_WIDTH * 0.70);
    private static final String PANEL_HTML_FORMAT = "<html><div style='width:%dpx;'>%s</div></html>";

    private static final int PADDING = scaleForGUI(10);

    private JEditorPane txtTooltipDisplay;
    private final Set<UUID> storedLifePaths;
    private Set<UUID> selectedLifePaths;
    private final Map<ATOWLifeStage, Set<LifePath>> sortedLifePaths = new HashMap<>();
    private final Map<LifePath, List<JCheckBox>> lifePathOptionDictionary = new HashMap<>();

    Set<UUID> getSelectedLifePaths() {
        return selectedLifePaths;
    }

    private void setTxtTooltipDisplay(String newText) {
        txtTooltipDisplay.setText("<div style='text-align:center;'>" + newText + "</div>");
    }

    LifePathLifePathPicker(Set<UUID> selectedLifePaths, Map<UUID, LifePath> lifePathLibrary,
          LifePathBuilderTabType tabType) {
        super();

        // Defensive copies to avoid external modification
        this.selectedLifePaths = new HashSet<>(selectedLifePaths);
        storedLifePaths = new HashSet<>(selectedLifePaths);

        populateDictionaries(lifePathLibrary.values());

        setTitle(getTextAt(RESOURCE_BUNDLE, "LifePathLifePathPicker.title"));

        JPanel pnlInstructions = initializeInstructionsPanel(tabType);
        JPanel pnlOptions = buildLifeStagePanel();
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

        pnlMain.add(pnlOptions, BorderLayout.CENTER);
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
        pnlControls.setPreferredSize(scaleForGUI(0, 150));

        txtTooltipDisplay = new JEditorPane();
        txtTooltipDisplay.setContentType("text/html");
        txtTooltipDisplay.setEditable(false);
        txtTooltipDisplay.setBorder(new EmptyBorder(0, PADDING, 0, PADDING));
        setTxtTooltipDisplay("");

        FastJScrollPane scrollTooltipArea = new FastJScrollPane(txtTooltipDisplay);
        scrollTooltipArea.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollTooltipArea.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollTooltipArea.setBorder(null);
        scrollTooltipArea.setMinimumSize(scaleForGUI(250, 50));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        String titleCancel = getTextAt(RESOURCE_BUNDLE, "LifePathLifePathPicker.button.cancel");
        RoundedJButton btnCancel = new RoundedJButton(titleCancel);
        btnCancel.addActionListener(e -> {
            selectedLifePaths = storedLifePaths;
            dispose();
        });

        String titleConfirm = getTextAt(RESOURCE_BUNDLE, "LifePathLifePathPicker.button.confirm");
        RoundedJButton btnConfirm = new RoundedJButton(titleConfirm);
        btnConfirm.addActionListener(e -> dispose());

        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(btnCancel);
        buttonPanel.add(Box.createHorizontalStrut(PADDING));
        buttonPanel.add(btnConfirm);
        buttonPanel.add(Box.createHorizontalGlue());

        pnlControls.add(scrollTooltipArea);
        pnlControls.add(Box.createVerticalStrut(PADDING));
        pnlControls.add(buttonPanel);

        return pnlControls;
    }

    private void populateDictionaries(Collection<LifePath> allLifePaths) {
        for (LifePath lifePath : allLifePaths) {
            for (ATOWLifeStage lifeStage : lifePath.lifeStages()) {
                if (!sortedLifePaths.containsKey(lifeStage)) {
                    sortedLifePaths.put(lifeStage, new HashSet<>());
                }
                sortedLifePaths.get(lifeStage).add(lifePath);
            }

            lifePathOptionDictionary.put(lifePath, new ArrayList<>());
        }
    }

    private JPanel buildLifeStagePanel() {
        JPanel pnlLifeStages = new JPanel();
        pnlLifeStages.setLayout(new BoxLayout(pnlLifeStages, BoxLayout.Y_AXIS));

        String titleOptions = getTextAt(RESOURCE_BUNDLE, "LifePathLifePathPicker.options.label");
        pnlLifeStages.setBorder(createRoundedLineBorder(titleOptions));

        EnhancedTabbedPane optionPane = new EnhancedTabbedPane();
        for (ATOWLifeStage lifeStage : ATOWLifeStage.getOrderedLifeStages()) {
            Set<LifePath> lifePaths = sortedLifePaths.get(lifeStage);
            if (lifePaths == null) {
                continue;
            }
            
            optionPane.addTab(lifeStage.getDisplayName(), buildOptionsPanel(lifePaths));
        }

        pnlLifeStages.add(optionPane, BorderLayout.NORTH);
        return pnlLifeStages;
    }

    private EnhancedTabbedPane buildOptionsPanel(Set<LifePath> lifePaths) {
        List<LifePath> lifePaths1 = new ArrayList<>();
        List<LifePath> lifePaths2 = new ArrayList<>();
        List<LifePath> lifePaths3 = new ArrayList<>();
        List<LifePath> lifePaths4 = new ArrayList<>();
        List<LifePath> lifePaths5 = new ArrayList<>();

        List<LifePath> sortedLifePaths = new ArrayList<>(lifePaths);
        sortedLifePaths.sort(Comparator.comparing(LifePath::name));

        int groups = 3; // Can go up to 5 without additional code changes
        int n = sortedLifePaths.size();
        for (int i = 0; i < n; i++) {
            LifePath lifePath = sortedLifePaths.get(i);
            int groupIdx = (int) Math.floor(i * groups / (double) n);
            switch (groupIdx) {
                case 0 -> lifePaths1.add(lifePath);
                case 1 -> lifePaths2.add(lifePath);
                case 2 -> lifePaths3.add(lifePath);
                case 3 -> lifePaths4.add(lifePath);
                case 4 -> lifePaths5.add(lifePath);
            }
        }

        EnhancedTabbedPane optionPane = new EnhancedTabbedPane();

        if (!lifePaths1.isEmpty()) {
            buildTab(lifePaths1, optionPane, getLifePathOptions(lifePaths1));
        }

        if (!lifePaths2.isEmpty()) {
            buildTab(lifePaths2, optionPane, getLifePathOptions(lifePaths2));
        }

        if (!lifePaths3.isEmpty()) {
            buildTab(lifePaths3, optionPane, getLifePathOptions(lifePaths3));
        }

        if (!lifePaths4.isEmpty()) {
            buildTab(lifePaths4, optionPane, getLifePathOptions(lifePaths4));
        }

        if (!lifePaths5.isEmpty()) {
            buildTab(lifePaths5, optionPane, getLifePathOptions(lifePaths5));
        }

        return optionPane;
    }

    private static void buildTab(List<LifePath> lifePaths, EnhancedTabbedPane optionPane,
          FastJScrollPane pnlOptions) {
        String firstName = lifePaths.get(0).name();
        String lastName = lifePaths.get(lifePaths.size() - 1).name();

        char firstLetter = firstName.isEmpty() ? '\0' : firstName.charAt(0);
        char lastLetter = lastName.isEmpty() ? '\0' : lastName.charAt(0);

        optionPane.addTab(getFormattedTextAt(RESOURCE_BUNDLE,
              "LifePathLifePathPicker.options.roleplay.label",
              firstLetter,
              lastLetter), pnlOptions);
    }

    private FastJScrollPane getLifePathOptions(List<LifePath> lifePaths) {
        JPanel pnlLifePaths = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int columns = 3;
        for (int i = 0; i < lifePaths.size(); i++) {
            LifePath lifePath = lifePaths.get(i);
            String label = lifePath.name();

            UUID id = lifePath.id();
            boolean isEnabled = selectedLifePaths.contains(id);

            JLabel lblLifePath = new JLabel(label);
            lblLifePath.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    tooltipUpdater(lifePath);
                }
            });
            JCheckBox chkLifePath = GetLifePathCheckbox(isEnabled, id, lifePath);
            lifePathOptionDictionary.get(lifePath).add(chkLifePath);

            gbc.gridx = i % columns;
            gbc.gridy = i / columns;

            JPanel pnlRows = new JPanel();
            pnlRows.setLayout(new BoxLayout(pnlRows, BoxLayout.X_AXIS));
            pnlRows.add(chkLifePath);
            pnlRows.add(Box.createHorizontalStrut(PADDING));
            pnlRows.add(lblLifePath);
            pnlRows.setAlignmentX(Component.LEFT_ALIGNMENT);

            pnlLifePaths.add(pnlRows, gbc);
        }

        FastJScrollPane scrollLifePaths = new FastJScrollPane(pnlLifePaths);
        scrollLifePaths.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollLifePaths.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollLifePaths.setBorder(null);

        return scrollLifePaths;
    }

    private JCheckBox GetLifePathCheckbox(boolean isEnabled, UUID id, LifePath lifePath) {
        JCheckBox chkLifePath = new JCheckBox();
        chkLifePath.setSelected(isEnabled);
        chkLifePath.addActionListener(evt -> {
            if (chkLifePath.isSelected()) {
                selectedLifePaths.add(id);

                for (JCheckBox option : lifePathOptionDictionary.get(lifePath)) {
                    option.setSelected(true);
                }
            } else {
                selectedLifePaths.remove(id);

                for (JCheckBox option : lifePathOptionDictionary.get(lifePath)) {
                    option.setSelected(false);
                }
            }
        });
        chkLifePath.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                tooltipUpdater(lifePath);
            }
        });
        return chkLifePath;
    }

    private void tooltipUpdater(LifePath lifePath) {
        String format = "<html><b>(%s)</b> %s</html>";
        String lifeStages = lifePath.lifeStages().stream()
                                  .map(ATOWLifeStage::getDisplayName)
                                  .collect(Collectors.joining(","));
        String display = String.format(format, lifeStages, lifePath.flavorText());
        setTxtTooltipDisplay(display);
    }

    private JPanel initializeInstructionsPanel(LifePathBuilderTabType tabType) {
        JPanel pnlInstructions = new JPanel();

        String titleInstructions = getTextAt(RESOURCE_BUNDLE, "LifePathLifePathPicker.instructions.label");
        pnlInstructions.setBorder(createRoundedLineBorder(titleInstructions));

        JEditorPane txtInstructions = new JEditorPane();
        txtInstructions.setContentType("text/html");
        txtInstructions.setEditable(false);
        String instructions = String.format(PANEL_HTML_FORMAT, TEXT_PANEL_WIDTH,
              getTextAt(RESOURCE_BUNDLE, "LifePathLifePathPicker.instructions.text." + tabType.getLookupName()));
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
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(LifePathLifePathPicker.class);
            this.setName("LifePathLifePathPicker");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
    }
}
