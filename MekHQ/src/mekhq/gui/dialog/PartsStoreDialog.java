/*
 * PartsStoreDialog.java
 *
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Objects;
import java.util.ResourceBundle;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import megamek.common.AmmoType;
import megamek.common.MiscType;
import megamek.common.TargetRoll;
import megamek.common.WeaponType;
import megamek.common.annotations.Nullable;
import megamek.common.logging.LogLevel;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.parts.AeroSensor;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Avionics;
import mekhq.campaign.parts.BattleArmorSuit;
import mekhq.campaign.parts.EnginePart;
import mekhq.campaign.parts.FireControlSystem;
import mekhq.campaign.parts.LandingGear;
import mekhq.campaign.parts.MekActuator;
import mekhq.campaign.parts.MekCockpit;
import mekhq.campaign.parts.MekGyro;
import mekhq.campaign.parts.MekLifeSupport;
import mekhq.campaign.parts.MekLocation;
import mekhq.campaign.parts.MekSensor;
import mekhq.campaign.parts.MissingPart;
import mekhq.campaign.parts.OmniPod;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.PartInventory;
import mekhq.campaign.parts.ProtomekArmActuator;
import mekhq.campaign.parts.ProtomekJumpJet;
import mekhq.campaign.parts.ProtomekLegActuator;
import mekhq.campaign.parts.ProtomekLocation;
import mekhq.campaign.parts.ProtomekSensor;
import mekhq.campaign.parts.TankLocation;
import mekhq.campaign.parts.VeeSensor;
import mekhq.campaign.parts.VeeStabiliser;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.gui.CampaignGUI;
import mekhq.gui.dialog.PartsStoreDialog.PartsTableModel.PartProxy;
import mekhq.gui.preferences.JComboBoxPreference;
import mekhq.gui.preferences.JTablePreference;
import mekhq.gui.preferences.JWindowPreference;
import mekhq.gui.sorter.PartsDetailSorter;
import mekhq.preferences.PreferencesNode;

/**
 *
 * @author  Taharqa
 */
public class PartsStoreDialog extends javax.swing.JDialog {
    private static final long serialVersionUID = -8038099101234445018L;

    //parts filter groups
    private static final int SG_ALL      = 0;
    private static final int SG_ARMOR    = 1;
    private static final int SG_SYSTEM   = 2;
    private static final int SG_EQUIP    = 3;
    private static final int SG_LOC      = 4;
    private static final int SG_WEAP     = 5;
    private static final int SG_AMMO     = 6;
    private static final int SG_MISC     = 7;
    private static final int SG_ENGINE   = 8;
    private static final int SG_GYRO     = 9;
    private static final int SG_ACT      = 10;
    private static final int SG_COCKPIT  = 11;
    private static final int SG_BA_SUIT  = 12;
    private static final int SG_OMNI_POD = 13;
    private static final int SG_NUM      = 14;

    @SuppressWarnings("unused")
    private Frame frame; // FIXME: Unused? Do we need it?
    private Campaign campaign;
    private CampaignGUI campaignGUI;
    private PartsTableModel partsModel;
    private TableRowSorter<PartsTableModel> partsSorter;
    boolean addToCampaign;
    Part selectedPart = null;
    private Person logisticsPerson;

    private JTable partsTable;
    private JScrollPane scrollPartsTable;
    private JPanel panFilter;
    private JLabel lblFilter;
    private javax.swing.JTextField txtFilter;
    private JComboBox<String> choiceParts;
    private JLabel lblPartsChoice;
    private JPanel panButtons;
    private JButton btnAdd;
    private JButton btnBuyBulk;
    private JButton btnBuy;
    private JButton btnUseBonusPart;
    private JButton btnClose;

    /** Creates new form PartsStoreDialog */
    public PartsStoreDialog(boolean modal, CampaignGUI gui) {
        this(gui.getFrame(), modal, gui, gui.getCampaign(), true);
    }

    /** Creates new form PartsStoreDialog */
    public PartsStoreDialog(Frame frame, boolean modal, CampaignGUI gui, Campaign campaign, boolean add) {
        super(frame, modal);
        this.frame = frame;
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

        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.PartsStoreDialog", new EncodeControl()); //$NON-NLS-1$
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        setTitle(resourceMap.getString("Form.title"));

        getContentPane().setLayout(new BorderLayout());

        partsTable = new JTable(partsModel);
        partsTable.setName("partsTable"); // NOI18N
        partsSorter = new TableRowSorter<PartsTableModel>(partsModel);
        partsSorter.setComparator(PartsTableModel.COL_DETAIL, new PartsDetailSorter());
        partsTable.setRowSorter(partsSorter);
        TableColumn column = null;
        for (int i = 0; i < PartsTableModel.N_COL; i++) {
            column = partsTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(partsModel.getColumnWidth(i));
            column.setCellRenderer(partsModel.getRenderer());
        }
        partsTable.setIntercellSpacing(new Dimension(0, 0));
        partsTable.setShowGrid(false);
        scrollPartsTable = new JScrollPane();
        scrollPartsTable.setName("scrollPartsTable"); // NOI18N
        scrollPartsTable.setViewportView(partsTable);
        getContentPane().add(scrollPartsTable, BorderLayout.CENTER);

        GridBagConstraints c = new GridBagConstraints();
        panFilter = new JPanel();
        lblPartsChoice = new JLabel(resourceMap.getString("lblPartsChoice.text")); // NOI18N
        DefaultComboBoxModel<String> partsGroupModel = new DefaultComboBoxModel<String>();
        for (int i = 0; i < SG_NUM; i++) {
            partsGroupModel.addElement(getPartsGroupName(i));
        }
        choiceParts = new JComboBox<String>(partsGroupModel);
        choiceParts.setName("choiceParts"); // NOI18N
        choiceParts.setSelectedIndex(0);
        choiceParts.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterParts();
            }
        });
        panFilter.setLayout(new GridBagLayout());
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.0;
        c.anchor = java.awt.GridBagConstraints.WEST;
        c.insets = new Insets(5,5,5,5);
        panFilter.add(lblPartsChoice, c);
        c.gridx = 1;
        c.weightx = 1.0;
        panFilter.add(choiceParts, c);

        lblFilter = new JLabel(resourceMap.getString("lblFilter.text")); // NOI18N
        lblFilter.setName("lblFilter"); // NOI18N
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0.0;
        panFilter.add(lblFilter, c);
        txtFilter = new javax.swing.JTextField();
        txtFilter.setText(""); // NOI18N
        txtFilter.setMinimumSize(new java.awt.Dimension(200, 28));
        txtFilter.setName("txtFilter"); // NOI18N
        txtFilter.setPreferredSize(new java.awt.Dimension(200, 28));
        txtFilter.getDocument().addDocumentListener(
            new DocumentListener() {
                public void changedUpdate(DocumentEvent e) {
                    filterParts();
                }
                public void insertUpdate(DocumentEvent e) {
                    filterParts();
                }
                public void removeUpdate(DocumentEvent e) {
                    filterParts();
                }
            });
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 1.0;
        panFilter.add(txtFilter, c);
        getContentPane().add(panFilter, BorderLayout.PAGE_START);

        panButtons = new JPanel();
        if (addToCampaign) {
            btnAdd = new JButton(resourceMap.getString("btnAdd.text"));
            btnAdd.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    if (partsTable.getSelectedRowCount() > 0) {
                        int selectedRow[] = partsTable.getSelectedRows();
                        for (int i : selectedRow) {
                            PartProxy partProxy = partsModel.getPartProxyAt(partsTable.convertRowIndexToModel(i));
                            addPart(false, partProxy.getPart(), 1);
                            partProxy.updateTargetAndInventories();
                            partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i), PartsTableModel.COL_TARGET);
                            partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i), PartsTableModel.COL_TRANSIT);
                            partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i), PartsTableModel.COL_SUPPLY);
                            partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i), PartsTableModel.COL_QUEUE);
                        }
                    }
                }
            });
            btnAdd.setEnabled(campaign.isGM());
            btnBuyBulk = new JButton(resourceMap.getString("btnBuyBulk.text"));
            btnBuyBulk.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    if (partsTable.getSelectedRowCount() > 0) {
                        int quantity = 1;
                        PopupValueChoiceDialog pcd = new PopupValueChoiceDialog(campaignGUI.getFrame(), true, "How Many?", quantity, 1, CampaignGUI.MAX_QUANTITY_SPINNER);
                        pcd.setVisible(true);
                        quantity = pcd.getValue();

                        int selectedRow[] = partsTable.getSelectedRows();
                        for (int i : selectedRow) {
                            PartProxy partProxy = partsModel.getPartProxyAt(partsTable.convertRowIndexToModel(i));
                            addPart(true, false, partProxy.getPart(), quantity);
                            partProxy.updateTargetAndInventories();
                            partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i), PartsTableModel.COL_TARGET);
                            partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i), PartsTableModel.COL_TRANSIT);
                            partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i), PartsTableModel.COL_SUPPLY);
                            partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i), PartsTableModel.COL_QUEUE);
                        }
                    }
                }
            });
            btnBuy = new JButton(resourceMap.getString("btnBuy.text"));
            btnBuy.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    if (partsTable.getSelectedRowCount() > 0) {
                        int selectedRow[] = partsTable.getSelectedRows();
                        for (int i : selectedRow) {
                            PartProxy partProxy = partsModel.getPartProxyAt(partsTable.convertRowIndexToModel(i));
                            addPart(true, partProxy.getPart(), 1);
                            partProxy.updateTargetAndInventories();
                            partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i), PartsTableModel.COL_TARGET);
                            partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i), PartsTableModel.COL_TRANSIT);
                            partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i), PartsTableModel.COL_SUPPLY);
                            partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i), PartsTableModel.COL_QUEUE);            }
                        }
                    }
            });
            btnUseBonusPart = new JButton();
            if (campaign.getCampaignOptions().getUseAtB()) {
                btnUseBonusPart.setText(resourceMap.getString("useBonusPart.text") + " (" + campaign.totalBonusParts() + ")");
                btnUseBonusPart.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent evt) {
                        if (partsTable.getSelectedRowCount() > 0) {
                            int selectedRow[] = partsTable.getSelectedRows();
                            for (int i : selectedRow) {
                                if (campaign.totalBonusParts() > 0) {
                                    campaign.addReport(resourceMap.getString("bonusPartLog.text") + " " + partsModel.getPartAt(partsTable.convertRowIndexToModel(i)).getPartName());
                                }
                                PartProxy partProxy = partsModel.getPartProxyAt(partsTable.convertRowIndexToModel(i));
                                addPart(true, campaign.totalBonusParts() > 0, partProxy.getPart(), 1);
                                partProxy.updateTargetAndInventories();
                                partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i), PartsTableModel.COL_TARGET);
                                partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i), PartsTableModel.COL_TRANSIT);
                                partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i), PartsTableModel.COL_SUPPLY);
                                partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(i), PartsTableModel.COL_QUEUE);

                                btnUseBonusPart.setText(resourceMap.getString("useBonusPart.text") + " (" + campaign.totalBonusParts() + ")");
                                btnUseBonusPart.setVisible(campaign.totalBonusParts() > 0);
                            }
                        }
                    }
                });
                btnUseBonusPart.setVisible(campaign.totalBonusParts() > 0);
            }
            btnClose = new JButton(resourceMap.getString("btnClose.text"));
            btnClose.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    setVisible(false);
                }
            });
            panButtons.setLayout(new GridBagLayout());
            panButtons.add(btnBuyBulk, new GridBagConstraints());
            panButtons.add(btnBuy, new GridBagConstraints());
            if (campaign.getCampaignOptions().getUseAtB()) {
                panButtons.add(btnUseBonusPart, new GridBagConstraints());
            }
            panButtons.add(btnAdd, new GridBagConstraints());
            panButtons.add(btnClose, new GridBagConstraints());
        } else {
            //if we aren't adding the unit to the campaign, then different buttons
            btnAdd = new JButton("Add");
            btnAdd.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    setSelectedPart();
                    setVisible(false);
                }
            });
            panButtons.add(btnAdd, new GridBagConstraints());

            btnClose = new JButton("Cancel"); // NOI18N
            btnClose.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    selectedPart = null;
                    setVisible(false);
                }
            });
            panButtons.add(btnClose, new GridBagConstraints());
        }
        getContentPane().add(panButtons, BorderLayout.PAGE_END);
        this.setPreferredSize(new Dimension(700,600));
        pack();
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(PartsStoreDialog.class);

        choiceParts.setName("partsType");
        preferences.manage(new JComboBoxPreference(choiceParts));

        partsTable.setName("partsTable");
        preferences.manage(new JTablePreference(partsTable));

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    public void filterParts() {
        RowFilter<PartsTableModel, Integer> partsTypeFilter = null;
        final int nGroup = choiceParts.getSelectedIndex();
        partsTypeFilter = new RowFilter<PartsTableModel,Integer>() {
            @Override
            public boolean include(Entry<? extends PartsTableModel, ? extends Integer> entry) {
                PartsTableModel partsModel = entry.getModel();
                Part part = partsModel.getPartAt(entry.getIdentifier());
                if ((txtFilter.getText().length() > 0)
                        && !part.getName().toLowerCase().contains(txtFilter.getText().toLowerCase())
                        && !part.getDetails().toLowerCase().contains(txtFilter.getText().toLowerCase())) {
                    return false;
                }
                if(part.getTechBase() == Part.T_CLAN && !campaign.getCampaignOptions().allowClanPurchases()) {
                    return false;
                }
                if((part.getTechBase() == Part.T_IS)
                        && !campaign.getCampaignOptions().allowISPurchases()
                        // Hack to allow Clan access to SL tech but not post-Exodus tech
                        // until 3050.
                        && !(campaign.useClanTechBase() && (part.getIntroductionDate() > 2787)
                                && (part.getIntroductionDate() < 3050))) {
                    return false;
                }
                if (!campaign.isLegal(part)) {
                    return false;
                }
                if(nGroup == SG_ALL) {
                    return true;
                } else if(nGroup == SG_ARMOR) {
                    return part instanceof Armor; // ProtomekAmor and BaArmor are derived from Armor
                } else if(nGroup == SG_SYSTEM) {
                    return part instanceof MekLifeSupport
                        || part instanceof MekSensor
                        || part instanceof LandingGear
                        || part instanceof Avionics
                        || part instanceof FireControlSystem
                        || part instanceof AeroSensor
                        || part instanceof VeeSensor
                        || part instanceof VeeStabiliser
                        || part instanceof ProtomekSensor;
                } else if(nGroup == SG_EQUIP) {
                    return part instanceof EquipmentPart || part instanceof ProtomekJumpJet;
                } else if(nGroup == SG_LOC) {
                    return part instanceof MekLocation || part instanceof TankLocation || part instanceof ProtomekLocation;
                } else if(nGroup == SG_WEAP) {
                    return part instanceof EquipmentPart && ((EquipmentPart)part).getType() instanceof WeaponType;
                } else if(nGroup == SG_AMMO) {
                    return part instanceof EquipmentPart && ((EquipmentPart)part).getType() instanceof AmmoType;
                } else if(nGroup == SG_MISC) {
                    return (part instanceof EquipmentPart && ((EquipmentPart)part).getType() instanceof MiscType) || part instanceof ProtomekJumpJet;
                } else if(nGroup == SG_ENGINE) {
                    return part instanceof EnginePart;
                } else if(nGroup == SG_GYRO) {
                    return part instanceof MekGyro;
                } else if(nGroup == SG_ACT) {
                    return part instanceof MekActuator || part instanceof ProtomekArmActuator || part instanceof ProtomekLegActuator;
                } else if(nGroup == SG_COCKPIT) {
                    return part instanceof MekCockpit;
                } else if(nGroup == SG_BA_SUIT) {
                    return part instanceof BattleArmorSuit;
                } else if(nGroup == SG_OMNI_POD) {
                    return part instanceof OmniPod;
                }
                return false;
            }
        };
        partsSorter.setRowFilter(partsTypeFilter);
    }

    private void addPart(boolean purchase, Part part, int quantity) {
        addPart(purchase, false, part, quantity);
    }

    private void addPart(boolean purchase, boolean bonus, Part part, int quantity) {
        final String METHOD_NAME = "addPart(boolean,boolean,Part,int)"; //$NON-NLS-1$

        if(bonus) {
            String report = part.getAcquisitionWork().find(0);
            if (report.endsWith("0 days.")) {
                AtBContract contract = null;
                for (Mission m : campaign.getMissions()) {
                    if (m.isActive() && m instanceof AtBContract &&
                            ((AtBContract)m).getNumBonusParts() > 0) {
                        contract = (AtBContract)m;
                        break;
                    }
                }
                if (null == contract) {
                    MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                            "AtB: used bonus part but no contract has bonus parts available."); //$NON-NLS-1$
                } else {
                    contract.useBonusPart();
                }
            }
        } else if(purchase) {
            campaign.getShoppingList().addShoppingItem(part.getAcquisitionWork(), quantity, campaign);
        } else {
            while(quantity > 0) {
                campaign.addPart(part.clone(), 0);
                quantity--;
            }
        }
    }

    private void setSelectedPart() {
        int row = partsTable.getSelectedRow();
        if(row < 0) {
            return;
        }
        selectedPart = partsModel.getPartAt(partsTable.convertRowIndexToModel(row));
    }

    public Part getPart() {
        return selectedPart;
    }

    public static String getPartsGroupName(int group) {
        switch(group) {
        case SG_ALL:
            return "All Parts";
        case SG_ARMOR:
            return "Armor";
        case SG_SYSTEM:
            return "System Components";
        case SG_EQUIP:
            return "Equipment";
        case SG_LOC:
            return "Locations";
        case SG_WEAP:
            return "Weapons";
        case SG_AMMO:
            return "Ammunition";
        case SG_MISC:
            return "Miscellaneous Equipment";
        case SG_ENGINE:
            return "Engines";
        case SG_GYRO:
            return "Gyros";
        case SG_ACT:
            return "Actuators";
        case SG_COCKPIT:
            return "Cockpits";
        case SG_BA_SUIT:
            return "Battle Armor Suits";
        case SG_OMNI_POD:
            return "Empty OmniPods";
        default:
            return "?";
        }
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
        private static final long serialVersionUID = 534443424190075264L;

        protected String[] columnNames;
        protected ArrayList<PartProxy> data;

        public final static int COL_NAME    =    0;
        public final static int COL_DETAIL   =   1;
        public final static int COL_TECH_BASE  = 2;
        public final static int COL_COST     =   3;
        public final static int COL_TON       =  4;
        public final static int COL_TARGET    =  5;
        public final static int COL_SUPPLY    =  6;
        public final static int COL_TRANSIT   =  7;
        public final static int COL_QUEUE     =  8;
        public final static int N_COL          = 9;

        /**
         * Provides a lazy view to a {@link TargetRoll} for use in a UI (e.g. sorting in a table).
         */
        public class TargetProxy implements Comparable<TargetProxy> {
            private TargetRoll target;
            private String details;
            private String description;

            /**
             * Creates a new proxy object for a {@link TargetRoll}.
             * @param t The {@link TargetRoll} to be proxied. May be null.
             */
            public TargetProxy(@Nullable TargetRoll t) {
                target = t;
            }

            /**
             * Gets the target roll.
             * @return The target roll.
             */
            public TargetRoll getTargetRoll() {
                return target;
            }

            /**
             * Gets a description of the target roll.
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
             * @return An integer representation of the {@link TargetRoll}.
             */
            private int coerceTargetRoll() {
                int r = target.getValue();
                if (r == TargetRoll.IMPOSSIBLE) {
                    return Integer.MAX_VALUE;
                }
                else if(r == TargetRoll.AUTOMATIC_FAIL) {
                    return Integer.MAX_VALUE-1;
                }
                else if(r == TargetRoll.AUTOMATIC_SUCCESS) {
                    return Integer.MIN_VALUE;
                }
                return r;
            }

            /**
             * {@inheritDoc}
             * @param o The {@link TargetProxy} to compare this instance to.
             * @return {@inheritDoc}
             */
            @Override
            public int compareTo(TargetProxy o) {
                return Integer.compare(coerceTargetRoll(), o.coerceTargetRoll());
            }
        }

        /**
         * Provides a container for a value formatted for display and the
         * value itself for sorting.
         */
        public class FormattedValue<T extends Comparable<T>> implements Comparable<FormattedValue<T>> {
            private T value;
            private String formatted;

            /** 
             * Creates a wrapper around a value and a
             * formatted string representing the value.
             */
            public FormattedValue(T v, String f) {
                value = v;
                formatted = f;
            }

            /**
             * Gets the wrapped value.
             * @return The value.
             */
            public T getValue() {
                return value;
            }

            /**
             * Gets the formatted value.
             * @return The formatted value.
             */
            @Override
            public String toString() {
                return formatted;
            }

            /**
             * {@inheritDoc}
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
             * Initializes a new of the class to provide a proxy view into
             * a part.
             * @param p The part to proxy. Must not be null.
             */
            public PartProxy(Part p) {
                part = Objects.requireNonNull(p);
            }

            /**
             * Updates the proxied view of the properties which
             * changed outside the proxy.
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
             * @return The part being proxied.
             */
            public Part getPart() {
                return part;
            }

            /**
             * Gets the part's name.
             * @return The part's name.
             */
            public String getName() {
                return part.getName();
            }

            /**
             * Gets the part's details.
             * @return The part's detailed.
             */
            public String getDetails() {
                if (null == details) {
                    details = part.getDetails();
                }

                return details;
            }

            /**
             * Gets the part's cost, suitable for use in a UI element
             * which requires both a display value and a sortable value.
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
             * @return The part's tonnage.
             */
            public double getTonnage() {
                return Math.round(part.getTonnage() * 100) / 100.0;
            }

            /**
             * Gets the part's tech base.
             * @return The part's tech base.
             */
            public String getTechBase() {
                return part.getTechBaseName();
            }

            /**
             * Gets the part's {@link TargetRoll}.
             * @return A {@link TargetProxy} representing the target
             * roll for the part.
             */
            public TargetProxy getTarget() {
                if (null == targetProxy) {
                    IAcquisitionWork shoppingItem = (MissingPart)part.getMissingPart();
                    if (null == shoppingItem && part instanceof IAcquisitionWork) {
                        shoppingItem = (IAcquisitionWork)part;
                    }
                    if (null != shoppingItem) {
                        TargetRoll target = campaign.getTargetForAcquisition(shoppingItem, getLogisticsPerson());
                        targetProxy = new TargetProxy(target);
                    }
                    else {
                        targetProxy = new TargetProxy(null);
                    }
                }

                return targetProxy;
            }

            /**
             * Gets the part's quantity on order, suitable for use in a UI element
             * which requires both a display value and a sortable value.
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
             * Gets the part's quantity on hand, suitable for use in a UI element
             * which requires both a display value and a sortable value.
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
             * Gets the part's quantity in transit, suitable for use in a UI element
             * which requires both a display value and a sortable value.
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
            for (Part p : inventory) {
                data.add(new PartProxy(p));
            }
        }

        public int getRowCount() {
            return data.size();
        }

        public int getColumnCount() {
            return N_COL;
        }

        @Override
        public String getColumnName(int column) {
            switch(column) {
                case COL_NAME:
                    return "Name";
                case COL_DETAIL:
                    return "Detail";
                case COL_COST:
                    return "Cost";
                case COL_TON:
                    return "Ton";
                case COL_TECH_BASE:
                    return "Tech";
                case COL_TARGET:
                    return "Target";
                case COL_QUEUE:
                    return "# Ordered";
                case COL_SUPPLY:
                    return "# Supply";
                case COL_TRANSIT:
                    return "# Transit";
                default:
                    return "?";
            }
        }

        public Object getValueAt(int row, int col) {
            PartProxy part;
            if(data.isEmpty()) {
                return "";
            } else {
                part = (PartProxy)data.get(row);
            }
            if(col == COL_NAME) {
                return part.getName();
            }
            if(col == COL_DETAIL) {
                return part.getDetails();
            }
            if(col == COL_COST) {
                return part.getCost();
            }
            if(col == COL_TON) {
                return part.getTonnage();
            }
            if(col == COL_TECH_BASE) {
                return part.getTechBase();
            }
            if(col == COL_TARGET) {
                return part.getTarget();
            }
            if(col == COL_SUPPLY) {
                return part.getSupply();
            }
            if(col == COL_TRANSIT) {
                return part.getTransit();
            }
            if(col == COL_QUEUE) {
                return part.getOrdered();
            }
            return "?";
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }

        @Override
        public Class<? extends Object> getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        public PartProxy getPartProxyAt(int row) {
            return data.get(row);
        }

        public Part getPartAt(int row) {
            return data.get(row).getPart();
        }

        public Part[] getPartstAt(int[] rows) {
            Part[] parts = new Part[rows.length];
            for (int i = 0; i < rows.length; i++) {
                int row = rows[i];
                parts[i] = data.get(row).getPart();
            }
            return parts;
        }

         public int getColumnWidth(int c) {
                switch(c) {
                case COL_NAME:
                case COL_DETAIL:
                    return 100;
                case COL_COST:
                case COL_TARGET:
                    return 40;
                case COL_SUPPLY:
                case COL_TRANSIT:
                case COL_QUEUE:
                    return 30;
                default:
                    return 15;
                }
            }

            public int getAlignment(int col) {
                switch(col) {
                case COL_COST:
                case COL_TON:
                    return SwingConstants.RIGHT;
                case COL_TARGET:
                    return SwingConstants.CENTER;
                default:
                    return SwingConstants.LEFT;
                }
            }

            public String getTooltip(int row, int col) {
                PartProxy part;
                if(data.isEmpty()) {
                    return null;
                } else {
                    part = data.get(row);
                }
                switch(col) {
                case COL_TARGET:
                    return part.getTarget().getDescription();
                default:
                    return null;
                }
            }
            public PartsTableModel.Renderer getRenderer() {
                return new PartsTableModel.Renderer();
            }

            public class Renderer extends DefaultTableCellRenderer {

                private static final long serialVersionUID = 9054581142945717303L;

                public Component getTableCellRendererComponent(JTable table,
                        Object value, boolean isSelected, boolean hasFocus,
                        int row, int column) {
                    super.getTableCellRendererComponent(table, value, isSelected,
                            hasFocus, row, column);
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
