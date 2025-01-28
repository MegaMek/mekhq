package mekhq.campaign.personnel.prisoners.records;

import mekhq.campaign.personnel.prisoners.enums.ResponseQuality;

import java.util.List;

/**
 * Represents an individual response entry for a prisoner event.
 * Each response entry defines a response quality and its associated effects.
 *
 * @param quality the quality of the response, as defined by the {@link ResponseQuality} enum
 * @param effects a list of effects resulting from the response quality, as defined by the {@link EventResult} record
 */
public record PrisonerResponseEntry(
    ResponseQuality quality,
    List<EventResult> effects
) { }
