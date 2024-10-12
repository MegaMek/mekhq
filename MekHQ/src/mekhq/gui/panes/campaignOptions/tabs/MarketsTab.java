package mekhq.gui.panes.campaignOptions.tabs;

import megamek.client.ui.baseComponents.MMComboBox;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.personnel.Skills;
import mekhq.module.PersonnelMarketServiceManager;
import mekhq.module.api.PersonnelMarketMethod;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JSpinner.DefaultEditor;
import java.util.HashMap;
import java.util.Map;

import static mekhq.gui.panes.campaignOptions.tabs.CampaignOptionsUtilities.*;

public class MarketsTab {
    JFrame frame;
    String name;

    //start Personnel Market
    private JPanel pnlPersonnelMarketGeneralOptions;
    private JLabel lblPersonnelMarketType;
    private MMComboBox<String> comboPersonnelMarketType;
    private JCheckBox chkPersonnelMarketReportRefresh;
    private JCheckBox chkUsePersonnelHireHiringHallOnly;

    private JPanel pnlRemovalTargets;
    private JLabel lblPersonnelMarketDylansWeight;
    private JSpinner spnPersonnelMarketDylansWeight;
    private Map<SkillLevel, JLabel> lblPersonnelMarketRandomRemovalTargets;
    private Map<SkillLevel, JSpinner> spnPersonnelMarketRandomRemovalTargets;
    //end Personnel Market

    //start Unit Market
    //end Unit Market

    //start Contract Market
    //end Contract Market

    MarketsTab(JFrame frame, String name) {
        this.frame = frame;
        this.name = name;

        initialize();
    }

    private void initialize() {
        initializePersonnelMarket();
        initializeUnitMarket();
        initializeContractMarket();
    }

    private void initializePersonnelMarket() {
        pnlPersonnelMarketGeneralOptions = new JPanel();
        lblPersonnelMarketType = new JLabel();
        comboPersonnelMarketType = new MMComboBox<>("comboPersonnelMarketType",
            getPersonnelMarketTypeOptions());
        chkPersonnelMarketReportRefresh = new JCheckBox();
        chkUsePersonnelHireHiringHallOnly = new JCheckBox();

        pnlRemovalTargets = new JPanel();
        lblPersonnelMarketDylansWeight = new JLabel();
        spnPersonnelMarketDylansWeight = new JSpinner();
        lblPersonnelMarketRandomRemovalTargets = new HashMap<>();
        spnPersonnelMarketRandomRemovalTargets = new HashMap<>();
    }

    private static DefaultComboBoxModel<String> getPersonnelMarketTypeOptions() {
        final DefaultComboBoxModel<String> personnelMarketTypeModel = new DefaultComboBoxModel<>();
        for (final PersonnelMarketMethod method : PersonnelMarketServiceManager.getInstance()
            .getAllServices(true)) {
            personnelMarketTypeModel.addElement(method.getModuleName());
        }
        return personnelMarketTypeModel;
    }

    JPanel createPersonnelMarketTab() {
        // Header
        JPanel headerPanel = createHeaderPanel("PersonnelMarketTab",
            getImageDirectory() + "logo_clan_sea_fox.png",
            false, "", true);

        // Contents
        pnlPersonnelMarketGeneralOptions = createPersonnelMarketGeneralOptionsPanel();
        pnlRemovalTargets = createPersonnelMarketRemovalOptionsPanel();

        // Layout the Panel
        final JPanel panel = createStandardPanel("PersonnelMarketTab", true,
            "");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(headerPanel)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(pnlPersonnelMarketGeneralOptions)
                    .addComponent(pnlRemovalTargets)));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(headerPanel, Alignment.CENTER)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(pnlPersonnelMarketGeneralOptions)
                    .addComponent(pnlRemovalTargets)
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE)));

        // Create Parent Panel and return
        return createParentPanel(panel, "PersonnelMarketTab");
    }

    private JPanel createPersonnelMarketGeneralOptionsPanel() {
        // Contents
        lblPersonnelMarketType = createLabel("PersonnelMarketType", null);
        comboPersonnelMarketType = new MMComboBox<>("comboPersonnelMarketType",
            getPersonnelMarketTypeOptions());

        lblPersonnelMarketDylansWeight = createLabel("PersonnelMarketDylansWeight", null);
        spnPersonnelMarketDylansWeight = createSpinner("PersonnelMarketDylansWeight", null,
            0.3, 0, 1, 0.1);

        chkPersonnelMarketReportRefresh = createCheckBox("PersonnelMarketReportRefresh", null);

        chkUsePersonnelHireHiringHallOnly = createCheckBox("UsePersonnelHireHiringHallOnly", null);

        // Layout the Panel
        final JPanel panel = createStandardPanel("PersonnelMarketGeneralOptionsPanel", false,
            "");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPersonnelMarketType)
                    .addComponent(comboPersonnelMarketType))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPersonnelMarketDylansWeight)
                    .addComponent(spnPersonnelMarketDylansWeight))
                .addComponent(chkPersonnelMarketReportRefresh)
                .addComponent(chkUsePersonnelHireHiringHallOnly));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblPersonnelMarketType)
                    .addComponent(comboPersonnelMarketType)
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblPersonnelMarketDylansWeight)
                    .addComponent(spnPersonnelMarketDylansWeight)
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                .addComponent(chkPersonnelMarketReportRefresh)
                .addComponent(chkUsePersonnelHireHiringHallOnly));

        return panel;
    }

    private JPanel createPersonnelMarketRemovalOptionsPanel() {
        // Contents
        for (final SkillLevel skillLevel : Skills.SKILL_LEVELS) {
            final JLabel jLabel = new JLabel(skillLevel.toString());
            lblPersonnelMarketRandomRemovalTargets.put(skillLevel, jLabel);

            final JSpinner jSpinner = new JSpinner(
                new SpinnerNumberModel(0, 0, 12, 1));

            DefaultEditor editor = (DefaultEditor) jSpinner.getEditor();
            editor.getTextField().setHorizontalAlignment(JTextField.LEFT);

            spnPersonnelMarketRandomRemovalTargets.put(skillLevel, jSpinner);
        }

        // Layout the Panels
        final JPanel leftPanel = createStandardPanel("LeftPanel", false, "");
        final GroupLayout leftLayout = createStandardLayout(leftPanel);
        leftPanel.setLayout(leftLayout);

        leftLayout.setVerticalGroup(
            leftLayout.createSequentialGroup()
                .addGroup(leftLayout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.NONE))
                    .addComponent(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.NONE)))
                .addGroup(leftLayout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.ULTRA_GREEN))
                    .addComponent(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.ULTRA_GREEN)))
                .addGroup(leftLayout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.GREEN))
                    .addComponent(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.GREEN)))
                .addGroup(leftLayout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.REGULAR))
                    .addComponent(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.REGULAR))));

        leftLayout.setHorizontalGroup(
            leftLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(leftLayout.createSequentialGroup()
                    .addComponent(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.NONE))
                    .addComponent(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.NONE))
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                .addGroup(leftLayout.createSequentialGroup()
                    .addComponent(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.ULTRA_GREEN))
                    .addComponent(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.ULTRA_GREEN))
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                .addGroup(leftLayout.createSequentialGroup()
                    .addComponent(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.GREEN))
                    .addComponent(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.GREEN))
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                .addGroup(leftLayout.createSequentialGroup()
                    .addComponent(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.REGULAR))
                    .addComponent(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.REGULAR))
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE)));

        final JPanel rightPanel = createStandardPanel("RightPanel", false, "");
        final GroupLayout rightLayout = createStandardLayout(rightPanel);
        rightPanel.setLayout(rightLayout);

        rightLayout.setVerticalGroup(
            rightLayout.createSequentialGroup()
                .addGroup(rightLayout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.VETERAN))
                    .addComponent(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.VETERAN)))
                .addGroup(rightLayout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.ELITE))
                    .addComponent(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.ELITE)))
                .addGroup(rightLayout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.HEROIC))
                    .addComponent(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.HEROIC)))
                .addGroup(rightLayout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.LEGENDARY))
                    .addComponent(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.LEGENDARY))));

        rightLayout.setHorizontalGroup(
            rightLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(rightLayout.createSequentialGroup()
                    .addComponent(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.VETERAN))
                    .addComponent(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.VETERAN))
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                .addGroup(rightLayout.createSequentialGroup()
                    .addComponent(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.ELITE))
                    .addComponent(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.ELITE))
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                .addGroup(rightLayout.createSequentialGroup()
                    .addComponent(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.HEROIC))
                    .addComponent(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.HEROIC))
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                .addGroup(rightLayout.createSequentialGroup()
                    .addComponent(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.LEGENDARY))
                    .addComponent(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.LEGENDARY))
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE)));

        final JPanel parentPanel = createStandardPanel("PersonnelMarketRemovalOptionsPanel",
            true, "PersonnelMarketRemovalOptionsPanel");
        final GroupLayout parentLayout = createStandardLayout(parentPanel);
        parentPanel.setLayout(parentLayout);

        parentLayout.setVerticalGroup(
            parentLayout.createSequentialGroup()
                .addGroup(parentLayout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(leftPanel)
                    .addComponent(rightPanel)));

        parentLayout.setHorizontalGroup(
            parentLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(parentLayout.createSequentialGroup()
                    .addComponent(leftPanel)
                    .addComponent(rightPanel)
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE)));

        return parentPanel;
    }

    private void initializeUnitMarket() {

    }

    JPanel createUnitMarketTab() {
        // Header
        JPanel headerPanel = createHeaderPanel("PriceMultipliersTab",
            getImageDirectory() + "logo_clan_sea_fox.png",
            false, "", true);

        return null;
    }

    private void initializeContractMarket() {

    }

    JPanel createContractMarketTab() {
        // Header
        JPanel headerPanel = createHeaderPanel("PriceMultipliersTab",
            getImageDirectory() + "logo_loathian_league.png",
            false, "", true);

        return null;
    }
}
