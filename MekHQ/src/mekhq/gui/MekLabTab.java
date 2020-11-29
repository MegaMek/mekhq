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

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import megamek.common.*;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.verifier.*;
import megameklab.com.MegaMekLab;
import megameklab.com.ui.EntitySource;
import megameklab.com.ui.tabs.FluffTab;
import megameklab.com.util.CConfig;
import megameklab.com.util.RefreshListener;
import megameklab.com.util.UnitUtil;
import mekhq.MekHQ;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.unit.Unit;

public class MekLabTab extends CampaignGuiTab {

    private static final long serialVersionUID = -5836932822468918198L;

    CampaignGUI campaignGUI;

    Unit unit;
    TestEntity testEntity;
    EntityVerifier entityVerifier;
    Refit refit;
    EntityPanel labPanel;
    JPanel emptyPanel;

    private JPanel summaryPane;
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

    MekLabTab(CampaignGUI gui, String name) {
        super(gui, name);
        this.campaignGUI = gui;

        this.repaint();
    }

    @Override
    public void initTab() {
        entityVerifier = EntityVerifier.getInstance(new File("data/mechfiles/UnitVerifierOptions.xml"));
        new CConfig();
        UnitUtil.loadFonts();
        MekHQ.getLogger().info(this, "Starting MegaMekLab version: " + MegaMekLab.VERSION);
        btnRefit = new JButton("Begin Refit");
        btnRefit.addActionListener(evt -> {
            Entity entity = labPanel.getEntity();
            if (null != entity && entity.getWeight() > testEntity.calculateWeight()) {
                int response = JOptionPane.showConfirmDialog(null, "This unit is underweight. Do you want to continue?",
                        "Underweight Unit", JOptionPane.YES_NO_OPTION);
                if (response == JOptionPane.NO_OPTION) {
                    return;
                }
            }
            campaignGUI.refitUnit(refit, true);
        });
        btnClear = new JButton("Clear Changes");
        btnClear.addActionListener(evt -> resetUnit());
        btnRemove = new JButton("Remove from Lab");
        btnRemove.addActionListener(evt -> clearUnit());

        setLayout(new BorderLayout());
        emptyPanel = new JPanel(new BorderLayout());
        emptyPanel.add(new JLabel("No Unit Loaded"), BorderLayout.PAGE_START);
        add(emptyPanel, BorderLayout.CENTER);

        summaryPane = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        refitPanel = new JPanel();
        refitPanel.setLayout(new BoxLayout(refitPanel, BoxLayout.PAGE_AXIS));
        refitPanel.setBorder(BorderFactory.createTitledBorder("Refit Statistics"));

        lblRefit = new JLabel();
        lblTime = new JLabel();
        lblCost = new JLabel();
        refitPanel.add(lblRefit);
        refitPanel.add(lblTime);
        refitPanel.add(lblCost);

        statPanel = new JPanel();
        statPanel.setLayout(new BoxLayout(statPanel, BoxLayout.PAGE_AXIS));
        statPanel.setBorder(BorderFactory.createTitledBorder("Unit Statistics"));
        lblMove = new JLabel();
        lblBV = new JLabel();
        lblTons = new JLabel();
        lblHeat = new JLabel();
        statPanel.add(lblMove);
        statPanel.add(lblBV);
        statPanel.add(lblTons);
        statPanel.add(lblHeat);

        shoppingPanel = new JPanel();
        shoppingPanel.setLayout(new BoxLayout(shoppingPanel, BoxLayout.PAGE_AXIS));
        shoppingPanel.setBorder(BorderFactory.createTitledBorder("Needed Parts"));

        lblName = new JLabel();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(10, 5, 2, 5);
        summaryPane.add(lblName, c);
        c.gridy++;
        c.insets = new Insets(0, 5, 2, 5);
        summaryPane.add(btnRefit, c);
        c.gridy++;
        summaryPane.add(btnClear, c);
        c.gridy++;
        summaryPane.add(btnRemove, c);
        c.gridy++;
        summaryPane.add(statPanel, c);
        c.gridy++;
        summaryPane.add(refitPanel, c);
        c.gridy++;
        c.weighty = 1.0;
        summaryPane.add(shoppingPanel, c);

        // TODO: compare units dialog that pops up mech views back-to-back
}

    @Override
    public void refreshAll() {
        // TODO Auto-generated method stub

    }

    @Override
    public GuiTabType tabType() {
        return GuiTabType.MEKLAB;
    }

    public Unit getUnit() {
        return unit;
    }

    public void loadUnit(Unit u) {
        unit = u;
        MechSummary mechSummary = MechSummaryCache.getInstance().getMech(unit.getEntity().getShortNameRaw());
        Entity entity = null;
        try {
            entity = (new MechFileParser(mechSummary.getSourceFile(), mechSummary.getEntryName())).getEntity();
        } catch (EntityLoadingException ex) {
            MekHQ.getLogger().error(getClass(), "loadUnit(Unit)", ex); //$NON-NLS-1$
            return;
        }
        entity.setYear(unit.getCampaign().getGameYear());
        UnitUtil.updateLoadedUnit(entity);
        entity.setModel(entity.getModel() + " Mk II");
        removeAll();
        // We need to override the values in the MML properties file with the campaign options settings.
        CConfig.setParam(CConfig.TECH_EXTINCT, String.valueOf(campaignGUI.getCampaign().showExtinct()));
        CConfig.setParam(CConfig.TECH_PROGRESSION, String.valueOf(campaignGUI.getCampaign().useVariableTechLevel()));
        CConfig.setParam(CConfig.TECH_SHOW_FACTION, String.valueOf(campaignGUI.getCampaign().getTechFaction() >= 0));
        CConfig.setParam(CConfig.TECH_UNOFFICAL_NO_YEAR, String.valueOf(campaignGUI.getCampaign().unofficialNoYear()));
        CConfig.setParam(CConfig.TECH_USE_YEAR, String.valueOf(campaignGUI.getCampaign().getGameYear()));
        CConfig.setParam(CConfig.TECH_YEAR, String.valueOf(campaignGUI.getCampaign().getGameYear()));
        labPanel = getCorrectLab(entity);
        labPanel.setTechFaction(campaignGUI.getCampaign().getTechFaction());
        refreshRefitSummary();
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
            entity = (new MechFileParser(mechSummary.getSourceFile(), mechSummary.getEntryName())).getEntity();
        } catch (EntityLoadingException ex) {
            MekHQ.getLogger().error(getClass(), "resetUnit()", ex); //$NON-NLS-1$
            return;
        }
        entity.setYear(unit.getCampaign().getGameYear());
        UnitUtil.updateLoadedUnit(entity);
        removeAll();
        labPanel = getCorrectLab(entity);
        refreshRefitSummary();
        add(summaryPane, BorderLayout.LINE_START);
        add(labPanel, BorderLayout.CENTER);
        labPanel.refreshAll();
    }

    public void refreshRefitSummary() {
        if (null == labPanel) {
            return;
        }
        Entity entity = labPanel.getEntity();
        if (null == entity) {
            return;
        }
        refit = new Refit(unit, entity, true, false);
        testEntity = null;
        if (entity instanceof SmallCraft) {
            testEntity = new TestSmallCraft((SmallCraft) entity, entityVerifier.aeroOption, null);
        } else if (entity instanceof Jumpship) {
            testEntity = new TestAdvancedAerospace((Jumpship) entity, entityVerifier.aeroOption, null);
        } else if (entity.isSupportVehicle()) {
            testEntity = new TestSupportVehicle(entity, entityVerifier.tankOption, null);
        } else if (entity instanceof Aero) {
            testEntity = new TestAero((Aero) entity, entityVerifier.aeroOption, null);
        } else if (entity instanceof Mech) {
            testEntity = new TestMech((Mech) entity, entityVerifier.mechOption, null);
        } else if (entity instanceof Tank) {
            testEntity = new TestTank((Tank) entity, entityVerifier.tankOption, null);
        } else if (entity instanceof BattleArmor) {
            testEntity = new TestBattleArmor((BattleArmor) entity, entityVerifier.baOption, null);
        } else if (entity instanceof Infantry) {
            testEntity = new TestInfantry((Infantry) entity, entityVerifier.tankOption, null);
        } else if (entity instanceof Protomech) {
            testEntity = new TestProtomech((Protomech) entity, entityVerifier.protomechOption, null);
        }
        if (null == testEntity) {
            return;
        }
        StringBuffer sb = new StringBuffer();
        testEntity.correctEntity(sb);

        int walk = entity.getOriginalWalkMP();
        int run = entity.getRunMP();
        if (entity instanceof Mech) {
            run = ((Mech) entity).getOriginalRunMPwithoutMASC();
        }
        int jump = entity.getOriginalJumpMP();
        int heat = entity.getHeatCapacity();

        double totalHeat = calculateTotalHeat();
        int bvDiff = entity.calculateBattleValue(true, true) - unit.getEntity().calculateBattleValue(true, true);
        double currentTonnage = testEntity.calculateWeight();
        currentTonnage += UnitUtil.getUnallocatedAmmoTonnage(entity);
        double tonnage = entity.getWeight();
        if (entity instanceof BattleArmor) {
            tonnage = ((BattleArmor)entity).getTrooperWeight() * ((BattleArmor)entity).getTroopers();
        }

        if (entity.getWeight() < testEntity.calculateWeight()) {
            btnRefit.setEnabled(false);
            btnRefit.setToolTipText("Unit is overweight.");
            // } else if (entity.getWeight() > testEntity.calculateWeight()) {
            // Taharqa: We are now going to allow users to build underweight
            // units, we will just give
            // them an are you sure warning pop up
            // btnRefit.setEnabled(false);
            // btnRefit.setToolTipText("Unit is underweight.");
        } else if (sb.length() > 0) {
            btnRefit.setEnabled(false);
            btnRefit.setToolTipText(sb.toString());
        } else if (null != refit.checkFixable()) {
            btnRefit.setEnabled(false);
            btnRefit.setToolTipText(refit.checkFixable());
        } else if (refit.getRefitClass() == Refit.NO_CHANGE && entity.getWeight() == testEntity.calculateWeight()) {
            btnRefit.setEnabled(false);
            btnRefit.setToolTipText("Nothing to change.");
        } else {
            btnRefit.setEnabled(true);
            btnRefit.setToolTipText(null);
        }

        lblName.setText("<html><b>" + unit.getName() + "</b></html>");
        lblRefit.setText(refit.getRefitClassName());
        lblTime.setText(refit.getTime() + " minutes");
        lblCost.setText(refit.getCost().toAmountAndSymbolString());
        lblMove.setText("Movement: " + walk + "/" + run + "/" + jump);
        if (bvDiff > 0) {
            lblBV.setText("<html>BV: " + entity.calculateBattleValue(true, true) + " (<font color='green'>+"
                    + bvDiff + "</font>)</html>");
        } else if (bvDiff < 0) {
            lblBV.setText("<html>BV: " + entity.calculateBattleValue(true, true) + " (<font color='red'>" + bvDiff
                    + "</font>)</html>");
        } else {
            lblBV.setText("<html>BV: " + entity.calculateBattleValue(true, true) + " (+" + bvDiff + ")</html>");
        }

        if (currentTonnage != tonnage) {
            lblTons.setText(
                    "<html>Tonnage: <font color='red'>" + currentTonnage + "/" + tonnage + "</font></html>");
        } else {
            lblTons.setText("Tonnage: " + currentTonnage + "/" + tonnage);
        }
        if (totalHeat > heat) {
            lblHeat.setText("<html>Heat: <font color='red'>" + totalHeat + "/" + heat + "</font></html>");
        } else {
            lblHeat.setText("<html>Heat: " + totalHeat + "/" + heat + "</html>");
        }
        shoppingPanel.removeAll();
        JLabel lblItem;
        for (String name : refit.getShoppingListDescription()) {
            lblItem = new JLabel(name);
            shoppingPanel.add(lblItem);
        }
        if (refit.getShoppingListDescription().length == 0) {
            lblItem = new JLabel("None");
            shoppingPanel.add(lblItem);
        }
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

        if (entity instanceof Mech) {
            if (((Mech) entity).hasNullSig()) {
                heat += 10;
            }

            if (((Mech) entity).hasChameleonShield()) {
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
            if ((wtype.getAmmoType() == AmmoType.T_SRM_STREAK) || (wtype.getAmmoType() == AmmoType.T_MRM_STREAK)
                    || (wtype.getAmmoType() == AmmoType.T_LRM_STREAK)) {
                weaponHeat *= 0.5;
            }
            heat += weaponHeat;
        }
        return heat;
    }

    private EntityPanel getCorrectLab(Entity en) {
        if (en instanceof SmallCraft) {
            return new DropshipPanel((SmallCraft) en);
        } else if (en instanceof Jumpship) {
            return new AdvancedAeroPanel((Jumpship) en);
        } else if (en.isSupportVehicle()) {
            return new SupportVehiclePanel(en);
        } else if (en instanceof Aero) {
            return new AeroPanel((Aero) en);
        } else if (en instanceof Mech) {
            return new MekPanel((Mech) en);
        } else if (en instanceof Tank) {
            return new TankPanel((Tank) en);
        } else if (en instanceof BattleArmor) {
            return new BattleArmorPanel((BattleArmor) en);
        } else if (en instanceof Infantry) {
            return new InfantryPanel((Infantry) en);
        } else if (en instanceof Protomech) {
            return new ProtomechPanel((Protomech) en);
        } else {
            return null;
        }
    }

    private abstract static class EntityPanel extends JTabbedPane implements RefreshListener, EntitySource {
        private static final long serialVersionUID = 6886946112861955446L;

        @Override
        public abstract Entity getEntity();

        abstract void setTechFaction(int techFaction);
    }

    private class AeroPanel extends EntityPanel {
        private static final long serialVersionUID = 6894731868670529166L;

        private Aero entity;
        private megameklab.com.ui.Aero.tabs.StructureTab structureTab;
        private megameklab.com.ui.Aero.tabs.EquipmentTab equipmentTab;
        private megameklab.com.ui.Aero.tabs.BuildTab buildTab;
        private megameklab.com.ui.tabs.PreviewTab previewTab;

        public AeroPanel(Aero a) {
            entity = a;
            reloadTabs();
        }

        @Override
        public Entity getEntity() {
            return entity;
        }

        public void reloadTabs() {
            removeAll();

            structureTab = new megameklab.com.ui.Aero.tabs.StructureTab(this);
            structureTab.setAsCustomization();
            previewTab = new megameklab.com.ui.tabs.PreviewTab(this);
            equipmentTab = new megameklab.com.ui.Aero.tabs.EquipmentTab(this);
            buildTab = new megameklab.com.ui.Aero.tabs.BuildTab(this, equipmentTab);
            FluffTab fluffTab = new FluffTab(this);
            structureTab.addRefreshedListener(this);
            equipmentTab.addRefreshedListener(this);
            buildTab.addRefreshedListener(this);
            fluffTab.setRefreshedListener(this);

            addTab("Structure/Armor", new JScrollPane(structureTab));
            addTab("Equipment", new JScrollPane(equipmentTab));
            addTab("Assign Criticals", new JScrollPane(buildTab));
            addTab("Fluff", new JScrollPane(fluffTab));
            addTab("Preview", new JScrollPane(previewTab));
            this.repaint();
        }

        @Override
        public void refreshAll() {
            structureTab.refresh();
            equipmentTab.refresh();
            buildTab.refresh();
            previewTab.refresh();
            refreshSummary();
        }

        @Override
        public void refreshArmor() {
            refreshSummary();
        }

        @Override
        public void refreshBuild() {
            buildTab.refresh();
            refreshSummary();
        }

        @Override
        public void refreshEquipment() {
            equipmentTab.refresh();
            refreshSummary();
        }

        @Override
        public void refreshTransport() {
            // not used for fighters
        }

        @Override
        public void refreshStatus() {
            refreshRefitSummary();
        }

        @Override
        public void refreshStructure() {
            structureTab.refresh();
            refreshSummary();
        }

        @Override
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

        @Override
        public void refreshSummary() {
            structureTab.refreshSummary();
        }

        @Override
        public void refreshEquipmentTable() {
            equipmentTab.refreshTable();
        }

        @Override
        public void createNewUnit(long entitytype, boolean isPrimitive, boolean isIndustrial, Entity oldUnit) {

        }

        @Override
        public ITechManager getTechManager() {
            if (null != structureTab) {
                return structureTab.getTechManager();
            }
            return null;
        }

        @Override
        void setTechFaction(int techFaction) {
            structureTab.setTechFaction(techFaction);
        }

    }

    private class DropshipPanel extends EntityPanel {
        private static final long serialVersionUID = 4348862352101110686L;

        private SmallCraft entity;
        private megameklab.com.ui.aerospace.DropshipStructureTab structureTab;
        private megameklab.com.ui.Aero.tabs.EquipmentTab equipmentTab;
        private megameklab.com.ui.aerospace.DropshipBuildTab buildTab;
        private megameklab.com.ui.tabs.TransportTab transportTab;
        private megameklab.com.ui.tabs.PreviewTab previewTab;

        public DropshipPanel(SmallCraft a) {
            entity = a;
            reloadTabs();
        }

        @Override
        public Entity getEntity() {
            return entity;
        }

        public void reloadTabs() {
            removeAll();

            structureTab = new megameklab.com.ui.aerospace.DropshipStructureTab(this);
            structureTab.setAsCustomization();
            previewTab = new megameklab.com.ui.tabs.PreviewTab(this);
            equipmentTab = new megameklab.com.ui.Aero.tabs.EquipmentTab(this);
            buildTab = new megameklab.com.ui.aerospace.DropshipBuildTab(this, equipmentTab);
            FluffTab fluffTab = new FluffTab(this);
            transportTab = new megameklab.com.ui.tabs.TransportTab(this);
            structureTab.addRefreshedListener(this);
            equipmentTab.addRefreshedListener(this);
            buildTab.addRefreshedListener(this);
            transportTab.addRefreshedListener(this);
            fluffTab.setRefreshedListener(this);

            addTab("Structure/Armor", new JScrollPane(structureTab));
            addTab("Equipment", new JScrollPane(equipmentTab));
            addTab("Assign Criticals", new JScrollPane(buildTab));
            addTab("Transport Bays", new JScrollPane(transportTab));
            addTab("Fluff", new JScrollPane(fluffTab));
            addTab("Preview", new JScrollPane(previewTab));
            this.repaint();
        }

        @Override
        public void refreshAll() {
            structureTab.refresh();
            equipmentTab.refresh();
            buildTab.refresh();
            transportTab.refresh();
            previewTab.refresh();
            refreshSummary();
        }

        @Override
        public void refreshArmor() {
            refreshSummary();
        }

        @Override
        public void refreshBuild() {
            buildTab.refresh();
            refreshSummary();
        }

        @Override
        public void refreshEquipment() {
            equipmentTab.refresh();
            refreshSummary();
        }

        @Override
        public void refreshTransport() {
            transportTab.refresh();
        }

        @Override
        public void refreshStatus() {
            refreshRefitSummary();
        }

        @Override
        public void refreshStructure() {
            structureTab.refresh();
            refreshSummary();
        }

        @Override
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

        @Override
        public void refreshSummary() {
            structureTab.refreshSummary();
        }

        @Override
        public void refreshEquipmentTable() {
            equipmentTab.refreshTable();
        }

        @Override
        public void createNewUnit(long entitytype, boolean isPrimitive, boolean isIndustrial, Entity oldUnit) {

        }

        @Override
        public ITechManager getTechManager() {
            if (null != structureTab) {
                return structureTab.getTechManager();
            }
            return null;
        }

        @Override
        void setTechFaction(int techFaction) {
            structureTab.setTechFaction(techFaction);
        }

    }

    private class MekPanel extends EntityPanel {
        private static final long serialVersionUID = 6894731868670529166L;

        private Mech entity;
        private megameklab.com.ui.Mek.tabs.StructureTab structureTab;
        private megameklab.com.ui.Mek.tabs.EquipmentTab equipmentTab;
        private megameklab.com.ui.Mek.tabs.BuildTab buildTab;
        private megameklab.com.ui.tabs.PreviewTab previewTab;

        public MekPanel(Mech m) {
            entity = m;
            reloadTabs();
        }

        @Override
        public Entity getEntity() {
            return entity;
        }

        public void reloadTabs() {
            removeAll();

            structureTab = new megameklab.com.ui.Mek.tabs.StructureTab(this);
            structureTab.setAsCustomization();
            equipmentTab = new megameklab.com.ui.Mek.tabs.EquipmentTab(this);
            previewTab = new megameklab.com.ui.tabs.PreviewTab(this);
            buildTab = new megameklab.com.ui.Mek.tabs.BuildTab(this, equipmentTab);
            FluffTab fluffTab = new FluffTab(this);
            structureTab.addRefreshedListener(this);
            equipmentTab.addRefreshedListener(this);
            buildTab.addRefreshedListener(this);
            fluffTab.setRefreshedListener(this);

            addTab("Structure/Armor", new JScrollPane(structureTab));
            addTab("Equipment", new JScrollPane(equipmentTab));
            addTab("Assign Critical", new JScrollPane(buildTab));
            addTab("Fluff", new JScrollPane(fluffTab));
            addTab("Preview", new JScrollPane(previewTab));
            this.repaint();
        }

        @Override
        public void refreshAll() {
            structureTab.refresh();
            equipmentTab.refresh();
            buildTab.refresh();
            previewTab.refresh();
            refreshSummary();
        }

        @Override
        public void refreshArmor() {
            refreshSummary();
        }

        @Override
        public void refreshBuild() {
            buildTab.refresh();
            refreshSummary();
        }

        @Override
        public void refreshEquipment() {
            equipmentTab.refresh();
            refreshSummary();
        }

        @Override
        public void refreshTransport() {
        }

        @Override
        public void refreshStatus() {
            refreshRefitSummary();
        }

        @Override
        public void refreshStructure() {
            structureTab.refresh();
            refreshSummary();
        }

        @Override
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

        @Override
        public void refreshSummary() {
            structureTab.refreshSummary();
        }

        @Override
        public void refreshEquipmentTable() {
            equipmentTab.refreshTable();
        }

        @Override
        public void createNewUnit(long entitytype, boolean isPrimitive, boolean isIndustrial, Entity oldUnit) {

        }

        @Override
        public ITechManager getTechManager() {
            if (null != structureTab) {
                return structureTab.getTechManager();
            }
            return null;
        }

        @Override
        void setTechFaction(int techFaction) {
            structureTab.setTechFaction(techFaction);
        }
    }

    private class TankPanel extends EntityPanel {
        private static final long serialVersionUID = 6894731868670529166L;

        private Tank entity;
        private megameklab.com.ui.Vehicle.tabs.StructureTab structureTab;
        private megameklab.com.ui.Vehicle.tabs.EquipmentTab equipmentTab;
        private megameklab.com.ui.Vehicle.tabs.BuildTab buildTab;
        private megameklab.com.ui.tabs.PreviewTab previewTab;

        public TankPanel(Tank t) {
            entity = t;
            reloadTabs();
        }

        @Override
        public Entity getEntity() {
            return entity;
        }

        public void reloadTabs() {
            removeAll();

            structureTab = new megameklab.com.ui.Vehicle.tabs.StructureTab(this);
            structureTab.setAsCustomization();
            equipmentTab = new megameklab.com.ui.Vehicle.tabs.EquipmentTab(this);
            buildTab = new megameklab.com.ui.Vehicle.tabs.BuildTab(this, equipmentTab.getEquipmentList());
            previewTab = new megameklab.com.ui.tabs.PreviewTab(this);
            FluffTab fluffTab = new FluffTab(this);
            structureTab.addRefreshedListener(this);
            equipmentTab.addRefreshedListener(this);
            buildTab.addRefreshedListener(this);
            fluffTab.setRefreshedListener(this);

            addTab("Structure", new JScrollPane(structureTab));
            addTab("Equipment", new JScrollPane(equipmentTab));
            addTab("Build", new JScrollPane(buildTab));
            addTab("Fluff", new JScrollPane(fluffTab));
            addTab("Preview", new JScrollPane(previewTab));
            this.repaint();
        }

        @Override
        public void refreshAll() {
            structureTab.refresh();
            equipmentTab.refresh();
            buildTab.refresh();
            previewTab.refresh();
            refreshSummary();
        }

        @Override
        public void refreshArmor() {
            refreshSummary();
        }

        @Override
        public void refreshBuild() {
            buildTab.refresh();
            refreshSummary();
        }

        @Override
        public void refreshEquipment() {
            equipmentTab.refresh();
            refreshSummary();
        }

        @Override
        public void refreshTransport() {
            // not used for vees
        }

        @Override
        public void refreshStatus() {
            refreshRefitSummary();
        }

        @Override
        public void refreshStructure() {
            structureTab.refresh();
            refreshSummary();
        }

        @Override
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

        @Override
        public void refreshSummary() {
            structureTab.refreshSummary();
        }

        @Override
        public void refreshEquipmentTable() {
            equipmentTab.refreshTable();
        }

        @Override
        public void createNewUnit(long entitytype, boolean isPrimitive, boolean isIndustrial, Entity oldUnit) {

        }

        @Override
        public ITechManager getTechManager() {
            if (null != structureTab) {
                return structureTab.getTechManager();
            }
            return null;
        }

        @Override
        void setTechFaction(int techFaction) {
            structureTab.setTechFaction(techFaction);
        }
    }

    private class SupportVehiclePanel extends EntityPanel {

        private static final long serialVersionUID = -2209864752115049947L;

        private Entity entity;
        private megameklab.com.ui.supportvehicle.SVStructureTab structureTab;
        private megameklab.com.ui.supportvehicle.SVArmorTab armorTab;
        private megameklab.com.ui.tabs.EquipmentTab equipmentTab;
        private megameklab.com.ui.supportvehicle.SVBuildTab buildTab;
        private megameklab.com.ui.tabs.TransportTab transportTab;
        private megameklab.com.ui.tabs.PreviewTab previewTab;

        SupportVehiclePanel(Entity en) {
            entity = en;
            reloadTabs();
        }

        @Override
        public Entity getEntity() {
            return entity;
        }

        void reloadTabs() {
            removeAll();

            structureTab = new megameklab.com.ui.supportvehicle.SVStructureTab(this);
            structureTab.setAsCustomization();
            armorTab = new megameklab.com.ui.supportvehicle.SVArmorTab(this, getTechManager());
            equipmentTab = new megameklab.com.ui.tabs.EquipmentTab(this);
            buildTab = new megameklab.com.ui.supportvehicle.SVBuildTab(this, equipmentTab);
            transportTab = new megameklab.com.ui.tabs.TransportTab(this);
            previewTab = new megameklab.com.ui.tabs.PreviewTab((this));
            FluffTab fluffTab = new FluffTab(this);
            structureTab.addRefreshedListener(this);
            armorTab.addRefreshedListener(this);
            equipmentTab.addRefreshedListener(this);
            buildTab.addRefreshedListener(this);
            transportTab.addRefreshedListener(this);
            fluffTab.setRefreshedListener(this);

            addTab("Structure", new JScrollPane(structureTab));
            addTab("Armor", new JScrollPane(armorTab));
            addTab("Equipment", new JScrollPane(equipmentTab));
            addTab("Build", new JScrollPane(buildTab));
            addTab("Transport", new JScrollPane(transportTab));
            addTab("Fluff", new JScrollPane(fluffTab));
            addTab("Preview", new JScrollPane(previewTab));
            this.repaint();
        }

        @Override
        public void refreshAll() {
            structureTab.refresh();
            armorTab.refresh();
            equipmentTab.refresh();
            buildTab.refresh();
            transportTab.refresh();
            previewTab.refresh();
            refreshSummary();
        }

        @Override
        public void refreshArmor() {
            armorTab.refresh();
            refreshSummary();
        }

        @Override
        public void refreshBuild() {
            buildTab.refresh();
            refreshSummary();
        }

        @Override
        public void refreshEquipment() {
            equipmentTab.refresh();
            refreshSummary();
        }

        @Override
        public void refreshTransport() {
            transportTab.refresh();
            refreshSummary();
        }

        @Override
        public void refreshStatus() {
            refreshRefitSummary();
        }

        @Override
        public void refreshStructure() {
            structureTab.refresh();
            refreshSummary();
        }

        @Override
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

        @Override
        public void refreshSummary() {
            structureTab.refreshSummary();
        }

        @Override
        public void refreshEquipmentTable() {
            equipmentTab.refreshTable();
        }

        @Override
        public void createNewUnit(long entitytype, boolean isPrimitive, boolean isIndustrial, Entity oldUnit) {
            // not used by MekHQ
        }

        @Override
        public ITechManager getTechManager() {
            if (null != structureTab) {
                return structureTab.getTechManager();
            }
            return null;
        }

        @Override
        void setTechFaction(int techFaction) {
            structureTab.setTechFaction(techFaction);
        }
    }

    private class BattleArmorPanel extends EntityPanel {

        private static final long serialVersionUID = 6894731868670529166L;

        private BattleArmor entity;
        private megameklab.com.ui.BattleArmor.tabs.StructureTab structureTab;
        private megameklab.com.ui.BattleArmor.tabs.EquipmentTab equipmentTab;
        private megameklab.com.ui.BattleArmor.tabs.BuildTab buildTab;

        public BattleArmorPanel(BattleArmor ba) {
            entity = ba;
            reloadTabs();
        }

        @Override
        public Entity getEntity() {
            return entity;
        }

        public void reloadTabs() {
            removeAll();

            structureTab = new megameklab.com.ui.BattleArmor.tabs.StructureTab(this);
            structureTab.setAsCustomization();
            equipmentTab = new megameklab.com.ui.BattleArmor.tabs.EquipmentTab(this);
            buildTab = new megameklab.com.ui.BattleArmor.tabs.BuildTab(this);
            FluffTab fluffTab = new FluffTab(this);
            structureTab.addRefreshedListener(this);
            equipmentTab.addRefreshedListener(this);
            buildTab.addRefreshedListener(this);
            fluffTab.setRefreshedListener(this);

            addTab("Structure", new JScrollPane(structureTab));
            addTab("Equipment", new JScrollPane(equipmentTab));
            addTab("Assign Criticals", new JScrollPane(buildTab));
            addTab("Fluff", new JScrollPane(fluffTab));
            this.repaint();
        }

        @Override
        public void refreshAll() {
            structureTab.refresh();
            equipmentTab.refresh();
            buildTab.refresh();
            refreshSummary();
        }

        @Override
        public void refreshArmor() {
            refreshSummary();
        }

        @Override
        public void refreshBuild() {
            buildTab.refresh();
            refreshSummary();
        }

        @Override
        public void refreshEquipment() {
            equipmentTab.refresh();
            refreshSummary();
        }

        @Override
        public void refreshTransport() {
            // not used for ba
        }

        @Override
        public void refreshStatus() {
            refreshRefitSummary();
        }

        @Override
        public void refreshStructure() {
            structureTab.refresh();
            refreshSummary();
        }

        @Override
        public void refreshWeapons() {
            refreshSummary();
        }

        @Override
        public void refreshHeader() {
        }

        @Override
        public void refreshPreview() {
            structureTab.refresh();
        }

        @Override
        public void refreshSummary() {
            // TODO Auto-generated method stub

        }

        @Override
        public void refreshEquipmentTable() {
            equipmentTab.refreshTable();
        }

        @Override
        public void createNewUnit(long entitytype, boolean isPrimitive, boolean isIndustrial, Entity oldUnit) {

        }

        @Override
        public ITechManager getTechManager() {
            if (null != structureTab) {
                return structureTab.getTechManager();
            }
            return null;
        }

        @Override
        void setTechFaction(int techFaction) {
            structureTab.setTechFaction(techFaction);
        }
    }

    private class InfantryPanel extends EntityPanel {
        private static final long serialVersionUID = 6894731868670529166L;

        private Infantry entity;
        private megameklab.com.ui.Infantry.tabs.StructureTab structureTab;
        private megameklab.com.ui.tabs.PreviewTab previewTab;

        public InfantryPanel(Infantry inf) {
            entity = inf;
            reloadTabs();
        }

        @Override
        public Entity getEntity() {
            return entity;
        }

        public void reloadTabs() {
            removeAll();

            structureTab = new megameklab.com.ui.Infantry.tabs.StructureTab(this);
            structureTab.setAsCustomization();
            structureTab.addRefreshedListener(this);
            previewTab = new megameklab.com.ui.tabs.PreviewTab(this);
            FluffTab fluffTab = new FluffTab(this);
            fluffTab.setRefreshedListener(this);

            addTab("Build", new JScrollPane(structureTab));
            addTab("Fluff", new JScrollPane(fluffTab));
            addTab("Preview", new JScrollPane(previewTab));
            this.repaint();
        }

        @Override
        public void refreshAll() {
            structureTab.refresh();
            previewTab.refresh();
            refreshSummary();
        }

        @Override
        public void refreshArmor() {
            refreshSummary();
        }

        @Override
        public void refreshBuild() {
            refreshSummary();
        }

        @Override
        public void refreshEquipment() {
            refreshSummary();
        }

        @Override
        public void refreshTransport() {
            // not used for infantry
        }

        @Override
        public void refreshStatus() {
            refreshRefitSummary();
        }

        @Override
        public void refreshStructure() {
            structureTab.refresh();
            refreshSummary();
        }

        @Override
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

        @Override
        public void refreshSummary() {

        }

        @Override
        public void refreshEquipmentTable() {

        }

        @Override
        public void createNewUnit(long entitytype, boolean isPrimitive, boolean isIndustrial, Entity oldUnit) {

        }

        @Override
        public ITechManager getTechManager() {
            if (null != structureTab) {
                return structureTab.getTechManager();
            }
            return null;
        }

        @Override
        void setTechFaction(int techFaction) {
            structureTab.setTechFaction(techFaction);
        }
    }

    private class ProtomechPanel extends EntityPanel {
        private static final long serialVersionUID = -4649180495358483182L;

        private Protomech entity;
        private megameklab.com.ui.protomek.ProtomekStructureTab structureTab;
        private megameklab.com.ui.tabs.EquipmentTab equipmentTab;
        private megameklab.com.ui.protomek.ProtomekBuildTab buildTab;
        private megameklab.com.ui.tabs.PreviewTab previewTab;

        ProtomechPanel(Protomech m) {
            entity = m;
            reloadTabs();
        }

        @Override
        public Entity getEntity() {
            return entity;
        }

        void reloadTabs() {
            removeAll();

            structureTab = new megameklab.com.ui.protomek.ProtomekStructureTab(this);
            structureTab.setAsCustomization();
            equipmentTab = new megameklab.com.ui.tabs.EquipmentTab(this);
            previewTab = new megameklab.com.ui.tabs.PreviewTab(this);
            buildTab = new megameklab.com.ui.protomek.ProtomekBuildTab(this, equipmentTab, this);
            FluffTab fluffTab = new FluffTab(this);
            structureTab.addRefreshedListener(this);
            equipmentTab.addRefreshedListener(this);
            buildTab.addRefreshedListener(this);
            fluffTab.setRefreshedListener(this);

            addTab("Structure/Armor", new JScrollPane(structureTab));
            addTab("Equipment", new JScrollPane(equipmentTab));
            addTab("Assign Critical", new JScrollPane(buildTab));
            addTab("FluffTab", new JScrollPane(fluffTab));
            addTab("Preview", new JScrollPane(previewTab));
            this.repaint();
        }

        @Override
        public void refreshAll() {
            structureTab.refresh();
            equipmentTab.refresh();
            buildTab.refresh();
            previewTab.refresh();
            refreshSummary();
        }

        @Override
        public void refreshArmor() {
            refreshSummary();
        }

        @Override
        public void refreshBuild() {
            buildTab.refresh();
            refreshSummary();
            // trick to catch toggling the main gun location, which does not affect the status bar
            refreshRefitSummary();
        }

        @Override
        public void refreshEquipment() {
            equipmentTab.refresh();
            refreshSummary();
        }

        @Override
        public void refreshTransport() {
        }

        @Override
        public void refreshStatus() {
            refreshRefitSummary();
        }

        @Override
        public void refreshStructure() {
            structureTab.refresh();
            refreshSummary();
        }

        @Override
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

        @Override
        public void refreshSummary() {
            structureTab.refreshSummary();
        }

        @Override
        public void refreshEquipmentTable() {
            equipmentTab.refreshTable();
        }

        @Override
        public void createNewUnit(long entitytype, boolean isPrimitive, boolean isIndustrial, Entity oldUnit) {
            // Not needed for MekHQ
        }

        @Override
        public ITechManager getTechManager() {
            if (null != structureTab) {
                return structureTab.getTechManager();
            }
            return null;
        }

        @Override
        void setTechFaction(int techFaction) {
            structureTab.setTechFaction(techFaction);
        }
    }

    private class AdvancedAeroPanel extends EntityPanel {
        private static final long serialVersionUID = 4031380495472570820L;

        private Jumpship entity;
        private megameklab.com.ui.aerospace.AdvancedAeroStructureTab structureTab;
        private megameklab.com.ui.Aero.tabs.EquipmentTab equipmentTab;
        private megameklab.com.ui.aerospace.DropshipBuildTab buildTab;
        private megameklab.com.ui.tabs.TransportTab transportTab;
        private megameklab.com.ui.tabs.PreviewTab previewTab;

        AdvancedAeroPanel(Jumpship a) {
            entity = a;
            reloadTabs();
        }

        @Override
        public Entity getEntity() {
            return entity;
        }

        void reloadTabs() {
            removeAll();

            structureTab = new megameklab.com.ui.aerospace.AdvancedAeroStructureTab(this);
            structureTab.setAsCustomization();
            previewTab = new megameklab.com.ui.tabs.PreviewTab(this);
            equipmentTab = new megameklab.com.ui.Aero.tabs.EquipmentTab(this);
            buildTab = new megameklab.com.ui.aerospace.DropshipBuildTab(this, equipmentTab);
            transportTab = new megameklab.com.ui.tabs.TransportTab(this);
            FluffTab fluffTab = new FluffTab(this);
            structureTab.addRefreshedListener(this);
            equipmentTab.addRefreshedListener(this);
            buildTab.addRefreshedListener(this);
            transportTab.addRefreshedListener(this);
            fluffTab.setRefreshedListener(this);

            addTab("Structure/Armor", new JScrollPane(structureTab));
            addTab("Equipment", new JScrollPane(equipmentTab));
            addTab("Assign Criticals", new JScrollPane(buildTab));
            addTab("Transport Bays", new JScrollPane(transportTab));
            addTab("FluffTab", new JScrollPane(fluffTab));
            addTab("Preview", new JScrollPane(previewTab));
            this.repaint();
        }

        @Override
        public void refreshAll() {
            structureTab.refresh();
            equipmentTab.refresh();
            buildTab.refresh();
            transportTab.refresh();
            previewTab.refresh();
            refreshSummary();
        }

        @Override
        public void refreshArmor() {
            refreshSummary();
        }

        @Override
        public void refreshBuild() {
            buildTab.refresh();
            refreshSummary();
        }

        @Override
        public void refreshEquipment() {
            equipmentTab.refresh();
            refreshSummary();
        }

        @Override
        public void refreshTransport() {
            transportTab.refresh();
        }

        @Override
        public void refreshStatus() {
            refreshRefitSummary();
        }

        @Override
        public void refreshStructure() {
            structureTab.refresh();
            refreshSummary();
        }

        @Override
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

        @Override
        public void refreshSummary() {
            structureTab.refreshSummary();
        }

        @Override
        public void refreshEquipmentTable() {
            equipmentTab.refreshTable();
        }

        @Override
        public void createNewUnit(long entitytype, boolean isPrimitive, boolean isIndustrial, Entity oldUnit) {
            // Not used by MekHQ
        }

        @Override
        public ITechManager getTechManager() {
            if (null != structureTab) {
                return structureTab.getTechManager();
            }
            return null;
        }

        @Override
        void setTechFaction(int techFaction) {
            structureTab.setTechFaction(techFaction);
        }
    }
}
