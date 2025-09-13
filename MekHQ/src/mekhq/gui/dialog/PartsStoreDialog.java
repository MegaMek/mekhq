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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Objects;
import java.util.ResourceBundle;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import megamek.client.ui.preferences.JComboBoxPreference;
import megamek.client.ui.preferences.JTablePreference;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.annotations.Nullable;
import megamek.common.enums.TechBase;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.WeaponType;
import megamek.common.rolls.TargetRoll;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.*;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.kfs.KFBoom;
import mekhq.campaign.parts.meks.MekActuator;
import mekhq.campaign.parts.meks.MekCockpit;
import mekhq.campaign.parts.meks.MekGyro;
import mekhq.campaign.parts.meks.MekLifeSupport;
import mekhq.campaign.parts.meks.MekLocation;
import mekhq.campaign.parts.meks.MekSensor;
import mekhq.campaign.parts.protomeks.ProtoMekArmActuator;
import mekhq.campaign.parts.protomeks.ProtoMekJumpJet;
import mekhq.campaign.parts.protomeks.ProtoMekLegActuator;
import mekhq.campaign.parts.protomeks.ProtoMekLocation;
import mekhq.campaign.parts.protomeks.ProtoMekSensor;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.gui.CampaignGUI;
import mekhq.gui.dialog.PartsStoreDialog.PartsTableModel.PartProxy;
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
    private PartsTableModel partsModel;
    private TableRowSorter<PartsTableModel> partsSorter;
    private boolean addToCampaign;
    private Part selectedPart;
    private Person logisticsPerson;

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
        partsModel = new PartsTableModel(campaign.getPartsStore().getInventory());
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
        partsSorter.setComparator(PartsTableModel.COL_DETAIL, new PartsDetailSorter());
        partsTable.setRowSorter(partsSorter);
        TableColumn column;
        for (int i = 0; i < PartsTableModel.N_COL; i++) {
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
                              PartsTableModel.COL_TARGET);
                        partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i),
                              PartsTableModel.COL_TRANSIT);
                        partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i),
                              PartsTableModel.COL_SUPPLY);
                        partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i),
                              PartsTableModel.COL_QUEUE);
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
                                  PartsTableModel.COL_TARGET);
                            partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i),
                                  PartsTableModel.COL_TRANSIT);
                            partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i),
                                  PartsTableModel.COL_SUPPLY);
                            partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i),
                                  PartsTableModel.COL_QUEUE);
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
                              PartsTableModel.COL_TARGET);
                        partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i),
                              PartsTableModel.COL_TRANSIT);
                        partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i),
                              PartsTableModel.COL_SUPPLY);
                        partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i),
                              PartsTableModel.COL_QUEUE);
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
                                  PartsTableModel.COL_TARGET);
                            partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i),
                                  PartsTableModel.COL_TRANSIT);
                            partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i),
                                  PartsTableModel.COL_SUPPLY);
                            partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i),
                                  PartsTableModel.COL_QUEUE);
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
        RowFilter<PartsTableModel, Integer> partsTypeFilter = new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends PartsTableModel, ? extends Integer> entry) {
                PartsTableModel partsModel = entry.getModel();
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
                } else if (((part.getTechBase() == TechBase.CLAN) || part.isClan()) &&
                                 !campaign.getCampaignOptions().isAllowClanPurchases()) {
                    return false;
                } else if ((part.getTechBase() == TechBase.IS) &&
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
                    return part instanceof Armor; // ProtoMekAmor and BAArmor are derived from Armor
                } else if (nGroup == SG_SYSTEM) {
                    return (part instanceof MekLifeSupport) ||
                                 (part instanceof MekSensor) ||
                                 (part instanceof LandingGear) ||
                                 (part instanceof Avionics) ||
                                 (part instanceof FireControlSystem) ||
                                 (part instanceof AeroSensor) ||
                                 (part instanceof KFBoom) ||
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

    private Person getLogisticsPerson() {
        if (null == logisticsPerson) {
            logisticsPerson = campaign.getLogisticsPerson();
        }
        return logisticsPerson;
    }

    /**
     * A table model for displaying parts - similar to the one in CampaignGUI, but not exactly
     */
    public class PartsTableModel extends AbstractTableModel {
        protected String[] columnNames;
        protected ArrayList<PartProxy> data;

        public final static int COL_NAME = 0;
        public final static int COL_DETAIL = 1;
        public final static int COL_TECH_BASE = 2;
        public final static int COL_COST = 3;
        public final static int COL_TON = 4;
        public final static int COL_TARGET = 5;
        public final static int COL_SUPPLY = 6;
        public final static int COL_TRANSIT = 7;
        public final static int COL_QUEUE = 8;
        public final static int N_COL = 9;

        /**
         * Provides a lazy view to a {@link TargetRoll} for use in a UI (e.g. sorting in a table).
         */
        public static class TargetProxy implements Comparable<TargetProxy> {
            private TargetRoll target;
            private String details;
            private String description;

            /**
             * Creates a new proxy object for a {@link TargetRoll}.
             *
             * @param t The {@link TargetRoll} to be proxied. May be null.
             */
            public TargetProxy(@Nullable TargetRoll t) {
                target = t;
            }

            /**
             * Gets the target roll.
             *
             * @return The target roll.
             */
            public TargetRoll getTargetRoll() {
                return target;
            }

            /**
             * Gets a description of the target roll.
             *
             * @return A description of the target roll.
             */
            @Nullable
            public String getDescription() {
                if (null == target) {
                    return null;
                }
                if (null == description) {
                    description = target.getDesc();
                }
                return description;
            }

            /**
             * Gets a string representation of a {@link TargetRoll}.
             *
             * @return A string representation of a {@link TargetRoll}.
             */
            @Override
            public String toString() {
                if (null == target) {
                    return "-";
                }

                if (null == details) {
                    details = target.getValueAsString();
                    if (target.getValue() != TargetRoll.IMPOSSIBLE &&
                              target.getValue() != TargetRoll.AUTOMATIC_SUCCESS &&
                              target.getValue() != TargetRoll.AUTOMATIC_FAIL) {
                        details += "+";
                    }
                }

                return details;
            }

            /**
             * Converts a {@link TargetRoll} into an integer for comparisons.
             *
             * @return An integer representation of the {@link TargetRoll}.
             */
            private int coerceTargetRoll() {
                int r = target.getValue();
                if (r == TargetRoll.IMPOSSIBLE) {
                    return Integer.MAX_VALUE;
                } else if (r == TargetRoll.AUTOMATIC_FAIL) {
                    return Integer.MAX_VALUE - 1;
                } else if (r == TargetRoll.AUTOMATIC_SUCCESS) {
                    return Integer.MIN_VALUE;
                }
                return r;
            }

            /**
             * {@inheritDoc}
             *
             * @param o The {@link TargetProxy} to compare this instance to.
             *
             * @return {@inheritDoc}
             */
            @Override
            public int compareTo(TargetProxy o) {
                return Integer.compare(coerceTargetRoll(), o.coerceTargetRoll());
            }
        }

        /**
         * Provides a container for a value formatted for display and the value itself for sorting.
         */
        public static class FormattedValue<T extends Comparable<T>> implements Comparable<FormattedValue<T>> {
            private T value;
            private String formatted;

            /**
             * Creates a wrapper around a value and a formatted string representing the value.
             */
            public FormattedValue(T v, String f) {
                value = v;
                formatted = f;
            }

            /**
             * Gets the wrapped value.
             *
             * @return The value.
             */
            public T getValue() {
                return value;
            }

            /**
             * Gets the formatted value.
             *
             * @return The formatted value.
             */
            @Override
            public String toString() {
                return formatted;
            }

            /**
             * {@inheritDoc}
             *
             * @return {@inheritDoc}
             */
            @Override
            public int compareTo(FormattedValue<T> o) {
                if (null == o) {
                    return -1;
                }
                return getValue().compareTo(o.getValue());
            }
        }

        /**
         * Provides a lazy view to a {@link Part} for use in a UI (e.g. sorting in a table).
         */
        public class PartProxy {
            private Part part;
            private String details;
            private TargetProxy targetProxy;
            private FormattedValue<Money> cost;
            private PartInventory inventories;
            private FormattedValue<Integer> ordered;
            private FormattedValue<Integer> supply;
            private FormattedValue<Integer> transit;

            /**
             * Initializes a new instance of the class to provide a proxy view into a part.
             *
             * @param p The part to proxy. Must not be null.
             */
            public PartProxy(Part p) {
                part = Objects.requireNonNull(p);
            }

            /**
             * Updates the proxied view of the properties which changed outside the proxy.
             */
            public void updateTargetAndInventories() {
                targetProxy = null;
                inventories = null;
                ordered = null;
                supply = null;
                transit = null;
            }

            /**
             * Gets the part being proxied.
             *
             * @return The part being proxied.
             */
            public Part getPart() {
                return part;
            }

            /**
             * Gets the part's name.
             *
             * @return The part's name.
             */
            public String getName() {
                return part.getName();
            }

            /**
             * Gets the part's details.
             *
             * @return The part's detailed.
             */
            public String getDetails() {
                if (null == details) {
                    details = part.getDetails();
                }

                return details;
            }

            /**
             * Gets the part's cost, suitable for use in a UI element which requires both a display value and a sortable
             * value.
             *
             * @return The part's cost as a {@link FormattedValue}
             */
            public FormattedValue<Money> getCost() {
                if (null == cost) {
                    Money actualValue = part.getActualValue();
                    cost = new FormattedValue<>(actualValue, actualValue.toAmountString());
                }
                return cost;
            }

            /**
             * Gets the part's tonnage.
             *
             * @return The part's tonnage.
             */
            public double getTonnage() {
                return Math.round(part.getTonnage() * 100) / 100.0;
            }

            /**
             * Gets the part's tech base.
             *
             * @return The part's tech base.
             */
            public String getTechBase() {
                return part.getTechBaseName();
            }

            /**
             * Gets the part's {@link TargetRoll}.
             *
             * @return A {@link TargetProxy} representing the target roll for the part.
             */
            public TargetProxy getTarget() {
                if (null == targetProxy) {
                    IAcquisitionWork shoppingItem = part.getMissingPart();
                    if (null == shoppingItem && part instanceof IAcquisitionWork) {
                        shoppingItem = (IAcquisitionWork) part;
                    }
                    if (null != shoppingItem) {
                        TargetRoll target = campaign.getTargetForAcquisition(shoppingItem, getLogisticsPerson(), true);
                        targetProxy = new TargetProxy(target);
                    } else {
                        targetProxy = new TargetProxy(null);
                    }
                }

                return targetProxy;
            }

            /**
             * Gets the part's quantity on order, suitable for use in a UI element which requires both a display value
             * and a sortable value.
             *
             * @return The part's quantity on order as a {@link FormattedValue}
             */
            public FormattedValue<Integer> getOrdered() {
                if (null == inventories) {
                    inventories = campaign.getPartInventory(part);
                }
                if (null == ordered) {
                    ordered = new FormattedValue<>(inventories.getOrdered(), inventories.orderedAsString());
                }
                return ordered;
            }

            /**
             * Gets the part's quantity on hand, suitable for use in a UI element which requires both a display value
             * and a sortable value.
             *
             * @return The part's quantity on hand as a {@link FormattedValue}
             */
            public FormattedValue<Integer> getSupply() {
                if (null == inventories) {
                    inventories = campaign.getPartInventory(part);
                }
                if (null == supply) {
                    supply = new FormattedValue<>(inventories.getSupply(), inventories.supplyAsString());
                }
                return supply;
            }

            /**
             * Gets the part's quantity in transit, suitable for use in a UI element which requires both a display value
             * and a sortable value.
             *
             * @return The part's quantity in transit as a {@link FormattedValue}
             */
            public FormattedValue<Integer> getTransit() {
                if (null == inventories) {
                    inventories = campaign.getPartInventory(part);
                }
                if (null == transit) {
                    transit = new FormattedValue<>(inventories.getTransit(), inventories.transitAsString());
                }
                return transit;
            }
        }

        public PartsTableModel(ArrayList<Part> inventory) {
            data = new ArrayList<>(inventory.size());
            for (Part part : inventory) {
                data.add(new PartProxy(part));
            }
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return N_COL;
        }

        @Override
        public String getColumnName(int column) {
            return switch (column) {
                case COL_NAME -> "Name";
                case COL_DETAIL -> "Detail";
                case COL_COST -> "Cost";
                case COL_TON -> "Ton";
                case COL_TECH_BASE -> "Tech";
                case COL_TARGET -> "Target";
                case COL_QUEUE -> "# Ordered";
                case COL_SUPPLY -> "# Supply";
                case COL_TRANSIT -> "# Transit";
                default -> "?";
            };
        }

        @Override
        public Object getValueAt(int row, int col) {
            PartProxy part;
            if (data.isEmpty()) {
                return "";
            } else {
                part = data.get(row);
            }
            if (col == COL_NAME) {
                return part.getName();
            }
            if (col == COL_DETAIL) {
                return part.getDetails();
            }
            if (col == COL_COST) {
                return part.getCost();
            }
            if (col == COL_TON) {
                return part.getTonnage();
            }
            if (col == COL_TECH_BASE) {
                return part.getTechBase();
            }
            if (col == COL_TARGET) {
                return part.getTarget();
            }
            if (col == COL_SUPPLY) {
                return part.getSupply();
            }
            if (col == COL_TRANSIT) {
                return part.getTransit();
            }
            if (col == COL_QUEUE) {
                return part.getOrdered();
            }
            return "?";
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        public PartProxy getPartProxyAt(int row) {
            return data.get(row);
        }

        public Part getPartAt(int row) {
            return data.get(row).getPart();
        }

        public int getColumnWidth(int c) {
            return switch (c) {
                case COL_NAME, COL_DETAIL -> 100;
                case COL_COST, COL_TARGET -> 40;
                case COL_SUPPLY, COL_TRANSIT, COL_QUEUE -> 30;
                default -> 15;
            };
        }

        public int getAlignment(int col) {
            return switch (col) {
                case COL_COST, COL_TON -> SwingConstants.RIGHT;
                case COL_TARGET -> SwingConstants.CENTER;
                default -> SwingConstants.LEFT;
            };
        }

        public String getTooltip(int row, int col) {
            PartProxy part;
            if (data.isEmpty()) {
                return null;
            } else {
                part = data.get(row);
            }
            if (col == COL_TARGET) {
                return part.getTarget().getDescription();
            }
            return null;
        }

        public Renderer getRenderer() {
            return new Renderer();
        }

        public class Renderer extends DefaultTableCellRenderer {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                  boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setOpaque(true);
                int actualCol = table.convertColumnIndexToModel(column);
                int actualRow = table.convertRowIndexToModel(row);
                setHorizontalAlignment(getAlignment(actualCol));
                setToolTipText(getTooltip(actualRow, actualCol));

                return this;
            }

        }
    }
}
