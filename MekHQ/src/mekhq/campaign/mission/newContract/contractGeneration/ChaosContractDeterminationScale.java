package mekhq.campaign.mission.newContract.contractGeneration;

import static java.lang.Math.ceil;
import static mekhq.campaign.force.FormationType.STANDARD;

import megamek.common.units.Entity;
import mekhq.campaign.LocalHangar;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.mission.enums.CombatRole;
import mekhq.campaign.unit.Unit;

public class ChaosContractDeterminationScale {
    private final static double BATTLE_VALUE_PER_SCALE = 4_500.0; // Draconis Reach first printing pg 36

    static int generateScaleForDetachment(PlayerForce playerForce, LocalHangar hanger, boolean isCadreDuty) {
        int validBattleValue = 0;

        for (Unit unit : hanger.getUnits()) {
            int formationId = unit.getFormationId();
            Formation formation = playerForce.getFormation(formationId);
            if (formation != null) {
                CombatRole roleInMemory = formation.getCombatRoleInMemory();
                boolean hasCombatRole = roleInMemory.isCombatRole() || (isCadreDuty && roleInMemory.isCadre());
                if (formation.isFormationType(STANDARD) && hasCombatRole) {
                    Entity entity = unit.getEntity();
                    validBattleValue += entity != null ? entity.calculateBattleValue(true, true) : 0;
                }
            }
        }

        return (int) ceil(validBattleValue / BATTLE_VALUE_PER_SCALE);
    }
}
