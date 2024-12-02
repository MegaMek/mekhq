package mekhq.gui.dialog;

import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.bot.princess.PrincessException;
import megamek.client.ui.baseComponents.MMButton;
import megamek.client.ui.dialogs.BotConfigDialog;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.gui.dialog.helpDialogs.AutoResolveBehaviorSettingsHelpDialog;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;

public class AutoResolveBehaviorSettingsDialog
    extends BotConfigDialog
{
    private final static MMLogger logger = MMLogger.create(AutoResolveBehaviorSettingsDialog.class);

    private static final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.AutoResolveBehaviorSettingsDialog",
        MekHQ.getMHQOptions().getLocale());

    private Campaign campaign;
    private final BehaviorSettingsFactory behaviorSettingsFactory = BehaviorSettingsFactory.getInstance();
    private JButton autoResolveHelpButton;

    /**
     * Creates a new instance of AutoResolveBehaviorSettingsDialog.
     * <p>
     * This dialog is used to configure the auto resolve behavior settings for a campaign.
     * It creates a default preset with a predetermined name and sets the behavior settings
     * to the campaign's auto resolve behavior settings.
     * </p>
     * @param frame The parent frame.
     * @param campaign The campaign to get the auto resolve behavior settings from.
     */
    public AutoResolveBehaviorSettingsDialog(final JFrame frame, final Campaign campaign) {
        super(frame, campaign.getName() + ":AI", campaign.getAutoResolveBehaviorSettings(), null);
        setAlwaysOnTop(true);
        setCampaign(campaign);
    }

    private JButton getAutoResolveHelpButton() {
        return autoResolveHelpButton;
    }

    private void setAutoResolveHelpButton(JButton autoResolveHelpButton) {
        this.autoResolveHelpButton = autoResolveHelpButton;
    }

    @Override
    protected Container createCenterPane() {
        var result = super.createCenterPane();
        result.add(createAutoResolveHelpButton());
        return result;
    }

    protected JPanel createAutoResolveHelpButton() {
        JPanel result = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        result.setAlignmentX(LEFT_ALIGNMENT);

        setAutoResolveHelpButton(new MMButton("btnNewYear",
            resourceMap.getString("AutoResolveBehaviorSettingsDialog.help"),
            resourceMap.getString("AutoResolveBehaviorSettingsDialog.helpTooltip"),
            this::autoResolveHelpActionPerformed));

        result.add(getAutoResolveHelpButton());
        return result;
    }

    private void setCampaign(final Campaign campaign) {
        this.campaign = campaign;
    }

    private void updateBehaviorSettings() {
        var autoResolveBehaviorSettings = getBehaviorSettings();
        try {
            autoResolveBehaviorSettings.setDescription(campaign.getName() + ":AI");
        } catch (PrincessException e) {
            // This should never happen, but if it does, it is not a critical error.
            // We set the auto resolve behavior setting, ignore that its description
            // could not be set, log the error and continue.
            logger.error("Could not set description for auto resolve behavior settings", e);
            campaign.setAutoResolveBehaviorSettings(autoResolveBehaviorSettings);
            return;
        }

        behaviorSettingsFactory.addBehavior(autoResolveBehaviorSettings);
        behaviorSettingsFactory.saveBehaviorSettings(false);

        campaign.setAutoResolveBehaviorSettings(autoResolveBehaviorSettings);
    }

    protected void autoResolveHelpActionPerformed(ActionEvent evt) {
        showAutoResolveHelp();
    }

    private void showAutoResolveHelp() {
        var autoResolveHelp = new AutoResolveBehaviorSettingsHelpDialog(getFrame());
        autoResolveHelp.setVisible(true);
        autoResolveHelp.setAlwaysOnTop(true);
    }

    @Override
    protected void okAction() {
        super.okAction();
        updateBehaviorSettings();
    }

}
