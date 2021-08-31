/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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

import megamek.client.ui.baseComponents.MMComboBox;
import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.RandomOriginOptions;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.gui.baseComponents.AbstractMHQPanel;
import mekhq.gui.displayWrappers.FactionDisplay;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;

public class RandomOriginOptionsPanel extends AbstractMHQPanel {
    //region Variable Declarations
    private final Campaign campaign;
    private final MMComboBox<FactionDisplay> comboFaction;

    private JCheckBox chkRandomizeOrigin;
    private JCheckBox chkRandomizeDependentsOrigin;
    private JCheckBox chkRandomizeAroundCentralPlanet;
    private JCheckBox chkCentralSystemFactionSpecific;
    private JComboBox<PlanetarySystem> comboCentralSystem;
    private JComboBox<Planet> comboCentralPlanet;
    private JSpinner spnOriginSearchRadius;
    private JSpinner spnOriginDistanceScale;
    private JCheckBox chkAllowClanOrigins;
    private JCheckBox chkExtraRandomOrigin;
    //endregion Variable Declarations

    //region Constructors
    public RandomOriginOptionsPanel(final JFrame frame, final Campaign campaign,
                                    final MMComboBox<FactionDisplay> comboFaction) {
        super(frame, "RandomOriginOptionsPanel");
        this.campaign = campaign;
        this.comboFaction = comboFaction;
        initialize();
    }
    //endregion Constructors

    //region Getters/Setters
    public Campaign getCampaign() {
        return campaign;
    }

    public MMComboBox<FactionDisplay> getComboFaction() {
        return comboFaction;
    }

    public Faction getFaction() {
        return Objects.requireNonNull(getComboFaction().getSelectedItem()).getFaction();
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

    public JCheckBox getChkRandomizeAroundCentralPlanet() {
        return chkRandomizeAroundCentralPlanet;
    }

    public void setChkRandomizeAroundCentralPlanet(final JCheckBox chkRandomizeAroundCentralPlanet) {
        this.chkRandomizeAroundCentralPlanet = chkRandomizeAroundCentralPlanet;
    }

    public JCheckBox getChkCentralSystemFactionSpecific() {
        return chkCentralSystemFactionSpecific;
    }

    public void setChkCentralSystemFactionSpecific(final JCheckBox chkCentralSystemFactionSpecific) {
        this.chkCentralSystemFactionSpecific = chkCentralSystemFactionSpecific;
    }

    public JComboBox<PlanetarySystem> getComboCentralSystem() {
        return comboCentralSystem;
    }

    public @Nullable PlanetarySystem getCentralSystem() {
        return (PlanetarySystem) getComboCentralSystem().getSelectedItem();
    }

    public void setComboCentralSystem(final JComboBox<PlanetarySystem> comboCentralSystem) {
        this.comboCentralSystem = comboCentralSystem;
    }

    private void restoreComboCentralSystem() {
        getComboCentralSystem().removeAllItems();
        getComboCentralSystem().setModel(new DefaultComboBoxModel<>(getPlanetarySystems(
                getChkCentralSystemFactionSpecific().isSelected() ? getFaction() : null)));
        restoreComboCentralPlanet();
    }

    public JComboBox<Planet> getComboCentralPlanet() {
        return comboCentralPlanet;
    }

    public @Nullable Planet getCentralPlanet() {
        return (Planet) getComboCentralPlanet().getSelectedItem();
    }

    public void setComboCentralPlanet(final JComboBox<Planet> comboCentralPlanet) {
        this.comboCentralPlanet = comboCentralPlanet;
    }

    private void restoreComboCentralPlanet() {
        if (getCentralSystem() != null) {
            getComboCentralPlanet().setModel(new DefaultComboBoxModel<>(
                    getCentralSystem().getPlanets().toArray(new Planet[]{})));
            getComboCentralPlanet().setSelectedItem(getCentralSystem().getPrimaryPlanet());
        } else {
            getComboCentralPlanet().removeAllItems();
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
    //endregion Getters/Setters

    //region Initialization
    @Override
    protected void initialize() {
        // Initialize Labels Used in ActionListeners
        final JLabel lblCentralPlanet = new JLabel();
        final JLabel lblOriginSearchRadius = new JLabel();
        final JLabel lblOriginDistanceScale = new JLabel();

        // Create Panel Components
        setChkRandomizeOrigin(new JCheckBox(resources.getString("chkRandomizeOrigin.text")));
        getChkRandomizeOrigin().setToolTipText(resources.getString("chkRandomizeOrigin.toolTipText"));
        getChkRandomizeOrigin().setName("chkRandomizeOrigin");
        getChkRandomizeOrigin().addActionListener(evt -> {
            final boolean selected = getChkRandomizeOrigin().isSelected();
            getChkRandomizeAroundCentralPlanet().setEnabled(selected);
            getChkCentralSystemFactionSpecific().setEnabled(selected && getChkRandomizeAroundCentralPlanet().isSelected());
            lblCentralPlanet.setEnabled(selected && getChkRandomizeAroundCentralPlanet().isSelected());
            getComboCentralSystem().setEnabled(selected && getChkRandomizeAroundCentralPlanet().isSelected());
            getComboCentralPlanet().setEnabled(selected && getChkRandomizeAroundCentralPlanet().isSelected());
            lblOriginSearchRadius.setEnabled(selected);
            getSpnOriginSearchRadius().setEnabled(selected);
            lblOriginDistanceScale.setEnabled(selected);
            getSpnOriginDistanceScale().setEnabled(selected);
            getChkAllowClanOrigins().setEnabled(selected);
            getChkExtraRandomOrigin().setEnabled(selected);
        });

        setChkRandomizeDependentsOrigin(new JCheckBox(resources.getString("chkRandomizeDependentsOrigin.text")));
        getChkRandomizeDependentsOrigin().setToolTipText(resources.getString("chkRandomizeDependentsOrigin.toolTipText"));
        getChkRandomizeDependentsOrigin().setName("chkRandomizeDependentsOrigin");

        setChkRandomizeAroundCentralPlanet(new JCheckBox(resources.getString("chkRandomizeAroundCentralPlanet.text")));
        getChkRandomizeAroundCentralPlanet().setToolTipText(resources.getString("chkRandomizeAroundCentralPlanet.toolTipText"));
        getChkRandomizeAroundCentralPlanet().setName("chkRandomizeAroundCentralPlanet");
        getChkRandomizeAroundCentralPlanet().addActionListener(evt -> {
            final boolean selected = getChkRandomizeAroundCentralPlanet().isSelected()
                    && getChkRandomizeAroundCentralPlanet().isEnabled();
            getChkCentralSystemFactionSpecific().setEnabled(selected);
            lblCentralPlanet.setEnabled(selected);
            getComboCentralSystem().setEnabled(selected);
            getComboCentralPlanet().setEnabled(selected);
        });

        setChkCentralSystemFactionSpecific(new JCheckBox(resources.getString("FactionSpecific.text")));
        getChkCentralSystemFactionSpecific().setToolTipText(resources.getString("chkCentralSystemFactionSpecific.toolTipText"));
        getChkCentralSystemFactionSpecific().setName("chkCentralSystemFactionSpecific");
        getChkCentralSystemFactionSpecific().addActionListener(evt -> {
            if ((getCentralSystem() == null) || ((getCentralSystem() != null)
                    && !getCentralSystem().getFactionSet(getCampaign().getLocalDate()).contains(getFaction()))) {
                restoreComboCentralSystem();
            }
        });

        lblCentralPlanet.setText(resources.getString("lblCentralPlanet.text"));
        lblCentralPlanet.setToolTipText(resources.getString("lblCentralPlanet.toolTipText"));
        lblCentralPlanet.setName("lblCentralPlanet");

        setComboCentralSystem(new JComboBox<>());
        getComboCentralSystem().setToolTipText(resources.getString("comboCentralSystem.toolTipText"));
        getComboCentralSystem().setName("comboCentralSystem");
        getComboCentralSystem().setRenderer(new DefaultListCellRenderer() {
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
        getComboCentralSystem().addActionListener(evt -> {
            if ((getCentralSystem() == null) || ((getCentralSystem() != null) && (getCentralPlanet() != null)
                    && !getCentralPlanet().getParentSystem().equals(getCentralSystem()))) {
                restoreComboCentralPlanet();
            }
        });

        setComboCentralPlanet(new JComboBox<>());
        getComboCentralPlanet().setToolTipText(resources.getString("lblCentralPlanet.toolTipText"));
        getComboCentralPlanet().setName("comboCentralPlanet");
        getComboCentralPlanet().setRenderer(new DefaultListCellRenderer() {
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
        lblCentralPlanet.setLabelFor(getComboCentralPlanet());
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
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(getChkRandomizeAroundCentralPlanet())
                                .addComponent(getChkCentralSystemFactionSpecific(), GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblCentralPlanet)
                                .addComponent(getComboCentralSystem())
                                .addComponent(getComboCentralPlanet(), GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblOriginSearchRadius)
                                .addComponent(getSpnOriginSearchRadius(), GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblOriginDistanceScale)
                                .addComponent(getSpnOriginDistanceScale(), GroupLayout.Alignment.LEADING))
                        .addComponent(getChkAllowClanOrigins())
                        .addComponent(getChkExtraRandomOrigin())
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(getChkRandomizeOrigin())
                        .addComponent(getChkRandomizeDependentsOrigin())
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(getChkRandomizeAroundCentralPlanet())
                                .addComponent(getChkCentralSystemFactionSpecific()))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblCentralPlanet)
                                .addComponent(getComboCentralSystem())
                                .addComponent(getComboCentralPlanet()))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblOriginSearchRadius)
                                .addComponent(getSpnOriginSearchRadius()))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblOriginDistanceScale)
                                .addComponent(getSpnOriginDistanceScale()))
                        .addComponent(getChkAllowClanOrigins())
                        .addComponent(getChkExtraRandomOrigin())
        );
    }

    private PlanetarySystem[] getPlanetarySystems(final @Nullable Faction faction) {
        return getCampaign().getSystems().stream()
                .filter(p -> (faction == null) || p.getFactionSet(getCampaign().getLocalDate()).contains(faction))
                .sorted(Comparator.comparing(p -> p.getName(getCampaign().getLocalDate())))
                .collect(Collectors.toList()).toArray(new PlanetarySystem[]{});
    }
    //endregion Initialization

    public void setOptions(final RandomOriginOptions options) {
        if (getChkRandomizeOrigin().isSelected() != options.isRandomizeOrigin()) {
            getChkRandomizeOrigin().doClick();
        }
        getChkRandomizeDependentsOrigin().setSelected(options.isRandomizeDependentOrigin());
        if (getChkRandomizeAroundCentralPlanet().isSelected() != options.isRandomizeAroundCentralPlanet()) {
            getChkRandomizeAroundCentralPlanet().doClick();
        }
        getChkCentralSystemFactionSpecific().setSelected(false);
        restoreComboCentralSystem();
        getComboCentralSystem().setSelectedItem(options.getCentralPlanet().getParentSystem());
        getComboCentralPlanet().setSelectedItem(options.getCentralPlanet());
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
            options.setRandomizeAroundCentralPlanet(getChkRandomizeAroundCentralPlanet().isSelected());
            options.setCentralPlanet(getCentralPlanet());
            options.setOriginSearchRadius((Integer) getSpnOriginSearchRadius().getValue());
            options.setOriginDistanceScale((Double) getSpnOriginDistanceScale().getValue());
            options.setAllowClanOrigins(getChkAllowClanOrigins().isSelected());
            options.setExtraRandomOrigin(getChkExtraRandomOrigin().isSelected());
        } catch (Exception e) {
            MekHQ.getLogger().error(e);
        }
        return options;
    }
}
