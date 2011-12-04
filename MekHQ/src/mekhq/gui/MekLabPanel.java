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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import megamek.common.AmmoType;
import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.Mech;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.Mounted;
import megamek.common.WeaponType;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.verifier.EntityVerifier;
import megamek.common.verifier.TestEntity;
import megamek.common.verifier.TestMech;
import megameklab.com.MegaMekLab;
import megameklab.com.ui.Mek.Header;
import megameklab.com.ui.Mek.tabs.ArmorTab;
import megameklab.com.ui.Mek.tabs.BuildTab;
import megameklab.com.ui.Mek.tabs.EquipmentTab;
import megameklab.com.ui.Mek.tabs.StructureTab;
import megameklab.com.ui.Mek.tabs.WeaponTab;
import megameklab.com.util.CConfig;
import megameklab.com.util.RefreshListener;
import megameklab.com.util.UnitUtil;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Unit;
import mekhq.campaign.parts.Refit;

public class MekLabPanel extends JPanel implements RefreshListener {

    private static final long serialVersionUID = -5836932822468918198L;

    CampaignGUI campaignGUI;
    
    Unit unit;
    Mech entity;
    TestEntity testEntity;
    EntityVerifier entityVerifier;
    Refit refit;
    JTabbedPane ConfigPane = new JTabbedPane(SwingConstants.TOP);
    JPanel summaryPane = new JPanel();
    private StructureTab structureTab;
    private ArmorTab armorTab;
    private EquipmentTab equipmentTab;
    private WeaponTab weaponTab;
    private BuildTab buildTab;
    private Header header;

    private JLabel lblName;
    
    private JPanel refitPanel;
    private JLabel lblRefit;
    private JLabel lblTime;
    private JLabel lblCost;
    
    private JButton btnRefit;
    private JButton btnClear;
    private JButton btnRemove;
    
    private JPanel statPanel;
    private JLabel lblMove;
    private JLabel lblBV;
    private JLabel lblHeat;
    private JLabel lblTons;
    
    public MekLabPanel(CampaignGUI gui) {
    	campaignGUI = gui;
		entityVerifier = new EntityVerifier(new File("data/mechfiles/UnitVerifierOptions.xml"));
        UnitUtil.loadFonts();
        new CConfig();
        MekHQ.logMessage("Staring MegaMekLab version: " + MegaMekLab.VERSION);
        btnRefit = new JButton("Begin Refit");
        btnRefit.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				campaignGUI.refitUnit(refit, true);
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
    	
    	MechSummary mechSummary = MechSummaryCache.getInstance().getMech(unit.getEntity().getShortNameRaw());
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
	        add(summaryPane, BorderLayout.LINE_START);

	        refreshHeader();
	        refreshAll();
        }
        
        this.repaint();
    }

    public void refreshAll() {
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
        //do nothing
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
    
    public Unit getUnit() {
    	return unit;
    }

	@Override
	public void refreshHeader() {
		//do nothing
	}
	
	public void refreshSummary() {
		refit = new Refit(unit, entity, true); 
        testEntity = new TestMech(entity, entityVerifier.mechOption, null);
        StringBuffer sb = new StringBuffer();
        testEntity.correctEntity(sb, true);
		
        int walk = entity.getOriginalWalkMP();
        int run = entity.getOriginalRunMPwithoutMASC();
        int jump = entity.getOriginalJumpMP();
        int heat = entity.getNumberOfSinks();
        if (entity.hasDoubleHeatSinks()) {
            heat *= 2;
        }
        double totalHeat = calculateTotalHeat();
		int bvDiff = entity.calculateBattleValue(true, true) - unit.getEntity().calculateBattleValue(true, true);
		float currentTonnage = testEntity.calculateWeight();
        currentTonnage += UnitUtil.getUnallocatedAmmoTonnage(entity);
        float tonnage = entity.getWeight();

        if(entity.getWeight() < testEntity.calculateWeight()) {
			btnRefit.setEnabled(false);
			btnRefit.setToolTipText("Unit is overweight.");
		} else if (entity.getWeight() > testEntity.calculateWeight()) {
			btnRefit.setEnabled(false);
			btnRefit.setToolTipText("Unit is underweight.");	
		} else if(sb.length() > 0) {
			btnRefit.setEnabled(false);
			btnRefit.setToolTipText(sb.toString());	
		} else if(null != refit.checkFixable()) {
			btnRefit.setEnabled(false);
			btnRefit.setToolTipText(refit.checkFixable());	
		} else if(refit.getRefitClass() == Refit.NO_CHANGE) {
        	btnRefit.setEnabled(false);
			btnRefit.setToolTipText("Nothing to change.");
        } else {
			btnRefit.setEnabled(true);
			btnRefit.setToolTipText(null);
		}
		
		summaryPane.removeAll();
		summaryPane.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		lblName = new JLabel("<html><b>" + unit.getName() + "</b></html>");
		lblRefit = new JLabel(refit.getRefitClassName());
		lblTime = new JLabel(refit.getTime() + " minutes");
		lblCost = new JLabel(Utilities.getCurrencyString(refit.getCost()));
		lblMove = new JLabel("Movement: " + walk + "/" + run + "/" + jump);
		if(bvDiff > 0) {
			lblBV = new JLabel("<html>BV: " + entity.calculateBattleValue(true, true) + " (<font color='green'>+" + bvDiff + "</font>)</html>");
		} else if(bvDiff < 0) {
			lblBV = new JLabel("<html>BV: " + entity.calculateBattleValue(true, true) + " (<font color='red'>" + bvDiff + "</font>)</html>");
		} else {
			lblBV = new JLabel("<html>BV: " + entity.calculateBattleValue(true, true) + " (+" + bvDiff + ")</html>");
		}
		
        if(currentTonnage != tonnage) {
            lblTons = new JLabel("<html>Tonnage: <font color='red'>" + currentTonnage + "/" + tonnage + "</font></html>");
        } else {
            lblTons = new JLabel("Tonnage: " + currentTonnage + "/" + tonnage);
        }
        if(totalHeat > heat) {
            lblHeat = new JLabel("<html>Heat: <font color='red'>" + totalHeat + "/" + heat + "</font></html>");
        } else {
            lblHeat = new JLabel("<html>Heat: " + totalHeat + "/" + heat + "</html>");
        }
        refitPanel = new JPanel();
        refitPanel.setLayout(new BoxLayout(refitPanel, BoxLayout.PAGE_AXIS));
        refitPanel.setBorder(BorderFactory.createTitledBorder("Refit Statistics"));

        refitPanel.add(lblRefit);
        refitPanel.add(lblTime);
        refitPanel.add(lblCost);

        statPanel = new JPanel();
        statPanel.setLayout(new BoxLayout(statPanel, BoxLayout.PAGE_AXIS));
        statPanel.setBorder(BorderFactory.createTitledBorder("Unit Statistics"));
        statPanel.add(lblMove);
        statPanel.add(lblBV);
        statPanel.add(lblTons);
        statPanel.add(lblHeat);
        
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(10,5,2,5);
		summaryPane.add(lblName,c);
		c.gridy++;
		c.insets = new Insets(0,5,2,5);
		summaryPane.add(statPanel,c);
		c.gridy++;
		summaryPane.add(refitPanel,c);
		c.gridy++;
		summaryPane.add(btnRefit,c);
		c.gridy++;
		summaryPane.add(btnClear,c);
		c.gridy++;
		c.weighty = 1.0;
		summaryPane.add(btnRemove,c);
		//TODO: compare units dialog that pops up mech views back-to-back
	}
	
	public double calculateTotalHeat() {
        double heat = 0;

        if (entity.getOriginalJumpMP() > 0) {
            if (entity.getJumpType() == Mech.JUMP_IMPROVED) {
                heat += Math.max(3, entity.getOriginalJumpMP() / 2);
            } else if (entity.getJumpType() != Mech.JUMP_BOOSTER) {
                heat += Math.max(3, entity.getOriginalJumpMP());
            }
            if (entity.getEngine().getEngineType() == Engine.XXL_ENGINE) {
                heat *= 2;
            }
        } else if (entity.getEngine().getEngineType() == Engine.XXL_ENGINE) {
            heat += 6;
        } else {
            heat += 2;
        }

        if (entity.hasNullSig()) {
            heat += 10;
        }

        if (entity.hasChameleonShield()) {
            heat += 6;
        }

        for (Mounted mounted : entity.getWeaponList()) {
            WeaponType wtype = (WeaponType) mounted.getType();
            double weaponHeat = wtype.getHeat();

            // only count non-damaged equipment
            if (mounted.isMissing() || mounted.isHit() || mounted.isDestroyed() || mounted.isBreached()) {
                continue;
            }

            // one shot weapons count 1/4
            if ((wtype.getAmmoType() == AmmoType.T_ROCKET_LAUNCHER) || wtype.hasFlag(WeaponType.F_ONESHOT)) {
                weaponHeat *= 0.25;
            }

            // double heat for ultras
            if ((wtype.getAmmoType() == AmmoType.T_AC_ULTRA) || (wtype.getAmmoType() == AmmoType.T_AC_ULTRA_THB)) {
                weaponHeat *= 2;
            }

            // Six times heat for RAC
            if (wtype.getAmmoType() == AmmoType.T_AC_ROTARY) {
                weaponHeat *= 6;
            }

            // half heat for streaks
            if ((wtype.getAmmoType() == AmmoType.T_SRM_STREAK) || (wtype.getAmmoType() == AmmoType.T_MRM_STREAK) || (wtype.getAmmoType() == AmmoType.T_LRM_STREAK)) {
                weaponHeat *= 0.5;
            }
            heat += weaponHeat;
        }
        return heat;
    }
}
