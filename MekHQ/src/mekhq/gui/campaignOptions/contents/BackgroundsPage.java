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

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.LEGACY_RULE_BEFORE_METADATA;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getMetadata;

import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Comparator;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.client.ui.comboBoxes.MMComboBox;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;

/**
 * The {@code BackgroundsPage} class builds and manages the Backgrounds leaf page of the Biography section of the
 * Campaign Options dialog. It owns the widgets for randomized backgrounds and random origin determination - random
 * personalities, simulated relationships, the specified planetary system/planet pickers, and origin search settings -
 * and synchronises them with a shared {@link BiographyOptionsModel}.
 *
 * <p>This view is a sub-component of {@link BiographyPages}: the model snapshot and the overall load/apply lifecycle
 * still live on {@code BiographyPages}, while this class is responsible only for constructing the Backgrounds panel and
 * copying its values to and from the model. Building the page requires the active {@link Campaign} (for the universe of
 * planetary systems) and the {@link GeneralPage} view (for the campaign date and faction). The page is built lazily;
 * until {@link #createPanel(BiographyOptionsModel, Campaign, GeneralPage)} is called,
 * {@link #readFromModel(BiographyOptionsModel)} and {@link #writeToModel(BiographyOptionsModel)} are no-ops.</p>
 */
class BackgroundsPage {
    private static final int LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    // Wider than the default control column because the Biography combo boxes need the extra room.
    private static final int CONTROL_COLUMN_WIDTH = 240;

    private Campaign campaign;
    private GeneralPage generalPage;

    private CampaignOptionsHeaderPanel backgroundHeader;
    private JPanel pnlRandomBackgrounds;
    private JCheckBox chkUseRandomPersonalities;
    private JCheckBox chkUseRandomPersonalityReputation;
    private JCheckBox chkUseReasoningXpMultiplier;
    private JCheckBox chkUseSimulatedRelationships;
    private JPanel pnlRandomOriginOptions;
    private JCheckBox chkRandomizeOrigin;
    private JCheckBox chkRandomizeDependentsOrigin;
    private JCheckBox chkRandomizeAroundSpecifiedPlanet;
    private JCheckBox chkSpecifiedSystemFactionSpecific;
    private JLabel lblSpecifiedSystem;
    private MMComboBox<PlanetarySystem> comboSpecifiedSystem;
    private JLabel lblSpecifiedPlanet;
    private MMComboBox<Planet> comboSpecifiedPlanet;
    private JLabel lblOriginSearchRadius;
    private JSpinner spnOriginSearchRadius;
    private JLabel lblOriginDistanceScale;
    private JSpinner spnOriginDistanceScale;
    private JCheckBox chkAllowClanOrigins;
    private JCheckBox chkExtraRandomOrigin;

    private boolean created;

    /**
     * Builds the Backgrounds page, populates its controls from the supplied model, and returns the assembled panel.
     *
     * @param model       the shared biography options model to populate the freshly built controls from
     * @param campaign    the active campaign, used to enumerate the available planetary systems
     * @param generalPage the General view, used to resolve the current campaign date and faction
     *
     * @return a {@link JPanel} representing the Backgrounds Page
     */
    @Nonnull JPanel createPanel(@Nullable BiographyOptionsModel model, @Nonnull Campaign campaign,
            GeneralPage generalPage) {
        this.campaign = campaign;
        this.generalPage = generalPage;

        // Header
        String imageAddress = getImageDirectory() + "logo_nueva_castile.png";
        backgroundHeader = new CampaignOptionsHeaderPanel("BackgroundsPage", imageAddress);

        // Contents
        comboSpecifiedSystem = new MMComboBox<>("comboSpecifiedSystem");
        comboSpecifiedPlanet = new MMComboBox<>("comboSpecifiedPlanet");

        pnlRandomOriginOptions = createRandomOriginOptionsPanel();
        pnlRandomBackgrounds = createRandomBackgroundsPanel();
        JPanel panel = CampaignOptionsPagePanel.builder("BackgroundsPage", "BackgroundsPage", imageAddress)
            .header(backgroundHeader)
            .quote("backgroundsPage")
            .section("lblRandomOriginOptionsPanel.text",
                "lblRandomOriginOptionsPanel.summary",
                pnlRandomOriginOptions,
                getMetadata(null, CampaignOptionFlag.CUSTOM_SYSTEM))
            .section("lblRandomBackgroundsPanel.text",
                "lblRandomBackgroundsPanel.summary",
                pnlRandomBackgrounds)
            .build();

        created = true;
        readFromModel(model);

        return panel;
    }

    /**
     * Creates the panel for configuring random background options in the campaign.
     * <p>
     * This includes controls to enable or disable features such as:
     * <p>
     * <li>Random personalities for characters.</li>
     * <li>Random personality reputation.</li>
     * <li>Reasoning XP multipliers.</li>
     * <li>Simulated relationships.</li>
     * </p>
     *
     * @return A {@code JPanel} representing the random background configuration UI.
     */
    private @Nonnull JPanel createRandomBackgroundsPanel() {
        // Contents
        chkUseRandomPersonalities = new CampaignOptionsCheckBox("UseRandomPersonalities",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.DOCUMENTED));
        chkUseRandomPersonalities.addMouseListener(createTipPanelUpdater("UseRandomPersonalities"));
        chkUseRandomPersonalityReputation = new CampaignOptionsCheckBox("UseRandomPersonalityReputation");
        chkUseRandomPersonalityReputation.addMouseListener(createTipPanelUpdater("UseRandomPersonalityReputation"));
        chkUseReasoningXpMultiplier = new CampaignOptionsCheckBox("UseReasoningXpMultiplier");
        chkUseReasoningXpMultiplier.addMouseListener(createTipPanelUpdater("UseReasoningXpMultiplier"));
        chkUseSimulatedRelationships = new CampaignOptionsCheckBox("UseSimulatedRelationships",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        chkUseSimulatedRelationships.addMouseListener(createTipPanelUpdater("UseSimulatedRelationships"));

        // Layout the Panels
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("RandomBackgroundsPanel",
            LABEL_COLUMN_WIDTH,
            CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2,
                chkUseRandomPersonalities,
                chkUseRandomPersonalityReputation,
                chkUseReasoningXpMultiplier,
                chkUseSimulatedRelationships);

        return panel;
    }

    /**
     * Creates and returns a panel for random origin options. This includes:
     * <p>
     * <li>Controls to enable or disable randomization of origins.</li>
     * <li>Options for selecting specific planetary systems or factions for origin
     * determination.</li>
     * <li>Search radius and distance scaling fields to tweak origin
     * calculations.</li>
     * </p>
     *
     * @return A `JPanel` for managing random origin settings.
     */
    private @Nonnull JPanel createRandomOriginOptionsPanel() {
        // Contents
        chkRandomizeOrigin = new CampaignOptionsCheckBox("RandomizeOrigin");
        chkRandomizeOrigin.addMouseListener(createTipPanelUpdater("RandomizeOrigin"));
        chkRandomizeDependentsOrigin = new CampaignOptionsCheckBox("RandomizeDependentsOrigin");
        chkRandomizeDependentsOrigin.addMouseListener(createTipPanelUpdater("RandomizeDependentsOrigin"));

        chkRandomizeAroundSpecifiedPlanet = new CampaignOptionsCheckBox("RandomizeAroundSpecifiedPlanet");
        chkRandomizeAroundSpecifiedPlanet.addActionListener(evt -> refreshSystemsAndPlanets());
        chkRandomizeAroundSpecifiedPlanet.addMouseListener(createTipPanelUpdater("RandomizeAroundSpecifiedPlanet"));

        chkSpecifiedSystemFactionSpecific = new CampaignOptionsCheckBox("SpecifiedSystemFactionSpecific",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        chkSpecifiedSystemFactionSpecific.addActionListener(evt -> refreshSystemsAndPlanets());
        chkSpecifiedSystemFactionSpecific.addMouseListener(createTipPanelUpdater("SpecifiedSystemFactionSpecific"));

        lblSpecifiedSystem = new CampaignOptionsLabel("SpecifiedSystem");
        lblSpecifiedSystem.addMouseListener(createTipPanelUpdater("SpecifiedSystem"));
        comboSpecifiedSystem.setModel(new DefaultComboBoxModel<>(
                getPlanetarySystems(chkSpecifiedSystemFactionSpecific.isSelected() ? generalPage.getFaction() : null)));
        comboSpecifiedSystem.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
                    final boolean isSelected, final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof PlanetarySystem) {
                    setText(((PlanetarySystem) value).getName(generalPage.getDate()));
                }
                return this;
            }
        });
        comboSpecifiedSystem.addActionListener(evt -> {
            final PlanetarySystem planetarySystem = comboSpecifiedSystem.getSelectedItem();
            final Planet planet = comboSpecifiedPlanet.getSelectedItem();
            if ((planetarySystem == null) || ((planet != null) && !planet.getParentSystem().equals(planetarySystem))) {
                restoreComboSpecifiedPlanet();
            }
        });
        comboSpecifiedSystem.addMouseListener(createTipPanelUpdater("SpecifiedSystem"));

        lblSpecifiedPlanet = new CampaignOptionsLabel("SpecifiedPlanet");
        lblSpecifiedPlanet.addMouseListener(createTipPanelUpdater("SpecifiedPlanet"));
        final PlanetarySystem planetarySystem = comboSpecifiedSystem.getSelectedItem();
        if (planetarySystem != null) {
            comboSpecifiedPlanet.setModel(new DefaultComboBoxModel<>(planetarySystem.getPlanets()
                    .toArray(new Planet[] {})));
        }
        comboSpecifiedPlanet.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
                    final boolean isSelected, final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Planet) {
                    setText(((Planet) value).getName(generalPage.getDate()));
                }
                return this;
            }
        });
        comboSpecifiedPlanet.addMouseListener(createTipPanelUpdater("SpecifiedPlanet"));

        lblOriginSearchRadius = new CampaignOptionsLabel("OriginSearchRadius");
        lblOriginSearchRadius.addMouseListener(createTipPanelUpdater("OriginSearchRadius"));
        spnOriginSearchRadius = new CampaignOptionsSpinner("OriginSearchRadius", 0, 0, 2000, 25);
        spnOriginSearchRadius.addMouseListener(createTipPanelUpdater("OriginSearchRadius"));

        lblOriginDistanceScale = new CampaignOptionsLabel("OriginDistanceScale",
                getMetadata(null, CampaignOptionFlag.IMPORTANT));
        lblOriginDistanceScale.addMouseListener(createTipPanelUpdater("OriginDistanceScale"));
        spnOriginDistanceScale = new CampaignOptionsSpinner("OriginDistanceScale", 0.6, 0.1, 2.0, 0.1);
        spnOriginDistanceScale.addMouseListener(createTipPanelUpdater("OriginDistanceScale"));

        chkAllowClanOrigins = new CampaignOptionsCheckBox("AllowClanOrigins");
        chkAllowClanOrigins.addMouseListener(createTipPanelUpdater("AllowClanOrigins"));
        chkExtraRandomOrigin = new CampaignOptionsCheckBox("ExtraRandomOrigin");
        chkExtraRandomOrigin.addMouseListener(createTipPanelUpdater("ExtraRandomOrigin"));

        // The system/planet combos are backed by the whole universe, so an unprototyped
        // combo would size itself to
        // the widest entry and stretch this section wider than the other Biography
        // sub-pages. Pin them to the control
        // column and surface the full selected value as a tooltip (it remains fully
        // visible in the dropdown).
        capComboWidthWithTooltip(comboSpecifiedSystem);
        capComboWidthWithTooltip(comboSpecifiedPlanet);

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("RandomOriginOptionsPanel",
            LABEL_COLUMN_WIDTH,
            CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2,
            chkRandomizeOrigin,
            chkRandomizeDependentsOrigin,
            chkRandomizeAroundSpecifiedPlanet,
            chkSpecifiedSystemFactionSpecific);
        panel.addRow(lblSpecifiedSystem, comboSpecifiedSystem);
        panel.addRow(lblSpecifiedPlanet, comboSpecifiedPlanet);
        panel.addRow(lblOriginSearchRadius, spnOriginSearchRadius);
        panel.addRow(lblOriginDistanceScale, spnOriginDistanceScale);
        panel.addCheckBoxGrid(2, chkAllowClanOrigins, chkExtraRandomOrigin);

        return panel;
    }

    /**
     * Pins {@code combo}'s preferred width to the form's control column so a
     * long-content model (such as the full list
     * of planetary systems or planets) cannot stretch the section wider than its
     * siblings. The combo still fills the
     * control column through the form's horizontal-fill layout, and the full
     * selected value stays available as a
     * tooltip (and in the dropdown). Without this, an unprototyped
     * {@link javax.swing.JComboBox} measures every model
     * entry and adopts the widest, which made the Backgrounds page noticeably wider
     * than the other Biography sub-pages.
     *
     * @param combo the combo box to constrain and decorate with a full-value
     *              tooltip
     */
    private void capComboWidthWithTooltip(MMComboBox<?> combo) {
        combo.setPreferredSize(new Dimension(CONTROL_COLUMN_WIDTH, combo.getPreferredSize().height));
        updateComboTooltip(combo);
        combo.addActionListener(evt -> updateComboTooltip(combo));
    }

    /**
     * Sets {@code combo}'s tooltip to the full, date-aware name of its selected
     * planetary system or planet, so a value
     * that is truncated with an ellipsis in the collapsed field can still be read
     * in full on hover.
     *
     * @param combo the combo box whose tooltip should reflect its current selection
     */
    private void updateComboTooltip(MMComboBox<?> combo) {
        final Object selected = combo.getSelectedItem();
        if (selected instanceof PlanetarySystem system) {
            combo.setToolTipText(system.getName(generalPage.getDate()));
        } else if (selected instanceof Planet planet) {
            combo.setToolTipText(planet.getName(generalPage.getDate()));
        } else {
            combo.setToolTipText(null);
        }
    }

    /**
     * Refreshes the planetary systems and planets displayed in the associated combo
     * boxes.
     *
     * <p>
     * This method first stores the currently selected planetary system and planet.
     * It then
     * restores the list of available planetary systems by repopulating the
     * `comboSpecifiedSystem`. Finally, it
     * re-selects the previously selected planetary system and planet in their
     * respective combo boxes.
     * </p>
     *
     * <p>
     * The method ensures that the user selection persists even after the combo
     * boxes are refreshed.
     * Any exceptions during the selection process are caught and ignored. As if we
     * can't restore the selection, that's
     * fine, we just use the fallback index of 0.
     * </p>
     */
    private void refreshSystemsAndPlanets() {
        final PlanetarySystem planetarySystem = comboSpecifiedSystem.getSelectedItem();
        final Planet planet = comboSpecifiedPlanet.getSelectedItem();

        restoreComboSpecifiedSystem();

        try {
            comboSpecifiedSystem.setSelectedItem(planetarySystem);
            comboSpecifiedPlanet.setSelectedItem(planet);
        } catch (Exception ignored) {
        }
    }

    /**
     * Resets the planet combo box to show only the planets matching the currently
     * selected planetary system.
     */
    private void restoreComboSpecifiedPlanet() {
        final PlanetarySystem planetarySystem = comboSpecifiedSystem.getSelectedItem();

        if (planetarySystem == null) {
            comboSpecifiedPlanet.removeAllItems();
        } else {
            comboSpecifiedPlanet.setModel(new DefaultComboBoxModel<>(planetarySystem.getPlanets()
                    .toArray(new Planet[] {})));
            comboSpecifiedPlanet.setSelectedItem(planetarySystem.getPrimaryPlanet());
        }
    }

    /**
     * Resets the system combo box to show only the planetary systems that match the
     * current faction, if applicable.
     */
    private void restoreComboSpecifiedSystem() {
        comboSpecifiedSystem.removeAllItems();

        comboSpecifiedSystem.setModel(new DefaultComboBoxModel<>(
                getPlanetarySystems(chkSpecifiedSystemFactionSpecific.isSelected() ? generalPage.getFaction() : null)));

        restoreComboSpecifiedPlanet();
    }

    /**
     * Filters planetary systems based on a given faction (if specified) and returns
     * a sorted array of matches.
     *
     * @param faction The faction to filter planetary systems by (nullable). If
     *                `null`, all systems are included.
     *
     * @return An array of `PlanetarySystem` objects meeting the filter criteria.
     */
    private PlanetarySystem[] getPlanetarySystems(final @Nullable Faction faction) {
        ArrayList<PlanetarySystem> systems = campaign.getSystems();
        ArrayList<PlanetarySystem> filteredSystems = new ArrayList<>();

        // Filter systems
        for (PlanetarySystem planetarySystem : systems) {
            if ((faction == null) || planetarySystem.getFactionSet(generalPage.getDate()).contains(faction)) {
                filteredSystems.add(planetarySystem);
            }
        }

        // Sort systems
        filteredSystems.sort(Comparator.comparing(p -> p.getName(generalPage.getDate())));

        // Convert to array
        return filteredSystems.toArray(new PlanetarySystem[0]);
    }

    /**
     * Copies background values from the shared model into this page's controls. This is a no-op until the page has been
     * built.
     *
     * @param model the shared biography options model to read values from
     */
    void readFromModel(@Nullable BiographyOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        chkUseRandomPersonalities.setSelected(model.useRandomPersonalities);
        chkUseRandomPersonalityReputation.setSelected(model.useRandomPersonalityReputation);
        chkUseReasoningXpMultiplier.setSelected(model.useReasoningXpMultiplier);
        chkUseSimulatedRelationships.setSelected(model.useSimulatedRelationships);
        chkRandomizeOrigin.setSelected(model.randomizeOrigin);
        chkRandomizeDependentsOrigin.setSelected(model.randomizeDependentOrigin);
        chkRandomizeAroundSpecifiedPlanet.setSelected(model.randomizeAroundSpecifiedPlanet);
        if (model.specifiedPlanet != null) {
            comboSpecifiedSystem.setSelectedItem(model.specifiedPlanet.getParentSystem());
            comboSpecifiedPlanet.setSelectedItem(model.specifiedPlanet);
        }
        spnOriginSearchRadius.setValue(model.originSearchRadius);
        spnOriginDistanceScale.setValue(model.originDistanceScale);
        chkAllowClanOrigins.setSelected(model.allowClanOrigins);
        chkExtraRandomOrigin.setSelected(model.extraRandomOrigin);
    }

    /**
     * Copies background values from this page's controls into the shared model. This is a no-op until the page has been
     * built.
     *
     * @param model the shared biography options model to write values into
     */
    void writeToModel(@Nullable BiographyOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        model.useRandomPersonalities = chkUseRandomPersonalities.isSelected();
        model.useRandomPersonalityReputation = chkUseRandomPersonalityReputation.isSelected();
        model.useReasoningXpMultiplier = chkUseReasoningXpMultiplier.isSelected();
        model.useSimulatedRelationships = chkUseSimulatedRelationships.isSelected();
        model.randomizeOrigin = chkRandomizeOrigin.isSelected();
        model.randomizeDependentOrigin = chkRandomizeDependentsOrigin.isSelected();
        model.randomizeAroundSpecifiedPlanet = chkRandomizeAroundSpecifiedPlanet.isSelected();
        model.specifiedPlanet = comboSpecifiedPlanet.getSelectedItem();
        model.originSearchRadius = (int) spnOriginSearchRadius.getValue();
        model.originDistanceScale = (double) spnOriginDistanceScale.getValue();
        model.allowClanOrigins = chkAllowClanOrigins.isSelected();
        model.extraRandomOrigin = chkExtraRandomOrigin.isSelected();
    }
}
