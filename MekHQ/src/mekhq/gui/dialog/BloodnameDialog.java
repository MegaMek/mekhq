/*
 * Copyright (c) 2014, 2020 The MegaMek Team
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.dialog;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.Border;

import mekhq.MekHQ;
import mekhq.campaign.personnel.Bloodname;
import mekhq.campaign.personnel.Clan;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.universe.Faction;
import mekhq.gui.preferences.JComboBoxPreference;
import mekhq.gui.preferences.JWindowPreference;
import mekhq.preferences.PreferencesNode;
import sun.swing.DefaultLookup;

/**
 * @author Neoancient
 *
 * Randomly selects an appropriate Bloodname based on Clan, phenotype, and year
 */
public class BloodnameDialog extends JDialog {
    //region Variable Declaration
    private static final long serialVersionUID = 120186090844572718L;

    private List<Clan> clans = new ArrayList<>();

    private static final Integer[] eras = {
            2807, 2825, 2850, 2900, 2950, 3000, 3050, 3060,
            3075, 3085, 3100
    };

    private JComboBox<String> cbClan;
    private JComboBox<Integer> cbEra;
    private JComboBox<Phenotype> cbPhenotype;
    private JButton btnGo;
    private JLabel lblName;
    private JLabel lblOrigClan;
    private JLabel lblPhenotype;
    private JLabel txtWarning;

    // The following are the parameters that the generation will use, and are set whenever the options
    // are modified
    private int clan;
    private Phenotype phenotype;
    private int year;
    //endregion Variable Declaration

    public BloodnameDialog(JFrame parent, String factionName, int year) {
        super (parent, false);
        setTitle ("Bloodname Generator");
        getContentPane().setLayout(new GridBagLayout());
        setPreferredSize(new Dimension(350,300));

        initComponents();

        setLocationRelativeTo(parent);
        pack();
        setUserPreferences();

        setYear(year);
        setFaction(factionName);
    }

    private void initComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.NORTHWEST;

        cbClan = new JComboBox<>();
        clans.addAll(Clan.getClans());
        clans.sort(Comparator.comparing(o -> o.getFullName(year)));
        for (Clan clan : clans) {
            cbClan.addItem(clan.getFullName(year));
        }
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(cbClan, gbc);
        cbClan.addActionListener(validateActionListener);

        cbEra = new JComboBox<>(eras);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        add(cbEra, gbc);
        cbEra.addActionListener(validateActionListener);

        cbPhenotype = new JComboBox<>();
        cbPhenotype.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                setOpaque(list.isOpaque());
                if (isSelected) {
                    setBackground(list.getSelectionBackground());
                    setForeground(list.getSelectionForeground());
                } else {
                    setBackground(list.getBackground());
                    setForeground(list.getForeground());
                }
                setFont(list.getFont());

                setText((value == null) ? ""
                        : (value instanceof Phenotype ? ((Phenotype) value).getGroupingName() : "ERROR"));

                setEnabled(list.isEnabled());

                if (cellHasFocus) {
                    Border border = null;
                    if (isSelected) {
                        border = DefaultLookup.getBorder(this, ui, "List.focusSelectedCellHighlightBorder");
                    }
                    if (border == null) {
                        border = DefaultLookup.getBorder(this, ui, "List.focusCellHighlightBorder");
                    }
                    setBorder(border);
                }

                return this;
            }
        });
        cbPhenotype.addItem(Phenotype.GENERAL);
        for (Phenotype phenotype : Phenotype.getExternalPhenotypes()) {
            cbPhenotype.addItem(phenotype);
        }
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        add(cbPhenotype, gbc);
        cbPhenotype.addActionListener(validateActionListener);

        btnGo = new JButton("Go");
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        add(btnGo, gbc);
        btnGo.addActionListener(e -> {
            Bloodname n = Bloodname.randomBloodname(clans.get(clan).getGenerationCode(), phenotype, year);
            if (n != null) {
                lblName.setText(n.getName() + " (" + n.getFounder() + ")");
                lblOrigClan.setText(Faction.getFaction(n.getOrigClan()).getFullName(year));
                lblPhenotype.setText(n.getPhenotype().getGroupingName());
            }
        });

        JLabel label = new JLabel("Result:");
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        add(label, gbc);

        lblName = new JLabel();
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        add(lblName, gbc);

        label = new JLabel("Original Clan:");
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        add(label, gbc);

        lblOrigClan = new JLabel();
        gbc.gridx = 1;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        add(lblOrigClan, gbc);

        label = new JLabel("Phenotype:");
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 1;
        add(label, gbc);

        lblPhenotype = new JLabel();
        gbc.gridx = 1;
        gbc.gridy = 7;
        gbc.gridwidth = 1;
        add(lblPhenotype, gbc);

        txtWarning = new JLabel();
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(txtWarning, gbc);
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(BloodnameDialog.class);

        this.cbClan.setName("clan");
        preferences.manage(new JComboBoxPreference(cbClan));

        this.cbEra.setName("era");
        preferences.manage(new JComboBoxPreference(cbEra));

        this.cbPhenotype.setName("phenotype");
        preferences.manage(new JComboBoxPreference(cbPhenotype));

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    private ActionListener validateActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent ev) {
            btnGo.setEnabled(validateInput());
        }
    };

    private boolean validateInput() {
        clan = cbClan.getSelectedIndex();
        year = eras[cbEra.getSelectedIndex()];
        phenotype = (Phenotype) cbPhenotype.getSelectedItem();

        if ((clan < 0) || (phenotype == Phenotype.NONE) || (phenotype == null)
                || (year < 2807) || (year > 3150)) {
            return false;
        }

        Clan selectedClan = clans.get(clan);
        String txt = "<html>";

        if (year < selectedClan.getStartDate()) {
            for (int era : eras) {
                if (era >= selectedClan.getStartDate()) {
                    year = era;
                    txt += "<div>" + selectedClan.getFullName(year) + " formed in "
                            + selectedClan.getStartDate() + ". Using " + year + ".</div>";
                    break;
                }
            }

            if (year < selectedClan.getStartDate()) {
                year = selectedClan.getStartDate();
            }
        } else if (year > selectedClan.getEndDate()) {
            for (int i = eras.length - 1; i >= 0; i--) {
                if (eras[i] <= selectedClan.getEndDate()) {
                    year = eras[i];
                    txt += "<div>" + selectedClan.getFullName(year) + " ceased to existed in "
                            + selectedClan.getEndDate() + ". Using " + year + ".</div>";
                    break;
                }
            }

            if (year > selectedClan.getEndDate()) {
                year = selectedClan.getEndDate();
            }
        }

        if ((phenotype == Phenotype.PROTOMECH) && (year < 3060)) {
            txt += "<div>ProtoMechs did not exist in " + year + ". Using Aerospace.</div>";
            phenotype = Phenotype.AEROSPACE;
        } else if ((phenotype == Phenotype.NAVAL) && (!"CSR".equals(selectedClan.getGenerationCode()))) {
            txt += "<div>The Naval phenotype is unique to Clan Snow Raven. Using General.</div>";
            phenotype = Phenotype.GENERAL;
        } else if ((phenotype == Phenotype.VEHICLE) && (!"CHH".equals(selectedClan.getGenerationCode()))) {
            txt += "<div>The vehicle phenotype is unique to Clan Hell's Horses. Using General.</div>";
            phenotype = Phenotype.GENERAL;
        } else if ((phenotype == Phenotype.VEHICLE) && (year < 3100)) {
            txt += "<div>The vehicle phenotype began development in the 32nd century. Using 3100.</div>";
            year = 3100;
        }
        txt += "</html>";
        txtWarning.setText(txt);

        return true;
    }

    public void setFaction(String factionName) {
        cbClan.setSelectedItem(factionName);
    }

    public void setYear(int year) {
        for (int i = eras.length - 1; i >= 0; i--) {
            if (eras[i] <= year) {
                cbEra.setSelectedIndex(i);
                return;
            }
        }
    }
}
