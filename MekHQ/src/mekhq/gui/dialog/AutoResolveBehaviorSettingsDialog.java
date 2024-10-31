package mekhq.gui.dialog;

import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.bot.princess.PrincessException;
import megamek.client.ui.dialogs.BotConfigDialog;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;

import javax.swing.*;

public class AutoResolveBehaviorSettingsDialog
    extends BotConfigDialog
{
    private final static MMLogger logger = MMLogger.create(AutoResolveBehaviorSettingsDialog.class);

    private Campaign campaign;
    private final BehaviorSettingsFactory behaviorSettingsFactory = BehaviorSettingsFactory.getInstance();

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

    public void setCampaign(final Campaign campaign) {
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

    @Override
    protected void okAction() {
        super.okAction();
        updateBehaviorSettings();
    }

}
