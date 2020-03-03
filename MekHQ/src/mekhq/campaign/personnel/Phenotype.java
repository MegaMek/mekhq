package mekhq.campaign.personnel;

public class Phenotype {
    public static final int P_NONE = 0; // No phenotype
    public static final int P_MECHWARRIOR = 1;
    public static final int P_ELEMENTAL = 2;
    public static final int P_AEROSPACE = 3;
    public static final int P_VEHICLE = 4;
    public static final int P_PROTOMECH = 5;
    public static final int P_NAVAL = 6;
    public static final int P_GENERAL = 7;
    public static final int P_NUM = 8;

    public static final String[] phenotypeNames = {
            "General", "MechWarrior", "Aerospace Pilot", "Elemental",
            "ProtoMech Pilot", "Naval Commander"
    };

    public static String getPhenotypeName(int phenotype) {
        switch (phenotype) {
            case P_NONE:
                return "Freeborn";
            case P_MECHWARRIOR:
                return "Trueborn Mechwarrior";
            case P_AEROSPACE:
                return "Trueborn Pilot";
            case P_VEHICLE:
                return "Trueborn Vehicle Crew";
            case P_ELEMENTAL:
                return "Trueborn Elemental";
            default:
                return "?";
        }
    }

    public static String getPhenotypeShortName(int phenotype) {
        switch (phenotype) {
            case P_NONE:
                return "Freeborn";
            case P_MECHWARRIOR:
            case P_AEROSPACE:
            case P_ELEMENTAL:
            case P_VEHICLE:
                return "Trueborn";
            default:
                return "?";
        }
    }
}
