package mekhq.gui.adapter;

import java.util.Optional;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;

import megamek.common.Entity;
import mekhq.MekHQ;
import mekhq.campaign.event.ProcurementEvent;
import mekhq.campaign.parts.Part;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.gui.CampaignGUI;
import mekhq.gui.model.ProcurementTableModel;

public class ProcurementTableMouseAdapter extends JPopupMenuAdapter {
    private final CampaignGUI gui;
    private final JTable table;
    private final ProcurementTableModel model;

    protected ProcurementTableMouseAdapter(CampaignGUI gui, JTable table, ProcurementTableModel model) {
        this.gui = gui;
        this.table = table;
        this.model = model;
    }

    public static void connect(CampaignGUI gui, JTable table, ProcurementTableModel model) {
        new ProcurementTableMouseAdapter(gui, table, model)
                .connect(table);
    }

    @Override
    protected Optional<JPopupMenu> createPopupMenu() {
        // GM Only (for now)
        if ((table.getSelectedRowCount() == 0) || !gui.getCampaign().isGM()) {
            return Optional.empty();
        }

        JPopupMenu popup = new JPopupMenu();
        JMenuItem menuItem;
        JMenu menu;
        final int row = table.convertRowIndexToModel(table.getSelectedRow());
        final int[] rows = table.getSelectedRows();
        final boolean oneSelected = table.getSelectedRowCount() == 1;
        
        // **lets fill the pop up menu**//
        // GM mode
        menu = new JMenu("GM Mode");

        menuItem = new JMenuItem("Procure single item now");
        menuItem.addActionListener(evt -> {
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
        });
        menu.add(menuItem);
        menuItem = new JMenuItem("Procure all items now");
        menuItem.addActionListener(evt -> {
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
        });
        menu.add(menuItem);
        menuItem = new JMenuItem("Clear From the List");
        menuItem.addActionListener(evt -> {
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
        });
        menu.add(menuItem);
        // end
        popup.add(menu);

        return Optional.of(popup);
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
