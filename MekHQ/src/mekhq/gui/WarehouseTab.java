/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui;

import static mekhq.campaign.parts.enums.PartQuality.QUALITY_A;
import static mekhq.campaign.personnel.skills.SkillUtilities.EXP_LEGENDARY;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Set;
import javax.swing.*;
import javax.swing.RowSorter.SortKey;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import megamek.client.ui.preferences.JComboBoxPreference;
import megamek.client.ui.preferences.JTablePreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.util.UIUtil;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.WeaponType;
import megamek.common.event.Subscribe;
import megamek.common.rolls.TargetRoll;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.events.AcquisitionEvent;
import mekhq.campaign.events.AsTechPoolChangedEvent;
import mekhq.campaign.events.OvertimeModeEvent;
import mekhq.campaign.events.parts.PartChangedEvent;
import mekhq.campaign.events.parts.PartModeChangedEvent;
import mekhq.campaign.events.parts.PartNewEvent;
import mekhq.campaign.events.parts.PartRemovedEvent;
import mekhq.campaign.events.parts.PartWorkEvent;
import mekhq.campaign.events.persons.PersonEvent;
import mekhq.campaign.events.units.UnitChangedEvent;
import mekhq.campaign.events.units.UnitRefitEvent;
import mekhq.campaign.events.units.UnitRemovedEvent;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.EnginePart;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.PartInUse;
import mekhq.campaign.parts.TankLocation;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.meks.MekActuator;
import mekhq.campaign.parts.meks.MekGyro;
import mekhq.campaign.parts.meks.MekLifeSupport;
import mekhq.campaign.parts.meks.MekLocation;
import mekhq.campaign.parts.meks.MekSensor;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.unit.Unit;
import mekhq.gui.adapter.PartsTableMouseAdapter;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.baseComponents.roundedComponents.RoundedMMToggleButton;
import mekhq.gui.dialog.PartsReportDialog;
import mekhq.gui.enums.MHQTabType;
import mekhq.gui.model.PartsTableModel;
import mekhq.gui.model.TechTableModel;
import mekhq.gui.panels.TutorialHyperlinkPanel;
import mekhq.gui.sorter.FormattedNumberSorter;
import mekhq.gui.sorter.PartsDetailSorter;
import mekhq.gui.sorter.TechSorter;
import mekhq.gui.sorter.WarehouseStatusSorter;
import mekhq.gui.utilities.JScrollPaneWithSpeed;

/**
 * Displays all spare parts in stock, parts on order, and permits repair of damaged parts.
 */
public final class WarehouseTab extends CampaignGuiTab implements ITechWorkPanel {
    private static final MMLogger LOGGER = MMLogger.create(WarehouseTab.class);

    // parts filter groups
    private static final int SG_ALL = 0;
    private static final int SG_ARMOR = 1;
    private static final int SG_SYSTEM = 2;
    private static final int SG_EQUIP = 3;
    private static final int SG_LOC = 4;
    private static final int SG_WEAPON = 5;
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
    private static final int SV_SPARE = 3;
    private static final int SV_UNDAMAGED = 4;
    private static final int SV_DAMAGED = 5;
    private static final int SV_NUM = 6;

    private JTable partsTable;
    private JTable techTable;
    private RoundedJButton btnDoTask;
    private RoundedMMToggleButton btnShowAllTechsWarehouse;
    private JLabel lblTargetNumWarehouse;
    private JTextArea textTargetWarehouse;
    private JLabel asTechPoolLabel;
    private JComboBox<String> choiceParts;
    private JComboBox<String> choicePartsView;

    private PartsTableModel partsModel;
    private TechTableModel techsModel;

    private TableRowSorter<PartsTableModel> partsSorter;
    private TableRowSorter<TechTableModel> techSorter;

    // remember current selections so they can be restored after refresh
    private int selectedRow = -1;
    private int partId = -1;
    private Person selectedTech;

    // region Constructors
    public WarehouseTab(CampaignGUI gui, String name) {
        super(gui, name);
        MekHQ.registerHandler(this);
        setUserPreferences();
    }
    // endregion Constructors

    /*
     * (non-Javadoc)
     *
     * @see mekhq.gui.CampaignGuiTab#initTab()
     */
    @Override
    public void initTab() {
        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CampaignGUI",
              MekHQ.getMHQOptions().getLocale());

        JPanel panSupplies = new JPanel(new GridBagLayout());
        panSupplies.setBorder(RoundedLineBorder.createRoundedLineBorder());

        RoundedJButton btnPartsReport = new RoundedJButton(resourceMap.getString("btnPartsReport.text"));
        btnPartsReport.setToolTipText(resourceMap.getString("btnPartsReport.toolTipText"));
        btnPartsReport.addActionListener(evt -> new PartsReportDialog(getCampaignGui(), true).setVisible(true));

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 0);
        panSupplies.add(new JLabel(resourceMap.getString("lblPartsChoice.text")), gridBagConstraints);

        DefaultComboBoxModel<String> partsGroupModel = new DefaultComboBoxModel<>();
        for (int i = 0; i < SG_NUM; i++) {
            partsGroupModel.addElement(getPartsGroupName(i));
        }
        choiceParts = new JComboBox<>(partsGroupModel);
        choiceParts.setSelectedIndex(0);
        choiceParts.addActionListener(ev -> filterParts());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 0);
        panSupplies.add(choiceParts, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 0);
        panSupplies.add(new JLabel(resourceMap.getString("lblPartsChoiceView.text")), gridBagConstraints);

        DefaultComboBoxModel<String> partsGroupViewModel = new DefaultComboBoxModel<>();
        for (int i = 0; i < SV_NUM; i++) {
            partsGroupViewModel.addElement(getPartsGroupViewName(i));
        }
        choicePartsView = new JComboBox<>(partsGroupViewModel);
        choicePartsView.setSelectedIndex(0);
        choicePartsView.addActionListener(ev -> filterParts());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 0);
        panSupplies.add(choicePartsView, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0; // expand for layout padding
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panSupplies.add(btnPartsReport, gridBagConstraints);

        Set<PartInUse> partsInUse = getCampaign().getPartsInUse(true, false, QUALITY_A);
        partsModel = new PartsTableModel(partsInUse);
        partsTable = new JTable(partsModel);
        partsSorter = new TableRowSorter<>(partsModel);
        partsSorter.setComparator(PartsTableModel.COL_COST, new FormattedNumberSorter());
        partsSorter.setComparator(PartsTableModel.COL_DETAIL, new PartsDetailSorter());
        partsSorter.setComparator(PartsTableModel.COL_STATUS, new WarehouseStatusSorter());
        partsSorter.setComparator(PartsTableModel.COL_TOTAL_COST, new FormattedNumberSorter());
        partsTable.setRowSorter(partsSorter);
        TableColumn column;
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
        PartsTableMouseAdapter.connect(getCampaignGui(), partsTable, partsModel);

        JScrollPane scrollPartsTable = new JScrollPaneWithSpeed(partsTable);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSupplies.add(scrollPartsTable, gridBagConstraints);

        JPanel panelDoTask = new JPanel(new GridBagLayout());

        btnDoTask = new RoundedJButton(resourceMap.getString("btnDoTask.text"));
        btnDoTask.setToolTipText(resourceMap.getString("btnDoTask.toolTipText"));
        btnDoTask.setEnabled(false);
        btnDoTask.setName("btnDoTask");
        btnDoTask.addActionListener(ev -> doTask());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        panelDoTask.add(btnDoTask, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = GridBagConstraints.SOUTH;
        panelDoTask.add(new JLabel(resourceMap.getString("lblTarget.text")), gridBagConstraints);

        lblTargetNumWarehouse = new JLabel(resourceMap.getString("lblTargetNum.text"));
        lblTargetNumWarehouse.setHorizontalAlignment(SwingConstants.CENTER);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = GridBagConstraints.NORTH;
        panelDoTask.add(lblTargetNumWarehouse, gridBagConstraints);

        textTargetWarehouse = new JTextArea();
        textTargetWarehouse.setColumns(20);
        textTargetWarehouse.setEditable(false);
        textTargetWarehouse.setLineWrap(true);
        textTargetWarehouse.setRows(5);
        textTargetWarehouse.setText("");
        textTargetWarehouse.setWrapStyleWord(true);
        textTargetWarehouse.setBorder(null);
        JScrollPane scrTargetWarehouse = new JScrollPaneWithSpeed(textTargetWarehouse);
        scrTargetWarehouse.setBorder(RoundedLineBorder.createRoundedLineBorder());

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        panelDoTask.add(scrTargetWarehouse, gridBagConstraints);

        btnShowAllTechsWarehouse = new RoundedMMToggleButton(resourceMap.getString("btnShowAllTechs.text"));
        btnShowAllTechsWarehouse.setToolTipText(resourceMap.getString("btnShowAllTechs.toolTipText"));
        btnShowAllTechsWarehouse.addActionListener(ev -> filterTechs());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        panelDoTask.add(btnShowAllTechsWarehouse, gridBagConstraints);

        techsModel = new TechTableModel(getCampaignGui(), this);
        techTable = new JTable(techsModel);
        techTable.setRowHeight(UIUtil.scaleForGUI(60));
        techTable.getColumnModel().getColumn(0).setCellRenderer(techsModel.getRenderer());
        techTable.getSelectionModel().addListSelectionListener(ev -> updateTechTarget());
        techSorter = new TableRowSorter<>(techsModel);
        techSorter.setComparator(0, new TechSorter());
        techTable.setRowSorter(techSorter);
        ArrayList<SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new SortKey(0, SortOrder.ASCENDING));
        techSorter.setSortKeys(sortKeys);
        JScrollPane scrollTechTable = new JScrollPaneWithSpeed(techTable);
        scrollTechTable.setBorder(RoundedLineBorder.createRoundedLineBorder());
        scrollTechTable.setMinimumSize(new Dimension(200, 200));
        scrollTechTable.setPreferredSize(new Dimension(300, 300));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        panelDoTask.add(scrollTechTable, gridBagConstraints);

        asTechPoolLabel = new JLabel("<html><b>AsTech Pool Minutes:</> " +
                                           getCampaign().getAsTechPoolMinutes() +
                                           " (" +
                                           getCampaign().getNumberAsTechs() +
                                           " AsTechs)</html>");
        asTechPoolLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panelDoTask.add(asTechPoolLabel, gridBagConstraints);

        JSplitPane splitWarehouse = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panSupplies, panelDoTask);
        splitWarehouse.setOneTouchExpandable(true);
        splitWarehouse.setResizeWeight(1.0);

        JPanel pnlTutorial = new TutorialHyperlinkPanel("warehouseTab");

        setLayout(new BorderLayout());
        add(splitWarehouse, BorderLayout.CENTER);
        add(pnlTutorial, BorderLayout.SOUTH);
    }

    /**
     * These need to be migrated to the Suite Constants / Suite Options Setup
     */
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(WarehouseTab.class);

            choiceParts.setName("partsType");
            preferences.manage(new JComboBoxPreference(choiceParts));

            choicePartsView.setName("partsView");
            preferences.manage(new JComboBoxPreference(choicePartsView));

            partsTable.setName("partsTable");
            preferences.manage(new JTablePreference(partsTable));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
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
    public MHQTabType tabType() {
        return MHQTabType.WAREHOUSE;
    }

    public void filterParts() {
        final int nGroup = choiceParts.getSelectedIndex();
        final int nGroupView = choicePartsView.getSelectedIndex();
        RowFilter<PartsTableModel, Integer> partsTypeFilter = new RowFilter<>() {
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
                    inGroup = (part instanceof Armor); // ProtoMekArmor and BAArmor are derived from Armor
                } else if (nGroup == SG_SYSTEM) {
                    inGroup = part instanceof MekGyro ||
                                    part instanceof EnginePart ||
                                    part instanceof MekActuator ||
                                    part instanceof MekLifeSupport ||
                                    part instanceof MekSensor;
                } else if (nGroup == SG_EQUIP) {
                    inGroup = part instanceof EquipmentPart;
                } else if (nGroup == SG_LOC) {
                    inGroup = part instanceof MekLocation || part instanceof TankLocation;
                } else if (nGroup == SG_WEAPON) {
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
                } else if (nGroupView == SV_SPARE) {
                    inView = part.isSpare();
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
        return switch (group) {
            case SG_ALL -> "All Parts";
            case SG_ARMOR -> "Armor";
            case SG_SYSTEM -> "System Components";
            case SG_EQUIP -> "Equipment";
            case SG_LOC -> "Locations";
            case SG_WEAPON -> "Weapons";
            case SG_AMMO -> "Ammunition";
            case SG_MISC -> "Miscellaneous Equipment";
            case SG_ENGINE -> "Engines";
            case SG_GYRO -> "Gyros";
            case SG_ACT -> "Actuators";
            default -> "?";
        };
    }

    public static String getPartsGroupViewName(int view) {
        return switch (view) {
            case SV_ALL -> "All";
            case SV_IN_TRANSIT -> "In Transit";
            case SV_RESERVED -> "Reserved for Refit/Repair";
            case SV_SPARE -> "Spares";
            case SV_UNDAMAGED -> "Undamaged";
            case SV_DAMAGED -> "Damaged";
            default -> "?";
        };
    }

    public void filterTechs() {
        final Part part = getSelectedTask();
        RowFilter<TechTableModel, Integer> techTypeFilter = new RowFilter<>() {
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
                } else if (part.getSkillMin() > EXP_LEGENDARY) {
                    return false;
                } else if (tech.getMinutesLeft() <= 0) {
                    return false;
                } else {
                    return getCampaign().getCampaignOptions().isDestroyByMargin() ||
                                 (part.getSkillMin() <=
                                        (skill.getExperienceLevel(tech.getOptions(), tech.getATOWAttributes()) -
                                               modePenalty));
                }
            }
        };
        techSorter.setRowFilter(techTypeFilter);
    }

    private void updateTechTarget() {
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
        // The next gets all techs who have more than 0 minutes free, and sorted by
        // skill descending (elites at bottom)
        techsModel.setData(getCampaign().getTechs(true));
        String astechString = "<html><b>AsTech Pool Minutes:</> " + getCampaign().getAsTechPoolMinutes();
        if (getCampaign().isOvertimeAllowed()) {
            astechString += " [" + getCampaign().getAsTechPoolOvertime() + " overtime]";
        }
        astechString += " (" + getCampaign().getNumberAsTechs() + " AsTechs)</html>";
        refreshAsTechPool(astechString);

        // If requested, switch to top entry
        if ((null == selectedTech || getCampaign().getCampaignOptions().isResetToFirstTech()) &&
                  techTable.getRowCount() > 0) {
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

    public void refreshAsTechPool(String astechString) {
        asTechPoolLabel.setText(astechString);
    }

    private final ActionScheduler partsScheduler = new ActionScheduler(this::refreshPartsList);
    private final ActionScheduler techsScheduler = new ActionScheduler(this::refreshTechsList);

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
    public void handle(AsTechPoolChangedEvent ev) {
        filterTechs();
    }

    @Subscribe
    public void handle(PartModeChangedEvent ev) {
        updateTechTarget();
    }
}
