/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import megamek.client.ui.dialogs.customMek.BombChoicePanel;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.AmmoType;
import megamek.common.BombLoadout;
import megamek.common.BombType;
import megamek.common.BombType.BombTypeEnum;
import megamek.common.EquipmentType;
import megamek.common.IBomber;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.gui.utilities.JScrollPaneWithSpeed;

/**
 * @author Deric Page (dericpage@users.sourceforge.net)
 * @author Joshua Bartz (jbartz at sbcglobal.net)
 * @since 4/7/2012
 */
public class BombsDialog extends JDialog implements ActionListener {
    private static final MMLogger logger = MMLogger.create(BombsDialog.class);

    private BombChoicePanel bombPanel;
    private final IBomber bomber;
    private final Campaign campaign;

    private final BombLoadout initialBombChoices;
    private final BombLoadout availableBombs = new BombLoadout();
    private final BombLoadout maxAvailable = new BombLoadout();

    // Maps bomb types to warehouse part IDs
    private final EnumMap<BombTypeEnum, Integer> bombCatalog = new EnumMap<>(BombTypeEnum.class);

    private JButton okayButton;
    private JButton cancelButton;

    public BombsDialog(IBomber iBomber, Campaign campaign, JFrame parent) {
        super(parent, "Select Bombs", true);
        this.bomber = iBomber;
        this.campaign = campaign;
        this.initialBombChoices = new BombLoadout(bomber.getBombChoices());

        initGUI();
        validate();
        pack();
        setLocationRelativeTo(parent);
        setUserPreferences();
    }

    private void initGUI() {
        buildBombInventory();
        calculateMaxAvailable();

        // BombChoicePanel takes care of managing internal and external stores, so we don't need to here.
        bombPanel = new BombChoicePanel(
              bomber,
              campaign.getGameOptions().booleanOption("at2_nukes"),
              true,
              maxAvailable
        );

        // Set up the display of this dialog.
        JScrollPane scroller = new JScrollPaneWithSpeed(bombPanel);
        scroller.setPreferredSize(new Dimension(300, 200));
        setLayout(new BorderLayout());
        add(scroller, BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);
    }

    /**
     * Scans warehouse for available bombs and builds the catalog.
     */
    private void buildBombInventory() {
        // Clear existing data
        bombCatalog.clear();
        availableBombs.clear();

        campaign.getWarehouse().forEachSparePart(spare -> {
            if (isBombAmmoStorage(spare)) {
                AmmoStorage ammoStorage = (AmmoStorage) spare;
                BombTypeEnum bombType = BombTypeEnum.fromInternalName(
                      ammoStorage.getType().getInternalName()
                );

                if ((bombType != null) && (bombType != BombTypeEnum.NONE)) {
                    // Using bombCatalog to store the part ID's of the bombs so don't have to keep full spare list in memory and
                    // for ease of access later
                    bombCatalog.put(bombType, spare.getId());
                    availableBombs.put(bombType, ammoStorage.getShots());
                }
            }
        });
    }

    /**
     * Checks if a spare part is bomb ammunition storage.
     */
    private boolean isBombAmmoStorage(Object spare) {
        return (spare instanceof AmmoStorage) &&
                     (((EquipmentPart) spare).getType() instanceof BombType) &&
                     ((AmmoStorage) spare).isPresent();
    }

    /**
     * Calculates maximum available bombs (warehouse + current loadout).
     */
    private void calculateMaxAvailable() {
        maxAvailable.clear();

        // Start with available bombs from warehouse
        for (Map.Entry<BombTypeEnum, Integer> entry : availableBombs.entrySet()) {
            maxAvailable.put(entry.getKey(), entry.getValue());
        }

        // Add current bomb choices to maximums
        for (Map.Entry<BombTypeEnum, Integer> entry : initialBombChoices.entrySet()) {
            BombTypeEnum bombType = entry.getKey();
            int count = entry.getValue();
            maxAvailable.addBombs(bombType, count);
        }
    }

    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));

        okayButton = new JButton("Okay");
        okayButton.setMnemonic('o');
        okayButton.addActionListener(this);
        panel.add(okayButton);

        cancelButton = new JButton("Cancel");
        cancelButton.setMnemonic('c');
        cancelButton.addActionListener(this);
        panel.add(cancelButton);

        return panel;
    }

    /**
     * These need to be migrated to the Suite Constants / Suite Options Setup
     */
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(BombsDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            logger.error("Failed to set user preferences", ex);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (okayButton.equals(e.getSource())) {
            handleOkayAction();
        } else if (cancelButton.equals(e.getSource())) {
            setVisible(false);
        }
    }

    /**
     * Handles the okay button action - applies bomb choices and updates warehouse.
     */
    private void handleOkayAction() {
        // Apply the bomb panel choices to the bomber
        bombPanel.applyChoice();
        BombLoadout newLoadout = bombPanel.getChoice();

        if (newLoadout == null) {
            newLoadout = new BombLoadout();
        }

        // Calculate the difference between initial and new loadouts
        Map<BombTypeEnum, Integer> warehouseDelta = calculateWarehouseDelta(initialBombChoices, newLoadout);

        // Update warehouse based on the delta
        updateWarehouse(warehouseDelta);

        setVisible(false);
    }

    /**
     * Calculates the change in warehouse inventory needed. Positive values mean bombs are being returned to warehouse.
     * Negative values mean bombs are being taken from warehouse.
     */
    private Map<BombTypeEnum, Integer> calculateWarehouseDelta(BombLoadout initial, BombLoadout newLoadout) {
        // Create a map to hold the delta of bomb counts
        // We don't use a BombLoadout here because it wouldn't handle negative counts
        Map<BombTypeEnum, Integer> delta = new HashMap<>();

        // Check all bomb types that exist in either loadout
        for (BombTypeEnum bombType : BombTypeEnum.values()) {
            if (bombType == BombTypeEnum.NONE) {continue;}

            int initialCount = initial.getCount(bombType);
            int newCount = newLoadout.getCount(bombType);
            int difference = initialCount - newCount;

            if (difference != 0) {
                delta.put(bombType, difference);
            }
        }

        return delta;
    }

    /**
     * Updates warehouse inventory based on bomb loadout changes.
     */
    private void updateWarehouse(Map<BombTypeEnum, Integer> delta) {
        for (Map.Entry<BombTypeEnum, Integer> entry : delta.entrySet()) {
            BombTypeEnum bombType = entry.getKey();
            int deltaCount = entry.getValue();

            if (deltaCount == 0) {continue;}

            updateWarehouseBombType(bombType, deltaCount);
        }
    }

    /**
     * Updates warehouse for a specific bomb type.
     */
    private void updateWarehouseBombType(BombTypeEnum bombType, int deltaCount) {
        Integer partId = bombCatalog.get(bombType);

        if (partId != null && partId > 0) {
            // Existing warehouse entry
            updateExistingWarehouseEntry(partId, deltaCount);
        } else if (deltaCount > 0) {
            // No existing entry but adding bombs - create new warehouse entry
            createNewWarehouseEntry(bombType, deltaCount);
        } else {
            // No existing entry and removing bombs - do nothing
            // (deltaCount < 0 with no existing partId means we can't remove anything)
            logger.warn("Attempted to remove bombs of type {} with no existing warehouse entry.", bombType);
        }
    }

    /**
     * Updates an existing warehouse entry.
     */
    private void updateExistingWarehouseEntry(int partId, int deltaCount) {
        AmmoStorage storedBombs = (AmmoStorage) campaign.getWarehouse().getPart(partId);
        if (storedBombs != null) {
            storedBombs.changeShots(deltaCount);

            if (storedBombs.getShots() <= 0) {
                campaign.getWarehouse().removePart(storedBombs);
            }
        }
    }

    /**
     * Creates a new warehouse entry for excess bombs.
     */
    private void createNewWarehouseEntry(BombTypeEnum bombType, int count) {
        try {
            AmmoType ammoType = (AmmoType) EquipmentType.get(bombType.getInternalName());
            if (ammoType != null) {
                AmmoStorage excessBombs = new AmmoStorage(0, ammoType, count, campaign);
                campaign.getQuartermaster().addPart(excessBombs, 0);
            } else {
                logger.error("Could not find AmmoType for bomb: {}", bombType.getInternalName());
            }
        } catch (Exception ex) {
            logger.error("Failed to create warehouse entry for bomb type: {}", bombType.getInternalName(), ex);
        }
    }
}
