package mekhq.gui.adapter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.event.MouseInputAdapter;

import mekhq.campaign.finances.Transaction;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.work.WorkTime;
import mekhq.gui.WarehouseTab;
import mekhq.gui.dialog.MassRepairSalvageDialog;
import mekhq.gui.dialog.PopupValueChoiceDialog;

public class PartsTableMouseAdapter extends MouseInputAdapter implements
        ActionListener {
    private WarehouseTab warehouseTab;

    public PartsTableMouseAdapter(WarehouseTab warehouseTab) {
        super();
        this.warehouseTab = warehouseTab;
    }

    public void actionPerformed(ActionEvent action) {
        String command = action.getActionCommand();
        int row = warehouseTab.getPartsTable().getSelectedRow();
        if (row < 0) {
            return;
        }
        Part selectedPart = warehouseTab.getPartsModel().getPartAt(warehouseTab.getPartsTable()
                .convertRowIndexToModel(row));
        int[] rows = warehouseTab.getPartsTable().getSelectedRows();
        Part[] parts = new Part[rows.length];
        for (int i = 0; i < rows.length; i++) {
            parts[i] = warehouseTab.getPartsModel().getPartAt(warehouseTab.getPartsTable()
                    .convertRowIndexToModel(rows[i]));
        }
        if (command.equalsIgnoreCase("SELL")) {
            for (Part p : parts) {
                if (null != p) {
                    warehouseTab.getCampaign().sellPart(p, 1);
                }
            }
            warehouseTab.refreshPartsList();
            warehouseTab.getCampaignGui().refreshTaskList();
            warehouseTab.getCampaignGui().refreshAcquireList();
            warehouseTab.getCampaignGui().refreshReport();
            warehouseTab.getCampaignGui().refreshFunds();
            warehouseTab.getCampaignGui().refreshFinancialTransactions();
            warehouseTab.getCampaignGui().refreshOverview();
            warehouseTab.getCampaignGui().filterTasks();
        } else if (command.equalsIgnoreCase("SELL_ALL")) {
            for (Part p : parts) {
                if (null != p) {
                    if (p instanceof AmmoStorage) {
                        warehouseTab.getCampaign().sellAmmo((AmmoStorage) p,
                                ((AmmoStorage) p).getShots());
                    } 
                    else if(p instanceof Armor) {
                    	warehouseTab.getCampaign().sellArmor((Armor)p, ((Armor)p).getAmount());
                    }
                    else {
                        warehouseTab.getCampaign().sellPart(p, p.getQuantity());
                    }
                }
            }
            warehouseTab.refreshPartsList();
            warehouseTab.getCampaignGui().refreshTaskList();
            warehouseTab.getCampaignGui().refreshAcquireList();
            warehouseTab.getCampaignGui().refreshReport();
            warehouseTab.getCampaignGui().refreshFunds();
            warehouseTab.getCampaignGui().refreshFinancialTransactions();
            warehouseTab.getCampaignGui().refreshOverview();
        } else if (command.equalsIgnoreCase("SELL_N")) {
            if (null != selectedPart) {
                int n = selectedPart.getQuantity();
                if (selectedPart instanceof AmmoStorage) {
                    n = ((AmmoStorage) selectedPart).getShots();
                }
                if (selectedPart instanceof Armor) {
                    n = ((Armor) selectedPart).getAmount();
                }
                PopupValueChoiceDialog pvcd = new PopupValueChoiceDialog(
                        warehouseTab.getFrame(), true, "Sell How Many "
                                + selectedPart.getName() + "s?", 1, 1, n);
                pvcd.setVisible(true);
                if (pvcd.getValue() < 0) {
                    return;
                }
                int q = pvcd.getValue();
                warehouseTab.getCampaign().sellPart(selectedPart, q);
            }
            warehouseTab.getCampaignGui().refreshFinancialTransactions();
            warehouseTab.refreshPartsList();
            warehouseTab.getCampaignGui().refreshTaskList();
            warehouseTab.getCampaignGui().refreshAcquireList();
            warehouseTab.getCampaignGui().refreshReport();
            warehouseTab.getCampaignGui().refreshOverview();
        } else if (command.equalsIgnoreCase("CANCEL_ORDER")) {
            double refund = warehouseTab.getCampaign().getCampaignOptions()
                    .GetCanceledOrderReimbursement();
            long refundAmount = 0;
            for (Part p : parts) {
                if (null != p) {
                    refundAmount += (refund * p.getStickerPrice() * p
                            .getQuantity());
                    warehouseTab.getCampaign().removePart(p);
                }
            }
            warehouseTab.getCampaign().getFinances().credit(refundAmount,
                    Transaction.C_EQUIP,
                    "refund for cancelled equipmemt sale",
                    warehouseTab.getCampaign().getDate());
            warehouseTab.getCampaignGui().refreshFinancialTransactions();
            warehouseTab.refreshPartsList();
            warehouseTab.getCampaignGui().refreshTaskList();
            warehouseTab.getCampaignGui().refreshAcquireList();
            warehouseTab.getCampaignGui().refreshReport();
            warehouseTab.getCampaignGui().refreshOverview();
        } else if (command.equalsIgnoreCase("ARRIVE")) {
            for (Part p : parts) {
                if (null != p) {
                    warehouseTab.getCampaign().arrivePart(p);
                }
            }
            warehouseTab.refreshPartsList();
            warehouseTab.getCampaignGui().refreshTaskList();
            warehouseTab.getCampaignGui().refreshAcquireList();
            warehouseTab.getCampaignGui().refreshReport();
            warehouseTab.getCampaignGui().refreshOverview();
        } else if (command.equalsIgnoreCase("REMOVE")) {
            for (Part p : parts) {
                if (null != p) {
                    warehouseTab.getCampaign().removePart(p);
                }
            }
            warehouseTab.refreshPartsList();
            warehouseTab.getCampaignGui().refreshTaskList();
            warehouseTab.getCampaignGui().refreshAcquireList();
            warehouseTab.getCampaignGui().refreshReport();
            warehouseTab.getCampaignGui().refreshOverview();
        } else if (command.contains("SET_QUALITY")) {
            int q = -1;
            boolean reverse = warehouseTab.getCampaign().getCampaignOptions().reverseQualityNames();
            Object[] possibilities = { Part.getQualityName(Part.QUALITY_A, reverse), 
            		Part.getQualityName(Part.QUALITY_B, reverse), 
            		Part.getQualityName(Part.QUALITY_C, reverse),
            		Part.getQualityName(Part.QUALITY_D, reverse),
            		Part.getQualityName(Part.QUALITY_E, reverse),
            		Part.getQualityName(Part.QUALITY_F, reverse) };
            String quality = (String) JOptionPane.showInputDialog(warehouseTab.getFrame(),
                    "Choose the new quality level", "Set Quality",
                    JOptionPane.PLAIN_MESSAGE, null, possibilities, Part.getQualityName(Part.QUALITY_D, reverse));
            for(int i = 0; i < possibilities.length; i++) {
            	if(possibilities[i].equals(quality)) {
            		q = i;
            		break;
            	}
            }
            if (q != -1) {
                for (Part p : parts) {
                    if (p != null) {
                        p.setQuality(q);
                    }
                }
            }
        } else if (command.contains("CHANGE_MODE")) {
            String sel = command.split(":")[1];
            selectedPart.setMode(WorkTime.of(sel));
            warehouseTab.refreshPartsList();
            warehouseTab.getCampaignGui().refreshOverview();
        } else if (command.contains("MASS_REPAIR")) {
            MassRepairSalvageDialog dlg = new MassRepairSalvageDialog(warehouseTab.getFrame(),
            		true, warehouseTab.getCampaignGui(), MassRepairSalvageDialog.MODE.WAREHOUSE);
            dlg.setVisible(true);
            
            warehouseTab.refreshPartsList();
            warehouseTab.getCampaignGui().refreshOverview();
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        maybeShowPopup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        maybeShowPopup(e);
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

    private void maybeShowPopup(MouseEvent e) {
        JPopupMenu popup = new JPopupMenu();
        if (e.isPopupTrigger()) {
            if (warehouseTab.getPartsTable().getSelectedRowCount() == 0) {
                return;
            }
            int[] rows = warehouseTab.getPartsTable().getSelectedRows();
            JMenuItem menuItem = null;
            JMenu menu = null;
            JCheckBoxMenuItem cbMenuItem = null;
            Part[] parts = new Part[rows.length];
            boolean oneSelected = false;
            for (int i = 0; i < rows.length; i++) {
                parts[i] = warehouseTab.getPartsModel().getPartAt(warehouseTab.getPartsTable()
                        .convertRowIndexToModel(rows[i]));
            }
            Part part = null;
            if (parts.length == 1) {
                oneSelected = true;
                part = parts[0];
            }
            // **lets fill the pop up menu**//
            // sell part
            if (warehouseTab.getCampaign().getCampaignOptions().canSellParts()
                    && areAllPartsPresent(parts)) {
                menu = new JMenu("Sell");
                if (areAllPartsAmmo(parts)) {
                    menuItem = new JMenuItem("Sell All Ammo of This Type");
                    menuItem.setActionCommand("SELL_ALL");
                    menuItem.addActionListener(this);
                    menu.add(menuItem);
                    if (oneSelected && ((AmmoStorage) part).getShots() > 1) {
                        menuItem = new JMenuItem(
                                "Sell # Ammo of This Type...");
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
                        menuItem = new JMenuItem(
                                "Sell # Armor points of This Type...");
                        menuItem.setActionCommand("SELL_N");
                        menuItem.addActionListener(this);
                        menu.add(menuItem);
                    }
                } else if (areAllPartsNotAmmo(parts)) {
                    menuItem = new JMenuItem(
                            "Sell Single Part of This Type");
                    menuItem.setActionCommand("SELL");
                    menuItem.addActionListener(this);
                    menu.add(menuItem);
                    menuItem = new JMenuItem("Sell All Parts of This Type");
                    menuItem.setActionCommand("SELL_ALL");
                    menuItem.addActionListener(this);
                    menu.add(menuItem);
                    if (oneSelected && part.getQuantity() > 2) {
                        menuItem = new JMenuItem(
                                "Sell # Parts of This Type...");
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
            if (oneSelected && part.needsFixing() && part.isPresent()) {
                menu = new JMenu("Repair Mode");
                for(WorkTime wt : WorkTime.DEFAULT_TIMES) {
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
                menuItem.setActionCommand("MASS_REPAIR");
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
            menuItem.addActionListener(ev -> warehouseTab.getCampaignGui().miExportPartsActionPerformed(ev));
            menuItem.setEnabled(true);
            popup.add(menuItem);
            // GM mode
            menu = new JMenu("GM Mode");
            if (areAllPartsInTransit(parts)) {
                menuItem = new JMenuItem("Deliver Part Now");
                menuItem.setActionCommand("ARRIVE");
                menuItem.addActionListener(this);
                menuItem.setEnabled(warehouseTab.getCampaign().isGM());
                menu.add(menuItem);
            }
            // remove part
            menuItem = new JMenuItem("Remove Part");
            menuItem.setActionCommand("REMOVE");
            menuItem.addActionListener(this);
            menuItem.setEnabled(warehouseTab.getCampaign().isGM());
            menu.add(menuItem);
            // set part quality
            menuItem = new JMenuItem("Set Quality...");
            menuItem.setActionCommand("SET_QUALITY");
            menuItem.addActionListener(this);
            menuItem.setEnabled(warehouseTab.getCampaign().isGM());
            menu.add(menuItem);
            // end
            popup.addSeparator();
            popup.add(menu);
            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }
}