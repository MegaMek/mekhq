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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;

import chat.In;
import mekhq.MekHQ;
import mekhq.campaign.personnel.Bloodname;
import mekhq.campaign.personnel.Phenotype;
import mekhq.campaign.universe.Faction;
import mekhq.gui.preferences.JComboBoxPreference;
import mekhq.gui.preferences.JWindowPreference;
import mekhq.preferences.PreferencesNode;

/**
 * @author Neoancient
 *
 * Randomly selects an appropriate Bloodname based on Clan, phenotype, and year
 */
public class BloodnameDialog extends JDialog {
    //region Variable Declaration
    private static final long serialVersionUID = 120186090844572718L;

    // TODO : these should probably not be inline
    public static final int CBS = 0;
    public static final int CB = 1;
    public static final int CCC = 2;
    public static final int CCO = 3;
    public static final int CDS = 4;
    public static final int CFM = 5;
    public static final int CGB = 6;
    public static final int CGS = 7;
    public static final int CHH = 8;
    public static final int CIH = 9;
    public static final int CJF = 10;
    public static final int CM = 11;
    public static final int CNC = 12;
    public static final int CSJ = 13;
    public static final int CSR = 14;
    public static final int CSA = 15;
    public static final int CSV = 16;
    public static final int CSL = 17;
    public static final int CWI = 18;
    public static final int CW = 19;
    public static final int CWIE = 20;
    public static final int CWOV = 21;
    public static final int C_NUM = 22;

    // TODO : these should probably not be inline
    public static final Integer[][] terminus = {
        {2807, 3084},
        {2807, 3059},
        {2807, null},
        {2807, null},
        {2807, null},
        {2807, 3073},
        {2807, null},
        {2807, null},
        {2807, null},
        {2807, 3074},
        {2807, null},
        {2807, 2868},
        {2807, null},
        {2807, 3060},
        {2807, null},
        {2807, null},
        {2807, 3075},
        {3075, null},
        {2807, 2834},
        {2807, null},
        {3057, null},
        {2807, 2823}
    };

    // TODO : these should probably not be inline
    public static final String[] clans = {
            "CBS", "CB", "CCC", "CCO", "CDS", "CFM", "CGB",
            "CGS", "CHH", "CIH", "CJF", "CMG", "CNC", "CSJ",
            "CSR", "CSA", "CSV", "CSL", "CWI", "CW", "CWIE", "CWOV"
    };

    // TODO : these should probably not be inline
    public static final String[] fullNames = {
        "Blood Spirit", "Burrock", "Cloud Cobra", "Coyote",
        "Diamond Shark/Sea Fox", "Fire Mandrill", "Ghost Bear",
        "Goliath Scorpion", "Hell's Horses", "Ice Hellion",
        "Jade Falcon", "Mongoose", "Nova Cat", "Smoke Jaguar",
        "Snow Raven", "Star Adder", "Steel Viper",
        "Stone Lion", "Widowmaker", "Wolf", "Wolf-in-Exile",
        "Wolverine"
    };

    public static Integer[] eras = {
        2807, 2825, 2850, 2900, 2950, 3000, 3050, 3060,
        3075, 3085, 3100
    };

    private JComboBox<String> cbClan;
    private JComboBox<Integer> cbEra;
    private JComboBox<String> cbPhenotype;
    private JButton btnGo;
    private JLabel lblName;
    private JLabel lblOrigClan;
    private JLabel lblPhenotype;
    private JLabel txtWarning;

    // The following are the parameters that the generation will use, and are set whenever the options
    // are modified
    private int clan;
    private int phenotype;
    private int year;
    //endregion Variable Declaration

    public BloodnameDialog(JFrame parent) {
        super (parent, false);
        setTitle ("Bloodname Generator");
        getContentPane().setLayout(new GridBagLayout());
        setPreferredSize(new Dimension(350,300));

        initComponents();

        setLocationRelativeTo(parent);
        pack();
        setUserPreferences();
    }

    private void initComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.NORTHWEST;

        cbClan = new JComboBox<>(fullNames);
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
        for (int i = 0; i <= Phenotype.P_NUM; i++) {
            cbPhenotype.addItem(Phenotype.getBloodnamePhenotypeGroupingName(i));
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
            Bloodname n = Bloodname.randomBloodname(clans[clan], phenotype, year);
            lblName.setText(n.getName() + " (" + n.getFounder() + ")");
            lblOrigClan.setText(Faction.getFaction(n.getOrigClan()).getFullName(year));
            lblPhenotype.setText(Phenotype.getBloodnamePhenotypeGroupingName(n.getPhenotype()));
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
        phenotype = cbPhenotype.getSelectedIndex();

        if ((clan < 0) || (phenotype == Phenotype.P_NONE) || (year < 2807) || (year > 3150)) {
            return false;
        }

        String txt = "<html>";

        if (year < terminus[clan][0]) {
            for (int era : eras) {
                if (era >= terminus[clan][0]) {
                    txt += "<div>" + fullNames[clan] + " formed in " + terminus[clan][0] + ". Using "
                            + era + ".</div>";
                    year = era;
                    break;
                }
            }
        }
        if ((terminus[clan][1] != null) && (year > terminus[clan][1])) {
            for (int i = eras.length - 1; i >= 0; i--) {
                if (eras[i] <= terminus[clan][1]) {
                    txt += "<div>" + fullNames[clan] + " ceased to existed in " + terminus[clan][1]
                            + ". Using " + eras[i] + ".</div>";
                    year = eras[i];
                    break;
                }
            }
        }
        if ((phenotype == Phenotype.P_PROTOMECH) && (year < 3060)) {
            txt += "<div>ProtoMechs did not exist in " + year + ". Using Aerospace.</div>";
            phenotype = Phenotype.P_AEROSPACE;
        }
        if ((phenotype == Phenotype.P_NAVAL) && (clan != CSR)) {
            txt += "<div>The Naval phenotype is unique to Clan Snow Raven. Using General.</div>";
            phenotype = Phenotype.P_GENERAL;
        }
        if ((phenotype == Phenotype.P_VEHICLE) && (clan != CHH)) {
             txt += "<div>The vehicle phenotype is unique to Clan Hell's Horses. Using General.</div>";
             phenotype = Phenotype.P_GENERAL;
        } else if ((phenotype == Phenotype.P_VEHICLE) && (year < 3100)) {
            txt += "<div>The vehicle phenotype began development in the 32nd century. Using 3100.</div>";
            year = 3100;
        }
        txt += "</html>";
        txtWarning.setText(txt);

        return true;
    }

    public void setFaction(String factionName) {
        // We set based on the full faction name, which will have a Clan in it. We therefore need
        // to split off the clan to ensure that the rest of the comparison is good
        String[] split = factionName.split(" ", 2);
        if (split.length == 2) {
            cbClan.setSelectedItem(split[1]);
        }
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
