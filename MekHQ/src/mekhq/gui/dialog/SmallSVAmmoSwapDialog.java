/*
 * Copyright (c) 2020 - The MegaMek Team
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

import megamek.common.AmmoType;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.InfantryAmmoBin;
import mekhq.campaign.unit.Unit;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Configures amount of standard and inferno ammo available to small support vehicle weapons.
 */
public class SmallSVAmmoSwapDialog extends JDialog {

    private final List<WeaponRow> rows = new ArrayList<>();
    private boolean canceled = true;
    private final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.SmallSVAmmoSwapDialog",
            new EncodeControl());

    public SmallSVAmmoSwapDialog(Frame frame, Unit unit) {
        super(frame, true);
        setTitle(unit.getName());
        getContentPane().setLayout(new BorderLayout());
        JPanel panMain = new JPanel();
        panMain.setLayout(new BoxLayout(panMain, BoxLayout.Y_AXIS));
        getContentPane().add(panMain, BorderLayout.CENTER);
        // Since we only care about weapons that have the option of inferno ammo,
        // we can search for all the ammo bins with inferno ammo and build
        // from there.
        for (Part part : unit.getParts()) {
            if ((part instanceof InfantryAmmoBin)
                    && (((InfantryAmmoBin) part).getType().getMunitionType() == AmmoType.M_INFERNO)) {
                WeaponRow row = new WeaponRow((InfantryAmmoBin) part);
                rows.add(row);
                panMain.add(row);
            }
        }

        JPanel panButtons = new JPanel();
        JButton button = new JButton(resourceMap.getString("cancel"));
        button.addActionListener(ev -> setVisible(false));
        panButtons.add(button);
        button = new JButton(resourceMap.getString("ok"));
        button.addActionListener(ev -> {
            rows.forEach(WeaponRow::apply);
            canceled = false;
            setVisible(false);
        });
        panButtons.add(button);
        getContentPane().add(panButtons, BorderLayout.SOUTH);

        pack();
        setUserPreferences();
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(SmallSVAmmoSwapDialog.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    public boolean wasCanceled() {
        return canceled;
    }

    private class WeaponRow extends JPanel {
        private final InfantryAmmoBin standardBin;
        private final InfantryAmmoBin infernoBin;
        private int totalClips;
        private final JSpinner spnInferno = new JSpinner();
        private final JLabel lblStandardClips = new JLabel();

        WeaponRow(InfantryAmmoBin infernoBin) {
            this.infernoBin = infernoBin;
            this.standardBin = infernoBin.findPartnerBin();
            if (standardBin != null) {
                totalClips = infernoBin.getClips() + standardBin.getClips();
            }
            initUI(String.format("%s (%s)", infernoBin.getWeaponType().getName(),
                    infernoBin.getUnit().getEntity().getLocationAbbr(infernoBin.getLocation())));
        }

        private void initUI(String title) {
            spnInferno.setModel(new SpinnerNumberModel(infernoBin.getClips(), 0, totalClips, 1));
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 4;
            add(new JLabel(title), gbc);
            gbc.gridx = 4;
            gbc.gridwidth = 1;
            add(new JLabel(String.format(resourceMap.getString("shotsPerClip.format"),
                    infernoBin.getWeaponType().getShots())), gbc);
            gbc.gridx = 0;
            gbc.gridy++;
            // It should not be possible to have an inferno bin but no standard bin.
            // If the standard ammo bin isn't found, something went wrong. Report it
            // rather than barfing.
            if (standardBin != null) {
                add(new JLabel(resourceMap.getString("standard")), gbc);
                gbc.gridx++;
                lblStandardClips.setText(String.valueOf(standardBin.getClips()));
                add(lblStandardClips, gbc);
                gbc.gridx++;
                add(new JLabel(resourceMap.getString("inferno")), gbc);
                gbc.gridx++;
                add(spnInferno, gbc);
                spnInferno.addChangeListener(ev ->
                        lblStandardClips.setText(String.valueOf(totalClips - ((Integer) spnInferno.getValue()))));
            } else {
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                add(new JLabel(resourceMap.getString("noStandardBin")), gbc);
            }
        }

        void apply() {
            if (standardBin != null) {
                int infernoClips = (Integer) spnInferno.getValue();
                infernoBin.changeCapacity(infernoClips);
                standardBin.changeCapacity(totalClips - infernoClips);
            }
        }
    }
}
