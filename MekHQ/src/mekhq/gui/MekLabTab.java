/*
 * MMLMekUICustom.java
 *
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
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
import javax.swing.SwingUtilities;

import megamek.common.*;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.verifier.*;
import megamek.logging.MMLogger;
import megameklab.MMLConstants;
import megameklab.ui.EntitySource;
import megameklab.ui.battleArmor.BABuildTab;
import megameklab.ui.battleArmor.BAEquipmentTab;
import megameklab.ui.battleArmor.BAStructureTab;
import megameklab.ui.combatVehicle.CVBuildTab;
import megameklab.ui.combatVehicle.CVEquipmentTab;
import megameklab.ui.combatVehicle.CVStructureTab;
import megameklab.ui.fighterAero.ASBuildTab;
import megameklab.ui.fighterAero.ASEquipmentTab;
import megameklab.ui.fighterAero.ASStructureTab;
import megameklab.ui.generalUnit.FluffTab;
import megameklab.ui.generalUnit.PreviewTab;
import megameklab.ui.generalUnit.TransportTab;
import megameklab.ui.infantry.CIStructureTab;
import megameklab.ui.largeAero.DSStructureTab;
import megameklab.ui.largeAero.LABuildTab;
import megameklab.ui.largeAero.LAEquipmentTab;
import megameklab.ui.largeAero.WSStructureTab;
import megameklab.ui.mek.BMBuildTab;
import megameklab.ui.mek.BMEquipmentTab;
import megameklab.ui.mek.BMStructureTab;
import megameklab.ui.protoMek.PMBuildTab;
import megameklab.ui.protoMek.PMEquipmentTab;
import megameklab.ui.protoMek.PMStructureTab;
import megameklab.ui.supportVehicle.SVArmorTab;
import megameklab.ui.supportVehicle.SVBuildTab;
import megameklab.ui.supportVehicle.SVEquipmentTab;
import megameklab.ui.supportVehicle.SVStructureTab;
import megameklab.ui.util.RefreshListener;
import megameklab.util.CConfig;
import megameklab.util.UnitUtil;
import mekhq.MekHQ;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.unit.Unit;
import mekhq.gui.enums.MHQTabType;
import mekhq.gui.utilities.JScrollPaneWithSpeed;

public class MekLabTab extends CampaignGuiTab {
    private static final MMLogger logger = MMLogger.create(MekLabTab.class);

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

    // region Constructors
    public MekLabTab(CampaignGUI gui, String name) {
        super(gui, name);
        this.campaignGUI = gui;
        this.repaint();
    }
    // endregion Constructors

    @Override
    public void initTab() {
        entityVerifier = EntityVerifier.getInstance(new File("data/mekfiles/UnitVerifierOptions.xml")); // TODO : Remove
                                                                                                        // inline file
                                                                                                        // path
        CConfig.load();
        UnitUtil.loadFonts();
        logger.info("Starting MegaMekLab version: " + MMLConstants.VERSION);
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

        // TODO: compare units dialog that pops up mek views back-to-back
    }

    @Override
    public void refreshAll() {

    }

    @Override
    public MHQTabType tabType() {
        return MHQTabType.MEK_LAB;
    }

    public Unit getUnit() {
        return unit;
    }

    public void loadUnit(Unit u) {
        unit = u;
        MekSummary mekSummary = MekSummaryCache.getInstance().getMek(unit.getEntity().getShortNameRaw());
        Entity entity;
        try {
            entity = (new MekFileParser(mekSummary.getSourceFile(), mekSummary.getEntryName())).getEntity();
        } catch (EntityLoadingException ex) {
            logger.error("", ex);
            return;
        }
        entity.setYear(unit.getCampaign().getGameYear());
        UnitUtil.updateLoadedUnit(entity);
        entity.setModel(entity.getModel() + " Mk II");
        removeAll();
        // We need to override the values in the MML properties file with the campaign
        // options settings.
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
        MekSummary mekSummary = MekSummaryCache.getInstance().getMek(unit.getEntity().getShortName());

        if (mekSummary == null) {
            logger.error(String.format(
                    "Cannot reset unit %s as it cannot be found in the cache.",
                    unit.getEntity().getDisplayName()));
            return;
        }

        Entity entity;
        try {
            entity = new MekFileParser(mekSummary.getSourceFile(), mekSummary.getEntryName()).getEntity();
        } catch (EntityLoadingException ex) {
            logger.error("", ex);
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
        refit = new Refit(unit, entity, true, false, true);
        testEntity = null;
        if (entity instanceof SmallCraft) {
            testEntity = new TestSmallCraft((SmallCraft) entity, entityVerifier.aeroOption, null);
        } else if (entity instanceof Jumpship) {
            testEntity = new TestAdvancedAerospace((Jumpship) entity, entityVerifier.aeroOption, null);
        } else if (entity.isSupportVehicle()) {
            testEntity = new TestSupportVehicle(entity, entityVerifier.tankOption, null);
        } else if (entity instanceof Aero) {
            testEntity = new TestAero((Aero) entity, entityVerifier.aeroOption, null);
        } else if (entity instanceof Mek) {
            testEntity = new TestMek((Mek) entity, entityVerifier.mekOption, null);
        } else if (entity instanceof Tank) {
            testEntity = new TestTank((Tank) entity, entityVerifier.tankOption, null);
        } else if (entity instanceof BattleArmor) {
            testEntity = new TestBattleArmor((BattleArmor) entity, entityVerifier.baOption, null);
        } else if (entity instanceof Infantry) {
            testEntity = new TestInfantry((Infantry) entity, entityVerifier.tankOption, null);
        } else if (entity instanceof ProtoMek) {
            testEntity = new TestProtoMek((ProtoMek) entity, entityVerifier.protomekOption, null);
        }
        if (null == testEntity) {
            return;
        }
        StringBuffer sb = new StringBuffer();
        testEntity.correctEntity(sb);

        int walk = entity.getOriginalWalkMP();
        int run = entity.getRunMP(MPCalculationSetting.NO_MASC);
        int jump = entity.getOriginalJumpMP();
        int heat = entity.getHeatCapacity();

        double totalHeat = calculateTotalHeat();
        int bvDiff = entity.calculateBattleValue(true, true) - unit.getEntity().calculateBattleValue(true, true);
        double currentTonnage = testEntity.calculateWeight();
        currentTonnage += UnitUtil.getUnallocatedAmmoTonnage(entity);
        double tonnage = entity.getWeight();
        if (entity instanceof BattleArmor) {
            tonnage = ((BattleArmor) entity).getTrooperWeight() * ((BattleArmor) entity).getTroopers();
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
            lblBV.setText("<html>BV: " + entity.calculateBattleValue(true, true) + " (<font color='"
                    + MekHQ.getMHQOptions().getFontColorPositiveHexColor() + "'>+"
                    + bvDiff + "</font>)</html>");
        } else if (bvDiff < 0) {
            lblBV.setText("<html>BV: " + entity.calculateBattleValue(true, true) + " (<font color='"
                    + MekHQ.getMHQOptions().getFontColorNegativeHexColor() + "'>" + bvDiff
                    + "</font>)</html>");
        } else {
            lblBV.setText("<html>BV: " + entity.calculateBattleValue(true, true) + " (+" + bvDiff + ")</html>");
        }

        if (currentTonnage != tonnage) {
            lblTons.setText(
                    "<html>Tonnage: <font color='" + MekHQ.getMHQOptions().getFontColorNegativeHexColor() + "'>"
                            + currentTonnage + '/' + tonnage + "</font></html>");
        } else {
            lblTons.setText("Tonnage: " + currentTonnage + '/' + tonnage);
        }
        if (totalHeat > heat) {
            lblHeat.setText("<html>Heat: <font color='" + MekHQ.getMHQOptions().getFontColorNegativeHexColor() + "'>"
                    + totalHeat + '/' + heat + "</font></html>");
        } else {
            lblHeat.setText("<html>Heat: " + totalHeat + '/' + heat + "</html>");
        }
        shoppingPanel.removeAll();
        JLabel lblItem;
        // for (String name : refit.getShoppingListDescription()) {
        //     lblItem = new JLabel(name);
        //     shoppingPanel.add(lblItem);
        // }
        // if (refit.getShoppingListDescription().length == 0) {
        lblItem = new JLabel("None");
        shoppingPanel.add(lblItem);
        // }
    }

    public double calculateTotalHeat() {
        double heat = 0;
        Entity entity = labPanel.getEntity();

        if (entity.getOriginalJumpMP() > 0 && !(entity instanceof Infantry)) {
            if (entity.getJumpType() == Mek.JUMP_IMPROVED) {
                heat += Math.max(3, entity.getOriginalJumpMP() / 2);
            } else if (entity.getJumpType() != Mek.JUMP_BOOSTER) {
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

        if (entity instanceof Mek) {
            if (((Mek) entity).hasNullSig()) {
                heat += 10;
            }

            if (((Mek) entity).hasChameleonShield()) {
                heat += 6;
            }
        }

        for (Mounted<?> mounted : entity.getWeaponList()) {
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
            if ((wtype.getAmmoType() == AmmoType.T_SRM_STREAK)
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
            return new supportVehiclePanel(en);
        } else if (en instanceof Aero) {
            return new AeroPanel((Aero) en);
        } else if (en instanceof Mek) {
            return new MekPanel((Mek) en);
        } else if (en instanceof Tank) {
            return new TankPanel((Tank) en);
        } else if (en instanceof BattleArmor) {
            return new BattleArmorPanel((BattleArmor) en);
        } else if (en instanceof Infantry) {
            return new InfantryPanel((Infantry) en);
        } else if (en instanceof ProtoMek) {
            return new ProtoMekPanel((ProtoMek) en);
        } else {
            return null;
        }
    }

    private abstract static class EntityPanel extends JTabbedPane implements RefreshListener, EntitySource {

        private boolean refreshRequired = false;

        @Override
        public abstract Entity getEntity();

        abstract void setTechFaction(int techFaction);

        @Override
        public void scheduleRefresh() {
            refreshRequired = true;
            SwingUtilities.invokeLater(this::performRefresh);
        }

        private void performRefresh() {
            if (refreshRequired) {
                refreshRequired = false;
                refreshAll();
            }
        }
    }

    private class AeroPanel extends EntityPanel {
        private final Aero entity;
        private ASStructureTab structureTab;
        private ASEquipmentTab equipmentTab;
        private ASBuildTab buildTab;
        private PreviewTab previewTab;

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

            structureTab = new ASStructureTab(this);
            structureTab.setAsCustomization();
            previewTab = new PreviewTab(this);
            equipmentTab = new ASEquipmentTab(this);
            buildTab = new ASBuildTab(this);
            FluffTab fluffTab = new FluffTab(this);
            structureTab.addRefreshedListener(this);
            equipmentTab.addRefreshedListener(this);
            buildTab.addRefreshedListener(this);
            fluffTab.setRefreshedListener(this);

            addTab("Structure/Armor", new JScrollPaneWithSpeed(structureTab));
            addTab("Equipment", new JScrollPaneWithSpeed(equipmentTab));
            addTab("Assign Criticals", new JScrollPaneWithSpeed(buildTab));
            addTab("Fluff", new JScrollPaneWithSpeed(fluffTab));
            addTab("Preview", new JScrollPaneWithSpeed(previewTab));
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
        private final SmallCraft entity;
        private DSStructureTab structureTab;
        private LAEquipmentTab equipmentTab;
        private LABuildTab buildTab;
        private TransportTab transportTab;
        private PreviewTab previewTab;

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

            structureTab = new DSStructureTab(this);
            structureTab.setAsCustomization();
            previewTab = new PreviewTab(this);
            equipmentTab = new LAEquipmentTab(this);
            buildTab = new LABuildTab(this);
            FluffTab fluffTab = new FluffTab(this);
            transportTab = new TransportTab(this);
            structureTab.addRefreshedListener(this);
            equipmentTab.addRefreshedListener(this);
            buildTab.addRefreshedListener(this);
            transportTab.addRefreshedListener(this);
            fluffTab.setRefreshedListener(this);

            addTab("Structure/Armor", new JScrollPaneWithSpeed(structureTab));
            addTab("Equipment", new JScrollPaneWithSpeed(equipmentTab));
            addTab("Assign Criticals", new JScrollPaneWithSpeed(buildTab));
            addTab("Transport Bays", new JScrollPaneWithSpeed(transportTab));
            addTab("Fluff", new JScrollPaneWithSpeed(fluffTab));
            addTab("Preview", new JScrollPaneWithSpeed(previewTab));
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
        private final Mek entity;
        private BMStructureTab structureTab;
        private BMEquipmentTab equipmentTab;
        private BMBuildTab buildTab;
        private PreviewTab previewTab;

        public MekPanel(Mek m) {
            entity = m;
            reloadTabs();
        }

        @Override
        public Entity getEntity() {
            return entity;
        }

        public void reloadTabs() {
            removeAll();

            structureTab = new BMStructureTab(this);
            structureTab.setAsCustomization();
            equipmentTab = new BMEquipmentTab(this);
            previewTab = new PreviewTab(this);
            buildTab = new BMBuildTab(this);
            FluffTab fluffTab = new FluffTab(this);
            structureTab.addRefreshedListener(this);
            equipmentTab.addRefreshedListener(this);
            buildTab.addRefreshedListener(this);
            fluffTab.setRefreshedListener(this);

            addTab("Structure/Armor", new JScrollPaneWithSpeed(structureTab));
            addTab("Equipment", new JScrollPaneWithSpeed(equipmentTab));
            addTab("Assign Critical", new JScrollPaneWithSpeed(buildTab));
            addTab("Fluff", new JScrollPaneWithSpeed(fluffTab));
            addTab("Preview", new JScrollPaneWithSpeed(previewTab));
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
        private final Tank entity;
        private CVStructureTab structureTab;
        private CVEquipmentTab equipmentTab;
        private CVBuildTab buildTab;
        private PreviewTab previewTab;

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

            structureTab = new CVStructureTab(this);
            structureTab.setAsCustomization();
            equipmentTab = new CVEquipmentTab(this);
            buildTab = new CVBuildTab(this);
            previewTab = new PreviewTab(this);
            FluffTab fluffTab = new FluffTab(this);
            structureTab.addRefreshedListener(this);
            equipmentTab.addRefreshedListener(this);
            buildTab.addRefreshedListener(this);
            fluffTab.setRefreshedListener(this);

            addTab("Structure", new JScrollPaneWithSpeed(structureTab));
            addTab("Equipment", new JScrollPaneWithSpeed(equipmentTab));
            addTab("Build", new JScrollPaneWithSpeed(buildTab));
            addTab("Fluff", new JScrollPaneWithSpeed(fluffTab));
            addTab("Preview", new JScrollPaneWithSpeed(previewTab));
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

    private class supportVehiclePanel extends EntityPanel {
        private final Entity entity;
        private SVStructureTab structureTab;
        private SVArmorTab armorTab;
        private SVEquipmentTab equipmentTab;
        private SVBuildTab buildTab;
        private TransportTab transportTab;
        private PreviewTab previewTab;

        supportVehiclePanel(Entity en) {
            entity = en;
            reloadTabs();
        }

        @Override
        public Entity getEntity() {
            return entity;
        }

        void reloadTabs() {
            removeAll();

            structureTab = new SVStructureTab(this);
            structureTab.setAsCustomization();
            armorTab = new SVArmorTab(this, getTechManager());
            equipmentTab = new SVEquipmentTab(this);
            buildTab = new SVBuildTab(this);
            transportTab = new TransportTab(this);
            previewTab = new PreviewTab((this));
            FluffTab fluffTab = new FluffTab(this);
            structureTab.addRefreshedListener(this);
            armorTab.addRefreshedListener(this);
            equipmentTab.addRefreshedListener(this);
            buildTab.addRefreshedListener(this);
            transportTab.addRefreshedListener(this);
            fluffTab.setRefreshedListener(this);

            addTab("Structure", new JScrollPaneWithSpeed(structureTab));
            addTab("Armor", new JScrollPaneWithSpeed(armorTab));
            addTab("Equipment", new JScrollPaneWithSpeed(equipmentTab));
            addTab("Build", new JScrollPaneWithSpeed(buildTab));
            addTab("Transport", new JScrollPaneWithSpeed(transportTab));
            addTab("Fluff", new JScrollPaneWithSpeed(fluffTab));
            addTab("Preview", new JScrollPaneWithSpeed(previewTab));
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
        private final BattleArmor entity;
        private BAStructureTab structureTab;
        private BAEquipmentTab equipmentTab;
        private BABuildTab buildTab;

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

            structureTab = new BAStructureTab(this);
            structureTab.setAsCustomization();
            equipmentTab = new BAEquipmentTab(this);
            buildTab = new BABuildTab(this);
            FluffTab fluffTab = new FluffTab(this);
            structureTab.addRefreshedListener(this);
            equipmentTab.addRefreshedListener(this);
            buildTab.addRefreshedListener(this);
            fluffTab.setRefreshedListener(this);

            addTab("Structure", new JScrollPaneWithSpeed(structureTab));
            addTab("Equipment", new JScrollPaneWithSpeed(equipmentTab));
            addTab("Assign Criticals", new JScrollPaneWithSpeed(buildTab));
            addTab("Fluff", new JScrollPaneWithSpeed(fluffTab));
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
        private final Infantry entity;
        private CIStructureTab structureTab;
        private PreviewTab previewTab;

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

            structureTab = new CIStructureTab(this);
            structureTab.setAsCustomization();
            structureTab.addRefreshedListener(this);
            previewTab = new PreviewTab(this);
            FluffTab fluffTab = new FluffTab(this);
            fluffTab.setRefreshedListener(this);

            addTab("Build", new JScrollPaneWithSpeed(structureTab));
            addTab("Fluff", new JScrollPaneWithSpeed(fluffTab));
            addTab("Preview", new JScrollPaneWithSpeed(previewTab));
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

    private class ProtoMekPanel extends EntityPanel {
        private final ProtoMek entity;
        private PMStructureTab structureTab;
        private PMEquipmentTab equipmentTab;
        private PMBuildTab buildTab;
        private PreviewTab previewTab;

        ProtoMekPanel(ProtoMek m) {
            entity = m;
            reloadTabs();
        }

        @Override
        public Entity getEntity() {
            return entity;
        }

        void reloadTabs() {
            removeAll();

            structureTab = new PMStructureTab(this);
            structureTab.setAsCustomization();
            equipmentTab = new PMEquipmentTab(this);
            previewTab = new PreviewTab(this);
            buildTab = new PMBuildTab(this, this);
            FluffTab fluffTab = new FluffTab(this);
            structureTab.addRefreshedListener(this);
            equipmentTab.addRefreshedListener(this);
            buildTab.addRefreshedListener(this);
            fluffTab.setRefreshedListener(this);

            addTab("Structure/Armor", new JScrollPaneWithSpeed(structureTab));
            addTab("Equipment", new JScrollPaneWithSpeed(equipmentTab));
            addTab("Assign Critical", new JScrollPaneWithSpeed(buildTab));
            addTab("FluffTab", new JScrollPaneWithSpeed(fluffTab));
            addTab("Preview", new JScrollPaneWithSpeed(previewTab));
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
            // trick to catch toggling the main gun location, which does not affect the
            // status bar
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
        private final Jumpship entity;
        private WSStructureTab structureTab;
        private LAEquipmentTab equipmentTab;
        private LABuildTab buildTab;
        private TransportTab transportTab;
        private PreviewTab previewTab;

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

            structureTab = new WSStructureTab(this);
            structureTab.setAsCustomization();
            previewTab = new PreviewTab(this);
            equipmentTab = new LAEquipmentTab(this);
            buildTab = new LABuildTab(this);
            transportTab = new TransportTab(this);
            FluffTab fluffTab = new FluffTab(this);
            structureTab.addRefreshedListener(this);
            equipmentTab.addRefreshedListener(this);
            buildTab.addRefreshedListener(this);
            transportTab.addRefreshedListener(this);
            fluffTab.setRefreshedListener(this);

            addTab("Structure/Armor", new JScrollPaneWithSpeed(structureTab));
            addTab("Equipment", new JScrollPaneWithSpeed(equipmentTab));
            addTab("Assign Criticals", new JScrollPaneWithSpeed(buildTab));
            addTab("Transport Bays", new JScrollPaneWithSpeed(transportTab));
            addTab("FluffTab", new JScrollPaneWithSpeed(fluffTab));
            addTab("Preview", new JScrollPaneWithSpeed(previewTab));
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
