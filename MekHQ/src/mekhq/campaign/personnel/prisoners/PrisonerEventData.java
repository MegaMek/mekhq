/*
 * Copyright (C) 2025 MegaMek team
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.personnel.prisoners;

import mekhq.campaign.personnel.prisoners.enums.PrisonerEvent;
import mekhq.campaign.personnel.prisoners.yaml.PrisonerResponseEntry;

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
