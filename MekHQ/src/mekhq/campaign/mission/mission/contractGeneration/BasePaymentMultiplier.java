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
package mekhq.campaign.mission.mission.contractGeneration;

import static mekhq.utilities.MHQInternationalization.getTextAt;

import megamek.logging.MMLogger;
import org.jspecify.annotations.NonNull;

public enum BasePaymentMultiplier {
    NORMAL("NORMAL", 0.05),
    SPENDTHRIFT("SPENDTHRIFT", 0.025),
    MISERLY("MISERLY", 0.01);

    private final String lookupName;
    private final String label;
    private final double multiplier;

    private final String RESOURCE_BUNDLE = "mekhq.resources.BasePaymentMultiplier";
    private final static MMLogger LOGGER = MMLogger.create(BasePaymentMultiplier.class);

    BasePaymentMultiplier(final String lookupName, final double multiplier) {
        this.lookupName = lookupName;
        this.label = generateLabel(lookupName);
        this.multiplier = multiplier;
    }

    private @NonNull String generateLabel(String lookupName) {
        return getTextAt(RESOURCE_BUNDLE, "BasePaymentMultiplier." + lookupName + ".name");
    }

    public String getLabel() {
        return label;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public static BasePaymentMultiplier fromString(String text) {
        try {
            return BasePaymentMultiplier.valueOf(text.toUpperCase().replace(" ", "_"));
        } catch (Exception ignored) {}

        for (BasePaymentMultiplier value : values()) {
            if (value.lookupName.equals(text)) {
                return value;
            }
        }

        try {
            return BasePaymentMultiplier.values()[Integer.parseInt(text)];
        } catch (Exception ignored) {}

        LOGGER.error("Unknown BasePaymentMultiplier ordinal: {} - returning {}.", text, NORMAL.lookupName);

        return NORMAL;
    }

    @Override
    public String toString() {
        return getLabel();
    }
}
