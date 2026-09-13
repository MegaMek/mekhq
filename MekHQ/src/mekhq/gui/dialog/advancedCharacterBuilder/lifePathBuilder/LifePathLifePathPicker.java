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

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder.createRoundedLineBorder;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;

import megamek.common.annotations.Nullable;
import megamek.common.ui.EnhancedTabbedPane;
import megamek.common.ui.FastJScrollPane;
import megamek.logging.MMLogger;
import mekhq.campaign.personnel.advancedCharacterBuilder.ATOWLifeStage;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePath;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathBuilderTabType;

/**
 * Lets an author say which other Life Paths a Life Path requires or excludes.
 *
 * <p>The library is shown one tab per life stage, in the order a character passes through them. A Life Path that
 * belongs to several stages appears on each of their tabs, and ticking it on one tab ticks it on the others.</p>
 *
 * <p>The Life Path being edited is never offered, because a path cannot require or exclude itself.</p>
 *
 * @since 0.50.11
 */
class LifePathLifePathPicker extends AbstractLifePathPicker {
    private static final MMLogger LOGGER = MMLogger.create(LifePathLifePathPicker.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.LifePathLifePathPicker";

    private static final int MINIMUM_MAIN_WIDTH = scaleForGUI(575);
    private static final int MINIMUM_COMPONENT_HEIGHT = scaleForGUI(575);

    private final Set<UUID> storedLifePaths;
    private Set<UUID> selectedLifePaths;
    private final Map<ATOWLifeStage, Set<LifePath>> sortedLifePaths = new HashMap<>();
    /**
     * Every checkbox that represents a given Life Path, so the duplicates across life stage tabs can be kept in
     * step.
     *
     * <p>Keyed by id, not by the record. {@link LifePath} is a record, so its {@code hashCode} walks more than
     * fifty maps on every lookup.</p>
     */
    private final Map<UUID, List<JCheckBox>> lifePathOptionDictionary = new HashMap<>();

    Set<UUID> getSelectedLifePaths() {
        return selectedLifePaths;
    }

    /**
     * Opens the picker.
     *
     * @param owner             the wizard this picker belongs to
     * @param selectedLifePaths the Life Path identifiers already on this group
     * @param lifePathLibrary   every Life Path the campaign knows about
     * @param currentLifePathId the identifier of the Life Path being edited, which is never offered; may be
     *                          {@code null} for a path that has never been saved
     * @param tabType           the section being edited
     * @param groupIndex        the group being edited, shown in the title
     *
     * @since 0.50.11
     */
    LifePathLifePathPicker(@Nullable Window owner, Set<UUID> selectedLifePaths,
          Map<UUID, LifePath> lifePathLibrary, @Nullable UUID currentLifePathId, LifePathBuilderTabType tabType,
          int groupIndex) {
        super(owner, RESOURCE_BUNDLE, "LifePathLifePathPicker", tabType, groupIndex, MINIMUM_MAIN_WIDTH,
              MINIMUM_COMPONENT_HEIGHT);

        // Defensive copies to avoid external modification
        this.selectedLifePaths = new HashSet<>(selectedLifePaths);
        this.storedLifePaths = new HashSet<>(selectedLifePaths);

        populateDictionaries(lifePathLibrary.values(), currentLifePathId);

        buildAndShow();
    }

    /**
     * Sorts the library into life stages and prepares the checkbox lookup.
     *
     * @param allLifePaths      every Life Path in the library
     * @param currentLifePathId the Life Path being edited, which is left out
     *
     * @since 0.50.11
     */
    private void populateDictionaries(Collection<LifePath> allLifePaths, @Nullable UUID currentLifePathId) {
        for (LifePath lifePath : allLifePaths) {
            // A Life Path cannot require or exclude itself, so offering it only lets the author make a path the
            // validator then rejects.
            if (currentLifePathId != null && currentLifePathId.equals(lifePath.id())) {
                continue;
            }

            if (lifePath.lifeStages().isEmpty()) {
                // Every tab is a life stage, so a path belonging to none has nowhere to appear.
                LOGGER.warn("LifePath [{}] ({}) belongs to no life stage, so it cannot be offered here.",
                      lifePath.name(), lifePath.id());
                continue;
            }

            for (ATOWLifeStage lifeStage : lifePath.lifeStages()) {
                sortedLifePaths.computeIfAbsent(lifeStage, stage -> new HashSet<>()).add(lifePath);
            }

            lifePathOptionDictionary.put(lifePath.id(), new ArrayList<>());
        }
    }

    @Override
    protected JPanel buildOptionsPanel() {
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

            optionPane.addTab(lifeStage.getDisplayName(), buildLifeStageTabs(lifePaths));
        }

        pnlLifeStages.add(optionPane, BorderLayout.NORTH);
        return pnlLifeStages;
    }

    private EnhancedTabbedPane buildLifeStageTabs(Set<LifePath> lifePaths) {
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
            JCheckBox chkLifePath = createLifePathCheckbox(isEnabled, id, lifePath);
            lifePathOptionDictionary.get(lifePath.id()).add(chkLifePath);

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

    /**
     * Builds one Life Path checkbox, wired so that toggling it updates every copy of itself on the other tabs.
     *
     * @param isEnabled  whether the option can be changed
     * @param id         the Life Path's id
     * @param lifePath   the Life Path the checkbox represents
     *
     * @return the checkbox
     *
     * @since 0.50.11
     */
    private JCheckBox createLifePathCheckbox(boolean isEnabled, UUID id, LifePath lifePath) {
        JCheckBox chkLifePath = new JCheckBox();
        chkLifePath.setSelected(isEnabled);
        chkLifePath.addActionListener(actionEvent -> {
            if (chkLifePath.isSelected()) {
                selectedLifePaths.add(id);

                for (JCheckBox option : lifePathOptionDictionary.get(lifePath.id())) {
                    option.setSelected(true);
                }
            } else {
                selectedLifePaths.remove(id);

                for (JCheckBox option : lifePathOptionDictionary.get(lifePath.id())) {
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
        setLblTooltipDisplay(display);
    }

    @Override
    protected void restoreStoredSelection() {
        selectedLifePaths = new HashSet<>(storedLifePaths);
    }

    @Override
    protected void clearSelection() {
        selectedLifePaths.clear();
        rebuildOptions();
    }
}
