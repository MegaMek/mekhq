package mekhq.gui.dialog;

import java.awt.BorderLayout;
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

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.CampaignOptions.MassRepairOption;
import mekhq.campaign.event.OptionsChangedEvent;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IPartWork;
import mekhq.gui.CampaignGUI;
import mekhq.gui.model.PartsTableModel;
import mekhq.gui.model.UnitTableModel;
import mekhq.gui.model.XTableColumnModel;
import mekhq.gui.sorter.PartsDetailSorter;
import mekhq.gui.sorter.UnitStatusSorter;
import mekhq.gui.sorter.UnitTypeSorter;
import mekhq.service.MassRepairService;

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
	private JPanel pnlUnits;
	private JScrollPane scrollUnitList;
	private JButton btnSelectNone;
	private JButton btnSelectAssigned;
	private JButton btnSelectUnassigned;

	private PartsTableModel partsTableModel;
	private JTable partsTable;
	private JPanel pnlParts;
	private JScrollPane scrollPartsTable;
	private JButton btnSelectAllParts;

	private JCheckBox useExtraTimeBox;
	private JCheckBox useRushJobBox;
	private JCheckBox allowCarryoverBox;
	private JCheckBox optimizeToCompleteTodayBox;
	private JCheckBox scrapImpossibleBox;
	private JCheckBox useAssignedTechsFirstBox;
	private JCheckBox replacePodPartsBox;

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

			if (!MassRepairService.isValidMRMSUnit(unit)) {
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
			int partType = IPartWork.findCorrectMassRepairType(part);

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
		content.setLayout(new BorderLayout());

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

		content.add(new JScrollPane(pnlMain), BorderLayout.CENTER);
		content.add(createActionButtons(), BorderLayout.SOUTH);

		pack();
	}

	private Object createBaseConstraints(int rowIdx) {
		GridBagConstraints gridBagConstraints = null;
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = rowIdx;
		gridBagConstraints.weightx = 1;
		gridBagConstraints.weighty = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;

		return gridBagConstraints;
	}

	private JPanel createUnitsPanel() {
		pnlUnits = new JPanel(new GridBagLayout());
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
		pnlParts = new JPanel(new GridBagLayout());
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
		allowCarryoverBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				optimizeToCompleteTodayBox.setEnabled(allowCarryoverBox.isSelected());
			}
		});

		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = gridRowIdx++;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlOptions.add(allowCarryoverBox, gridBagConstraints);

		optimizeToCompleteTodayBox = new JCheckBox();
		optimizeToCompleteTodayBox.setText(resourceMap.getString("optimizeToCompleteTodayBox.text"));
		optimizeToCompleteTodayBox.setToolTipText(resourceMap.getString("optimizeToCompleteTodayBox.toolTipText"));
		optimizeToCompleteTodayBox.setName("massRepairOptimizeToCompleteTodayBox");
		optimizeToCompleteTodayBox.setSelected(campaignOptions.massRepairOptimizeToCompleteToday());
		optimizeToCompleteTodayBox.setEnabled(campaignOptions.massRepairAllowCarryover());
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = gridRowIdx++;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlOptions.add(optimizeToCompleteTodayBox, gridBagConstraints);

		if (!isModeWarehouse()) {
			useAssignedTechsFirstBox = new JCheckBox();
			useAssignedTechsFirstBox.setText(resourceMap.getString("useAssignedTechsFirstBox.text"));
			useAssignedTechsFirstBox.setToolTipText(resourceMap.getString("useAssignedTechsFirstBox.toolTipText"));
			useAssignedTechsFirstBox.setName("massRepairUseAssignedTechsFirstBox");
			useAssignedTechsFirstBox.setSelected(campaignOptions.massRepairUseAssignedTechsFirst());
			gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = gridRowIdx++;
			gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
			pnlOptions.add(useAssignedTechsFirstBox, gridBagConstraints);

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

            replacePodPartsBox = new JCheckBox();
            replacePodPartsBox.setText(resourceMap.getString("replacePodPartsBox.text"));
            replacePodPartsBox.setToolTipText(resourceMap.getString("replacePodPartsBox.toolTipText"));
            replacePodPartsBox.setName("replacePodParts");
            replacePodPartsBox.setSelected(campaignOptions.massRepairReplacePod());
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = gridRowIdx++;
            gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            pnlOptions.add(replacePodPartsBox, gridBagConstraints);
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
        massRepairOptionControlMap.put(Part.REPAIR_PART_TYPE.POD_SPACE,
                createMassRepairOptionControls(Part.REPAIR_PART_TYPE.POD_SPACE,
                        "Replace/Salvage OmniPod Equipment",
                        "All pod-mounted equipment will be replaced or salvaged regardless of other categories selected",
                        "massRepairItemPod", pnlItems, rowIdx++));

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
		if (name.equals("massRepairItemPod") && !isModeWarehouse()) {
            replacePodPartsBox.setEnabled(selected);
		}
		optionItemBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mroOptionChecked();
				if (((JCheckBox)e.getSource()).getName().equals("massRepairItemPod") && !isModeWarehouse()) {
				    replacePodPartsBox.setEnabled(((JCheckBox)e.getSource()).isSelected());
				}
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

		String btnHideDoHideLabel = "Hide Unit List";
		String btnHideDoHideTooltip = "Hide units to save room on small screens";
		
		String btnHideDoShowLabel = "Show Unit List";
		String btnHideDoShowTooltip = "Show list of units";

		JDialog dlg = this;
		
		JButton btnHideUnits = new JButton();
		
		if (pnlUnits.isVisible()) {
			btnHideUnits.setText(btnHideDoHideLabel);
			btnHideUnits.setToolTipText(btnHideDoHideTooltip);
		} else {
			btnHideUnits.setText(btnHideDoShowLabel);
			btnHideUnits.setToolTipText(btnHideDoShowTooltip);
		}
		
		btnHideUnits.setName("btnHideUnits");
		btnHideUnits.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				if (pnlUnits.isVisible()) {
					pnlUnits.setVisible(false);
					
					btnHideUnits.setText(btnHideDoShowLabel);
					btnHideUnits.setToolTipText(btnHideDoShowTooltip);
					
					dlg.pack();
				} else {
					pnlUnits.setVisible(true);
					
					btnHideUnits.setText(btnHideDoHideLabel);
					btnHideUnits.setToolTipText(btnHideDoHideTooltip);
					
					dlg.pack();
				}
			}
		});

		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = btnIdx++;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);

		pnlButtons.add(btnHideUnits, gridBagConstraints);

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

		String btnHideDoHideLabel = "Hide Parts List";
		String btnHideDoHideTooltip = "Hide parts to save room on small screens";
		
		String btnHideDoShowLabel = "Show Parts List";
		String btnHideDoShowTooltip = "Show list of parts";

		JDialog dlg = this;
		
		JButton btnHideParts = new JButton();
		
		if (pnlParts.isVisible()) {
			btnHideParts.setText(btnHideDoHideLabel);
			btnHideParts.setToolTipText(btnHideDoHideTooltip);
		} else {
			btnHideParts.setText(btnHideDoShowLabel);
			btnHideParts.setToolTipText(btnHideDoShowTooltip);
		}
		
		btnHideParts.setName("btnHideParts");
		btnHideParts.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				if (pnlParts.isVisible()) {
					pnlParts.setVisible(false);
					
					btnHideParts.setText(btnHideDoShowLabel);
					btnHideParts.setToolTipText(btnHideDoShowTooltip);
					
					dlg.pack();
				} else {
					pnlParts.setVisible(true);
					
					btnHideParts.setText(btnHideDoHideLabel);
					btnHideParts.setToolTipText(btnHideDoHideTooltip);
					
					dlg.pack();
				}
			}
		});

		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = btnIdx++;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);

		pnlButtons.add(btnHideParts, gridBagConstraints);

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

			MassRepairOptionControl mroc = massRepairOptionControlMap.get(type);

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

			MassRepairService.MassRepairConfiguredOptions configuredOptions = new MassRepairService.MassRepairConfiguredOptions();
			configuredOptions.setup(this);

			for (Unit unit : units) {
				repairsCompleted += MassRepairService.performUnitMassRepairOrSalvage(campaignGUI, unit, unit.isSalvage(), activeMROs,
						configuredOptions);
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

			List<IPartWork> parts = new ArrayList<IPartWork>();

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

			MassRepairService.MassRepairConfiguredOptions configuredOptions = new MassRepairService.MassRepairConfiguredOptions();
			configuredOptions.setup(this);
			configuredOptions.setScrapImpossible(false);

			repairsCompleted = MassRepairService.performWarehouseMassRepair(parts, activeMROs, configuredOptions, campaignGUI);

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
	}

	private void btnSaveAsDefaultActionPerformed(ActionEvent evt) {
		updateOptions();
	}

	private void btnCancelActionPerformed(ActionEvent evt) {
		this.setVisible(false);
	}

	private void updateOptions() {
		campaignOptions.setMassRepairUseExtraTime(useExtraTimeBox.isSelected());
		campaignOptions.setMassRepairUseRushJob(useRushJobBox.isSelected());
		campaignOptions.setMassRepairAllowCarryover(allowCarryoverBox.isSelected());
		campaignOptions.setMassRepairOptimizeToCompleteToday(optimizeToCompleteTodayBox.isSelected());

		if (!isModeWarehouse()) {
			campaignOptions.setMassRepairScrapImpossible(scrapImpossibleBox.isSelected());
            campaignOptions.setMassRepairUseAssignedTechsFirst(useAssignedTechsFirstBox.isSelected());
            campaignOptions.setMassRepairReplacePod(replacePodPartsBox.isSelected());
		}

		for (int i = 0; i < MassRepairOption.VALID_REPAIR_TYPES.length; i++) {
			int type = MassRepairOption.VALID_REPAIR_TYPES[i];

			MassRepairOptionControl mroc = massRepairOptionControlMap.get(type);

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

		MekHQ.triggerEvent(new OptionsChangedEvent(campaignGUI.getCampaign(), campaignOptions));

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

	public JCheckBox getUseExtraTimeBox() {
		return useExtraTimeBox;
	}

	public JCheckBox getUseRushJobBox() {
		return useRushJobBox;
	}

	public JCheckBox getAllowCarryoverBox() {
		return allowCarryoverBox;
	}

	public JCheckBox getOptimizeToCompleteTodayBox() {
		return optimizeToCompleteTodayBox;
	}

	public JCheckBox getScrapImpossibleBox() {
		return scrapImpossibleBox;
	}

	public JCheckBox getUseAssignedTechsFirstBox() {
		return useAssignedTechsFirstBox;
	}

	public JCheckBox getReplacePodPartsBox() {
		return replacePodPartsBox;
	}
}
