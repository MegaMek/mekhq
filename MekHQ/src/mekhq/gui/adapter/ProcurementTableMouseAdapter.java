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
        
        // GM Only (for now)
        if (e.isPopupTrigger() && gui.getCampaign().isGM()) {
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
                        model.getAcquisition(row)
                                .ifPresent(ProcurementTableMouseAdapter.this::tryProcureOneItem);
                    } else {
                        for (int curRow : rows) {
                            if (curRow < 0) {
                                continue;
                            }
                            model.getAcquisition(table.convertRowIndexToModel(curRow))
                                    .ifPresent(ProcurementTableMouseAdapter.this::tryProcureOneItem);
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
                        model.getAcquisition(row)
                                .ifPresent(ProcurementTableMouseAdapter.this::procureAllItems);
                    } else {
                        for (int curRow : rows) {
                            if (curRow < 0) {
                                continue;
                            }
                            model.getAcquisition(table.convertRowIndexToModel(curRow))
                                    .ifPresent(ProcurementTableMouseAdapter.this::procureAllItems);
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
                        model.getAcquisition(row).ifPresent(a -> {
                            model.removeRow(row);
                            MekHQ.triggerEvent(new ProcurementEvent(a));
                        });
                    } else {
                        for (int curRow : rows) {
                            if (curRow < 0) {
                                continue;
                            }
                            model.getAcquisition(table.convertRowIndexToModel(curRow))
                                    .ifPresent(a -> {
                                        model.removeRow(row);
                                        MekHQ.triggerEvent(new ProcurementEvent(a));
                                    });
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

    private void procureAllItems(IAcquisitionWork acquisition) {
        while (acquisition.getQuantity() > 0) {
            if (!tryProcureOneItem(acquisition)) {
                break;
            }
        }
    }

    private boolean tryProcureOneItem(IAcquisitionWork acquisition) {
        Object equipment = acquisition.getNewEquipment();
        int transitTime = gui.getCampaign().calculatePartTransitTime(0);
        if (equipment instanceof Part) {
            if (gui.getCampaign().getQuartermaster().buyPart((Part) equipment, transitTime)) {
                reportAcquisitionSuccess(acquisition);
                acquisition.decrementQuantity();
                return true;
            } else {
                reportAcquisitionFailure(acquisition);
            }
        } else if (equipment instanceof Entity) {
            if (gui.getCampaign().getQuartermaster().buyUnit((Entity) equipment, transitTime)) {
                reportAcquisitionSuccess(acquisition);
                acquisition.decrementQuantity();
                return true;
            } else {
                reportAcquisitionFailure(acquisition);
            }
        }

        return false;
    }

    private void reportAcquisitionSuccess(IAcquisitionWork acquisition) {
        gui.getCampaign()
                .addReport(String.format("<font color='Green'><b>Procured %s</b></font>",
                        acquisition.getAcquisitionName()));
    }

    private void reportAcquisitionFailure(IAcquisitionWork acquisition) {
        gui.getCampaign()
                .addReport(String.format("<font color='red'><b>You cannot afford to purchase %s</b></font>",
                        acquisition.getAcquisitionName()));
    }
}
