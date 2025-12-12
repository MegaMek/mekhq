/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.campaign.enums.DailyReportType.ACQUISITIONS;
import static mekhq.campaign.enums.DailyReportType.BATTLE;
import static mekhq.campaign.enums.DailyReportType.FINANCES;
import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.campaign.enums.DailyReportType.MEDICAL;
import static mekhq.campaign.enums.DailyReportType.PERSONNEL;
import static mekhq.campaign.enums.DailyReportType.POLITICS;
import static mekhq.campaign.enums.DailyReportType.SKILL_CHECKS;
import static mekhq.campaign.enums.DailyReportType.TECHNICAL;
import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import megamek.client.ui.util.UIUtil;
import megamek.common.event.Subscribe;
import megamek.common.ui.EnhancedTabbedPane;
import megamek.utilities.ImageUtilities;
import mekhq.MHQOptionsChangedEvent;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignSummary;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.enums.DailyReportType;
import mekhq.campaign.events.AcquisitionEvent;
import mekhq.campaign.events.NewDayEvent;
import mekhq.campaign.events.OptionsChangedEvent;
import mekhq.campaign.events.ProcurementEvent;
import mekhq.campaign.events.ReportEvent;
import mekhq.campaign.events.TransitCompleteEvent;
import mekhq.campaign.events.assets.AssetEvent;
import mekhq.campaign.events.loans.LoanEvent;
import mekhq.campaign.events.missions.MissionEvent;
import mekhq.campaign.events.persons.PersonEvent;
import mekhq.campaign.events.scenarios.ScenarioEvent;
import mekhq.campaign.events.transactions.TransactionEvent;
import mekhq.campaign.events.units.UnitEvent;
import mekhq.campaign.events.units.UnitRefitEvent;
import mekhq.campaign.finances.FinancialReport;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.mission.TransportCostCalculations;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.report.CargoReport;
import mekhq.campaign.report.HangarReport;
import mekhq.campaign.report.PersonnelReport;
import mekhq.campaign.report.TransportReport;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.gui.adapter.ProcurementTableMouseAdapter;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.dialog.AcquisitionsDialog;
import mekhq.gui.dialog.DiplomacyReport;
import mekhq.gui.dialog.JumpCostsSummary;
import mekhq.gui.dialog.PartsReportDialog;
import mekhq.gui.dialog.factionStanding.FactionStandingReport;
import mekhq.gui.dialog.reportDialogs.CargoReportDialog;
import mekhq.gui.dialog.reportDialogs.HangarReportDialog;
import mekhq.gui.dialog.reportDialogs.PersonnelReportDialog;
import mekhq.gui.dialog.reportDialogs.ReputationReportDialog;
import mekhq.gui.dialog.reportDialogs.TransportReportDialog;
import mekhq.gui.enums.MHQTabType;
import mekhq.gui.model.ProcurementTableModel;
import mekhq.gui.panels.TutorialHyperlinkPanel;
import mekhq.gui.sorter.FormattedNumberSorter;
import mekhq.gui.sorter.TargetSorter;
import mekhq.gui.utilities.JScrollPaneWithSpeed;
import mekhq.utilities.ReportingUtilities;

/**
 * Collates important information about the campaign and displays it, along with some actionable buttons
 */
public final class CommandCenterTab extends CampaignGuiTab {

    // basic info panel
    private JPanel panInfo;
    private JLabel lblRatingHead;
    private JLabel lblRating;
    private JLabel lblExperience;
    private JLabel lblPersonnel;
    private JLabel lblHRCapacity;
    private JLabel lblMissionSuccess;
    private JLabel lblComposition;
    private JLabel lblRepairStatus;
    private JLabel lblTransportCapacity;
    private JLabel lblCargoSummary;
    private JLabel lblFacilityCapacities;

    // objectives panel
    private JPanel panObjectives;
    JList<String> listObjectives;

    // daily report
    private EnhancedTabbedPane tabLogs;
    private DailyReportLogPanel pnlGeneralLog;
    private DailyReportLogPanel pnlSkillLog;
    private DailyReportLogPanel pnlBattleLog;
    private DailyReportLogPanel pnlPoliticsLog;
    private DailyReportLogPanel pnlPersonnelLog;
    private DailyReportLogPanel pnlMedicalLog;
    private DailyReportLogPanel pnlFinancesLog;
    private DailyReportLogPanel pnlAcquisitionsLog;
    private DailyReportLogPanel pnlTechnicalLog;

    private boolean logNagActiveGeneral = false;
    private boolean logNagActiveBattle = false;
    private boolean logNagActivePolitics = false;
    private boolean logNagActivePersonnel = false;
    private boolean logNagActiveMedical = false;
    private boolean logNagActiveFinances = false;
    private boolean logNagActiveAcquisitions = false;
    private boolean logNagActiveTechnical = false;
    private boolean logNagActiveSkillChecks = false;

    // procurement table
    private JPanel panProcurement;
    private JTable procurementTable;
    private JLabel procurementTotalCostLabel;
    private ProcurementTableModel procurementModel;
    private RoundedJButton btnPauseProcurement;
    private RoundedJButton btnResumeProcurement;
    private RoundedJButton btnMRMSDialog;
    private RoundedJButton btnMRMSInstant;

    // available reports
    private JPanel panReports;
    private RoundedJButton btnUnitRating;

    private JLabel lblIcon;

    private static final String RESOURCE_BUNDLE = "mekhq.resources.CampaignGUI";
    @Deprecated(since = "0.50.10", forRemoval = true)
    private static final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CampaignGUI",
          MekHQ.getMHQOptions().getLocale());

    /**
     * @param gui  a {@link CampaignGUI} object that this tab is a component of
     * @param name a <code>String</code> giving the name of this tab
     */
    public CommandCenterTab(CampaignGUI gui, String name) {
        super(gui, name);
        MekHQ.registerHandler(this);
    }

    //region Getters/Setters
    public DailyReportLogPanel getGeneralLog() {
        return pnlGeneralLog;
    }

    public DailyReportLogPanel getSkillLog() {
        return pnlSkillLog;
    }

    public DailyReportLogPanel getBattleLog() {
        return pnlBattleLog;
    }

    public DailyReportLogPanel getPoliticsLog() {
        return pnlPoliticsLog;
    }

    public DailyReportLogPanel getPersonnelLog() {
        return pnlPersonnelLog;
    }

    public DailyReportLogPanel getMedicalLog() {
        return pnlMedicalLog;
    }

    public DailyReportLogPanel getFinancesLog() {
        return pnlFinancesLog;
    }

    public DailyReportLogPanel getAcquisitionsLog() {
        return pnlAcquisitionsLog;
    }

    public DailyReportLogPanel getTechnicalLog() {
        return pnlTechnicalLog;
    }
    //endregion Getters/Setters

    /**
     * initialize the contents and layout of the tab
     */
    @Override
    public void initTab() {
        JPanel panCommand = new JPanel(new GridBagLayout());

        initInfoPanel();
        initLogPanel();
        initReportsPanel();
        initProcurementPanel();
        initObjectivesPanel();
        //icon panel
        JPanel panIcon = new JPanel(new BorderLayout());
        lblIcon = new JLabel();
        lblIcon.getAccessibleContext().setAccessibleName("Player Camouflage");
        panIcon.add(lblIcon, BorderLayout.CENTER);
        ImageIcon icon = getAndScaleCampaignIcon();
        lblIcon.setIcon(icon);

        /* Set overall layout */
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 1.0;
        panCommand.add(tabLogs, gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panCommand.add(panProcurement, gridBagConstraints);
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        panCommand.add(panReports, gridBagConstraints);
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        panCommand.add(panObjectives, gridBagConstraints);
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        panCommand.add(panInfo, gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        panCommand.add(panIcon, gridBagConstraints);

        JPanel pnlTutorial = new TutorialHyperlinkPanel("commandCenterTab");

        setLayout(new BorderLayout());
        add(panCommand, BorderLayout.CENTER);
        add(pnlTutorial, BorderLayout.SOUTH);
    }

    /**
     * Retrieves the campaign's main faction icon and scales it to a uniform display size.
     *
     * <p>This method obtains the image associated with the current campaign's faction and scales it proportionally
     * to a width or height of 150 pixels (whichever is larger), preserving aspect ratio, for consistent display in the
     * UI.</p>
     *
     * @return a {@link ImageIcon} representing the scaled campaign faction icon
     *
     * @author Illiani
     * @since 0.50.07
     */
    private ImageIcon getAndScaleCampaignIcon() {
        ImageIcon icon = getCampaign().getCampaignFactionIcon();
        icon = ImageUtilities.scaleImageIcon(icon, 150, true);
        return icon;
    }

    private void initInfoPanel() {
        panInfo = new JPanel(new GridBagLayout());
        GridBagConstraints gridBagConstraints;
        int y = 0;

        /* Unit Rating */
        lblRatingHead = new JLabel(resourceMap.getString("lblRating.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 1, 5);
        panInfo.add(lblRatingHead, gridBagConstraints);
        lblRating = new JLabel(getCampaign().getUnitRatingText());
        lblRatingHead.setLabelFor(lblRating);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panInfo.add(lblRating, gridBagConstraints);

        JLabel lblExperienceHead = new JLabel(resourceMap.getString("lblExperience.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(1, 5, 1, 5);
        panInfo.add(lblExperienceHead, gridBagConstraints);

        lblExperience = new JLabel();
        // This seems to be overwritten completely and immediately by refresh
        String experienceString = "<html><b>" +
                                        SkillType.getColoredExperienceLevelName(getCampaign().getReputation()
                                                                                      .getAverageSkillLevel()) +
                                        "</b></html>";
        lblExperience.setText(experienceString);

        lblExperienceHead.setLabelFor(lblExperience);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panInfo.add(lblExperience, gridBagConstraints);

        JLabel lblMissionSuccessHead = new JLabel(resourceMap.getString("lblMissionSuccess.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(1, 5, 1, 5);
        panInfo.add(lblMissionSuccessHead, gridBagConstraints);
        lblMissionSuccess = new JLabel(getCampaign().getCampaignSummary().getMissionSuccessReport());
        lblMissionSuccessHead.setLabelFor(lblMissionSuccess);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panInfo.add(lblMissionSuccess, gridBagConstraints);

        JLabel lblPersonnelHead = new JLabel(resourceMap.getString("lblPersonnel.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(1, 5, 1, 5);
        panInfo.add(lblPersonnelHead, gridBagConstraints);
        lblPersonnel = new JLabel(getCampaign().getCampaignSummary().getPersonnelReport());
        lblPersonnelHead.setLabelFor(lblPersonnel);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panInfo.add(lblPersonnel, gridBagConstraints);

        if ((getCampaign().getCampaignOptions().isUseRandomRetirement()) &&
                  (getCampaign().getCampaignOptions().isUseHRStrain())) {
            JLabel lblHRCapacityHead = new JLabel(resourceMap.getString("lblHRCapacity.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y++;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints.insets = new Insets(1, 5, 1, 5);
            panInfo.add(lblHRCapacityHead, gridBagConstraints);
            lblHRCapacity = new JLabel(getCampaign().getCampaignSummary()
                                             .getHRCapacityReport(getCampaign()));
            lblHRCapacityHead.setLabelFor(lblHRCapacity);
            gridBagConstraints.gridx = 1;
            gridBagConstraints.weightx = 1.0;
            panInfo.add(lblHRCapacity, gridBagConstraints);
        }

        JLabel lblCompositionHead = new JLabel(resourceMap.getString("lblComposition.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(1, 5, 1, 5);
        panInfo.add(lblCompositionHead, gridBagConstraints);
        lblComposition = new JLabel(getCampaign().getCampaignSummary().getForceCompositionReport());
        lblCompositionHead.setLabelFor(lblComposition);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panInfo.add(lblComposition, gridBagConstraints);

        JLabel lblRepairStatusHead = new JLabel(resourceMap.getString("lblRepairStatus.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(1, 5, 1, 5);
        panInfo.add(lblRepairStatusHead, gridBagConstraints);
        lblRepairStatus = new JLabel(getCampaign().getCampaignSummary().getForceRepairReport());
        lblRepairStatusHead.setLabelFor(lblRepairStatus);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panInfo.add(lblRepairStatus, gridBagConstraints);

        JLabel lblTransportCapacityHead = new JLabel(resourceMap.getString("lblTransportCapacity.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(1, 5, 1, 5);
        panInfo.add(lblTransportCapacityHead, gridBagConstraints);
        lblTransportCapacity = new JLabel(getCampaign().getCampaignSummary().getTransportCapacity());
        lblTransportCapacityHead.setLabelFor(lblTransportCapacity);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panInfo.add(lblTransportCapacity, gridBagConstraints);

        JLabel lblCargoSummaryHead = new JLabel(resourceMap.getString("lblCargoSummary.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(1, 5, 1, 5);
        panInfo.add(lblCargoSummaryHead, gridBagConstraints);
        lblCargoSummary = new JLabel(getCampaign().getCampaignSummary().getCargoCapacityReport().toString());
        lblCargoSummaryHead.setLabelFor(lblCargoSummary);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panInfo.add(lblCargoSummary, gridBagConstraints);

        if ((getCampaignOptions().isUseFatigue()) ||
                  (getCampaignOptions().isUseAdvancedMedical() ||
                         (!getCampaignOptions().getPrisonerCaptureStyle().isNone()))) {
            JLabel lblFacilityCapacitiesHead = new JLabel(resourceMap.getString("lblFacilityCapacities.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints.insets = new Insets(1, 5, 1, 5);
            panInfo.add(lblFacilityCapacitiesHead, gridBagConstraints);
            lblFacilityCapacities = new JLabel(getCampaign().getCampaignSummary().getFacilityReport());
            lblFacilityCapacitiesHead.setLabelFor(lblFacilityCapacities);
            gridBagConstraints.gridx = 1;
            gridBagConstraints.weightx = 1.0;
            panInfo.add(lblFacilityCapacities, gridBagConstraints);
        }

        panInfo.setBorder(RoundedLineBorder.createRoundedLineBorder(getCampaign().getName()));
    }

    /**
     * Initialize the panel for showing any objectives that might exist. Objectives might come from different play
     * modes.
     */
    private void initObjectivesPanel() {
        panObjectives = new JPanel(new BorderLayout());
        panObjectives.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString("panObjectives.title")));

        listObjectives = new JList<>();

        listObjectives.setModel(new DefaultListModel<>());
        refreshObjectives();

        panObjectives.add(new JScrollPaneWithSpeed(listObjectives), BorderLayout.CENTER);
    }

    /**
     * Initialize the panel for displaying the daily report log
     */
    private void initLogPanel() {
        Dimension size = scaleForGUI(400, 100);

        pnlGeneralLog = new DailyReportLogPanel(getCampaignGui());
        pnlGeneralLog.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString("panLog.title")));
        pnlGeneralLog.setMinimumSize(size);
        pnlGeneralLog.setPreferredSize(size);

        pnlSkillLog = new DailyReportLogPanel(getCampaignGui());
        pnlSkillLog.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString("panLog.title")));
        pnlSkillLog.setMinimumSize(size);
        pnlSkillLog.setPreferredSize(size);

        pnlBattleLog = new DailyReportLogPanel(getCampaignGui());
        pnlBattleLog.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString("panLog.title")));
        pnlBattleLog.setMinimumSize(size);
        pnlBattleLog.setPreferredSize(size);

        pnlPoliticsLog = new DailyReportLogPanel(getCampaignGui());
        pnlPoliticsLog.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString("panLog.title")));
        pnlPoliticsLog.setMinimumSize(size);
        pnlPoliticsLog.setPreferredSize(size);

        pnlPersonnelLog = new DailyReportLogPanel(getCampaignGui());
        pnlPersonnelLog.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString("panLog.title")));
        pnlPersonnelLog.setMinimumSize(size);
        pnlPersonnelLog.setPreferredSize(size);

        pnlMedicalLog = new DailyReportLogPanel(getCampaignGui());
        pnlMedicalLog.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString("panLog.title")));
        pnlMedicalLog.setMinimumSize(size);
        pnlMedicalLog.setPreferredSize(size);

        pnlFinancesLog = new DailyReportLogPanel(getCampaignGui());
        pnlFinancesLog.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString("panLog.title")));
        pnlFinancesLog.setMinimumSize(size);
        pnlFinancesLog.setPreferredSize(size);

        pnlAcquisitionsLog = new DailyReportLogPanel(getCampaignGui());
        pnlAcquisitionsLog.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString("panLog.title")));
        pnlAcquisitionsLog.setMinimumSize(size);
        pnlAcquisitionsLog.setPreferredSize(size);

        pnlTechnicalLog = new DailyReportLogPanel(getCampaignGui());
        pnlTechnicalLog.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString("panLog.title")));
        pnlTechnicalLog.setMinimumSize(size);
        pnlTechnicalLog.setPreferredSize(size);

        tabLogs = new EnhancedTabbedPane();
        tabLogs.setName("dailyReportTabs");
        tabLogs.addTab(GENERAL.getIconString(), pnlGeneralLog);
        tabLogs.setToolTipTextAt(GENERAL.getTabIndex(), GENERAL.getTooltip());
        tabLogs.addTab(BATTLE.getIconString(), pnlBattleLog);
        tabLogs.setToolTipTextAt(BATTLE.getTabIndex(), BATTLE.getTooltip());
        tabLogs.addTab(PERSONNEL.getIconString(), pnlPersonnelLog);
        tabLogs.setToolTipTextAt(PERSONNEL.getTabIndex(), PERSONNEL.getTooltip());
        tabLogs.addTab(MEDICAL.getIconString(), pnlMedicalLog);
        tabLogs.setToolTipTextAt(MEDICAL.getTabIndex(), MEDICAL.getTooltip());
        tabLogs.addTab(FINANCES.getIconString(), pnlFinancesLog);
        tabLogs.setToolTipTextAt(FINANCES.getTabIndex(), FINANCES.getTooltip());
        tabLogs.addTab(ACQUISITIONS.getIconString(), pnlAcquisitionsLog);
        tabLogs.setToolTipTextAt(ACQUISITIONS.getTabIndex(), ACQUISITIONS.getTooltip());
        tabLogs.addTab(TECHNICAL.getIconString(), pnlTechnicalLog);
        tabLogs.setToolTipTextAt(TECHNICAL.getTabIndex(), TECHNICAL.getTooltip());
        tabLogs.addTab(POLITICS.getIconString(), pnlPoliticsLog);
        tabLogs.setToolTipTextAt(POLITICS.getTabIndex(), POLITICS.getTooltip());
        tabLogs.addTab(SKILL_CHECKS.getIconString(), pnlSkillLog);
        tabLogs.setToolTipTextAt(SKILL_CHECKS.getTabIndex(), SKILL_CHECKS.getTooltip());

        tabLogs.addChangeListener(evt -> {
            int selectedIndex = tabLogs.getSelectedIndex();
            clearDailyReportNag(selectedIndex);
        });
    }

    void clearDailyReportNag(int selectedIndex) {
        DailyReportType type = DailyReportType.getTypeFromIndex(selectedIndex);
        if (type != null) {
            tabLogs.setBackgroundAt(selectedIndex, null);
            setLogNagActive(type, false);
        }
    }

    public EnhancedTabbedPane getTabLogs() {
        return tabLogs;
    }

    public boolean isLogNagActive(DailyReportType logType) {
        return switch (logType) {
            case GENERAL -> logNagActiveGeneral;
            case BATTLE -> logNagActiveBattle;
            case POLITICS -> logNagActivePolitics;
            case PERSONNEL -> logNagActivePersonnel;
            case MEDICAL -> logNagActiveMedical;
            case FINANCES -> logNagActiveFinances;
            case ACQUISITIONS -> logNagActiveAcquisitions;
            case TECHNICAL -> logNagActiveTechnical;
            case SKILL_CHECKS -> logNagActiveSkillChecks;
        };
    }

    public void setLogNagActive(DailyReportType logType, boolean isActive) {
        switch (logType) {
            case GENERAL -> logNagActiveGeneral = isActive;
            case BATTLE -> logNagActiveBattle = isActive;
            case POLITICS -> logNagActivePolitics = isActive;
            case PERSONNEL -> logNagActivePersonnel = isActive;
            case MEDICAL -> logNagActiveMedical = isActive;
            case FINANCES -> logNagActiveFinances = isActive;
            case ACQUISITIONS -> logNagActiveAcquisitions = isActive;
            case TECHNICAL -> logNagActiveTechnical = isActive;
            case SKILL_CHECKS -> logNagActiveSkillChecks = isActive;
        }
    }

    public void nagLogTab(int logIndex) {
        if (logIndex >= 0 && logIndex < tabLogs.getTabCount()) {
            tabLogs.setBackgroundAt(logIndex, UIUtil.uiDarkBlue());
        }
    }

    /**
     * Initialize the panel for displaying procurement information
     */
    private void initProcurementPanel() {
        /* shopping buttons */
        JPanel panProcurementButtons = new JPanel(new GridLayout(8, 1, 0, 5));
        panProcurementButtons.getAccessibleContext().setAccessibleName("Procurement Actions");

        RoundedJButton btnNeededParts = new RoundedJButton(resourceMap.getString("btnNeededParts.text"));
        btnNeededParts.setToolTipText(resourceMap.getString("btnNeededParts.toolTipText"));
        btnNeededParts.addActionListener(evt -> new AcquisitionsDialog(getFrame(), true, getCampaignGui()).setVisible(
              true));
        panProcurementButtons.add(btnNeededParts);

        RoundedJButton btnPartsReport = new RoundedJButton(resourceMap.getString("btnPartsReport.text"));
        btnPartsReport.setToolTipText(resourceMap.getString("btnPartsReport.toolTipText"));
        btnPartsReport.addActionListener(evt -> new PartsReportDialog(getCampaignGui(), true).setVisible(true));
        panProcurementButtons.add(btnPartsReport);

        btnPauseProcurement = new RoundedJButton(resourceMap.getString("btnPauseProcurement.text"));
        btnPauseProcurement.addActionListener(evt -> {
            btnPauseProcurement.setEnabled(false);
            btnResumeProcurement.setEnabled(true);
            getCampaign().setProcessProcurement(false);
        });
        btnPauseProcurement.setEnabled(getCampaign().isProcessProcurement());
        panProcurementButtons.add(btnPauseProcurement);

        btnResumeProcurement = new RoundedJButton(resourceMap.getString("btnResumeProcurement.text"));
        btnResumeProcurement.addActionListener(evt -> {
            btnResumeProcurement.setEnabled(false);
            btnPauseProcurement.setEnabled(true);
            getCampaign().setProcessProcurement(true);
        });
        btnResumeProcurement.setEnabled(!getCampaign().isProcessProcurement());
        panProcurementButtons.add(btnResumeProcurement);
        /* shopping table */
        procurementTotalCostLabel = new JLabel();
        refreshProcurementTotalCost();
        procurementModel = new ProcurementTableModel(getCampaign());
        procurementTable = new JTable(procurementModel);
        procurementTable.getAccessibleContext().setAccessibleName("Pending Procurements");
        TableRowSorter<ProcurementTableModel> shoppingSorter = new TableRowSorter<>(procurementModel);
        shoppingSorter.setComparator(ProcurementTableModel.COL_COST, new FormattedNumberSorter());
        shoppingSorter.setComparator(ProcurementTableModel.COL_TOTAL_COST, new FormattedNumberSorter());
        shoppingSorter.setComparator(ProcurementTableModel.COL_TARGET, new TargetSorter());
        procurementTable.setRowSorter(shoppingSorter);
        TableColumn column;
        for (int i = 0; i < ProcurementTableModel.N_COL; i++) {
            column = procurementTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(procurementModel.getColumnWidth(i));
            column.setCellRenderer(procurementModel.getRenderer());
        }
        procurementTable.setIntercellSpacing(new Dimension(0, 0));
        procurementTable.setShowGrid(false);
        ProcurementTableMouseAdapter.connect(getCampaignGui(), procurementTable, procurementModel);
        procurementTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        procurementTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "ADD");
        procurementTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, 0), "ADD");
        procurementTable.getInputMap(JComponent.WHEN_FOCUSED)
              .put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "REMOVE");
        procurementTable.getInputMap(JComponent.WHEN_FOCUSED)
              .put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0), "REMOVE");

        procurementTable.getActionMap().put("ADD", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (final int row : procurementTable.getSelectedRows()) {
                    if (row >= 0) {
                        procurementModel.incrementItem(procurementTable.convertRowIndexToModel(row));
                    }
                }
                refreshProcurementTotalCost();
            }
        });

        procurementTable.getActionMap().put("REMOVE", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (final int rowIndex : procurementTable.getSelectedRows()) {
                    if (rowIndex < 0) {
                        continue;
                    }
                    final int row = procurementTable.convertRowIndexToModel(rowIndex);
                    if (procurementModel.getAcquisition(row).map(IAcquisitionWork::getQuantity).orElse(0) > 0) {
                        procurementModel.decrementItem(row);
                    }
                }
                refreshProcurementTotalCost();
            }
        });

        JScrollPane scrollProcurement = new JScrollPaneWithSpeed(procurementTable);
        panProcurement = new JPanel(new GridBagLayout());
        panProcurement.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString("panProcurement.title")));
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        panProcurement.add(panProcurementButtons, gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panProcurement.add(scrollProcurement, gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        panProcurement.add(procurementTotalCostLabel, gridBagConstraints);
    }

    /**
     * Initialize the panel for displaying available reports
     */
    private void initReportsPanel() {
        panReports = new JPanel(new GridLayout(8, 1, 0, 5));

        RoundedJButton btnTransportReport = new RoundedJButton(resourceMap.getString("btnTransportReport.text"));
        btnTransportReport.addActionListener(ev -> new TransportReportDialog(getCampaignGui().getFrame(),
              new TransportReport(getCampaign())).setVisible(true));
        panReports.add(btnTransportReport);

        RoundedJButton btnHangarOverview = new RoundedJButton(resourceMap.getString("btnHangarOverview.text"));
        btnHangarOverview.addActionListener(evt -> new HangarReportDialog(getCampaignGui().getFrame(),
              new HangarReport(getCampaign())).setVisible(true));
        panReports.add(btnHangarOverview);

        RoundedJButton btnPersonnelOverview = new RoundedJButton(resourceMap.getString("btnPersonnelOverview.text"));
        btnPersonnelOverview.addActionListener(evt -> new PersonnelReportDialog(getCampaignGui().getFrame(),
              new PersonnelReport(getCampaign())).setVisible(true));
        panReports.add(btnPersonnelOverview);

        RoundedJButton btnCargoCapacity = new RoundedJButton(resourceMap.getString("btnCargoCapacity.text"));
        btnCargoCapacity.addActionListener(evt -> new CargoReportDialog(getCampaignGui().getFrame(),
              new CargoReport(getCampaign())).setVisible(true));
        panReports.add(btnCargoCapacity);

        btnUnitRating = new RoundedJButton(resourceMap.getString("btnUnitRating.text"));
        btnUnitRating.addActionListener(evt -> new ReputationReportDialog(getCampaignGui().getFrame(),
              getCampaign()).setVisible(true));
        panReports.add(btnUnitRating);

        RoundedJButton btnFactionStanding = new RoundedJButton(resourceMap.getString("btnFactionStanding.text"));
        btnFactionStanding.addActionListener(evt -> {
            FactionStandingReport factionStandingReport = new FactionStandingReport(getCampaignGui().getFrame(),
                  getCampaign());

            for (String report : factionStandingReport.getReports()) {
                if (report != null && !report.isBlank()) {
                    getCampaign().addReport(POLITICS, report);
                }
            }
        });
        panReports.add(btnFactionStanding);

        RoundedJButton btnDiplomacy = new RoundedJButton(resourceMap.getString("btnDiplomacy.text"));
        btnDiplomacy.addActionListener(evt -> {
            new DiplomacyReport(getCampaignGui().getFrame(), getCampaign().isClanCampaign(),
                  getCampaign().getLocalDate());
        });
        panReports.add(btnDiplomacy);

        RoundedJButton btnJumpFees = new RoundedJButton(resourceMap.getString("btnJumpFees.text"));
        btnJumpFees.addActionListener(evt -> {
            TransportCostCalculations transportCostCalculations =
                  getCampaign().getTransportCostCalculation(EXP_REGULAR);
            transportCostCalculations.calculateJumpCostForEachDay();
            new JumpCostsSummary(getCampaignGui().getFrame(), transportCostCalculations);
        });
        panReports.add(btnJumpFees);

        panReports.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString("panReports.title")));
    }

    @Override
    public MHQTabType tabType() {
        return MHQTabType.COMMAND_CENTER;
    }

    /**
     * refresh all components
     */
    @Override
    public void refreshAll() {
        refreshBasicInfo();
        refreshProcurementList();
        refreshObjectives();

        refreshGeneralLog();
        refreshSkillLog();
        refreshBattleLog();
        refreshPoliticsLog();
        refreshPersonnelLog();
        refreshMedicalLog();
        refreshFinancesLog();
        refreshAcquisitionsLog();
        refreshTechnicalLog();
    }

    /**
     * refresh the basic info panel with campaign information
     */
    private void refreshBasicInfo() {
        final Campaign campaign = getCampaign();
        final CampaignOptions campaignOptions = campaign.getCampaignOptions();
        final CampaignSummary campaignSummary = campaign.getCampaignSummary();

        if (panInfo.getBorder() instanceof TitledBorder titledBorder) {
            titledBorder.setTitle(getCampaign().getName());
            panInfo.repaint();
        }

        String experienceString = "<html><b>" +
                                        SkillType.getColoredExperienceLevelName(campaign.getReputation()
                                                                                      .getAverageSkillLevel()) +
                                        "</b></html>";
        lblExperience.setText(experienceString);

        campaignSummary.updateInformation();
        lblRating.setText(campaign.getUnitRatingText());
        lblPersonnel.setText(campaignSummary.getPersonnelReport());
        lblMissionSuccess.setText(campaignSummary.getMissionSuccessReport());
        lblComposition.setText(campaignSummary.getForceCompositionReport());
        lblCargoSummary.setText(campaignSummary.getCargoCapacityReport().toString());
        lblRepairStatus.setText(campaignSummary.getForceRepairReport());
        lblTransportCapacity.setText(campaignSummary.getTransportCapacity());

        if (campaignOptions.isUseHRStrain()) {
            try {
                lblHRCapacity.setText(campaignSummary.getHRCapacityReport(campaign));
            } catch (Exception ignored) {
            }
        }

        try {
            lblFacilityCapacities.setText(campaignSummary.getFacilityReport());
        } catch (Exception ignored) {
        }
    }

    private void refreshObjectives() {
        // Define the DefaultListModel
        DefaultListModel<String> model = new DefaultListModel<>();

        // Add items to the model
        if (getCampaign().getStoryArc() != null) {
            for (String objective : getCampaign().getCurrentObjectives()) {
                model.addElement(objective);
            }
        } else {
            for (String report : getAbridgedFinancialReport()) {
                model.addElement(String.format(report));
            }

            for (Mission mission : getCampaign().getActiveMissions(false)) {
                List<Scenario> scenarios = mission.getScenarios();

                scenarios.sort(Comparator.comparing(Scenario::getDate,
                      Comparator.nullsFirst(Comparator.naturalOrder())));
                Collections.reverse(scenarios);

                if (!scenarios.isEmpty()) {
                    model.addElement(String.format("<html><b>" + mission.getName() + "</b></html>"));

                    for (Scenario scenario : scenarios) {
                        if (scenario.getStatus().isCurrent()) {
                            // StratCon facility contacts that haven't yet been discovered are stored as scenarios with null start dates
                            if (scenario.getDate() != null) {
                                model.addElement(String.format("<html><b>" +
                                                                     scenario.getName() +
                                                                     ":</b> " +
                                                                     "<font color='" +
                                                                     MekHQ.getMHQOptions()
                                                                           .getFontColorWarningHexColor() +
                                                                     "'>" +
                                                                     ChronoUnit.DAYS.between(getCampaign().getLocalDate(),
                                                                           scenario.getDate())) + " days</font</html>");
                            }
                        }
                    }
                }
            }
        }

        // Set the model to the list
        listObjectives.setModel(model);
    }


    /**
     * @return a {@code List<String>} containing the abridged financial report entries.
     */
    public List<String> getAbridgedFinancialReport() {
        List<String> reportString = new ArrayList<>();

        FinancialReport report = FinancialReport.calculate(getCampaign());

        String formatted = "%ss";

        reportString.add("<html><b>Net Worth:</b> " +
                               String.format(formatted, report.getNetWorth().toAmountAndSymbolString()) +
                               "</html>");

        reportString.add("<html><b>Monthly Profit:</b> " +
                               String.format(formatted,
                                     report.getMonthlyIncome()
                                           .minus(report.getMonthlyExpenses())
                                           .toAmountAndSymbolString()) +
                               "</html>");

        reportString.add("<html><br></html>");

        return reportString;
    }

    /**
     * refresh the procurement list
     */
    private void refreshProcurementList() {
        procurementModel.setData(getCampaign().getShoppingList().getShoppingList());
        refreshProcurementTotalCost();
    }

    /**
     * refresh the total cost of procurement
     */
    private void refreshProcurementTotalCost() {

        String formatString, totalCostString;
        Money totalCost, funds;
        formatString = resourceMap.getString("lblProcurementTotalCost.text");
        totalCost = getCampaign().getShoppingList().getTotalBuyCost();
        funds = getCampaign().getFunds();
        if (funds.compareTo(totalCost) < 0) {
            String warningColor = ReportingUtilities.getWarningColor();
            String formatedString = "<b>" + totalCost.toAmountAndSymbolString() + "</b>";
            totalCostString = ReportingUtilities.messageSurroundedBySpanWithColor(warningColor, formatedString);
        } else {
            totalCostString = totalCost.toAmountAndSymbolString();
        }
        procurementTotalCostLabel.setText(String.format(formatString, totalCostString));
    }

    /**
     * Initialize a new daily log report
     */
    private void initLog() {
        String generalReport = getCampaign().getCurrentReportHTML();
        pnlGeneralLog.refreshLog(generalReport, GENERAL);
        getCampaign().fetchAndClearNewReports();

        String skillReport = getCampaign().getSkillReportHTML();
        pnlSkillLog.refreshLog(skillReport, SKILL_CHECKS);
        getCampaign().fetchAndClearNewSkillReports();

        String battleReport = getCampaign().getBattleReportHTML();
        pnlBattleLog.refreshLog(battleReport, BATTLE);
        getCampaign().fetchAndClearNewBattleReports();

        String politicsReport = getCampaign().getPoliticsReportHTML();
        pnlPoliticsLog.refreshLog(politicsReport, POLITICS);
        getCampaign().fetchAndClearNewPoliticsReports();

        String personnelReport = getCampaign().getPersonnelReportHTML();
        pnlPersonnelLog.refreshLog(personnelReport, PERSONNEL);
        getCampaign().fetchAndClearNewPersonnelReports();

        String medicalReport = getCampaign().getMedicalReportHTML();
        pnlMedicalLog.refreshLog(medicalReport, MEDICAL);
        getCampaign().fetchAndClearNewMedicalReports();

        String financesReport = getCampaign().getFinancesReportHTML();
        pnlFinancesLog.refreshLog(financesReport, FINANCES);
        getCampaign().fetchAndClearNewFinancesReports();

        String acquisitionsReport = getCampaign().getAcquisitionsReportHTML();
        pnlAcquisitionsLog.refreshLog(acquisitionsReport, ACQUISITIONS);
        getCampaign().fetchAndClearNewAcquisitionsReports();

        String technicalReport = getCampaign().getTechnicalReportHTML();
        pnlTechnicalLog.refreshLog(technicalReport, TECHNICAL);
        getCampaign().fetchAndClearNewTechnicalReports();
    }

    /**
     * append new reports to the daily log report
     */
    synchronized private void refreshGeneralLog() {
        pnlGeneralLog.appendLog(getCampaign().fetchAndClearNewReports(), GENERAL);
    }

    synchronized private void refreshSkillLog() {
        pnlSkillLog.appendLog(getCampaign().fetchAndClearNewSkillReports(), SKILL_CHECKS);
    }

    synchronized private void refreshBattleLog() {
        pnlBattleLog.appendLog(getCampaign().fetchAndClearNewBattleReports(), BATTLE);
    }

    synchronized private void refreshPoliticsLog() {
        pnlPoliticsLog.appendLog(getCampaign().fetchAndClearNewPoliticsReports(), POLITICS);
    }

    synchronized private void refreshPersonnelLog() {
        pnlPersonnelLog.appendLog(getCampaign().fetchAndClearNewPersonnelReports(), PERSONNEL);
    }

    synchronized private void refreshMedicalLog() {
        pnlMedicalLog.appendLog(getCampaign().fetchAndClearNewMedicalReports(), MEDICAL);
    }

    synchronized private void refreshFinancesLog() {
        pnlFinancesLog.appendLog(getCampaign().fetchAndClearNewFinancesReports(), FINANCES);
    }

    synchronized private void refreshAcquisitionsLog() {
        pnlAcquisitionsLog.appendLog(getCampaign().fetchAndClearNewAcquisitionsReports(), ACQUISITIONS);
    }

    synchronized private void refreshTechnicalLog() {
        pnlTechnicalLog.appendLog(getCampaign().fetchAndClearNewTechnicalReports(), TECHNICAL);
    }

    private final ActionScheduler procurementListScheduler = new ActionScheduler(this::refreshProcurementList);
    private final ActionScheduler basicInfoScheduler = new ActionScheduler(this::refreshBasicInfo);
    private final ActionScheduler objectivesScheduler = new ActionScheduler(this::refreshObjectives);

    @Subscribe
    public void handle(UnitRefitEvent ev) {
        procurementListScheduler.schedule();
    }

    @Subscribe
    public void handle(AcquisitionEvent ev) {
        procurementListScheduler.schedule();
        basicInfoScheduler.schedule();
    }

    @Subscribe
    public void handle(ProcurementEvent ev) {
        procurementListScheduler.schedule();
    }

    @Subscribe
    public void handle(ReportEvent ev) {
        refreshGeneralLog();
        refreshSkillLog();
        refreshBattleLog();
        refreshPoliticsLog();
        refreshPersonnelLog();
        refreshMedicalLog();
        refreshFinancesLog();
        refreshAcquisitionsLog();
        refreshTechnicalLog();
    }

    @Subscribe
    public void handleNewDay(NewDayEvent evt) {
        procurementListScheduler.schedule();
        basicInfoScheduler.schedule();
        objectivesScheduler.schedule();
        initLog();
    }

    @Subscribe
    public void handle(MissionEvent evt) {
        basicInfoScheduler.schedule();
        objectivesScheduler.schedule();
    }

    @Subscribe
    public void handle(ScenarioEvent evt) {
        objectivesScheduler.schedule();
    }

    @Subscribe
    public void handle(TransitCompleteEvent evt) {
        objectivesScheduler.schedule();
    }

    @Subscribe
    public void handle(PersonEvent evt) {
        basicInfoScheduler.schedule();
    }

    @Subscribe
    public void handle(UnitEvent evt) {
        basicInfoScheduler.schedule();
    }

    @Subscribe
    public void handle(final OptionsChangedEvent evt) {
        basicInfoScheduler.schedule();
        procurementListScheduler.schedule();
        ImageIcon icon = getAndScaleCampaignIcon();
        lblIcon.setIcon(icon);
    }

    @Subscribe
    public void handle(MHQOptionsChangedEvent evt) {
        btnMRMSDialog.setVisible(MekHQ.getMHQOptions().getCommandCenterMRMS());
        btnMRMSInstant.setVisible(MekHQ.getMHQOptions().getCommandCenterMRMS());
    }

    @Subscribe
    public void handle(TransactionEvent ev) {
        basicInfoScheduler.schedule();
    }

    @Subscribe
    public void handle(LoanEvent ev) {
        basicInfoScheduler.schedule();
    }

    @Subscribe
    public void handle(AssetEvent ev) {
        basicInfoScheduler.schedule();
    }
}
