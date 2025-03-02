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

import mekhq.campaign.randomEvents.prisoners.enums.PrisonerEvent;

import java.util.List;

/**
 * Represents data relevant to a random prisoner event, including its type,
 * severity, and response map structure.
 *
 * @param prisonerEvent The type of prisoner event as a {@link PrisonerEvent}.
 *                      This represents the name of the event.
 * @param responseEntries   A list of {@link PrisonerResponseEntry} defining the responses
 *                      and their associated qualities and effects.
 */
public record PrisonerEventData(
    PrisonerEvent prisonerEvent,
    List<PrisonerResponseEntry> responseEntries
) { }
