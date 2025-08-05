/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ResourceBundle;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import megamek.client.ui.preferences.JComboBoxPreference;
import megamek.client.ui.preferences.JTablePreference;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.MiscType;
import megamek.common.TargetRoll;
import megamek.common.WeaponType;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.*;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.gui.CampaignGUI;
import mekhq.gui.model.PartsStoreModel;
import mekhq.gui.model.PartsStoreModel.PartProxy;
import mekhq.gui.sorter.PartsDetailSorter;
import mekhq.gui.utilities.JScrollPaneWithSpeed;

/**
 * @author Taharqa
 */
public class PartsStoreDialog extends JDialog {
    private static final MMLogger logger = MMLogger.create(PartsStoreDialog.class);

    // region Variable Declarations
    // parts filter groups
    private static final int SG_ALL = 0;
    private static final int SG_ARMOR = 1;
    private static final int SG_SYSTEM = 2;
    private static final int SG_EQUIP = 3;
    private static final int SG_LOC = 4;
    private static final int SG_WEAP = 5;
    private static final int SG_AMMO = 6;
    private static final int SG_MISC = 7;
    private static final int SG_ENGINE = 8;
    private static final int SG_GYRO = 9;
    private static final int SG_ACT = 10;
    private static final int SG_COCKPIT = 11;
    private static final int SG_BA_SUIT = 12;
    private static final int SG_OMNI_POD = 13;
    private static final int SG_NUM = 14;

    private Campaign campaign;
    private CampaignGUI campaignGUI;
    private PartsStoreModel partsModel;
    private TableRowSorter<PartsStoreModel> partsSorter;
    private boolean addToCampaign;
    private Part selectedPart;

    private JTable partsTable;
    private JTextField txtFilter;
    private JComboBox<String> choiceParts;
    private JCheckBox hideImpossible;

    private final transient ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.PartsStoreDialog",
          MekHQ.getMHQOptions().getLocale());
    // endregion Variable Declarations

    /** Creates new form PartsStoreDialog */
    public PartsStoreDialog(boolean modal, CampaignGUI gui) {
        this(gui.getFrame(), modal, gui, gui.getCampaign(), true);
    }

    public PartsStoreDialog(final JFrame frame, final boolean modal, final CampaignGUI gui, final Campaign campaign,
          final boolean add) {
        super(frame, modal);
        this.campaignGUI = gui;
        this.campaign = campaign;
        this.addToCampaign = add;
        partsModel = new PartsStoreModel(gui, campaign.getPartsStore().getInventory());
        initComponents();
        filterParts();
        setLocationRelativeTo(frame);
        selectedPart = null;
        setUserPreferences();
    }

    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form");
        setTitle(resourceMap.getString("Form.title"));

        getContentPane().setLayout(new BorderLayout());

        partsTable = new JTable(partsModel);
        partsTable.setName("partsTable");
        partsSorter = new TableRowSorter<>(partsModel);
        partsSorter.setComparator(PartsStoreModel.COL_DETAIL, new PartsDetailSorter());
        partsTable.setRowSorter(partsSorter);
        TableColumn column;
        for (int i = 0; i < PartsStoreModel.N_COL; i++) {
            column = partsTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(partsModel.getColumnWidth(i));
            column.setCellRenderer(partsModel.getRenderer());
        }
        partsTable.setIntercellSpacing(new Dimension(0, 0));
        partsTable.setShowGrid(false);
        JScrollPane scrollPartsTable = new JScrollPaneWithSpeed();
        scrollPartsTable.setName("scrollPartsTable");
        scrollPartsTable.setViewportView(partsTable);
        getContentPane().add(scrollPartsTable, BorderLayout.CENTER);

        GridBagConstraints c = new GridBagConstraints();
        JPanel panFilter = new JPanel();
        JLabel lblPartsChoice = new JLabel(resourceMap.getString("lblPartsChoice.text"));
        DefaultComboBoxModel<String> partsGroupModel = new DefaultComboBoxModel<>();
        for (int i = 0; i < SG_NUM; i++) {
            partsGroupModel.addElement(getPartsGroupName(i));
        }
        choiceParts = new JComboBox<>(partsGroupModel);
        choiceParts.setName("choiceParts");
        choiceParts.setSelectedIndex(0);
        choiceParts.addActionListener(evt -> filterParts());
        panFilter.setLayout(new GridBagLayout());
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 5, 5, 5);
        panFilter.add(lblPartsChoice, c);
        c.gridx = 1;
        c.weightx = 1.0;
        panFilter.add(choiceParts, c);

        JLabel lblFilter = new JLabel(resourceMap.getString("lblFilter.text"));
        lblFilter.setName("lblFilter");
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0.0;
        panFilter.add(lblFilter, c);
        txtFilter = new JTextField();
        txtFilter.setText("");
        txtFilter.setMinimumSize(new Dimension(200, 28));
        txtFilter.setName("txtFilter");
        txtFilter.setPreferredSize(new Dimension(200, 28));
        txtFilter.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                filterParts();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                filterParts();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterParts();
            }
        });
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 1.0;
        panFilter.add(txtFilter, c);

        hideImpossible = new JCheckBox(resourceMap.getString("hideImpossible.text"));
        hideImpossible.setName("hideImpossible");
        hideImpossible.addActionListener(e -> filterParts());
        c.gridx = 2;
        panFilter.add(hideImpossible, c);

        getContentPane().add(panFilter, BorderLayout.PAGE_START);

        JPanel panButtons = new JPanel();
        JButton btnAdd;
        JButton btnClose;
        if (addToCampaign) {
            panButtons.setLayout(new GridBagLayout());

            // region Buy
            JButton btnBuy = new JButton(resourceMap.getString("btnBuy.text"));
            btnBuy.addActionListener(evt -> {
                if (partsTable.getSelectedRowCount() > 0) {
                    int[] selectedRow = partsTable.getSelectedRows();
                    for (int i : selectedRow) {
                        PartProxy partProxy = partsModel.getPartProxyAt(partsTable.convertRowIndexToModel(i));
                        addPart(true, partProxy.getPart(), 1);
                        partProxy.updateTargetAndInventories();
                        partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i),
                              PartsStoreModel.COL_TARGET);
                        partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i),
                              PartsStoreModel.COL_TRANSIT);
                        partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i),
                              PartsStoreModel.COL_SUPPLY);
                        partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i),
                              PartsStoreModel.COL_QUEUE);
                    }
                }
            });
            panButtons.add(btnBuy, new GridBagConstraints());
            // endregion Buy

            // region Buy Bulk
            JButton btnBuyBulk = new JButton(resourceMap.getString("btnBuyBulk.text"));
            btnBuyBulk.addActionListener(evt -> {
                if (partsTable.getSelectedRowCount() > 0) {
                    int[] selectedRow = partsTable.getSelectedRows();
                    for (int i : selectedRow) {
                        PartProxy partProxy = partsModel.getPartProxyAt(partsTable.convertRowIndexToModel(i));
                        int quantity = 1;
                        PopupValueChoiceDialog pcd = new PopupValueChoiceDialog(campaignGUI.getFrame(),
                              true,
                              "How Many " + partProxy.getName() + '?',
                              quantity,
                              1,
                              CampaignGUI.MAX_QUANTITY_SPINNER);
                        pcd.setVisible(true);
                        quantity = pcd.getValue();

                        if (quantity > 0) {
                            addPart(true, partProxy.getPart(), quantity);
                            partProxy.updateTargetAndInventories();
                            partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i),
                                  PartsStoreModel.COL_TARGET);
                            partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i),
                                  PartsStoreModel.COL_TRANSIT);
                            partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i),
                                  PartsStoreModel.COL_SUPPLY);
                            partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i),
                                  PartsStoreModel.COL_QUEUE);
                        }
                    }
                }
            });
            panButtons.add(btnBuyBulk, new GridBagConstraints());
            // endregion Buy Bulk

            // region Add
            btnAdd = new JButton(resourceMap.getString("btnGMAdd.text"));
            btnAdd.addActionListener(evt -> {
                if (partsTable.getSelectedRowCount() > 0) {
                    int[] selectedRow = partsTable.getSelectedRows();
                    for (int i : selectedRow) {
                        PartProxy partProxy = partsModel.getPartProxyAt(partsTable.convertRowIndexToModel(i));
                        addPart(false, partProxy.getPart(), 1);
                        partProxy.updateTargetAndInventories();
                        partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i),
                              PartsStoreModel.COL_TARGET);
                        partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i),
                              PartsStoreModel.COL_TRANSIT);
                        partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i),
                              PartsStoreModel.COL_SUPPLY);
                        partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i),
                              PartsStoreModel.COL_QUEUE);
                    }
                }
            });
            if (campaign.isGM()) {
                panButtons.add(btnAdd, new GridBagConstraints());
            }
            // endregion Add

            // region Add Bulk
            JButton btnAddBulk = new JButton(resourceMap.getString("btnAddBulk.text"));
            btnAddBulk.addActionListener(evt -> {
                if (partsTable.getSelectedRowCount() > 0) {
                    int[] selectedRow = partsTable.getSelectedRows();
                    for (int i : selectedRow) {
                        PartProxy partProxy = partsModel.getPartProxyAt(partsTable.convertRowIndexToModel(i));

                        int quantity = 1;
                        PopupValueChoiceDialog pcd = new PopupValueChoiceDialog(campaignGUI.getFrame(),
                              true,
                              "How Many " + partProxy.getName() + '?',
                              quantity,
                              1,
                              CampaignGUI.MAX_QUANTITY_SPINNER);
                        pcd.setVisible(true);
                        quantity = pcd.getValue();

                        if (quantity > 0) {
                            addPart(false, partProxy.getPart(), quantity);
                            partProxy.updateTargetAndInventories();
                            partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i),
                                  PartsStoreModel.COL_TARGET);
                            partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i),
                                  PartsStoreModel.COL_TRANSIT);
                            partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i),
                                  PartsStoreModel.COL_SUPPLY);
                            partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i),
                                  PartsStoreModel.COL_QUEUE);
                        }
                    }
                }
            });
            if (campaign.isGM()) {
                panButtons.add(btnAddBulk, new GridBagConstraints());
            }
            // endregion Add Bulk

            // region Button Close
            btnClose = new JButton(resourceMap.getString("btnClose.text"));
            btnClose.addActionListener(evt -> setVisible(false));
            // endregion Button Close
        } else {
            // if we aren't adding the unit to the campaign, then different buttons
            btnAdd = new JButton(resourceMap.getString("btnAdd.text"));
            btnAdd.addActionListener(evt -> {
                setSelectedPart();
                setVisible(false);
            });
            panButtons.add(btnAdd, new GridBagConstraints());

            btnClose = new JButton(resourceMap.getString("btnCancel.text"));
            btnClose.addActionListener(evt -> {
                selectedPart = null;
                setVisible(false);
            });
        }
        panButtons.add(btnClose, new GridBagConstraints());

        getContentPane().add(panButtons, BorderLayout.PAGE_END);
        this.setPreferredSize(new Dimension(700, 600));
        pack();
    }

    /**
     * These need to be migrated to the Suite Constants / Suite Options Setup
     */
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(PartsStoreDialog.class);

            choiceParts.setName("partsType");
            preferences.manage(new JComboBoxPreference(choiceParts));

            partsTable.setName("partsTable");
            preferences.manage(new JTablePreference(partsTable));

            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            logger.error("Failed to set user preferences", ex);
        }
    }

    public void filterParts() {
        final int nGroup = choiceParts.getSelectedIndex();
        RowFilter<PartsStoreModel, Integer> partsTypeFilter = new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends PartsStoreModel, ? extends Integer> entry) {
                PartsStoreModel partsModel = entry.getModel();
                Part part = partsModel.getPartAt(entry.getIdentifier());

                if (hideImpossible.isSelected()) {
                    int target = partsModel.getPartProxyAt(entry.getIdentifier())
                                       .getTarget()
                                       .getTargetRoll()
                                       .getValue();
                    if (target == TargetRoll.IMPOSSIBLE || target == TargetRoll.AUTOMATIC_FAIL) {
                        return false;
                    }
                } // This MUST NOT be an else if

                if (!txtFilter.getText().isBlank() &&
                          !part.getName().toLowerCase().contains(txtFilter.getText().toLowerCase()) &&
                          !part.getDetails().toLowerCase().contains(txtFilter.getText().toLowerCase())) {
                    return false;
                } else if (((part.getTechBase() == Part.TechBase.CLAN) || part.isClan()) &&
                                 !campaign.getCampaignOptions().isAllowClanPurchases()) {
                    return false;
                } else if ((part.getTechBase() == Part.TechBase.IS) &&
                                 !campaign.getCampaignOptions().isAllowISPurchases()
                                 // Hack to allow Clan access to SL tech but not post-Exodus tech
                                 // until 3050.
                                 &&
                                 !(campaign.useClanTechBase() &&
                                         (part.getIntroductionDate() > 2787) &&
                                         (part.getIntroductionDate() < 3050))) {
                    return false;
                } else if (!campaign.isLegal(part)) {
                    return false;
                }

                if (nGroup == SG_ALL) {
                    return true;
                } else if (nGroup == SG_ARMOR) {
                    return part instanceof Armor; // ProtoMekAmor and BaArmor are derived from Armor
                } else if (nGroup == SG_SYSTEM) {
                    return (part instanceof MekLifeSupport) ||
                                 (part instanceof MekSensor) ||
                                 (part instanceof LandingGear) ||
                                 (part instanceof Avionics) ||
                                 (part instanceof FireControlSystem) ||
                                 (part instanceof AeroSensor) ||
                                 (part instanceof KfBoom) ||
                                 (part instanceof DropshipDockingCollar) ||
                                 (part instanceof JumpshipDockingCollar) ||
                                 (part instanceof BayDoor) ||
                                 (part instanceof Cubicle) ||
                                 (part instanceof GravDeck) ||
                                 (part instanceof VeeSensor) ||
                                 (part instanceof VeeStabilizer) ||
                                 (part instanceof ProtoMekSensor);
                } else if (nGroup == SG_EQUIP) {
                    return (part instanceof EquipmentPart) || (part instanceof ProtoMekJumpJet);
                } else if (nGroup == SG_LOC) {
                    return (part instanceof MekLocation) ||
                                 (part instanceof TankLocation) ||
                                 (part instanceof ProtoMekLocation);
                } else if (nGroup == SG_WEAP) {
                    return (part instanceof EquipmentPart) && (((EquipmentPart) part).getType() instanceof WeaponType);
                } else if (nGroup == SG_AMMO) {
                    return part instanceof AmmoStorage;
                } else if (nGroup == SG_MISC) {
                    return ((part instanceof EquipmentPart) && (((EquipmentPart) part).getType() instanceof MiscType) ||
                                  (part instanceof ProtoMekJumpJet));
                } else if (nGroup == SG_ENGINE) {
                    return part instanceof EnginePart;
                } else if (nGroup == SG_GYRO) {
                    return part instanceof MekGyro;
                } else if (nGroup == SG_ACT) {
                    return ((part instanceof MekActuator) ||
                                  (part instanceof ProtoMekArmActuator) ||
                                  (part instanceof ProtoMekLegActuator));
                } else if (nGroup == SG_COCKPIT) {
                    return part instanceof MekCockpit;
                } else if (nGroup == SG_BA_SUIT) {
                    return part instanceof BattleArmorSuit;
                } else if (nGroup == SG_OMNI_POD) {
                    return part instanceof OmniPod;
                } else {
                    return false;
                }
            }
        };
        partsSorter.setRowFilter(partsTypeFilter);
    }

    /**
     * Adds a part to the campaign, either by purchasing it or directly adding it to the quartermaster's inventory.
     *
     * @param purchase determines if the part should be purchased or directly added. If {@code true}, the part will be
     *                 added to the shopping list. If {@code false}, the part will be cloned and added directly to the
     *                 inventory.
     * @param part     the {@link Part} to be added.
     * @param quantity the number of parts to add.
     */
    private void addPart(boolean purchase, Part part, int quantity) {
        if (purchase) {
            campaign.getShoppingList().addShoppingItem(part.getAcquisitionWork(), quantity, campaign);
        } else {
            while (quantity > 0) {
                campaign.getQuartermaster().addPart(part.clone(), 0, true);
                quantity--;
            }
        }
    }

    private void setSelectedPart() {
        int row = partsTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        selectedPart = partsModel.getPartAt(partsTable.convertRowIndexToModel(row));
    }

    public Part getPart() {
        return selectedPart;
    }

    public static String getPartsGroupName(int group) {
        return switch (group) {
            case SG_ALL -> "All Parts";
            case SG_ARMOR -> "Armor";
            case SG_SYSTEM -> "System Components";
            case SG_EQUIP -> "Equipment";
            case SG_LOC -> "Locations";
            case SG_WEAP -> "Weapons";
            case SG_AMMO -> "Ammunition";
            case SG_MISC -> "Miscellaneous Equipment";
            case SG_ENGINE -> "Engines";
            case SG_GYRO -> "Gyros";
            case SG_ACT -> "Actuators";
            case SG_COCKPIT -> "Cockpits";
            case SG_BA_SUIT -> "Battle Armor Suits";
            case SG_OMNI_POD -> "Empty OmniPods";
            default -> "?";
        };
    }
}
