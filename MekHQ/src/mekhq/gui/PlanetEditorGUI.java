/*
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;

@SuppressWarnings("unused") // FIXME!
public class PlanetEditorGUI extends JPanel {
    private final JPanel pnlGeneral = new JPanel();
    private final JLabel lblSlot = new JLabel("Slot: ");
    private JComboBox<String> orbitalSlot;
    private final JLabel lblType = new JLabel("Type: ");
    private JComboBox<String> type;
    private final JLabel lblPressure = new JLabel("Pressure: ");
    private JComboBox<String> pressure;
    private final JLabel lblLifezone = new JLabel("Lifezone: ");
    private JComboBox<String> lifeZone;
    private final JLabel lblClimate = new JLabel("Climate: ");
    private JComboBox<String> climate;
    private final JLabel lblLifeforms = new JLabel("Lifeforms: ");
    private JComboBox<String> lifeforms;
    private final JLabel lblDensity = new JLabel("Density: ");
    private JTextField density;
    private final JLabel lblDay = new JLabel("Day Length: ");
    private JTextField dayLength;
    private final JLabel lblYear = new JLabel("Year Length: ");
    private JTextField yearLength;
    private final JLabel lblDiameter = new JLabel("Diameter: ");
    private JTextField diameter;
    private final JLabel lblGravity = new JLabel("Gravity: ");
    private JTextField gravity;
    private final JLabel lblPercentWater = new JLabel("Percent Water: ");
    private JSpinner percentWater;
    private final JLabel lblTemp = new JLabel("Temperature: ");
    private JSpinner temperature;
    private final JLabel lblLandmasses = new JLabel("Land Masses: ");
    private JSpinner landmasses;

    // Habitability
    private final JPanel pnlHabitability = new JPanel();
    private ButtonGroup habGroup;
    private JRadioButton habTrue;
    private JRadioButton habFalse;

    // Hyper-pulse Generators
    private final JPanel pnlHPG = new JPanel();
    private ButtonGroup hpgGroup;
    private JRadioButton hpgClassA;
    private JRadioButton hpgClassB;
    private JRadioButton hpgClassC;

    // Zenith Recharge Station?
    private final JPanel pnlZenith = new JPanel();
    private ButtonGroup zenithGroup;
    private JRadioButton zenithTrue;
    private JRadioButton zenithFalse;

    // Nadir Recharge Station?
    private final JPanel pnlNadir = new JPanel();
    private ButtonGroup nadirGroup;
    private JRadioButton nadirTrue;
    private JRadioButton nadirFalse;

    // Socio-Industrial Ratings
    private final JPanel pnlSocioIndi = new JPanel();
    private JSpinner socioIndustrial1;
    private JSpinner socioIndustrial2;
    private JSpinner socioIndustrial3;
    private JSpinner socioIndustrial4;
    private JSpinner socioIndustrial5;

    public PlanetEditorGUI(boolean fullEditor) {
        setLayout(new GridBagLayout());
        initializeComponents(fullEditor);
    }

    public void initializeComponents(boolean fullEditor) {
        GridBagConstraints gbc = new GridBagConstraints();

        // Start General Panel
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        pnlGeneral.add(lblSlot, gbc);
        gbc.gridx++;
        pnlGeneral.add(orbitalSlot, gbc);
        gbc.gridx--;
        gbc.gridy++;
        pnlGeneral.add(lblType, gbc);
        gbc.gridx++;
        pnlGeneral.add(type, gbc);
        gbc.gridx--;
        gbc.gridy++;
        pnlGeneral.add(lblPressure, gbc);
        gbc.gridx++;
        pnlGeneral.add(pressure, gbc);
        gbc.gridx--;
        gbc.gridy++;
        pnlGeneral.add(lblLifezone, gbc);
        gbc.gridx++;
        pnlGeneral.add(lifeZone, gbc);
        gbc.gridx--;
        gbc.gridy++;
        pnlGeneral.add(lblClimate, gbc);
        gbc.gridx++;
        pnlGeneral.add(climate, gbc);
        gbc.gridx--;
        gbc.gridy++;
        pnlGeneral.add(lblLifeforms, gbc);
        gbc.gridx++;
        pnlGeneral.add(lifeforms, gbc);
        gbc.gridx--;
        gbc.gridy++;
        pnlGeneral.add(lblDensity, gbc);
        gbc.gridx++;
        pnlGeneral.add(density, gbc);
        gbc.gridx--;
        gbc.gridy++;
        pnlGeneral.add(lblDay, gbc);
        gbc.gridx++;
        pnlGeneral.add(dayLength, gbc);
        gbc.gridx--;
        gbc.gridy++;
        pnlGeneral.add(lblYear, gbc);
        gbc.gridx++;
        pnlGeneral.add(yearLength, gbc);
        gbc.gridx--;
        gbc.gridy++;
        pnlGeneral.add(lblDiameter, gbc);
        gbc.gridx++;
        pnlGeneral.add(diameter, gbc);
        gbc.gridx--;
        gbc.gridy++;
        pnlGeneral.add(lblGravity, gbc);
        gbc.gridx++;
        pnlGeneral.add(gravity, gbc);
        gbc.gridx--;
        gbc.gridy++;
        pnlGeneral.add(lblPercentWater, gbc);
        gbc.gridx++;
        pnlGeneral.add(percentWater, gbc);
        gbc.gridx--;
        gbc.gridy++;
        pnlGeneral.add(lblTemp, gbc);
        gbc.gridx++;
        pnlGeneral.add(temperature, gbc);
        gbc.gridx--;
        gbc.gridy++;
        pnlGeneral.add(lblLandmasses, gbc);
        gbc.gridx++;
        pnlGeneral.add(landmasses, gbc);

        gbc.gridx = 0;
        gbc.gridy = 0;
        pnlGeneral.setBorder(BorderFactory.createTitledBorder("General"));
        add(pnlGeneral, gbc);
        // End General Panel
    }
}
