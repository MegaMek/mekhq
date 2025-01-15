package mekhq.gui.dialog;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignFactory.CampaignProblemType;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.MHQDialogImmersive;

import java.util.List;
import java.util.ResourceBundle;

import static mekhq.campaign.Campaign.AdministratorSpecialization.COMMAND;
import static mekhq.campaign.CampaignFactory.CampaignProblemType.CANT_LOAD_FROM_NEWER_VERSION;

/**
 * Dialog to inform and handle campaign-loading problems within MekHQ.
 *
 * <p>This dialog prompts the user with both in-character and out-of-character messages,
 * providing actionable options if any issues arise during campaign load, such as version
 * incompatibility or missing contracts.</p>
 *
 * <p>The dialog presents two options: "Cancel" to abort loading the campaign or
 * "Continue Regardless" to proceed despite the detected issues. It dynamically generates
 * text based on the problem type and campaign information.</p>
 */
public class CampaignHasProblemOnLoad extends MHQDialogImmersive {
    private static final String BUNDLE_KEY = "mekhq.resources.CampaignHasProblemOnLoad";
    private static final ResourceBundle resources = ResourceBundle.getBundle(
        BUNDLE_KEY, MekHQ.getMHQOptions().getLocale());

    /**
     * Constructs the dialog to handle campaign load problems.
     *
     * <p>The dialog is built using localized messages for both
     * in-character and out-of-character messages, following the detected
     * problem type and campaign details. It also sets up predefined
     * buttons for user interaction.</p>
     *
     * @param campaign    the {@link Campaign} for which the load problem dialog is presented
     * @param problemType the {@link CampaignProblemType} specifying the nature of the load problem
     */
    public CampaignHasProblemOnLoad(Campaign campaign, CampaignProblemType problemType) {
        super(campaign, getSpeaker(campaign), null, createInCharacterMessage(campaign, problemType),
            createButtons(problemType), createOutOfCharacterMessage(problemType), 0,
            null, null, null);
    }

    /**
     * Generates the list of buttons for the dialog based on the specified problem type.
     *
     * <p>Buttons include:</p>
     * <ul>
     *   <li>"Cancel" button: Stops loading the campaign in all scenarios.</li>
     *   <li>"Continue" button: Allows proceeding with loading the campaign, provided the problem type permits it.</li>
     * </ul>
     *
     * <p>If the problem type is {@code CANT_LOAD_FROM_NEWER_VERSION}, only the "Cancel" button is available
     * because proceeding is not allowed for this issue. For other problem types, both "Cancel" and "Continue"
     * buttons are included in the dialog.</p>
     *
     * @param problemType the {@link CampaignProblemType} specifying the nature of the issue, which determines
     *                    the buttons to display
     * @return a {@link List} of {@link ButtonLabelTooltipPair} objects representing the dialog's buttons
     */
    private static List<ButtonLabelTooltipPair> createButtons(CampaignProblemType problemType) {
        ButtonLabelTooltipPair btnCancel = new ButtonLabelTooltipPair(
            resources.getString("cancel.button"), null);

        ButtonLabelTooltipPair btnContinue = new ButtonLabelTooltipPair(
            resources.getString("continue.button"), null);

        if (problemType == CANT_LOAD_FROM_NEWER_VERSION) {
            return List.of(btnCancel);
        } else {
            return List.of(btnCancel, btnContinue);
        }
    }

    /**
     * Retrieves the speaker for in-character dialog.
     *
     * <p>The speaker is determined as the senior administrator for the campaign
     * with the "Command" specialization. If no such administrator is found, {@code null}
     * is returned.</p>
     *
     * @param campaign the {@link Campaign} whose senior administrator is to be retrieved
     * @return a {@link Person} representing the senior administrator, or {@code null} if none exists
     */
    private static @Nullable Person getSpeaker(Campaign campaign) {
        return campaign.getSeniorAdminPerson(COMMAND);
    }

    /**
     * Creates the in-character message dynamically based on the problem type.
     *
     * <p>This message is localized and assembled using resource bundles, with campaign-specific
     * information such as the commander's address.</p>
     *
     * @param campaign    the {@link Campaign} for which the in-character message is generated
     * @param problemType the {@link CampaignProblemType} specifying the nature of the load problem
     * @return a localized {@link String} containing the in-character message
     */
    private static String createInCharacterMessage(Campaign campaign, CampaignProblemType problemType) {
        String typeKey = problemType.toString();
        String commanderAddress = campaign.getCommanderAddress(false);

        return String.format(resources.getString(typeKey + ".message"), commanderAddress);
    }

    /**
     * Creates the out-of-character message dynamically based on the problem type.
     *
     * <p>This message is localized and is more technical or process-oriented,
     * explaining the detected issues in plain terms.</p>
     *
     * @param problemType the {@link CampaignProblemType} specifying the nature of the load problem
     * @return a localized {@link String} containing the out-of-character message
     */
    private static String createOutOfCharacterMessage(CampaignProblemType problemType) {
        String typeKey = problemType.toString();
        return resources.getString(typeKey + ".ooc");
    }
}
