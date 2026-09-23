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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import mekhq.campaign.digitalGM.stratCon.StratConContractMechanics.EscalationMode;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.IStratConPointOfInterestBehavior;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConBeleagueredForcesBehavior;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConDataCacheBehavior;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterestBehaviors;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConShowOfForceBehavior;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests the one place that says how each contract type's StratCon mechanics differ.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConContractMechanicsTest {
    @Test
    void aMissingObjectiveTypeHasNoMechanics() {
        StratConContractMechanics mechanics = StratConContractMechanics.forObjectiveType(null);

        assertFalse(mechanics.hasSpecialPointOfInterest());
        assertFalse(mechanics.isReplacingEssentialScenarios());
        assertEquals(EscalationMode.NONE, mechanics.escalationMode());
        assertFalse(mechanics.isUsingReconnaissance());
        assertFalse(mechanics.isRiotAffectingMorale());
    }

    @Test
    void anUndefinedObjectiveTypeHasNoMechanics() {
        StratConContractMechanics mechanics = StratConContractMechanics.forObjectiveType(
              ContractObjectiveType.UNDEFINED);

        assertFalse(mechanics.hasSpecialPointOfInterest());
        assertEquals(EscalationMode.NONE, mechanics.escalationMode());
    }

    @ParameterizedTest
    @EnumSource(value = ContractObjectiveType.class, names = { "UNDEFINED", "RECON_RAID" },
          mode = EnumSource.Mode.EXCLUDE)
    void everyOtherContractTypeHasASpecialPointOfInterestWithARegisteredBehavior(
          ContractObjectiveType objectiveType) {
        StratConContractMechanics mechanics = StratConContractMechanics.forObjectiveType(objectiveType);

        assertTrue(mechanics.hasSpecialPointOfInterest(), objectiveType + " should have a special point of interest");
        assertNotNull(mechanics.pointOfInterestBehaviorId());

        IStratConPointOfInterestBehavior behavior =
              StratConPointOfInterestBehaviors.getBehavior(mechanics.pointOfInterestBehaviorId());
        assertNotSame(StratConPointOfInterestBehaviors.getBehavior(null), behavior,
              objectiveType + " names a behavior that is not registered");
    }

    @Test
    void noTwoContractTypesShareASpecialPointOfInterest() {
        Set<String> typeIds = new HashSet<>();
        for (ContractObjectiveType objectiveType : ContractObjectiveType.values()) {
            String typeId = StratConContractMechanics.forObjectiveType(objectiveType).pointOfInterestTypeId();
            if (typeId != null) {
                assertTrue(typeIds.add(typeId), typeId + " is used by more than one contract type");
            }
        }
    }

    @Test
    void aSpecialPointOfInterestLeadsBackToItsContractType() {
        StratConContractMechanics espionage = StratConContractMechanics.forObjectiveType(
              ContractObjectiveType.ESPIONAGE);

        assertSame(espionage, StratConContractMechanics.forPointOfInterestTypeId(StratConDataCacheBehavior.TYPE_ID));
        assertNull(StratConContractMechanics.forPointOfInterestTypeId("NotASpecialPointOfInterest"));
        assertNull(StratConContractMechanics.forPointOfInterestTypeId(null));
    }

    @Test
    void contractsThatKeepTheirEssentialScenarios() {
        for (ContractObjectiveType objectiveType : new ContractObjectiveType[] {
              ContractObjectiveType.RELIEF_DUTY, ContractObjectiveType.CADRE_DUTY,
              ContractObjectiveType.PLANETARY_ASSAULT, ContractObjectiveType.SECURITY_DUTY,
              ContractObjectiveType.GARRISON_DUTY, ContractObjectiveType.RECON_RAID }) {
            assertFalse(StratConContractMechanics.forObjectiveType(objectiveType).isReplacingEssentialScenarios(),
                  objectiveType + " keeps its Essential scenarios");
        }

        assertTrue(StratConContractMechanics.forObjectiveType(ContractObjectiveType.DIVERSIONARY_RAID)
                         .isReplacingEssentialScenarios());
        assertTrue(StratConContractMechanics.forObjectiveType(ContractObjectiveType.OBJECTIVE_RAID)
                         .isReplacingEssentialScenarios());
        assertTrue(StratConContractMechanics.forObjectiveType(ContractObjectiveType.PIRATE_HUNTING)
                         .isReplacingEssentialScenarios());
    }

    @Test
    void specialPointsOfInterestThatAreNotObjectives() {
        for (ContractObjectiveType objectiveType : new ContractObjectiveType[] {
              ContractObjectiveType.DIVERSIONARY_RAID, ContractObjectiveType.OBJECTIVE_RAID,
              ContractObjectiveType.GARRISON_DUTY, ContractObjectiveType.PIRATE_HUNTING }) {
            assertFalse(StratConContractMechanics.forObjectiveType(objectiveType).isPointOfInterestObjective(),
                  objectiveType + "'s special points of interest are not objectives");
        }

        assertTrue(StratConContractMechanics.forObjectiveType(ContractObjectiveType.RELIEF_DUTY)
                         .isPointOfInterestObjective());
        assertEquals(StratConBeleagueredForcesBehavior.TYPE_ID,
              StratConContractMechanics.forObjectiveType(ContractObjectiveType.RELIEF_DUTY).pointOfInterestTypeId());
        assertEquals(StratConShowOfForceBehavior.TYPE_ID,
              StratConContractMechanics.forObjectiveType(ContractObjectiveType.GARRISON_DUTY).pointOfInterestTypeId());
    }

    @Test
    void escalationModes() {
        assertEquals(EscalationMode.DEESCALATING,
              StratConContractMechanics.forObjectiveType(ContractObjectiveType.GARRISON_DUTY).escalationMode());
        assertEquals(EscalationMode.ESCALATING,
              StratConContractMechanics.forObjectiveType(ContractObjectiveType.SABOTAGE).escalationMode());
        assertEquals(EscalationMode.ESCALATING,
              StratConContractMechanics.forObjectiveType(ContractObjectiveType.TERRORISM).escalationMode());
        assertEquals(EscalationMode.ESCALATING,
              StratConContractMechanics.forObjectiveType(ContractObjectiveType.PIRATE_RAID).escalationMode());
        assertEquals(EscalationMode.ESCALATING,
              StratConContractMechanics.forObjectiveType(ContractObjectiveType.DIVERSIONARY_RAID).escalationMode());
    }

    @Test
    void onlyAReconRaidUsesReconnaissance() {
        for (ContractObjectiveType objectiveType : ContractObjectiveType.values()) {
            assertEquals(objectiveType == ContractObjectiveType.RECON_RAID,
                  StratConContractMechanics.forObjectiveType(objectiveType).isUsingReconnaissance(),
                  objectiveType.name());
        }
    }

    @Test
    void onlyRiotDutyLetsRiotsMoveMorale() {
        for (ContractObjectiveType objectiveType : ContractObjectiveType.values()) {
            assertEquals(objectiveType == ContractObjectiveType.RIOT_DUTY,
                  StratConContractMechanics.forObjectiveType(objectiveType).isRiotAffectingMorale(),
                  objectiveType.name());
        }
    }
}
