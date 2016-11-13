package mekhq.gui.dialog;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import mekhq.campaign.parts.MissingMekLocation;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.WorkTime;
import mekhq.gui.CampaignGUI;
import mekhq.gui.model.PartsTableModel;
import mekhq.gui.model.UnitTableModel;
import mekhq.gui.model.XTableColumnModel;
import mekhq.gui.sorter.PartsDetailSorter;
import mekhq.gui.sorter.UnitStatusSorter;
import mekhq.gui.sorter.UnitTypeSorter;

/**
 * @author Kipsta
 *
 */
public class MassRepairSalvageDialog extends JDialog {
	private static final long serialVersionUID = -7859207613578378162L;

	public interface MODE {
		public static final int UNITS = 0;
		public static final int WAREHOUSE = 1;
	}

	private CampaignGUI campaignGUI;
	private CampaignOptions campaignOptions;

	private int mode = MODE.UNITS;

	private Unit selectedUnit;
	private UnitTableModel unitTableModel;
	private JTable unitTable;
	private TableRowSorter<UnitTableModel> unitSorter;
	private JScrollPane scrollUnitList;
	private JButton btnSelectNone;
	private JButton btnSelectAssigned;
	private JButton btnSelectUnassigned;

	private PartsTableModel partsTableModel;
	private JTable partsTable;
	private JScrollPane scrollPartsTable;
	private JButton btnSelectAllParts;

	private JCheckBox useExtraTimeBox;
	private JCheckBox useRushJobBox;
	private JCheckBox allowCarryoverBox;
	private JCheckBox scrapImpossibleBox;

	private Map<Integer, MassRepairOptionControl> massRepairOptionControlMap = null;

	ArrayList<Unit> unitList = null;
	ArrayList<Part> completePartsList = null;
	ArrayList<Part> filteredPartsList = null;

	public MassRepairSalvageDialog(Frame _parent, boolean _modal, CampaignGUI _campaignGUI, Unit _selectedUnit,
			int mode) {
		super(_parent, _modal);
		this.campaignGUI = _campaignGUI;
		this.selectedUnit = _selectedUnit;

		campaignOptions = campaignGUI.getCampaign().getCampaignOptions();

		this.mode = mode;

		initComponents();

		if (isModeUnits()) {
			filterUnits();

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
		} else if (isModeWarehouse()) {
			filterCompletePartsList(true);
		}

		setLocationRelativeTo(_parent);
	}

	public MassRepairSalvageDialog(Frame _parent, boolean _modal, CampaignGUI _campaignGUI, int mode) {
		this(_parent, _modal, _campaignGUI, null, mode);
	}

	private boolean isModeUnits() {
		return mode == MODE.UNITS;
	}

	private boolean isModeWarehouse() {
		return mode == MODE.WAREHOUSE;
	}

	private static boolean isValidMRMSUnit(Unit unit) {
		if (unit.isSelfCrewed() || !unit.isAvailable()) {
			return false;
		}

		if (unit.isDeployed()) {
			return false;
		}

		return (unit.getEntity() instanceof Tank) || (unit.getEntity() instanceof Aero)
				|| (unit.getEntity() instanceof Mech);
	}

	private void filterUnits() {
		// Store selections so after the table is refreshed we can re-select
		// them
		Map<String, Unit> selectedUnitMap = new HashMap<String, Unit>();

		int[] selectedRows = unitTable.getSelectedRows();

		for (int i = 0; i < selectedRows.length; i++) {
			int rowIdx = unitTable.convertRowIndexToModel(selectedRows[i]);
			Unit unit = unitTableModel.getUnit(rowIdx);

			if (null == unit) {
				continue;
			}

			selectedUnitMap.put(unit.getId().toString(), unit);
		}

		int activeCount = 0;
		int inactiveCount = 0;

		unitList = new ArrayList<Unit>();

		for (int i = 0; i < campaignGUI.getCampaign().getServiceableUnits().size(); i++) {
			Unit unit = campaignGUI.getCampaign().getServiceableUnits().get(i);

			if (!isValidMRMSUnit(unit)) {
				continue;
			}

			unitList.add(unit);

			if ((null == unit.getActiveCrew()) || unit.getActiveCrew().isEmpty()) {
				inactiveCount++;
			} else {
				activeCount++;
			}
		}

		btnSelectAssigned.setText("Select Active Units (" + activeCount + ")");
		btnSelectUnassigned.setText("Select Inactive Units (" + inactiveCount + ")");

		unitTableModel.setData(unitList);

		int unitCount = unitTable.getRowCount();

		for (int i = 0; i < unitCount; i++) {
			int rowIdx = unitTable.convertRowIndexToModel(i);
			Unit unit = unitTableModel.getUnit(rowIdx);

			if (!selectedUnitMap.containsKey(unit.getId().toString())) {
				continue;
			}

			unitTable.addRowSelectionInterval(i, i);
		}
	}

	private void filterCompletePartsList(boolean refreshCompleteList) {
		Map<Integer, Integer> activeMROMap = new HashMap<Integer, Integer>();

		for (int i = 0; i < MassRepairOption.VALID_REPAIR_TYPES.length; i++) {
			int type = MassRepairOption.VALID_REPAIR_TYPES[i];

			MassRepairOptionControl mroc = massRepairOptionControlMap.get(i);

			if ((null == mroc) || !mroc.activeBox.isSelected()) {
				continue;
			}

			activeMROMap.put(type, type);
		}

		if (refreshCompleteList) {
			completePartsList = new ArrayList<Part>();

			for (Part part : campaignGUI.getCampaign().getSpareParts()) {
				if (!part.isBeingWorkedOn() && part.needsFixing() && !(part instanceof AmmoBin)
						&& (part.getSkillMin() <= SkillType.EXP_ELITE)) {
					completePartsList.add(part);
				}
			}
		}

		filteredPartsList = new ArrayList<Part>();
		int quantity = 0;

		for (Part part : completePartsList) {
			int partType = Part.findCorrectMassRepairType(part);

			if (activeMROMap.containsKey(partType)) {
				filteredPartsList.add(part);

				quantity += part.getQuantity();
			}
		}

		btnSelectAllParts.setText("Select All (" + quantity + ")");
		partsTableModel.setData(filteredPartsList);

		int count = partsTable.getRowCount();
		partsTable.addRowSelectionInterval(0, count - 1);
	}

	private void initComponents() {
		ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.MassRepair", new EncodeControl());

		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

		if (isModeUnits()) {
			setTitle("Mass Repair/Salvage");
		} else if (isModeWarehouse()) {
			setTitle("Mass Repair");
		}

		final Container content = getContentPane();
		content.setLayout(new GridBagLayout());

		JPanel pnlMain = new JPanel();
		pnlMain.setLayout(new GridBagLayout());

		if (isModeUnits()) {
			pnlMain.add(createUnitsPanel(), createBaseConstraints(0));
			pnlMain.add(createUnitActionButtons(), createBaseConstraints(1));
		} else if (isModeWarehouse()) {
			pnlMain.add(createPartsPanel(), createBaseConstraints(0));
			pnlMain.add(createPartsActionButtons(), createBaseConstraints(1));
		}

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
		unitSorter.setComparator(UnitTableModel.COL_RSTATUS, new Comparator<String>() {
			@Override
			public int compare(String s0, String s1) {
				return s0.compareTo(s1);
			}
		});

		ArrayList<RowSorter.SortKey> sortKeys = new ArrayList<RowSorter.SortKey>();
		sortKeys.add(new RowSorter.SortKey(UnitTableModel.COL_STATUS, SortOrder.DESCENDING));
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

			if (i != UnitTableModel.COL_NAME && i != UnitTableModel.COL_STATUS && i != UnitTableModel.COL_TYPE
					&& i != UnitTableModel.COL_RSTATUS) {
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

	private JPanel createPartsPanel() {
		JPanel pnlParts = new JPanel(new GridBagLayout());
		pnlParts.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Parts"),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));

		partsTableModel = new PartsTableModel();
		partsTable = new JTable(partsTableModel);
		partsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		partsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		partsTable.setColumnModel(new XTableColumnModel());
		partsTable.createDefaultColumnsFromModel();
		TableRowSorter<PartsTableModel> partsSorter = new TableRowSorter<PartsTableModel>(partsTableModel);
		partsSorter.setComparator(PartsTableModel.COL_DETAIL, new PartsDetailSorter());
		partsTable.setRowSorter(partsSorter);

		TableColumn column = null;

		for (int i = 0; i < PartsTableModel.N_COL; i++) {
			column = ((XTableColumnModel) partsTable.getColumnModel()).getColumnByModelIndex(i);
			column.setPreferredWidth(partsTableModel.getColumnWidth(i));
			column.setCellRenderer(partsTableModel.getRenderer());

			if (i != PartsTableModel.COL_QUANTITY && i != PartsTableModel.COL_NAME && i != PartsTableModel.COL_DETAIL
					&& i != PartsTableModel.COL_TECH_BASE) {
				((XTableColumnModel) partsTable.getColumnModel()).setColumnVisible(column, false);
			}
		}

		partsTable.setIntercellSpacing(new Dimension(0, 0));
		partsTable.setShowGrid(false);

		scrollPartsTable = new JScrollPane(partsTable);
		scrollPartsTable.setMinimumSize(new java.awt.Dimension(350, 200));
		scrollPartsTable.setPreferredSize(new java.awt.Dimension(350, 200));

		GridBagConstraints gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlParts.add(scrollPartsTable, gridBagConstraints);

		return pnlParts;
	}

	private JPanel createOptionsPanel(ResourceBundle resourceMap) {
		JPanel pnlOptions = new JPanel(new GridBagLayout());
		pnlOptions.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Options"),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));

		int gridRowIdx = 0;

		GridBagConstraints gridBagConstraints = null;

		useExtraTimeBox = new JCheckBox();
		useExtraTimeBox.setText(resourceMap.getString("useExtraTimeBox.text"));
		useExtraTimeBox.setToolTipText(resourceMap.getString("useExtraTimeBox.toolTipText"));
		useExtraTimeBox.setName("massRepairUseExtraTimeBox");
		useExtraTimeBox.setSelected(campaignOptions.massRepairUseExtraTime());
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = gridRowIdx++;
		gridBagConstraints.weightx = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlOptions.add(useExtraTimeBox, gridBagConstraints);

		useRushJobBox = new JCheckBox();
		useRushJobBox.setText(resourceMap.getString("useRushJobBox.text"));
		useRushJobBox.setToolTipText(resourceMap.getString("useRushJobBox.toolTipText"));
		useRushJobBox.setName("massRepairUseRushJobBox");
		useRushJobBox.setSelected(campaignOptions.massRepairUseRushJob());
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = gridRowIdx++;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlOptions.add(useRushJobBox, gridBagConstraints);

		allowCarryoverBox = new JCheckBox();
		allowCarryoverBox.setText(resourceMap.getString("allowCarryoverBox.text"));
		allowCarryoverBox.setToolTipText(resourceMap.getString("allowCarryoverBox.toolTipText"));
		allowCarryoverBox.setName("massRepairAllowCarryoverBox");
		allowCarryoverBox.setSelected(campaignOptions.massRepairAllowCarryover());
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = gridRowIdx++;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlOptions.add(allowCarryoverBox, gridBagConstraints);

		if (!isModeWarehouse()) {
			scrapImpossibleBox = new JCheckBox();
			scrapImpossibleBox.setText(resourceMap.getString("scrapImpossibleBox.text"));
			scrapImpossibleBox.setToolTipText(resourceMap.getString("scrapImpossibleBox.toolTipText"));
			scrapImpossibleBox.setName("massRepairScrapImpossibleBox");
			scrapImpossibleBox.setSelected(campaignOptions.massRepairScrapImpossible());
			gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = gridRowIdx++;
			gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
			pnlOptions.add(scrapImpossibleBox, gridBagConstraints);
		}

		JPanel pnlItems = new JPanel(new GridBagLayout());
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = gridRowIdx++;
		gridBagConstraints.weightx = 0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new Insets(10, 0, 0, 0);
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		pnlOptions.add(pnlItems, gridBagConstraints);

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

		massRepairOptionControlMap = new HashMap<Integer, MassRepairOptionControl>();

		if (!isModeWarehouse()) {
			massRepairOptionControlMap.put(Part.REPAIR_PART_TYPE.ARMOR,
					createMassRepairOptionControls(Part.REPAIR_PART_TYPE.ARMOR, "Repair/Salvage Armor",
							"Allow mass repair/salvage of armor", "massRepairItemArmor", pnlItems, rowIdx++));

			massRepairOptionControlMap.put(Part.REPAIR_PART_TYPE.AMMO,
					createMassRepairOptionControls(Part.REPAIR_PART_TYPE.AMMO, "Repair/Salvage Ammo",
							"Allow mass repair/salvage of ammo", "massRepairItemAmmo", pnlItems, rowIdx++));
		}

		massRepairOptionControlMap.put(Part.REPAIR_PART_TYPE.WEAPON,
				createMassRepairOptionControls(Part.REPAIR_PART_TYPE.WEAPON, "Repair/Salvage Weapons",
						"Allow mass repair/salvage of weapons", "massRepairItemWeapons", pnlItems, rowIdx++));
		massRepairOptionControlMap.put(Part.REPAIR_PART_TYPE.GENERAL_LOCATION,
				createMassRepairOptionControls(Part.REPAIR_PART_TYPE.GENERAL_LOCATION, "Repair/Salvage Locations",
						"Allow mass repair/salvage of mek body parts and vehicle locations", "massRepairItemLocations",
						pnlItems, rowIdx++));
		massRepairOptionControlMap.put(Part.REPAIR_PART_TYPE.ENGINE,
				createMassRepairOptionControls(Part.REPAIR_PART_TYPE.ENGINE, "Repair/Salvage Engines",
						"Allow mass repair/salvage of engines", "massRepairItemEngines", pnlItems, rowIdx++));
		massRepairOptionControlMap.put(Part.REPAIR_PART_TYPE.GYRO,
				createMassRepairOptionControls(Part.REPAIR_PART_TYPE.GYRO, "Repair/Salvage Gyros",
						"Allow mass repair/salvage of gyros", "massRepairItemGyros", pnlItems, rowIdx++));
		massRepairOptionControlMap.put(Part.REPAIR_PART_TYPE.ACTUATOR,
				createMassRepairOptionControls(Part.REPAIR_PART_TYPE.ACTUATOR, "Repair/Salvage Actuators",
						"Allow mass repair/salvage of actuators", "massRepairItemActuators", pnlItems, rowIdx++));
		massRepairOptionControlMap.put(Part.REPAIR_PART_TYPE.ELECTRONICS,
				createMassRepairOptionControls(Part.REPAIR_PART_TYPE.ELECTRONICS,
						"Repair/Salvage Cockpits/Sensors/Life Support",
						"Allow mass repair/salvage of cockpits, life support, and sensors", "massRepairItemHead",
						pnlItems, rowIdx++));
		massRepairOptionControlMap.put(Part.REPAIR_PART_TYPE.GENERAL,
				createMassRepairOptionControls(Part.REPAIR_PART_TYPE.GENERAL, "Repair/Salvage Other",
						"Allow mass repair/salvage of items which do not fall into the specific categories",
						"massRepairItemOther", pnlItems, rowIdx++));

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
		optionItemBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mroOptionChecked();
			}
		});

		GridBagConstraints gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = columnIdx;
		gridBagConstraints.gridy = rowIdx;
		gridBagConstraints.insets = new Insets(0, 0, 0, 5);
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;

		pnlItems.add(optionItemBox, gridBagConstraints);

		return optionItemBox;
	}

	private void mroOptionChecked() {
		if (isModeWarehouse()) {
			filterCompletePartsList(false);
		}
	}

	private JPanel createUnitActionButtons() {
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
		btnSelectAssigned.setText("Select Active Units"); // NOI18N
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
		btnSelectUnassigned.setText("Select Inactive Units"); // NOI18N
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

	private JPanel createPartsActionButtons() {
		JPanel pnlButtons = new JPanel();

		int btnIdx = 0;
		GridBagConstraints gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = btnIdx++;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);

		JButton btnUnselectParts = new JButton();
		btnUnselectParts.setText("Unselect All"); // NOI18N
		btnUnselectParts.setToolTipText("Unselect all parts");
		btnUnselectParts.setName("btnUnselectParts"); // NOI18N
		btnUnselectParts.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnUnselectPartsActionPerformed(evt);
			}
		});

		pnlButtons.add(btnUnselectParts, gridBagConstraints);

		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = btnIdx++;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);

		btnSelectAllParts = new JButton();
		btnSelectAllParts.setText("Select All"); // NOI18N
		btnSelectAllParts.setToolTipText("Select all parts");
		btnSelectAllParts.setName("btnSelectAllParts"); // NOI18N
		btnSelectAllParts.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnSelectAllPartsActionPerformed(evt);
			}
		});

		pnlButtons.add(btnSelectAllParts, gridBagConstraints);

		return pnlButtons;
	}

	private void btnUnselectPartsActionPerformed(ActionEvent evt) {
		partsTable.removeRowSelectionInterval(0, partsTable.getRowCount() - 1);
	}

	private void btnSelectAllPartsActionPerformed(ActionEvent evt) {
		partsTable.addRowSelectionInterval(0, partsTable.getRowCount() - 1);
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

		JButton btnStart = new JButton();

		if (isModeUnits()) {
			btnStart.setText("Start Mass Repair/Salvage"); // NOI18N
		} else if (isModeWarehouse()) {
			btnStart.setText("Start Mass Repair"); // NOI18N
		}

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

		JButton btnSaveAsDefault = new JButton();
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

		JButton btnCancel = new JButton();
		btnCancel.setText("Done"); // NOI18N
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
		List<MassRepairOption> activeMROs = new ArrayList<MassRepairOption>();

		for (int i = 0; i < MassRepairOption.VALID_REPAIR_TYPES.length; i++) {
			int type = MassRepairOption.VALID_REPAIR_TYPES[i];

			MassRepairOptionControl mroc = massRepairOptionControlMap.get(i);

			if ((null == mroc) || !mroc.activeBox.isSelected()) {
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

		int repairsCompleted = 0;
		String msg = "";

		if (isModeUnits()) {
			int[] selectedRows = unitTable.getSelectedRows();

			if ((null == selectedRows) || (selectedRows.length == 0)) {
				JOptionPane.showMessageDialog(this,
						"Can not started Mass Repair/Salvage if there are no selected units.", "No selected unit",
						JOptionPane.ERROR_MESSAGE);
				return;
			}

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

			for (Unit unit : units) {
				repairsCompleted += performUnitMassRepairOrSalvage(campaignGUI, unit, unit.isSalvage(), activeMROs,
						useExtraTimeBox.isSelected(), useRushJobBox.isSelected(), allowCarryoverBox.isSelected(),
						scrapImpossibleBox.isSelected());
			}

			if (repairsCompleted == 1) {
				msg = "Mass Repair/Salvage complete. There was 1 repair completed or scheduled.";
			} else {
				msg = String.format("Mass Repair/Salvage complete. There were %d repairs completed or scheduled.",
						repairsCompleted);
			}

			filterUnits();
		} else if (isModeWarehouse()) {
			int[] selectedRows = partsTable.getSelectedRows();

			if ((null == selectedRows) || (selectedRows.length == 0)) {
				JOptionPane.showMessageDialog(this, "Can not started Mass Repair if there are no selected parts.",
						"No selected parts", JOptionPane.ERROR_MESSAGE);
				return;
			}

			List<Part> parts = new ArrayList<Part>();

			for (int i = 0; i < selectedRows.length; i++) {
				int rowIdx = partsTable.convertRowIndexToModel(selectedRows[i]);
				Part part = partsTableModel.getPartAt(rowIdx);

				if (null == part) {
					continue;
				}

				parts.add(part);
			}

			if (parts.isEmpty()) {
				JOptionPane.showMessageDialog(this, "No valid parts selected", "No parts", JOptionPane.ERROR_MESSAGE);
				return;
			}

			repairsCompleted = performWarehouseMassRepair(parts, activeMROs, useExtraTimeBox.isSelected(),
					useRushJobBox.isSelected(), allowCarryoverBox.isSelected(), false);

			if (repairsCompleted == 1) {
				msg = "Mass Repair complete. There was 1 repair completed or scheduled.";
			} else {
				msg = String.format("Mass Repair complete. There were %d repairs completed or scheduled.",
						repairsCompleted);
			}

			filterCompletePartsList(true);
		}

		JOptionPane.showMessageDialog(this, msg, "Complete", JOptionPane.INFORMATION_MESSAGE);

		campaignGUI.getCampaign().addReport(msg);

		campaignGUI.refreshReport();
	}

	private void btnSaveAsDefaultActionPerformed(ActionEvent evt) {
		updateOptions();
	}

	private void btnCancelActionPerformed(ActionEvent evt) {
		this.setVisible(false);
	}

	private static List<MassRepairOption> createActiveMROsFromConfiguration(CampaignGUI campaignGUI) {
		List<MassRepairOption> activeMROs = new ArrayList<MassRepairOption>();
		List<MassRepairOption> mroList = campaignGUI.getCampaign().getCampaignOptions().getMassRepairOptions();

		if (null != mroList) {
			for (int i = 0; i < mroList.size(); i++) {
				MassRepairOption mro = mroList.get(i);

				if (mro.isActive()) {
					activeMROs.add(mro);
				}
			}
		}

		return activeMROs;
	}

	public static void performSingleUnitMassRepairOrSalvage(CampaignGUI campaignGUI, Unit unit) {
		CampaignOptions options = campaignGUI.getCampaign().getCampaignOptions();
		List<MassRepairOption> activeMROs = createActiveMROsFromConfiguration(campaignGUI);
		String msg = "";

		int repairsCompleted = performUnitMassRepairOrSalvage(campaignGUI, unit, unit.isSalvage(), activeMROs,
				options.massRepairUseExtraTime(), options.massRepairUseRushJob(), options.massRepairAllowCarryover(),
				options.massRepairScrapImpossible());

		if (repairsCompleted == 1) {
			msg = "Mass Repair/Salvage complete. There was 1 repair completed or scheduled.";
		} else {
			msg = String.format("Mass Repair/Salvage complete. There were %d repairs completed or scheduled.",
					repairsCompleted);
		}

		JOptionPane.showMessageDialog(campaignGUI.getFrame(), msg, "Complete", JOptionPane.INFORMATION_MESSAGE);

		campaignGUI.getCampaign().addReport(msg);

		campaignGUI.refreshReport();
	}

	public static void massRepairSalvageAllUnits(CampaignGUI campaignGUI) {
		CampaignOptions options = campaignGUI.getCampaign().getCampaignOptions();
		List<MassRepairOption> activeMROs = createActiveMROsFromConfiguration(campaignGUI);
		String msg = "";
		int repairsCompleted = 0;

		List<Unit> units = new ArrayList<>();

		for (int i = 0; i < campaignGUI.getCampaign().getServiceableUnits().size(); i++) {
			Unit unit = campaignGUI.getCampaign().getServiceableUnits().get(i);

			if (!isValidMRMSUnit(unit)) {
				continue;
			}

			units.add(unit);
		}

		// Sort the list status fixing the least damaged first
		Collections.sort(units, new Comparator<Unit>() {
			@Override
			public int compare(Unit o1, Unit o2) {
				int damageIdx1 = UnitStatusSorter.getDamageStateIndex(Unit.getDamageStateName(o1.getDamageState()));
				int damageIdx2 = UnitStatusSorter.getDamageStateIndex(Unit.getDamageStateName(o2.getDamageState()));

				if (damageIdx2 == damageIdx1) {
					return 0;
				} else if (damageIdx2 < damageIdx1) {
					return -1;
				}

				return 1;
			}
		});

		for (Unit unit : units) {
			repairsCompleted += performUnitMassRepairOrSalvage(campaignGUI, unit, unit.isSalvage(), activeMROs,
					options.massRepairUseExtraTime(), options.massRepairUseRushJob(),
					options.massRepairAllowCarryover(), options.massRepairScrapImpossible());
		}

		if (repairsCompleted == 1) {
			msg = "Mass Repair/Salvage complete. There was 1 repair completed or scheduled.";
		} else {
			msg = String.format("Mass Repair/Salvage complete. There were %d repairs completed or scheduled.",
					repairsCompleted);
		}

		JOptionPane.showMessageDialog(campaignGUI.getFrame(), msg, "Complete", JOptionPane.INFORMATION_MESSAGE);

		campaignGUI.getCampaign().addReport(msg);

		campaignGUI.refreshReport();

	}

	public static int performUnitMassRepairOrSalvage(CampaignGUI campaignGUI, Unit unit, boolean isSalvage,
			List<MassRepairOption> mroList, boolean useExtraTime, boolean useRushJob, boolean allowCarryover,
			boolean scrapImpossible) {
		String actionDescriptor = isSalvage ? "salvage" : "repair";
		Campaign campaign = campaignGUI.getCampaign();

		campaign.addReport(String.format("Beginning mass %s on %s.", actionDescriptor, unit.getName()));

		ArrayList<Person> techs = campaign.getTechs(true);

		int totalActionsPerformed = 0;

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
			int actionsPerformed = 1;

			while (actionsPerformed > 0) {
				actionsPerformed = performUnitMassTechAction(campaignGUI, unit, techs, mroByTypeMap, isSalvage,
						useExtraTime, useRushJob, allowCarryover, scrapImpossible);
				totalActionsPerformed += actionsPerformed;
			}

			campaign.addReport(String.format("Mass %s complete on %s. %d total actions performed.", actionDescriptor,
					unit.getName(), totalActionsPerformed));
		}

		return totalActionsPerformed;
	}

	private static int performUnitMassTechAction(CampaignGUI campaignGUI, Unit unit, List<Person> techs,
			Map<Integer, MassRepairOption> mroByTypeMap, boolean salvaging, boolean useExtraTime, boolean useRushJob,
			boolean allowCarryover, boolean scrapImpossible) {
		Campaign campaign = campaignGUI.getCampaign();
		int totalActionsPerformed = 0;
		String actionDescriptor = salvaging ? "salvage" : "repair";

		List<Part> parts = campaignGUI.getCampaign().getPartsNeedingServiceFor(unit.getId());

		/*
		 * If we're repairing a unit and we allow auto-scrapping of parts that
		 * can't be fixed by an elite tech, let's first get rid of those parts
		 * and start with a cleaner slate
		 */
		if (scrapImpossible && !salvaging) {
			boolean refreshParts = false;

			for (Part part : parts) {
				if (part.getSkillMin() > SkillType.EXP_ELITE) {
					campaign.addReport(part.scrap());
					refreshParts = true;
				}
			}

			if (refreshParts) {
				parts = campaignGUI.getCampaign().getPartsNeedingServiceFor(unit.getId());
			}
		}

		if (techs.isEmpty()) {
			campaign.addReport(
					String.format("Unable to %s any more parts from %s because there are no available techs.",
							actionDescriptor, unit.getName()));
			return totalActionsPerformed;
		}

		/*
		 * If we're a mek and we have a limb with a bad shoulder/hip, we're
		 * going to try to flip it to salvageable and remove all the parts so
		 * that we can nuke the limb. If we do this, when we're finally done we
		 * need to flip the mek back to repairable so that we don't accidentally
		 * strip everything off it.
		 */
		boolean scrappingLimbMode = false;

		/*
		 * Pre checking for hips/shoulders on repairable meks. If we have a bad
		 * hip or shoulder, we're not going to do anything until we get those
		 * parts out of the location and scrap it. Once we're at a happy place,
		 * we'll proceed.
		 */

		if ((unit.getEntity() instanceof Mech) && !salvaging) {
			Map<Integer, Part> locationMap = new HashMap<Integer, Part>();

			for (Part part : parts) {
				if ((part instanceof MekLocation) && part.onBadHipOrShoulder()) {
					locationMap.put(((MekLocation) part).getLoc(), part);
				} else if (part instanceof MissingMekLocation) {
					locationMap.put(((MissingMekLocation) part).getLoc(), part);
				}
			}

			if (!locationMap.isEmpty()) {
				MassRepairOption mro = mroByTypeMap.get(Part.REPAIR_PART_TYPE.GENERAL_LOCATION);

				if ((null == mro) || !mro.isActive()) {
					campaign.addReport(
							"Unable to proceed with repairs because this mek has an unfixable limb but configured settings do not allow location repairs.");

					return 0;
				}

				/*
				 * Find our parts in our bad locations. If we don't actually
				 * have, just scrap the limbs and move on with our normal work
				 */

				scrappingLimbMode = true;
				unit.setSalvage(true);

				List<Part> partsTemp = campaignGUI.getCampaign().getPartsNeedingServiceFor(unit.getId());
				List<Part> partsToBeRemoved = new ArrayList<Part>();
				Map<Integer, Integer> countOfPartsPerLocation = new HashMap<Integer, Integer>();

				for (Part part : partsTemp) {
					if (!(part instanceof MekLocation) && !(part instanceof MissingMekLocation)
							&& locationMap.containsKey(part.getLocation()) && part.isSalvaging()) {
						partsToBeRemoved.add(part);

						int count = 0;

						if (countOfPartsPerLocation.containsKey(part.getLocation())) {
							count = countOfPartsPerLocation.get(part.getLocation());
						}

						count++;

						countOfPartsPerLocation.put(part.getLocation(), count);
					}
				}

				if (partsToBeRemoved.isEmpty()) {
					/*
					 * We have no parts left on our unfixable locations, so
					 * we'll just scrap those locations and rebuild the parts
					 * list and reset back our normal repair mode
					 */

					for (Part part : locationMap.values()) {
						if (part instanceof MekLocation) {
							campaign.addReport(part.scrap());
						}
					}

					scrappingLimbMode = false;
					unit.setSalvage(false);

					parts = campaignGUI.getCampaign().getPartsNeedingServiceFor(unit.getId());
				} else {
					for (int locId : countOfPartsPerLocation.keySet()) {
						boolean unfixable = false;
						Part loc = null;

						if (locationMap.containsKey(locId)) {
							loc = locationMap.get(locId);
							unfixable = (loc instanceof MekLocation);
						}

						if (unfixable) {
							campaign.addReport(String.format(
									"Found an unfixable limb - %s which contains %s parts. Going to remove all parts and scrap the limb before proceeding with other repairs.",
									loc.getName(), countOfPartsPerLocation.get(locId)));
						} else {
							campaign.addReport(String.format(
									"Found missing location - %s which contains %s parts. Going to remove all parts before proceeding with other repairs.",
									loc.getName(), countOfPartsPerLocation.get(locId)));
						}
					}

					parts = partsToBeRemoved;
				}
			}
		}

		/*
		 * If we're scrapping limbs, we don't want salvage repairs to go into a
		 * new day otherwise it can be confusing when trying to figure why a
		 * unit can't be repaired because 'salvage' repairs don't show up on the
		 * task list as scheduled if we're in 'repair' mode.
		 */
		if (scrappingLimbMode) {
			allowCarryover = false;
		}

		/*
		 * Filter our parts list to only those that aren't being worked on or
		 * those that meet our criteria as defined in the campaign
		 * configurations
		 */
		parts = filterParts(parts, mroByTypeMap);

		if (parts.isEmpty()) {
			campaign.addReport(
					String.format("Unable to %s any more parts from %s because there are no valid parts left to %s.",
							actionDescriptor, unit.getName(), actionDescriptor));

			if (scrappingLimbMode) {
				unit.setSalvage(false);
			}

			return totalActionsPerformed;
		}

		for (Part part : parts) {
			if (techs.isEmpty()) {
				campaign.addReport(
						String.format("Unable to %s any more parts from %s because there are no available techs.",
								actionDescriptor, unit.getName()));
				continue;
			}

			// Search the list of techs each time for a variety of checks. We'll
			// create a temporary truncated list of techs
			if (repairPart(campaignGUI, part, techs, mroByTypeMap, useExtraTime, useRushJob, allowCarryover,
					scrapImpossible, false)) {
				totalActionsPerformed++;
			}
		}

		if (scrappingLimbMode) {
			unit.setSalvage(false);
		}

		return totalActionsPerformed;
	}

	private int performWarehouseMassRepair(List<Part> selectedParts, List<MassRepairOption> mroList,
			boolean useExtraTime, boolean useRushJob, boolean allowCarryover, boolean scrapImpossible) {
		Campaign campaign = campaignGUI.getCampaign();

		campaign.addReport("Beginning mass warehouse repair.");

		ArrayList<Person> techs = campaign.getTechs(true);

		int totalActionsPerformed = 0;

		if (techs.isEmpty()) {
			campaign.addReport("No available techs to repairs parts.");
		} else {
			Map<Integer, MassRepairOption> mroByTypeMap = new HashMap<Integer, MassRepairOption>();

			for (int i = 0; i < mroList.size(); i++) {
				mroByTypeMap.put(mroList.get(i).getType(), mroList.get(i));
			}

			/*
			 * Filter our parts list to only those that aren't being worked on
			 * or those that meet our criteria as defined in the campaign
			 * configurations
			 */
			List<Part> parts = filterParts(selectedParts, mroByTypeMap);

			if (parts.isEmpty()) {
				return totalActionsPerformed;
			}

			for (Part part : parts) {
				if (techs.isEmpty()) {
					campaign.addReport("Unable to repair any more parts because there are no available techs.");
					continue;
				}

				int originalQuantity = part.getQuantity();

				for (int i = 0; i < originalQuantity; i++) {
					if (repairPart(campaignGUI, part, techs, mroByTypeMap, useExtraTime, useRushJob, allowCarryover,
							false, true)) {
						totalActionsPerformed++;
					}
				}
			}
		}

		return totalActionsPerformed;
	}

	private static boolean repairPart(CampaignGUI campaignGUI, Part part, List<Person> techs,
			Map<Integer, MassRepairOption> mroByTypeMap, boolean useExtraTime, boolean useRushJob,
			boolean allowCarryover, boolean scrapImpossible, boolean warehouseMode) {
		Map<String, WorkTime> techToWorktimeMap = new HashMap<String, WorkTime>();
		int modePenalty = part.getMode().expReduction;
		Campaign campaign = campaignGUI.getCampaign();
		List<Person> validTechs = new ArrayList<Person>();

		for (int i = techs.size() - 1; i >= 0; i--) {
			/*
			 * Reset our WorkTime back to normal so that we can adjust as
			 * necessary
			 */
			WorkTime selectedWorktime = WorkTime.NORMAL;
			part.setMode(WorkTime.of(selectedWorktime.id));

			Person tech = techs.get(i);

			if (warehouseMode && !tech.isRightTechTypeFor(part)) {
				continue;
			}

			Skill skill = tech.getSkillForWorkingOn(part);
			MassRepairOption mro = mroByTypeMap.get(Part.findCorrectMassRepairType(part));

			if (null == mro) {
				continue;
			}

			if (null == skill) {
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

			if ((targetRoll.getValue() == TargetRoll.IMPOSSIBLE) || (targetRoll.getValue() == TargetRoll.AUTOMATIC_FAIL)
					|| (targetRoll.getValue() == TargetRoll.CHECK_FALSE)) {
				continue;
			}

			// Check if we need to increase the time to meet the min BTH
			if (targetRoll.getValue() > mro.getBthMin()) {
				if (!useExtraTime) {
					continue;
				}

				WorkTime newWorkTime = calculateNewMassRepairWorktime(part, tech, mro, campaign, true);

				if (null == newWorkTime) {
					continue;
				}

				selectedWorktime = newWorkTime;
			} else if (targetRoll.getValue() < mro.getBthMax()) {
				// Or decrease the time to meet the max BTH
				if (useRushJob) {
					WorkTime newWorkTime = calculateNewMassRepairWorktime(part, tech, mro, campaign, false);

					// This should never happen, but...
					if (null != newWorkTime) {
						selectedWorktime = newWorkTime;
					}
				}
			}

			if ((tech.getMinutesLeft() < part.getActualTime()) && !allowCarryover) {
				continue;
			}

			validTechs.add(tech);
			techToWorktimeMap.put(tech.getId().toString(), selectedWorktime);

			part.setMode(WorkTime.of(WorkTime.NORMAL.id));
		}

		if (!validTechs.isEmpty()) {
			/*
			 * Sort the valid techs by applicable skill. Let's start with the
			 * least experienced and work our way up until we find someone who
			 * can perform the work. If we have two techs with the same skill,
			 * put the one with the lesser XP in the front.
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

			if (warehouseMode) {
				campaign.fixWarehousePart(part, tech);
			} else {
				campaign.fixPart(part, tech);
			}

			// If this tech has no time left, filter them out so we don't
			// spend cycles on them in the future
			if (tech.getMinutesLeft() <= 0) {
				techs.remove(tech);
			}

			Thread.yield();

			return true;
		}

		return false;
	}

	private static List<Part> filterParts(List<Part> parts, Map<Integer, MassRepairOption> mroByTypeMap) {
		List<Part> newParts = new ArrayList<Part>();

		for (Part part : parts) {
			if (!part.isBeingWorkedOn()) {
				int repairType = Part.findCorrectMassRepairType(part);

				MassRepairOption mro = mroByTypeMap.get(repairType);

				if ((null != mro) && mro.isActive()) {
					newParts.add(part);
				}
			}
		}

		return newParts;
	}

	private static WorkTime calculateNewMassRepairWorktime(Part part, Person tech, MassRepairOption mro,
			Campaign campaign, boolean increaseTime) {
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

		if (!isModeWarehouse()) {
			campaignOptions.setMassRepairScrapImpossible(scrapImpossibleBox.isSelected());
		}

		for (int i = 0; i < MassRepairOption.VALID_REPAIR_TYPES.length; i++) {
			int type = MassRepairOption.VALID_REPAIR_TYPES[i];

			MassRepairOptionControl mroc = massRepairOptionControlMap.get(i);

			if (null == mroc) {
				continue;
			}

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
