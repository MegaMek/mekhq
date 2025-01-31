package mekhq.campaign.randomEvents.prisoners.records;

import com.fasterxml.jackson.annotation.JsonProperty;
import mekhq.campaign.randomEvents.prisoners.enums.EventResultEffect;

import static mekhq.campaign.randomEvents.prisoners.enums.EventResultEffect.NONE;

/**
 * Represents the result of an event response, including the effect type,
 * the guard flag, the magnitude, and an optional skill type.
 *
 * @param effect The type of effect this result describes
 * @param isGuard Whether this result applies to a guard
 * @param magnitude The intensity or magnitude of the effect
 * @param skillType An optional skill type associated with the effect
 */
public record EventResult(
    @JsonProperty(value = "effect") EventResultEffect effect,
    @JsonProperty(value = "isGuard") boolean isGuard,
    @JsonProperty(value = "magnitude") int magnitude,
    @JsonProperty(value = "skillType") String skillType
) {
    // Additional logic to provide defaults for missing properties
    public EventResult {
        effect = (effect != null) ? effect : NONE;
        skillType = (skillType != null) ? skillType : "";
    }
}

