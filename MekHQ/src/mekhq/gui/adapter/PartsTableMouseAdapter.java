package mekhq.gui.adapter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.event.MouseInputAdapter;

import megamek.common.TargetRoll;
import mekhq.MekHQ;
import mekhq.campaign.event.PartChangedEvent;
import mekhq.campaign.event.PartModeChangedEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.Transaction;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.work.WorkTime;
import mekhq.gui.CampaignGUI;
import mekhq.gui.dialog.MassRepairSalvageDialog;
import mekhq.gui.dialog.PopupValueChoiceDialog;
import mekhq.gui.model.PartsTableModel;
import mekhq.service.MassRepairMassSalvageMode;

public class PartsTableMouseAdapter extends MouseInputAdapter implements ActionListener {

    private CampaignGUI gui;
    private JTable partsTable;
    private PartsTableModel partsModel;

    public PartsTableMouseAdapter(CampaignGUI gui, JTable partsTable, PartsTableModel partsModel) {
        super();
        this.gui = gui;
        this.partsTable = partsTable;
        this.partsModel = partsModel;
    }

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
                PopupValueChoiceDialog pvcd = new PopupValueChoiceDialog(gui.getFrame(), true,
                        "Sell How Many " + selectedPart.getName() + "s?", 1, 1, n);
                pvcd.setVisible(true);
                if (pvcd.getValue() < 0) {
                    return;
                }
                int q = pvcd.getValue();
                gui.getCampaign().getQuartermaster().sellPart(selectedPart, q);
            }
        } else if (command.equalsIgnoreCase("CANCEL_ORDER")) {
            double refund = gui.getCampaign().getCampaignOptions().GetCanceledOrderReimbursement();
            Money refundAmount = Money.zero();
            for (Part p : parts) {
                if (null != p) {
                    refundAmount = refundAmount.plus(p.getStickerPrice().multipliedBy(p.getQuantity()).multipliedBy(refund));
                    gui.getCampaign().getWarehouse().removePart(p);
                }
            }
            gui.getCampaign().getFinances().credit(refundAmount, Transaction.C_EQUIP,
                    "refund for cancelled equipment sale", gui.getCampaign().getLocalDate());
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
        } else if (command.contains("SET_QUALITY")) {
            int q = -1;
            boolean reverse = gui.getCampaign().getCampaignOptions().reverseQualityNames();
            Object[] possibilities = { Part.getQualityName(Part.QUALITY_A, reverse),
                    Part.getQualityName(Part.QUALITY_B, reverse), Part.getQualityName(Part.QUALITY_C, reverse),
                    Part.getQualityName(Part.QUALITY_D, reverse), Part.getQualityName(Part.QUALITY_E, reverse),
                    Part.getQualityName(Part.QUALITY_F, reverse) };
            String quality = (String) JOptionPane.showInputDialog(gui.getFrame(), "Choose the new quality level",
                    "Set Quality", JOptionPane.PLAIN_MESSAGE, null, possibilities,
                    Part.getQualityName(Part.QUALITY_D, reverse));
            for (int i = 0; i < possibilities.length; i++) {
                if (possibilities[i].equals(quality)) {
                    q = i;
                    break;
                }
            }
            if (q != -1) {
                for (Part p : parts) {
                    if (p != null) {
                        p.setQuality(q);
                        MekHQ.triggerEvent(new PartChangedEvent(p));
                    }
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
        } else if (command.contains("MASS_REPAIR")) {
            MassRepairSalvageDialog dlg = new MassRepairSalvageDialog(gui.getFrame(), true, gui,
                    MassRepairMassSalvageMode.WAREHOUSE);
            dlg.setVisible(true);
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
                PopupValueChoiceDialog pvcd = new PopupValueChoiceDialog(gui.getFrame(), true,
                        "Remove How Many Pods" + selectedPart.getName() + "s?", 1, 1, n);
                pvcd.setVisible(true);
                if (pvcd.getValue() < 0) {
                    return;
                }
                int q = pvcd.getValue();
                gui.getCampaign().getQuartermaster().depodPart(selectedPart, q);
            }
        } else if(command.equalsIgnoreCase("BUY")) {
            for (Part p : parts) {
                if(null != p) {
                    gui.getCampaign().getShoppingList().addShoppingItem(p.getAcquisitionWork(), 1, gui.getCampaign());
                }
            }
        } else if(command.equalsIgnoreCase("BUY_N")) {
            if(null != selectedPart) {
                PopupValueChoiceDialog pvcd = new PopupValueChoiceDialog(gui.getFrame(), true,
                        "Buy How Much " + selectedPart.getName(), 1, 1);
                pvcd.setVisible(true);
                if(pvcd.getValue() < 1) {
                    return;
                }
                int q = pvcd.getValue();
                gui.getCampaign().getShoppingList().addShoppingItem(selectedPart.getAcquisitionWork(), q, gui.getCampaign());
            }
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

    public boolean areAllPartsPodded(Part[] parts) {
        for (Part p : parts) {
            if (!p.isOmniPodded()) {
                return false;
            }
        }
        return true;
    }

    private void maybeShowPopup(MouseEvent e) {
        JPopupMenu popup = new JPopupMenu();
        if (e.isPopupTrigger()) {
            if (partsTable.getSelectedRowCount() == 0) {
                return;
            }
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
            if (gui.getCampaign().getCampaignOptions().canSellParts() && areAllPartsPresent(parts)) {
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

            // also add the ability to order one or many parts, if we have at least one part selected
            if(rows.length > 0) {
                menu = new JMenu("Buy");
                menuItem = new JMenuItem("Buy Single Part of This Type");
                menuItem.setActionCommand("BUY");
                menuItem.addActionListener(this);
                menu.add(menuItem);

                if(oneSelected) {
                    menuItem = new JMenuItem ("Buy # Parts of This Type...");
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
            menuItem.addActionListener(ev -> gui.miExportPartsActionPerformed(ev));
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
