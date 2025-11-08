package mekhq.campaign.personnel.medical.advancedMedicalAlternate;

import static megamek.common.compute.Compute.randomInt;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.log.MedicalLogger;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.medical.BodyLocation;
import mekhq.campaign.universe.Planet;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogConfirmation;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNotification;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

public class Inoculations {
    private static final MMLogger LOGGER = MMLogger.create(Inoculations.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Inoculations";

    private static final int MONTHLY_NEW_DISEASE_CHANCE = 250;
    private static final int MONTHLY_DISEASE_SPREAD_CHANCE = 50;
    private static final int INOCULATION_COST_PER_PERSON = 50;

    private static final int DIALOG_CHOICE_EVERYBODY = 0;
    private static final int DIALOG_CHOICE_MILITARY = 1;
    private static final int DIALOG_CHOICE_CIVILIAN = 2;
    private static final int DIALOG_CHOICE_NOBODY = 3;

    public static void triggerInoculationPrompt(Campaign campaign, boolean isAdHoc) {
        CurrentLocation location = campaign.getLocation();
        if (!location.isOnPlanet()) {
            new ImmersiveDialogNotification(campaign, getTextAt(RESOURCE_BUNDLE, "Inoculations.inTransit"), true);
            return;
        }
        Planet currentPlanet = location.getPlanet();

        // Determine who, if anyone, needs inoculations
        Collection<Person> allPersonnel = campaign.getPersonnel();
        List<Person> militaryPersonnel = new ArrayList<>();
        List<Person> civilianPersonnel = new ArrayList<>();
        gatherPersonnelInNeedOfInoculations(allPersonnel, currentPlanet.getId(), civilianPersonnel, militaryPersonnel);

        if (militaryPersonnel.isEmpty() && civilianPersonnel.isEmpty()) { // Nobody needs treatment
            return;
        }

        Money militaryInoculationCost = Money.of(INOCULATION_COST_PER_PERSON).multipliedBy(militaryPersonnel.size());
        Money civilianInoculationCost = Money.of(INOCULATION_COST_PER_PERSON).multipliedBy(civilianPersonnel.size());
        Money totalInoculationCost = militaryInoculationCost.plus(civilianInoculationCost);

        LocalDate today = campaign.getLocalDate();

        ImmersiveDialogSimple dialog = null;
        boolean wasConfirmed = false;
        while (!wasConfirmed) {
            dialog = triggerDialog(campaign,
                  currentPlanet.getName(today),
                  militaryInoculationCost.toAmountString(),
                  civilianInoculationCost.toAmountString(),
                  totalInoculationCost.toAmountString(),
                  isAdHoc);
            ImmersiveDialogConfirmation confirmation = new ImmersiveDialogConfirmation(campaign);
            wasConfirmed = confirmation.wasConfirmed();
        }

        int dialogChoice = dialog.getDialogChoice();
        if (dialogChoice == DIALOG_CHOICE_NOBODY) {
            return;
        }

        handleDialogChoice(campaign,
              dialog.getDialogChoice(),
              today,
              totalInoculationCost,
              militaryPersonnel,
              currentPlanet,
              civilianPersonnel,
              militaryInoculationCost,
              civilianInoculationCost);
    }

    private static void gatherPersonnelInNeedOfInoculations(Collection<Person> allPersonnel, String planetId,
          List<Person> civilianPersonnel, List<Person> militaryPersonnel) {
        for (Person person : allPersonnel) {
            PersonnelStatus status = person.getStatus();
            if (status.isDepartedUnit() || status.isAbsent()) {
                continue;
            }

            if (person.hasPlanetaryInoculation(planetId)) {
                continue;
            }

            if (person.isCivilian() && person.getUnit() == null && person.getTechUnits().isEmpty()) {
                civilianPersonnel.add(person);
            } else {
                militaryPersonnel.add(person);
            }
        }
    }

    private static ImmersiveDialogSimple triggerDialog(Campaign campaign, String planetName, String militaryCost,
          String civilianCost, String totalCost, boolean isAdHoc) {
        List<Person> doctors = campaign.getDoctors();
        Person seniorDoctor = getDoctor(doctors, campaign);

        String inCharacterKey = isAdHoc ? "Inoculations.prompt.ic.adHoc" : "Inoculations.prompt.ic";
        String outOfCharacterKey = isAdHoc ? "Inoculations.prompt.ooc.adHoc" : "Inoculations.prompt.ooc";

        return new ImmersiveDialogSimple(campaign,
              seniorDoctor,
              null,
              getFormattedTextAt(RESOURCE_BUNDLE, inCharacterKey, campaign.getCommanderAddress(), planetName,
                    campaign.getFunds().toAmountString()),
              getButtons(militaryCost, civilianCost, totalCost),
              getTextAt(RESOURCE_BUNDLE, outOfCharacterKey),
              null,
              true);
    }

    private static void handleDialogChoice(Campaign campaign, int choiceIndex, LocalDate today,
          Money totalInoculationCost, List<Person> militaryPersonnel, Planet location, List<Person> civilianPersonnel,
          Money militaryInoculationCost, Money civilianInoculationCost) {
        Finances finances = campaign.getFinances();
        switch (choiceIndex) {
            case DIALOG_CHOICE_EVERYBODY -> {
                if (payForInoculations(finances, today, totalInoculationCost)) {
                    inoculatePersonnel(today, militaryPersonnel, location);
                    inoculatePersonnel(today, civilianPersonnel, location);
                } else {
                    transactionFailureAlert(campaign, totalInoculationCost.toAmountString());
                }
            }
            case DIALOG_CHOICE_MILITARY -> {
                if (payForInoculations(finances, today, militaryInoculationCost)) {
                    inoculatePersonnel(today, militaryPersonnel, location);
                } else {
                    transactionFailureAlert(campaign, militaryInoculationCost.toAmountString());
                }
            }
            case DIALOG_CHOICE_CIVILIAN -> {
                if (payForInoculations(finances, today, civilianInoculationCost)) {
                    inoculatePersonnel(today, civilianPersonnel, location);
                } else {
                    transactionFailureAlert(campaign, civilianInoculationCost.toAmountString());
                }
            }
            case DIALOG_CHOICE_NOBODY -> {}
        }
    }

    private static void inoculatePersonnel(LocalDate today, List<Person> personnel, Planet planet) {
        String planetName = planet.getName(today);

        for (Person person : personnel) {
            person.addPlanetaryInoculation(planet.getId());
            MedicalLogger.inoculation(person, today, planetName);
        }
    }

    private static void transactionFailureAlert(Campaign campaign, String cost) {
        new ImmersiveDialogNotification(campaign,
              getFormattedTextAt(RESOURCE_BUNDLE, "Inoculations.transaction.failed", cost),
              true);
    }

    private static boolean payForInoculations(Finances finances, LocalDate today, Money cost) {
        return finances.debit(TransactionType.MEDICAL_EXPENSES, today, cost,
              getTextAt(RESOURCE_BUNDLE, "Inoculations.transaction"));
    }

    private static @Nullable Person getDoctor(List<Person> allDoctors, Campaign campaign) {
        Person seniorDoctor = null;
        for (Person doctor : allDoctors) {
            if (seniorDoctor == null) {
                seniorDoctor = doctor;
                continue;
            }

            if (doctor.outRanksUsingSkillTiebreaker(campaign, seniorDoctor)) {
                seniorDoctor = doctor;
            }
        }

        return seniorDoctor;
    }

    private static List<String> getButtons(String militaryCost, String civilianCost, String totalCost) {
        String everybodyLabel = getFormattedTextAt(RESOURCE_BUNDLE, "Inoculations.prompt.everybody",
              totalCost);
        String militaryLabel = getFormattedTextAt(RESOURCE_BUNDLE, "Inoculations.prompt.military",
              militaryCost);
        String civilianLabel = getFormattedTextAt(RESOURCE_BUNDLE, "Inoculations.prompt.civilian",
              civilianCost);
        String nobodyLabel = getTextAt(RESOURCE_BUNDLE, "Inoculations.prompt.nobody");

        return List.of(everybodyLabel, militaryLabel, civilianLabel, nobodyLabel);
    }

    public static void performDiseaseChecks(Campaign campaign) {
        CurrentLocation location = campaign.getLocation();
        String planetCode = location.isOnPlanet() ? location.getPlanet().getId() : null;

        Collection<Person> allPersonnel = campaign.getPersonnelFilteringOutDepartedAndAbsent();

        // Gather the active diseases in the players' campaign
        Set<InjuryType> activeDiseases = getActiveDiseases(allPersonnel);
        int diseaseChance = activeDiseases.isEmpty() ? MONTHLY_NEW_DISEASE_CHANCE : MONTHLY_DISEASE_SPREAD_CHANCE;

        // If there are no active diseases, add one
        if (activeDiseases.isEmpty()) {
            activeDiseases.add(DiseaseService.catchRandomDisease());
        }

        // Now roll for the spread of disease among vaccine dodgers
        Set<String> spreadingDiseases = getSpreadingDiseases(campaign,
              allPersonnel,
              planetCode,
              activeDiseases,
              diseaseChance);

        // Inform the player
        triggerDiseaseSpreadMessages(campaign, planetCode == null, spreadingDiseases);
    }

    private static Set<String> getSpreadingDiseases(Campaign campaign, Collection<Person> allPersonnel,
          String planetCode, Set<InjuryType> activeDiseases, int diseaseChance) {
        Set<String> spreadingDiseases = new HashSet<>();
        LocalDate today = campaign.getLocalDate();
        for (Person person : allPersonnel) {
            // Some of these diseases are of a venereal nature, we don't want children getting infected.
            if (person.isChild(today, true)) {
                continue;
            }

            // If planetCode is null, the campaign is in transit. If they have an active disease while in transit, the
            // close confines remove any benefit from vaccinations
            if (planetCode != null && person.hasPlanetaryInoculation(planetCode)) {
                continue;
            }

            for (InjuryType disease : activeDiseases) {
                if (randomInt(diseaseChance) == 0) {
                    applyDisease(campaign, person, disease);
                    spreadingDiseases.add(disease.getSimpleName());
                }
            }
        }
        return spreadingDiseases;
    }

    private static Set<InjuryType> getActiveDiseases(Collection<Person> allPersonnel) {
        Set<InjuryType> activeDiseases = new HashSet<>();
        for (Person person : allPersonnel) {
            for (Injury injury : person.getInjuries()) {
                if (injury.getLevel().isDisease()) {
                    activeDiseases.add(injury.getType());
                }
            }
        }
        return activeDiseases;
    }

    public static void applyDisease(Campaign campaign, Person person, InjuryType disease) {
        // Some diseases are of a venereal nature, we deliberately exclude children for that reason.
        if (person.isChild(campaign.getLocalDate(), true)) {
            return;
        }

        Injury newInjury = disease.newInjury(campaign, person, BodyLocation.INTERNAL, 1);
        if (newInjury == null) {
            LOGGER.error("Failed to generate disease of type {} at body location {} with duration multiplier {}",
                  disease, BodyLocation.INTERNAL, 1);
        } else {
            person.addInjury(newInjury);
        }
    }

    public static void triggerDiseaseSpreadMessages(Campaign campaign, boolean isInTransit,
          Set<String> spreadingDiseases) {
        String alertColor = spanOpeningWithCustomColor(isInTransit ? getNegativeColor() : getWarningColor());
        for (String spread : spreadingDiseases) {
            String reportKey;
            if (isInTransit) {
                reportKey = "Inoculations.spread.transit";
            } else {
                reportKey = "Inoculations.spread.normal";
            }

            campaign.addReport(getFormattedTextAt(RESOURCE_BUNDLE, reportKey, alertColor, CLOSING_SPAN_TAG, spread));
        }
    }
}
