package mekhq.campaign.mission.newContract.contractGeneration;

import static megamek.common.compute.Compute.d6;
import static megamek.common.compute.Compute.randomInt;
import static mekhq.campaign.universe.Faction.COMSTAR_FACTION_CODE;
import static mekhq.campaign.universe.Faction.WORD_OF_BLAKE_FACTION_CODE;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import jakarta.annotation.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.RandomFactionGenerator;
import org.jspecify.annotations.NonNull;

public class ChaosContractEmployerDetermination {
    private static final MMLogger LOGGER = MMLogger.create(ChaosContractEmployerDetermination.class);

    private static final int COMSTAR_EMPLOYER_CHANCE = 100;
    private static final int WORD_OF_BLAKE_EMPLOYER_CHANCE = 40;

    public static EmployerGenerationData getEmployerGenerationData(LocalDate currentDate, ILocation currentLocation,
          boolean isMercenarySearch) {
        ChaosEmployerType employerType = determineEmployerType();
        Faction employer = determineEmployerFaction(employerType, currentDate, currentLocation, isMercenarySearch);
        return new EmployerGenerationData(employerType, employer);
    }

    private static ChaosEmployerType determineEmployerType() {
        // Hot Spots Draconis Reach, pg 143 first printing
        int roll = d6(2);
        return switch (roll) {
            case 2 -> getCivilianEmployer();
            case 3 -> ChaosEmployerType.LOCAL_PLANETARY_GOVERNMENT;
            case 4, 12 -> ChaosEmployerType.MERCENARY_SUBCONTRACT;
            case 5, 9 -> ChaosEmployerType.CORPORATION;
            case 6 -> ChaosEmployerType.LOCAL_SYSTEM_OWNER;
            case 7, 11 -> ChaosEmployerType.ANY_SYSTEM_OWNER;
            case 8 -> ChaosEmployerType.NOBLE;
            case 10 -> ChaosEmployerType.ANY_PLANETARY_GOVERNMENT;
            default -> throw new IllegalStateException("Unexpected value: " + roll);
        };
    }

    private static @NonNull ChaosEmployerType getCivilianEmployer() {
        int roll = d6(1);
        return switch (roll) {
            case 1 -> ChaosEmployerType.CIVILIAN_ORGANIZATION_REBELS;
            case 2, 3, 4, 5 -> ChaosEmployerType.CIVILIAN_ORGANIZATION_MILITIA;
            case 6 -> ChaosEmployerType.CIVILIAN_ORGANIZATION_BUSINESS;
            default -> throw new IllegalStateException("Unexpected value: " + roll);
        };
    }

    private static @Nullable Faction determineEmployerFaction(ChaosEmployerType employerType,
          LocalDate currentDate, ILocation currentLocation, boolean isMercenarySearch) {
        if (employerType.isCurrentSystemEmployer()) {
            Faction employerFaction = getCurrentSystemEmployer(currentDate, currentLocation);
            if (employerFaction != null) {return employerFaction;}
        }

        RandomFactionGenerator generator = RandomFactionGenerator.getInstance();
        return generator.getRandomEmployerFaction(currentLocation, currentDate, isMercenarySearch);
    }

    private static @Nullable Faction getCurrentSystemEmployer(LocalDate currentDate, ILocation currentLocation) {
        PlanetarySystem currentSystem = currentLocation.getCurrentSystem();
        List<String> residentFactions = currentSystem.getFactions(currentDate);

        if (residentFactions.isEmpty()) {
            return null;
        }

        Factions factionsInstance = Factions.getInstance();
        Collections.shuffle(residentFactions);
        for (String residentFaction : residentFactions) {
            Faction employer = factionsInstance.getFaction(residentFaction);
            if (employer != null) {
                return employer;
            }
        }

        return null;
    }

    private static @Nullable Faction checkForSpecialEmployer(int currentYear) {
        Faction specialEmployer = checkForEmployerOverride(currentYear,
              COMSTAR_FACTION_CODE,
              COMSTAR_EMPLOYER_CHANCE,
              true);
        if (specialEmployer != null) {
            return specialEmployer;
        }

        specialEmployer = checkForEmployerOverride(currentYear, WORD_OF_BLAKE_FACTION_CODE,
              WORD_OF_BLAKE_EMPLOYER_CHANCE, false);
        return specialEmployer;
    }

    private static @Nullable Faction checkForEmployerOverride(int currentYear, String factionCode, int chance,
          boolean useSpecialComStarStartYear) {
        Faction faction = Factions.getInstance().getFaction(factionCode);
        int startYear = useSpecialComStarStartYear ? 2788 : faction.getStartYear();
        int endYear = faction.getEndYear();

        if (isFactionOperating(currentYear, startYear, endYear)) {
            int roll = randomInt(chance);

            if (roll == 0) {
                return faction;
            }
        }
        return null;
    }

    private static boolean isFactionOperating(int currentYear, int startYearComStar, int endingYearComStar) {
        return (currentYear >= startYearComStar) &&
                     (currentYear <= endingYearComStar);
    }
}
