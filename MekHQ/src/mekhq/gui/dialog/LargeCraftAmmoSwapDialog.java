/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import megamek.client.ui.dialogs.customMek.BayMunitionsChoicePanel;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.equipment.Mounted;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.LargeCraftAmmoBin;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.actions.AdjustLargeCraftAmmoAction;
import mekhq.gui.utilities.JScrollPaneWithSpeed;

/**
 * @author Neoancient
 */
public class LargeCraftAmmoSwapDialog extends JDialog {
    private static final MMLogger logger = MMLogger.create(LargeCraftAmmoSwapDialog.class);

    private final Unit unit;
    private final BayMunitionsChoicePanel mainPanel;
    private boolean canceled = true;

    public LargeCraftAmmoSwapDialog(final JFrame frame, final Unit unit) {
        super(frame, true);
        this.unit = unit;

        getContentPane().setLayout(new BorderLayout());
        mainPanel = new BayMunitionsChoicePanel(unit.getEntity(), unit.getCampaign().getGame());
        getContentPane().add(new JScrollPaneWithSpeed(mainPanel), BorderLayout.CENTER);
        JPanel panButtons = new JPanel();
        JButton button = new JButton("OK");
        button.addActionListener(ev -> apply());
        panButtons.add(button);
        button = new JButton("Cancel");
        button.addActionListener(ev -> setVisible(false));
        panButtons.add(button);
        getContentPane().add(panButtons, BorderLayout.SOUTH);

        pack();
        setUserPreferences();
    }

    /**
     * These need to be migrated to the Suite Constants / Suite Options Setup
     */
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(LargeCraftAmmoSwapDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            logger.error("Failed to set user preferences", ex);
        }
    }

    public boolean wasCanceled() {
        return canceled;
    }

    private void apply() {
        // Save the current number of shots by bay and ammo type
        Map<Mounted<?>, Map<String, Integer>> shotsByBay = new HashMap<>();
        for (Part p : unit.getParts()) {
            if (p instanceof LargeCraftAmmoBin bin) {
                Mounted<?> m = unit.getEntity().getEquipment(bin.getEquipmentNum());
                shotsByBay.putIfAbsent(bin.getBay(), new HashMap<>());
                shotsByBay.get(bin.getBay()).merge(bin.getType().getInternalName(), m.getBaseShotsLeft(), Integer::sum);
            }
        }
        // Actually apply the ammo change
        mainPanel.apply();

        // Rebuild bin parts as necessary
        new AdjustLargeCraftAmmoAction().execute(unit.getCampaign(), unit);

        // Update the parts and set the number of shots needed based on the current size and the number of shots stored.
        for (Part p : unit.getParts()) {
            if (p instanceof LargeCraftAmmoBin bin) {
                bin.updateConditionFromEntity(false);
                Mounted<?> ammo = unit.getEntity().getEquipment(bin.getEquipmentNum());
                int oldShots = shotsByBay.get(bin.getBay()).getOrDefault(bin.getType().getInternalName(), 0);

                // If we're removing ammo, add it the warehouse
                int shotsToChange = oldShots - ammo.getBaseShotsLeft();
                if (bin.getCapacity() == 0) {
                    // Then we've got a valid bin for which the ammo's out
                    shotsToChange = oldShots;
                }

                if (shotsToChange > 0) {
                    unit.getCampaign().getQuartermaster().addAmmo(bin.getType(), shotsToChange);
                }

                if (shotsByBay.containsKey(bin.getBay())) {
                    Map<String, Integer> oldAmmo = shotsByBay.get(bin.getBay());
                    if (oldAmmo.containsKey(bin.getType().getInternalName())) {
                        // We've found the matching ammo bin, even though they've moved around.
                        if (shotsToChange < 0) {
                            // We need to load some extra ammo, but part of the bin is already loaded
                            bin.setShotsNeeded(Math.abs(shotsToChange));
                        } else {
                            // If we've just removed ammo, don't do anything else.
                            continue;
                        }
                    } else {
                        // We've got a new bin for a new ammo type. It needs loading.
                        bin.setShotsNeeded(bin.getFullShots());
                    }
                } else {
                    // This bin isn't on our original ammo list at all. It needs loading.
                    // This shouldn't ever happen - it would mean we've created a totally new bay.
                    bin.setShotsNeeded(bin.getFullShots());
                }
                bin.updateConditionFromPart();
            }
        }
        canceled = false;
        setVisible(false);
    }
}
