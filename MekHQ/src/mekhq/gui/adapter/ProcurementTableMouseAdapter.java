package mekhq.gui.adapter;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.event.MouseInputAdapter;

import megamek.common.Entity;
import mekhq.MekHQ;
import mekhq.campaign.event.ProcurementEvent;
import mekhq.campaign.parts.Part;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.gui.CampaignGUI;
import mekhq.gui.model.ProcurementTableModel;

public class ProcurementTableMouseAdapter extends MouseInputAdapter {
    private CampaignGUI gui;

    public ProcurementTableMouseAdapter(CampaignGUI gui) {
        super();
        this.gui = gui;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        maybeShowPopup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        maybeShowPopup(e);
    }

    @SuppressWarnings("serial")
    private void maybeShowPopup(MouseEvent e) {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem menuItem;
        JMenu menu;
        final JTable table = (JTable) e.getSource();
        final ProcurementTableModel model = (ProcurementTableModel) table
                .getModel();
        if (table.getSelectedRow() < 0) {
            return;
        }
        if (table.getSelectedRowCount() == 0) {
            return;
        }
        final int row = table
                .convertRowIndexToModel(table.getSelectedRow());
        final int[] rows = table.getSelectedRows();
        final boolean oneSelected = table.getSelectedRowCount() == 1;
        if (e.isPopupTrigger()) {
            // **lets fill the pop up menu**//
            // GM mode
            menu = new JMenu("GM Mode");

            menuItem = new JMenuItem("Procure single item now");
            menuItem.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (row < 0) {
                        return;
                    }
                    if (oneSelected) {
                        IAcquisitionWork acquisition = model
                                .getAcquisition(row);
                        Object equipment = acquisition.getNewEquipment();
                        if (equipment instanceof Part) {
                            if (gui.getCampaign().buyPart(
                                    (Part) equipment,
                                    gui.getCampaign().calculatePartTransitTime(
                                            0))) {
                                gui.getCampaign()
                                        .addReport(
                                                "<font color='Green'><b>"
                                                        + acquisition
                                                                .getAcquisitionName()
                                                        + " found.</b></font>");
                                acquisition.decrementQuantity();
                            } else {
                                gui.getCampaign()
                                        .addReport(
                                                "<font color='red'><b>You cannot afford to purchase "
                                                        + acquisition
                                                                .getAcquisitionName()
                                                        + "</b></font>");
                            }
                        } else if (equipment instanceof Entity) {
                            if (gui.getCampaign().buyUnit(
                                    (Entity) equipment,
                                    gui.getCampaign().calculatePartTransitTime(
                                            0))) {
                                gui.getCampaign()
                                        .addReport(
                                                "<font color='Green'><b>"
                                                        + acquisition
                                                                .getAcquisitionName()
                                                        + " found.</b></font>");
                                acquisition.decrementQuantity();
                            } else {
                                gui.getCampaign()
                                        .addReport(
                                                "<font color='red'><b>You cannot afford to purchase "
                                                        + acquisition
                                                                .getAcquisitionName()
                                                        + "</b></font>");
                            }
                        }
                    } else {
                        for (int curRow : rows) {
                            if (curRow < 0) {
                                continue;
                            }
                            int row = table.convertRowIndexToModel(curRow);
                            IAcquisitionWork acquisition = model
                                    .getAcquisition(row);
                            Object equipment = acquisition
                                    .getNewEquipment();
                            if (equipment instanceof Part) {
                                if (gui.getCampaign()
                                        .buyPart(
                                                (Part) equipment,
                                                gui.getCampaign()
                                                        .calculatePartTransitTime(
                                                                0))) {
                                    gui.getCampaign()
                                            .addReport(
                                                    "<font color='Green'><b>"
                                                            + acquisition
                                                                    .getAcquisitionName()
                                                            + " found.</b></font>");
                                    acquisition.decrementQuantity();
                                } else {
                                    gui.getCampaign()
                                            .addReport(
                                                    "<font color='red'><b>You cannot afford to purchase "
                                                            + acquisition
                                                                    .getAcquisitionName()
                                                            + "</b></font>");
                                }
                            } else if (equipment instanceof Entity) {
                                if (gui.getCampaign()
                                        .buyUnit(
                                                (Entity) equipment,
                                                gui.getCampaign()
                                                        .calculatePartTransitTime(
                                                                0))) {
                                    gui.getCampaign()
                                            .addReport(
                                                    "<font color='Green'><b>"
                                                            + acquisition
                                                                    .getAcquisitionName()
                                                            + " found.</b></font>");
                                    acquisition.decrementQuantity();
                                } else {
                                    gui.getCampaign()
                                            .addReport(
                                                    "<font color='red'><b>You cannot afford to purchase "
                                                            + acquisition
                                                                    .getAcquisitionName()
                                                            + "</b></font>");
                                }
                            }
                        }
                    }
                }
            });
            menuItem.setEnabled(gui.getCampaign().isGM());
            menu.add(menuItem);
            menuItem = new JMenuItem("Procure all items now");
            menuItem.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (row < 0) {
                        return;
                    }
                    if (oneSelected) {
                        IAcquisitionWork acquisition = model
                                .getAcquisition(row);
                        boolean canAfford = true;
                        while (canAfford && acquisition.getQuantity() > 0) {
                            Object equipment = acquisition
                                    .getNewEquipment();
                            if (equipment instanceof Part) {
                                if (gui.getCampaign()
                                        .buyPart(
                                                (Part) equipment,
                                                gui.getCampaign()
                                                        .calculatePartTransitTime(
                                                                0))) {
                                    gui.getCampaign()
                                            .addReport(
                                                    "<font color='Green'><b>"
                                                            + acquisition
                                                                    .getAcquisitionName()
                                                            + " found.</b></font>");
                                    acquisition.decrementQuantity();
                                } else {
                                    gui.getCampaign()
                                            .addReport(
                                                    "<font color='red'><b>You cannot afford to purchase "
                                                            + acquisition
                                                                    .getAcquisitionName()
                                                            + "</b></font>");
                                    canAfford = false;
                                }
                            } else if (equipment instanceof Entity) {
                                if (gui.getCampaign()
                                        .buyUnit(
                                                (Entity) equipment,
                                                gui.getCampaign()
                                                        .calculatePartTransitTime(
                                                                0))) {
                                    gui.getCampaign()
                                            .addReport(
                                                    "<font color='Green'><b>"
                                                            + acquisition
                                                                    .getAcquisitionName()
                                                            + " found.</b></font>");
                                    acquisition.decrementQuantity();
                                } else {
                                    gui.getCampaign()
                                            .addReport(
                                                    "<font color='red'><b>You cannot afford to purchase "
                                                            + acquisition
                                                                    .getAcquisitionName()
                                                            + "</b></font>");
                                    canAfford = false;
                                }
                            }
                        }
                    } else {
                        for (int curRow : rows) {
                            if (curRow < 0) {
                                continue;
                            }
                            int row = table.convertRowIndexToModel(curRow);
                            IAcquisitionWork acquisition = model
                                    .getAcquisition(row);
                            boolean canAfford = true;
                            while (canAfford
                                    && acquisition.getQuantity() > 0) {
                                Object equipment = acquisition
                                        .getNewEquipment();
                                if (equipment instanceof Part) {
                                    if (gui.getCampaign()
                                            .buyPart(
                                                    (Part) equipment,
                                                    gui.getCampaign()
                                                            .calculatePartTransitTime(
                                                                    0))) {
                                        gui.getCampaign()
                                                .addReport(
                                                        "<font color='Green'><b>"
                                                                + acquisition
                                                                        .getAcquisitionName()
                                                                + " found.</b></font>");
                                        acquisition.decrementQuantity();
                                    } else {
                                        gui.getCampaign()
                                                .addReport(
                                                        "<font color='red'><b>You cannot afford to purchase "
                                                                + acquisition
                                                                        .getAcquisitionName()
                                                                + "</b></font>");
                                        canAfford = false;
                                    }
                                } else if (equipment instanceof Entity) {
                                    if (gui.getCampaign()
                                            .buyUnit(
                                                    (Entity) equipment,
                                                    gui.getCampaign()
                                                            .calculatePartTransitTime(
                                                                    0))) {
                                        gui.getCampaign()
                                                .addReport(
                                                        "<font color='Green'><b>"
                                                                + acquisition
                                                                        .getAcquisitionName()
                                                                + " found.</b></font>");
                                        acquisition.decrementQuantity();
                                    } else {
                                        gui.getCampaign()
                                                .addReport(
                                                        "<font color='red'><b>You cannot afford to purchase "
                                                                + acquisition
                                                                        .getAcquisitionName()
                                                                + "</b></font>");
                                        canAfford = false;
                                    }
                                }
                            }
                        }
                    }
                }
            });
            menuItem.setEnabled(gui.getCampaign().isGM());
            menu.add(menuItem);
            menuItem = new JMenuItem("Clear From the List");
            menuItem.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (row < 0) {
                        return;
                    }
                    if (oneSelected) {
                        IAcquisitionWork acquisition = model
                                .getAcquisition(row);
                        model.removeRow(row);
                        MekHQ.triggerEvent(new ProcurementEvent(acquisition));
                    } else {
                        for (int curRow : rows) {
                            if (curRow < 0) {
                                continue;
                            }
                            int row = table.convertRowIndexToModel(curRow);
                            IAcquisitionWork acquisition = model
                                    .getAcquisition(row);
                            model.removeRow(row);
                            MekHQ.triggerEvent(new ProcurementEvent(acquisition));
                        }
                    }
                }
            });
            menuItem.setEnabled(gui.getCampaign().isGM());
            menu.add(menuItem);
            // end
            popup.addSeparator();
            popup.add(menu);
            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }
}
