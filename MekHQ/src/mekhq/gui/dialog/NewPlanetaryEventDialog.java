/*
 * Copyright (C) 2016 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;

import javax.swing.*;
import javax.swing.event.ChangeListener;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.codeUtilities.ObjectUtility;
import megamek.common.EquipmentType;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.adapter.SocioIndustrialDataAdapter;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.Planet;

public class NewPlanetaryEventDialog extends JDialog {
    private static final MMLogger logger = MMLogger.create(NewPlanetaryEventDialog.class);

    private static final String FIELD_MESSAGE = "message";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_SHORTNAME = "shortName";
    private static final String FIELD_FACTION = "faction";
    private static final String FIELD_LIFE_FORM = "lifeForm";
    private static final String FIELD_CLIMATE = "climate";
    private static final String FIELD_WATER = "water";
    private static final String FIELD_TEMPERATURE = "temperature";
    private static final String FIELD_SOCIO_INDUSTRIAL = "socioindustrial";
    private static final String FIELD_HPG = "hpg";
    private static final String FIELD_PRESSURE = "pressure";
    private static final String FIELD_PRESSURE_ATM = "pressureAtm";
    private static final String FIELD_ATM_MASS = "atmMass";
    private static final String FIELD_ATMOSPHERE = "atmosphere";
    private static final String FIELD_POPULATION = "population";
    private static final String FIELD_GOVERNMENT = "government";

    private final static SocioIndustrialDataAdapter SOCIO_INDUSTRIAL_ADAPTER = new SocioIndustrialDataAdapter();

    private final Planet planet;

    private LocalDate date;

    private List<Planet.PlanetaryEvent> changedEvents = null;

    private JButton dateButton;
    private JTextField messageField;
    private JTextField nameField;
    private JTextField shortNameField;
    private JButton factionsButton;
    private JTextField socioindustrialField;
    private JComboBox<HPGChoice> hpgField;

    private JCheckBox nameKeep;
    private JCheckBox shortNameKeep;
    private JCheckBox factionKeep;
    private JCheckBox socioindustrialKeep;
    private JCheckBox hpgKeep;

    private JLabel nameCombined;
    private JLabel shortNameCombined;
    private JLabel factionCombined;
    private JLabel socioindustrialCombined;
    private JLabel hpgCombined;

    private final transient ResourceBundle resourceMap = ResourceBundle.getBundle(
            "mekhq.resources.NewPlanetaryEventDialog",
            MekHQ.getMHQOptions().getLocale());

    public NewPlanetaryEventDialog(final JFrame frame, final Campaign campaign,
            final Planet planet) {
        this(frame, true, campaign, planet);
    }

    public NewPlanetaryEventDialog(final JFrame frame, final boolean modal,
            final Campaign campaign, final Planet planet) {
        super(frame, modal);
        this.planet = new Planet(Objects.requireNonNull(planet).getId());
        this.planet.copyDataFrom(planet);
        this.date = campaign.getLocalDate();
        initComponents(campaign);
        setLocationRelativeTo(frame);
        setUserPreferences();
    }

    public List<Planet.PlanetaryEvent> getChangedEvents() {
        return changedEvents;
    }

    protected void initComponents(Campaign campaign) {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setName("form");
        setTitle(resourceMap.getString("Form.title"));
        setPreferredSize(new Dimension(600, 600));

        final Container content = getContentPane();
        content.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        content.add(new JLabel(String.format(resourceMap.getString("planetId.format"), planet.getId())), gbc);

        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        content.add(new JButton(new AbstractAction(resourceMap.getString("previousDay.label")) {
            {
                putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, ActionEvent.CTRL_MASK));
                putValue(SHORT_DESCRIPTION, resourceMap.getString("previousDay.tooltip"));
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if ((e.getModifiers() & ActionEvent.ALT_MASK) > 0) {
                    date = date.minusYears(1);
                } else if ((e.getModifiers() & ActionEvent.CTRL_MASK) > 0) {
                    date = date.minusMonths(1);
                } else {
                    date = date.minusDays(1);
                }
                updateDate();
            }
        }), gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        dateButton = new JButton(new AbstractAction() {
            {
                putValue(SHORT_DESCRIPTION, resourceMap.getString("setDay.tooltip"));
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                DateChooser dc = new DateChooser((content instanceof JFrame) ? (JFrame) content : null, date);
                if (dc.showDateChooser() == DateChooser.OK_OPTION) {
                    date = dc.getDate();
                    updateDate();
                }
            }
        });
        content.add(dateButton, gbc);

        gbc.gridx = 2;
        gbc.anchor = GridBagConstraints.WEST;
        content.add(new JButton(new AbstractAction(resourceMap.getString("nextDay.label")) {
            {
                putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, ActionEvent.CTRL_MASK));
                putValue(ACTION_COMMAND_KEY, "nextDay");
                putValue(SHORT_DESCRIPTION, resourceMap.getString("nextDay.tooltip"));
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if ((e.getModifiers() & ActionEvent.ALT_MASK) > 0) {
                    date = date.plusYears(1);
                } else if ((e.getModifiers() & ActionEvent.CTRL_MASK) > 0) {
                    date = date.plusMonths(1);
                } else {
                    date = date.plusDays(1);
                }
                updateDate();
            }
        }), gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        JPanel data = new JPanel(new GridBagLayout());
        data.setName("data");
        data.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resourceMap.getString("eventData.text")),
                BorderFactory.createEmptyBorder(1, 5, 1, 5)));
        content.add(data, gbc);

        prepareDataPane(data, campaign);

        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0.0;
        content.add(new JButton(new AbstractAction(resourceMap.getString("save.text")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                changedEvents = new ArrayList<>(planet.getCustomEvents());
                setVisible(false);
            }
        }), gbc);

        gbc.gridx = 2;
        gbc.anchor = GridBagConstraints.EAST;
        content.add(new JButton(new AbstractAction(resourceMap.getString("cancel.text")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        }), gbc);

        updateDate();
        pack();
    }

    private void prepareDataPane(JPanel pane, Campaign campaign) {
        GridBagConstraints gbc = new GridBagConstraints();

        Action changeValueAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateEvent((Component) e.getSource(), planet.getOrCreateEvent(date));
                updateDate();
            }
        };

        ChangeListener changeListener = e -> {
            updateEvent((Component) e.getSource(), planet.getOrCreateEvent(date));
            updateDate();
        };

        Action noChangeAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JCheckBox check = ((JCheckBox) e.getSource());
                String field = check.getName();
                if (check.isSelected()) {
                    cleanEventField(getCurrentEvent(), field);
                    updateDate();
                }
            }
        };

        FocusAdapter textFocusAdapter = new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                super.focusGained(e);
                ((JTextField) e.getSource()).selectAll();
            }

            @Override
            public void focusLost(FocusEvent e) {
                super.focusLost(e);
                if (e.getSource() instanceof JTextField) {
                    final JTextField source = (JTextField) e.getSource();
                    source.dispatchEvent(new KeyEvent(source, KeyEvent.KEY_PRESSED,
                            System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, '\n'));
                }
            }
        };

        gbc.gridx = 0;
        gbc.gridy = 0;
        pane.add(new JLabel(resourceMap.getString("changeOf.text")), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        pane.add(new JLabel(resourceMap.getString("newValue.text")), gbc);
        gbc.gridx = 3;
        gbc.weightx = 0.0;
        pane.add(new JLabel(resourceMap.getString("combinedValue.text")), gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        pane.add(new JLabel(resourceMap.getString("message.text")), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        messageField = new JTextField();
        messageField.addActionListener(changeValueAction);
        messageField.addFocusListener(textFocusAdapter);
        messageField.setName(FIELD_MESSAGE);
        pane.add(messageField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        pane.add(new JLabel(resourceMap.getString("name.text")), gbc);

        gbc.gridx = 1;
        nameField = new JTextField();
        nameField.addActionListener(changeValueAction);
        nameField.addFocusListener(textFocusAdapter);
        nameField.setName(FIELD_NAME);
        pane.add(nameField, gbc);

        gbc.gridx = 2;
        gbc.ipadx = 10;
        nameKeep = new JCheckBox(noChangeAction);
        nameKeep.setText(resourceMap.getString("noChange.text"));
        nameKeep.setName(FIELD_NAME);
        pane.add(nameKeep, gbc);
        gbc.ipadx = 0;

        gbc.gridx = 3;
        nameCombined = new JLabel();
        pane.add(nameCombined, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        pane.add(new JLabel(resourceMap.getString("shortName.text")), gbc);

        gbc.gridx = 1;
        shortNameField = new JTextField();
        shortNameField.addActionListener(changeValueAction);
        shortNameField.addFocusListener(textFocusAdapter);
        shortNameField.setName(FIELD_SHORTNAME);
        pane.add(shortNameField, gbc);

        gbc.gridx = 2;
        shortNameKeep = new JCheckBox(noChangeAction);
        shortNameKeep.setText(resourceMap.getString("noChange.text"));
        shortNameKeep.setName(FIELD_SHORTNAME);
        pane.add(shortNameKeep, gbc);

        gbc.gridx = 3;
        shortNameCombined = new JLabel();
        pane.add(shortNameCombined, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        pane.add(new JLabel(resourceMap.getString("factionList.text")), gbc);

        gbc.gridx = 1;
        factionsButton = new JButton(new AbstractAction("") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Planet.PlanetaryEvent event = planet.getOrCreateEvent(date);
                ChooseFactionsDialog chooser = new ChooseFactionsDialog(null, date, event.faction);
                chooser.setVisible(true);
                if (chooser.isChanged()) {
                    event.faction = chooser.getResult();
                    event.custom = true;
                    planet.refreshEvents();
                    updateDate();
                }
            }
        });
        pane.add(factionsButton, gbc);

        gbc.gridx = 2;
        factionKeep = new JCheckBox(noChangeAction);
        factionKeep.setText(resourceMap.getString("noChange.text"));
        factionKeep.setName(FIELD_FACTION);
        pane.add(factionKeep, gbc);

        gbc.gridx = 3;
        factionCombined = new JLabel();
        pane.add(factionCombined, gbc);

        gbc.gridx = 1;

        gbc.gridx = 0;
        gbc.gridy = 9;
        pane.add(new JLabel(resourceMap.getString("socioindustrial.text")), gbc);

        gbc.gridx = 1;
        socioindustrialField = new JTextField();
        socioindustrialField.addActionListener(changeValueAction);
        socioindustrialField.addFocusListener(textFocusAdapter);
        socioindustrialField.setName(FIELD_SOCIO_INDUSTRIAL);
        socioindustrialField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                String text = ((JTextField) input).getText();
                try {
                    SOCIO_INDUSTRIAL_ADAPTER.unmarshal(text);
                } catch (Exception ex) {
                    return false;
                }
                return true;
            }
        });
        pane.add(socioindustrialField, gbc);

        gbc.gridx = 2;
        socioindustrialKeep = new JCheckBox(noChangeAction);
        socioindustrialKeep.setText(resourceMap.getString("noChange.text"));
        socioindustrialKeep.setName(FIELD_SOCIO_INDUSTRIAL);
        pane.add(socioindustrialKeep, gbc);

        gbc.gridx = 3;
        socioindustrialCombined = new JLabel();
        pane.add(socioindustrialCombined, gbc);

        gbc.gridx = 0;
        gbc.gridy = 10;
        pane.add(new JLabel(resourceMap.getString("hpg.text")), gbc);

        gbc.gridx = 1;
        hpgField = new JComboBox<>(new HPGChoice[] {
                new HPGChoice(null, resourceMap.getString("hpg.undefined.text")),
                new HPGChoice(EquipmentType.RATING_A, resourceMap.getString("hpg.a.text")),
                new HPGChoice(EquipmentType.RATING_B, resourceMap.getString("hpg.b.text")),
                new HPGChoice(EquipmentType.RATING_C, resourceMap.getString("hpg.c.text")),
                new HPGChoice(EquipmentType.RATING_D, resourceMap.getString("hpg.d.text")),
                new HPGChoice(EquipmentType.RATING_X, resourceMap.getString("hpg.none.text"))
        });
        hpgField.addActionListener(changeValueAction);
        hpgField.setName(FIELD_HPG);
        pane.add(hpgField, gbc);

        gbc.gridx = 2;
        hpgKeep = new JCheckBox(noChangeAction);
        hpgKeep.setText(resourceMap.getString("noChange.text"));
        hpgKeep.setName(FIELD_HPG);
        pane.add(hpgKeep, gbc);

        gbc.gridx = 3;
        hpgCombined = new JLabel();
        pane.add(hpgCombined, gbc);
    }

    @Deprecated // These need to be migrated to the Suite Constants / Suite Options Setup
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(NewPlanetaryEventDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            logger.error("Failed to set user preferences", ex);
        }
    }

    private Planet.PlanetaryEvent getCurrentEvent() {
        return planet.getEvent(date);
    }

    private void updateDate() {
        dateButton.setText(MekHQ.getMHQOptions().getDisplayFormattedDate(date));
        Planet.PlanetaryEvent event = getCurrentEvent();

        messageField.setText((null != event) ? event.message : null);
        nameField.setText((null != event) ? event.name : null);
        shortNameField.setText((null != event) ? event.shortName : null);
        Set<Faction> factionSet = null;
        if ((null != event) && (null != event.faction)) {
            factionSet = new HashSet<>();
            for (String f : event.faction) {
                factionSet.add(Factions.getInstance().getFaction(f));
            }
        }
        factionsButton.setText(Faction.getFactionNames(factionSet, date.getYear()));
        try {
            socioindustrialField.setText(((null != event) && (null != event.socioIndustrial))
                    ? SOCIO_INDUSTRIAL_ADAPTER.marshal(event.socioIndustrial)
                    : null);
        } catch (Exception ex) {
            socioindustrialField.setText(null);
        }
        hpgField.setSelectedItem(new HPGChoice((null != event) ? event.hpg : null, null));

        nameKeep.setSelected((null == event) || (null == event.name));
        shortNameKeep.setSelected((null == event) || (null == event.shortName));
        factionKeep.setSelected((null == event) || (null == event.faction));
        socioindustrialKeep.setSelected((null == event) || (null == event.socioIndustrial));
        hpgKeep.setSelected((null == event) || (null == event.hpg));

        nameCombined.setText(ObjectUtility.nonNull(planet.getName(date), resourceMap.getString("undefined.text")));
        shortNameCombined
                .setText(ObjectUtility.nonNull(planet.getShortName(date), resourceMap.getString("undefined.text")));
        factionCombined.setText(planet.getFactionDesc(date));
        String socioIndustrialText = "";
        try {
            socioIndustrialText = SOCIO_INDUSTRIAL_ADAPTER.marshal(planet.getSocioIndustrial(date));
        } catch (Exception ignored) {

        }
        socioindustrialCombined.setText(socioIndustrialText);
        hpgCombined.setText(planet.getHPGClass(date));
    }

    private void cleanEventField(Planet.PlanetaryEvent event, String field) {
        if ((null == event) || (null == field)) {
            return;
        }
        switch (field) {
            case FIELD_NAME:
                event.name = null;
                break;
            case FIELD_SHORTNAME:
                event.shortName = null;
                break;
            case FIELD_FACTION:
                event.faction = null;
                break;
            case FIELD_SOCIO_INDUSTRIAL:
                event.socioIndustrial = null;
                break;
            case FIELD_HPG:
                event.hpg = null;
                break;
            default:
                break;
        }
        event.custom = true;
        planet.refreshEvents();
    }

    private String nullEmptyText(JTextField field) {
        final String text = field.getText();
        return ((null == text) || text.isEmpty()) ? null : text;
    }

    private void updateEvent(Component source, Planet.PlanetaryEvent event) {
        switch (source.getName()) {
            case FIELD_MESSAGE:
                event.message = nullEmptyText(messageField);
                break;
            case FIELD_NAME:
                event.name = nullEmptyText(nameField);
                break;
            case FIELD_SHORTNAME:
                event.shortName = nullEmptyText(shortNameField);
                break;
            case FIELD_SOCIO_INDUSTRIAL:
                String socioindustrialText = nullEmptyText(socioindustrialField);
                try {
                    event.socioIndustrial = SOCIO_INDUSTRIAL_ADAPTER.unmarshal(socioindustrialText);
                    String newText = SOCIO_INDUSTRIAL_ADAPTER.marshal(event.socioIndustrial);
                    if (!socioindustrialText.equals(newText)) {
                        socioindustrialField.setText(newText);
                    }
                } catch (Exception ignored) {

                }
                break;
            case FIELD_HPG:
                event.hpg = ((HPGChoice) hpgField.getSelectedItem()).hpg;
                break;
            default:
                return;
        }
        event.custom = true;
        planet.refreshEvents();
    }

    private static class HPGChoice {
        public Integer hpg;
        public String text;

        public HPGChoice(Integer hpg, String text) {
            this.hpg = hpg;
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(hpg);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if ((null == obj) || (getClass() != obj.getClass())) {
                return false;
            }
            final HPGChoice other = (HPGChoice) obj;
            return Objects.equals(hpg, other.hpg);
        }
    }
}
