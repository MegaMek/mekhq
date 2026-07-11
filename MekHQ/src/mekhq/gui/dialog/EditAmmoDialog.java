/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;

import megamek.client.ui.util.UIUtil;
import megamek.common.equipment.AmmoMounted;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.MekHQ;

/**
 * Dialog for editing ammo consumption of units during manual scenario resolution.
 * Allows users to adjust the remaining shots for each ammo bin on a unit.
 */
public class EditAmmoDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(EditAmmoDialog.class);

    private static final int DIALOG_MIN_WIDTH = 600;
    private static final int DIALOG_MIN_HEIGHT = 200;
    private static final int DIALOG_MAX_HEIGHT = 500;
    private static final int DIALOG_ROW_HEIGHT = 30;
    private static final int DIALOG_BUTTON_ROW_HEIGHT = 100;
    private static final Insets SPACING_INSETS = new Insets(5, 5, 5, 5);

    private final transient ResourceBundle resourceMap = ResourceBundle.getBundle(
        "mekhq.resources.EditAmmoDialog", MekHQ.getMHQOptions().getLocale());

    private final Entity entity;
    private final List<AmmoBinding> ammoBindings = new ArrayList<>();
    private boolean confirmed = false;

    /**
     * Wrapper class to eliminate parallel list anti-pattern.
     * Groups ammo data with its UI component and validation data.
     */
    private static class AmmoBinding {
        final AmmoMounted ammo;
        final JSpinner spinner;
        final int maxShots;
        
        AmmoBinding(AmmoMounted ammo, JSpinner spinner, int maxShots) {
            this.ammo = ammo;
            this.spinner = spinner;
            this.maxShots = maxShots;
        }
    }

    public EditAmmoDialog(Frame parent, Entity entity) {
        super(parent, true);
        this.entity = entity;
        
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(resourceMap.getString("dialog.title"));
        
        initComponents();
        setLocationRelativeTo(parent);
        pack();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        
        // Main panel with ammo controls
        JPanel ammoPanel = createAmmoPanel();
        
        // Wrap in scrollpane for overflow handling
        JScrollPane scrollPane = new JScrollPane(ammoPanel);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);
        
        // Button panel (always visible, not scrolled)
        add(buildButtonPanel(), BorderLayout.SOUTH);
        
        // Calculate preferred size with scrollbar limit
        int calculatedHeight = Math.max(DIALOG_MIN_HEIGHT, 
            Math.min(DIALOG_MAX_HEIGHT, ammoBindings.size() * DIALOG_ROW_HEIGHT + DIALOG_BUTTON_ROW_HEIGHT));
        setPreferredSize(UIUtil.scaleForGUI(DIALOG_MIN_WIDTH, calculatedHeight));
    }

    /**
     * Creates the main panel containing ammo bin controls.
     * @return configured JPanel with ammo bindings
     */
    private JPanel createAmmoPanel() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(new TitledBorder(
            String.format(resourceMap.getString("panel.title"), entity.getDisplayName())));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = SPACING_INSETS;
        gbc.anchor = GridBagConstraints.WEST;
        
        // Header row
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(new JLabel(resourceMap.getString("label.weaponLocation")), gbc);
        gbc.gridx = 1;
        mainPanel.add(new JLabel(resourceMap.getString("label.ammoType")), gbc);
        gbc.gridx = 2;
        mainPanel.add(new JLabel(resourceMap.getString("label.shotsRemaining")), gbc);
        
        // Add ammo bins
        int row = 1;
        for (AmmoMounted ammo : entity.getAmmo()) {
            if ((ammo != null) && (ammo.getType() != null)) {
                createAmmoRow(mainPanel, ammo, row);
                row++;
            }
        }
        
        // Check if we have any ammo at all
        if (ammoBindings.isEmpty()) {
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.gridwidth = 3;
            gbc.anchor = GridBagConstraints.CENTER;
            mainPanel.add(new JLabel(resourceMap.getString("label.noAmmo")), gbc);
        }
        
        return mainPanel;
    }

    /**
     * Creates a single ammo row with spinner control.
     * @param mainPanel panel to add row to
     * @param ammo the ammo mounted item
     * @param row the row index
     */
    private void createAmmoRow(JPanel mainPanel, AmmoMounted ammo, int row) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = SPACING_INSETS;
        
        try {
            // Weapon name and location
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.anchor = GridBagConstraints.WEST;
            String weaponName = getAmmoWeaponName(ammo);
            String location = entity.getLocationName(ammo.getLocation());
            String displayText = String.format("%s (%s)", weaponName, location);
            mainPanel.add(new JLabel(displayText), gbc);
            
            // Ammo type
            gbc.gridx = 1;
            String ammoName = ammo.getType().getName();
            if (ammo.isHotLoaded()) {
                ammoName = String.format("%s %s", ammoName, resourceMap.getString("label.hotLoaded"));
            }
            mainPanel.add(new JLabel(ammoName), gbc);
            
            // Shots remaining spinner
            gbc.gridx = 2;
            int currentShots = ammo.getBaseShotsLeft();
            int maxShots = calculateMaxShots(ammo, weaponName, currentShots);
            
            SpinnerNumberModel spinnerModel = new SpinnerNumberModel(currentShots, 0, maxShots, 1);
            JSpinner spinner = new JSpinner(spinnerModel);
            spinner.setName(String.format("ammoSpinner_%d", row));
            spinner.setToolTipText(String.format(
                resourceMap.getString("spinner.shotsTooltip"), maxShots));
            
            AmmoBinding binding = new AmmoBinding(ammo, spinner, maxShots);
            ammoBindings.add(binding);
            
            mainPanel.add(spinner, gbc);
            LOGGER.debug("Created ammo row {} for {}: current={}, max={}", 
                row, ammo.getType().getName(), currentShots, maxShots);
            
        } catch (Exception e) {
            LOGGER.error("Error creating spinner for ammo: {}", ammo.getType().getName(), e);
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.gridwidth = 3;
            mainPanel.add(new JLabel(String.format(
                resourceMap.getString("label.error"), e.getMessage())), gbc);
        }
    }

    /**
     * Gets the weapon name for ammo display.
     * @param ammo the ammo mounted item
     * @return weapon name or fallback
     */
    private String getAmmoWeaponName(AmmoMounted ammo) {
        if (ammo.getLinked() != null) {
            return ammo.getLinked().getName();
        } else if (ammo.getLinkedBy() != null) {
            return ammo.getLinkedBy().getName();
        } else {
            return ammo.getType().getName();
        }
    }

    /**
     * Calculates the maximum shots for an ammo bin with fallback logic.
     * @param ammo the ammo mounted item
     * @param weaponName name of weapon (for logging)
     * @param currentShots current shots
     * @return calculated max shots
     */
    private int calculateMaxShots(AmmoMounted ammo, String weaponName, int currentShots) {
        int originalShots = ammo.getOriginalShots();
        
        // Handle case where original shots is 0 (data issue)
        if (originalShots == 0) {
            int ammoCapacity = ammo.getType().getShots();
            if (ammoCapacity > 0) {
                LOGGER.warn("Original shots is 0 for {}, using ammo type capacity ({})", 
                    weaponName, ammoCapacity);
                originalShots = ammoCapacity;
            } else {
                originalShots = Math.max(currentShots, 1);
                LOGGER.warn("Both original and capacity are 0 for {}, using default ({})", 
                    weaponName, originalShots);
            }
        }
        
        // Ensure valid values
        currentShots = Math.max(0, currentShots);
        if (currentShots > originalShots) {
            LOGGER.warn("Ammo {} has more shots ({}) than original ({}), capping at original", 
                ammo.getType().getName(), currentShots, originalShots);
        }
        
        return originalShots;
    }

    /**
     * Builds the button panel with standard buttons.
     * @return configured button panel
     */
    private JPanel buildButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton setAllZeroButton = new JButton(resourceMap.getString("btn.setAllZero.text"));
        setAllZeroButton.setName("setAllZeroButton");
        setAllZeroButton.addActionListener(e -> setAllToZero());
        buttonPanel.add(setAllZeroButton);
        
        JButton setAllFullButton = new JButton(resourceMap.getString("btn.setAllFull.text"));
        setAllFullButton.setName("setAllFullButton");
        setAllFullButton.addActionListener(e -> setAllToFull());
        buttonPanel.add(setAllFullButton);
        
        JButton cancelButton = new JButton(resourceMap.getString("btn.cancel.text"));
        cancelButton.setName("cancelButton");
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(cancelButton);
        
        JButton okayButton = new JButton(resourceMap.getString("btn.okay.text"));
        okayButton.setName("okayButton");
        okayButton.addActionListener(e -> confirmChanges());
        getRootPane().setDefaultButton(okayButton);
        buttonPanel.add(okayButton);
        
        return buttonPanel;
    }

    private void setAllToZero() {
        for (AmmoBinding binding : ammoBindings) {
            binding.spinner.setValue(0);
        }
    }

    private void setAllToFull() {
        for (AmmoBinding binding : ammoBindings) {
            binding.spinner.setValue(binding.maxShots);
        }
    }

    /**
     * Validates and applies ammo changes to the entity.
     */
    private void confirmChanges() {
        try {
            for (AmmoBinding binding : ammoBindings) {
                Integer spinnerValue = (Integer) binding.spinner.getValue();
                if (spinnerValue == null) {
                    continue;
                }
                
                int newShots = spinnerValue;
                
                // Validate the new value
                LOGGER.debug("Validating ammo {}: newShots={}, maxShots={}", 
                    binding.ammo.getType().getName(), newShots, binding.maxShots);
                
                if ((newShots < 0) || (newShots > binding.maxShots)) {
                    LOGGER.warn("Invalid shot count for {}: newShots={}, maxShots={}", 
                        binding.ammo.getType().getName(), newShots, binding.maxShots);
                    JOptionPane.showMessageDialog(this,
                        String.format(resourceMap.getString("error.invalidShotCount"),
                            binding.ammo.getType().getName(), binding.maxShots),
                        resourceMap.getString("error.invalidInput"), 
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                binding.ammo.setShotsLeft(newShots);
                LOGGER.info("Set ammo {} shots to {}", binding.ammo.getType().getName(), newShots);
            }
            
            confirmed = true;
        } catch (Exception e) {
            LOGGER.error("Error confirming ammo changes", e);
            JOptionPane.showMessageDialog(this,
                String.format(resourceMap.getString("error.savingChanges"), e.getMessage()),
                resourceMap.getString("error.title"), 
                JOptionPane.ERROR_MESSAGE);
        }
        
        dispose();
    }

    public boolean wasConfirmed() {
        return confirmed;
    }
}
