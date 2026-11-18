/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.digitalGM.stratCon;

import java.time.LocalDate;

import jakarta.annotation.Nullable;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState.LocalDateAdapter;

/**
 * A normal scenario the week's roll has called for (see {@link StratConScenarioTempo#rollWeeklyScenarios}): the day it
 * appears, and the sector whose threat it was rolled against, where it appears.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConScheduledScenario {
    /** A sector index meaning no sector in particular: the scenario appears in a random one. */
    public static final int ANY_SECTOR = -1;

    private LocalDate date;
    private int sectorIndex = ANY_SECTOR;

    /** Used when loading saved campaigns. */
    public StratConScheduledScenario() {
    }

    /**
     * @param date        the day the scenario appears
     * @param sectorIndex the index of the sector it appears in, among the contract's sectors; {@link #ANY_SECTOR} for
     *                    a random one
     *
     * @author Illiani
     * @since 0.51.01
     */
    public StratConScheduledScenario(LocalDate date, int sectorIndex) {
        this.date = date;
        this.sectorIndex = sectorIndex;
    }

    /**
     * @return the day the scenario appears
     */
    @XmlJavaTypeAdapter(value = LocalDateAdapter.class)
    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    /**
     * @return the index of the sector the scenario appears in, among the contract's sectors; {@link #ANY_SECTOR} for a
     *       random one
     */
    public int getSectorIndex() {
        return sectorIndex;
    }

    public void setSectorIndex(int sectorIndex) {
        this.sectorIndex = sectorIndex;
    }

    /**
     * @param campaignState the contract's StratCon campaign state
     *
     * @return the sector the scenario appears in, or {@code null} for a random one - including when the sector it was
     *       rolled for no longer exists
     *
     * @author Illiani
     * @since 0.51.01
     */
    public @Nullable StratConTrackState getSector(StratConCampaignState campaignState) {
        if ((sectorIndex < 0) || (sectorIndex >= campaignState.getTrackCount())) {
            return null;
        }
        return campaignState.getTrack(sectorIndex);
    }

    @Override
    public String toString() {
        return "StratConScheduledScenario[" + date + ", sector " + sectorIndex + ']';
    }
}
