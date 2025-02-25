/*
 * Copyright (c) 2021-2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.panels;

import java.awt.Component;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;

import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.enums.ValidationState;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.RandomOriginOptions;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.gui.baseComponents.AbstractMHQPanel;
import mekhq.gui.displayWrappers.FactionDisplay;

/**
 * This is used to select a set of RandomOriginOptions. It requires either the
 * faction or the
 * ComboBox from which a faction is selected, the former when the faction is
 * constant and the
 * latter when it can change after this panel is initialized.
 *
 * @author Justin "Windchild" Bowen
 */
public class RandomOriginOptionsPanel extends AbstractMHQPanel {
    private static final MMLogger logger = MMLogger.create(RandomOriginOptionsPanel.class);

    // region Variable Declarations
    private final Campaign campaign;
    private final Faction faction;
    private final MMComboBox<FactionDisplay> comboFaction;

    private JCheckBox chkRandomizeOrigin;
    private JCheckBox chkRandomizeDependentsOrigin;
    private JCheckBox chkRandomizeAroundSpecifiedPlanet;
    private JCheckBox chkSpecifiedSystemFactionSpecific;
    private MMComboBox<PlanetarySystem> comboSpecifiedSystem;
    private MMComboBox<Planet> comboSpecifiedPlanet;
    private JSpinner spnOriginSearchRadius;
    private JSpinner spnOriginDistanceScale;
    private JCheckBox chkAllowClanOrigins;
    private JCheckBox chkExtraRandomOrigin;
    // endregion Variable Declarations

    // region Constructors
    public RandomOriginOptionsPanel(final JFrame frame, final Campaign campaign,
            final Faction faction) {
        this(frame, campaign, faction, null);
    }

    public RandomOriginOptionsPanel(final JFrame frame, final Campaign campaign,
            final MMComboBox<FactionDisplay> comboFaction) {
        this(frame, campaign, null, comboFaction);
    }

    private RandomOriginOptionsPanel(final JFrame frame, final Campaign campaign,
            final @Nullable Faction faction,
            final @Nullable MMComboBox<FactionDisplay> comboFaction) {
        super(frame, "RandomOriginOptionsPanel");
        this.campaign = campaign;
        this.faction = faction;
        this.comboFaction = comboFaction;
        initialize();
    }
    // endregion Constructors

    // region Getters/Setters
    public Campaign getCampaign() {
        return campaign;
    }

    public Faction getFaction() {
        return (getComboFaction() == null) ? getFactionDirect()
                : Objects.requireNonNull(getComboFaction().getSelectedItem()).getFaction();
    }

    private @Nullable Faction getFactionDirect() {
        return faction;
    }

    public @Nullable MMComboBox<FactionDisplay> getComboFaction() {
        return comboFaction;
    }

    public JCheckBox getChkRandomizeOrigin() {
        return chkRandomizeOrigin;
    }

    public void setChkRandomizeOrigin(final JCheckBox chkRandomizeOrigin) {
        this.chkRandomizeOrigin = chkRandomizeOrigin;
    }

    public JCheckBox getChkRandomizeDependentsOrigin() {
        return chkRandomizeDependentsOrigin;
    }

    public void setChkRandomizeDependentsOrigin(final JCheckBox chkRandomizeDependentsOrigin) {
        this.chkRandomizeDependentsOrigin = chkRandomizeDependentsOrigin;
    }

    public JCheckBox getChkRandomizeAroundSpecifiedPlanet() {
        return chkRandomizeAroundSpecifiedPlanet;
    }

    public void setChkRandomizeAroundSpecifiedPlanet(final JCheckBox chkRandomizeAroundSpecifiedPlanet) {
        this.chkRandomizeAroundSpecifiedPlanet = chkRandomizeAroundSpecifiedPlanet;
    }

    public JCheckBox getChkSpecifiedSystemFactionSpecific() {
        return chkSpecifiedSystemFactionSpecific;
    }

    public void setChkSpecifiedSystemFactionSpecific(final JCheckBox chkSpecifiedSystemFactionSpecific) {
        this.chkSpecifiedSystemFactionSpecific = chkSpecifiedSystemFactionSpecific;
    }

    public MMComboBox<PlanetarySystem> getComboSpecifiedSystem() {
        return comboSpecifiedSystem;
    }

    public void setComboSpecifiedSystem(final MMComboBox<PlanetarySystem> comboSpecifiedSystem) {
        this.comboSpecifiedSystem = comboSpecifiedSystem;
    }

    private void restoreComboSpecifiedSystem() {
        getComboSpecifiedSystem().removeAllItems();
        getComboSpecifiedSystem().setModel(new DefaultComboBoxModel<>(getPlanetarySystems(
                getChkSpecifiedSystemFactionSpecific().isSelected() ? getFaction() : null)));
        restoreComboSpecifiedPlanet();
    }

    public MMComboBox<Planet> getComboSpecifiedPlanet() {
        return comboSpecifiedPlanet;
    }

    public void setComboSpecifiedPlanet(final MMComboBox<Planet> comboSpecifiedPlanet) {
        this.comboSpecifiedPlanet = comboSpecifiedPlanet;
    }

    private void restoreComboSpecifiedPlanet() {
        final PlanetarySystem planetarySystem = getComboSpecifiedSystem().getSelectedItem();
        if (planetarySystem == null) {
            getComboSpecifiedPlanet().removeAllItems();
        } else {
            getComboSpecifiedPlanet().setModel(new DefaultComboBoxModel<>(
                    planetarySystem.getPlanets().toArray(new Planet[] {})));
            getComboSpecifiedPlanet().setSelectedItem(planetarySystem.getPrimaryPlanet());
        }
    }

    public JSpinner getSpnOriginSearchRadius() {
        return spnOriginSearchRadius;
    }

    public void setSpnOriginSearchRadius(final JSpinner spnOriginSearchRadius) {
        this.spnOriginSearchRadius = spnOriginSearchRadius;
    }

    public JSpinner getSpnOriginDistanceScale() {
        return spnOriginDistanceScale;
    }

    public void setSpnOriginDistanceScale(final JSpinner spnOriginDistanceScale) {
        this.spnOriginDistanceScale = spnOriginDistanceScale;
    }

    public JCheckBox getChkAllowClanOrigins() {
        return chkAllowClanOrigins;
    }

    public void setChkAllowClanOrigins(final JCheckBox chkAllowClanOrigins) {
        this.chkAllowClanOrigins = chkAllowClanOrigins;
    }

    public JCheckBox getChkExtraRandomOrigin() {
        return chkExtraRandomOrigin;
    }

    public void setChkExtraRandomOrigin(final JCheckBox chkExtraRandomOrigin) {
        this.chkExtraRandomOrigin = chkExtraRandomOrigin;
    }
    // endregion Getters/Setters

    // region Initialization
    @Override
    protected void initialize() {
        // Initialize Labels Used in ActionListeners
        final JLabel lblSpecifiedPlanet = new JLabel();
        final JLabel lblOriginSearchRadius = new JLabel();
        final JLabel lblOriginDistanceScale = new JLabel();

        // Create Panel Components
        setChkRandomizeOrigin(new JCheckBox(resources.getString("chkRandomizeOrigin.text")));
        getChkRandomizeOrigin().setToolTipText(resources.getString("chkRandomizeOrigin.toolTipText"));
        getChkRandomizeOrigin().setName("chkRandomizeOrigin");
        getChkRandomizeOrigin().addActionListener(evt -> {
            final boolean selected = getChkRandomizeOrigin().isSelected();
            getChkRandomizeAroundSpecifiedPlanet().setEnabled(selected);
            getChkSpecifiedSystemFactionSpecific()
                    .setEnabled(selected && getChkRandomizeAroundSpecifiedPlanet().isSelected());
            lblSpecifiedPlanet.setEnabled(selected && getChkRandomizeAroundSpecifiedPlanet().isSelected());
            getComboSpecifiedSystem().setEnabled(selected && getChkRandomizeAroundSpecifiedPlanet().isSelected());
            getComboSpecifiedPlanet().setEnabled(selected && getChkRandomizeAroundSpecifiedPlanet().isSelected());
            lblOriginSearchRadius.setEnabled(selected);
            getSpnOriginSearchRadius().setEnabled(selected);
            lblOriginDistanceScale.setEnabled(selected);
            getSpnOriginDistanceScale().setEnabled(selected);
            getChkAllowClanOrigins().setEnabled(selected);
            getChkExtraRandomOrigin().setEnabled(selected);
        });

        setChkRandomizeDependentsOrigin(new JCheckBox(resources.getString("chkRandomizeDependentsOrigin.text")));
        getChkRandomizeDependentsOrigin()
                .setToolTipText(resources.getString("chkRandomizeDependentsOrigin.toolTipText"));
        getChkRandomizeDependentsOrigin().setName("chkRandomizeDependentsOrigin");

        setChkRandomizeAroundSpecifiedPlanet(
                new JCheckBox(resources.getString("chkRandomizeAroundSpecifiedPlanet.text")));
        getChkRandomizeAroundSpecifiedPlanet()
                .setToolTipText(resources.getString("chkRandomizeAroundSpecifiedPlanet.toolTipText"));
        getChkRandomizeAroundSpecifiedPlanet().setName("chkRandomizeAroundSpecifiedPlanet");
        getChkRandomizeAroundSpecifiedPlanet().addActionListener(evt -> {
            final boolean selected = getChkRandomizeAroundSpecifiedPlanet().isSelected()
                    && getChkRandomizeAroundSpecifiedPlanet().isEnabled();
            getChkSpecifiedSystemFactionSpecific().setEnabled(selected);
            lblSpecifiedPlanet.setEnabled(selected);
            getComboSpecifiedSystem().setEnabled(selected);
            getComboSpecifiedPlanet().setEnabled(selected);
        });

        setChkSpecifiedSystemFactionSpecific(new JCheckBox(resources.getString("FactionSpecific.text")));
        getChkSpecifiedSystemFactionSpecific()
                .setToolTipText(resources.getString("chkSpecifiedSystemFactionSpecific.toolTipText"));
        getChkSpecifiedSystemFactionSpecific().setName("chkSpecifiedSystemFactionSpecific");
        getChkSpecifiedSystemFactionSpecific().addActionListener(evt -> {
            final PlanetarySystem planetarySystem = getComboSpecifiedSystem().getSelectedItem();
            if ((planetarySystem == null)
                    || !planetarySystem.getFactionSet(getCampaign().getLocalDate()).contains(getFaction())) {
                restoreComboSpecifiedSystem();
            }
        });

        lblSpecifiedPlanet.setText(resources.getString("lblSpecifiedPlanet.text"));
        lblSpecifiedPlanet.setToolTipText(resources.getString("lblSpecifiedPlanet.toolTipText"));
        lblSpecifiedPlanet.setName("lblSpecifiedPlanet");

        setComboSpecifiedSystem(new MMComboBox<>("comboSpecifiedSystem"));
        getComboSpecifiedSystem().setToolTipText(resources.getString("comboSpecifiedSystem.toolTipText"));
        getComboSpecifiedSystem().setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                    final int index, final boolean isSelected,
                    final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof PlanetarySystem) {
                    setText(((PlanetarySystem) value).getName(getCampaign().getLocalDate()));
                }
                return this;
            }
        });
        getComboSpecifiedSystem().addActionListener(evt -> {
            final PlanetarySystem planetarySystem = getComboSpecifiedSystem().getSelectedItem();
            final Planet planet = getComboSpecifiedPlanet().getSelectedItem();
            if ((planetarySystem == null)
                    || ((planet != null) && !planet.getParentSystem().equals(planetarySystem))) {
                restoreComboSpecifiedPlanet();
            }
        });

        setComboSpecifiedPlanet(new MMComboBox<>("comboSpecifiedPlanet"));
        getComboSpecifiedPlanet().setToolTipText(resources.getString("lblSpecifiedPlanet.toolTipText"));
        getComboSpecifiedPlanet().setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                    final int index, final boolean isSelected,
                    final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Planet) {
                    setText(((Planet) value).getName(getCampaign().getLocalDate()));
                }
                return this;
            }
        });

        lblOriginSearchRadius.setText(resources.getString("lblOriginSearchRadius.text"));
        lblOriginSearchRadius.setToolTipText(resources.getString("lblOriginSearchRadius.toolTipText"));
        lblOriginSearchRadius.setName("lblOriginSearchRadius");

        setSpnOriginSearchRadius(new JSpinner(new SpinnerNumberModel(0, 0, 2000, 25)));
        getSpnOriginSearchRadius().setToolTipText(resources.getString("lblOriginSearchRadius.toolTipText"));
        getSpnOriginSearchRadius().setName("spnOriginSearchRadius");

        lblOriginDistanceScale.setText(resources.getString("lblOriginDistanceScale.text"));
        lblOriginDistanceScale.setToolTipText(resources.getString("lblOriginDistanceScale.toolTipText"));
        lblOriginDistanceScale.setName("lblOriginDistanceScale");

        setSpnOriginDistanceScale(new JSpinner(new SpinnerNumberModel(0.6, 0.1, 2.0, 0.1)));
        getSpnOriginDistanceScale().setToolTipText(resources.getString("lblOriginDistanceScale.toolTipText"));
        getSpnOriginDistanceScale().setName("spnOriginDistanceScale");

        setChkAllowClanOrigins(new JCheckBox(resources.getString("chkAllowClanOrigins.text")));
        getChkAllowClanOrigins().setToolTipText(resources.getString("chkAllowClanOrigins.toolTipText"));
        getChkAllowClanOrigins().setName("chkAllowClanOrigins");

        setChkExtraRandomOrigin(new JCheckBox(resources.getString("chkExtraRandomOrigin.text")));
        getChkExtraRandomOrigin().setToolTipText(resources.getString("chkExtraRandomOrigin.toolTipText"));
        getChkExtraRandomOrigin().setName("chkExtraRandomOrigin");

        // Programmatically Assign Accessibility Labels
        lblSpecifiedPlanet.setLabelFor(getComboSpecifiedPlanet());
        lblOriginSearchRadius.setLabelFor(getSpnOriginSearchRadius());
        lblOriginDistanceScale.setLabelFor(getSpnOriginDistanceScale());

        // Disable Panel by Default
        getChkRandomizeOrigin().setSelected(true);
        getChkRandomizeOrigin().doClick();

        // Layout the UI
        setBorder(BorderFactory.createTitledBorder(resources.getString("RandomOriginOptionsPanel.title")));
        setName("personnelRandomizationPanel");
        final GroupLayout layout = new GroupLayout(this);
        setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(getChkRandomizeOrigin())
                        .addComponent(getChkRandomizeDependentsOrigin())
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(getChkRandomizeAroundSpecifiedPlanet())
                                .addComponent(getChkSpecifiedSystemFactionSpecific(), Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblSpecifiedPlanet)
                                .addComponent(getComboSpecifiedSystem())
                                .addComponent(getComboSpecifiedPlanet(), Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblOriginSearchRadius)
                                .addComponent(getSpnOriginSearchRadius(), Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblOriginDistanceScale)
                                .addComponent(getSpnOriginDistanceScale(), Alignment.LEADING))
                        .addComponent(getChkAllowClanOrigins())
                        .addComponent(getChkExtraRandomOrigin()));

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(getChkRandomizeOrigin())
                        .addComponent(getChkRandomizeDependentsOrigin())
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(getChkRandomizeAroundSpecifiedPlanet())
                                .addComponent(getChkSpecifiedSystemFactionSpecific()))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblSpecifiedPlanet)
                                .addComponent(getComboSpecifiedSystem())
                                .addComponent(getComboSpecifiedPlanet()))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblOriginSearchRadius)
                                .addComponent(getSpnOriginSearchRadius()))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblOriginDistanceScale)
                                .addComponent(getSpnOriginDistanceScale()))
                        .addComponent(getChkAllowClanOrigins())
                        .addComponent(getChkExtraRandomOrigin()));
    }

    private PlanetarySystem[] getPlanetarySystems(final @Nullable Faction faction) {
        return getCampaign().getSystems().stream()
                .filter(p -> (faction == null) || p.getFactionSet(getCampaign().getLocalDate()).contains(faction))
                .sorted(Comparator.comparing(p -> p.getName(getCampaign().getLocalDate())))
                .collect(Collectors.toList()).toArray(new PlanetarySystem[] {});
    }
    // endregion Initialization

    // region Options
    public void setOptions(final RandomOriginOptions options) {
        if (getChkRandomizeOrigin().isSelected() != options.isRandomizeOrigin()) {
            getChkRandomizeOrigin().doClick();
        }
        getChkRandomizeDependentsOrigin().setSelected(options.isRandomizeDependentOrigin());
        if (getChkRandomizeAroundSpecifiedPlanet().isSelected() != options.isRandomizeAroundSpecifiedPlanet()) {
            getChkRandomizeAroundSpecifiedPlanet().doClick();
        }
        getChkSpecifiedSystemFactionSpecific().setSelected(false);
        restoreComboSpecifiedSystem();
        getComboSpecifiedSystem().setSelectedItem(options.getSpecifiedPlanet().getParentSystem());
        getComboSpecifiedPlanet().setSelectedItem(options.getSpecifiedPlanet());
        getSpnOriginSearchRadius().setValue(options.getOriginSearchRadius());
        getSpnOriginDistanceScale().setValue(options.getOriginDistanceScale());
        getChkAllowClanOrigins().setSelected(options.isAllowClanOrigins());
        getChkExtraRandomOrigin().setSelected(options.isExtraRandomOrigin());
    }

    public RandomOriginOptions createOptionsFromPanel() {
        final RandomOriginOptions options = new RandomOriginOptions(true);
        try {
            options.setRandomizeOrigin(getChkRandomizeOrigin().isSelected());
            options.setRandomizeDependentOrigin(getChkRandomizeDependentsOrigin().isSelected());
            options.setRandomizeAroundSpecifiedPlanet(getChkRandomizeAroundSpecifiedPlanet().isSelected());
            options.setSpecifiedPlanet(getComboSpecifiedPlanet().getSelectedItem());
            options.setOriginSearchRadius((Integer) getSpnOriginSearchRadius().getValue());
            options.setOriginDistanceScale((Double) getSpnOriginDistanceScale().getValue());
            options.setAllowClanOrigins(getChkAllowClanOrigins().isSelected());
            options.setExtraRandomOrigin(getChkExtraRandomOrigin().isSelected());
        } catch (Exception ex) {
            logger.error("", ex);
        }
        return options;
    }

    /**
     * Validates the data contained in this panel, returning the current state of
     * validation.
     * 
     * @param display to display dialogs containing the messages or not
     * @return ValidationState.SUCCESS if the data validates successfully,
     *         ValidationState.WARNING
     *         if a warning was issued, or ValidationState.FAILURE if validation
     *         fails
     */
    public ValidationState validateOptions(final boolean display) {
        // region Errors
        // Specified System/Planet Validation
        if ((getComboSpecifiedSystem().getSelectedItem() == null)
                || (getComboSpecifiedPlanet().getSelectedItem() == null)) {
            if (display) {
                JOptionPane.showMessageDialog(getFrame(),
                        resources.getString("RandomOriginOptionsPanel.InvalidSpecifiedPlanet.text"),
                        resources.getString("InvalidOptions.title"),
                        JOptionPane.ERROR_MESSAGE);
            }
            return ValidationState.FAILURE;
        }
        // endregion Errors

        // The options specified are correct, and thus can be saved
        return ValidationState.SUCCESS;
    }
    // endregion Options
}
