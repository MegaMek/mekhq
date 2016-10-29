package mekhq.gui.dialog;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import megamek.common.Aero;
import megamek.common.Mech;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.CampaignOptions.MassRepairOption;
import mekhq.campaign.event.OptionsChangedEvent;
import mekhq.campaign.parts.MekLocation;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.WorkTime;
import mekhq.gui.CampaignGUI;
import mekhq.gui.model.UnitTableModel;
import mekhq.gui.model.XTableColumnModel;
import mekhq.gui.sorter.UnitStatusSorter;
import mekhq.gui.sorter.UnitTypeSorter;

/**
 * @author Kipsta
 *
 */
public class MassRepairSalvageDialog extends JDialog {
	private static final long serialVersionUID = -7859207613578378162L;

	private CampaignGUI campaignGUI;
	private Unit selectedUnit;
	private CampaignOptions campaignOptions;

	private UnitTableModel unitTableModel;
	private JTable unitTable;
	private TableRowSorter<UnitTableModel> unitSorter;

	JScrollPane scrollUnitList;

	private JButton btnStart = null;
	private JButton btnSaveAsDefault = null;
	private JButton btnCancel = null;

	private JButton btnSelectNone = null;
	private JButton btnSelectAssigned = null;
	private JButton btnSelectUnassigned = null;

	private JCheckBox useExtraTimeBox;
	private JCheckBox useRushJobBox;
	private JCheckBox allowCarryoverBox;

	private MassRepairOptionControl[] massRepairOptionControls = null;

	ArrayList<Unit> unitList = null;

	public MassRepairSalvageDialog(Frame _parent, boolean _modal, CampaignGUI _campaignGUI, Unit _selectedUnit) {
		super(_parent, _modal);
		this.campaignGUI = _campaignGUI;
		this.selectedUnit = _selectedUnit;

		campaignOptions = campaignGUI.getCampaign().getCampaignOptions();

		initData();
		initComponents();

		unitTableModel.setData(unitList);

		if (null != selectedUnit) {
			int unitCount = unitTable.getRowCount();

			for (int i = 0; i < unitCount; i++) {
				int rowIdx = unitTable.convertRowIndexToModel(i);
				Unit unit = unitTableModel.getUnit(rowIdx);

				if (null == unit) {
					continue;
				}

				if (unit.getId().toString().equals(selectedUnit.getId().toString())) {
					unitTable.addRowSelectionInterval(i, i);
					break;
				}
			}
		}

		setLocationRelativeTo(_parent);
	}

	private void initData() {
		unitList = new ArrayList<Unit>();

		for (int i = 0; i < campaignGUI.getCampaign().getServiceableUnits().size(); i++) {
			Unit unit = campaignGUI.getCampaign().getServiceableUnits().get(i);

			if (unit.isSelfCrewed() || !unit.isAvailable()) {
				continue;
			}

			if ((unit.getEntity() instanceof Tank) || (unit.getEntity() instanceof Aero)
					|| (unit.getEntity() instanceof Mech)) {
				unitList.add(unit);
			}
		}
	}

	private void initComponents() {
		ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.MassRepair", new EncodeControl());

		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		setTitle("Mass Repair/Salvage");

		final Container content = getContentPane();
		content.setLayout(new GridBagLayout());

		JPanel pnlMain = new JPanel();
		pnlMain.setLayout(new GridBagLayout());

		pnlMain.add(createUnitsPanel(), createBaseConstraints(0));
		pnlMain.add(createUnitActionButtons(), createBaseConstraints(1));
		pnlMain.add(createOptionsPanel(resourceMap), createBaseConstraints(2));

		content.add(pnlMain, createBaseConstraints(0));
		content.add(createActionButtons(), createBaseConstraints(1));

		pack();
	}

	private Object createBaseConstraints(int rowIdx) {
		GridBagConstraints gridBagConstraints = null;
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = rowIdx;
		gridBagConstraints.weightx = 1;
		gridBagConstraints.weighty = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;

		return gridBagConstraints;
	}

	private JPanel createUnitsPanel() {
		JPanel pnlUnits = new JPanel(new GridBagLayout());
		pnlUnits.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Units"),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));

		unitTableModel = new UnitTableModel(campaignGUI.getCampaign());

		unitSorter = new TableRowSorter<UnitTableModel>(unitTableModel);
		unitSorter.setComparator(UnitTableModel.COL_STATUS, new UnitStatusSorter());
		unitSorter.setComparator(UnitTableModel.COL_TYPE, new UnitTypeSorter());

		ArrayList<RowSorter.SortKey> sortKeys = new ArrayList<RowSorter.SortKey>();
		sortKeys.add(new RowSorter.SortKey(UnitTableModel.COL_TYPE, SortOrder.DESCENDING));
		unitSorter.setSortKeys(sortKeys);

		unitTable = new JTable(unitTableModel);
		unitTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		unitTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		unitTable.setColumnModel(new XTableColumnModel());
		unitTable.createDefaultColumnsFromModel();
		unitTable.setRowSorter(unitSorter);

		TableColumn column = null;

		for (int i = 0; i < UnitTableModel.N_COL; i++) {
			column = ((XTableColumnModel) unitTable.getColumnModel()).getColumnByModelIndex(i);
			column.setPreferredWidth(unitTableModel.getColumnWidth(i));
			column.setCellRenderer(unitTableModel.getRenderer(false, null));

			if (i != UnitTableModel.COL_NAME && i != UnitTableModel.COL_STATUS && i != UnitTableModel.COL_TYPE) {
				((XTableColumnModel) unitTable.getColumnModel()).setColumnVisible(column, false);
			}
		}

		unitTable.setIntercellSpacing(new Dimension(0, 0));
		unitTable.setShowGrid(false);

		scrollUnitList = new JScrollPane(unitTable);
		scrollUnitList.setMinimumSize(new java.awt.Dimension(350, 200));
		scrollUnitList.setPreferredSize(new java.awt.Dimension(350, 200));

		GridBagConstraints gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;

		pnlUnits.add(scrollUnitList, gridBagConstraints);

		return pnlUnits;
	}

	private JPanel createOptionsPanel(ResourceBundle resourceMap) {
		JPanel pnlOptions = new JPanel(new GridBagLayout());
		pnlOptions.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Options"),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));

		useExtraTimeBox = new JCheckBox();
		useRushJobBox = new JCheckBox();
		allowCarryoverBox = new JCheckBox();

		GridBagConstraints gridBagConstraints = null;

		useExtraTimeBox.setText(resourceMap.getString("useExtraTimeBox.text"));
		useExtraTimeBox.setToolTipText(resourceMap.getString("useExtraTimeBox.toolTipText"));
		useExtraTimeBox.setName("massRepairUseExtraTimeBox");
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.weightx = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlOptions.add(useExtraTimeBox, gridBagConstraints);

		useRushJobBox.setText(resourceMap.getString("useRushJobBox.text"));
		useRushJobBox.setToolTipText(resourceMap.getString("useRushJobBox.toolTipText"));
		useRushJobBox.setName("massRepairUseRushJobBox");
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlOptions.add(useRushJobBox, gridBagConstraints);

		allowCarryoverBox.setText(resourceMap.getString("allowCarryoverBox.text"));
		allowCarryoverBox.setToolTipText(resourceMap.getString("allowCarryoverBox.toolTipText"));
		allowCarryoverBox.setName("massRepairAllowCarryoverBox");
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlOptions.add(allowCarryoverBox, gridBagConstraints);

		JPanel pnlItems = new JPanel(new GridBagLayout());
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.weightx = 0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new Insets(10, 0, 0, 0);
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		pnlOptions.add(pnlItems, gridBagConstraints);

		useExtraTimeBox.setSelected(campaignOptions.massRepairUseExtraTime());
		useRushJobBox.setSelected(campaignOptions.massRepairUseRushJob());
		allowCarryoverBox.setSelected(campaignOptions.massRepairAllowCarryover());

		JLabel lbl = new JLabel("Item");
		Font boldFont = new Font(lbl.getFont().getFontName(), Font.BOLD, lbl.getFont().getSize());

		lbl.setFont(boldFont);

		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.insets = new Insets(0, 5, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		pnlItems.add(lbl, gridBagConstraints);

		lbl = new JLabel("Min Skill");
		lbl.setFont(boldFont);

		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.insets = new Insets(0, 5, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		pnlItems.add(lbl, gridBagConstraints);

		lbl = new JLabel("Max Skill");
		lbl.setFont(boldFont);

		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.insets = new Insets(0, 5, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		pnlItems.add(lbl, gridBagConstraints);

		lbl = new JLabel("Min BTH");
		lbl.setFont(boldFont);

		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 3;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.insets = new Insets(0, 5, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		pnlItems.add(lbl, gridBagConstraints);

		lbl = new JLabel("Max BTH");
		lbl.setFont(boldFont);

		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 4;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.insets = new Insets(0, 5, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		pnlItems.add(lbl, gridBagConstraints);

		int rowIdx = 1;

		massRepairOptionControls = new MassRepairOptionControl[MassRepairOption.VALID_REPAIR_TYPES.length];
		massRepairOptionControls[Part.REPAIR_PART_TYPE.ARMOR] = createMassRepairOptionControls(
				Part.REPAIR_PART_TYPE.ARMOR, "Repair/Salvage Armor", "Allow mass repair/salvage of armor",
				"massRepairItemArmor", pnlItems, rowIdx++);
		massRepairOptionControls[Part.REPAIR_PART_TYPE.AMMO] = createMassRepairOptionControls(
				Part.REPAIR_PART_TYPE.AMMO, "Repair/Salvage Ammo", "Allow mass repair/salvage of ammo",
				"massRepairItemAmmo", pnlItems, rowIdx++);
		massRepairOptionControls[Part.REPAIR_PART_TYPE.WEAPON] = createMassRepairOptionControls(
				Part.REPAIR_PART_TYPE.WEAPON, "Repair/Salvage Weapons", "Allow mass repair/salvage of weapons",
				"massRepairItemWeapons", pnlItems, rowIdx++);
		massRepairOptionControls[Part.REPAIR_PART_TYPE.GENERAL_LOCATION] = createMassRepairOptionControls(
				Part.REPAIR_PART_TYPE.GENERAL_LOCATION, "Repair/Salvage Locations",
				"Allow mass repair/salvage of mek body parts and vehicle locations", "massRepairItemLocations",
				pnlItems, rowIdx++);
		massRepairOptionControls[Part.REPAIR_PART_TYPE.ENGINE] = createMassRepairOptionControls(
				Part.REPAIR_PART_TYPE.ENGINE, "Repair/Salvage Engines", "Allow mass repair/salvage of engines",
				"massRepairItemEngines", pnlItems, rowIdx++);
		massRepairOptionControls[Part.REPAIR_PART_TYPE.GYRO] = createMassRepairOptionControls(
				Part.REPAIR_PART_TYPE.GYRO, "Repair/Salvage Gyros", "Allow mass repair/salvage of gyros",
				"massRepairItemGyros", pnlItems, rowIdx++);
		massRepairOptionControls[Part.REPAIR_PART_TYPE.ACTUATOR] = createMassRepairOptionControls(
				Part.REPAIR_PART_TYPE.ACTUATOR, "Repair/Salvage Actuators", "Allow mass repair/salvage of actuators",
				"massRepairItemActuators", pnlItems, rowIdx++);
		massRepairOptionControls[Part.REPAIR_PART_TYPE.ELECTRONICS] = createMassRepairOptionControls(
				Part.REPAIR_PART_TYPE.ELECTRONICS, "Repair/Salvage Cockpits/Sensors/Life Support",
				"Allow mass repair/salvage of cockpits, life support, and sensors", "massRepairItemHead", pnlItems,
				rowIdx++);
		massRepairOptionControls[Part.REPAIR_PART_TYPE.GENERAL] = createMassRepairOptionControls(
				Part.REPAIR_PART_TYPE.GENERAL, "Repair/Salvage Other",
				"Allow mass repair/salvage of items which do not fall into the specific categories",
				"massRepairItemOther", pnlItems, rowIdx++);

		return pnlOptions;
	}

	private MassRepairOptionControl createMassRepairOptionControls(int type, String text, String tooltipText,
			String activeBoxName, JPanel pnlItems, int rowIdx) {
		MassRepairOption mro = null;

		List<MassRepairOption> mroList = campaignOptions.getMassRepairOptions();

		if (null != mroList) {
			for (int i = 0; i < mroList.size(); i++) {
				if (mroList.get(i).getType() == type) {
					mro = mroList.get(i);
					break;
				}
			}
		}

		if (null == mro) {
			mro = new MassRepairOption(type);
		}

		int columnIdx = 0;

		MassRepairOptionControl mroc = new MassRepairOptionControl();
		mroc.activeBox = createMassRepairOptionItemBox(text, tooltipText, activeBoxName, mro.isActive(), pnlItems,
				rowIdx, columnIdx++);
		mroc.minSkillCBox = createMassRepairSkillCBox(mro.getSkillMin(), mro.isActive(), pnlItems, rowIdx, columnIdx++);
		mroc.maxSkillCBox = createMassRepairSkillCBox(mro.getSkillMax(), mro.isActive(), pnlItems, rowIdx, columnIdx++);
		mroc.minBTHSpn = createMassRepairSkillBTHSpinner(mro.getBthMin(), mro.isActive(), pnlItems, rowIdx,
				columnIdx++);
		mroc.maxBTHSpn = createMassRepairSkillBTHSpinner(mro.getBthMax(), mro.isActive(), pnlItems, rowIdx,
				columnIdx++);

		mroc.activeBox.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				if (mroc.activeBox.isSelected()) {
					mroc.minSkillCBox.setEnabled(true);
					mroc.maxSkillCBox.setEnabled(true);
					mroc.minBTHSpn.setEnabled(true);
					mroc.maxBTHSpn.setEnabled(true);
				} else {
					mroc.minSkillCBox.setEnabled(false);
					mroc.maxSkillCBox.setEnabled(false);
					mroc.minBTHSpn.setEnabled(false);
					mroc.maxBTHSpn.setEnabled(false);
				}
			}
		});

		return mroc;
	}

	private JSpinner createMassRepairSkillBTHSpinner(int selectedValue, boolean enabled, JPanel pnlItems, int rowIdx,
			int columnIdx) {
		JSpinner skillBTHSpn = new JSpinner(new SpinnerNumberModel(selectedValue, 1, 12, 1));
		((JSpinner.DefaultEditor) skillBTHSpn.getEditor()).getTextField().setEditable(false);
		skillBTHSpn.setEnabled(enabled);

		GridBagConstraints gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = columnIdx;
		gridBagConstraints.gridy = rowIdx;
		gridBagConstraints.insets = new Insets(0, 5, 0, 5);
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;

		pnlItems.add(skillBTHSpn, gridBagConstraints);

		return skillBTHSpn;
	}

	private JComboBox<String> createMassRepairSkillCBox(int selectedValue, boolean enabled, JPanel pnlItems, int rowIdx,
			int columnIdx) {
		DefaultComboBoxModel<String> skillModel = new DefaultComboBoxModel<String>();
		skillModel.addElement(SkillType.getExperienceLevelName(SkillType.EXP_ULTRA_GREEN));
		skillModel.addElement(SkillType.getExperienceLevelName(SkillType.EXP_GREEN));
		skillModel.addElement(SkillType.getExperienceLevelName(SkillType.EXP_REGULAR));
		skillModel.addElement(SkillType.getExperienceLevelName(SkillType.EXP_VETERAN));
		skillModel.addElement(SkillType.getExperienceLevelName(SkillType.EXP_ELITE));
		skillModel.setSelectedItem(SkillType.getExperienceLevelName(selectedValue));
		JComboBox<String> skillCBox = new JComboBox<String>(skillModel);
		skillCBox.setEnabled(enabled);

		GridBagConstraints gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = columnIdx;
		gridBagConstraints.gridy = rowIdx;
		gridBagConstraints.insets = new Insets(0, 5, 0, 5);
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;

		pnlItems.add(skillCBox, gridBagConstraints);

		return skillCBox;
	}

	private JCheckBox createMassRepairOptionItemBox(String text, String toolTipText, String name, boolean selected,
			JPanel pnlItems, int rowIdx, int columnIdx) {
		JCheckBox optionItemBox = new JCheckBox();
		optionItemBox.setText(text);
		optionItemBox.setToolTipText(toolTipText);
		optionItemBox.setName(name);
		optionItemBox.setSelected(selected);

		GridBagConstraints gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = columnIdx;
		gridBagConstraints.gridy = rowIdx;
		gridBagConstraints.insets = new Insets(0, 0, 0, 5);
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;

		pnlItems.add(optionItemBox, gridBagConstraints);

		return optionItemBox;
	}

	private JPanel createUnitActionButtons() {
		int unitCount = unitList.size();
		int activeCount = 0;
		int inactiveCount = 0;

		for (int i = 0; i < unitCount; i++) {
			Unit unit = unitList.get(i);

			if (null == unit) {
				continue;
			}

			if ((null == unit.getActiveCrew()) || unit.getActiveCrew().isEmpty()) {
				inactiveCount++;
			} else {
				activeCount++;
			}
		}

		JPanel pnlButtons = new JPanel();

		int btnIdx = 0;
		GridBagConstraints gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = btnIdx++;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);

		btnSelectNone = new JButton();
		btnSelectNone.setText("Unselect All"); // NOI18N
		btnSelectNone.setToolTipText("Unselect all units");
		btnSelectNone.setName("btnSelectNone"); // NOI18N
		btnSelectNone.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnUnitsSelectNoneActionPerformed(evt);
			}
		});

		pnlButtons.add(btnSelectNone, gridBagConstraints);

		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = btnIdx++;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);

		btnSelectAssigned = new JButton();
		btnSelectAssigned.setText("Select Active Units (" + activeCount + ")"); // NOI18N
		btnSelectAssigned.setToolTipText("Select units with assigned pilots/crews");
		btnSelectAssigned.setName("btnSelectAssigned"); // NOI18N
		btnSelectAssigned.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnUnitsSelectAssignedActionPerformed(evt);
			}
		});

		pnlButtons.add(btnSelectAssigned, gridBagConstraints);

		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = btnIdx++;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);

		btnSelectUnassigned = new JButton();
		btnSelectUnassigned.setText("Select Inactive Units (" + inactiveCount + ")"); // NOI18N
		btnSelectUnassigned.setToolTipText("Select units without assigned pilots/crews");
		btnSelectUnassigned.setName("btnSelectUnassigned"); // NOI18N
		btnSelectUnassigned.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnUnitsSelectUnassignedActionPerformed(evt);
			}
		});

		pnlButtons.add(btnSelectUnassigned, gridBagConstraints);

		return pnlButtons;
	}

	private void btnUnitsSelectNoneActionPerformed(ActionEvent evt) {
		unitTable.removeRowSelectionInterval(0, unitTable.getRowCount() - 1);
	}

	private void btnUnitsSelectAssignedActionPerformed(ActionEvent evt) {
		int unitCount = unitTable.getRowCount();

		for (int i = 0; i < unitCount; i++) {
			int rowIdx = unitTable.convertRowIndexToModel(i);
			Unit unit = unitTableModel.getUnit(rowIdx);

			if (null == unit) {
				continue;
			}

			if ((null == unit.getActiveCrew()) || unit.getActiveCrew().isEmpty()) {
				continue;
			}

			unitTable.addRowSelectionInterval(i, i);
		}
	}

	private void btnUnitsSelectUnassignedActionPerformed(ActionEvent evt) {
		int unitCount = unitTable.getRowCount();

		for (int i = 0; i < unitCount; i++) {
			int rowIdx = unitTable.convertRowIndexToModel(i);
			Unit unit = unitTableModel.getUnit(rowIdx);

			if (null == unit) {
				continue;
			}

			if ((null == unit.getActiveCrew()) || unit.getActiveCrew().isEmpty()) {
				unitTable.addRowSelectionInterval(i, i);
			}
		}
	}

	private JPanel createActionButtons() {
		JPanel pnlButtons = new JPanel();

		int btnIdx = 0;

		GridBagConstraints gridBagConstraints = null;

		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = btnIdx++;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);

		btnStart = new JButton();
		btnStart.setText("Start Mass Repair/Salvage"); // NOI18N
		btnStart.setName("btnStart"); // NOI18N
		btnStart.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnStartMassRepairActionPerformed(evt);
			}
		});

		pnlButtons.add(btnStart, gridBagConstraints);

		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = btnIdx++;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);

		btnSaveAsDefault = new JButton();
		btnSaveAsDefault.setText("Save Options as Default"); // NOI18N
		btnSaveAsDefault.setName("btnSaveAsDefault"); // NOI18N
		btnSaveAsDefault.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnSaveAsDefaultActionPerformed(evt);
			}
		});

		pnlButtons.add(btnSaveAsDefault, gridBagConstraints);

		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = btnIdx++;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);

		btnCancel = new JButton();
		btnCancel.setText("Cancel"); // NOI18N
		btnCancel.setName("btnClose"); // NOI18N
		btnCancel.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnCancelActionPerformed(evt);
			}
		});

		pnlButtons.add(btnCancel, gridBagConstraints);

		return pnlButtons;
	}

	private void btnStartMassRepairActionPerformed(ActionEvent evt) {
		int[] selectedRows = unitTable.getSelectedRows();

		if ((null == selectedRows) || (selectedRows.length == 0)) {
			JOptionPane.showMessageDialog(this, "Can not started Mass Repair/Salvage if there are no selected units.",
					"No selected unit", JOptionPane.ERROR_MESSAGE);
			return;
		}

		try {
			List<Unit> units = new ArrayList<Unit>();

			for (int i = 0; i < selectedRows.length; i++) {
				int rowIdx = unitTable.convertRowIndexToModel(selectedRows[i]);
				Unit unit = unitTableModel.getUnit(rowIdx);

				if (null == unit) {
					continue;
				}

				units.add(unit);
			}

			if (units.isEmpty()) {
				JOptionPane.showMessageDialog(this, "No valid units selected", "No units", JOptionPane.ERROR_MESSAGE);
				return;
			}

			List<MassRepairOption> activeMROs = new ArrayList<MassRepairOption>();

			for (int i = 0; i < MassRepairOption.VALID_REPAIR_TYPES.length; i++) {
				int type = MassRepairOption.VALID_REPAIR_TYPES[i];

				MassRepairOptionControl mroc = massRepairOptionControls[i];

				if (!mroc.activeBox.isSelected()) {
					continue;
				}

				MassRepairOption mro = new MassRepairOption();
				mro.setType(type);
				mro.setActive(mroc.activeBox.isSelected());
				mro.setSkillMin(mroc.minSkillCBox.getSelectedIndex());
				mro.setSkillMax(mroc.maxSkillCBox.getSelectedIndex());
				mro.setBthMin((Integer) mroc.minBTHSpn.getValue());
				mro.setBthMax((Integer) mroc.maxBTHSpn.getValue());

				activeMROs.add(mro);
			}

			if (activeMROs.isEmpty()) {
				JOptionPane.showMessageDialog(this,
						"No repair options are currently enabled. Please activate at least one type of item to repair.",
						"No repair options", JOptionPane.ERROR_MESSAGE);
				return;
			}

			btnStart.setEnabled(false);
			btnSaveAsDefault.setEnabled(false);
			btnCancel.setEnabled(false);

			btnSelectNone.setEnabled(false);
			btnSelectAssigned.setEnabled(false);
			btnSelectUnassigned.setEnabled(false);
			
			for (Unit unit : units) {
				performMassRepairOrSalvage(unit, unit.isSalvage(), activeMROs);
			}
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this,
					"An error occurred while trying to perform the mass repair/salvage",
					"Error occurred", JOptionPane.ERROR_MESSAGE);
		} finally {
			btnStart.setEnabled(true);
			btnSaveAsDefault.setEnabled(true);
			btnCancel.setEnabled(true);

			btnSelectNone.setEnabled(true);
			btnSelectAssigned.setEnabled(true);
			btnSelectUnassigned.setEnabled(true);
		}
	}

	private void btnSaveAsDefaultActionPerformed(ActionEvent evt) {
		updateOptions();
	}

	private void btnCancelActionPerformed(ActionEvent evt) {
		this.setVisible(false);
	}

	private void performMassRepairOrSalvage(Unit unit, boolean isSalvage, List<MassRepairOption> mroList) {
		String actionDescriptor = isSalvage ? "salvage" : "repair";
		Campaign campaign = campaignGUI.getCampaign();

		campaign.addReport(String.format("Beginning mass %s on %s.", actionDescriptor, unit.getName()));

		ArrayList<Person> techs = campaign.getTechs(true);

		if (techs.isEmpty()) {
			campaign.addReport(String.format("No available techs to %s parts %s %s.", actionDescriptor,
					isSalvage ? "from" : "on", unit.getName()));
		} else {
			// Filter our tech list to only our techs that can work on this unit
			for (int i = techs.size() - 1; i >= 0; i--) {
				Person tech = techs.get(i);

				if (!tech.canTech(unit.getEntity())) {
					techs.remove(i);
				}
			}

			Map<Integer, MassRepairOption> mroByTypeMap = new HashMap<Integer, MassRepairOption>();

			for (int i = 0; i < mroList.size(); i++) {
				mroByTypeMap.put(mroList.get(i).getType(), mroList.get(i));
			}

			/*
			 * Possibly call this multiple times. Sometimes some actions are
			 * first dependent upon others being finished, also failed actions
			 * can be performed again by a tech with a higher skill.
			 */
			int totalActionsPerformed = 0;
			int actionsPerformed = 1;

			while (actionsPerformed > 0) {
				actionsPerformed = performMassTechAction(unit, techs, mroByTypeMap, isSalvage);
				totalActionsPerformed += actionsPerformed;
			}

			campaign.addReport(String.format("Mass %s complete on %s. %d total actions performed.", actionDescriptor,
					unit.getName(), totalActionsPerformed));
		}

		campaignGUI.refreshReport();
	}

	private int performMassTechAction(Unit unit, List<Person> techs, Map<Integer, MassRepairOption> mroByTypeMap,
			boolean salvaging) {
		Campaign campaign = campaignGUI.getCampaign();
		int totalActionsPerformed = 0;
		String actionDescriptor = salvaging ? "salvage" : "repair";

		if (techs.isEmpty()) {
			campaign.addReport(
					String.format("Unable to %s any more parts from %s because there are no available techs.",
							actionDescriptor, unit.getName()));
			return totalActionsPerformed;
		}

		List<Part> parts = campaignGUI.getCampaign().getPartsNeedingServiceFor(unit.getId());

		/*
		 * Pre checking for hips/shoulders on repairable meks. If we have a bad
		 * hip or shoulder, we're not going to do anything until we get those
		 * parts out of the location and scrap it. Once we're at a happy place,
		 * we'll proceed.
		 */

		if ((unit.getEntity() instanceof Mech) && !salvaging) {
			for (Part part : parts) {
				if ((part instanceof MekLocation) && part.onBadHipOrShoulder()) {
					campaign.addReport(String.format(
							"Unable to repair any more parts on %s because the %s has a bad hip/shoulder.",
							unit.getName(), part.getName()));

					return 0;
				}
			}
		}

		/*
		 * Filter our parts list to only those that aren't being worked on or
		 * those that meet our criteria as defined in the campaign
		 * configurations
		 */
		for (int i = parts.size() - 1; i >= 0; i--) {
			Part part = parts.get(i);

			if (part.isBeingWorkedOn()) {
				parts.remove(i);
			} else {
				int repairType = Part.findCorrectMassRepairType(part);

				MassRepairOption mro = mroByTypeMap.get(repairType);

				if ((null == mro) || !mro.isActive()) {
					parts.remove(i);
				}
			}
		}

		if (parts.isEmpty()) {
			campaign.addReport(
					String.format("Unable to %s any more parts from %s because there are no valid parts left to %s.",
							actionDescriptor, unit.getName(), actionDescriptor));
			return totalActionsPerformed;
		}

		for (Part part : parts) {
			if (techs.isEmpty()) {
				campaign.addReport(
						String.format("Unable to %s any more parts from %s because there are no available techs.",
								actionDescriptor, unit.getName()));
				continue;
			}

			int modePenalty = part.getMode().expReduction;

			// Search the list of techs each time for a variety of checks. We'll
			// create a temporary truncated list of techs
			List<Person> validTechs = new ArrayList<Person>();
			Map<String, WorkTime> techToWorktimeMap = new HashMap<String, WorkTime>();

			for (int i = techs.size() - 1; i >= 0; i--) {
				// Reset our WorkTime back to normal so that we can adjust as
				// necessary
				WorkTime selectedWorktime = WorkTime.NORMAL;
				part.setMode(WorkTime.of(selectedWorktime.id));

				Person tech = techs.get(i);
				Skill skill = tech.getSkillForWorkingOn(part);
				MassRepairOption mro = mroByTypeMap.get(Part.findCorrectMassRepairType(part));

				if (null == mro) {
					continue;
				}

				if (mro.getSkillMin() > skill.getExperienceLevel()) {
					continue;
				}

				if (mro.getSkillMax() < skill.getExperienceLevel()) {
					continue;
				}

				if (part.getSkillMin() > (skill.getExperienceLevel() - modePenalty)) {
					continue;
				}

				if (tech.getMinutesLeft() <= 0) {
					continue;
				}

				// Check if we can actually even repair this part
				TargetRoll targetRoll = campaign.getTargetFor(part, tech);

				if ((targetRoll.getValue() == TargetRoll.IMPOSSIBLE)
						|| (targetRoll.getValue() == TargetRoll.AUTOMATIC_FAIL)
						|| (targetRoll.getValue() == TargetRoll.CHECK_FALSE)) {
					continue;
				}

				// Check if we need to increase the time to meet the min BTH
				if (targetRoll.getValue() > mro.getBthMin()) {
					if (!campaign.getCampaignOptions().massRepairUseExtraTime()) {
						continue;
					}

					WorkTime newWorkTime = calculateNewMassRepairWorktime(part, tech, mro, campaign, true);

					if (null == newWorkTime) {
						continue;
					}

					selectedWorktime = newWorkTime;
				} else if (targetRoll.getValue() < mro.getBthMax()) {
					// Or decrease the time to meet the max BTH
					if (campaign.getCampaignOptions().massRepairUseRushJob()) {
						WorkTime newWorkTime = calculateNewMassRepairWorktime(part, tech, mro, campaign, false);

						// This should never happen, but...
						if (null != newWorkTime) {
							selectedWorktime = newWorkTime;
						}
					}
				}

				if ((tech.getMinutesLeft() < part.getActualTime())
						&& !campaign.getCampaignOptions().massRepairAllowCarryover()) {
					continue;
				}

				validTechs.add(tech);
				techToWorktimeMap.put(tech.getId().toString(), selectedWorktime);

				part.setMode(WorkTime.of(WorkTime.NORMAL.id));
			}

			if (!validTechs.isEmpty()) {
				/*
				 * Sort the valid techs by applicable skill. Let's start with
				 * the least experienced and work our way up until we find
				 * someone who can perform the work. If we have two techs with
				 * the same skill, put the one with the lesser XP in the front.
				 */
				Collections.sort(validTechs, new Comparator<Person>() {
					@Override
					public int compare(Person tech1, Person tech2) {
						Skill skill1 = tech1.getSkillForWorkingOn(part);
						Skill skill2 = tech2.getSkillForWorkingOn(part);

						if (skill1.getExperienceLevel() == skill2.getExperienceLevel()) {
							if (tech1.getXp() == tech2.getXp()) {
								return 0;
							}

							return tech1.getXp() < tech2.getXp() ? -1 : 1;
						}

						return skill1.getExperienceLevel() < skill2.getExperienceLevel() ? -1 : 1;
					}
				});

				Person tech = validTechs.get(0);
				WorkTime wt = techToWorktimeMap.get(tech.getId().toString());

				part.setMode(wt);

				campaign.fixPart(part, tech);

				totalActionsPerformed++;

				// If this tech has no time left, filter them out so we don't
				// spend cycles on them in the future
				if (tech.getMinutesLeft() <= 0) {
					techs.remove(tech);
				}

				Thread.yield();
			}
		}

		return totalActionsPerformed;
	}

	private WorkTime calculateNewMassRepairWorktime(Part part, Person tech, MassRepairOption mro, Campaign campaign,
			boolean increaseTime) {
		WorkTime newWorkTime = part.getMode();
		WorkTime previousNewWorkTime = newWorkTime;
		TargetRoll targetRoll = campaign.getTargetFor(part, tech);

		while (null != newWorkTime) {
			previousNewWorkTime = newWorkTime;
			newWorkTime = newWorkTime.moveTimeToNextLevel(increaseTime);

			if (null == newWorkTime) {
				if (!increaseTime) {
					return previousNewWorkTime;
				} else {
					return null;
				}
			}

			part.setMode(newWorkTime);

			targetRoll = campaign.getTargetFor(part, tech);

			if ((targetRoll.getValue() == TargetRoll.IMPOSSIBLE) || (targetRoll.getValue() == TargetRoll.AUTOMATIC_FAIL)
					|| (targetRoll.getValue() == TargetRoll.CHECK_FALSE)) {
				continue;
			}

			if (increaseTime) {
				if (targetRoll.getValue() <= mro.getBthMin()) {
					return newWorkTime;
				}
			} else {
				if (targetRoll.getValue() > mro.getBthMax()) {
					return previousNewWorkTime;
				}

				return newWorkTime;
			}
		}

		return null;
	}

	private void updateOptions() {
		campaignOptions.setMassRepairUseExtraTime(useExtraTimeBox.isSelected());
		campaignOptions.setMassRepairUseRushJob(useRushJobBox.isSelected());
		campaignOptions.setMassRepairAllowCarryover(allowCarryoverBox.isSelected());

		for (int i = 0; i < MassRepairOption.VALID_REPAIR_TYPES.length; i++) {
			int type = MassRepairOption.VALID_REPAIR_TYPES[i];

			MassRepairOptionControl mroc = massRepairOptionControls[i];
			MassRepairOption mro = new MassRepairOption();
			mro.setType(type);
			mro.setActive(mroc.activeBox.isSelected());
			mro.setSkillMin(mroc.minSkillCBox.getSelectedIndex());
			mro.setSkillMax(mroc.maxSkillCBox.getSelectedIndex());
			mro.setBthMin((Integer) mroc.minBTHSpn.getValue());
			mro.setBthMax((Integer) mroc.maxBTHSpn.getValue());

			campaignOptions.addMassRepairOption(mro);
		}

		MekHQ.EVENT_BUS.trigger(new OptionsChangedEvent(campaignGUI.getCampaign(), campaignOptions));

		JOptionPane.showMessageDialog(this, "Current settings saved as default options", "Default options saved",
				JOptionPane.INFORMATION_MESSAGE);
	}

	private static class MassRepairOptionControl {
		protected JCheckBox activeBox = null;
		protected JComboBox<String> minSkillCBox = null;
		protected JComboBox<String> maxSkillCBox = null;
		protected JSpinner minBTHSpn = null;
		protected JSpinner maxBTHSpn = null;
	}
}
