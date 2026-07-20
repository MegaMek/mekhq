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
package mekhq.campaign.universe.companyGeneration;

import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.FormationLevel;
import mekhq.campaign.force.FormationType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;

/**
 * Organizes freshly generated support personnel into the campaign's TOE.
 *
 * <p>Support staff (techs, astechs, doctors, medics, administrators) are grouped into three
 * segregated sections - Maintenance, Medical, and Command - under a "Support Command" formation that
 * hangs off the campaign HQ. Each section's personnel become the soldiers of infantry-style carrier
 * units sized to the faction's echelon: Inner Sphere fills platoons (28), then squads (7); Clan fills
 * Points (25), then squads (5).</p>
 *
 * <p>Carriers hold a single profession: each role (mek tech, astech, doctor, medic, each administrator
 * type) packs into its own carriers, and the last carrier of a role is left understaffed rather than
 * topped up with a different role. Each profession's carriers then gather into a company / Star
 * formation named for the profession ("MekTech Company"); a profession that yields a single carrier
 * hangs that lone platoon / squad directly under the section. Capability vehicles (recovery, MASH)
 * form their own function-named company. So a section reads as a battalion of profession companies,
 * mirroring how the combat force is organized.</p>
 *
 * <p>The support personnel keep their real duties: assigning a tech as a carrier's soldier records
 * them in the TOE but does not remove them from the maintenance pool, because tech eligibility keys
 * off role and available minutes, not crew assignment.</p>
 *
 * @author Illiani
 * @since 0.51.0
 */
public final class SupportPersonnelToTOE {
    private static final MMLogger LOGGER = MMLogger.create(SupportPersonnelToTOE.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.SupportPersonnelToTOE";

    // Carrier unit names (looked up by MekSummaryCache by <Name>; see
    // mm-data/data/mekfiles/infantry/Support Units). Filenames differ from these names.
    private static final String IS_PLATOON_UNIT = "Support Platoon";
    private static final String IS_SQUAD_UNIT = "Support Squad";
    private static final String CLAN_POINT_UNIT = "Clan Support Point";
    private static final String CLAN_SQUAD_UNIT = "Clan Support Squad";

    // Echelon sizes (troopers per carrier).
    private static final int IS_PLATOON_SIZE = 28;
    private static final int IS_SQUAD_SIZE = 7;
    private static final int CLAN_POINT_SIZE = 25;
    private static final int CLAN_SQUAD_SIZE = 5;

    private SupportPersonnelToTOE() {
        // utility class
    }

    /**
     * Immutable per-faction echelon description: the two carrier unit tiers (platoon / squad) with
     * their trooper capacities, the roll-up formation label ("Company" / "Star"), and the formation
     * levels used for the section and its companies.
     */
    record EchelonProfile(String topUnitName, int topUnitSize, String squadUnitName,
          int squadUnitSize, String rollupLabel,
          FormationLevel sectionLevel, FormationLevel rollupLevel) {}

    /**
     * Organizes {@code supportPersonnel} into the campaign TOE. No-op when the list is empty or holds
     * no recognizable support roles.
     *
     * @param campaign          the active campaign that owns the TOE and hangar
     * @param supportPersonnel  the support Persons to place (from {@code SupportPersonnelGenerator});
     *                          pooled astechs / medics are not Persons and never appear here
     * @param useClanStructure  {@code true} to use Clan echelons (5 / 25 / Star), {@code false} for
     *                          Inner Sphere (7 / 28 / Company)
     */
    public static void organize(Campaign campaign, List<Person> supportPersonnel, boolean useClanStructure) {
        if (campaign == null || supportPersonnel == null || supportPersonnel.isEmpty()) {
            return;
        }

        List<Person> maintenance = new ArrayList<>();
        List<Person> medical = new ArrayList<>();
        List<Person> command = new ArrayList<>();
        for (Person person : supportPersonnel) {
            if (person == null) {
                continue;
            }
            PersonnelRole role = person.getPrimaryRole();
            if (role.isTech() || role.isAstech()) {
                maintenance.add(person);
            } else if (role.isMedicalStaff()) {
                medical.add(person);
            } else if (role.isAdministrator()) {
                command.add(person);
            }
            // Any other role is not a support section we organize; leave it in the roster untouched.
        }

        if (maintenance.isEmpty() && medical.isEmpty() && command.isEmpty()) {
            return;
        }

        LOGGER.info("[CompanyGen][SupportTOE] organize START: {} support persons -> maintenance={} medical={} command={} (clan={}); role histogram: {}",
              supportPersonnel.size(), maintenance.size(), medical.size(), command.size(), useClanStructure,
              roleHistogram(supportPersonnel));

        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        Formation hqFormation = AddSupportUnitsToTOE.getHqFormation(campaign);
        LOGGER.info("[CompanyGen][SupportTOE] HQ formation '{}'(id={})", hqFormation.getName(), hqFormation.getId());
        FormationLevel commandLevel = useClanStructure ? FormationLevel.CLUSTER : FormationLevel.REGIMENT;
        Formation supportCommand = createFormation(campaign, label("supportCommand"),
              FormationType.SUPPORT, hqFormation, commandLevel);

        EchelonProfile profile = useClanStructure ? clanProfile() : innerSphereProfile();

        // Capability vehicles that overlap a personnel section join that section and are crewed from
        // its own staff instead of generating fresh crew, so medical/salvage personnel are not
        // doubled. Vehicles with no matching section (canteen, convoy, security) stay standalone and
        // are generated by SupportUnitGenerator in the pipeline.
        List<VehicleSpec> maintenanceVehicles = campaignOptions.isUseCamOpsSalvage()
              ? List.of(new VehicleSpec(SupportUnitGenerator.SALVAGE_UNIT, SupportUnitGenerator.scaledCount(campaign)))
              : List.of();
        List<VehicleSpec> medicalVehicles = campaignOptions.isUseMASHTheatres()
              ? List.of(new VehicleSpec(SupportUnitGenerator.MEDICAL_UNIT, SupportUnitGenerator.medicalUnitCount(campaign)))
              : List.of();

        LOGGER.info("[CompanyGen][SupportTOE] === Support Command: {} staff (maintenance={} medical={} command={}, clan={}) ===",
              maintenance.size() + medical.size() + command.size(),
              maintenance.size(), medical.size(), command.size(), useClanStructure);

        // Within each section, staff are grouped by profession into companies (e.g. "MekTech Company");
        // capability vehicles get their own company named for their function ("Recovery",
        // "Field Hospital"). Command has no capability vehicles, so its label is unused.
        organizeSection(campaign, supportCommand, label("maintenance"), maintenance, profile,
              useClanStructure, maintenanceVehicles, label("recovery"));
        organizeSection(campaign, supportCommand, label("medical"), medical, profile,
              useClanStructure, medicalVehicles, label("fieldHospital"));
        organizeSection(campaign, supportCommand, label("command"), command, profile,
              useClanStructure, List.of(), label("command"));

        LOGGER.info("[CompanyGen][SupportTOE] organized support staff: maintenance={} medical={} command={} (clan={})",
              maintenance.size(), medical.size(), command.size(), useClanStructure);
    }

    static EchelonProfile innerSphereProfile() {
        return new EchelonProfile(IS_PLATOON_UNIT, IS_PLATOON_SIZE, IS_SQUAD_UNIT, IS_SQUAD_SIZE,
              label("company"), FormationLevel.BATTALION, FormationLevel.COMPANY);
    }

    static EchelonProfile clanProfile() {
        return new EchelonProfile(CLAN_POINT_UNIT, CLAN_POINT_SIZE, CLAN_SQUAD_UNIT, CLAN_SQUAD_SIZE,
              label("star"), FormationLevel.BINARY_OR_TRINARY, FormationLevel.STAR_OR_NOVA);
    }

    /**
     * Packs one section's personnel into carrier units and files them under the section, grouped by
     * profession. Each profession (primary role) is packed on its own into platoon / squad carriers.
     * A profession that yields two or more carriers gets its own company named for it (e.g.
     * "MekTech Company") gathering its platoons and squads; a profession that yields a single carrier
     * hangs that lone platoon (or squad) directly under the battalion-level section, as a small
     * detachment that does not warrant a company of its own. The section's capability vehicles
     * (recovery / MASH), crewed from the front of the staff pool, get their own function-named company
     * ("Recovery Company", "Field Hospital Company"). Each carrier is fluff-named after the profession
     * it carries (e.g. "Support Squad - MekTech").
     */
    private static void organizeSection(Campaign campaign, Formation supportCommand, String sectionLabel,
          List<Person> people, EchelonProfile profile, boolean useClanStructure, List<VehicleSpec> vehicles,
          String vehicleCompanyLabel) {
        if (people.isEmpty()) {
            return;
        }

        Formation section = createFormation(campaign, sectionLabel, FormationType.SUPPORT, supportCommand,
              profile.sectionLevel());

        // Capability vehicles get their own company under the section, crewed from the front of the
        // staff pool so their crew is not double-generated.
        List<Person> pool = new ArrayList<>(people);
        int consumed = 0;
        if (!vehicles.isEmpty()) {
            Formation vehicleCompany = createFormation(campaign,
                  vehicleCompanyLabel + " " + profile.rollupLabel(),
                  FormationType.SUPPORT, section, profile.rollupLevel());
            for (VehicleSpec vehicle : vehicles) {
                consumed += addCapabilityVehicles(campaign, vehicleCompany, vehicle, pool, consumed);
            }
        }
        List<Person> remaining = pool.subList(consumed, pool.size());

        // Group each profession's carriers into its own company named for the profession. A profession
        // that produces just one carrier hangs that lone platoon/squad directly under the section.
        Map<String, List<CarrierSpec>> carriersByProfession = new LinkedHashMap<>();
        for (CarrierSpec spec : packByProfession(remaining, profile, useClanStructure)) {
            carriersByProfession.computeIfAbsent(spec.professionLabel(), key -> new ArrayList<>()).add(spec);
        }

        int professionCompanies = 0;
        int loneCarriers = 0;
        for (Map.Entry<String, List<CarrierSpec>> entry : carriersByProfession.entrySet()) {
            List<CarrierSpec> carriers = entry.getValue();
            if (carriers.size() == 1) {
                fileCarrier(campaign, section, carriers.get(0));
                loneCarriers++;
                continue;
            }
            Formation company = createFormation(campaign, entry.getKey() + " " + profile.rollupLabel(),
                  FormationType.SUPPORT, section, profile.rollupLevel());
            for (CarrierSpec spec : carriers) {
                fileCarrier(campaign, company, spec);
            }
            professionCompanies++;
        }

        LOGGER.info("[CompanyGen][SupportTOE] {}: {} staff, {} crewed onto vehicles, {} profession company(ies), {} lone carrier(s) under the section",
              sectionLabel, people.size(), consumed, professionCompanies, loneCarriers);
    }

    /**
     * Creates each of {@code vehicle}'s crewless copies, crews it from the staff pool starting at
     * {@code startIndex} (up to the vehicle's full crew size, understaffed if the pool runs out), and
     * files it under {@code parent} (the section's capability-vehicle company). Returns the number of
     * people consumed as crew. Stops early once the pool is exhausted.
     */
    private static int addCapabilityVehicles(Campaign campaign, Formation parent, VehicleSpec vehicle,
          List<Person> pool, int startIndex) {
        MekSummary mekSummary = MekSummaryCache.getInstance().getMek(vehicle.unitName());
        if (mekSummary == null) {
            LOGGER.error("Cannot find capability vehicle entry for {}", vehicle.unitName());
            return 0;
        }

        int consumed = 0;
        for (int index = 0; index < vehicle.count() && (startIndex + consumed) < pool.size(); index++) {
            try {
                // allowNewPilots = false: crew comes from the generated staff, not fresh personnel.
                Unit unit = campaign.addNewUnit(mekSummary.loadEntity(), false, 0);
                int crewNeeded = Math.max(1, unit.getFullCrewSize());
                int available = pool.size() - startIndex - consumed;
                int crewSize = Math.min(crewNeeded, available);
                for (int crewIndex = 0; crewIndex < crewSize; crewIndex++) {
                    unit.addPilotOrSoldier(pool.get(startIndex + consumed + crewIndex));
                }
                consumed += crewSize;
                campaign.addUnitToFormation(unit, parent.getId());
                LOGGER.info("[CompanyGen][SupportTOE]     {} vehicle '{}' unitId={} crewed with {}/{} staff",
                      parent.getName(), vehicle.unitName(), unit.getId(), crewSize, crewNeeded);
            } catch (Exception exception) {
                LOGGER.error(exception, "Unable to load capability vehicle {}: {}", vehicle.unitName(),
                      mekSummary.getSourceFile());
            }
        }
        return consumed;
    }

    /** A capability vehicle to place into a section, crewed from that section's staff. */
    record VehicleSpec(String unitName, int count) {}

    /**
     * Groups people by their primary role, preserving first-seen role order for deterministic output.
     */
    static Map<PersonnelRole, List<Person>> groupByPrimaryRole(List<Person> people) {
        Map<PersonnelRole, List<Person>> byRole = new LinkedHashMap<>();
        for (Person person : people) {
            byRole.computeIfAbsent(person.getPrimaryRole(), role -> new ArrayList<>()).add(person);
        }
        return byRole;
    }

    /** Renders a {@code role=count} histogram of the given people for the generation log. */
    private static String roleHistogram(List<Person> people) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<PersonnelRole, List<Person>> entry : groupByPrimaryRole(people).entrySet()) {
            parts.add(entry.getKey().name() + "=" + entry.getValue().size());
        }
        return String.join(", ", parts);
    }

    /** Packs each profession in {@code people} into its own carriers via {@link #packPool}. */
    private static List<CarrierSpec> packByProfession(List<Person> people, EchelonProfile profile,
          boolean useClanStructure) {
        List<CarrierSpec> specs = new ArrayList<>();
        for (Map.Entry<PersonnelRole, List<Person>> roleEntry : groupByPrimaryRole(people).entrySet()) {
            String professionLabel = roleEntry.getKey().getLabel(useClanStructure);
            LOGGER.info("[CompanyGen][SupportTOE]   packByProfession role='{}' count={}",
                  professionLabel, roleEntry.getValue().size());
            specs.addAll(packPool(roleEntry.getValue(), profile, professionLabel));
        }
        return specs;
    }

    /**
     * Packs a pool of people into carriers, filling smallest-to-largest: a full platoon / Point unit
     * for each whole platoon's worth, then the remainder as squads (the last understaffed) - unless
     * the remainder is more than 3.5 squads' worth (4.5 for Clan), in which case it becomes a single
     * understaffed platoon / Point rather than a near-full stack of squads. Never a single-person
     * carrier. Platoon / Point carriers are top-tier (they roll up); squads hang off the section.
     */
    static List<CarrierSpec> packPool(List<Person> people, EchelonProfile profile, String label) {
        List<CarrierSpec> specs = new ArrayList<>();
        int total = people.size();
        int index = 0;

        int fullTopUnits = total / profile.topUnitSize();
        for (int unitIndex = 0; unitIndex < fullTopUnits; unitIndex++) {
            specs.add(new CarrierSpec(profile.topUnitName(),
                  crewSlice(people, index, profile.topUnitSize()), true, label));
            index += profile.topUnitSize();
        }

        int remainder = total - index;
        if (remainder == 0) {
            LOGGER.info("[CompanyGen][SupportTOE]     packPool '{}': total={} -> {} full {} (no remainder)",
                  label, total, fullTopUnits, profile.topUnitName());
            return specs;
        }

        // Promote the remainder to a platoon / Point once it exceeds (squadsPerTopUnit - 0.5) squads
        // - 3.5 squads for a 4-squad IS platoon, 4.5 for a 5-squad Clan Point. Integer form of
        // remainder / squadSize > squadsPerTopUnit - 0.5.
        if (remainder * 2 > 2 * profile.topUnitSize() - profile.squadUnitSize()) {
            specs.add(new CarrierSpec(profile.topUnitName(), crewSlice(people, index, remainder), true, label));
            LOGGER.info("[CompanyGen][SupportTOE]     packPool '{}': total={} -> {} full {} + 1 understaffed {} (remainder {} promoted to top tier)",
                  label, total, fullTopUnits, profile.topUnitName(), profile.topUnitName(), remainder);
            return specs;
        }

        int squadCount = 0;
        while (remainder > 0) {
            int crewSize = Math.min(profile.squadUnitSize(), remainder);
            specs.add(new CarrierSpec(profile.squadUnitName(), crewSlice(people, index, crewSize), false, label));
            index += crewSize;
            remainder -= crewSize;
            squadCount++;
        }
        LOGGER.info("[CompanyGen][SupportTOE]     packPool '{}': total={} -> {} full {} + {} detachment {}(s)",
              label, total, fullTopUnits, profile.topUnitName(), squadCount, profile.squadUnitName());
        return specs;
    }

    private static List<Person> crewSlice(List<Person> pool, int fromIndex, int count) {
        return new ArrayList<>(pool.subList(fromIndex, fromIndex + count));
    }

    /**
     * A carrier to create: which unit to load, who crews it, whether it counts for roll-up, and the
     * profession label stamped on it as a fluff name (so the TOE reads "Support Squad - Command").
     */
    record CarrierSpec(String unitName, List<Person> crew, boolean topTier, String professionLabel) {}

    /**
     * Creates the carrier for {@code spec} and files it under {@code parent}. If the unit fails to
     * load, its people stay in the roster (already recruited) and are simply absent from the TOE.
     */
    private static void fileCarrier(Campaign campaign, Formation parent, CarrierSpec spec) {
        Unit unit = createCarrierUnit(campaign, spec);
        if (unit != null) {
            campaign.addUnitToFormation(unit, parent.getId());
            LOGGER.info("[CompanyGen][SupportTOE]     filed carrier '{}' ({}) unitId={} crew={} topTier={} under '{}'(id={})",
                  spec.unitName(), spec.professionLabel(), unit.getId(), spec.crew().size(), spec.topTier(),
                  parent.getName(), parent.getId());
        } else {
            // The unit failed to load, so its crew never made it into the TOE and stay loose in the
            // roster - the classic "orphaned support personnel" symptom. Log loudly with the crew so a
            // playtest can see exactly who was dropped and which carrier name failed to resolve.
            LOGGER.warn("[CompanyGen][SupportTOE]     carrier '{}' ({}) FAILED to load; {} crew left unassigned (orphans): {}",
                  spec.unitName(), spec.professionLabel(), spec.crew().size(), crewNames(spec.crew()));
        }
    }

    /** Comma-joined full names of the crew, for orphan diagnostics. */
    private static String crewNames(List<Person> crew) {
        List<String> names = new ArrayList<>();
        for (Person person : crew) {
            names.add(person.getFullName());
        }
        return String.join(", ", names);
    }

    /**
     * Loads the carrier unit from the summary cache, adds it to the campaign without generating crew,
     * assigns each crew Person as one of its soldiers, and fluff-names it after the profession it
     * carries. Returns {@code null} if the unit cannot be found or loaded.
     */
    private static Unit createCarrierUnit(Campaign campaign, CarrierSpec spec) {
        MekSummary mekSummary = MekSummaryCache.getInstance().getMek(spec.unitName());
        if (mekSummary == null) {
            LOGGER.error("Cannot find carrier unit entry for {}", spec.unitName());
            return null;
        }

        try {
            // allowNewPilots = false: the carrier arrives crewless so the support staff fill it.
            Unit unit = campaign.addNewUnit(mekSummary.loadEntity(), false, 0);
            unit.setFluffName(spec.professionLabel());
            for (Person person : spec.crew()) {
                ensureInfantrySkill(person);
                unit.addPilotOrSoldier(person);
            }
            return unit;
        } catch (Exception exception) {
            LOGGER.error(exception, "Unable to load carrier unit {}: {}", spec.unitName(),
                  mekSummary.getSourceFile());
            return null;
        }
    }

    /**
     * Support staff carry no infantry weapon skill, so a conventional-infantry carrier would count
     * them as zero effective troopers (strength and BV of 0). Give each a baseline Small Arms skill
     * so the entity registers its full crew as troopers.
     */
    private static void ensureInfantrySkill(Person person) {
        if (!person.hasSkill(SkillType.S_SMALL_ARMS)) {
            person.addSkill(SkillType.S_SMALL_ARMS, 0, 0);
        }
    }

    private static Formation createFormation(Campaign campaign, String name, FormationType type,
          Formation parent, FormationLevel level) {
        Formation formation = new Formation(name);
        campaign.addFormation(formation, parent); // must precede unit assignment
        formation.setFormationType(type, true);
        // Set an explicit level so FormationIconBuilder can pick a shape for the support formations.
        formation.setFormationLevel(level);
        LOGGER.info("[CompanyGen][SupportTOE]   createFormation name='{}' id={} type={} level={} under '{}'(id={})",
              name, formation.getId(), type, level, parent == null ? "null" : parent.getName(),
              parent == null ? -1 : parent.getId());
        return formation;
    }

    private static String label(String key) {
        return getTextAt(RESOURCE_BUNDLE, "SupportPersonnelToTOE." + key + ".label");
    }
}
