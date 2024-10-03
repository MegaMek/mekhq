package mekhq.campaign.market.contractMarket;

import megamek.common.Compute;
import mekhq.campaign.mission.enums.AtBContractType;

public class MissionSelector {
    public static AtBContractType getInnerSphereClanMission(int roll, int margin, boolean isClan) {
        if (isClan) {
            int result = roll + margin;
            if (result < 4 || result > 11) {
                // Reroll on the IS/Clan column if the force is clan and the result is Covert or Special
                return getInnerSphereClanMission(Compute.d6(2), margin, true);
            }
        }
        roll += margin;
        if (roll < 3) {
            return getCovertMission(Compute.d6(2), margin);
        } else if (roll == 3 || roll > 11) {
            return getSpecialMission(Compute.d6(2), margin);
        } else if (roll == 4) {
            return AtBContractType.PIRATE_HUNTING;
        } else if (roll == 5) {
            return AtBContractType.PLANETARY_ASSAULT;
        } else if (roll == 6 || roll == 7) {
            return AtBContractType.RECON_RAID;
        } else if (roll == 8) {
            return AtBContractType.EXTRACTION_RAID;
        } else if (roll == 9) {
            return AtBContractType.EXTRACTION_RAID;
        } else if (roll == 10) {
            return AtBContractType.GARRISON_DUTY;
        } else {
            return AtBContractType.CADRE_DUTY;
        }
    }

    public static AtBContractType getIndependentMission(int roll, int margin, boolean isClan) {
        if (isClan) {
            int result = roll + margin;
            if (result < 4 || result > 11) {
                // Reroll on the IS/Clan column if the force is clan and the result is Covert or Special
                return getInnerSphereClanMission(Compute.d6(2), margin, true);
            }
        }
        roll += margin;
        if (roll < 3) {
            return getCovertMission(Compute.d6(2), margin);
        } else if (roll == 3 || roll > 11) {
            return getSpecialMission(Compute.d6(2), margin);
        } else if (roll == 4) {
            return AtBContractType.PLANETARY_ASSAULT;
        } else if (roll == 5 || roll == 9) {
            return AtBContractType.OBJECTIVE_RAID;
        } else if (roll == 6) {
            return AtBContractType.EXTRACTION_RAID;
        } else if (roll == 7) {
            return AtBContractType.PIRATE_HUNTING;
        } else if (roll == 8) {
            return AtBContractType.SECURITY_DUTY;
        } else if (roll == 10) {
            return AtBContractType.GARRISON_DUTY;
        } else {
            return AtBContractType.CADRE_DUTY;
        }
    }

    public static AtBContractType getCorporationMission(int roll, int margin, boolean isClan) {
        if (isClan) {
            int result = roll + margin;
            if (result < 5 || result > 11) {
                // Reroll on the IS/Clan column if the force is clan and the result is Covert or Special
                return getInnerSphereClanMission(Compute.d6(2), margin, true);
            }
        }
        roll += margin;
        if (roll < 4) {
            return getCovertMission(Compute.d6(2), margin);
        } else if (roll == 4 || roll > 11) {
            return getSpecialMission(Compute.d6(2), margin);
        } else if (roll == 5 || roll == 8) {
            return AtBContractType.OBJECTIVE_RAID;
        } else if (roll == 6) {
            return AtBContractType.EXTRACTION_RAID;
        } else if (roll == 7) {
            return AtBContractType.RECON_RAID;
        } else if (roll == 9) {
            return AtBContractType.SECURITY_DUTY;
        } else if (roll == 10) {
            return AtBContractType.GARRISON_DUTY;
        } else {
            // TODO: determine which is the higher paying between cadre/garrison and return that
            return AtBContractType.CADRE_DUTY;
        }
    }

    public static AtBContractType getPirateMission(int roll, int margin) {
        roll += margin;
        if (roll < 6) {
            return AtBContractType.RECON_RAID;
        } else {
            return AtBContractType.OBJECTIVE_RAID;
        }
    }

    private static AtBContractType getSpecialMission(int roll, int margin) {
        roll += margin;
        if (roll < 3) {
            return getCovertMission(Compute.d6(2), margin);
        } else if (roll < 5) {
            // TODO: figure out how to offer planetary assault followup contracts
            return AtBContractType.GUERRILLA_WARFARE;
        } else if (roll == 5 || roll == 8) {
            // TODO: figure out how to offer planetary assault followup contracts
            return AtBContractType.RECON_RAID;
        } else if (roll == 6) {
            return AtBContractType.EXTRACTION_RAID;
        } else if (roll == 7) {
            // TODO: change this to RETAINER if/when that is implemented
            return AtBContractType.GARRISON_DUTY;
        } else if (roll == 9) {
            return AtBContractType.RELIEF_DUTY;
        } else if (roll == 10) {
            // TODO: figure out how to offer planetary assault followup contracts
            return AtBContractType.DIVERSIONARY_RAID;
        } else if (roll == 11) {
            //  // TODO: determine which is the higher paying between riot/garrison and return that
            return AtBContractType.RIOT_DUTY;
        } else {
            // TODO: determine which is the higher paying between cadre/garrison and return that
            return AtBContractType.CADRE_DUTY;
        }
    }

    private static AtBContractType getCovertMission(int roll, int margin) {
        // TODO: most of the covert mission types are not implemented in MekHQ at the time of writing,
        //  so just use the special missions table for now.
        return getSpecialMission(Compute.d6(2), margin);
    }
}
