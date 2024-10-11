package mekhq.gui.panes.campaignOptions.tabs;

import megamek.client.ui.baseComponents.MMComboBox;
import mekhq.campaign.personnel.enums.BabySurnameStyle;
import mekhq.campaign.personnel.enums.RandomDivorceMethod;
import mekhq.campaign.personnel.enums.RandomMarriageMethod;
import mekhq.campaign.personnel.enums.RandomProcreationMethod;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import java.awt.*;

import static mekhq.gui.panes.campaignOptions.tabs.CampaignOptionsUtilities.*;

public class RelationshipsTab {
    JFrame frame;
    String name;

    //start Marriage Tab
    private JPanel pnlMarriageGeneralOptions;
    private JCheckBox chkUseManualMarriages;
    private JCheckBox chkUseClanPersonnelMarriages;
    private JCheckBox chkUsePrisonerMarriages;
    private JLabel lblNoInterestInMarriageDiceSize;
    private JSpinner spnNoInterestInMarriageDiceSize;
    private JLabel lblCheckMutualAncestorsDepth;
    private JSpinner spnCheckMutualAncestorsDepth;
    private JCheckBox chkLogMarriageNameChanges;

    private JPanel pnlRandomMarriage;
    private JLabel lblRandomMarriageMethod;
    private MMComboBox<RandomMarriageMethod> comboRandomMarriageMethod;
    private JCheckBox chkUseRandomClanPersonnelMarriages;
    private JCheckBox chkUseRandomPrisonerMarriages;
    private JLabel lblRandomMarriageAgeRange;
    private JSpinner spnRandomMarriageAgeRange;

    private JPanel pnlPercentageRandomMarriage;
    private JLabel lblRandomMarriageOppositeSexDiceSize;
    private JSpinner spnRandomMarriageDiceSize;
    private JLabel lblRandomSameSexMarriageDiceSize;
    private JSpinner spnRandomSameSexMarriageDiceSize;
    private JLabel lblRandomNewDependentMarriage;
    private JSpinner spnRandomNewDependentMarriage;
    //end Marriage Tab

    //start Divorce Tab
    private JCheckBox chkUseManualDivorce;
    private JCheckBox chkUseClanPersonnelDivorce;
    private JCheckBox chkUsePrisonerDivorce;

    private JPanel pnlRandomDivorce;

    private JLabel lblRandomDivorceMethod;
    private MMComboBox<RandomDivorceMethod> comboRandomDivorceMethod;
    private JCheckBox chkUseRandomOppositeSexDivorce;
    private JCheckBox chkUseRandomSameSexDivorce;
    private JCheckBox chkUseRandomClanPersonnelDivorce;
    private JCheckBox chkUseRandomPrisonerDivorce;
    private JLabel lblRandomDivorceDiceSize;
    private JSpinner spnRandomDivorceDiceSize;
    //end Divorce Tab

    //start Procreation Tab
    private JCheckBox chkUseManualProcreation;
    private JCheckBox chkUseClanPersonnelProcreation;
    private JCheckBox chkUsePrisonerProcreation;
    private JLabel lblMultiplePregnancyOccurrences;
    private JSpinner spnMultiplePregnancyOccurrences;
    private JLabel lblBabySurnameStyle;
    private MMComboBox<BabySurnameStyle> comboBabySurnameStyle;
    private JCheckBox chkAssignNonPrisonerBabiesFounderTag;
    private JCheckBox chkAssignChildrenOfFoundersFounderTag;
    private JCheckBox chkDetermineFatherAtBirth;
    private JCheckBox chkDisplayTrueDueDate;
    private JLabel lblNoInterestInChildrenDiceSize;
    private JSpinner spnNoInterestInChildrenDiceSize;
    private JCheckBox chkUseMaternityLeave;
    private JCheckBox chkLogProcreation;

    private JPanel pnlProcreationGeneralOptionsPanel;
    private JPanel pnlRandomProcreationPanel;
    private JLabel lblRandomProcreationMethod;
    private MMComboBox<RandomProcreationMethod> comboRandomProcreationMethod;
    private JCheckBox chkUseRelationshiplessRandomProcreation;
    private JCheckBox chkUseRandomClanPersonnelProcreation;
    private JCheckBox chkUseRandomPrisonerProcreation;
    private JLabel lblRandomProcreationRelationshipDiceSize;
    private JSpinner spnRandomProcreationRelationshipDiceSize;
    private JLabel lblRandomProcreationRelationshiplessDiceSize;
    private JSpinner spnRandomProcreationRelationshiplessDiceSize;
    //end Procreation Tab

    RelationshipsTab(JFrame frame, String name) {
        this.frame = frame;
        this.name = name;

        initialize();
    }

    protected void initialize() {
        // Marriage Tab
        pnlMarriageGeneralOptions = new JPanel();
        chkUseManualMarriages = new JCheckBox();
        chkUseClanPersonnelMarriages = new JCheckBox();
        chkUsePrisonerMarriages = new JCheckBox();
        lblNoInterestInMarriageDiceSize = new JLabel();
        spnNoInterestInMarriageDiceSize = new JSpinner();
        lblCheckMutualAncestorsDepth = new JLabel();
        spnCheckMutualAncestorsDepth = new JSpinner();
        chkLogMarriageNameChanges = new JCheckBox();

        pnlRandomMarriage = new JPanel();
        comboRandomMarriageMethod = new MMComboBox<>("comboRandomMarriageMethod",
            RandomMarriageMethod.values());

        pnlRandomMarriage = new JPanel();
        lblRandomMarriageMethod = new JLabel();
        comboRandomMarriageMethod = new MMComboBox<>("comboRandomMarriageMethod",
            RandomMarriageMethod.values());
        chkUseRandomClanPersonnelMarriages = new JCheckBox();
        chkUseRandomPrisonerMarriages = new JCheckBox();
        lblRandomMarriageAgeRange = new JLabel();
        spnRandomMarriageAgeRange = new JSpinner();

        pnlPercentageRandomMarriage = new JPanel();
        lblRandomMarriageOppositeSexDiceSize = new JLabel();
        spnRandomMarriageDiceSize = new JSpinner();
        lblRandomSameSexMarriageDiceSize = new JLabel();
        spnRandomSameSexMarriageDiceSize = new JSpinner();
        lblRandomNewDependentMarriage = new JLabel();
        spnRandomNewDependentMarriage = new JSpinner();

        // Divorce Tab
        chkUseManualDivorce = new JCheckBox();
        chkUseClanPersonnelDivorce = new JCheckBox();
        chkUsePrisonerDivorce = new JCheckBox();

        pnlRandomDivorce = new JPanel();
        lblRandomDivorceMethod = new JLabel();
        comboRandomDivorceMethod = new MMComboBox<>("comboRandomDivorceMethod", RandomDivorceMethod.values());
        chkUseRandomOppositeSexDivorce = new JCheckBox();
        chkUseRandomSameSexDivorce = new JCheckBox();
        chkUseRandomClanPersonnelDivorce = new JCheckBox();
        chkUseRandomPrisonerDivorce = new JCheckBox();
        lblRandomDivorceDiceSize = new JLabel();
        spnRandomDivorceDiceSize = new JSpinner();

        // Procreation Tab
        pnlProcreationGeneralOptionsPanel = new JPanel();
        chkUseManualProcreation = new JCheckBox();
        chkUseClanPersonnelProcreation = new JCheckBox();
        chkUsePrisonerProcreation = new JCheckBox();
        lblMultiplePregnancyOccurrences = new JLabel();
        spnMultiplePregnancyOccurrences = new JSpinner();
        lblBabySurnameStyle = new JLabel();
        comboBabySurnameStyle = new MMComboBox<>("comboBabySurnameStyle", BabySurnameStyle.values());
        chkAssignNonPrisonerBabiesFounderTag = new JCheckBox();
        chkAssignChildrenOfFoundersFounderTag = new JCheckBox();
        chkDetermineFatherAtBirth = new JCheckBox();
        chkDisplayTrueDueDate = new JCheckBox();
        lblNoInterestInChildrenDiceSize = new JLabel();
        spnNoInterestInChildrenDiceSize = new JSpinner();
        chkUseMaternityLeave = new JCheckBox();
        chkLogProcreation = new JCheckBox();

        pnlRandomProcreationPanel = new JPanel();
        lblRandomProcreationMethod = new JLabel();
        comboRandomProcreationMethod = new MMComboBox<>("comboRandomProcreationMethod",
            RandomProcreationMethod.values());
        chkUseRelationshiplessRandomProcreation = new JCheckBox();
        chkUseRandomClanPersonnelProcreation = new JCheckBox();
        chkUseRandomPrisonerProcreation = new JCheckBox();
        lblRandomProcreationRelationshipDiceSize = new JLabel();
        spnRandomProcreationRelationshipDiceSize = new JSpinner();
        lblRandomProcreationRelationshiplessDiceSize = new JLabel();
        spnRandomProcreationRelationshiplessDiceSize = new JSpinner();
    }/**
     * Creates a panel for the Marriage tab with various input components and panels related to marriage settings.
     *
     * @return a {@link} representing the Marriage tab with checkboxes for manual, clan personnel,
     * prisoner marriages, options for marriage characteristics, logging marriage name changes, surname
     * weight settings, and random marriage generation.
     */
    JPanel createMarriageTab() {
        // Header
        JPanel headerPanel = createHeaderPanel("MarriageTab",
            getImageDirectory() + "logo_federated_commonwealth.png",
            false, "", true);

        // Contents
        pnlMarriageGeneralOptions = createMarriageGeneralOptionsPanel();
        pnlRandomMarriage = createRandomMarriagePanel();

        // Layout the Panel
        final JPanel panel = createStandardPanel("MarriageTab", true, "");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(headerPanel)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(pnlMarriageGeneralOptions)
                    .addComponent(pnlRandomMarriage)));

        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addComponent(headerPanel, Alignment.CENTER)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(pnlMarriageGeneralOptions)
                        .addComponent(pnlRandomMarriage)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))));

        // Create Parent Panel and return
        return createParentPanel(panel, "MarriageTab");
    }

    /**
     * Creates a panel for general marriage options with checkboxes and input components.
     *
     * @return a {@link JPanel} representing the general marriage options panel
     */
    private JPanel createMarriageGeneralOptionsPanel() {
        // Contents
        chkUseManualMarriages = createCheckBox("UseManualMarriages", null);
        chkUseClanPersonnelMarriages = createCheckBox("UseClanPersonnelMarriages", null);
        chkUsePrisonerMarriages = createCheckBox("UsePrisonerMarriages", null);

        lblNoInterestInMarriageDiceSize = createLabel("NoInterestInMarriageDiceSize", null);
        spnNoInterestInMarriageDiceSize = createSpinner("NoInterestInMarriageDiceSize", null,
            10, 1, 100000, 1);

        lblCheckMutualAncestorsDepth = createLabel("CheckMutualAncestorsDepth", null);
        spnCheckMutualAncestorsDepth = createSpinner("CheckMutualAncestorsDepth", null,
            4, 0, 20, 1);

        chkLogMarriageNameChanges = createCheckBox("LogMarriageNameChanges", null);

        // Layout the Panel
        final JPanel panel = createStandardPanel("MarriageGeneralOptionsPanel",
            false, "");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(chkUseManualMarriages)
                .addComponent(chkUseClanPersonnelMarriages)
                .addComponent(chkUsePrisonerMarriages)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblNoInterestInMarriageDiceSize)
                    .addComponent(spnNoInterestInMarriageDiceSize))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblCheckMutualAncestorsDepth)
                    .addComponent(spnCheckMutualAncestorsDepth))
                .addComponent(chkLogMarriageNameChanges));

        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addComponent(chkUseManualMarriages)
                    .addComponent(chkUseClanPersonnelMarriages)
                    .addComponent(chkUsePrisonerMarriages)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblNoInterestInMarriageDiceSize)
                        .addComponent(spnNoInterestInMarriageDiceSize)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblCheckMutualAncestorsDepth)
                        .addComponent(spnCheckMutualAncestorsDepth)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                    .addComponent(chkLogMarriageNameChanges)));

        return panel;
    }

    /**
     * Creates a panel for random marriage settings, including options for different marriage methods,
     * using random clan personnel and prisoner marriages, setting age range for marriages, and
     * percentage settings.
     *
     * @return a {@link JPanel} representing the random marriage panel with various input components
     * and panels for configuring random marriage settings
     */
    private JPanel createRandomMarriagePanel() {
        // Contents
        lblRandomMarriageMethod = createLabel("RandomMarriageMethod", null);
        comboRandomMarriageMethod.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof RandomMarriageMethod) {
                    list.setToolTipText(((RandomMarriageMethod) value).getToolTipText());
                }
                return this;
            }
        });

        chkUseRandomClanPersonnelMarriages = createCheckBox("UseRandomClanPersonnelMarriages", null);
        chkUseRandomPrisonerMarriages = createCheckBox("UseRandomPrisonerMarriages", null);

        lblRandomMarriageAgeRange = createLabel("RandomMarriageAgeRange", null);
        spnRandomMarriageAgeRange = createSpinner("RandomMarriageAgeRange", null,
            10, 0, 100, 1.0);

        pnlPercentageRandomMarriage = createPercentageRandomMarriagePanel();

        // Layout the Panel
        final JPanel panel = createStandardPanel("RandomMarriages", true,
            "RandomMarriages");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblRandomMarriageMethod)
                    .addComponent(comboRandomMarriageMethod))
                .addComponent(chkUseRandomClanPersonnelMarriages)
                .addComponent(chkUseRandomPrisonerMarriages)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblRandomMarriageAgeRange)
                    .addComponent(spnRandomMarriageAgeRange))
                .addComponent(pnlPercentageRandomMarriage));

        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblRandomMarriageMethod)
                        .addComponent(comboRandomMarriageMethod)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                    .addComponent(chkUseRandomClanPersonnelMarriages)
                    .addComponent(chkUseRandomPrisonerMarriages)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblRandomMarriageAgeRange)
                        .addComponent(spnRandomMarriageAgeRange)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                    .addComponent(pnlPercentageRandomMarriage)));

        return panel;
    }

    /**
     * Creates a panel for setting percentage-based random marriage settings.
     *
     * @return a {@link JPanel} representing the Percentage Random Marriage panel with input components
     * for setting opposite-sex marriage dice size, same-sex marriage dice size, and new dependent
     * marriage dice size
     */
    private JPanel createPercentageRandomMarriagePanel() {
        // Contents
        lblRandomMarriageOppositeSexDiceSize = createLabel("RandomMarriageOppositeSexDiceSize",
            null);
        spnRandomMarriageDiceSize = createSpinner("RandomMarriageOppositeSexDiceSize",
            null, 5000, 0, 100000, 1);

        lblRandomSameSexMarriageDiceSize = createLabel("RandomSameSexMarriageDiceSize", null);
        spnRandomSameSexMarriageDiceSize = createSpinner("RandomSameSexMarriageDiceSize",
            null, 14, 0, 100000, 1);

        lblRandomNewDependentMarriage = createLabel("RandomNewDependentMarriage", null);
        spnRandomNewDependentMarriage = createSpinner("RandomSameSexMarriageDiceSize",
            null, 20, 0, 100000, 1);

        // Layout the Panel
        final JPanel panel = createStandardPanel("PercentageRandomMarriagePanel", true,
            "PercentageRandomMarriagePanel");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblRandomMarriageOppositeSexDiceSize)
                    .addComponent(spnRandomMarriageDiceSize))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblRandomSameSexMarriageDiceSize)
                    .addComponent(spnRandomSameSexMarriageDiceSize))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblRandomNewDependentMarriage)
                    .addComponent(spnRandomNewDependentMarriage)));

        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblRandomMarriageOppositeSexDiceSize)
                        .addComponent(spnRandomMarriageDiceSize)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblRandomSameSexMarriageDiceSize)
                        .addComponent(spnRandomSameSexMarriageDiceSize)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblRandomNewDependentMarriage)
                        .addComponent(spnRandomNewDependentMarriage)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))));

        return panel;
    }

    /**
     * Creates a tab for divorce settings with various checkboxes and panels for manual, clan personnel,
     * and prisoner divorces.
     *
     * @return a {@link JPanel} representing the Divorce tab with checkboxes for manual divorce,
     * clan personnel divorce, prisoner divorce, and a panel for configuring random divorce settings.
     */
    JPanel createDivorceTab() {
        // Header
        JPanel headerPanel = createHeaderPanel("DivorceTab",
            getImageDirectory() + "logo_clan_hells_horses.png",
            false, "", true);

        // Contents
        chkUseManualDivorce = createCheckBox("UseManualDivorce", null);
        chkUseClanPersonnelDivorce = createCheckBox("UseClanPersonnelDivorce", null);
        chkUsePrisonerDivorce = createCheckBox("UsePrisonerDivorce", null);

        pnlRandomDivorce = createRandomDivorcePanel();

        // Layout the Panel
        final JPanel panel = createStandardPanel("DivorceTab", true, "");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(headerPanel)
                .addComponent(chkUseManualDivorce)
                .addComponent(chkUseClanPersonnelDivorce)
                .addComponent(chkUsePrisonerDivorce)
                .addComponent(pnlRandomDivorce));

        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addComponent(headerPanel, Alignment.CENTER)
                    .addComponent(chkUseManualDivorce)
                    .addComponent(chkUseClanPersonnelDivorce)
                    .addComponent(chkUsePrisonerDivorce)
                    .addComponent(pnlRandomDivorce)));

        // Create Parent Panel and return
        return createParentPanel(panel, "DivorceTab");
    }

    /**
     * Creates a panel for the Divorce tab with checkboxes for manual divorce, clan personnel divorce,
     * prisoner divorce, and a panel for configuring random divorce settings.
     *
     * @return a {@link JPanel} representing the Divorce tab with various components for configuring divorce settings
     */
    private JPanel createRandomDivorcePanel() {
        // Contents
        lblRandomDivorceMethod = createLabel("RandomDivorceMethod", null);
        comboRandomDivorceMethod.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof RandomDivorceMethod) {
                    list.setToolTipText(((RandomDivorceMethod) value).getToolTipText());
                }
                return this;
            }
        });

        chkUseRandomOppositeSexDivorce = createCheckBox("UseRandomOppositeSexDivorce", null);
        chkUseRandomSameSexDivorce = createCheckBox("UseRandomSameSexDivorce", null);
        chkUseRandomClanPersonnelDivorce = createCheckBox("UseRandomClanPersonnelDivorce", null);
        chkUseRandomPrisonerDivorce = createCheckBox("UseRandomPrisonerDivorce", null);

        lblRandomDivorceDiceSize = createLabel("RandomDivorceDiceSize", null);
        spnRandomDivorceDiceSize = createSpinner("RandomDivorceDiceSize", null,
            900, 0, 100000, 1);

        // Layout the Panel
        final JPanel panel = createStandardPanel("RandomDivorcePanel", true, "RandomDivorcePanel");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblRandomDivorceMethod)
                    .addComponent(comboRandomDivorceMethod))
                .addComponent(chkUseRandomOppositeSexDivorce)
                .addComponent(chkUseRandomSameSexDivorce)
                .addComponent(chkUseRandomClanPersonnelDivorce)
                .addComponent(chkUseRandomPrisonerDivorce)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblRandomDivorceDiceSize)
                    .addComponent(spnRandomDivorceDiceSize)));

        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblRandomDivorceMethod)
                        .addComponent(comboRandomDivorceMethod)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                    .addComponent(chkUseRandomOppositeSexDivorce)
                    .addComponent(chkUseRandomSameSexDivorce)
                    .addComponent(chkUseRandomClanPersonnelDivorce)
                    .addComponent(chkUseRandomPrisonerDivorce)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblRandomDivorceDiceSize)
                        .addComponent(spnRandomDivorceDiceSize)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))));

        return panel;
    }

    /**
     * Creates a panel for the Procreation tab with header, general options panel, and random procreation panel.
     *
     * @return a {@link JPanel} representing the Procreation tab with header, general options panel,
     * and random procreation panel
     */
    JPanel createProcreationTab() {
        // Header
        JPanel headerPanel = createHeaderPanel("ProcreationTab",
            getImageDirectory() + "logo_clan_ice_hellion.png",
            false, "", true);

        // Contents
        pnlProcreationGeneralOptionsPanel = createProcreationGeneralOptionsPanel();
        pnlRandomProcreationPanel = createRandomProcreationPanel();

        // Layout the Panel
        final JPanel panel = createStandardPanel("ProcreationTab", true, "");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(headerPanel)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(pnlProcreationGeneralOptionsPanel)
                    .addComponent(pnlRandomProcreationPanel)));

        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addComponent(headerPanel, Alignment.CENTER)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(pnlProcreationGeneralOptionsPanel)
                        .addComponent(pnlRandomProcreationPanel)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))));

        // Create Parent Panel and return
        return createParentPanel(panel, "ProcreationTab");
    }

    /**
     * @return the {@link JPanel} containing the procreation settings components
     */
    private JPanel createProcreationGeneralOptionsPanel() {
        // Contents
        chkUseManualProcreation = createCheckBox("UseManualProcreation", null);
        chkUseClanPersonnelProcreation = createCheckBox("UseClanPersonnelProcreation", null);
        chkUsePrisonerProcreation = createCheckBox("UsePrisonerProcreation", null);

        lblMultiplePregnancyOccurrences = createLabel("MultiplePregnancyOccurrences", null);
        spnMultiplePregnancyOccurrences = createSpinner("MultiplePregnancyOccurrences", null,
            50, 1, 1000, 1);

        lblBabySurnameStyle = createLabel("BabySurnameStyle", null);
        comboBabySurnameStyle.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof BabySurnameStyle) {
                    list.setToolTipText(((BabySurnameStyle) value).getToolTipText());
                }
                return this;
            }
        });

        chkAssignNonPrisonerBabiesFounderTag = createCheckBox("AssignNonPrisonerBabiesFounderTag",
            null);
        chkAssignChildrenOfFoundersFounderTag = createCheckBox("AssignChildrenOfFoundersFounderTag",
            null);
        chkDetermineFatherAtBirth = createCheckBox("DetermineFatherAtBirth", null);
        chkDisplayTrueDueDate = createCheckBox("DisplayTrueDueDate", null);

        lblNoInterestInChildrenDiceSize = createLabel("NoInterestInChildrenDiceSize", null);
        spnNoInterestInChildrenDiceSize = createSpinner("NoInterestInChildrenDiceSize", null,
            3, 1, 100000, 1);

        chkUseMaternityLeave = createCheckBox("UseMaternityLeave", null);
        chkLogProcreation = createCheckBox("LogProcreation", null);

        pnlRandomProcreationPanel = createRandomProcreationPanel();

        // Layout the Panel
        final JPanel panel = createStandardPanel("ProcreationGeneralOptionsPanel",
            false, "");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(chkUseManualProcreation)
                .addComponent(chkUseClanPersonnelProcreation)
                .addComponent(chkUsePrisonerProcreation)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblMultiplePregnancyOccurrences)
                    .addComponent(spnMultiplePregnancyOccurrences))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblBabySurnameStyle)
                    .addComponent(comboBabySurnameStyle))
                .addComponent(chkAssignNonPrisonerBabiesFounderTag)
                .addComponent(chkAssignChildrenOfFoundersFounderTag)
                .addComponent(chkDetermineFatherAtBirth)
                .addComponent(chkDisplayTrueDueDate)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblNoInterestInChildrenDiceSize)
                    .addComponent(spnNoInterestInChildrenDiceSize))
                .addComponent(chkUseMaternityLeave)
                .addComponent(chkLogProcreation));

        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addComponent(chkUseManualProcreation)
                    .addComponent(chkUseClanPersonnelProcreation)
                    .addComponent(chkUsePrisonerProcreation)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblMultiplePregnancyOccurrences)
                        .addComponent(spnMultiplePregnancyOccurrences)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblBabySurnameStyle)
                        .addComponent(comboBabySurnameStyle)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                    .addComponent(chkAssignNonPrisonerBabiesFounderTag)
                    .addComponent(chkAssignChildrenOfFoundersFounderTag)
                    .addComponent(chkDetermineFatherAtBirth)
                    .addComponent(chkDisplayTrueDueDate)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblNoInterestInChildrenDiceSize)
                        .addComponent(spnNoInterestInChildrenDiceSize)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                    .addComponent(chkUseMaternityLeave)
                    .addComponent(chkLogProcreation)));

        return panel;
    }

    /**
     * @return a {@link JPanel} containing the configured components for random procreation settings
     */
    private JPanel createRandomProcreationPanel() {
        // Contents
        lblRandomProcreationMethod = createLabel("RandomProcreationMethod", null);
        comboRandomProcreationMethod.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof RandomProcreationMethod) {
                    list.setToolTipText(((RandomProcreationMethod) value).getToolTipText());
                }
                return this;
            }
        });

        chkUseRelationshiplessRandomProcreation =createCheckBox("UseRelationshiplessRandomProcreation",
            null);
        chkUseRandomClanPersonnelProcreation = createCheckBox("UseRandomClanPersonnelProcreation",
            null);
        chkUseRandomPrisonerProcreation = createCheckBox("UseRandomPrisonerProcreation", null);

        lblRandomProcreationRelationshipDiceSize = createLabel("RandomProcreationRelationshipDiceSize",
            null);
        spnRandomProcreationRelationshipDiceSize = createSpinner("RandomProcreationRelationshipDiceSize",
            null, 621, 0, 100000, 1);

        lblRandomProcreationRelationshiplessDiceSize = createLabel("RandomProcreationRelationshiplessDiceSize",
            null);
        spnRandomProcreationRelationshiplessDiceSize = createSpinner("RandomProcreationRelationshiplessDiceSize",
            null, 1861, 0, 100000, 1);

        // Layout the Panel
        final JPanel panel = createStandardPanel("RandomProcreationPanel", true,
            "RandomProcreationPanel");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblRandomProcreationMethod)
                    .addComponent(comboRandomProcreationMethod))
                .addComponent(chkUseRelationshiplessRandomProcreation)
                .addComponent(chkUseRandomClanPersonnelProcreation)
                .addComponent(chkUseRandomPrisonerProcreation)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblRandomProcreationRelationshipDiceSize)
                    .addComponent(spnRandomProcreationRelationshipDiceSize))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblRandomProcreationRelationshiplessDiceSize)
                    .addComponent(spnRandomProcreationRelationshiplessDiceSize)));

        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblRandomProcreationMethod)
                        .addComponent(comboRandomProcreationMethod)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                    .addComponent(chkUseRelationshiplessRandomProcreation)
                    .addComponent(chkUseRandomClanPersonnelProcreation)
                    .addComponent(chkUseRandomPrisonerProcreation)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblRandomProcreationRelationshipDiceSize)
                        .addComponent(spnRandomProcreationRelationshipDiceSize)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblRandomProcreationRelationshiplessDiceSize)
                        .addComponent(spnRandomProcreationRelationshiplessDiceSize)
                        .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))));

        return panel;
    }
}
