package mekhq.gui.dialog;

import megamek.codeUtilities.ObjectUtility;
import megamek.common.options.IOption;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.randomEvents.personalities.PersonalityController.PersonalityTraitType;
import mekhq.campaign.randomEvents.personalities.enums.Aggression;
import mekhq.campaign.randomEvents.personalities.enums.Ambition;
import mekhq.campaign.randomEvents.personalities.enums.Greed;
import mekhq.campaign.randomEvents.personalities.enums.Social;
import mekhq.gui.baseComponents.MHQDialogImmersive;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import static megamek.common.options.PilotOptions.LVL3_ADVANTAGES;
import static mekhq.campaign.Campaign.AdministratorSpecialization.HR;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

/**
 * Provides the dialog for handling Ronin offers in the campaign.
 *
 * <p>This dialog is used to present the player with the option to accept or decline a Ronin character
 * as part of their campaign. It handles both in-character (IC) and out-of-character (OOC) messaging
 * based on whether the offer is initiated by the HR Administrator or directly by the Ronin.</p>
 */
public class RoninOfferDialog extends MHQDialogImmersive {
    private static final String RESOURCE_BUNDLE = "mekhq.resources." + RoninOfferDialog.class.getSimpleName();

    /**
     * Constructs the Ronin Offer dialog.
     *
     * @param campaign   the {@link Campaign} in which the offer is taking place
     * @param isFromHR   {@code true} if the offer is initiated by the HR Administrator;
     *                   {@code false} if initiated by the Ronin
     * @param ronin      the {@link Person} representing the Ronin making the offer
     */
    public RoninOfferDialog(Campaign campaign, boolean isFromHR, Person ronin) {
        super(campaign, isFromHR ? campaign.getSeniorAdminPerson(HR) : ronin, null,
              createInCharacterMessage(isFromHR, ronin, campaign.getCommanderAddress(false)),
              createButtons(isFromHR, ronin.getCallsign()), isFromHR ? null : createOutOfCharacterMessage(ronin),
              null, true);
    }

    /**
     * Creates the in-character message for the dialog.
     *
     * <p>The message varies depending on whether the offer is initiated by the HR Administrator or
     * by the Ronin. If initiated by HR, the message will reference the administrative context. Otherwise,
     * the message will include the Ronin's personality traits and their context.</p>
     *
     * @param isFromHR           {@code true} if the offer originates from HR, {@code false} if from the Ronin
     * @param ronin              the {@link Person} representing the Ronin
     * @param commanderAddress   the address of the commander for use in the message
     * @return the in-character message as a {@link String}
     */
    private static String createInCharacterMessage(boolean isFromHR, Person ronin, String commanderAddress) {
        if (isFromHR) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "message.ic.fromHR",
                  commanderAddress, ronin.getCallsign().toUpperCase());
        }

        List<PersonalityTraitType> normalTraits = new ArrayList<>();
        List<PersonalityTraitType> majorTraits = new ArrayList<>();

        Aggression aggression = ronin.getAggression();
        if (!aggression.isNone()) {
            if (aggression.isTraitMajor()) {
                majorTraits.add(PersonalityTraitType.AGGRESSION);
            } else {
                normalTraits.add(PersonalityTraitType.AGGRESSION);
            }
        }

        Ambition ambition = ronin.getAmbition();
        if (!ambition.isNone()) {
            if (ambition.isTraitMajor()) {
                majorTraits.add(PersonalityTraitType.AMBITION);
            } else {
                normalTraits.add(PersonalityTraitType.AMBITION);
            }
        }

        Greed greed = ronin.getGreed();
        if (!greed.isNone()) {
            if (greed.isTraitMajor()) {
                majorTraits.add(PersonalityTraitType.GREED);
            } else {
                normalTraits.add(PersonalityTraitType.GREED);
            }
        }

        Social social = ronin.getSocial();
        if (!social.isNone()) {
            if (social.isTraitMajor()) {
                majorTraits.add(PersonalityTraitType.SOCIAL);
            } else {
                normalTraits.add(PersonalityTraitType.SOCIAL);
            }
        }

        PersonalityTraitType chosenTrait = null;

        if (!majorTraits.isEmpty()) {
            chosenTrait = ObjectUtility.getRandomItem(majorTraits);
        }

        if (!normalTraits.isEmpty()) {
            chosenTrait = ObjectUtility.getRandomItem(normalTraits);
        }

        String message = "";
        if (chosenTrait != null) {
            message = switch (chosenTrait) {
                case AGGRESSION -> aggression.getRoninMessage(commanderAddress);
                case AMBITION -> ambition.getRoninMessage(commanderAddress);
                case GREED -> greed.getRoninMessage(commanderAddress);
                case SOCIAL -> social.getRoninMessage(commanderAddress);
                default -> "";
            };
        }

        if (message.isBlank()) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "message.ic.fallback",
                  commanderAddress);
        } else {
            return String.format(message, commanderAddress);
        }
    }

    /**
     * Creates the buttons for the dialog based on the origin of the offer.
     *
     * <p>If the offer is initiated by HR, the buttons include an "accept" button and a "decline" button that
     * displays the Ronin's callsign. If initiated by the Ronin, additional "decline" options are provided
     * based on politeness level (polite, neutral, or rude).</p>
     *
     * @param isFromHR   {@code true} if the offer originates from HR, {@code false} if from the Ronin
     * @param callsign   the callsign of the Ronin, used for buttons and tooltips
     * @return a {@link List} of {@link ButtonLabelTooltipPair} representing the created buttons
     */
    private static List<ButtonLabelTooltipPair> createButtons(boolean isFromHR, String callsign) {
        List<ButtonLabelTooltipPair> buttons = new ArrayList<>();

        final String KEY_FORWARD = "button.";
        final String KEY_CONTEXT = isFromHR ? "fromHR" : "fromRonin";
        final String KEY_ACCEPT = ".accept";
        final String KEY_DECLINE = ".decline.";

        ButtonLabelTooltipPair btnAccept = new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE,
              KEY_FORWARD + KEY_CONTEXT + KEY_ACCEPT), null);
        buttons.add(btnAccept);

        ButtonLabelTooltipPair btnDeclinePolite = new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE,
              KEY_FORWARD + KEY_CONTEXT + KEY_DECLINE + "polite"), null);
        buttons.add(btnDeclinePolite);

        ButtonLabelTooltipPair btnDeclineNeutral = new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE,
              KEY_FORWARD + KEY_CONTEXT + KEY_DECLINE + "neutral"), null);
        buttons.add(btnDeclineNeutral);

        ButtonLabelTooltipPair btnDeclineRude = new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE,
              KEY_FORWARD + KEY_CONTEXT + KEY_DECLINE + "rude", callsign), null);
        buttons.add(btnDeclineRude);

        return buttons;
    }

    /**
     * Creates the out-of-character report for the dialog.
     *
     * <p>The report includes detailed information about the Ronin, including their name, skills,
     * and abilities. This information is presented in a clear and formatted way for the player's
     * decision-making process.</p>
     *
     * @param ronin the {@link Person} representing the Ronin whose attributes are to be included in the report
     * @return the out-of-character report as a {@link String}
     */
    private static String createOutOfCharacterMessage(Person ronin) {
        StringBuilder report = new StringBuilder();

        // Header with Ronin's name and primary role
        report.append("<div style='text-align: center;'>")
              .append("<b>").append(ronin.getFullTitle()).append("</b>")
              .append(" (").append(ronin.getPrimaryRole()).append(')')
              .append("</div>");

        // Start table for skills and abilities
        report.append("<table style='width:75%;'>");

        // Left column: Skills
        report.append("<tr><td style='vertical-align:top; width:50%;'><b>")
              .append(getFormattedTextAt(RESOURCE_BUNDLE, "message.ooc.skills"))
              .append("</b><br>");
        for (Skill skill : ronin.getSkills().getSkills()) {
            report.append(skill.getType().getName()).append(": ").append(skill).append("<br>");
        }
        report.append("</td>");

        // Right column: Abilities
        report.append("<td style='vertical-align:top; width:50%;'><b>")
              .append(getFormattedTextAt(RESOURCE_BUNDLE, "message.ooc.abilities"))
              .append("</b><br>");
        boolean hasAbilities = false;
        for (Enumeration<IOption> i = ronin.getOptions(LVL3_ADVANTAGES); i.hasMoreElements();) {
            final IOption ability = i.nextElement();
            if (ability.booleanValue()) {
                report.append(Utilities.getOptionDisplayName(ability)).append("<br>");
                hasAbilities = true;
            }
        }
        if (!hasAbilities) {
            report.append(getFormattedTextAt(RESOURCE_BUNDLE, "message.ooc.noAbilities"));
        }
        report.append("</td></tr>");

        // End table
        report.append("</table>");

        return report.toString();
    }
}
