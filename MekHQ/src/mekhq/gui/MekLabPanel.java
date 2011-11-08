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

package mekhq.gui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import megamek.common.BipedMech;
import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Mech;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.QuadMech;
import megamek.common.TechConstants;
import megamek.common.loaders.EntityLoadingException;
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
import megameklab.com.util.CConfig;
import megameklab.com.util.RefreshListener;
import megameklab.com.util.UnitUtil;
import mekhq.MekHQ;
import mekhq.campaign.Refit;
import mekhq.campaign.Unit;

public class MekLabPanel extends JPanel implements RefreshListener {

    private static final long serialVersionUID = -5836932822468918198L;

    Unit unit;
    Mech entity;
    Refit refit;
    JTabbedPane ConfigPane = new JTabbedPane(SwingConstants.TOP);
    JPanel summaryPane = new JPanel();
    private StructureTab structureTab;
    private ArmorTab armorTab;
    private EquipmentTab equipmentTab;
    private WeaponTab weaponTab;
    private BuildTab buildTab;
    private Header header;
    private StatusBar statusbar;

    private JLabel lblName;
    private JLabel lblRefit;
    private JLabel lblTime;
    private JLabel lblCost;
    private JButton btnRefit;
    private JButton btnClear;
    private JButton btnRemove;
    
    public MekLabPanel() {
        UnitUtil.loadFonts();
        new CConfig();
        MekHQ.logMessage("Staring MegaMekLab version: " + MegaMekLab.VERSION);
        btnRefit = new JButton("Begin Refit");
        btnRefit.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				refitUnit();
			}
		});
        btnClear = new JButton("Clear Changes");
        btnClear.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				resetUnit();
			}
		});
        btnRemove = new JButton("Remove from Lab");
        btnRemove.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				clearUnit();
			}
		});
        reloadTabs();
        this.repaint();
    }

    public void loadUnit(Unit u) {
    	unit = u;
    	
    	MechSummary mechSummary = MechSummaryCache.getInstance().getMech(unit.getEntity().getShortName());
		Mech mech = null;
		try {
			Entity e = (new MechFileParser(mechSummary.getSourceFile(),mechSummary.getEntryName())).getEntity();
			if (e instanceof Mech) {
				mech = (Mech) e;
			}
		} catch (EntityLoadingException ex) {
			Logger.getLogger(CampaignGUI.class.getName()).log(Level.SEVERE, null, ex);
		}

        this.entity = mech;
        entity.setYear(unit.campaign.getCalendar().get(GregorianCalendar.YEAR));
        UnitUtil.updateLoadedMech(entity);
        reloadTabs();
        this.repaint();
    }

    public void clearUnit() {
    	this.unit = null;
    	this.entity = null;
    	reloadTabs();
        this.repaint();
    }
    
    public void resetUnit() {
    	MechSummary mechSummary = MechSummaryCache.getInstance().getMech(unit.getEntity().getShortName());
		Mech mech = null;
		try {
			Entity e = (new MechFileParser(mechSummary.getSourceFile(),mechSummary.getEntryName())).getEntity();
			if (e instanceof Mech) {
				mech = (Mech) e;
			}
		} catch (EntityLoadingException ex) {
			Logger.getLogger(CampaignGUI.class.getName()).log(Level.SEVERE, null, ex);
		}

        this.entity = mech;
        entity.setYear(unit.campaign.getCalendar().get(GregorianCalendar.YEAR));
        UnitUtil.updateLoadedMech(entity);
        reloadTabs();
        this.repaint();
    }
    
    public void refitUnit() {
    	//TODO: implement this
    	//select a model name
    	String s = (String)JOptionPane.showInputDialog(
                null,
                "Choose a new model name",
                "Designate Model",
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                entity.getModel() + " Mk II");
    	entity.setModel(s);
    	//equipment check?
		//check to see if user really wants to do it - give some info on what will be done
    	if(0 != JOptionPane.showConfirmDialog(null,
				"Are you sure you want to refit this unit?"
			, "Proceed?",
				JOptionPane.YES_NO_OPTION)) {
    		return;
    	}
		refit.complete();
		clearUnit();
		//select a tech to work on the unit
    }
    
    public void reloadTabs() {
        removeAll();
        ConfigPane.removeAll();

        setLayout(new BorderLayout());

        if(null == entity) {
        	add(new JLabel("No Unit Loaded"), BorderLayout.PAGE_START);
        } else {     
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
	
	        refreshSummary();
	        
	        add(ConfigPane, BorderLayout.CENTER);
	        add(statusbar, BorderLayout.PAGE_END);
	        add(summaryPane, BorderLayout.LINE_START);

	        refreshHeader();
	        refreshAll();
        }
        
        this.repaint();
    }

    public void refreshAll() {
        statusbar.refresh();
        structureTab.refresh();
        armorTab.refresh();
        equipmentTab.refresh();
        weaponTab.refresh();
        buildTab.refresh();
    }

    public void refreshArmor() {
        armorTab.refresh();
        refreshSummary();
    }

    public void refreshBuild() {
        buildTab.refresh();
        refreshSummary();
    }

    public void refreshEquipment() {
        equipmentTab.refresh();
        refreshSummary();
    }

    public void refreshStatus() {
        statusbar.refresh();
        refreshSummary();
    }

    public void refreshStructure() {
        structureTab.refresh();
        refreshSummary();
    }

    public void refreshWeapons() {
        weaponTab.refresh();
        refreshSummary();
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
	
	public void refreshSummary() {
		refit = new Refit(unit, entity);
		btnRefit.setEnabled(isEntityValid(entity));
		
		summaryPane.removeAll();
		summaryPane.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		lblName = new JLabel("<html><b>" + unit.getEntity().getDisplayName() + "</b></html>");
		lblRefit = new JLabel(refit.getRefitClassName());
		lblTime = new JLabel(refit.getTime() + " minutes");
		lblCost = new JLabel(refit.getCost() + " C-Bills");
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(10,5,2,5);
		summaryPane.add(lblName,c);
		c.gridy = 1;
		c.insets = new Insets(0,5,2,5);
		summaryPane.add(lblRefit,c);
		c.gridy = 2;
		summaryPane.add(lblTime,c);
		c.gridy = 3;
		summaryPane.add(lblCost,c);
		c.gridy = 4;
		summaryPane.add(btnRefit,c);
		c.gridy = 5;
		summaryPane.add(btnClear,c);
		c.gridy = 6;
		c.weighty = 1.0;
		summaryPane.add(btnRemove,c);
		//TODO: choose tech to work on refit
		//TODO: compare units dialog that pops up mech views back-to-back
	}
	
}
