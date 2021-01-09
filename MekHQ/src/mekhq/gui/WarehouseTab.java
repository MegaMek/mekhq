/*
 * Copyright (c) 2017 The MegaMek Team. All rights reserved.
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
package mekhq.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.ResourceBundle;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import megamek.common.MiscType;
import megamek.common.TargetRoll;
import megamek.common.WeaponType;
import megamek.common.event.Subscribe;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.event.AcquisitionEvent;
import mekhq.campaign.event.AstechPoolChangedEvent;
import mekhq.campaign.event.OvertimeModeEvent;
import mekhq.campaign.event.PartChangedEvent;
import mekhq.campaign.event.PartModeChangedEvent;
import mekhq.campaign.event.PartNewEvent;
import mekhq.campaign.event.PartRemovedEvent;
import mekhq.campaign.event.PartWorkEvent;
import mekhq.campaign.event.PersonEvent;
import mekhq.campaign.event.UnitChangedEvent;
import mekhq.campaign.event.UnitRefitEvent;
import mekhq.campaign.event.UnitRemovedEvent;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.EnginePart;
import mekhq.campaign.parts.MekActuator;
import mekhq.campaign.parts.MekGyro;
import mekhq.campaign.parts.MekLifeSupport;
import mekhq.campaign.parts.MekLocation;
import mekhq.campaign.parts.MekSensor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.TankLocation;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.gui.adapter.PartsTableMouseAdapter;
import mekhq.gui.model.PartsTableModel;
import mekhq.gui.model.TechTableModel;
import mekhq.gui.preferences.JComboBoxPreference;
import mekhq.gui.preferences.JTablePreference;
import mekhq.gui.sorter.FormattedNumberSorter;
import mekhq.gui.sorter.PartsDetailSorter;
import mekhq.gui.sorter.TechSorter;
import mekhq.preferences.PreferencesNode;

/**
 * Displays all spare parts in stock, parts on order, and permits repair of damaged
 * parts.
 */
public final class WarehouseTab extends CampaignGuiTab implements ITechWorkPanel {
    private static final long serialVersionUID = 9172184916479921364L;

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
    private static final int SG_NUM = 11;

    // parts views
    private static final int SV_ALL = 0;
    private static final int SV_IN_TRANSIT = 1;
    private static final int SV_RESERVED = 2;
    private static final int SV_UNDAMAGED = 3;
    private static final int SV_DAMAGED = 4;
    private static final int SV_NUM = 5;

    private JPanel panSupplies;
    private JSplitPane splitWarehouse;
    private JTable partsTable;
    private JTable techTable;
    private JButton btnDoTask;
    private JToggleButton btnShowAllTechsWarehouse;
    private JLabel lblTargetNumWarehouse;
    private JTextArea textTargetWarehouse;
    private JLabel astechPoolLabel;
    private JComboBox<String> choiceParts;
    private JComboBox<String> choicePartsView;

    private PartsTableModel partsModel;
    private TechTableModel techsModel;

    private TableRowSorter<PartsTableModel> partsSorter;
    private TableRowSorter<TechTableModel> techSorter;

    //remember current selections so they can be restored after refresh
    private int selectedRow = -1;
    private int partId = -1;
    private Person selectedTech;

    WarehouseTab(CampaignGUI gui, String name) {
        super(gui, name);
        MekHQ.registerHandler(this);
        setUserPreferences();
    }

    /*
     * (non-Javadoc)
     *
     * @see mekhq.gui.CampaignGuiTab#initTab()
     */
    @Override
    public void initTab() {
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CampaignGUI", //$NON-NLS-1$ ;
                new EncodeControl());
        GridBagConstraints gridBagConstraints;

        panSupplies = new JPanel(new GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 0);
        panSupplies.add(new JLabel(resourceMap.getString("lblPartsChoice.text")), gridBagConstraints);

        DefaultComboBoxModel<String> partsGroupModel = new DefaultComboBoxModel<String>();
        for (int i = 0; i < SG_NUM; i++) {
            partsGroupModel.addElement(getPartsGroupName(i));
        }
        choiceParts = new JComboBox<String>(partsGroupModel);
        choiceParts.setSelectedIndex(0);
        choiceParts.addActionListener(ev -> filterParts());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 0);
        panSupplies.add(choiceParts, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 0);
        panSupplies.add(new JLabel(resourceMap.getString("lblPartsChoiceView.text")), gridBagConstraints);

        DefaultComboBoxModel<String> partsGroupViewModel = new DefaultComboBoxModel<String>();
        for (int i = 0; i < SV_NUM; i++) {
            partsGroupViewModel.addElement(getPartsGroupViewName(i));
        }
        choicePartsView = new JComboBox<String>(partsGroupViewModel);
        choicePartsView.setSelectedIndex(0);
        choicePartsView.addActionListener(ev -> filterParts());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 0);
        panSupplies.add(choicePartsView, gridBagConstraints);

        partsModel = new PartsTableModel();
        partsTable = new JTable(partsModel);
        partsSorter = new TableRowSorter<PartsTableModel>(partsModel);
        partsSorter.setComparator(PartsTableModel.COL_COST, new FormattedNumberSorter());
        partsSorter.setComparator(PartsTableModel.COL_DETAIL, new PartsDetailSorter());
        partsSorter.setComparator(PartsTableModel.COL_TOTAL_COST, new FormattedNumberSorter());
        partsTable.setRowSorter(partsSorter);
        TableColumn column = null;
        for (int i = 0; i < PartsTableModel.N_COL; i++) {
            column = partsTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(partsModel.getColumnWidth(i));
            column.setCellRenderer(partsModel.getRenderer());
        }
        partsTable.setIntercellSpacing(new Dimension(0, 0));
        partsTable.setShowGrid(false);
        partsTable.getSelectionModel().addListSelectionListener(ev -> {
            filterTechs();
            updateTechTarget();
        });
        partsTable.addMouseListener(new PartsTableMouseAdapter(getCampaignGui(),
                partsTable, partsModel));

        JScrollPane scrollPartsTable = new JScrollPane(partsTable);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSupplies.add(scrollPartsTable, gridBagConstraints);

        JPanel panelDoTask = new JPanel(new GridBagLayout());

        btnDoTask = new JButton(resourceMap.getString("btnDoTask.text")); // NOI18N
        btnDoTask.setToolTipText(resourceMap.getString("btnDoTask.toolTipText")); // NOI18N
        btnDoTask.setEnabled(false);
        btnDoTask.setName("btnDoTask"); // NOI18N
        btnDoTask.addActionListener(ev -> doTask());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        panelDoTask.add(btnDoTask, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        panelDoTask.add(new JLabel(resourceMap.getString("lblTarget.text")), gridBagConstraints);

        lblTargetNumWarehouse = new JLabel(resourceMap.getString("lblTargetNum.text"));
        lblTargetNumWarehouse.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        panelDoTask.add(lblTargetNumWarehouse, gridBagConstraints);

        textTargetWarehouse = new JTextArea();
        textTargetWarehouse.setColumns(20);
        textTargetWarehouse.setEditable(false);
        textTargetWarehouse.setLineWrap(true);
        textTargetWarehouse.setRows(5);
        textTargetWarehouse.setText(resourceMap.getString("textTarget.text")); // NOI18N
        textTargetWarehouse.setWrapStyleWord(true);
        textTargetWarehouse.setBorder(null);
        JScrollPane scrTargetWarehouse = new JScrollPane(textTargetWarehouse);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        panelDoTask.add(scrTargetWarehouse, gridBagConstraints);

        btnShowAllTechsWarehouse = new JToggleButton(resourceMap.getString("btnShowAllTechs.text"));
        btnShowAllTechsWarehouse.setToolTipText(resourceMap.getString("btnShowAllTechs.toolTipText")); // NOI18N
        btnShowAllTechsWarehouse.addActionListener(ev -> filterTechs());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        panelDoTask.add(btnShowAllTechsWarehouse, gridBagConstraints);

        techsModel = new TechTableModel(getCampaignGui(), this);
        techTable = new JTable(techsModel);
        techTable.setRowHeight(60);
        techTable.getColumnModel().getColumn(0).setCellRenderer(techsModel.getRenderer());
        techTable.getSelectionModel().addListSelectionListener(ev -> updateTechTarget());
        techSorter = new TableRowSorter<>(techsModel);
        techSorter.setComparator(0, new TechSorter());
        techTable.setRowSorter(techSorter);
        ArrayList<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        techSorter.setSortKeys(sortKeys);
        JScrollPane scrollTechTable = new JScrollPane(techTable);
        scrollTechTable.setMinimumSize(new java.awt.Dimension(200, 200));
        scrollTechTable.setPreferredSize(new java.awt.Dimension(300, 300));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        panelDoTask.add(scrollTechTable, gridBagConstraints);

        astechPoolLabel = new JLabel("<html><b>Astech Pool Minutes:</> " + getCampaign().getAstechPoolMinutes() + " ("
                + getCampaign().getNumberAstechs() + " Astechs)</html>"); // NOI18N
        astechPoolLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panelDoTask.add(astechPoolLabel, gridBagConstraints);

        splitWarehouse = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panSupplies, panelDoTask);
        splitWarehouse.setOneTouchExpandable(true);
        splitWarehouse.setResizeWeight(1.0);

        setLayout(new BorderLayout());
        add(splitWarehouse, BorderLayout.CENTER);
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(WarehouseTab.class);

        choiceParts.setName("partsType");
        preferences.manage(new JComboBoxPreference(choiceParts));

        choicePartsView.setName("partsView");
        preferences.manage(new JComboBoxPreference(choicePartsView));

        partsTable.setName("partsTable");
        preferences.manage(new JTablePreference(partsTable));
    }

    /* For parts export */
    public JTable getPartsTable() {
        return partsTable;
    }

    public PartsTableModel getPartsModel() {
        return partsModel;
    }

    /*
     * (non-Javadoc)
     *
     * @see mekhq.gui.CampaignGuiTab#refreshAll()
     */
    @Override
    public void refreshAll() {
        refreshTechsList();
        refreshPartsList();
    }

    /*
     * (non-Javadoc)
     *
     * @see mekhq.gui.CampaignGuiTab#tabType()
     */
    @Override
    public GuiTabType tabType() {
        return GuiTabType.WAREHOUSE;
    }

    public void filterParts() {
        RowFilter<PartsTableModel, Integer> partsTypeFilter = null;
        final int nGroup = choiceParts.getSelectedIndex();
        final int nGroupView = choicePartsView.getSelectedIndex();
        partsTypeFilter = new RowFilter<PartsTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends PartsTableModel, ? extends Integer> entry) {
                PartsTableModel partsModel = entry.getModel();
                Part part = partsModel.getPartAt(entry.getIdentifier());
                boolean inGroup = false;
                boolean inView = false;

                // Check grouping
                if (nGroup == SG_ALL) {
                    inGroup = true;
                } else if (nGroup == SG_ARMOR) {
                    inGroup = (part instanceof Armor); // ProtomekArmor and BaArmor are derived from Armor
                } else if (nGroup == SG_SYSTEM) {
                    inGroup = part instanceof MekGyro || part instanceof EnginePart || part instanceof MekActuator
                            || part instanceof MekLifeSupport || part instanceof MekSensor;
                } else if (nGroup == SG_EQUIP) {
                    inGroup = part instanceof EquipmentPart;
                } else if (nGroup == SG_LOC) {
                    inGroup = part instanceof MekLocation || part instanceof TankLocation;
                } else if (nGroup == SG_WEAP) {
                    inGroup = part instanceof EquipmentPart && ((EquipmentPart) part).getType() instanceof WeaponType;
                } else if (nGroup == SG_AMMO) {
                    inGroup = part instanceof AmmoStorage;
                } else if (nGroup == SG_MISC) {
                    inGroup = part instanceof EquipmentPart && ((EquipmentPart) part).getType() instanceof MiscType;
                } else if (nGroup == SG_ENGINE) {
                    inGroup = part instanceof EnginePart;
                } else if (nGroup == SG_GYRO) {
                    inGroup = part instanceof MekGyro;
                } else if (nGroup == SG_ACT) {
                    inGroup = part instanceof MekActuator;
                }

                // Check view
                if (nGroupView == SV_ALL) {
                    inView = true;
                } else if (nGroupView == SV_IN_TRANSIT) {
                    inView = !part.isPresent();
                } else if (nGroupView == SV_RESERVED) {
                    inView = part.isReservedForRefit() || part.isReservedForReplacement();
                } else if (nGroupView == SV_UNDAMAGED) {
                    inView = !part.needsFixing();
                } else if (nGroupView == SV_DAMAGED) {
                    inView = part.needsFixing();
                }
                return (inGroup && inView);
            }
        };
        partsSorter.setRowFilter(partsTypeFilter);
    }

    public static String getPartsGroupName(int group) {
        switch (group) {
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
        default:
            return "?";
        }
    }

    public static String getPartsGroupViewName(int view) {
        switch (view) {
        case SV_ALL:
            return "All";
        case SV_IN_TRANSIT:
            return "In Transit";
        case SV_RESERVED:
            return "Reserved for Refit/Repair";
        case SV_UNDAMAGED:
            return "Undamaged";
        case SV_DAMAGED:
            return "Damaged";
        default:
            return "?";
        }
    }

    public void filterTechs() {
        RowFilter<TechTableModel, Integer> techTypeFilter = null;
        final Part part = getSelectedTask();
        techTypeFilter = new RowFilter<TechTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends TechTableModel, ? extends Integer> entry) {
                if (null == part) {
                    return false;
                }
                if (!part.needsFixing() && !part.isSalvaging()) {
                    return false;
                }
                TechTableModel techModel = entry.getModel();
                Person tech = techModel.getTechAt(entry.getIdentifier());
                if (!tech.isRightTechTypeFor(part) && !btnShowAllTechsWarehouse.isSelected()) {
                    return false;
                }
                Skill skill = tech.getSkillForWorkingOn(part);
                int modePenalty = part.getMode().expReduction;
                if (skill == null) {
                    return false;
                }
                if (part.getSkillMin() > SkillType.EXP_ELITE) {
                    return false;
                }
                if (tech.getMinutesLeft() <= 0) {
                    return false;
                }
                return (getCampaign().getCampaignOptions().isDestroyByMargin()
                        || part.getSkillMin() <= (skill.getExperienceLevel() - modePenalty));
            }
        };
        techSorter.setRowFilter(techTypeFilter);
    }

    protected void updateTechTarget() {
        TargetRoll target = null;

        Part part = getSelectedTask();
        if (null != part) {
            Unit u = part.getUnit();
            Person tech = getSelectedTech();
            if (null != u && u.isSelfCrewed()) {
                tech = u.getEngineer();
                if (null == tech) {
                    target = new TargetRoll(TargetRoll.IMPOSSIBLE,
                            "You must have a crew assigned to large vessels to attempt repairs.");
                }
            }
            if (null != tech) {
                boolean wasNull = false;
                // Temporarily set the Team ID if it isn't already.
                // This is needed for the Clan Tech flag
                if (part.getTech() == null) {
                    part.setTech(tech);
                    wasNull = true;
                }
                target = getCampaign().getTargetFor(part, tech);
                if (wasNull) { // If it was null, make it null again
                    part.setTech(null);
                }
            }
            ((TechSorter) techSorter.getComparator(0)).clearPart();
        }
        if (null != target) {
            btnDoTask.setEnabled(target.getValue() != TargetRoll.IMPOSSIBLE);
            textTargetWarehouse.setText(target.getDesc());
            lblTargetNumWarehouse.setText(target.getValueAsString());
        } else {
            btnDoTask.setEnabled(false);
            textTargetWarehouse.setText("");
            lblTargetNumWarehouse.setText("-");
        }
    }

    public void refreshTechsList() {
        // The next gets all techs who have more than 0 minutes free, and sorted by skill descending (elites at bottom)
        techsModel.setData(getCampaign().getTechs(true, null, true, false));
        String astechString = "<html><b>Astech Pool Minutes:</> " + getCampaign().getAstechPoolMinutes();
        if (getCampaign().isOvertimeAllowed()) {
            astechString += " [" + getCampaign().getAstechPoolOvertime() + " overtime]";
        }
        astechString += " (" + getCampaign().getNumberAstechs() + " Astechs)</html>";
        refreshAstechPool(astechString);

        // If requested, switch to top entry
        if ((null == selectedTech || getCampaign().getCampaignOptions().useResetToFirstTech())
                && techTable.getRowCount() > 0) {
            techTable.setRowSelectionInterval(0, 0);
        } else if (null != selectedTech) {
            // Or get the selected tech back
            for (int i = 0; i < techTable.getRowCount(); i++) {
                Person p = techsModel.getTechAt(techTable.convertRowIndexToModel(i));
                if (selectedTech.getId().equals(p.getId())) {
                    techTable.setRowSelectionInterval(i, i);
                    break;
                }
            }
        }
    }

    public void refreshPartsList() {
        partsModel.setData(getCampaign().getWarehouse().getSpareParts());
        getCampaign().getShoppingList().removeZeroQuantityFromList(); // To
                                                                      // prevent
                                                                      // zero
                                                                      // quantity
                                                                      // from
                                                                      // hanging
                                                                      // around
        // get the selected row back for tasks
        if (selectedRow != -1) {
            boolean found = false;
            for (int i = 0; i < partsTable.getRowCount(); i++) {
                Part p = partsModel.getPartAt(partsTable.convertRowIndexToModel(i));
                if (p.getId() == partId) {
                    partsTable.setRowSelectionInterval(i, i);
                    partsTable.scrollRectToVisible(partsTable.getCellRect(i, 0, true));
                    found = true;
                    break;
                }
            }
            if (!found) {
                // then set to the current selected row
                if (partsTable.getRowCount() > 0) {
                    if (partsTable.getRowCount() <= selectedRow) {
                        selectedRow = partsTable.getRowCount() - 1;
                    }
                    partsTable.setRowSelectionInterval(selectedRow, selectedRow);
                }
            }
        }
    }

    private void doTask() {
        selectedTech = getSelectedTech();
        selectedRow = partsTable.getSelectedRow();

        Part part = getSelectedTask();
        if (null == part) {
            return;
        }
        if (null == selectedTech) {
            return;
        }
        partId = part.getId();

        Part repairable = getCampaign().fixWarehousePart(part, selectedTech);
        // if the break off part failed to be repaired, then follow it with
        // the focus
        // otherwise keep the focus on the current row
        if (repairable.needsFixing() && !repairable.isBeingWorkedOn()) {
            partId = repairable.getId();
        }
    }

    @Override
    public Person getSelectedTech() {
        int row = techTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        return techsModel.getTechAt(techTable.convertRowIndexToModel(row));
    }

    @Override
    public Part getSelectedTask() {
        int row = partsTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        return partsModel.getPartAt(partsTable.convertRowIndexToModel(row));
    }

    public void refreshAstechPool(String astechString) {
        astechPoolLabel.setText(astechString);
    }

    private ActionScheduler partsScheduler = new ActionScheduler(this::refreshPartsList);
    private ActionScheduler techsScheduler = new ActionScheduler(this::refreshTechsList);

    @Subscribe
    public void handle(UnitRemovedEvent ev) {
        filterParts();
    }

    @Subscribe
    public void handle(UnitChangedEvent ev) {
        filterParts();
    }

    @Subscribe
    public void handle(UnitRefitEvent ev) {
        partsScheduler.schedule();
    }

    @Subscribe
    public void handle(PersonEvent ev) {
        techsScheduler.schedule();
    }

    @Subscribe
    public void handle(PartNewEvent ev) {
        partsScheduler.schedule();
    }

    @Subscribe
    public void handle(PartRemovedEvent ev) {
        partsScheduler.schedule();
    }

    @Subscribe
    public void handle(PartChangedEvent ev) {
        filterParts();
    }

    @Subscribe
    public void handle(AcquisitionEvent ev) {
        partsScheduler.schedule();
    }

    @Subscribe
    public void handle(PartWorkEvent ev) {
        if (ev.getPartWork().getUnit() == null) {
            partsScheduler.schedule();
        }
        techsScheduler.schedule();
    }

    @Subscribe
    public void handle(OvertimeModeEvent ev) {
        filterTechs();
    }

    @Subscribe
    public void handle(AstechPoolChangedEvent ev) {
        filterTechs();
    }

    @Subscribe
    public void handle(PartModeChangedEvent ev) {
        updateTechTarget();
    }
}
