package mekhq.campaign.rating.CamOpsRatingV2;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.randomEvents.personalities.Aggression;
import mekhq.campaign.personnel.enums.randomEvents.personalities.Ambition;
import mekhq.campaign.personnel.enums.randomEvents.personalities.Greed;
import mekhq.campaign.personnel.enums.randomEvents.personalities.Social;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class CommandRating {
    private static final MMLogger logger = MMLogger.create(CommandRating.class);

    /**
     * Calculates the rating of a commander based on their skills and personality.
     *
     * @param campaign   the campaign the commander belongs to
     * @param commander  the commander to calculate the rating for
     * @return a map containing the commander's rating in different areas:
     *         - "leadership": the commander's leadership skill value
     *         - "tactics": the commander's tactics skill value
     *         - "strategy": the commander's strategy skill value
     *         - "negotiation": the commander's negotiation skill value
     *         - "traits": the commander's traits (not currently tracked, always 0)
     *         - "personality": the value of the commander's personality characteristics (or 0, if disabled)
     */
    protected static Map<String, Integer> calculateCommanderRating(Campaign campaign, Person commander) {
        Map<String, Integer> commanderRating = new HashMap<>();

        commanderRating.put("leadership", getSkillValue(commander, SkillType.S_LEADER));
        commanderRating.put("tactics", getSkillValue(commander, SkillType.S_TACTICS));
        commanderRating.put("strategy", getSkillValue(commander, SkillType.S_STRATEGY));
        commanderRating.put("negotiation", getSkillValue(commander, SkillType.S_NEG));

        // ATOW traits are not currently tracked by mhq, but when they are, this is where we'd add that data
        commanderRating.put("traits", 0);

        // this will return 0 if personalities are disabled
        commanderRating.put("personality", getPersonalityValue(campaign, commander));

        commanderRating.put("total", commanderRating.values().stream().mapToInt(rating -> rating).sum());

        logger.info(commanderRating.keySet().stream()
                .map(key -> "Command Rating = " + key + ": " + commanderRating.get(key) + '\n')
                .collect(Collectors.joining()));

        return commanderRating;
    }

    /**
     * @return the final skill value for the given skill,
     * or 0 if the person does not have the skill
     *
     * @param person the person
     * @param skill the skill
     */
    private static int getSkillValue(Person person, String skill) {
        if (person == null) {
            return 0;
        }

        if (person.hasSkill(skill)) {
            return person.getSkill(skill).getExperienceLevel();
        } else {
            return 0;
        }
    }

    /**
     * Calculates the total value of a person's personality characteristics.
     *
     * @param campaign the current campaign
     * @param person the person to calculate the personality value for
     * @return the total personality value of the person in the campaign
     */
    private static int getPersonalityValue(Campaign campaign, Person person) {
        if (person == null) {
            return 0;
        }

        if (campaign.getCampaignOptions().isUseRandomPersonalities()) {
            int personalityValue = 0;
            int modifier;

            Aggression aggression = person.getAggression();
            if (!person.getAggression().isNone()) {
                modifier = aggression.isTraitPositive() ? 1 : -1;
                personalityValue += aggression.isTraitMajor() ? modifier * 2 : modifier;
            }

            Ambition ambition = person.getAmbition();
            if (!person.getAmbition().isNone()) {
                modifier = ambition.isTraitPositive() ? 1 : -1;
                personalityValue += ambition.isTraitMajor() ? modifier * 2 : modifier;
            }

            Greed greed = person.getGreed();
            if (!person.getGreed().isNone()) {
                modifier = greed.isTraitPositive() ? 1 : -1;
                personalityValue += greed.isTraitMajor() ? modifier * 2 : modifier;
            }

            Social social = person.getSocial();
            if (!person.getSocial().isNone()) {
                modifier = social.isTraitPositive() ? 1 : -1;
                personalityValue += social.isTraitMajor() ? modifier * 2 : modifier;
            }

            return personalityValue;
        } else {
            return 0;
        }
    }
}
