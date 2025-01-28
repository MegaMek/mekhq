package mekhq.campaign.personnel.prisoners.records;

import megamek.common.annotations.Nullable;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.prisoners.enums.EventResultEffect;

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
    EventResultEffect effect,
    boolean isGuard,
    int magnitude,
    @Nullable SkillType skillType
) {}

