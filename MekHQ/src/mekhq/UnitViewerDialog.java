/*
 * MegaMekLab - Copyright (C) 2008 
 * 
 * Original author - jtighe (torren@users.sourceforge.net)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */

package mekhq;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import megamek.client.ui.MechView;
import megamek.client.ui.swing.UnitFailureDialog;
import megamek.client.ui.swing.UnitLoadingDialog;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.EquipmentType;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.MechSummaryComparator;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.TechConstants;
import megamek.common.UnitType;
import megamek.common.WeaponType;
import megamek.common.loaders.EntityLoadingException;

/*
 * Allows a user to sort through a list of MechSummaries and select one
 */
public class UnitViewerDialog extends JDialog implements ActionListener, KeyListener, ListSelectionListener, WindowListener, ItemListener {

    /**
     * 
     */
    private static final long serialVersionUID = -5073769715388980404L;
    // how long after a key is typed does a new search begin
    private final static int KEY_TIMEOUT = 1000;

    // these indices should match up with the static values in the
    // MechSummaryComparator
    private String[] saSorts = { "Name", "Ref", "Weight", "BV", "Year" };

    private MechSummary[] mechsCurrent;
    //private UnitLoadingDialog unitLoadingDialog;

    private StringBuilder m_sbSearch = new StringBuilder();
    private long m_nLastSearch = 0;

    private JLabel labelType = new JLabel("Tech: ", SwingConstants.RIGHT);
    private JComboBox chType = new JComboBox();
    private JLabel labelWeightClass = new JLabel("Class: ", SwingConstants.RIGHT);
    private JComboBox chWeightClass = new JComboBox();
    private JLabel labelSort = new JLabel("Sort: ", SwingConstants.RIGHT);
    private JComboBox chSort = new JComboBox();
    private JLabel labelUnit = new JLabel("Unit: ", SwingConstants.RIGHT);
    private JComboBox chUnit = new JComboBox();
    private JPanel pParams = new JPanel();
    private JPanel textBoxSpring = new JPanel(new SpringLayout());
    private JPanel springHolder = new JPanel(new SpringLayout());
    private JPanel fluffBoxSpring = new JPanel(new SpringLayout());

    DefaultListModel defaultModel = null;
    ListSelectionModel listSelectionModel = null;
    JList mechList = null;
    JScrollPane listScrollPane = null;
    JScrollPane leftScrollPane = null;
    JScrollPane rightScrollPane = null;
    JScrollPane fluffScrollPane = null;

    private JButton bCancel = new JButton("Close");
    private JButton bSelect = new JButton("Select");

    private JTextArea mechViewLeft = null;
    private JTextArea mechViewRight = null;
    private JTextArea unitFluff = null;

    private JPanel pUpper = new JPanel();
    // private JPanel pPreview = new JPanel();

    // private String selectedUnit = null;

    private boolean viewFluff = false;

    private JPanel m_pOpenAdvanced = new JPanel();
    private JButton m_bToggleAdvanced = new JButton("< Show Advanced Search >");
    private JPanel m_pSouthParams = new JPanel();

    private JComboBox m_cWalk = new JComboBox();
    private JTextField m_tWalk = new JTextField(2);
    private JComboBox m_cJump = new JComboBox();
    private JTextField m_tJump = new JTextField(2);
    private JComboBox m_cArmor = new JComboBox();
    private JTextField m_tWeapons1 = new JTextField(2);
    private JComboBox m_cWeapons1 = new JComboBox();
    private JComboBox m_cOrAnd = new JComboBox();
    private JTextField m_tWeapons2 = new JTextField(2);
    private JComboBox m_cWeapons2 = new JComboBox();
    private JCheckBox m_chkEquipment = new JCheckBox();
    private JComboBox m_cEquipment = new JComboBox();
    private JButton m_bSearch = new JButton("Search");
    private JButton m_bReset = new JButton("Reset");
    private JLabel m_lCount = new JLabel();

    private int m_count;
    private int m_old_nType;
    private int m_old_nUnitType;
    private int nUnitType = 0;

    private JFrame clientgui;

    Entity selectedUnit = null;

    public UnitViewerDialog(JFrame frame, int unitType) {

        super(frame, "Unit Selector", true);

        clientgui = frame;
        nUnitType = unitType;

        // save params
//        unitLoadingDialog = uld;
        viewFluff = false;

        // construct 2 text boxes
        mechViewLeft = new JTextArea(17, 25);
        mechViewRight = new JTextArea(17, 34);
        unitFluff = new JTextArea(10, 40);

        // construct a model and list
        defaultModel = new DefaultListModel();
        mechList = new JList(defaultModel);
        listSelectionModel = mechList.getSelectionModel();
        mechList.setVisibleRowCount(17);// give the list same number of rows as
        // the text boxes
        listSelectionModel.addListSelectionListener(this);

        // place the list and text boxes in scroll panes
        listScrollPane = new JScrollPane(mechList);
        leftScrollPane = new JScrollPane(mechViewLeft);
        rightScrollPane = new JScrollPane(mechViewRight);
        fluffScrollPane = new JScrollPane(unitFluff);

        // set list/scroll options
        listScrollPane.setAlignmentX(LEFT_ALIGNMENT);
        listScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        leftScrollPane.setAlignmentX(LEFT_ALIGNMENT);
        leftScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        rightScrollPane.setAlignmentX(LEFT_ALIGNMENT);
        rightScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        fluffScrollPane.setAlignmentX(LEFT_ALIGNMENT);
        fluffScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        fluffScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        mechList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // set fonts
        mechViewLeft.setFont(new Font("Monospaced", Font.PLAIN, 11));
        mechViewRight.setFont(new Font("Monospaced", Font.PLAIN, 11));
        mechList.setFont(new Font("Monospaced", Font.PLAIN, 11));

        for (String saSort : saSorts) {
            chSort.addItem(saSort);
        }

        // set up the upper panel (combo boxes, preview image)
        // pPreview = new MechInfo(mwclient);
        // pPreview.setVisible(false);
        // pPreview.setMinimumSize(new Dimension(86, 74));
        // pPreview.setMaximumSize(new Dimension(86, 74));
        // pUpper.setLayout(new FlowLayout(FlowLayout.CENTER));
        pUpper.setLayout(new BorderLayout());
        pUpper.add(pParams, BorderLayout.WEST);
        // pUpper.add(pPreview, BorderLayout.CENTER);
        pUpper.add(m_pSouthParams, BorderLayout.SOUTH);

        // lay out the combo boxes
        pParams.setLayout(new GridLayout(4, 2));

        if (unitType == -1) {
            pParams.add(labelUnit);
            pParams.add(chUnit);
        }
        pParams.add(labelWeightClass);
        pParams.add(chWeightClass);
        pParams.add(labelType);
        pParams.add(chType);
        pParams.add(labelSort);
        pParams.add(chSort);
        pParams.doLayout();

        populateChoices();
        populateJComboBoxs();
        buildSouthParams(false);

        getContentPane().add(springHolder);

        clearMechPreview();
        setSize(785, 560);
        setResizable(false);

        // add all the listeners
        chWeightClass.addItemListener(this);
        chType.addItemListener(this);
        chSort.addItemListener(this);
        chUnit.addItemListener(this);
        mechList.addListSelectionListener(this);
        mechList.addKeyListener(this);
        bCancel.addActionListener(this);
        bSelect.addActionListener(this);
        m_bSearch.addActionListener(this);
        m_bReset.addActionListener(this);
        m_bToggleAdvanced.addActionListener(this);
        addWindowListener(this);
    }

    public void run() {

        // Loading mechs can take a while, so it will have its own thread.
        // This prevents the UI from freezing, and allows the
        // "Please wait..." dialog to behave properly on various Java VMs.

        filterMechs();
        //unitLoadingDialog.setVisible(false);

        final Map<String, String> hFailedFiles = MechSummaryCache.getInstance().getFailedFiles();
        if (hFailedFiles != null && hFailedFiles.size() > 0) {
            new UnitFailureDialog(clientgui, hFailedFiles); // self-showing
            // dialog
        }

        //try {
          //  String previousIndex = CConfig.getParam("UNITVIEWERUNIT");
            //mechList.setSelectedValue(previousIndex, true);
        //} catch (Exception e) {
            mechList.setSelectedIndex(-1);
        //}

        // pPreview.setVisible(true);
        setVisible(true);
        mechList.requestFocus();
    }

    private void buildSouthParams(boolean showAdvanced) {
        if (showAdvanced) {
            m_bToggleAdvanced.setText("> Hide Advanced Search <");
            m_pOpenAdvanced.add(m_bToggleAdvanced);

            m_pSouthParams.setLayout(new GridLayout(5, 1));
            m_pSouthParams.add(m_pOpenAdvanced);

            JPanel row1 = new JPanel();
            row1.setLayout(new FlowLayout(FlowLayout.CENTER));
            row1.add(new JLabel("Walk"));
            row1.add(m_cWalk);
            row1.add(m_tWalk);
            row1.add(new JLabel("Jump"));
            row1.add(m_cJump);
            row1.add(m_tJump);
            row1.add(new JLabel("Armor"));
            row1.add(m_cArmor);
            m_pSouthParams.add(row1);

            JPanel row2 = new JPanel();
            row2.setLayout(new FlowLayout(FlowLayout.LEFT));
            row2.add(new JLabel("Weapons:"));
            row2.add(new JLabel("At least"));
            row2.add(m_tWeapons1);
            row2.add(m_cWeapons1);
            row2.add(m_cOrAnd);
            row2.add(new JLabel("At least"));
            row2.add(m_tWeapons2);
            row2.add(m_cWeapons2);
            m_pSouthParams.add(row2);

            JPanel row3 = new JPanel();
            row3.setLayout(new FlowLayout(FlowLayout.CENTER));
            row3.add(new JLabel("Equipment"));
            row3.add(m_chkEquipment);
            row3.add(m_cEquipment);
            m_pSouthParams.add(row3);

            JPanel row4 = new JPanel();
            row4.add(m_bSearch);
            row4.add(m_bReset);
            row4.add(m_lCount);
            m_pSouthParams.add(row4);
        } else {
            m_bToggleAdvanced.setText("< Show Advanced Search >");
            m_pOpenAdvanced.add(m_bToggleAdvanced);

            m_pSouthParams.setLayout(new GridLayout(2, 1));
            m_pSouthParams.add(m_pOpenAdvanced);
        }
        paintScreen(false);

    }

    private void toggleAdvanced() {
        pUpper.remove(m_pSouthParams);
        m_pSouthParams = new JPanel();
        buildSouthParams(m_bToggleAdvanced.getText().equals("< Show Advanced Search >"));
        pUpper.add(m_pSouthParams, BorderLayout.SOUTH);
        // invalidate();
        pack();
        repaint();
    }

    private void populateJComboBoxs() {
        //String weight = CConfig.getParam("UNITVIEWERWEIGHT");
        //chWeightClass.setSelectedItem(weight);
        //String tech = CConfig.getParam("UNITVIEWERTECH");
        //chType.setSelectedItem(tech);
    }

    private void saveComboBoxSettings() {
/*
        CConfig.setParam("UNITVIEWERWEIGHT", (String) chWeightClass.getSelectedItem());
        CConfig.setParam("UNITVIEWERTECH", (String) chType.getSelectedItem());
        if (mechList.getSelectedValue() != null) {
            CConfig.setParam("UNITVIEWERUNIT", mechList.getSelectedValue().toString());
        }
        CConfig.saveConfig();
 * */
    }

    private void filterMechs() {
        filterMechs(false);
    }

    private void filterMechs(boolean calledByAdvancedSearch) {

        Vector<MechSummary> vMechs = new Vector<MechSummary>(1, 1);

        int nType = chType.getSelectedIndex();
        int nClass = chWeightClass.getSelectedIndex();
        int nUnit = nUnitType;

        if (nUnit == -1) {
            nUnit = chUnit.getSelectedIndex();
        }

        MechSummary[] mechs = MechSummaryCache.getInstance().getAllMechs();

        // break out if there are no units to filter
        if (mechs == null) {
            System.err.println("No units to filter!");
            return;
        }

        try {
            for (MechSummary mech : mechs) {
                /*
                 * A hacky check which prevents errors units from being added to
                 * the unit viewer lists, leaving only "valid" units from the
                 * dataset.
                 */
                if (mech.getName().startsWith("Error")) {
                    continue;
                }

                if (/* Weight */
                (nClass == chWeightClass.getItemCount() - 1 || mech.getWeightClass() == nClass) &&
                /*
                 * Technology Level
                 */
                ((nType == TechConstants.T_ALL) || (nType == mech.getType()) || ((nType == TechConstants.T_IS_TW_ALL) && (mech.getType() <= TechConstants.T_IS_TW_NON_BOX || mech.getType() == TechConstants.T_INTRO_BOXSET)) || (nType == TechConstants.T_TW_ALL && (mech.getType() <= TechConstants.T_IS_TW_NON_BOX || mech.getType() <= TechConstants.T_INTRO_BOXSET || mech.getType() <= TechConstants.T_CLAN_TW))) && (nUnit == UnitType.SIZE || mech.getUnitType().equals(UnitType.getTypeName(nUnit)))) {
                    vMechs.add(mech);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("mechs size: " + mechs.length);
        }
        mechsCurrent = new MechSummary[vMechs.size()];
        m_count = vMechs.size();

        if (!calledByAdvancedSearch && (m_old_nType != nType || m_old_nUnitType != nUnit)) {
            populateWeaponsAndEquipmentChoices();
        }
        m_old_nType = nType;
        m_old_nUnitType = nUnit;
        vMechs.copyInto(mechsCurrent);
        sortMechs();
    }

    private void populateChoices() {

        for (int i = 0; i < EntityWeightClass.SIZE; i++) {
            chWeightClass.addItem(EntityWeightClass.getClassName(i));
        }
        chWeightClass.addItem("All"); //$NON-NLS-1$
        chWeightClass.setSelectedIndex(0);

        for (int i = 0; i < TechConstants.SIZE; i++) {
            chType.addItem(TechConstants.getLevelDisplayableName(i));
        }
        chType.setSelectedIndex(0);

        for (int i = 0; i < UnitType.SIZE; i++) {
            chUnit.addItem(UnitType.getTypeDisplayableName(i));
        }
        chUnit.setSelectedIndex(0);

        m_cWalk.addItem("At Least");
        m_cWalk.addItem("Equal To");
        m_cWalk.addItem("No More Than");
        m_cJump.addItem("At Least");
        m_cJump.addItem("Equal To");
        m_cJump.addItem("No More Than");
        m_cArmor.addItem("Any");
        m_cArmor.addItem("%25 maximum");
        m_cArmor.addItem("%50 maximum");
        m_cArmor.addItem("%75 maximum");
        m_cArmor.addItem("%90 maximum");
        m_cOrAnd.addItem("or");
        m_cOrAnd.addItem("and");
        populateWeaponsAndEquipmentChoices();
    }

    private void populateWeaponsAndEquipmentChoices() {
        m_cWeapons1.removeAllItems();
        m_cWeapons2.removeAllItems();
        m_cEquipment.removeAllItems();
        m_tWeapons1.setText("");
        m_tWeapons2.setText("");
        m_chkEquipment.setSelected(false);
        int nType = chType.getSelectedIndex();
        int uType = nUnitType;

        if (nUnitType == -1) {
            uType = chUnit.getSelectedIndex();
        }

        for (Enumeration<EquipmentType> e = EquipmentType.getAllTypes(); e.hasMoreElements();) {
            EquipmentType et = e.nextElement();
            if (et instanceof WeaponType && (et.getTechLevel() == nType || ((nType == TechConstants.T_ALL) || ((nType == TechConstants.T_IS_TW_ALL) && (et.getTechLevel() <= TechConstants.T_IS_TW_NON_BOX || et.getTechLevel() == TechConstants.T_INTRO_BOXSET)) || (nType == TechConstants.T_TW_ALL && (et.getTechLevel() <= TechConstants.T_IS_TW_NON_BOX || et.getTechLevel() <= TechConstants.T_INTRO_BOXSET || et.getTechLevel() <= TechConstants.T_CLAN_TW))))) {
                if (!(nUnitType == UnitType.SIZE) && ((UnitType.getTypeName(uType).equals("Mek") || UnitType.getTypeName(uType).equals("Tank")) && (et.hasFlag(WeaponType.F_INFANTRY) || et.hasFlag(WeaponType.F_INFANTRY_ONLY)))) {
                    continue;
                }
                m_cWeapons1.addItem(et.getName());
                m_cWeapons2.addItem(et.getName());
            }
            if (et instanceof MiscType && (et.getTechLevel() == nType || ((nType == TechConstants.T_ALL) || ((nType == TechConstants.T_IS_TW_ALL) && (et.getTechLevel() <= TechConstants.T_IS_TW_NON_BOX || et.getTechLevel() == TechConstants.T_INTRO_BOXSET)) || (nType == TechConstants.T_TW_ALL && (et.getTechLevel() <= TechConstants.T_IS_TW_NON_BOX || et.getTechLevel() <= TechConstants.T_INTRO_BOXSET || et.getTechLevel() <= TechConstants.T_CLAN_TW))))) {
                m_cEquipment.addItem(et.getName());
            }
        }
        try {
            m_cWeapons1.setSelectedIndex(-1);
            m_cWeapons2.setSelectedIndex(-1);
            m_cEquipment.setSelectedIndex(-1);
        } catch (IllegalArgumentException ex) {
            System.err.println("Error in Unit Viewer. Could not set slider indices to 0");
            ex.printStackTrace();
        }
        m_cWeapons1.invalidate();
        m_cWeapons2.invalidate();
        m_cEquipment.invalidate();
    }

    private void sortMechs() {
        Arrays.sort(mechsCurrent, new MechSummaryComparator(chSort.getSelectedIndex()));
        defaultModel.clear();
        try {
            mechList.setEnabled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            for (MechSummary element : mechsCurrent) {
                defaultModel.addElement(formatMech(element));
            }
        } finally {
            setCursor(Cursor.getDefaultCursor());
            mechList.setEnabled(true);
            // workaround for bug 1263380
            // mechList.setFont(mechList.getFont());
        }
        m_lCount.setText(mechsCurrent.length + "/" + m_count);
        // mechList.setPreferredSize(new Dimension(180,mechsCurrent.length *
        // 19));
        repaint();
    }

    private void searchFor(String search) {
        for (int i = 0; i < mechsCurrent.length; i++) {
            if (mechsCurrent[i].getName().toLowerCase().startsWith(search)) {
                mechList.setSelectedIndex(i);
                mechList.ensureIndexIsVisible(i);
                break;
            }
        }
    }

    @Override
    public void setVisible(boolean show) {
        setLocationRelativeTo(null);
        super.setVisible(show);
        pack();
    }

    private String formatMech(MechSummary ms) {

        String result = "";
        result = makeLength(ms.getModel(), 12) + " " + makeLength(ms.getChassis(), 10) + " " + makeLength(Integer.toString(ms.getYear()), 4) + " " + makeLength(Integer.toString(ms.getTons()), 3) + " " + makeLength(Integer.toString(ms.getBV()), 5);

        return result;
    }

    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == bCancel) {
            saveComboBoxSettings();
            selectedUnit = null;
            dispose();
        }
        if (ae.getSource() == bSelect) {
            saveComboBoxSettings();
            setVisible(false);
        } else if (ae.getSource().equals(m_bSearch)) {
            advancedSearch();
        } else if (ae.getSource().equals(m_bReset)) {
            resetSearch();
        } else if (ae.getSource().equals(m_bToggleAdvanced)) {
            toggleAdvanced();
        }
    }

    private void advancedSearch() {
        String s = m_lCount.getText();
        int first = Integer.parseInt(s.substring(0, s.indexOf('/')));
        int second = Integer.parseInt(s.substring(s.indexOf('/') + 1));
        if (first != second) {
            // Search already active, reset list before starting new one.
            filterMechs(true);
        }

        ArrayList<MechSummary> vMatches = new ArrayList<MechSummary>();
        for (MechSummary ms : mechsCurrent) {
            try {
                Entity entity = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
                if (isMatch(entity)) {
                    vMatches.add(ms);
                }
            } catch (EntityLoadingException ex) {
                // do nothing, I guess
            }
        }
        mechsCurrent = vMatches.toArray(new MechSummary[0]);
        clearMechPreview();
        sortMechs();
        paintScreen(false);
    }

    private boolean isMatch(Entity entity) {
        int walk = -1;
        try {
            walk = Integer.parseInt(m_tWalk.getText());
        } catch (NumberFormatException ne) {
            // never get here
        }
        if (walk > -1) {
            if (m_cWalk.getSelectedIndex() == 0) { // at least
                if (entity.getWalkMP() < walk) {
                    return false;
                }
            } else if (m_cWalk.getSelectedIndex() == 1) { // equal to
                if (walk != entity.getWalkMP()) {
                    return false;
                }
            } else if (m_cWalk.getSelectedIndex() == 2) { // not more than
                if (entity.getWalkMP() > walk) {
                    return false;
                }
            }
        }

        int jump = -1;
        try {
            jump = Integer.parseInt(m_tJump.getText());
        } catch (NumberFormatException ne) {
            // never get here
        }
        if (jump > -1) {
            if (m_cJump.getSelectedIndex() == 0) { // at least
                if (entity.getJumpMP() < jump) {
                    return false;
                }
            } else if (m_cJump.getSelectedIndex() == 1) { // equal to
                if (jump != entity.getJumpMP()) {
                    return false;
                }
            } else if (m_cJump.getSelectedIndex() == 2) { // not more than
                if (entity.getJumpMP() > jump) {
                    return false;
                }
            }
        }

        int sel = m_cArmor.getSelectedIndex();
        if (sel > 0) {
            int armor = entity.getTotalArmor();
            int maxArmor = entity.getTotalInternal() * 2 + 3;
            if (sel == 1) {
                if (armor < (maxArmor * .25)) {
                    return false;
                }
            } else if (sel == 2) {
                if (armor < (maxArmor * .5)) {
                    return false;
                }
            } else if (sel == 3) {
                if (armor < (maxArmor * .75)) {
                    return false;
                }
            } else if (sel == 4) {
                if (armor < (maxArmor * .9)) {
                    return false;
                }
            }
        }

        boolean weaponLine1Active = false;
        boolean weaponLine2Active = false;
        boolean foundWeapon1 = false;
        boolean foundWeapon2 = false;

        int count = 0;
        int weapon1 = -1;
        try {
            weapon1 = Integer.parseInt(m_tWeapons1.getText());
        } catch (NumberFormatException ne) {
            // never get here
        }
        if (weapon1 > -1) {
            weaponLine1Active = true;
            for (int i = 0; i < entity.getWeaponList().size(); i++) {
                WeaponType wt = (WeaponType) (entity.getWeaponList().get(i)).getType();
                if (wt.getName().equals(m_cWeapons1.getSelectedItem())) {
                    count++;
                }
            }
            if (count >= weapon1) {
                foundWeapon1 = true;
            }
        }

        count = 0;
        int weapon2 = -1;
        try {
            weapon2 = Integer.parseInt(m_tWeapons2.getText());
        } catch (NumberFormatException ne) {
            // never get here
        }
        if (weapon2 > -1) {
            weaponLine2Active = true;
            for (int i = 0; i < entity.getWeaponList().size(); i++) {
                WeaponType wt = (WeaponType) (entity.getWeaponList().get(i)).getType();
                if (wt.getName().equals(m_cWeapons2.getSelectedItem())) {
                    count++;
                }
            }
            if (count >= weapon2) {
                foundWeapon2 = true;
            }
        }

        if (weaponLine1Active && !weaponLine2Active && !foundWeapon1) {
            return false;
        }
        if (weaponLine2Active && !weaponLine1Active && !foundWeapon2) {
            return false;
        }
        if (weaponLine1Active && weaponLine2Active) {
            if (m_cOrAnd.getSelectedIndex() == 0 /* 0 is "or" choice */) {
                if (!foundWeapon1 && !foundWeapon2) {
                    return false;
                }
            } else { // "and" choice in effect
                if (!foundWeapon1 || !foundWeapon2) {
                    return false;
                }
            }
        }

        count = 0;
        if (m_chkEquipment.isSelected()) {
            for (Mounted m : entity.getMisc()) {
                MiscType mt = (MiscType) m.getType();
                if (mt.getName().equals(m_cEquipment.getSelectedItem())) {
                    count++;
                }
            }
            if (count < 1) {
                return false;
            }
        }

        return true;
    }

    private void resetSearch() {
        m_cWalk.setSelectedIndex(0);
        m_tWalk.setText("");
        m_cJump.setSelectedIndex(0);
        m_tJump.setText("");
        m_cArmor.setSelectedIndex(0);
        m_cOrAnd.setSelectedIndex(0);

        populateWeaponsAndEquipmentChoices();
        filterMechs(false);
        paintScreen(false);

    }

    /**
     * for compliance with ListSelectionListener
     */
    public void valueChanged(ListSelectionEvent event) {

        int selected = mechList.getSelectedIndex();
        if (selected == -1) {
            clearMechPreview();
            return;
        }
        // else
        MechSummary ms = mechsCurrent[selected];
        try {
            // For some unknown reason the base path gets screwed up after you
            // print so this sets the source file to the full path.
            ms.setSourceFile(new File(ms.getSourceFile().getAbsolutePath()));
            Entity entity = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
            previewMech(entity);
        } catch (EntityLoadingException ex) {
            System.out.println("Unable to load mech: " + ms.getSourceFile() + ": " + ms.getEntryName() + ": " + ex.getMessage());
            ex.printStackTrace();
            clearMechPreview();
            return;
        }
    }

    public void itemStateChanged(ItemEvent ie) {

        Object currSelection = mechList.getSelectedValue();

        if (ie.getSource() == chSort) {
            sortMechs();
        } else if (ie.getSource() == chWeightClass || ie.getSource() == chType || ie.getSource() == chUnit) {
            filterMechs();
        }

        // try to reselect the previous choice. if the choice cant be found,
        // the list automatically reverts to -1 (no selection)
        mechList.setSelectedValue(currSelection, true);
    }

    void clearMechPreview() {
        mechViewLeft.setEditable(false);
        mechViewRight.setEditable(false);
        unitFluff.setEditable(false);
        // fluffScrollPane.setVisible(false);
        mechViewLeft.setText("");
        mechViewRight.setText("");
        unitFluff.setText("");

        // Remove preview image.
        previewMech(null);

    }

    void previewMech(Entity entity) {

        selectedUnit = entity;
        boolean populateTextFields = true;

        paintScreen(entity != null && selectedUnit.getFluff() != null && viewFluff);

        // null entity, so load a default unit.
        if (entity == null) {
            /*
             * try { MechSummary ms =
             * MechSummaryCache.getInstance().getMech("Error OMG-UR-FD");
             * selectedUnit = new MechFileParser(ms.getSourceFile(),
             * ms.getEntryName()).getEntity(); populateTextFields = false; }
             * catch (EntityLoadingException e) { // this would be very very bad
             * ... }
             */
            return;
        }

        MechView mechView = null;
        try {
            mechView = new MechView(selectedUnit, true);
        } catch (Exception e) {
            // error unit didn't load right. this is bad news.
            populateTextFields = false;
        }

        mechViewLeft.setEditable(false);
        mechViewRight.setEditable(false);
        if (populateTextFields && mechView != null) {
            mechViewLeft.setText(mechView.getMechReadoutBasic());
            mechViewRight.setText(mechView.getMechReadoutLoadout());
            if (selectedUnit.getFluff() != null && viewFluff) {
                unitFluff.setEditable(false);
                unitFluff.setLineWrap(true);
                // unitFluff.setText(parseFluff(currEntity.getFluff()));
                unitFluff.setText(selectedUnit.getFluff());
                unitFluff.setCaretPosition(0);

            } else {
                unitFluff.setText("");
            }
        } else {
            mechViewLeft.setText("No unit selected");
            mechViewRight.setText("No unit selected");
        }
        mechViewLeft.setCaretPosition(0);
        mechViewRight.setCaretPosition(0);

        // Preview image of the unit...
        try {
            // ((MechInfo) pPreview).setUnit(currEntity);
            // ((MechInfo) pPreview).setImageVisible(true);
            // pPreview.paint(pPreview.getGraphics());
        } catch (Exception ex) {
            // shouldnt ever get here ...
        }
    }

    private static final String SPACES = "                        ";

    private String makeLength(String s, int nLength) {
        if (s.length() == nLength) {
            return s;
        } else if (s.length() > nLength) {
            return s.substring(0, nLength - 2) + "..";
        } else {
            return s + SPACES.substring(0, nLength - s.length());
        }
    }

    public void keyReleased(java.awt.event.KeyEvent ke) {
        // no action on release
    }

    public void keyPressed(java.awt.event.KeyEvent ke) {
        if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
            ActionEvent event = new ActionEvent(bCancel, ActionEvent.ACTION_PERFORMED, "");
            actionPerformed(event);
        }
        long curTime = System.currentTimeMillis();
        if (curTime - m_nLastSearch > KEY_TIMEOUT) {
            m_sbSearch = new StringBuilder();
        }
        m_nLastSearch = curTime;
        m_sbSearch.append(ke.getKeyChar());
        searchFor(m_sbSearch.toString().toLowerCase());
    }

    private void paintScreen(boolean fluff) {

        springHolder.removeAll();
        textBoxSpring.removeAll();
        fluffBoxSpring.removeAll();

        if (fluff) {
            // panel w/ 1x4 SpringLayout for the mechView bits
            textBoxSpring.add(listScrollPane);
            textBoxSpring.add(leftScrollPane);
            textBoxSpring.add(rightScrollPane);
            SpringLayoutHelper.setupSpringGrid(textBoxSpring, 3);
            fluffBoxSpring.add(fluffScrollPane);
            SpringLayoutHelper.setupSpringGrid(fluffBoxSpring, 1);
        } else {
            // panel w/ 1x3 SpringLayout for the mechView bits
            textBoxSpring.add(listScrollPane);
            textBoxSpring.add(leftScrollPane);
            textBoxSpring.add(rightScrollPane);
            SpringLayoutHelper.setupSpringGrid(textBoxSpring, 3);
        }

        // set up a formatting holder for the cancel button
        JPanel buttonHolder = new JPanel();
        buttonHolder.add(bSelect);
        buttonHolder.add(bCancel);

        // set up the overall SpringLayout
        springHolder.add(pUpper);
        // springHolder.add(flowHolder);
        springHolder.add(textBoxSpring);
        if (fluff) {
            springHolder.add(fluffBoxSpring);
        }
        springHolder.add(buttonHolder);
        SpringLayoutHelper.setupSpringGrid(springHolder, 1);
        pack();
        this.repaint();

        mechList.grabFocus();

    }

    public void keyTyped(java.awt.event.KeyEvent ke) {
    }

    // WindowListener
    public void windowActivated(java.awt.event.WindowEvent windowEvent) {
    }

    public void windowClosed(java.awt.event.WindowEvent windowEvent) {
    }

    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
        dispose();
    }

    public void windowDeactivated(java.awt.event.WindowEvent windowEvent) {
    }

    public void windowDeiconified(java.awt.event.WindowEvent windowEvent) {
    }

    public void windowIconified(java.awt.event.WindowEvent windowEvent) {
    }

    public void windowOpened(java.awt.event.WindowEvent windowEvent) {
    }

    public Entity getSelectedEntity() {
        return selectedUnit;

    }
}
