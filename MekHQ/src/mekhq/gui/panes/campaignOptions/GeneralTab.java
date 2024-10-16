package mekhq.gui.panes.campaignOptions;

import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.dialogs.CamoChooserDialog;
import megamek.client.ui.swing.util.FlatLafStyleBuilder;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.common.icons.Camouflage;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.icons.StandardForceIcon;
import mekhq.campaign.personnel.backgrounds.BackgroundsController;
import mekhq.campaign.rating.UnitRatingMethod;
import mekhq.campaign.universe.Factions;
import mekhq.gui.baseComponents.AbstractMHQScrollablePanel;
import mekhq.gui.baseComponents.AbstractMHQTabbedPane;
import mekhq.gui.baseComponents.DefaultMHQScrollablePanel;
import mekhq.gui.dialog.DateChooser;
import mekhq.gui.dialog.iconDialogs.UnitIconDialog;
import mekhq.gui.displayWrappers.FactionDisplay;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalDate;

import static mekhq.gui.panes.campaignOptions.CampaignOptionsUtilities.*;

/**
 * Represents a tab that allows the user to configure general settings for a campaign.
 * Extends the {@link AbstractMHQTabbedPane} class.
 */
public class GeneralTab {
    // region Variable Declarations
    JFrame frame;
    String name;

    private final Campaign campaign;

    private JLabel lblName;
    private JTextField txtName;
    private JButton btnNameGenerator;
    private JLabel lblFaction;
    private MMComboBox<FactionDisplay> comboFaction;
    private JLabel lblReputation;
    private MMComboBox<UnitRatingMethod> unitRatingMethodCombo;
    private JLabel lblManualUnitRatingModifier;
    private JSpinner manualUnitRatingModifier;
    private JLabel lblDate;
    private JButton btnDate;
    private LocalDate date;
    private JLabel lblCamo;
    private JButton btnCamo;
    private Camouflage camouflage;
    private JLabel lblIcon;
    private JButton btnIcon;
    private StandardForceIcon unitIcon;

    /**
     * Constructs a new {@link GeneralTab} object.
     *
     * @param campaign the campaign object associated with the tab
     * @param frame the {@link JFrame} object that contains the tab
     * @param name the name of the tab
     */
    public GeneralTab(Campaign campaign, JFrame frame, String name) {
        this.frame = frame;
        this.name = name;
        this.campaign = campaign;
        this.date = campaign.getLocalDate();
        this.camouflage = campaign.getCamouflage();
        this.unitIcon = campaign.getUnitIcon();

        initialize();
    }

    /**
     * Creates the general tab for a campaign.
     *
     * @return the created general tab as an AbstractMHQScrollablePanel
     */
    public AbstractMHQScrollablePanel createGeneralTab() {
        // Header
        JPanel headerPanel = createGeneralHeader();

        // Campaign name
        lblName = new CampaignOptionsLabel("Name");
        txtName = new CampaignOptionsTextField("Name");

        // Generate new random campaign name
        btnNameGenerator = new CampaignOptionsButton("NameGenerator");
        btnNameGenerator.addActionListener(e -> txtName.setText(BackgroundsController
                .randomMercenaryCompanyNameGenerator(campaign.getFlaggedCommander())));

        // Campaign faction
        lblFaction = new CampaignOptionsLabel("Faction");
        comboFaction.setSelectedItem(new FactionDisplay(campaign.getFaction(), campaign.getLocalDate()));

        // Reputation
        lblReputation = new CampaignOptionsLabel("Reputation");
        lblManualUnitRatingModifier = new CampaignOptionsLabel("ManualUnitRatingModifier");
        manualUnitRatingModifier = new CampaignOptionsSpinner("ManualUnitRatingModifier",
            0, -200, 200, 1);

        // Date
        lblDate = new CampaignOptionsLabel("Date");
        btnDate = new CampaignOptionsButton("Date");
        btnDate.addActionListener(this::btnDateActionPerformed);

        // Camouflage
        lblCamo = new CampaignOptionsLabel("Camo");
        btnCamo.setName("btnCamo");
        btnCamo.addActionListener(this::btnCamoActionPerformed);

        // Unit icon
        lblIcon = new CampaignOptionsLabel("Icon");
        btnIcon.setName("btnIcon");
        btnIcon.addActionListener(this::btnIconActionPerformed);

        // Initialize the parent panel
        AbstractMHQScrollablePanel generalPanel = new DefaultMHQScrollablePanel(frame, "generalPanel",
            new GridBagLayout());

        // Layout the Panel
        JPanel panel = new JPanel();
        GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridwidth = 5;
        panel.add(headerPanel, layout);

        layout.gridwidth = 1;
        layout.gridy++;
        panel.add(lblName, layout);

        layout.gridwidth = 2;
        layout.weightx = 1;
        panel.add(txtName, layout);

        layout.gridwidth = 1;
        layout.weightx = 0;
        layout.fill = GridBagConstraints.NONE;
        panel.add(btnNameGenerator, layout);
        layout.fill = GridBagConstraints.HORIZONTAL;

        layout.gridy++;
        panel.add(lblFaction, layout);
        layout.gridwidth = 2;
        panel.add(comboFaction, layout);

        layout.gridwidth = 1;
        layout.gridy++;
        panel.add(lblReputation, layout);
        layout.gridwidth = 2;
        panel.add(unitRatingMethodCombo, layout);

        layout.gridwidth = 1;
        layout.gridy++;
        panel.add(lblManualUnitRatingModifier, layout);
        panel.add(manualUnitRatingModifier, layout);

        layout.gridy++;
        panel.add(lblDate, layout);
        panel.add(btnDate, layout);

        layout.gridy++;
        layout.gridwidth = 5;
        layout.gridx = GridBagConstraints.RELATIVE;

        JPanel iconsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        iconsPanel.setBorder(BorderFactory.createTitledBorder(""));

        iconsPanel.add(lblIcon);
        iconsPanel.add(btnIcon);
        iconsPanel.add(Box.createHorizontalStrut(50));
        iconsPanel.add(lblCamo);
        iconsPanel.add(btnCamo);

        panel.add(iconsPanel, layout);

        final JPanel outerPanel = new CampaignOptionsStandardPanel("generalTab", true);
        // Add some padding left and right (inside the border!)
        outerPanel.setLayout(new BorderLayout());
        outerPanel.add(Box.createHorizontalStrut(25), BorderLayout.LINE_START);
        outerPanel.add(Box.createHorizontalStrut(25), BorderLayout.LINE_END);
        outerPanel.add(panel, BorderLayout.CENTER);
        JLabel label = new JLabel(resources.getString("lblQuote01.text"));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        outerPanel.add(label, BorderLayout.SOUTH);

        generalPanel.add(outerPanel);

        return generalPanel;
    }

    /**
     * Creates a {@link JPanel} with a header containing an image, title, and body text.
     *
     * @return the created {@link JPanel} with the header
     */
    private static JPanel createGeneralHeader() {
        ImageIcon imageIcon = new ImageIcon("data/images/misc/MekHQ.png");
        JLabel imageLabel = new JLabel(imageIcon);

        final JLabel lblHeader = new JLabel(resources.getString("lblGeneral.text"), SwingConstants.CENTER);
        new FlatLafStyleBuilder().font(MHQConstants.PROJECT_NAME).bold().size(3).apply(lblHeader);
        lblHeader.setName("lblGeneral");

        JLabel lblBody = new JLabel(String.format("<html>%s</html>",
            resources.getString("lblGeneralBody.text")), SwingConstants.CENTER);
        lblBody.setName("lblGeneralHeaderBody");
        Dimension size = lblBody.getPreferredSize();
        lblBody.setMaximumSize(UIUtil.scaleForGUI(750, size.height));

        final JPanel panel = new CampaignOptionsStandardPanel("pnlGeneralHeaderPanel", false);
        final GroupLayout layout = createGroupLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(lblHeader)
                .addComponent(imageLabel)
                .addComponent(lblBody)
                .addGap(UIUtil.scaleForGUI(20)));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.CENTER)
                .addComponent(lblHeader)
                .addComponent(imageLabel)
                .addComponent(lblBody));

        return panel;
    }

    /**
     * Initialize the components of the {@link GeneralTab} class.
     */
    private void initialize() {
        lblName = new JLabel();
        txtName = new JTextField();

        btnNameGenerator = new JButton();

        lblFaction = new JLabel();
        comboFaction = new MMComboBox<>("comboFaction", buildFactionDisplayOptions());

        lblReputation = new JLabel();
        unitRatingMethodCombo = new MMComboBox<>("unitRatingMethodCombo", UnitRatingMethod.values());

        lblManualUnitRatingModifier = new JLabel();
        manualUnitRatingModifier = new JSpinner();

        lblDate = new JLabel();
        btnDate = new JButton();

        lblCamo = new JLabel();
        btnCamo = new JButton() {
            @Override
            public Dimension getPreferredSize() {
                return UIUtil.scaleForGUI(100, 100);
            }
        };

        lblIcon = new JLabel();
        btnIcon = new JButton() {
            @Override
            public Dimension getPreferredSize() {
                return UIUtil.scaleForGUI(100, 100);
            }
        };
    }

    /**
     * Builds a {@link DefaultComboBoxModel} of {@link FactionDisplay} options.
     */
    private DefaultComboBoxModel<FactionDisplay> buildFactionDisplayOptions() {
        DefaultComboBoxModel<FactionDisplay> factionModel = new DefaultComboBoxModel<>();

        factionModel.addAll(FactionDisplay.getSortedValidFactionDisplays(
            Factions.getInstance().getChoosableFactions(), campaign.getLocalDate()));

        return factionModel;
    }

    /**
     * This method is called when the "btnDate" button is clicked.
     * It shows a date chooser dialog and sets the selected date if the user chooses a date.
     *
     * @param actionEvent The action event that triggered the method.
     */
    private void btnDateActionPerformed(ActionEvent actionEvent) {
        // show the date chooser
        DateChooser dateChooser = new DateChooser(frame, date);
        // user can either choose a date or cancel by closing
        if (dateChooser.showDateChooser() == DateChooser.OK_OPTION) {
            setDate(dateChooser.getDate());
        }
    }

    /**
     * Sets the date for the {@link RepairAndMaintenanceTab}.
     * This method is called when the "btnDate" button is clicked.
     * It shows a date chooser dialog and sets the selected date if the user chooses a date.
     *
     * @param date The selected date. This parameter may contain a null value.
     */
    private void setDate(final @Nullable LocalDate date) {
        if (date == null) {
            return;
        }

        this.date = date;
        btnDate.setText(MekHQ.getMHQOptions().getDisplayFormattedDate(date));

        final FactionDisplay factionDisplay = comboFaction.getSelectedItem();
        comboFaction.removeAllItems();
        ((DefaultComboBoxModel<FactionDisplay>) comboFaction.getModel()).addAll(FactionDisplay
            .getSortedValidFactionDisplays(Factions.getInstance().getChoosableFactions(), date));
        comboFaction.setSelectedItem(factionDisplay);
    }

    /**
     * This method is called when the "btnCamo" button is clicked.
     * It opens a {@link CamoChooserDialog} and sets the selected camouflage if the user confirms the
     * dialog.
     *
     * @param actionEvent The action event that triggered this method.
     */
    private void btnCamoActionPerformed(ActionEvent actionEvent) {
        CamoChooserDialog camoChooserDialog = new CamoChooserDialog(frame, camouflage);
        if (camoChooserDialog.showDialog().isConfirmed()) {
            camouflage = camoChooserDialog.getSelectedItem();
            btnCamo.setIcon(camouflage.getImageIcon());
        }
    }

    private void btnIconActionPerformed(ActionEvent actionEvent) {
        final UnitIconDialog unitIconDialog = new UnitIconDialog(frame, unitIcon);
        if (unitIconDialog.showDialog().isConfirmed() && (unitIconDialog.getSelectedItem() != null)) {
            unitIcon = unitIconDialog.getSelectedItem();
            btnIcon.setIcon(unitIcon.getImageIcon(UIUtil.scaleForGUI(75)));
        }
    }
}
