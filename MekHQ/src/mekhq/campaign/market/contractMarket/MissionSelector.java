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

    public static AtBContractType getSpecialMission(int roll, int margin) {
        roll += margin;
        if (roll < 3) {

        }
    }

    public static AtBContractType getCovertMission(int roll, int margin) {
        roll += margin;
    }

    public static AtBContractType getPirateMission(int roll, int margin) {
        roll += margin;
        if (roll < 6) {
            return AtBContractType.RECON_RAID;
        } else {
            return AtBContractType.OBJECTIVE_RAID;
        }
    }
}
