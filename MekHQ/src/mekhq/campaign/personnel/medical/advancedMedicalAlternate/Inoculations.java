/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

/**
 * Manages planetary inoculation systems for campaign personnel.
 *
 * <p>This class handles the vaccination of personnel against planetary diseases, tracking disease spread, and
 * managing the associated costs and administrative processes. It provides functionality for prompting inoculation
 * decisions, processing disease checks, and applying disease effects to personnel.</p>
 *
 * @author Illiani
 * @since 0.50.10
 */
public class Inoculations {
    private static final MMLogger LOGGER = MMLogger.create(Inoculations.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Inoculations";

    private static final int MONTHLY_NEW_DISEASE_CHANCE = 1000;
    private static final int MONTHLY_DISEASE_SPREAD_CHANCE = 50;

    // ATOW says 50 C-Bills/Person; we've increased it as these vaccines are 100% effective, unlike those in ATOW.
    // They also don't require MedTech checks, and they don't run the risk of an allergic reaction or failing. That
    // suggests these are higher quality. Also, this whole mechanic is meant to be a money sink, so it needs to have
    // some teeth.
    private static final int INOCULATION_COST_PER_PERSON = 200;

    private static final int DIALOG_CHOICE_EVERYBODY = 0;
    private static final int DIALOG_CHOICE_MILITARY = 1;
    private static final int DIALOG_CHOICE_CIVILIAN = 2;
    private static final int DIALOG_CHOICE_NOBODY = 3;

    /**
     * Triggers the inoculation prompt dialog for the player.
     *
     * <p>This method determines which personnel need inoculations for the current planet, calculates the costs, and
     * presents dialog options to the player. Handles cases where the campaign is in transit or all personnel are
     * already vaccinated.</p>
     *
     * @param campaign the current campaign
     * @param isAdHoc  {@code true} if this is an adhoc request (player-initiated), {@code false} if automatic
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static void triggerInoculationPrompt(Campaign campaign, boolean isAdHoc) {
        CurrentLocation location = campaign.getLocation();
        if (!location.isInTransit()) {
            new ImmersiveDialogNotification(campaign, getTextAt(RESOURCE_BUNDLE, "Inoculations.inTransit"), true);
            return;
        }
        Planet currentPlanet = location.getPlanet();
        LocalDate today = campaign.getLocalDate();
        String planetName = currentPlanet.getName(today);

        // Determine who, if anyone, needs inoculations
        Collection<Person> allPersonnel = campaign.getPersonnel();
        List<Person> militaryPersonnel = new ArrayList<>();
        List<Person> civilianPersonnel = new ArrayList<>();
        gatherPersonnelInNeedOfInoculations(allPersonnel, currentPlanet.getId(), civilianPersonnel, militaryPersonnel);

        if (militaryPersonnel.isEmpty() && civilianPersonnel.isEmpty()) { // Nobody needs treatment
            new ImmersiveDialogNotification(campaign,
                  getFormattedTextAt(RESOURCE_BUNDLE, "Inoculations.fullVaccinated", planetName), true);
            return;
        }

        Money militaryInoculationCost = Money.of(INOCULATION_COST_PER_PERSON).multipliedBy(militaryPersonnel.size());
        Money civilianInoculationCost = Money.of(INOCULATION_COST_PER_PERSON).multipliedBy(civilianPersonnel.size());
        Money totalInoculationCost = militaryInoculationCost.plus(civilianInoculationCost);

        ImmersiveDialogSimple dialog = null;
        boolean wasConfirmed = false;
        while (!wasConfirmed) {
            dialog = triggerDialog(campaign,
                  planetName,
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

    /**
     * Gathers personnel who need inoculations for the specified planet.
     *
     * <p>Separates personnel into military and civilian lists based on their role and whether they already have
     * inoculations for the current planet.Excludes departed and absent personnel.</p>
     *
     * @param allPersonnel      the collection of all campaign personnel
     * @param planetId          the ID of the planet to check inoculations for
     * @param civilianPersonnel output list to populate with civilians needing inoculations
     * @param militaryPersonnel output list to populate with military personnel needing inoculations
     *
     * @author Illiani
     * @since 0.50.10
     */
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

    /**
     * Creates and displays the inoculation dialog with appropriate options and costs.
     *
     * @param campaign     the current campaign
     * @param planetName   the name of the planet requiring inoculations
     * @param militaryCost the formatted cost string for military personnel inoculations
     * @param civilianCost the formatted cost string for civilian personnel inoculations
     * @param totalCost    the formatted total cost string for all inoculations
     * @param isAdHoc      true if this is an adhoc request, false if automatic
     *
     * @return the dialog instance with the player's choice
     *
     * @author Illiani
     * @since 0.50.10
     */
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

    /**
     * Handles the player's dialog choice and processes the corresponding inoculations.
     *
     * <p>Based on the choice index, this method processes payment and inoculation for everybody, military only,
     * civilians only, or nobody. Displays failure alerts if payment cannot be processed.</p>
     *
     * @param campaign                the current campaign
     * @param choiceIndex             the index of the dialog choice selected
     * @param today                   the current date
     * @param totalInoculationCost    the total cost for all inoculations
     * @param militaryPersonnel       the list of military personnel to inoculate
     * @param location                the planet where inoculations are being administered
     * @param civilianPersonnel       the list of civilian personnel to inoculate
     * @param militaryInoculationCost the cost for military personnel inoculations
     * @param civilianInoculationCost the cost for civilian personnel inoculations
     *
     * @author Illiani
     * @since 0.50.10
     */
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

    /**
     * Inoculates all personnel in the provided list for the specified planet.
     *
     * <p>Adds planetary inoculation records to each person and logs the inoculation event.</p>
     *
     * @param today     the current date
     * @param personnel the list of personnel to inoculate
     * @param planet    the planet for which inoculations are being administered
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static void inoculatePersonnel(LocalDate today, List<Person> personnel, Planet planet) {
        String planetName = planet.getName(today);

        for (Person person : personnel) {
            person.addPlanetaryInoculation(planet.getId());
            MedicalLogger.inoculation(person, today, planetName);
        }
    }

    /**
     * Displays an alert dialog indicating that payment for inoculations failed.
     *
     * @param campaign the current campaign
     * @param cost     the formatted cost string that could not be paid
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static void transactionFailureAlert(Campaign campaign, String cost) {
        new ImmersiveDialogNotification(campaign,
              getFormattedTextAt(RESOURCE_BUNDLE, "Inoculations.transaction.failed", cost),
              true);
    }

    /**
     * Processes payment for inoculations through the campaign finances.
     *
     * @param finances the campaign finances
     * @param today    the current date
     * @param cost     the cost of the inoculations
     *
     * @return {@code true} if payment was successful, {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static boolean payForInoculations(Finances finances, LocalDate today, Money cost) {
        return finances.debit(TransactionType.MEDICAL_EXPENSES, today, cost,
              getTextAt(RESOURCE_BUNDLE, "Inoculations.transaction"));
    }

    /**
     * Finds the senior doctor from a list of all doctors.
     *
     * <p>Determines seniority based on rank and uses skill as a tiebreaker.</p>
     *
     * @param allDoctors the list of all available doctors
     * @param campaign   the current campaign
     *
     * @return the senior doctor, or {@code null} if no doctors are available
     *
     * @author Illiani
     * @since 0.50.10
     */
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

    /**
     * Creates the list of button labels for the inoculation dialog.
     *
     * @param militaryCost the formatted cost for military personnel
     * @param civilianCost the formatted cost for civilian personnel
     * @param totalCost    the formatted total cost
     *
     * @return the list of button labels with costs
     *
     * @author Illiani
     * @since 0.50.10
     */
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

    /**
     * Performs monthly disease checks for all campaign personnel.
     *
     * <p>Identifies active diseases in the campaign, determines disease spread chance, and applies diseases to
     * unvaccinated personnel based on probability rolls. If no diseases are active, a new random disease is
     * introduced.</p>
     *
     * @param campaign the current campaign
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static void performDiseaseChecks(Campaign campaign) {
        CurrentLocation location = campaign.getLocation();
        String planetCode = location.isOnPlanet() ? location.getPlanet().getId() : null;

        Collection<Person> allPersonnel = campaign.getPersonnelFilteringOutDepartedAndAbsent();

        // Gather the active diseases in the players' campaign
        Set<InjuryType> activeDiseases = getActiveDiseases(allPersonnel);
        int diseaseChance = activeDiseases.isEmpty() ? MONTHLY_NEW_DISEASE_CHANCE : MONTHLY_DISEASE_SPREAD_CHANCE;

        if (activeDiseases.isEmpty()) {
            if (planetCode != null) {
                // If there are no active diseases, add one
                activeDiseases.add(DiseaseService.catchRandomDisease());
            } else {
                // No new diseases are introduced while in transit
                return;
            }
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

    /**
     * Determines which diseases are spreading and applies them to unvaccinated personnel.
     *
     * <p>For each person without proper vaccination, rolls for each active disease based on the disease chance.
     * Children are excluded from disease spread. Vaccinations do not protect personnel in transit due to close
     * confines.</p>
     *
     * @param campaign       the current campaign
     * @param allPersonnel   all active personnel
     * @param planetCode     the planet code, or null if in transit
     * @param activeDiseases the set of currently active diseases
     * @param diseaseChance  the chance for disease spread (1 in diseaseChance)
     *
     * @return a set of disease names that spread during this check
     *
     * @author Illiani
     * @since 0.50.10
     */
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

    /**
     * Collects all active diseases currently present in the personnel.
     *
     * @param allPersonnel all active personnel
     *
     * @return a set of injury types representing active diseases
     *
     * @author Illiani
     * @since 0.50.10
     */
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

    /**
     * Applies a disease to a person.
     *
     * <p>Creates a new disease injury with random duration and adds it to the person's injury list. Children are
     * excluded from disease application. If the disease is fatal, changes the person's status to
     * {@link PersonnelStatus#CONTAGIOUS_DISEASE}.</p>
     *
     * @param campaign the current campaign
     * @param person   the person to infect with the disease
     * @param disease  the type of disease to apply
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static void applyDisease(Campaign campaign, Person person, InjuryType disease) {
        // Some diseases are of a venereal nature, we deliberately exclude children for that reason.
        if (person.isChild(campaign.getLocalDate(), true)) {
            return;
        }

        Injury newDisease = disease.newInjury(campaign, person, BodyLocation.INTERNAL, 1);
        if (newDisease == null) {
            LOGGER.error("Failed to generate disease of type {} at body location {} with duration multiplier {}",
                  disease, BodyLocation.INTERNAL, 1);
        } else {
            int duration = DiseaseService.getDiseaseDuration();
            newDisease.setOriginalTime(duration);
            newDisease.setTime(duration);
            person.addInjury(newDisease);

            if (disease.impliesDead(BodyLocation.INTERNAL)) {
                person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.CONTAGIOUS_DISEASE);
            }
        }
    }

    /**
     * Triggers campaign reports for diseases that have spread.
     *
     * <p>Displays colored alerts for each spreading disease. Alert color is red if in transit (higher danger) or
     * yellow if on a planet.</p>
     *
     * @param campaign          the current campaign
     * @param isInTransit       true if the campaign is in transit between planets
     * @param spreadingDiseases the set of disease names that have spread
     *
     * @author Illiani
     * @since 0.50.10
     */
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
