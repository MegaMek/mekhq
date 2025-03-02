/*
 * Copyright (C) 2025 The MegaMek Team
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community. BattleMech,
 * BattleTech, and MechWarrior are trademarks of The Topps Company, Inc.
 * The MegaMek organization is not affiliated with The Topps Company, Inc.
 * or Catalyst Game Labs.
 */
package mekhq.campaign.randomEvents.prisoners.records;

import mekhq.campaign.randomEvents.prisoners.enums.ResponseQuality;

import java.util.List;

/**
 * Represents an individual response entry for a prisoner event.
 * Each response entry defines a response quality and its associated effects.
 *
 * @param quality the quality of the response, as defined by the {@link ResponseQuality} enum
 * @param effectsSuccess a list of effects resulting from successful resolution of the event, as
 *                      defined by the {@link EventResult} record
 * @param effectsFailure a list of effects resulting from failing to resolve the event, as defined
 *                      by the {@link EventResult} record
 */
public record PrisonerResponseEntry(
    ResponseQuality quality,
    List<EventResult> effectsSuccess,
    List<EventResult> effectsFailure
) { }
