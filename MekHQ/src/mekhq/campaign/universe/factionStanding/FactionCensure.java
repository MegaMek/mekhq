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
package mekhq.campaign.universe.factionStanding;

import static mekhq.campaign.universe.factionStanding.FactionCensureLevel.MIN_CENSURE_SEVERITY;
import static mekhq.campaign.universe.factionStanding.FactionStandingLevel.STANDING_LEVEL_2;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;

public class FactionCensure {
    private static final MMLogger LOGGER = MMLogger.create(FactionCensure.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionCensure";

    static final int THRESHOLD_FOR_CENSURE = STANDING_LEVEL_2.getStandingLevel();

    private final Map<String, CensureEntry> factionCensures = new HashMap<>();

    public FactionCensure() {
    }

    public void resetAllFactionCensures() {
        factionCensures.clear();
    }

    public void resetFactionCensure(final String factionCode) {
        factionCensures.remove(factionCode);
    }

    public Map<String, CensureEntry> getAllFactionCensures() {
        return factionCensures;
    }

    public FactionCensureLevel getCensureForFaction(final String factionCode) {
        CensureEntry censure = factionCensures.get(factionCode);
        return censure == null ? FactionCensureLevel.NONE : censure.level();
    }

    public void setCensureForFaction(final String factionCode, final FactionCensureLevel censureLevel,
          final LocalDate today) {
        CensureEntry censureEntry = new CensureEntry(censureLevel, today);
        factionCensures.put(factionCode, censureEntry);
    }

    public boolean canBeCensured(final String factionCode, final LocalDate today) {
        CensureEntry censureEntry = factionCensures.get(factionCode);
        if (censureEntry == null) {
            return true;
        }

        return censureEntry.canEscalate();
    }

    public void processCensureDegradation(final LocalDate today) {
        for (Map.Entry<String, CensureEntry> entry : factionCensures.entrySet()) {
            String factionCode = entry.getKey();
            CensureEntry censureEntry = entry.getValue();

            if (censureEntry.hasExpired(today)) {
                decreaseCensureForFaction(factionCode, today);
            }
        }
    }

    public void decreaseCensureForFaction(final String factionCode, final LocalDate today) {
        CensureEntry censureEntry = factionCensures.get(factionCode);

        if (censureEntry == null) {
            return;
        }

        FactionCensureLevel currentCensureLevel = censureEntry.level();
        int currentSeverity = currentCensureLevel.getSeverity();

        if (currentSeverity > MIN_CENSURE_SEVERITY) {
            currentSeverity--;
            FactionCensureLevel newCensureLevel = FactionCensureLevel.getCensureLevelFromSeverity(currentSeverity);

            setCensureForFaction(factionCode, newCensureLevel, today);
        }
    }

    public @Nullable FactionCensureLevel increaseCensureForFaction(final String factionCode, final LocalDate today) {
        CensureEntry censureEntry = factionCensures.get(factionCode);

        if (censureEntry == null) {
            setCensureForFaction(factionCode, FactionCensureLevel.FINE, today);
            return FactionCensureLevel.FINE;
        }

        if (!censureEntry.canEscalate()) {
            return null;
        }

        FactionCensureLevel currentCensureLevel = censureEntry.level();
        int currentSeverity = currentCensureLevel.getSeverity();
        currentSeverity++;
        FactionCensureLevel newCensureLevel = FactionCensureLevel.getCensureLevelFromSeverity(currentSeverity);

        setCensureForFaction(factionCode, newCensureLevel, today);
        return newCensureLevel;
    }

    public static void triggerCensureDialog(final String factionCode, final FactionCensureLevel level) {

    }
}
