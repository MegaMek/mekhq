/*
 * MMLMekUICustom.java
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

package mekhq;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultCaret;
import javax.swing.text.html.HTMLEditorKit;

import megamek.MegaMek;
import megamek.client.ui.swing.MechView;
import megamek.client.ui.swing.UnitLoadingDialog;
import megamek.common.BipedMech;
import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Mech;
import megamek.common.MechFileParser;
import megamek.common.QuadMech;
import megamek.common.TechConstants;
import megamek.common.UnitType;
import megamek.common.verifier.EntityVerifier;
import megamek.common.verifier.TestEntity;
import megamek.common.verifier.TestMech;
import megameklab.com.MegaMekLab;
import megameklab.com.ui.Mek.Header;
import megameklab.com.ui.Mek.StatusBar;
import megameklab.com.ui.Mek.tabs.ArmorTab;
import megameklab.com.ui.Mek.tabs.BuildTab;
import megameklab.com.ui.Mek.tabs.EquipmentTab;
import megameklab.com.ui.Mek.tabs.StructureTab;
import megameklab.com.ui.Mek.tabs.WeaponTab;
import megameklab.com.ui.dialog.UnitViewerDialog;
import megameklab.com.util.CConfig;
import megameklab.com.util.ConfigurationDialog;
import megameklab.com.util.RefreshListener;
import megameklab.com.util.UnitPrintManager;
import megameklab.com.util.UnitUtil;

public class MekLabPanel extends JPanel implements RefreshListener {

    private static final long serialVersionUID = -5836932822468918198L;

    Mech entity = null;
    JTabbedPane ConfigPane = new JTabbedPane(SwingConstants.TOP);
    JPanel contentPane;
    private StructureTab structureTab;
    private ArmorTab armorTab;
    private EquipmentTab equipmentTab;
    private WeaponTab weaponTab;
    private BuildTab buildTab;
    private Header header;
    private StatusBar statusbar;
    JPanel masterPanel = new JPanel();

    public MekLabPanel() {
        UnitUtil.loadFonts();
        new CConfig();
        MekHQApp.logMessage("Staring MegaMekLab version: " + MegaMekLab.VERSION);

        /*
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        	MekHQApp.logError("Setting look and feel failed: ");
        	MekHQApp.logError(e);
        }

*/
        // ConfigPane.setMinimumSize(new Dimension(300, 300));
        createNewMech(false);
        this.add(masterPanel);
        reloadTabs();
        repaint();
        refreshAll();
    }

    public void loadUnit(Mech entity) {

        this.entity = entity;

        UnitUtil.updateLoadedMech(entity);

        reloadTabs();
        this.repaint();
        refreshAll();
    }

    public void reloadTabs() {
        masterPanel.removeAll();
        ConfigPane.removeAll();

        masterPanel.setLayout(new BoxLayout(masterPanel, BoxLayout.Y_AXIS));

        structureTab = new StructureTab(entity);

        armorTab = new ArmorTab(entity);
        armorTab.setArmorType(entity.getArmorType(0));
        armorTab.refresh();

        header = new Header(entity);
        statusbar = new StatusBar(entity);
        equipmentTab = new EquipmentTab(entity);
        weaponTab = new WeaponTab(entity);
        buildTab = new BuildTab(entity, equipmentTab, weaponTab);
        header.addRefreshedListener(this);
        structureTab.addRefreshedListener(this);
        armorTab.addRefreshedListener(this);
        equipmentTab.addRefreshedListener(this);
        weaponTab.addRefreshedListener(this);
        buildTab.addRefreshedListener(this);

        ConfigPane.addTab("Structure", structureTab);
        ConfigPane.addTab("Armor", armorTab);
        ConfigPane.addTab("Equipment", equipmentTab);
        ConfigPane.addTab("Weapons", weaponTab);
        ConfigPane.addTab("Build", buildTab);

        masterPanel.add(header);
        masterPanel.add(ConfigPane);
        masterPanel.add(statusbar);

        refreshHeader();
        this.repaint();
    }

    public void createNewMech(boolean isQuad) {

        if (isQuad) {
            entity = new QuadMech(Mech.GYRO_STANDARD, Mech.COCKPIT_STANDARD);
        } else {
            entity = new BipedMech(Mech.GYRO_STANDARD, Mech.COCKPIT_STANDARD);
        }

        entity.setYear(2750);
        entity.setTechLevel(TechConstants.T_INTRO_BOXSET);
        entity.setWeight(25);
        entity.setEngine(new Engine(25, Engine.NORMAL_ENGINE, 0));
        entity.setArmorType(EquipmentType.T_ARMOR_STANDARD);
        entity.setStructureType(EquipmentType.T_STRUCTURE_STANDARD);

        entity.addGyro();
        entity.addEngineCrits();
        entity.addCockpit();
        UnitUtil.updateHeatSinks(entity, 10, "Single");

        entity.autoSetInternal();
        for (int loc = 0; loc < entity.locations(); loc++) {
            entity.setArmor(0, loc);
            entity.setArmor(0, loc, true);
        }

        entity.setChassis("New");
        entity.setModel("Mek");

    }

    public void refreshAll() {

        if ((structureTab.isQuad() && !(entity instanceof QuadMech)) || (!structureTab.isQuad() && (entity instanceof QuadMech))) {
            createNewMech(structureTab.isQuad());
            setVisible(false);
            reloadTabs();
            setVisible(true);
            repaint();
            refreshAll();
        }
        statusbar.refresh();
        structureTab.refresh();
        armorTab.refresh();
        equipmentTab.refresh();
        weaponTab.refresh();
        buildTab.refresh();
    }

    public void refreshArmor() {
        armorTab.refresh();
    }

    public void refreshBuild() {
        buildTab.refresh();
    }

    public void refreshEquipment() {
        equipmentTab.refresh();

    }

    public void refreshStatus() {
        statusbar.refresh();
    }

    public void refreshStructure() {
        structureTab.refresh();
    }

    public void refreshWeapons() {
        weaponTab.refresh();
    }

    public Mech getEntity() {
        return entity;
    }

    public static boolean isEntityValid (Mech entity) {
        EntityVerifier entityVerifier = new EntityVerifier(new File("data/mechfiles/UnitVerifierOptions.xml"));
        StringBuffer sb = new StringBuffer();
        TestEntity testEntity = null;

        testEntity = new TestMech(entity, entityVerifier.mechOption, null);

        testEntity.correctEntity(sb, true);

        return (sb.length() == 0);
    }

	@Override
	public void refreshHeader() {
		// TODO Auto-generated method stub
		
	}
}
