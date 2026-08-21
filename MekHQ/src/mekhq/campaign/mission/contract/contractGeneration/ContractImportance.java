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
package mekhq.campaign.mission.contract.contractGeneration;

import static java.lang.Math.clamp;

/**
 * How big a deal a Chaos contract is, synthesized from the three things that make an engagement matter: who is hiring
 * (the employer's station in the galaxy), what they are hiring for (the objective's strategic scope), and what is being
 * fought over (the target world's strategic value).
 *
 * <p>Each of those contributes points to a raw importance score, which is then bucketed into one of these four tiers.
 * The tier is the single galaxy-grounded dial that {@link ChaosEmployerForceRating} reads when deciding how good a
 * force an employer or enemy commits &mdash; so the objective and the target world feed into force quality exactly
 * once, through here, rather than being counted again separately.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public enum ContractImportance {
    MINOR(-1),
    STANDARD(0),
    STRATEGIC(1),
    CRITICAL(2);

    private final int forceQualityModifier;

    ContractImportance(final int forceQualityModifier) {
        this.forceQualityModifier = forceQualityModifier;
    }

    /**
     * @return the force-quality delta this importance tier contributes when rating an employer's or enemy's committed
     *       force
     */
    public int getForceQualityModifier() {
        return forceQualityModifier;
    }

    /**
     * Synthesizes the importance of a contract from its employer, objective, and target-world strategic value.
     *
     * @param employerType         who is hiring
     * @param objectiveType        what they are hiring for
     * @param planetStrategicValue the target world's strategic value, as produced by
     *                             {@link ChaosPlanetStrategicValue#calculate}
     *
     * @return the resulting importance tier
     */
    public static ContractImportance from(final ChaosEmployerType employerType, final ChaosObjectiveType objectiveType,
          final int planetStrategicValue) {
        int score = employerTierPoints(employerType)
                          + objectivePoints(objectiveType)
                          + planetValuePoints(planetStrategicValue);
        return fromScore(score);
    }

    /**
     * The employer's station in the galaxy: a whole system's owner commits far more than a single planetary business or
     * militia band. Ranges {@code 0..3}.
     */
    static int employerTierPoints(final ChaosEmployerType employerType) {
        return switch (employerType) {
            case ANY_SYSTEM_OWNER, LOCAL_SYSTEM_OWNER -> 3;
            case ANY_PLANETARY_GOVERNMENT, LOCAL_PLANETARY_GOVERNMENT, NOBLE -> 2;
            case CORPORATION, MERCENARY_SUBCONTRACT -> 1;
            case CIVILIAN_ORGANIZATION_BUSINESS, CIVILIAN_ORGANIZATION_MILITIA, CIVILIAN_ORGANIZATION_REBELS -> 0;
        };
    }

    /**
     * The objective's strategic scope, taken from {@link ChaosObjectiveType#getForceCommitmentModifier()} (range
     * {@code -2..3}) and shifted into a non-negative {@code 0..5} contribution.
     */
    static int objectivePoints(final ChaosObjectiveType objectiveType) {
        return clamp(objectiveType.getForceCommitmentModifier() + 2, 0, 5);
    }

    /**
     * The target world's strategic worth, bucketed from the
     * {@code 0..}{@link ChaosPlanetStrategicValue#MAX_STRATEGIC_VALUE} strategic-value score into a {@code 0..5}
     * contribution.
     */
    static int planetValuePoints(final int planetStrategicValue) {
        if (planetStrategicValue <= 3) {
            return 0;
        } else if (planetStrategicValue <= 7) {
            return 1;
        } else if (planetStrategicValue <= 11) {
            return 2;
        } else if (planetStrategicValue <= 15) {
            return 3;
        } else if (planetStrategicValue <= 19) {
            return 4;
        } else {
            return 5;
        }
    }

    /**
     * Buckets a raw importance score (range {@code 0..13}) into a tier.
     */
    static ContractImportance fromScore(final int score) {
        if (score <= 3) {
            return MINOR;
        } else if (score <= 7) {
            return STANDARD;
        } else if (score <= 10) {
            return STRATEGIC;
        } else {
            return CRITICAL;
        }
    }
}
