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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;

import megamek.common.annotations.Nullable;
import megamek.common.ui.FastJScrollPane;
import megamek.logging.MMLogger;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathBuilderTabType;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.utilities.TooltipMouseListenerUtil;
import mekhq.gui.utilities.JSuggestField;

/**
 * Lets an author name the planetary systems a Life Path requires or excludes.
 *
 * <p>There are well over two thousand systems, so they are not offered as a wall of checkboxes the way factions and
 * categories are. The author types a name instead, the same way the interstellar map's Find Planet box works, and each
 * system they accept is added to a list they can remove from.</p>
 *
 * <p>Systems are held by identifier rather than by name, because a system's name changes with the year and its
 * identifier does not.</p>
 *
 * @since 0.50.11
 */
class LifePathSystemPicker extends AbstractLifePathPicker {
    private static final MMLogger LOGGER = MMLogger.create(LifePathSystemPicker.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.LifePathSystemPicker";

    private static final int MINIMUM_MAIN_WIDTH = scaleForGUI(400);
    private static final int MINIMUM_COMPONENT_HEIGHT = scaleForGUI(450);

    private final Systems planetarySystems = Systems.getInstance();
    private final LocalDate today;

    private final Set<String> storedSystems;
    private Set<String> selectedSystems;

    private final DefaultListModel<String> selectedSystemsModel = new DefaultListModel<>();

    /**
     * Returns the system identifiers the author settled on.
     *
     * @return the selected system identifiers
     *
     * @since 0.50.11
     */
    Set<String> getSelectedSystems() {
        return selectedSystems;
    }

    /**
     * Opens the picker.
     *
     * @param owner           the wizard this picker belongs to
     * @param selectedSystems the system identifiers already on this group
     * @param today           the campaign's current date, used to name systems as they are known now
     * @param tabType         the section being edited
     * @param groupIndex      the group being edited, shown in the title
     *
     * @since 0.50.11
     */
    LifePathSystemPicker(@Nullable Window owner, Set<String> selectedSystems, LocalDate today,
          LifePathBuilderTabType tabType, int groupIndex) {
        super(owner, RESOURCE_BUNDLE, "LifePathSystemPicker", tabType, groupIndex, MINIMUM_MAIN_WIDTH,
              MINIMUM_COMPONENT_HEIGHT);

        this.today = today;

        // Defensive copies to avoid external modification
        this.selectedSystems = new HashSet<>(selectedSystems);
        this.storedSystems = new HashSet<>(selectedSystems);

        buildAndShow();
    }

    @Override
    protected JPanel buildOptionsPanel() {
        JPanel pnlOptions = new JPanel();
        pnlOptions.setLayout(new BoxLayout(pnlOptions, BoxLayout.Y_AXIS));
        pnlOptions.setBorder(createRoundedLineBorder(getPickerText("options.label")));

        String tooltipSearch = getPickerText("search.tooltip");

        JLabel lblSearch = new JLabel(getPickerText("search.label"));
        lblSearch.addMouseListener(TooltipMouseListenerUtil.forTooltip(this::setLblTooltipDisplay, tooltipSearch));

        JSuggestField fieldSearch = new JSuggestField(this, buildSystemNameSuggestions());
        fieldSearch.addMouseListener(TooltipMouseListenerUtil.forTooltip(this::setLblTooltipDisplay, tooltipSearch));

        RoundedJButton btnAdd = new RoundedJButton(getPickerText("button.add"));
        btnAdd.addMouseListener(TooltipMouseListenerUtil.forTooltip(this::setLblTooltipDisplay, tooltipSearch));

        // Both the button and accepting a suggestion do the same thing, because JSuggestField fires an action when a
        // suggestion is picked.
        btnAdd.addActionListener(actionEvent -> addTypedSystem(fieldSearch));
        fieldSearch.addActionListener(actionEvent -> addTypedSystem(fieldSearch));

        JPanel pnlSearchRow = new JPanel();
        pnlSearchRow.setLayout(new BoxLayout(pnlSearchRow, BoxLayout.X_AXIS));
        pnlSearchRow.add(lblSearch);
        pnlSearchRow.add(Box.createHorizontalStrut(PADDING));
        pnlSearchRow.add(fieldSearch);
        pnlSearchRow.add(Box.createHorizontalStrut(PADDING));
        pnlSearchRow.add(btnAdd);
        pnlSearchRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        refreshSelectedSystems();

        JList<String> listSelected = new JList<>(selectedSystemsModel);
        listSelected.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        FastJScrollPane scrollSelected = new FastJScrollPane(listSelected);
        scrollSelected.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollSelected.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollSelected.setPreferredSize(new Dimension(MINIMUM_MAIN_WIDTH, MINIMUM_COMPONENT_HEIGHT));
        scrollSelected.setAlignmentX(Component.LEFT_ALIGNMENT);

        RoundedJButton btnRemove = new RoundedJButton(getPickerText("button.remove"));
        btnRemove.addActionListener(actionEvent -> removeSelectedSystems(listSelected));
        btnRemove.setAlignmentX(Component.LEFT_ALIGNMENT);

        pnlOptions.add(pnlSearchRow);
        pnlOptions.add(Box.createVerticalStrut(PADDING));
        pnlOptions.add(scrollSelected);
        pnlOptions.add(Box.createVerticalStrut(PADDING));
        pnlOptions.add(btnRemove);

        return pnlOptions;
    }

    /**
     * Returns every system name the author can type, in alphabetical order.
     *
     * @return the suggestion data for the typing field
     *
     * @since 0.50.11
     */
    private Vector<String> buildSystemNameSuggestions() {
        List<String> names = new ArrayList<>();

        for (PlanetarySystem system : planetarySystems.getSystems().values()) {
            String name = system.getName(today);

            if (name != null && !name.isBlank()) {
                names.add(name);
            }
        }

        names.sort(Comparator.naturalOrder());

        return new Vector<>(names);
    }

    /**
     * Adds whatever system the author has typed, if it names one.
     *
     * @param fieldSearch the typing field
     *
     * @since 0.50.11
     */
    private void addTypedSystem(JSuggestField fieldSearch) {
        String typedName = fieldSearch.getText();

        if (typedName == null || typedName.isBlank()) {
            return;
        }

        PlanetarySystem system = planetarySystems.getSystemByName(typedName, today);

        if (system == null) {
            setLblTooltipDisplay(getFormattedTextAt(RESOURCE_BUNDLE, "LifePathSystemPicker.search.notFound",
                  typedName));
            return;
        }

        selectedSystems.add(system.getId());
        fieldSearch.setText("");
        refreshSelectedSystems();
    }

    /**
     * Removes whichever systems are highlighted in the list.
     *
     * @param listSelected the list of chosen systems
     *
     * @since 0.50.11
     */
    private void removeSelectedSystems(JList<String> listSelected) {
        for (String displayName : listSelected.getSelectedValuesList()) {
            String systemId = findSystemIdByDisplayName(displayName);

            if (systemId != null) {
                selectedSystems.remove(systemId);
            }
        }

        refreshSelectedSystems();
    }

    /**
     * Rebuilds the list of chosen systems from the current selection.
     *
     * @since 0.50.11
     */
    private void refreshSelectedSystems() {
        List<String> displayNames = new ArrayList<>();

        for (String systemId : selectedSystems) {
            displayNames.add(buildDisplayName(systemId));
        }

        displayNames.sort(Comparator.naturalOrder());

        selectedSystemsModel.clear();
        for (String displayName : displayNames) {
            selectedSystemsModel.addElement(displayName);
        }
    }

    /**
     * Returns how a chosen system is shown in the list.
     *
     * <p>The identifier is shown alongside the name because the identifier is what the file stores, so a system the
     * universe data no longer knows about can still be seen and removed instead of appearing as a blank row.</p>
     *
     * @param systemId the system's identifier
     *
     * @return the text for the list row
     *
     * @since 0.50.11
     */
    private String buildDisplayName(String systemId) {
        PlanetarySystem system = planetarySystems.getSystemById(systemId);

        if (system == null) {
            LOGGER.warn("Life Path names planetary system {}, which is not in the universe data.", systemId);
            return getFormattedTextAt(RESOURCE_BUNDLE, "LifePathSystemPicker.options.system.unknown", systemId);
        }

        return getFormattedTextAt(RESOURCE_BUNDLE, "LifePathSystemPicker.options.system.label",
              system.getName(today), systemId);
    }

    /**
     * Returns the system identifier behind a row in the list.
     *
     * @param displayName the row's text
     *
     * @return the system identifier, or {@code null} when no selected system produces that row
     *
     * @since 0.50.11
     */
    private @Nullable String findSystemIdByDisplayName(String displayName) {
        for (String systemId : selectedSystems) {
            if (buildDisplayName(systemId).equals(displayName)) {
                return systemId;
            }
        }

        return null;
    }

    @Override
    protected void restoreStoredSelection() {
        selectedSystems = new HashSet<>(storedSystems);
    }

    @Override
    protected void clearSelection() {
        selectedSystems.clear();
        rebuildOptions();
    }
}
