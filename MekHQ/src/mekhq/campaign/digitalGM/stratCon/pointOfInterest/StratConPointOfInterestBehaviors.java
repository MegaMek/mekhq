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
package mekhq.campaign.digitalGM.stratCon.pointOfInterest;

import java.util.HashMap;
import java.util.Map;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;

/**
 * The registry of {@link IStratConPointOfInterestBehavior}s, keyed by the behavior ID a definition names. Code outside
 * StratCon adds a new kind of point of interest by registering its behavior here and supplying a definition.
 *
 * <p>An unknown or missing behavior ID resolves to the {@link #DEFAULT_BEHAVIOR_ID default behavior}, whose hooks all
 * keep their interface defaults, so a point of interest whose behavior is missing stays inert rather than breaking.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConPointOfInterestBehaviors {
    private static final MMLogger LOGGER = MMLogger.create(StratConPointOfInterestBehaviors.class);

    public static final String DEFAULT_BEHAVIOR_ID = "default";

    private static final IStratConPointOfInterestBehavior DEFAULT_BEHAVIOR = new IStratConPointOfInterestBehavior() {
    };

    private static final Map<String, IStratConPointOfInterestBehavior> behaviors = new HashMap<>();

    static {
        behaviors.put(DEFAULT_BEHAVIOR_ID, DEFAULT_BEHAVIOR);
        behaviors.put(StratConDataCacheBehavior.BEHAVIOR_ID, new StratConDataCacheBehavior());
        behaviors.put(StratConVulnerableInfrastructureBehavior.BEHAVIOR_ID,
              new StratConVulnerableInfrastructureBehavior());
        behaviors.put(StratConPotentialLeadBehavior.BEHAVIOR_ID, new StratConPotentialLeadBehavior());
        behaviors.put(StratConAssassinationLeadBehavior.BEHAVIOR_ID, new StratConAssassinationLeadBehavior());
        behaviors.put(StratConLookoutPointBehavior.BEHAVIOR_ID, new StratConLookoutPointBehavior());
        behaviors.put(StratConBeleagueredForcesBehavior.BEHAVIOR_ID, new StratConBeleagueredForcesBehavior());
        behaviors.put(StratConTrainingManeuversBehavior.BEHAVIOR_ID, new StratConTrainingManeuversBehavior());
        behaviors.put(StratConHighProfileTargetBehavior.BEHAVIOR_ID, new StratConHighProfileTargetBehavior());
        behaviors.put(StratConVIPBehavior.BEHAVIOR_ID, new StratConVIPBehavior());
        behaviors.put(StratConStrategicPositionBehavior.BEHAVIOR_ID, new StratConStrategicPositionBehavior());
        behaviors.put(StratConScheduledParadeBehavior.BEHAVIOR_ID, new StratConScheduledParadeBehavior());
        behaviors.put(StratConCivilDisobedienceBehavior.BEHAVIOR_ID, new StratConCivilDisobedienceBehavior());
        behaviors.put(StratConSabotageTargetBehavior.BEHAVIOR_ID, new StratConSabotageTargetBehavior());
        behaviors.put(StratConCivilianInfrastructureBehavior.BEHAVIOR_ID,
              new StratConCivilianInfrastructureBehavior());
        behaviors.put(StratConSecurityReviewBehavior.BEHAVIOR_ID, new StratConSecurityReviewBehavior());
        behaviors.put(StratConPlunderTargetBehavior.BEHAVIOR_ID, new StratConPlunderTargetBehavior());
        behaviors.put(StratConTargetIntelligenceBehavior.BEHAVIOR_ID, new StratConTargetIntelligenceBehavior());
        behaviors.put(StratConShowOfForceBehavior.BEHAVIOR_ID, new StratConShowOfForceBehavior());
    }

    private StratConPointOfInterestBehaviors() {
    }

    /**
     * Registers a behavior under the given ID, replacing any behavior already registered under it. The default
     * behavior cannot be replaced.
     *
     * @param behaviorId the ID definitions use to name this behavior
     * @param behavior   the behavior
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void registerBehavior(String behaviorId, IStratConPointOfInterestBehavior behavior) {
        if ((behaviorId == null) || behaviorId.isBlank() || (behavior == null)) {
            LOGGER.warn("Ignoring a point of interest behavior registered without an ID or implementation.");
            return;
        }

        if (DEFAULT_BEHAVIOR_ID.equals(behaviorId)) {
            LOGGER.warn("The default point of interest behavior cannot be replaced.");
            return;
        }

        behaviors.put(behaviorId, behavior);
    }

    /**
     * Removes a registered behavior. Points of interest naming it fall back to the default behavior.
     *
     * @param behaviorId the ID of the behavior to remove
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void unregisterBehavior(String behaviorId) {
        if (!DEFAULT_BEHAVIOR_ID.equals(behaviorId)) {
            behaviors.remove(behaviorId);
        }
    }

    /**
     * @param behaviorId the behavior ID to look up
     *
     * @return the behavior registered under that ID, or the default behavior if there is none
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static IStratConPointOfInterestBehavior getBehavior(@Nullable String behaviorId) {
        if (behaviorId == null) {
            return DEFAULT_BEHAVIOR;
        }

        return behaviors.getOrDefault(behaviorId, DEFAULT_BEHAVIOR);
    }
}
