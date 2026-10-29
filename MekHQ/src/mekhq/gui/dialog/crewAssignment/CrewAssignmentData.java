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
package mekhq.gui.dialog.crewAssignment;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToIntFunction;

import jakarta.annotation.Nullable;
import megamek.common.enums.SkillLevel;
import megamek.common.options.IOption;
import megamek.common.units.Entity;
import megamek.common.units.UnitType;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.LocalHangar;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.Formation;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.turnoverAndRetention.Fatigue;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan;
import mekhq.campaign.unit.crewAssignment.AutoFill;
import mekhq.campaign.unit.crewAssignment.CrewSeatLayout;
import mekhq.campaign.unit.crewAssignment.SeatEligibility;
import mekhq.gui.model.LocationFilterItem;

/**
 * Everything the {@link CrewAssignmentDialog} shows for one location, read once when the dialog opens: the units that
 * can be crewed there, the people who could crew them, and the staged plan.
 *
 * <p>Each person's experience level, special abilities and search text are worked out here once, so filtering and
 * sorting thousands of people as the player types never asks the campaign again.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
final class CrewAssignmentData {
    /** A unit and what the dialog shows about it. */
    /**
     * A unit and what the dialog shows about it.
     *
     * @param damageState the unit's damage level, one of the {@code Entity.DMG_} values
     * @param damageName  the damage level as the Hangar tab names it, such as "Light Damage"
     */
    record UnitEntry(Unit unit, String typeName, String formationName, int damageState, String damageName,
          String searchText) {}

    /** A special ability, as shown on a chip and in its tooltip. */
    record AbilityEntry(String name, String description) {}

    /** A person and what the dialog shows about them. */
    /**
     * A person and what the dialog shows about them.
     *
     * @param hits    the person's hits, counting advanced medical injuries
     * @param fatigue the person's effective fatigue, or {@code null} when the campaign does not use fatigue
     */
    record PersonEntry(Person person, SkillLevel skillLevel, List<AbilityEntry> abilities, int hits,
          @Nullable Integer fatigue, String searchText) {}

    private final Map<UUID, UnitEntry> units = new LinkedHashMap<>();
    private final Map<UUID, PersonEntry> people = new LinkedHashMap<>();
    private final AssignmentPlan plan;
    private final ToIntFunction<PersonnelRole> tempCrewPool;
    private final Comparator<Person> ranking;
    private final boolean useTransfers;
    private final LocalDate today;
    private final String locationName;

    /**
     * @param campaign the campaign
     * @param location the location whose units and people are shown
     */
    CrewAssignmentData(Campaign campaign, LocationFilterItem location) {
        CampaignOptions options = campaign.getCampaignOptions();
        today = campaign.getLocalDate();
        useTransfers = options.get(CampaignOption.USE_TRANSFERS);
        locationName = location.toString();
        plan = new AssignmentPlan(options.get(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL),
              options.get(CampaignOption.USE_IMPLANTS), options.get(CampaignOption.TECHS_USE_ADMINISTRATION));
        // A tech busy on a refit at this location cannot take on more units
        ForceHumanResources humanResources = campaign.getPlayerForce().getHumanResources();
        LocalHangar hangar = (location.getBase() == null) ? campaign.getPlayerForce().getHangar() :
                                   location.getBase().getBaseHangar();
        plan.setTechBusy(person -> humanResources.isWorkingOnRefit(hangar, person));
        // The pool is read when a fill is staged; the plan subtracts what it has already staged from it
        tempCrewPool = role -> humanResources.isBlobCrewEnabled(role, options) ?
                                     humanResources.getAvailableTempCrewPool(campaign, role) : 0;

        boolean isClan = campaign.getPlayerForce().isClanForce();
        boolean useFatigue = options.get(CampaignOption.USE_FATIGUE);
        Map<Person, SkillLevel> levels = new HashMap<>();
        for (Person person : location.selectPersonnel(campaign)) {
            if (isShown(person)) {
                levels.put(person, person.getSkillLevel(options, isClan, today, false, true));
            }
        }
        ranking = Comparator.<Person, SkillLevel>comparing(levels::get).reversed()
                        .thenComparing(Person::getFullName);
        levels.keySet().stream()
              .sorted(ranking)
              .forEach(person -> people.put(person.getId(), describe(campaign, person, levels.get(person),
                    useFatigue)));

        List<UnitEntry> unitEntries = new ArrayList<>();
        for (Unit unit : location.selectUnits(campaign)) {
            if ((CrewSeatLayout.takesCrew(unit) || SeatEligibility.takesTech(unit)) && unit.isAvailable()) {
                unitEntries.add(describe(campaign, unit));
            }
        }
        unitEntries.sort(Comparator.comparing(UnitEntry::formationName).thenComparing(entry -> entry.unit().getName()));
        unitEntries.forEach(entry -> units.put(entry.unit().getId(), entry));
    }

    /** Someone is listed if they are active and either have a combat role or are a tech; other support staff are not. */
    private static boolean isShown(Person person) {
        return person.getStatus().isActive()
                     && (person.getPrimaryRole().isCombat() || person.getSecondaryRole().isCombat() || person.isTech());
    }

    private static PersonEntry describe(Campaign campaign, Person person, SkillLevel level, boolean useFatigue) {
        List<AbilityEntry> abilities = new ArrayList<>();
        for (Enumeration<IOption> options = person.getOptions(PersonnelOptions.LVL3_ADVANTAGES);
              options.hasMoreElements(); ) {
            IOption option = options.nextElement();
            if (option.booleanValue()) {
                abilities.add(new AbilityEntry(Utilities.getOptionDisplayName(option), option.getDescription()));
            }
        }
        StringBuilder search = new StringBuilder(person.getFullTitle()).append(' ')
                                     .append(person.getPrimaryRole().getLabel(false)).append(' ')
                                     .append(person.getSecondaryRole().getLabel(false));
        for (AbilityEntry ability : abilities) {
            search.append(' ').append(ability.name());
        }
        Integer fatigue = useFatigue ? Fatigue.getEffectiveFatigue(person, campaign) : null;
        return new PersonEntry(person, level, List.copyOf(abilities), person.getTotalInjurySeverity(), fatigue,
              search.toString().toLowerCase(Locale.ROOT));
    }

    private static UnitEntry describe(Campaign campaign, Unit unit) {
        Entity entity = unit.getEntity();
        String typeName = UnitType.getTypeDisplayableName(entity.getUnitType());
        Formation formation = campaign.getPlayerForce().getFormationFor(unit);
        String formationName = (formation == null) ? "" : formation.getFullName();
        String search = (unit.getName() + ' ' + typeName + ' ' + formationName).toLowerCase(Locale.ROOT);
        int damageState = unit.getDamageState();
        return new UnitEntry(unit, typeName, formationName, damageState, Unit.getDamageStateName(damageState),
              search);
    }

    /** @return the units, by formation and then name */
    List<UnitEntry> units() {
        return List.copyOf(units.values());
    }

    /** @return the people, best first */
    List<PersonEntry> people() {
        return List.copyOf(people.values());
    }

    @Nullable UnitEntry entryFor(Unit unit) {
        return units.get(unit.getId());
    }

    @Nullable PersonEntry entryFor(Person person) {
        return people.get(person.getId());
    }

    AssignmentPlan plan() {
        return plan;
    }

    /** @return the campaign's free temporary crew of each role, before any staged change; 0 where it uses none */
    ToIntFunction<PersonnelRole> tempCrewPool() {
        return tempCrewPool;
    }

    /** @return best first, by experience level and then name; {@link AutoFill} uses the same order */
    Comparator<Person> ranking() {
        return ranking;
    }

    boolean isUseTransfers() {
        return useTransfers;
    }

    LocalDate today() {
        return today;
    }

    String locationName() {
        return locationName;
    }
}
