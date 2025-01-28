package mekhq.campaign.personnel.prisoners.records;

import mekhq.campaign.personnel.prisoners.enums.PrisonerEvent;

import java.util.List;

/**
 * Represents data relevant to a random prisoner event, including its type,
 * severity, and response map structure.
 *
 * @param prisonerEvent The type of prisoner event as a {@link PrisonerEvent}.
 *                      This represents the name of the event.
 * @param responseMap   A list of {@link PrisonerResponseEntry} defining the responses
 *                      and their associated qualities and effects.
 */
public record PrisonerEventData(
    PrisonerEvent prisonerEvent,
    List<PrisonerResponseEntry> responseMap
) { }
