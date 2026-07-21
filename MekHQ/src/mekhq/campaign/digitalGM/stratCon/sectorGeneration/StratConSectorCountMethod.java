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
package mekhq.campaign.digitalGM.stratCon.sectorGeneration;

import static mekhq.utilities.MHQInternationalization.getTextAt;

/**
 * How many sectors a contract generates, and therefore how large each one is.
 *
 * <p>These are mutually exclusive ways of dividing a contract's required combat teams into sectors. Whichever is
 * chosen, every team is accounted for: the methods differ in how many sectors those teams are spread across, and a
 * sector's size follows directly from the teams assigned to it. Fewer sectors therefore means larger ones, and the
 * total mapped area stays roughly constant.</p>
 *
 * <p>This replaced a pair of independent checkboxes ({@code useStratConAlternateSectorCount} and
 * {@code useStratConCondenseSectors}) which could be set to combinations that contradicted each other - notably
 * condensing without the alternate count, where the promised ten-sector cap silently did not apply.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public enum StratConSectorCountMethod {
    /** One sector per three combat teams, plus a smaller one for any remainder. StratCon's historical behavior. */
    LEGACY("LEGACY"),
    /** Roughly one sector per nine combat teams: fewer, larger sectors for the same force. */
    ALTERNATE("ALTERNATE"),
    /** The alternate count, capped at ten sectors, with the surplus teams shared across the ten that remain. */
    CONDENSED("CONDENSED"),
    /** Exactly one sector, holding every combat team the contract requires. */
    SINGLE("SINGLE");

    private final String lookupName;
    private final String label;
    private final String tooltip;

    private static final String RESOURCE_BUNDLE = "mekhq.resources.StratConSectorCountMethod";

    StratConSectorCountMethod(String lookupName) {
        this.lookupName = lookupName;
        this.label = generateLabel();
        this.tooltip = generateTooltip();
    }

    public String getLookupName() {
        return lookupName;
    }

    public String getLabel() {
        return label;
    }

    public String getTooltip() {
        return tooltip;
    }

    /**
     * @return {@code true} when this method uses the recon-budget sector sizing rather than the legacy flat allowance.
     *       Only {@link #LEGACY} keeps the historical dimensions; every other method sizes sectors from the teams
     *       assigned to them, which is also what applies the 1024-hex area ceiling.
     */
    public boolean usesImprovedSizing() {
        return this != LEGACY;
    }

    private String generateLabel() {
        return getTextAt(RESOURCE_BUNDLE, "StratConSectorCountMethod." + lookupName + ".label");
    }

    private String generateTooltip() {
        return getTextAt(RESOURCE_BUNDLE, "StratConSectorCountMethod." + lookupName + ".tooltip");
    }

    public static StratConSectorCountMethod fromLookupName(String lookupName) {
        for (StratConSectorCountMethod method : StratConSectorCountMethod.values()) {
            if (method.lookupName.equals(lookupName)) {
                return method;
            }
        }
        return CONDENSED;
    }

    /**
     * Maps the pre-0.51.01 pair of booleans onto a method, for loading saves written before this option existed.
     *
     * <p>Condensing wins when it is set, because the ten-sector cap was the more visible of the two settings. That
     * folds away the one combination with no equivalent here - condensing without the alternate count, which capped a
     * legacy count at ten - and those campaigns land on {@link #CONDENSED}.</p>
     *
     * @param alternateCount  the old {@code useStratConAlternateSectorCount} value
     * @param condenseSectors the old {@code useStratConCondenseSectors} value
     *
     * @return the equivalent method
     */
    public static StratConSectorCountMethod fromLegacyOptions(boolean alternateCount, boolean condenseSectors) {
        if (condenseSectors) {
            return CONDENSED;
        }

        return alternateCount ? ALTERNATE : LEGACY;
    }

    @Override
    public String toString() {
        return getLabel();
    }
}
