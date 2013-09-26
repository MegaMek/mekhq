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

import megamek.common.AmmoType;
import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.Mounted;
import megamek.common.Tank;
import megamek.common.WeaponType;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.verifier.EntityVerifier;
import megamek.common.verifier.TestEntity;
import megamek.common.verifier.TestInfantry;
import megamek.common.verifier.TestMech;
import megamek.common.verifier.TestTank;
import megameklab.com.MegaMekLab;
import megameklab.com.util.CConfig;
import megameklab.com.util.RefreshListener;
import megameklab.com.util.UnitUtil;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.unit.Unit;

public class MekLabPanel extends JPanel {

    private static final long serialVersionUID = -5836932822468918198L;

    CampaignGUI campaignGUI;
    
    Unit unit;
    TestEntity testEntity;
    EntityVerifier entityVerifier;
    Refit refit;
    EntityPanel labPanel;
    JPanel summaryPane = new JPanel();
    JPanel emptyPanel;
   
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
    
    private JPanel shoppingPanel;
    
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
        initComponents();
        this.repaint();
    }
    
    public void initComponents() {
    	setLayout(new BorderLayout());
    	emptyPanel = new JPanel(new BorderLayout());
		emptyPanel.add(new JLabel("No Unit Loaded"), BorderLayout.PAGE_START);
		add(emptyPanel, BorderLayout.CENTER);
    }

    public Unit getUnit() {
    	return unit;
    }
    
    public void loadUnit(Unit u) {
    	unit = u;
    	MechSummary mechSummary = MechSummaryCache.getInstance().getMech(unit.getEntity().getShortNameRaw());
		Entity entity = null;
		try {
			entity = (new MechFileParser(mechSummary.getSourceFile(),mechSummary.getEntryName())).getEntity();
		} catch (EntityLoadingException ex) {
			Logger.getLogger(CampaignGUI.class.getName()).log(Level.SEVERE, null, ex);
		}
        entity.setYear(unit.campaign.getCalendar().get(GregorianCalendar.YEAR));
        UnitUtil.updateLoadedMech(entity);
        entity.setModel(entity.getModel() + " Mk II");
        removeAll();
        labPanel = getCorrectLab(entity);
        refreshSummary();
    	add(summaryPane, BorderLayout.LINE_START);
    	add(labPanel, BorderLayout.CENTER);
    	labPanel.refreshAll();
    }

    public void clearUnit() {
    	this.unit = null;
        removeAll();
		add(emptyPanel, BorderLayout.CENTER);
		this.repaint();
    }
    
    public void resetUnit() {
    	MechSummary mechSummary = MechSummaryCache.getInstance().getMech(unit.getEntity().getShortName());
		Entity entity = null;
		try {
			entity = (new MechFileParser(mechSummary.getSourceFile(),mechSummary.getEntryName())).getEntity();
		} catch (EntityLoadingException ex) {
			Logger.getLogger(CampaignGUI.class.getName()).log(Level.SEVERE, null, ex);
		}
        entity.setYear(unit.campaign.getCalendar().get(GregorianCalendar.YEAR));
        UnitUtil.updateLoadedMech(entity);
        removeAll();
        labPanel = getCorrectLab(entity);
        refreshSummary();
    	add(summaryPane, BorderLayout.LINE_START);
    	add(labPanel, BorderLayout.CENTER);
    	labPanel.refreshAll();
    }

	public void refreshSummary() {
		if(null == labPanel) {
			return;
		}
		Entity entity = labPanel.getEntity();
		if(null == entity) {
			return;
		}
		refit = new Refit(unit, entity, true); 
		testEntity = null;
		if(entity instanceof Mech) {
			testEntity = new TestMech((Mech)entity, entityVerifier.mechOption, null);
		}
		else if(entity instanceof Tank) {
			testEntity = new TestTank((Tank)entity, entityVerifier.tankOption, null);
		}
		else if(entity instanceof Infantry) {
			testEntity = new TestInfantry((Infantry)entity, entityVerifier.tankOption, null);
		}
		if(null == testEntity) {
			return;
		}
        StringBuffer sb = new StringBuffer();
        testEntity.correctEntity(sb, true);
		
        int walk = entity.getOriginalWalkMP();
        int run = entity.getRunMP();
        if(entity instanceof Mech) {
        	run = ((Mech)entity).getOriginalRunMPwithoutMASC();
        }
        int jump = entity.getOriginalJumpMP();
        int heat = entity.getHeatCapacity();

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
        
        shoppingPanel = new JPanel();
        shoppingPanel.setLayout(new BoxLayout(shoppingPanel, BoxLayout.PAGE_AXIS));
        shoppingPanel.setBorder(BorderFactory.createTitledBorder("Needed Parts"));
        JLabel lblItem;
        for(String name : refit.getShoppingListDescription()) {
        	lblItem = new JLabel(name);
        	shoppingPanel.add(lblItem);
        }
        if(refit.getShoppingListDescription().length == 0) {
        	lblItem = new JLabel("None");
        	shoppingPanel.add(lblItem);
        }
        
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(10,5,2,5);
		summaryPane.add(lblName,c);
		c.gridy++;
		c.insets = new Insets(0,5,2,5);
		summaryPane.add(btnRefit,c);
		c.gridy++;
		summaryPane.add(btnClear,c);
		c.gridy++;
		summaryPane.add(btnRemove,c);
		c.gridy++;
		summaryPane.add(statPanel,c);
		c.gridy++;
		summaryPane.add(refitPanel,c);
		c.gridy++;
		c.weighty = 1.0;
		summaryPane.add(shoppingPanel,c);
		
		//TODO: compare units dialog that pops up mech views back-to-back
	}
	
	public double calculateTotalHeat() {
        double heat = 0;
        Entity entity = labPanel.getEntity();

        if (entity.getOriginalJumpMP() > 0 && !(entity instanceof Infantry)) {
            if (entity.getJumpType() == Mech.JUMP_IMPROVED) {
                heat += Math.max(3, entity.getOriginalJumpMP() / 2);
            } else if (entity.getJumpType() != Mech.JUMP_BOOSTER) {
                heat += Math.max(3, entity.getOriginalJumpMP());
            }
            if (entity.getEngine().getEngineType() == Engine.XXL_ENGINE) {
                heat *= 2;
            }
        } else if (!(entity instanceof Infantry) && entity.getEngine().getEngineType() == Engine.XXL_ENGINE) {
            heat += 6;
        } else if (!(entity instanceof Infantry)) {
            heat += 2;
        }

        if(entity instanceof Mech) {
	        if (((Mech)entity).hasNullSig()) {
	            heat += 10;
	        }
	
	        if (((Mech)entity).hasChameleonShield()) {
	            heat += 6;
	        }
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
	
	private EntityPanel getCorrectLab(Entity en) {
		if(en instanceof Mech) {
			return new MekPanel((Mech)en);
		}
		else if(en instanceof Tank) {
			return new TankPanel((Tank)en);
		}
		else if(en instanceof Infantry) {
			return new InfantryPanel((Infantry)en);
		}
		return null;
	}
	
	private abstract class EntityPanel extends JTabbedPane implements RefreshListener {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 6886946112861955446L;

		public abstract Entity getEntity();
	}
	
	private class MekPanel extends EntityPanel {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 6894731868670529166L;
	
		private Mech entity;
		private megameklab.com.ui.Mek.tabs.StructureTab structureTab;
		private megameklab.com.ui.Mek.tabs.EquipmentTab equipmentTab;
		private megameklab.com.ui.Mek.tabs.BuildTab buildTab;
	      private megameklab.com.ui.Mek.tabs.PreviewTab previewTab;
		
		public MekPanel(Mech m) {
			entity = m;
			reloadTabs();
		}
		 
		public Entity getEntity() {
			return entity;
		}
		
		public void reloadTabs() {
			removeAll();
			    
			structureTab = new megameklab.com.ui.Mek.tabs.StructureTab(entity);
			structureTab.setAsCustomization();
			equipmentTab = new megameklab.com.ui.Mek.tabs.EquipmentTab(entity);
	        previewTab = new megameklab.com.ui.Mek.tabs.PreviewTab(entity);
			buildTab = new megameklab.com.ui.Mek.tabs.BuildTab(entity, equipmentTab);
			structureTab.addRefreshedListener(this);
			equipmentTab.addRefreshedListener(this);
			buildTab.addRefreshedListener(this);

			addTab("Structure/Armor", structureTab);
			addTab("Equipment", equipmentTab);
			addTab("Build", buildTab);
	        addTab("Preview", previewTab);
	        this.repaint();
		}
		
		public void refreshAll() {
			structureTab.refresh();
			equipmentTab.refresh();
			buildTab.refresh();
			previewTab.refresh();
			refreshSummary();
		}

		public void refreshArmor() {
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
			refreshSummary();
		}

		public void refreshStructure() {
			structureTab.refresh();
			refreshSummary();
		}

		public void refreshWeapons() {
			refreshSummary();
		}

		@Override
		public void refreshHeader() {
			
		}

        @Override
        public void refreshPreview() {
            previewTab.refresh();
        }
	}
	
	private class TankPanel extends EntityPanel {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 6894731868670529166L;
	
		private Tank entity;
		private megameklab.com.ui.Vehicle.tabs.StructureTab structureTab;
		private megameklab.com.ui.Vehicle.tabs.EquipmentTab equipmentTab;
		private megameklab.com.ui.Vehicle.tabs.BuildTab buildTab;
		
		public TankPanel(Tank t) {
			entity = t;
			reloadTabs();
		}
		 
		public Entity getEntity() {
			return entity;
		}
		
		public void reloadTabs() {
			removeAll();
			    
			structureTab = new megameklab.com.ui.Vehicle.tabs.StructureTab(entity);
			equipmentTab = new megameklab.com.ui.Vehicle.tabs.EquipmentTab(entity);
			buildTab = new megameklab.com.ui.Vehicle.tabs.BuildTab(entity, equipmentTab);
			structureTab.addRefreshedListener(this);
			equipmentTab.addRefreshedListener(this);
			buildTab.addRefreshedListener(this);

			addTab("Structure", structureTab);
			addTab("Equipment", equipmentTab);
			addTab("Build", buildTab);
	        this.repaint();
		}
		
		public void refreshAll() {
			structureTab.refresh();
			equipmentTab.refresh();
			buildTab.refresh();
			refreshSummary();
		}

		public void refreshArmor() {
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
			refreshSummary();
		}

		public void refreshStructure() {
			structureTab.refresh();
			refreshSummary();
		}

		public void refreshWeapons() {
			refreshSummary();
		}

		@Override
		public void refreshHeader() {
			// TODO Auto-generated method stub
			
		}

        @Override
        public void refreshPreview() {
            // TODO Auto-generated method stub
            
        }
	}
	
	private class InfantryPanel extends EntityPanel {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 6894731868670529166L;
	
		private Infantry entity;
		private megameklab.com.ui.Infantry.tabs.StructureTab structureTab;
	    private megameklab.com.ui.Infantry.tabs.PreviewTab previewTab;

		
		public InfantryPanel(Infantry inf) {
			entity = inf;
			reloadTabs();
		}
		 
		public Entity getEntity() {
			return entity;
		}
		
		public void reloadTabs() {
			removeAll();
			    
			structureTab = new megameklab.com.ui.Infantry.tabs.StructureTab(entity);
	        structureTab.setAsCustomization();
			structureTab.addRefreshedListener(this);
	        previewTab = new megameklab.com.ui.Infantry.tabs.PreviewTab(entity);

			addTab("Build", structureTab);
	        addTab("Preview", previewTab);
	        this.repaint();
		}
		
		public void refreshAll() {
			structureTab.refresh();
	        previewTab.refresh();
			refreshSummary();
		}

		public void refreshArmor() {
			refreshSummary();
		}

		public void refreshBuild() {
			refreshSummary();
		}

		public void refreshEquipment() {
			refreshSummary();
		}

		public void refreshStatus() {
			refreshSummary();
		}

		public void refreshStructure() {
			structureTab.refresh();
			refreshSummary();
		}

		public void refreshWeapons() {
			refreshSummary();
		}

		@Override
		public void refreshHeader() {
			// TODO Auto-generated method stub
			
		}

        @Override
        public void refreshPreview() {
            previewTab.refresh();
        }
	}
}
