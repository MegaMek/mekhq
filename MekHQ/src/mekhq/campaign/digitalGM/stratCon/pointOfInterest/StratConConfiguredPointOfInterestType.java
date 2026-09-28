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

import megamek.common.annotations.Nullable;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConContestedPointOfInterestBehavior.NoScenarioOutcome;

/**
 * The point of interest types whose rules are entirely those of a shared family - contested (see
 * {@link StratConContestedPointOfInterestBehavior}) or ambush (see {@link StratConAmbushPointOfInterestBehavior}) -
 * set up only by which scenario template is fought there and, for a contested type, what becomes of it when no scenario
 * breaks out. Each has no class of its own: its behavior is an instance of its family, registered under its behavior ID
 * (see {@link StratConPointOfInterestBehaviors}).
 *
 * <p>A type with rules beyond its family's has a class of its own instead, extending that family.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public enum StratConConfiguredPointOfInterestType {
    /** Recovered by Espionage contracts: secured unless contested, and then won or lost in a Recon Evasion. */
    DATA_CACHE("DataCache", "dataCache", Family.CONTESTED, "Recon Evasion.json", NoScenarioOutcome.SECURE),
    /** Extracted on Extraction Raids: secured unless contested, and then broken out of in a Breakout. */
    VIP("VIP", "vip", Family.CONTESTED, "Breakout.json", NoScenarioOutcome.SECURE),
    /** Carried out on Security Duty: completed unless enemy probes turn up, and then fought off in an Engagement. */
    SECURITY_REVIEW("SecurityReview", "securityReview", Family.CONTESTED, "Engagement.json",
          NoScenarioOutcome.SECURE),
    /** Captured on Planetary Assaults: taken unless held in strength, and then won or lost in a Pivotal Engagement. */
    STRATEGIC_POSITION("StrategicPosition", "strategicPosition", Family.CONTESTED, "Pivotal Engagement.json",
          NoScenarioOutcome.SECURE),
    /** Relieved on Relief Duty: reached unless surrounded, and then relieved or overrun in a Relief Column. */
    BELEAGUERED_FORCES("BeleagueredForces", "beleagueredForces", Family.CONTESTED, "Relief Column.json",
          NoScenarioOutcome.SECURE),
    /** Followed up on Mole Hunting contracts: a dud unless it pans out, and then fought over in a Mole Hunt. */
    POTENTIAL_LEAD("PotentialLead", "potentialLead", Family.CONTESTED, "Mole Hunt.json", NoScenarioOutcome.WITHDRAW),
    /** Used on Observation Raids: observed from unless watched, and then an ambush. */
    LOOKOUT_POINT("LookoutPoint", "lookoutPoint", Family.AMBUSH, null, null),
    /** Destroyed on Guerrilla Warfare contracts: destroyed unless a trap, and then an ambush. */
    VULNERABLE_INFRASTRUCTURE("VulnerableInfrastructure", "vulnerableInfrastructure", Family.AMBUSH, null, null);

    /** The shared rules a configured type follows. */
    private enum Family {
        CONTESTED,
        AMBUSH
    }

    private final String typeId;
    private final String behaviorId;
    private final Family family;
    // null for an ambush suited to the deploying formation's unit type
    private final String scenarioTemplateName;
    // null for an ambush type, which is always secured when no scenario breaks out
    private final NoScenarioOutcome noScenarioOutcome;

    StratConConfiguredPointOfInterestType(String typeId, String behaviorId, Family family,
          @Nullable String scenarioTemplateName, @Nullable NoScenarioOutcome noScenarioOutcome) {
        this.typeId = typeId;
        this.behaviorId = behaviorId;
        this.family = family;
        this.scenarioTemplateName = scenarioTemplateName;
        this.noScenarioOutcome = noScenarioOutcome;
    }

    /**
     * @return the type ID of this type's definition, as its data file names it
     *
     * @author Illiani
     * @since 0.51.01
     */
    public String getTypeId() {
        return typeId;
    }

    /**
     * @return the ID this type's behavior is registered under, as its definition names it
     *
     * @author Illiani
     * @since 0.51.01
     */
    public String getBehaviorId() {
        return behaviorId;
    }

    /**
     * @return the file name of the scenario template fought here, or {@code null} for an ambush suited to the
     *       deploying formation's unit type
     *
     * @author Illiani
     * @since 0.51.01
     */
    public @Nullable String getScenarioTemplateName() {
        return scenarioTemplateName;
    }

    /**
     * @return a new behavior following this type's rules
     *
     * @author Illiani
     * @since 0.51.01
     */
    AbstractStratConRolledPointOfInterestBehavior createBehavior() {
        return switch (family) {
            case CONTESTED -> new StratConContestedPointOfInterestBehavior(behaviorId,
                  scenarioTemplateName,
                  noScenarioOutcome);
            case AMBUSH -> new StratConAmbushPointOfInterestBehavior(behaviorId, scenarioTemplateName);
        };
    }
}
