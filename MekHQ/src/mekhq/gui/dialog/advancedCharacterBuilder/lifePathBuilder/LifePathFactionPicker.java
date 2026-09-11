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

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;

import megamek.common.annotations.Nullable;
import megamek.common.ui.EnhancedTabbedPane;
import megamek.common.ui.FastJScrollPane;
import megamek.common.universe.FactionTag;
import megamek.logging.MMLogger;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathBuilderTabType;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;

/**
 * Lets an author choose the factions a Life Path requires or excludes.
 *
 * <p>There are around 190 factions, so they are split alphabetically across tabs, with the three aggregate factions
 * that stand for whole regions on a tab of their own. Special and hidden factions are not offered, because they are
 * not something a character belongs to.</p>
 *
 * <p>Factions are stored by code rather than by name, because a faction's name changes with the era.</p>
 *
 * @since 0.50.11
 */
class LifePathFactionPicker extends AbstractLifePathPicker {
    private static final MMLogger LOGGER = MMLogger.create(LifePathFactionPicker.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.LifePathFactionPicker";

    private static final int MINIMUM_MAIN_WIDTH = scaleForGUI(575);
    private static final int MINIMUM_COMPONENT_HEIGHT = scaleForGUI(525);

    /** How many alphabetical tabs the factions are spread across. */
    private static final int TAB_COUNT = 4;

    /** How many columns of checkboxes each tab holds. */
    private static final int COLUMN_COUNT = 3;

    /**
     * The codes of the factions that stand for a whole region rather than a single state.
     *
     * <p>"DP" used to be listed here as a fourth, but no faction by that code exists in the universe data, so it never
     * matched anything.</p>
     */
    private static final List<String> SUPER_FACTION_CODES = List.of("IS", "CLAN", "Periphery");

    private final Factions factions = Factions.getInstance();
    private final int gameYear;

    private final Set<String> storedFactions;
    private Set<String> selectedFactions;

    /**
     * Returns the faction codes the author settled on.
     *
     * @return the selected faction codes
     *
     * @since 0.50.11
     */
    Set<String> getSelectedFactions() {
        return selectedFactions;
    }

    /**
     * Opens the picker.
     *
     * @param owner            the wizard this picker belongs to
     * @param selectedFactions the faction codes already on this group
     * @param gameYear         the year faction names are shown for
     * @param tabType          the section being edited
     * @param groupIndex       the group being edited, shown in the title
     *
     * @since 0.50.11
     */
    LifePathFactionPicker(@Nullable Window owner, Set<String> selectedFactions, int gameYear,
          LifePathBuilderTabType tabType, int groupIndex) {
        super(owner, RESOURCE_BUNDLE, "LifePathFactionPicker", tabType, groupIndex, MINIMUM_MAIN_WIDTH,
              MINIMUM_COMPONENT_HEIGHT);

        this.gameYear = gameYear;

        // Defensive copies to avoid external modification
        this.selectedFactions = new HashSet<>(selectedFactions);
        this.storedFactions = new HashSet<>(selectedFactions);

        buildAndShow();
    }

    @Override
    protected JPanel buildOptionsPanel() {
        JPanel pnlOptions = new JPanel();
        pnlOptions.setLayout(new BoxLayout(pnlOptions, BoxLayout.Y_AXIS));
        pnlOptions.setBorder(createRoundedLineBorder(getPickerText("options.label")));

        List<String> superFactions = new ArrayList<>();
        List<Faction> selectableFactions = new ArrayList<>();

        for (Faction faction : factions.getFactions(false)) {
            if (SUPER_FACTION_CODES.contains(faction.getShortName())) {
                superFactions.add(faction.getShortName());
                continue;
            }

            // Hidden factions go for the same reason special ones do: they are not something a character belongs to.
            if (faction.is(FactionTag.SPECIAL) || faction.is(FactionTag.HIDDEN)) {
                continue;
            }

            selectableFactions.add(faction);
        }

        selectableFactions.sort(Comparator.comparing(faction -> faction.getFullName(gameYear)));

        EnhancedTabbedPane optionPane = new EnhancedTabbedPane();

        // One list per tab, sized by TAB_COUNT. This used to be ten separately named lists for four tabs, six of
        // which could never be filled.
        for (List<Faction> tabFactions : splitIntoTabs(selectableFactions)) {
            if (!tabFactions.isEmpty()) {
                buildTab(tabFactions, optionPane);
            }
        }

        if (!superFactions.isEmpty()) {
            optionPane.addTab(getPickerText("options.tab.special"), getFactionOptions(superFactions));
        }

        pnlOptions.add(optionPane);

        return pnlOptions;
    }

    /**
     * Splits the factions into one contiguous alphabetical block per tab.
     *
     * @param allFactions every selectable faction, already sorted by name
     *
     * @return one list per tab
     *
     * @since 0.50.11
     */
    private static List<List<Faction>> splitIntoTabs(List<Faction> allFactions) {
        List<List<Faction>> tabs = new ArrayList<>();
        int perTab = (int) Math.ceil(allFactions.size() / (double) TAB_COUNT);

        for (int tabIndex = 0; tabIndex < TAB_COUNT; tabIndex++) {
            int firstEntry = Math.min(tabIndex * perTab, allFactions.size());
            int lastEntry = Math.min(firstEntry + perTab, allFactions.size());
            tabs.add(new ArrayList<>(allFactions.subList(firstEntry, lastEntry)));
        }

        return tabs;
    }

    /**
     * Adds one alphabetical tab, titled with the range of initials it covers.
     *
     * @param tabFactions the factions on this tab
     * @param optionPane  the tabbed pane to add to
     *
     * @since 0.50.11
     */
    private void buildTab(List<Faction> tabFactions, EnhancedTabbedPane optionPane) {
        String firstName = tabFactions.getFirst().getFullName(gameYear);
        String lastName = tabFactions.getLast().getFullName(gameYear);

        char firstLetter = firstName.isEmpty() ? '\0' : firstName.charAt(0);
        char lastLetter = lastName.isEmpty() ? '\0' : lastName.charAt(0);

        List<String> factionCodes = new ArrayList<>();
        for (Faction faction : tabFactions) {
            factionCodes.add(faction.getShortName());
        }

        optionPane.addTab(getFormattedTextAt(RESOURCE_BUNDLE, "LifePathFactionPicker.options.tab", firstLetter,
              lastLetter), getFactionOptions(factionCodes));
    }

    /**
     * Builds the checkboxes for one tab's factions.
     *
     * @param factionOptions the faction codes to build checkboxes for
     *
     * @return the scrollable panel of checkboxes
     *
     * @since 0.50.11
     */
    private FastJScrollPane getFactionOptions(List<String> factionOptions) {
        JPanel pnlFactions = new JPanel(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;

        for (int factionIndex = 0; factionIndex < factionOptions.size(); factionIndex++) {
            String factionCode = factionOptions.get(factionIndex);
            Faction faction = factions.getFaction(factionCode);

            if (faction == null) {
                LOGGER.error("Faction not found: {}", factionCode);
                continue;
            }

            String label = getFormattedTextAt(RESOURCE_BUNDLE, "LifePathFactionPicker.options.faction.label",
                  faction.getFullName(gameYear), factionCode);
            JCheckBox chkFaction = new JCheckBox(label);
            chkFaction.setSelected(selectedFactions.contains(factionCode));
            chkFaction.addActionListener(actionEvent -> {
                if (chkFaction.isSelected()) {
                    selectedFactions.add(factionCode);
                } else {
                    selectedFactions.remove(factionCode);
                }
            });

            constraints.gridx = factionIndex % COLUMN_COUNT;
            constraints.gridy = factionIndex / COLUMN_COUNT;

            JPanel pnlRows = new JPanel();
            pnlRows.setLayout(new BoxLayout(pnlRows, BoxLayout.X_AXIS));
            pnlRows.add(chkFaction);
            pnlRows.setAlignmentX(Component.LEFT_ALIGNMENT);

            pnlFactions.add(pnlRows, constraints);
        }

        FastJScrollPane scrollFactions = new FastJScrollPane(pnlFactions);
        scrollFactions.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollFactions.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollFactions.setBorder(null);

        return scrollFactions;
    }

    @Override
    protected void restoreStoredSelection() {
        selectedFactions = new HashSet<>(storedFactions);
    }

    @Override
    protected void clearSelection() {
        selectedFactions.clear();
        rebuildOptions();
    }
}
