/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.randomEvents.prisoners.records;

import java.util.List;

import mekhq.campaign.randomEvents.prisoners.enums.ResponseQuality;

/**
 * Represents an individual response entry for a prisoner event. Each response entry defines a response quality and its
 * associated effects.
 *
 * @param quality        the quality of the response, as defined by the {@link ResponseQuality} enum
 * @param effectsSuccess a list of effects resulting from successful resolution of the event, as defined by the
 *                       {@link EventResult} record
 * @param effectsFailure a list of effects resulting from failing to resolve the event, as defined by the
 *                       {@link EventResult} record
 */
public record PrisonerResponseEntry(
      ResponseQuality quality,
      List<EventResult> effectsSuccess,
      List<EventResult> effectsFailure
) {}
