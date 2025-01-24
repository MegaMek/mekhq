package mekhq.campaign.personnel.prisoners;

import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

import java.util.UUID;

import static megamek.common.Compute.d6;
import static mekhq.campaign.mission.enums.AtBMoraleLevel.STALEMATE;

public class MonthlyPrisonerEventPicker {
    private static int DEFAULT_EVENT_CHANCE = STALEMATE.ordinal();

    public static void rollForMonthlyPrisonerEvent(Campaign campaign) {
        if (campaign.hasActiveContract()) {
            Contract contract = campaign.getActiveContracts().get(0);

            int ransomEventChance = DEFAULT_EVENT_CHANCE;
            if (contract instanceof AtBContract) {
                ransomEventChance = ((AtBContract) contract).getMoraleLevel().ordinal();
            }

            int roll = d6(2);
            if (roll <= ransomEventChance) {
                boolean isFriendlyPOWs = false;

                if (!campaign.getFriendlyPrisoners().isEmpty()) {
                    isFriendlyPOWs = d6(1) <= 2;
                }

                new PrisonerRansomEvent(campaign, isFriendlyPOWs);
            }
        }
    }

    public static int calculatePrisonerCapacity(Campaign campaign) {
        // These values are based on CamOps. CamOps states a platoon of CI or one squad of BA can
        // handle 100 prisoners. As there are usually around 28 soldiers in a CI platoon and 5 BA
        // in a BA Squad, we extrapolated from there to more easily handle different platoon and
        // squad sizes.
        final int PRISONER_CAPACITY_CI = 4;
        final int PRISONER_CAPACITY_BA = 20;

        int prisonerCapacity = 0;

        for (Force force : campaign.getAllForces()) {
            if (!force.getForceType().isSecurity()) {
                continue;
            }

            for (UUID unitId : force.getUnits()) {
                Unit unit = campaign.getUnit(unitId);
                if (unit == null) {
                    continue;
                }

                if (unit.isBattleArmor()) {
                    int crewSize = unit.getCrew().size();
                    for (int trooper = 0; trooper < crewSize; trooper++) {
                        if (unit.isBattleArmorSuitOperable(trooper)) {
                            prisonerCapacity += PRISONER_CAPACITY_BA;
                        }
                    }

                    prisonerCapacity += crewSize * PRISONER_CAPACITY_BA;
                    continue;
                }

                if (unit.isConventionalInfantry()) {
                    for (Person soldier : unit.getCrew()) {
                        if (!soldier.needsFixing() && !soldier.needsAMFixing()) {
                            prisonerCapacity += PRISONER_CAPACITY_CI;
                        }
                    }
                }
            }
        }

        return prisonerCapacity + campaign.getTemporaryPrisonerCapacity();
    }
}
