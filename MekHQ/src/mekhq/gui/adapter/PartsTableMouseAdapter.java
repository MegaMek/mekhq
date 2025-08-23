/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.adapter;

import java.awt.event.ActionEvent;
import java.util.Optional;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;

import megamek.common.rolls.TargetRoll;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.event.PartChangedEvent;
import mekhq.campaign.event.PartModeChangedEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.work.WorkTime;
import mekhq.gui.CampaignGUI;
import mekhq.gui.dialog.MRMSDialog;
import mekhq.gui.dialog.PopupValueChoiceDialog;
import mekhq.gui.model.PartsTableModel;
import mekhq.service.enums.MRMSMode;

public class PartsTableMouseAdapter extends JPopupMenuAdapter {
    private static final MMLogger logger = MMLogger.create(PartsTableMouseAdapter.class);

    private CampaignGUI gui;
    private JTable partsTable;
    private PartsTableModel partsModel;

    protected PartsTableMouseAdapter(CampaignGUI gui, JTable partsTable, PartsTableModel partsModel) {
        this.gui = gui;
        this.partsTable = partsTable;
        this.partsModel = partsModel;
    }

    public static void connect(CampaignGUI gui, JTable partsTable, PartsTableModel partsModel) {
        new PartsTableMouseAdapter(gui, partsTable, partsModel).connect(partsTable);
    }

    @Override
    public void actionPerformed(ActionEvent action) {
        String command = action.getActionCommand();
        int row = partsTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        Part selectedPart = partsModel.getPartAt(partsTable.convertRowIndexToModel(row));
        int[] rows = partsTable.getSelectedRows();
        Part[] parts = new Part[rows.length];
        for (int i = 0; i < rows.length; i++) {
            parts[i] = partsModel.getPartAt(partsTable.convertRowIndexToModel(rows[i]));
        }
        if (command.equalsIgnoreCase("SELL")) {
            for (Part p : parts) {
                if (null != p) {
                    gui.getCampaign().getQuartermaster().sellPart(p, 1);
                }
            }
        } else if (command.equalsIgnoreCase("SELL_ALL")) {
            for (Part p : parts) {
                if (null != p) {
                    gui.getCampaign().getQuartermaster().sellPart(p);
                }
            }
        } else if (command.equalsIgnoreCase("SELL_N")) {
            if (null != selectedPart) {
                int n = selectedPart.getQuantity();
                if (selectedPart instanceof AmmoStorage) {
                    n = ((AmmoStorage) selectedPart).getShots();
                }
                if (selectedPart instanceof Armor) {
                    n = ((Armor) selectedPart).getAmount();
                }
                PopupValueChoiceDialog pvcd = new PopupValueChoiceDialog(gui.getFrame(),
                      true,
                      "Sell How Many " + selectedPart.getName() + "s?",
                      0,
                      0,
                      n);
                pvcd.setVisible(true);
                if (pvcd.getValue() < 0) {
                    return;
                }
                int q = pvcd.getValue();
                gui.getCampaign().getQuartermaster().sellPart(selectedPart, q);
            }
        } else if (command.equalsIgnoreCase("CANCEL_ORDER")) {
            Money refundAmount = Money.zero();
            for (Part p : parts) {
                if (null != p) {
                    refundAmount = refundAmount.plus(p.getActualValue()
                                                           .multipliedBy(p.getQuantity())
                                                           .multipliedBy(gui.getCampaign()
                                                                               .getCampaignOptions()
                                                                               .getCancelledOrderRefundMultiplier()));
                    gui.getCampaign().getWarehouse().removePart(p);
                }
            }
            gui.getCampaign()
                  .getFinances()
                  .credit(TransactionType.EQUIPMENT_PURCHASE,
                        gui.getCampaign().getLocalDate(),
                        refundAmount,
                        "refund for cancelled equipment sale");
        } else if (command.equalsIgnoreCase("ARRIVE")) {
            for (Part p : parts) {
                if (null != p) {
                    gui.getCampaign().getQuartermaster().arrivePart(p);
                }
            }
        } else if (command.equalsIgnoreCase("REMOVE")) {
            for (Part p : parts) {
                if (null != p) {
                    gui.getCampaign().getWarehouse().removePart(p);
                }
            }
        } else if (command.equalsIgnoreCase("REMOVE_N")) {
            if (null != selectedPart) {
                int n = selectedPart.getQuantity();
                if (selectedPart instanceof AmmoStorage) {
                    n = ((AmmoStorage) selectedPart).getShots();
                }
                if (selectedPart instanceof Armor) {
                    n = ((Armor) selectedPart).getAmount();
                }
                PopupValueChoiceDialog dialog = new PopupValueChoiceDialog(gui.getFrame(),
                      true,
                      "Remove How Many " + selectedPart.getName() + "s?",
                      0,
                      0,
                      n);
                dialog.setVisible(true);
                if (dialog.getValue() < 0) {
                    return;
                }
                int quantity = dialog.getValue();
                gui.getCampaign().getWarehouse().removePart(selectedPart, quantity);
            }
        } else if (command.equalsIgnoreCase("ADD_N")) {
            if (null != selectedPart) {
                PopupValueChoiceDialog dialog = new PopupValueChoiceDialog(gui.getFrame(),
                      true,
                      "Add How Many " + selectedPart.getName() + "s?",
                      0,
                      0,
                      9999999);
                dialog.setVisible(true);
                if (dialog.getValue() < 0) {
                    return;
                }

                int quantity = dialog.getValue();
                Part clonedPart = selectedPart.clone();

                if (selectedPart instanceof AmmoStorage) {
                    ((AmmoStorage) clonedPart).setShots(quantity);
                } else if (selectedPart instanceof Armor) {
                    ((Armor) clonedPart).setAmount(quantity);
                } else {
                    clonedPart.setQuantity(quantity);
                }

                gui.getCampaign().getWarehouse().addPart(clonedPart, true);
            }
        } else if (command.contains("SET_QUALITY")) {
            boolean reverse = gui.getCampaign().getCampaignOptions().isReverseQualityNames();
            Object[] possibilities = { PartQuality.QUALITY_A.toName(reverse), PartQuality.QUALITY_B.toName(reverse),
                                       PartQuality.QUALITY_C.toName(reverse), PartQuality.QUALITY_D.toName(reverse),
                                       PartQuality.QUALITY_E.toName(reverse), PartQuality.QUALITY_F.toName(reverse) };

            String quality = (String) JOptionPane.showInputDialog(gui.getFrame(),
                  "Choose the new quality level",
                  "Set Quality",
                  JOptionPane.PLAIN_MESSAGE,
                  null,
                  possibilities,
                  PartQuality.QUALITY_D.toName(reverse));

            if (quality != null) {
                PartQuality partQuality = PartQuality.fromName(quality, reverse);

                for (Part part : parts) {
                    if (part != null) {
                        part.setQuality(partQuality);
                        MekHQ.triggerEvent(new PartChangedEvent(part));
                    }
                }
            }
        } else if (command.contains("SWITCH_NEWNESS")) {
            for (Part part : parts) {
                if (part != null) {
                    part.setBrandNew(!part.isBrandNew());
                    MekHQ.triggerEvent(new PartChangedEvent(part));
                }
            }
        } else if (command.contains("CHANGE_MODE")) {
            String sel = command.split(":")[1];
            for (Part p : parts) {
                if (p.getAllMods(null).getValue() != TargetRoll.AUTOMATIC_SUCCESS) {
                    p.setMode(WorkTime.of(sel));
                    MekHQ.triggerEvent(new PartModeChangedEvent(p));
                }
            }
        } else if (command.contains("MRMS")) {
            new MRMSDialog(gui.getFrame(), true, gui, MRMSMode.WAREHOUSE).setVisible(true);
        } else if (command.equalsIgnoreCase("DEPOD")) {
            for (Part p : parts) {
                if (null != p) {
                    gui.getCampaign().getQuartermaster().depodPart(p, 1);
                }
            }
        } else if (command.equalsIgnoreCase("DEPOD_ALL")) {
            for (Part p : parts) {
                if (null != p) {
                    gui.getCampaign().getQuartermaster().depodPart(p);
                }
            }
        } else if (command.equalsIgnoreCase("DEPOD_N")) {
            if (null != selectedPart) {
                int n = selectedPart.getQuantity();
                PopupValueChoiceDialog pvcd = new PopupValueChoiceDialog(gui.getFrame(),
                      true,
                      "Remove How Many Pods" + selectedPart.getName() + "s?",
                      1,
                      1,
                      n);
                pvcd.setVisible(true);
                if (pvcd.getValue() < 0) {
                    return;
                }
                int q = pvcd.getValue();
                gui.getCampaign().getQuartermaster().depodPart(selectedPart, q);
            }
        } else if (command.equalsIgnoreCase("BUY")) {
            for (Part p : parts) {
                if (null != p) {
                    gui.getCampaign().getShoppingList().addShoppingItem(p.getAcquisitionWork(), 1, gui.getCampaign());
                }
            }
        } else if (command.equalsIgnoreCase("BUY_N")) {
            if (null != selectedPart) {
                PopupValueChoiceDialog pvcd = new PopupValueChoiceDialog(gui.getFrame(),
                      true,
                      "Buy How Much " + selectedPart.getName(),
                      1,
                      1);
                pvcd.setVisible(true);
                if (pvcd.getValue() < 1) {
                    return;
                }
                int q = pvcd.getValue();
                gui.getCampaign()
                      .getShoppingList()
                      .addShoppingItem(selectedPart.getAcquisitionWork(), q, gui.getCampaign());
            }
        }
    }

    public boolean areAllPartsArmor(Part[] parts) {
        for (Part p : parts) {
            if (!(p instanceof Armor)) {
                return false;
            }
        }
        return true;
    }

    public boolean areAllPartsAmmo(Part[] parts) {
        for (Part p : parts) {
            if (!(p instanceof AmmoStorage)) {
                return false;
            }
        }
        return true;
    }

    public boolean areAllPartsNotAmmo(Part[] parts) {
        for (Part p : parts) {
            if (p instanceof AmmoStorage) {
                return false;
            }
        }
        return true;
    }

    public boolean areAllPartsPresent(Part[] parts) {
        for (Part p : parts) {
            if (!p.isPresent()) {
                return false;
            }
        }
        return true;
    }

    public boolean areAllPartsInTransit(Part[] parts) {
        for (Part p : parts) {
            if (p.isPresent()) {
                return false;
            }
        }
        return true;
    }

    public boolean areAllPartsPodded(Part[] parts) {
        for (Part p : parts) {
            if (!p.isOmniPodded()) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected Optional<JPopupMenu> createPopupMenu() {
        if (partsTable.getSelectedRowCount() == 0) {
            return Optional.empty();
        }

        JPopupMenu popup = new JPopupMenu();

        int[] rows = partsTable.getSelectedRows();
        JMenuItem menuItem = null;
        JMenu menu = null;
        JCheckBoxMenuItem cbMenuItem = null;
        Part[] parts = new Part[rows.length];
        boolean oneSelected = false;
        for (int i = 0; i < rows.length; i++) {
            parts[i] = partsModel.getPartAt(partsTable.convertRowIndexToModel(rows[i]));
        }
        Part part = null;
        if (parts.length == 1) {
            oneSelected = true;
            part = parts[0];
        }
        // **lets fill the pop up menu**//
        // sell part
        if (gui.getCampaign().getCampaignOptions().isSellParts() && areAllPartsPresent(parts)) {
            menu = new JMenu("Sell");
            if (areAllPartsAmmo(parts)) {
                menuItem = new JMenuItem("Sell All Ammo of This Type");
                menuItem.setActionCommand("SELL_ALL");
                menuItem.addActionListener(this);
                menu.add(menuItem);
                if (oneSelected && ((AmmoStorage) part).getShots() > 1) {
                    menuItem = new JMenuItem("Sell # Ammo of This Type...");
                    menuItem.setActionCommand("SELL_N");
                    menuItem.addActionListener(this);
                    menu.add(menuItem);
                }
            } else if (areAllPartsArmor(parts)) {
                menuItem = new JMenuItem("Sell All Armor of This Type");
                menuItem.setActionCommand("SELL_ALL");
                menuItem.addActionListener(this);
                menu.add(menuItem);
                if (oneSelected && ((Armor) part).getAmount() > 1) {
                    menuItem = new JMenuItem("Sell # Armor points of This Type...");
                    menuItem.setActionCommand("SELL_N");
                    menuItem.addActionListener(this);
                    menu.add(menuItem);
                }
            } else if (areAllPartsNotAmmo(parts)) {
                menuItem = new JMenuItem("Sell Single Part of This Type");
                menuItem.setActionCommand("SELL");
                menuItem.addActionListener(this);
                menu.add(menuItem);
                menuItem = new JMenuItem("Sell All Parts of This Type");
                menuItem.setActionCommand("SELL_ALL");
                menuItem.addActionListener(this);
                menu.add(menuItem);
                if (oneSelected && part.getQuantity() > 2) {
                    menuItem = new JMenuItem("Sell # Parts of This Type...");
                    menuItem.setActionCommand("SELL_N");
                    menuItem.addActionListener(this);
                    menu.add(menuItem);
                }
            } else {
                // when armor, ammo, and non-ammo only allow sell all
                menuItem = new JMenuItem("Sell All Parts of This Type");
                menuItem.setActionCommand("SELL_ALL");
                menuItem.addActionListener(this);
                menu.add(menuItem);
            }
            popup.add(menu);
        }

        // also add the ability to order one or many parts, if we have at least one part
        // selected
        if (rows.length > 0) {
            menu = new JMenu("Buy");
            menuItem = new JMenuItem("Buy Single Part of This Type");
            menuItem.setActionCommand("BUY");
            menuItem.addActionListener(this);
            menu.add(menuItem);

            if (oneSelected) {
                menuItem = new JMenuItem("Buy # Parts of This Type...");
                menuItem.setActionCommand("BUY_N");
                menuItem.addActionListener(this);
                menu.add(menuItem);
            }

            popup.add(menu);
        }

        if (oneSelected && part.needsFixing() && part.isPresent()) {
            menu = new JMenu("Repair Mode");
            for (WorkTime wt : WorkTime.DEFAULT_TIMES) {
                cbMenuItem = new JCheckBoxMenuItem(wt.name);
                if (part.getMode() == wt) {
                    cbMenuItem.setSelected(true);
                } else {
                    cbMenuItem.setActionCommand("CHANGE_MODE:" + wt.id);
                    cbMenuItem.addActionListener(this);
                }
                cbMenuItem.setEnabled(!part.isBeingWorkedOn());
                menu.add(cbMenuItem);
            }
            popup.add(menu);

            menuItem = new JMenuItem("Mass Repair");
            menuItem.setActionCommand("MRMS");
            menuItem.addActionListener(this);
            popup.add(menuItem);
        }
        if (areAllPartsInTransit(parts)) {
            menuItem = new JMenuItem("Cancel This Delivery");
            menuItem.setActionCommand("CANCEL_ORDER");
            menuItem.addActionListener(this);
            popup.add(menuItem);
        }
        menuItem = new JMenuItem("Export Parts");
        menuItem.addActionListener(ev -> gui.savePartsFile());
        menuItem.setEnabled(true);
        popup.add(menuItem);

        // remove from omnipods
        if (areAllPartsPodded(parts)) {
            menu = new JMenu("Remove Pod");
            menuItem = new JMenuItem("Remove Single Pod of This Type");
            menuItem.setActionCommand("DEPOD");
            menuItem.addActionListener(this);
            menu.add(menuItem);
            menuItem = new JMenuItem("Remove All Pods of This Type");
            menuItem.setActionCommand("DEPOD_ALL");
            menuItem.addActionListener(this);
            menu.add(menuItem);
            if (oneSelected && part.getQuantity() > 2) {
                menuItem = new JMenuItem("Remove # Pods of This Type...");
                menuItem.setActionCommand("DEPOD_N");
                menuItem.addActionListener(this);
                menu.add(menuItem);
            }
            popup.add(menu);
        }

        if (gui.getCampaign().isGM()) {
            // GM mode
            menu = new JMenu("GM Mode");
            if (areAllPartsInTransit(parts)) {
                menuItem = new JMenuItem("Deliver Part Now");
                menuItem.setActionCommand("ARRIVE");
                menuItem.addActionListener(this);
                menu.add(menuItem);
            }
            // add parts
            menuItem = new JMenuItem("Add Parts...");
            menuItem.setActionCommand("ADD_N");
            menuItem.addActionListener(this);
            menu.add(menuItem);
            // remove parts
            menuItem = new JMenuItem("Remove Part");
            menuItem.setActionCommand("REMOVE");
            menuItem.addActionListener(this);
            menu.add(menuItem);

            menuItem = new JMenuItem("Remove Parts...");
            menuItem.setActionCommand("REMOVE_N");
            menuItem.addActionListener(this);
            menu.add(menuItem);
            // set part quality
            menuItem = new JMenuItem("Set Quality...");
            menuItem.setActionCommand("SET_QUALITY");
            menuItem.addActionListener(this);
            menu.add(menuItem);
            // set part newness
            menuItem = new JMenuItem("Switch Newness");
            menuItem.setActionCommand("SWITCH_NEWNESS");
            menuItem.addActionListener(this);
            menu.add(menuItem);
            // end
            popup.addSeparator();
            popup.add(menu);
        }

        return Optional.of(popup);
    }
}
