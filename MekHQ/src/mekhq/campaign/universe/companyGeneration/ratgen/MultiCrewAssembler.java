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
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;

/**
 * Builds the {@link Person} crew for a {@link Unit}, sized to the unit's actual seat count, and attaches
 * each Person to the unit via {@link Unit#addDriver}, {@link Unit#addGunner}, or
 * {@link Unit#addPilotOrSoldier}.
 *
 * <p>This is the sole owner of crew-size logic. Phase 1 covers the Mek path only (single pilot via
 * {@code addPilotOrSoldier}); Phase 2 fills in vehicles, VTOLs, infantry, BA, aero, and vessels with
 * full multi-person crew assembly.</p>
 *
 * <p>Crew skills are decided by MekHQ's {@link AbstractPersonnelGenerator} based on the campaign's
 * configured skill setting; the {@link CrewDescriptor} commander's name is overridden onto the first
 * Person via {@link CrewDescriptorAdapter}. The descriptor's gunnery/piloting numbers are not directly
 * applied in Phase 1; revisit in Phase 2 if alignment drifts noticeably.</p>
 */
public final class MultiCrewAssembler {

    private static final MMLogger LOGGER = MMLogger.create(MultiCrewAssembler.class);

    private MultiCrewAssembler() {
        // utility class
    }

    /**
     * Generates and attaches the appropriate number of crew Persons to the given unit.
     *
     * @param unit       the MekHQ Unit to crew; must already wrap an Entity
     * @param commander  the commander descriptor from the {@code ForceDescriptor.getCo()} for naming;
     *                   may be null (then the commander gets a fully random name)
     * @param campaign   the campaign that owns the unit and supplies the personnel generator
     * @param overrideName when true, the descriptor's name replaces MekHQ's random name on the commander
     * @return the list of Persons created and attached, with the commander first
     */
    public static List<Person> assemble(Unit unit, CrewDescriptor commander, Campaign campaign,
          boolean overrideName) {
        List<Person> crew = new ArrayList<>();
        // Phase 1: Mek-only single-pilot path. Phase 2 will branch on entity.defaultCrewType() etc.
        int unitType = unit.getEntity() == null ? 0 : unit.getEntity().getUnitType();
        PersonnelRole primary = PersonnelRoleResolver.primaryRole(unitType);
        LOGGER.info("[CompanyGen]     MultiCrewAssembler.assemble unitType={} primaryRole={} overrideName={} hasCommander={}",
              unitType, primary, overrideName, commander != null);
        Person pilot = campaign.newPerson(primary);
        CrewDescriptorAdapter.apply(commander, pilot, overrideName);
        unit.addPilotOrSoldier(pilot);
        crew.add(pilot);
        LOGGER.info("[CompanyGen]       crew[0]={} role={} attached to Unit",
              pilot.getFullName(), pilot.getPrimaryRole());
        return crew;
    }
}
