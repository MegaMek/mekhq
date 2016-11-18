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
import mekhq.gui.CampaignGUI;
import mekhq.gui.dialog.MassRepairSalvageDialog;
import mekhq.gui.dialog.PopupValueChoiceDialog;

public class PartsTableMouseAdapter extends MouseInputAdapter implements
        ActionListener {
    private CampaignGUI gui;

    public PartsTableMouseAdapter(CampaignGUI gui) {
        super();
        this.gui = gui;
    }

    public void actionPerformed(ActionEvent action) {
        String command = action.getActionCommand();
        int row = gui.getPartsTable().getSelectedRow();
        if (row < 0) {
            return;
        }
        Part selectedPart = gui.getPartsModel().getPartAt(gui.getPartsTable()
                .convertRowIndexToModel(row));
        int[] rows = gui.getPartsTable().getSelectedRows();
        Part[] parts = new Part[rows.length];
        for (int i = 0; i < rows.length; i++) {
            parts[i] = gui.getPartsModel().getPartAt(gui.getPartsTable()
                    .convertRowIndexToModel(rows[i]));
        }
        if (command.equalsIgnoreCase("SELL")) {
            for (Part p : parts) {
                if (null != p) {
                    gui.getCampaign().sellPart(p, 1);
                }
            }
            gui.refreshPartsList();
            gui.refreshTaskList();
            gui.refreshAcquireList();
            gui.refreshReport();
            gui.refreshFunds();
            gui.refreshFinancialTransactions();
            gui.refreshOverview();
            gui.filterTasks();
        } else if (command.equalsIgnoreCase("SELL_ALL")) {
            for (Part p : parts) {
                if (null != p) {
                    if (p instanceof AmmoStorage) {
                        gui.getCampaign().sellAmmo((AmmoStorage) p,
                                ((AmmoStorage) p).getShots());
                    } 
                    else if(p instanceof Armor) {
                    	gui.getCampaign().sellArmor((Armor)p, ((Armor)p).getAmount());
                    }
                    else {
                        gui.getCampaign().sellPart(p, p.getQuantity());
                    }
                }
            }
            gui.refreshPartsList();
            gui.refreshTaskList();
            gui.refreshAcquireList();
            gui.refreshReport();
            gui.refreshFunds();
            gui.refreshFinancialTransactions();
            gui.refreshOverview();
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
                        gui.getFrame(), true, "Sell How Many "
                                + selectedPart.getName() + "s?", 1, 1, n);
                pvcd.setVisible(true);
                if (pvcd.getValue() < 0) {
                    return;
                }
                int q = pvcd.getValue();
                gui.getCampaign().sellPart(selectedPart, q);
            }
            gui.refreshFinancialTransactions();
            gui.refreshPartsList();
            gui.refreshTaskList();
            gui.refreshAcquireList();
            gui.refreshReport();
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("CANCEL_ORDER")) {
            double refund = gui.getCampaign().getCampaignOptions()
                    .GetCanceledOrderReimbursement();
            long refundAmount = 0;
            for (Part p : parts) {
                if (null != p) {
                    refundAmount += (refund * p.getStickerPrice() * p
                            .getQuantity());
                    gui.getCampaign().removePart(p);
                }
            }
            gui.getCampaign().getFinances().credit(refundAmount,
                    Transaction.C_EQUIP,
                    "refund for cancelled equipmemt sale",
                    gui.getCampaign().getDate());
            gui.refreshFinancialTransactions();
            gui.refreshPartsList();
            gui.refreshTaskList();
            gui.refreshAcquireList();
            gui.refreshReport();
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("ARRIVE")) {
            for (Part p : parts) {
                if (null != p) {
                    gui.getCampaign().arrivePart(p);
                }
            }
            gui.refreshPartsList();
            gui.refreshTaskList();
            gui.refreshAcquireList();
            gui.refreshReport();
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("REMOVE")) {
            for (Part p : parts) {
                if (null != p) {
                    gui.getCampaign().removePart(p);
                }
            }
            gui.refreshPartsList();
            gui.refreshTaskList();
            gui.refreshAcquireList();
            gui.refreshReport();
            gui.refreshOverview();
        } else if (command.contains("SET_QUALITY")) {
            int q = -1;
            boolean reverse = gui.getCampaign().getCampaignOptions().reverseQualityNames();
            Object[] possibilities = { Part.getQualityName(Part.QUALITY_A, reverse), 
            		Part.getQualityName(Part.QUALITY_B, reverse), 
            		Part.getQualityName(Part.QUALITY_C, reverse),
            		Part.getQualityName(Part.QUALITY_D, reverse),
            		Part.getQualityName(Part.QUALITY_E, reverse),
            		Part.getQualityName(Part.QUALITY_F, reverse) };
            String quality = (String) JOptionPane.showInputDialog(gui.getFrame(),
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
            gui.refreshPartsList();
            gui.refreshOverview();
        } else if (command.contains("MASS_REPAIR")) {
            MassRepairSalvageDialog dlg = new MassRepairSalvageDialog(gui.getFrame(), true, gui, MassRepairSalvageDialog.MODE.WAREHOUSE);
            dlg.setVisible(true);
            
            gui.refreshPartsList();
            gui.refreshOverview();
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
            if (gui.getPartsTable().getSelectedRowCount() == 0) {
                return;
            }
            int[] rows = gui.getPartsTable().getSelectedRows();
            JMenuItem menuItem = null;
            JMenu menu = null;
            JCheckBoxMenuItem cbMenuItem = null;
            Part[] parts = new Part[rows.length];
            boolean oneSelected = false;
            for (int i = 0; i < rows.length; i++) {
                parts[i] = gui.getPartsModel().getPartAt(gui.getPartsTable()
                        .convertRowIndexToModel(rows[i]));
            }
            Part part = null;
            if (parts.length == 1) {
                oneSelected = true;
                part = parts[0];
            }
            // **lets fill the pop up menu**//
            // sell part
            if (gui.getCampaign().getCampaignOptions().canSellParts()
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
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    gui.miExportPartsActionPerformed(evt);
                }
            });
            menuItem.setEnabled(true);
            popup.add(menuItem);
            // GM mode
            menu = new JMenu("GM Mode");
            if (areAllPartsInTransit(parts)) {
                menuItem = new JMenuItem("Deliver Part Now");
                menuItem.setActionCommand("ARRIVE");
                menuItem.addActionListener(this);
                menuItem.setEnabled(gui.getCampaign().isGM());
                menu.add(menuItem);
            }
            // remove part
            menuItem = new JMenuItem("Remove Part");
            menuItem.setActionCommand("REMOVE");
            menuItem.addActionListener(this);
            menuItem.setEnabled(gui.getCampaign().isGM());
            menu.add(menuItem);
            // set part quality
            menuItem = new JMenuItem("Set Quality...");
            menuItem.setActionCommand("SET_QUALITY");
            menuItem.addActionListener(this);
            menuItem.setEnabled(gui.getCampaign().isGM());
            menu.add(menuItem);
            // end
            popup.addSeparator();
            popup.add(menu);
            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }
}