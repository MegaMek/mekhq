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
package mekhq.campaign.universe.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;

/**
 * Administrative capital status of a planetary system within its controlling faction, as recorded per-era in the
 * planetary system data ({@code capitalType} event field).
 *
 * <p>The values form a significance ranking - a national capital outranks a regional (province) capital, which
 * outranks a district capital. {@link #significance()} exposes that ranking so a system that contains several planets
 * can report the most important capital status among them.</p>
 *
 * <p>The stored labels ({@code "National Capital"}, {@code "Region Capital"}, {@code "District Capital"}) are the exact
 * strings authored in the data files; {@link #fromLabel(String)} maps them (and the bare enum names) back to constants
 * during deserialization.</p>
 */
public enum CapitalType {
    NONE("", 0),
    DISTRICT("District Capital", 1),
    REGION("Region Capital", 2),
    NATIONAL("National Capital", 3);

    private static final MMLogger LOGGER = MMLogger.create(CapitalType.class);

    private final String label;
    private final int significance;

    CapitalType(String label, int significance) {
        this.label = label;
        this.significance = significance;
    }

    /**
     * @return the exact label used in the planetary system data files, or an empty string for {@link #NONE}
     */
    @JsonValue
    public String getLabel() {
        return label;
    }

    /**
     * @return the significance rank of this capital status: higher is more important ({@link #NATIONAL} = 3,
     *       {@link #REGION} = 2, {@link #DISTRICT} = 1, {@link #NONE} = 0). Used to pick the most important capital
     *       status among the planets of a system.
     */
    public int significance() {
        return significance;
    }

    /**
     * @return {@code true} if this system carries no capital status
     */
    public boolean isNone() {
        return this == NONE;
    }

    /**
     * Resolves a data label (or bare enum name) to a {@link CapitalType}. Leading/trailing whitespace and letter case
     * are ignored. A {@code null}, blank, or unrecognized value resolves to {@link #NONE} so that a stray value in the
     * data cannot abort planet loading; unrecognized non-blank values are logged.
     *
     * @param value the label from the data, e.g. {@code "Region Capital"}, or {@code null}
     *
     * @return the matching {@link CapitalType}, or {@link #NONE} when there is no match
     */
    @JsonCreator
    public static CapitalType fromLabel(@Nullable String value) {
        if ((value == null) || value.isBlank()) {
            return NONE;
        }
        String normalized = value.trim();
        for (CapitalType capitalType : values()) {
            if (normalized.equalsIgnoreCase(capitalType.label) || normalized.equalsIgnoreCase(capitalType.name())) {
                return capitalType;
            }
        }
        LOGGER.warn("Unrecognized capitalType value '{}' in planetary system data; treating as NONE.", value);
        return NONE;
    }
}
