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
package mekhq.campaign.universe.companyGeneration.ratgen;

import java.util.ArrayList;
import java.util.List;

import megamek.client.ratgenerator.CrewDescriptor;
import megamek.common.units.Entity;
import megamek.common.units.Jumpship;
import megamek.common.units.SmallCraft;
import megamek.common.units.SpaceStation;
import megamek.common.units.UnitType;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;

/**
 * Builds the {@link Person} crew for a {@link Unit}, sized to the unit's actual seat count, and attaches
 * each Person to the unit through the appropriate {@code Unit.add*} method.
 *
 * <p>The seat count comes from MekHQ's {@code Unit.getTotalDriverNeeds()}, {@code getTotalGunnerNeeds()},
 * {@code getTotalCrewNeeds()}, and {@code canTakeNavigator()} — these wrap {@code Compute} and already
 * understand cockpit type, command consoles, tripod / superheavy variations, infantry squad size, BA
 * trooper count, etc. We don't reimplement that logic here.</p>
 *
 * <p>Role assignment per seat is delegated to {@link PersonnelRoleResolver}. The commander (descriptor
 * from {@code ForceDescriptor.getCo()}) is the first Person; their name is overridden from the
 * descriptor when {@code overrideName} is true. All other crew are randomly named by MekHQ's standard
 * personnel generator.</p>
 */
public final class MultiCrewAssembler {

    private static final MMLogger LOGGER = MMLogger.create(MultiCrewAssembler.class);

    private MultiCrewAssembler() {
        // utility class
    }

    /**
     * Generates and attaches the appropriate Persons to the given unit. The commander is always first
     * in the returned list.
     *
     * @param unit         the MekHQ Unit to crew; must already wrap an Entity
     * @param commander    the commander descriptor from {@code ForceDescriptor.getCo()}; may be null
     *                     (then the commander is fully randomly named)
     * @param campaign     the campaign that owns the unit and supplies the personnel generator
     * @param overrideName when true, the descriptor's name replaces MekHQ's random name on the commander
     * @return the list of Persons created and attached, with the commander first
     */
    public static List<Person> assemble(Unit unit, CrewDescriptor commander, Campaign campaign,
          boolean overrideName) {
        List<Person> crew = new ArrayList<>();
        Entity entity = unit.getEntity();
        if (entity == null) {
            LOGGER.warn("[CompanyGen]     MultiCrewAssembler.assemble: unit has no entity, skipping");
            return crew;
        }
        int unitType = entity.getUnitType();
        PersonnelRole primary = PersonnelRoleResolver.primaryRole(unitType, entity);
        PersonnelRole gunner = PersonnelRoleResolver.gunnerRole(unitType);

        // Every crew member of a single leaf shares the leaf's CrewDescriptor — same gunnery /
        // piloting target numbers, same experience class. Only the commander adopts the descriptor's
        // name; the rest are randomly named but skill-equal. We always pass `commander` through to
        // newPerson() and let CrewDescriptorAdapter decide whether to apply the name based on the
        // overrideName flag.

        // Single-pilot path: Meks, ProtoMeks, single-seat Aero / ConvFighter / LAM. usesSoloPilot()
        // returns true exactly for these. Infantry / BA are NOT solo-pilot but share addPilotOrSoldier
        // as the attach method, so we route them through their own branch below.
        if (unit.usesSoloPilot()) {
            Person pilot = newPerson(campaign, primary, commander, entity, overrideName);
            unit.addPilotOrSoldier(pilot);
            crew.add(pilot);
            LOGGER.info("[CompanyGen]     MultiCrewAssembler.assemble unitType={} solo-pilot role={} crew=[{}]",
                  unitType, primary, pilot.getFullName());
            return crew;
        }

        // Infantry / Battle Armor: addPilotOrSoldier per trooper, sized by the unit's full crew count.
        // The commander is one of the squad; remaining troopers share the skill profile but get
        // random names.
        if (unit.usesSoldiers() || unit.isBattleArmor()) {
            int squadSize = Math.max(1, unit.getFullCrewSize());
            for (int i = 0; i < squadSize; i++) {
                boolean isCommander = (i == 0);
                Person trooper = newPerson(campaign, primary, commander, entity,
                      isCommander && overrideName);
                unit.addPilotOrSoldier(trooper);
                crew.add(trooper);
            }
            LOGGER.info("[CompanyGen]     MultiCrewAssembler.assemble unitType={} squad role={} size={}",
                  unitType, primary, crew.size());
            return crew;
        }

        // Multi-seat path: tanks, VTOLs, naval, multi-pilot aero, large craft. Drivers / gunners /
        // vessel crew / navigator each have their own attach method on Unit.
        int driverNeeds = unit.getTotalDriverNeeds();
        int gunnerNeeds = unit.getTotalGunnerNeeds();
        int vesselCrewNeeds = needsVesselCrew(entity) ? unit.getTotalCrewNeeds() : 0;
        boolean needsNavigator = unit.canTakeNavigator();
        int totalSeats = driverNeeds + gunnerNeeds + vesselCrewNeeds + (needsNavigator ? 1 : 0);
        LOGGER.info("[CompanyGen]     MultiCrewAssembler.assemble unitType={} multi-seat drivers={} gunners={} vesselCrew={} navigator={} total={}",
              unitType, driverNeeds, gunnerNeeds, vesselCrewNeeds, needsNavigator, totalSeats);

        // Drivers. Commander becomes the first driver when at least one driver seat exists.
        for (int i = 0; i < driverNeeds; i++) {
            boolean isCommander = crew.isEmpty();
            Person driver = newPerson(campaign, primary, commander, entity,
                  isCommander && overrideName);
            unit.addDriver(driver);
            crew.add(driver);
        }
        // Gunners.
        for (int i = 0; i < gunnerNeeds; i++) {
            boolean isCommander = crew.isEmpty();
            Person g = newPerson(campaign, gunner, commander, entity,
                  isCommander && overrideName);
            unit.addGunner(g);
            crew.add(g);
        }
        // Generic vessel crew.
        if (vesselCrewNeeds > 0) {
            PersonnelRole crewRole = PersonnelRoleResolver.vesselCrewRole();
            for (int i = 0; i < vesselCrewNeeds; i++) {
                Person c = newPerson(campaign, crewRole, commander, entity, false);
                unit.addVesselCrew(c);
                crew.add(c);
            }
        }
        // Navigator (JumpShip / WarShip, not SpaceStation).
        if (needsNavigator) {
            Person nav = newPerson(campaign, PersonnelRoleResolver.navigatorRole(), commander, entity,
                  false);
            unit.setNavigator(nav);
            crew.add(nav);
        }

        if (crew.isEmpty()) {
            // Fallback: if the seat math collapsed to zero (e.g. an unusual entity), give the unit a
            // single pilot so it isn't left uncrewed.
            Person fallback = newPerson(campaign, primary, commander, entity, overrideName);
            unit.addPilotOrSoldier(fallback);
            crew.add(fallback);
            LOGGER.warn("[CompanyGen]     MultiCrewAssembler.assemble unitType={} fell through to fallback single-pilot",
                  unitType);
        }

        LOGGER.info("[CompanyGen]     MultiCrewAssembler.assemble unitType={} attached {} persons; commander={}",
              unitType, crew.size(), crew.get(0).getFullName());
        return crew;
    }

    private static Person newPerson(Campaign campaign, PersonnelRole role, CrewDescriptor descriptor,
          Entity entity, boolean overrideName) {
        Person person = campaign.newPerson(role);
        if (descriptor != null) {
            CrewDescriptorAdapter.apply(descriptor, person, entity, overrideName);
        }
        return person;
    }

    /**
     * Vessel crew slots only apply to large craft. SmallCraft, DropShips, JumpShips, WarShips, and
     * SpaceStations all need them; everything else is driver+gunner (or solo pilot).
     */
    private static boolean needsVesselCrew(Entity entity) {
        if (entity instanceof SmallCraft || entity instanceof Jumpship || entity instanceof SpaceStation) {
            return true;
        }
        int ut = entity.getUnitType();
        return ut == UnitType.SMALL_CRAFT || ut == UnitType.DROPSHIP || ut == UnitType.JUMPSHIP
              || ut == UnitType.WARSHIP || ut == UnitType.SPACE_STATION;
    }
}
