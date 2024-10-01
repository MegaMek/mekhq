package mekhq.gui.panes.campaignOptions.tabs;

import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.dialogs.CamoChooserDialog;
import megamek.common.annotations.Nullable;
import megamek.common.icons.Camouflage;
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
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import static mekhq.gui.panes.campaignOptions.CampaignOptionsUtilities.*;

/**
 * Represents a tab that allows the user to configure general settings for a campaign.
 * Extends the {@link AbstractMHQTabbedPane} class.
 */
public class GeneralTab extends AbstractMHQTabbedPane {
    // region Variable Declarations
    private static String RESOURCE_PACKAGE = "mekhq/resources/NEWCampaignOptionsDialog";
    private static final ResourceBundle resources = ResourceBundle.getBundle(RESOURCE_PACKAGE,
        MekHQ.getMHQOptions().getLocale());

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
        super(frame, name);

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
        // Promotional Image
        JPanel imagePanel = createHeaderPanel("General", "data/images/misc/MekHQ.png",
            false, "", false);

        // Campaign name
        Map<JLabel, JTextField> nameFields = createLabeledTextField("Name", null,
            300, 30, null, null);
        for (Entry<JLabel, JTextField> entry : nameFields.entrySet()) {
            lblName = entry.getKey();
            txtName = entry.getValue();
        }

        // Generate new random campaign name
        btnNameGenerator = new JButton(resources.getString("btnNameGenerator.text"));
        btnNameGenerator.setToolTipText(resources.getString("btnNameGenerator.tooltip"));
        btnNameGenerator.setName("btnNameGenerator");
        btnNameGenerator.addActionListener(e -> txtName.setText(BackgroundsController
                .randomMercenaryCompanyNameGenerator(campaign.getFlaggedCommander())));

        // Campaign faction
        lblFaction = createLabel("lblFaction", null);
        comboFaction.setSelectedItem(new FactionDisplay(campaign.getFaction(), campaign.getLocalDate()));
        comboFaction.setMinimumSize(new Dimension(275, 30));
        comboFaction.setMaximumSize(new Dimension(275, 30));

        // Reputation
        lblReputation = createLabel("lblReputation", null);
        unitRatingMethodCombo.setMinimumSize(new Dimension(150, 30));
        unitRatingMethodCombo.setMaximumSize(new Dimension(150, 30));

        Map<JLabel, JSpinner> manualReputationModifierFields = createLabeledSpinner(
            "ManualUnitRatingModifier", null, 0, -200,
            200, 1);
        for (Entry<JLabel, JSpinner> entry : manualReputationModifierFields.entrySet()) {
            lblManualUnitRatingModifier = entry.getKey();
            manualUnitRatingModifier = entry.getValue();
        }

        // Date
        lblDate = createLabel("lblDate", null);
        btnDate.setName("btnDate");
        btnDate.setMinimumSize(new Dimension(100, 30));
        btnDate.setMaximumSize(new Dimension(100, 30));
        btnDate.addActionListener(this::btnDateActionPerformed);

        // Camouflage
        lblCamo = createLabel("lblCamo", null);
        btnCamo.setName("btnCamo");
        btnCamo.setMinimumSize(new Dimension(84, 72));
        btnCamo.setMaximumSize(new Dimension(84, 72));
        btnCamo.addActionListener(this::btnCamoActionPerformed);

        // Unit icon
        lblIcon = createLabel("lblIcon", null);
        btnIcon.setMinimumSize(new Dimension(84, 72));
        btnIcon.setMaximumSize(new Dimension(84, 72));
        btnIcon.addActionListener(evt -> {
            final UnitIconDialog unitIconDialog = new UnitIconDialog(getFrame(), unitIcon);
            if (unitIconDialog.showDialog().isConfirmed() && (unitIconDialog.getSelectedItem() != null)) {
                unitIcon = unitIconDialog.getSelectedItem();
                btnIcon.setIcon(unitIcon.getImageIcon(75));
            }
        });

        // Initialize the parent panel
        AbstractMHQScrollablePanel generalPanel = new DefaultMHQScrollablePanel(getFrame(), "generalPanel",
            new GridBagLayout());

        // Layout the Panel
        final JPanel panel = createStandardPanel("generalTab", true, "");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(imagePanel)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblName)
                    .addComponent(txtName, Alignment.LEADING)
                    .addComponent(btnNameGenerator))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblFaction)
                    .addComponent(comboFaction, Alignment.LEADING))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblReputation)
                    .addComponent(unitRatingMethodCombo, Alignment.LEADING))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblManualUnitRatingModifier)
                    .addComponent(manualUnitRatingModifier, Alignment.LEADING))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblDate)
                    .addComponent(btnDate, Alignment.LEADING))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblCamo)
                    .addComponent(btnCamo, Alignment.LEADING))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblIcon)
                    .addComponent(btnIcon, Alignment.LEADING)));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(imagePanel)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblName)
                    .addComponent(txtName)
                    .addComponent(btnNameGenerator))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblFaction)
                    .addComponent(comboFaction))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblReputation)
                    .addComponent(unitRatingMethodCombo))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblManualUnitRatingModifier)
                    .addComponent(manualUnitRatingModifier))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblDate)
                    .addComponent(btnDate))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblCamo)
                    .addComponent(btnCamo))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblIcon)
                    .addComponent(btnIcon)));

        generalPanel.add(panel);

        return generalPanel;
    }

    @Override
    protected void initialize() {
        lblName = new JLabel();
        txtName = new JTextField();

        btnNameGenerator = new JButton();

        lblFaction = new JLabel();
        DefaultComboBoxModel<FactionDisplay> factionModel = new DefaultComboBoxModel<>();
        factionModel.addAll(FactionDisplay.getSortedValidFactionDisplays(
            Factions.getInstance().getChoosableFactions(), campaign.getLocalDate()));
        comboFaction = new MMComboBox<>("comboFaction", factionModel);

        lblReputation = new JLabel();
        unitRatingMethodCombo = new MMComboBox<>("unitRatingMethodCombo", UnitRatingMethod.values());

        lblManualUnitRatingModifier = new JLabel();
        manualUnitRatingModifier = new JSpinner();

        lblDate = new JLabel();
        btnDate = new JButton();

        lblCamo = new JLabel();
        btnCamo = new JButton();

        lblIcon = new JLabel();
        btnIcon = new JButton();
    }


    /**
     * This method is called when the "btnDate" button is clicked.
     * It shows a date chooser dialog and sets the selected date if the user chooses a date.
     *
     * @param actionEvent The action event that triggered the method.
     */
    private void btnDateActionPerformed(ActionEvent actionEvent) {
        // show the date chooser
        DateChooser dateChooser = new DateChooser(getFrame(), date);
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
        CamoChooserDialog camoChooserDialog = new CamoChooserDialog(getFrame(), camouflage);
        if (camoChooserDialog.showDialog().isConfirmed()) {
            camouflage = camoChooserDialog.getSelectedItem();
            btnCamo.setIcon(camouflage.getImageIcon());
        }
    }
}
