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
package mekhq.campaign.personnel.prisoners.yaml;

import mekhq.campaign.personnel.prisoners.enums.EventResultEffect;
import mekhq.campaign.personnel.prisoners.enums.ResponseQuality;

import java.util.List;

/**
 * Represents an individual response entry for a prisoner event.
 * Each response entry defines a response quality and its associated effects.
 *
 * @param quality the quality of the response, as defined by the {@link ResponseQuality} enum
 * @param effects a list of effects resulting from the response quality, as defined by the {@link EventResultEffect} enum
 */
public record PrisonerResponseEntry(
    ResponseQuality quality,
    List<EventResultEffect> effects
) { }
