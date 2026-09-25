package mekhq.gui.utilities;

import static mekhq.campaign.personnel.turnoverAndRetention.Fatigue.getEffectiveFatigue;

import java.util.List;

import jakarta.annotation.Nullable;
import mekhq.MHQOptions;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.utilities.MHQInternationalization;

/**
 * Picks the row colors used to flag a person's state (absent, deployed, injured, fatigued, and so on) in personnel
 * tables, so every table highlights the same states in the same way.
 *
 * <p>The colors themselves are user-configurable through {@link MHQOptions}.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class PersonnelStateColors {
    /**
     * A person with at least this much effective fatigue is flagged as fatigued.
     */
    private static final int FATIGUE_HIGHLIGHT_THRESHOLD = 5;

    private PersonnelStateColors() {}

    /**
     * Determines the colors for a person's row, and collects the reasons they are highlighted.
     *
     * <p>A person may be in several highlighted states at once. The color comes from the most important state, while
     * every applicable state is added to {@code colorReasonKeys}.</p>
     *
     * @param campaign        the current campaign
     * @param person          the person to check
     * @param colorReasonKeys receives a resource key (in the default GUI bundle) for every state that applies
     *
     * @return the colors for the most important state, or {@code null} if no highlighted state applies
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static @Nullable ComponentColors getStateColors(Campaign campaign, Person person,
          List<String> colorReasonKeys) {
        // Set color based on priority (first match wins for display color)
        // But collect ALL applicable reasons for tooltip
        MHQOptions mhqOptions = MekHQ.getMHQOptions();
        CampaignOptions campaignOptions = campaign.getCampaignOptions();

        ComponentColors cellColors = null;
        if (person.getStatus().isAbsent()) {
            colorReasonKeys.add("colorReason.personnel.absent");
            cellColors = mhqOptions.getAbsentColors();
        }
        if (person.getStatus().isDepartedUnit()) {
            colorReasonKeys.add("colorReason.personnel.departed");
            cellColors = (cellColors == null) ? mhqOptions.getGoneColors() : cellColors;
        }
        if (person.isDeployed()) {
            colorReasonKeys.add("colorReason.personnel.deployed");
            cellColors = (cellColors == null) ? mhqOptions.getDeployedColors() : cellColors;
        }
        if (person.isQueuedForTravel(campaign.getCampaignLocationManager())) {
            colorReasonKeys.add("colorReason.personnel.queuedForTravel");
            cellColors = (cellColors == null) ? mhqOptions.getQueuedForTravelColors() : cellColors;
        }
        if (PersonnelStatus.computeIsAwayFromMainForce(campaign, person)) {
            colorReasonKeys.add("colorReason.personnel.awayFromMainForce");
            cellColors = (cellColors == null) ? mhqOptions.getAwayFromMainForceColors() : cellColors;
        }
        if (campaignOptions.isUseAdvancedMedical() ? person.hasInjuries(true) : (person.getHits() > 0)) {
            colorReasonKeys.add("colorReason.personnel.injured");
            cellColors = (cellColors == null) ? mhqOptions.getInjuredColors() : cellColors;
        }
        if (person.isPregnant()) {
            colorReasonKeys.add("colorReason.personnel.pregnant");
            cellColors = (cellColors == null) ? mhqOptions.getPregnantColors() : cellColors;
        }
        if (campaignOptions.get(CampaignOption.USE_FATIGUE) &&
                  (getEffectiveFatigue(person, campaign) >= FATIGUE_HIGHLIGHT_THRESHOLD)) {
            colorReasonKeys.add("colorReason.personnel.fatigued");
            cellColors = (cellColors == null) ? mhqOptions.getFatiguedColors() : cellColors;
        }
        boolean isUseAlternativeAdvancedMedical = campaignOptions.get(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL);
        if (person.hasNonProstheticPermanentInjuries(isUseAlternativeAdvancedMedical)) {
            colorReasonKeys.add("colorReason.personnel.healedInjuries");
            cellColors = (cellColors == null) ? mhqOptions.getHealedInjuriesColors() : cellColors;
        }
        return cellColors;
    }

    /**
     * Converts the reason keys collected by {@link #getStateColors(Campaign, Person, List)} into tooltip text, one
     * reason per line.
     *
     * @param colorReasonKeys the collected reason keys
     *
     * @return the reasons as HTML-ready text (without {@code <html>} tags), or an empty string if there are none
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static String getColorReasonsText(List<String> colorReasonKeys) {
        StringBuilder colorReasons = new StringBuilder();
        for (String key : colorReasonKeys) {
            if (!colorReasons.isEmpty()) {
                colorReasons.append("<br>");
            }
            colorReasons.append(MHQInternationalization.getText(key));
        }
        return colorReasons.toString();
    }
}
