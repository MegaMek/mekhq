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
 * units sized to the faction's echelon: Inner Sphere fills platoons (28), then squads (7), then
 * single-person units; Clan fills Points (25), then squads (5), then single-person units. Top-tier
 * units (platoons / Points) roll up into companies (3 platoons) or Stars (5 Points), mirroring how
 * the combat force is organized.</p>
 *
 * <p>Carriers hold a single profession: each role (mek tech, astech, doctor, medic, each administrator
 * type) packs into its own carriers, and the last carrier of a role is left understaffed rather than
 * topped up with a different role.</p>
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

    // Echelon sizes (troopers per carrier, and top-tier carriers per roll-up formation).
    private static final int IS_PLATOON_SIZE = 28;
    private static final int IS_SQUAD_SIZE = 7;
    private static final int IS_PLATOONS_PER_COMPANY = 3;
    private static final int CLAN_POINT_SIZE = 25;
    private static final int CLAN_SQUAD_SIZE = 5;
    private static final int CLAN_POINTS_PER_STAR = 5;

    private SupportPersonnelToTOE() {
        // utility class
    }

    /**
     * Immutable per-faction echelon description: the three carrier unit tiers (top / squad / single),
     * their trooper capacities, and how many top-tier units roll up into one company / Star formation.
     */
    record EchelonProfile(String topUnitName, int topUnitSize, String squadUnitName,
          int squadUnitSize, int topUnitsPerRollup, String rollupLabel,
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

        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        Formation hqFormation = AddSupportUnitsToTOE.getHqFormation(campaign);
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
              ? List.of(new VehicleSpec(SupportUnitGenerator.MEDICAL_UNIT, 1))
              : List.of();

        LOGGER.info("[CompanyGen][SupportTOE] === Support Command: {} staff (maintenance={} medical={} command={}, clan={}) ===",
              maintenance.size() + medical.size() + command.size(),
              maintenance.size(), medical.size(), command.size(), useClanStructure);

        organizeSection(campaign, supportCommand, label("maintenance"), maintenance, profile,
              useClanStructure, maintenanceVehicles, false);
        organizeSection(campaign, supportCommand, label("medical"), medical, profile,
              useClanStructure, medicalVehicles, false);
        // Command pools its four administrator roles into shared squads (overflow to a platoon at full
        // strength) rather than one tiny carrier per role.
        organizeSection(campaign, supportCommand, label("command"), command, profile,
              useClanStructure, List.of(), true);

        LOGGER.info("[CompanyGen][SupportTOE] organized support staff: maintenance={} medical={} command={} (clan={})",
              maintenance.size(), medical.size(), command.size(), useClanStructure);
    }

    static EchelonProfile innerSphereProfile() {
        return new EchelonProfile(IS_PLATOON_UNIT, IS_PLATOON_SIZE, IS_SQUAD_UNIT, IS_SQUAD_SIZE,
              IS_PLATOONS_PER_COMPANY, label("company"),
              FormationLevel.BATTALION, FormationLevel.COMPANY);
    }

    static EchelonProfile clanProfile() {
        return new EchelonProfile(CLAN_POINT_UNIT, CLAN_POINT_SIZE, CLAN_SQUAD_UNIT, CLAN_SQUAD_SIZE,
              CLAN_POINTS_PER_STAR, label("star"),
              FormationLevel.BINARY_OR_TRINARY, FormationLevel.STAR_OR_NOVA);
    }

    /**
     * Packs one section's personnel into carrier units and files them under a new section formation.
     * Each profession (primary role) is packed on its own so a carrier never mixes roles: full
     * top-tier carriers first, then one right-sized final carrier for the remainder (understaffed if
     * it does not fill its tier). Top-tier carriers across the section roll up into company / Star
     * formations; squad and single-person carriers hang off the section as detachments. Each carrier
     * is fluff-named after the profession it carries (e.g. "Support Squad - Command").
     */
    private static void organizeSection(Campaign campaign, Formation supportCommand, String sectionLabel,
          List<Person> people, EchelonProfile profile, boolean useClanStructure, List<VehicleSpec> vehicles,
          boolean poolProfessions) {
        if (people.isEmpty()) {
            return;
        }

        Formation section = createFormation(campaign, sectionLabel, FormationType.SUPPORT, supportCommand,
              profile.sectionLevel());

        // Crew this section's capability vehicles from the front of its staff pool, then pack whoever
        // is left into carriers. This keeps the vehicle's crew from being double-generated.
        List<Person> pool = new ArrayList<>(people);
        int consumed = 0;
        for (VehicleSpec vehicle : vehicles) {
            consumed += addCapabilityVehicles(campaign, section, vehicle, pool, consumed);
        }
        List<Person> remaining = pool.subList(consumed, pool.size());

        // Pooled sections (Command) mix their roles into shared carriers with a squad-overflow rule;
        // everyone else packs one profession per carrier.
        List<CarrierSpec> packed = poolProfessions
              ? packPool(remaining, profile, sectionLabel)
              : packByProfession(remaining, profile, useClanStructure);

        List<CarrierSpec> topTierCarriers = new ArrayList<>();
        List<CarrierSpec> detachmentCarriers = new ArrayList<>();
        for (CarrierSpec spec : packed) {
            (spec.topTier() ? topTierCarriers : detachmentCarriers).add(spec);
        }

        // Top-tier carriers (platoons / Points) roll up into company / Star formations.
        int placed = 0;
        int rollupFormations = ceilDiv(topTierCarriers.size(), profile.topUnitsPerRollup());
        for (int rollupIndex = 0; rollupIndex < rollupFormations; rollupIndex++) {
            Formation rollup = createFormation(campaign, profile.rollupLabel() + " " + (rollupIndex + 1),
                  FormationType.SUPPORT, section, profile.rollupLevel());
            int inThisRollup = Math.min(profile.topUnitsPerRollup(), topTierCarriers.size() - placed);
            for (int unitIndex = 0; unitIndex < inThisRollup; unitIndex++) {
                fileCarrier(campaign, rollup, topTierCarriers.get(placed));
                placed++;
            }
        }

        // Squad and single-person carriers hang directly off the section as detachments.
        for (CarrierSpec spec : detachmentCarriers) {
            fileCarrier(campaign, section, spec);
        }

        LOGGER.info("[CompanyGen][SupportTOE] {}: {} staff, {} crewed onto vehicles, {} rollup(s), carriers=[{}]",
              sectionLabel, people.size(), consumed, rollupFormations,
              summarizeCarriers(topTierCarriers, detachmentCarriers));
    }

    /** Renders the section's carriers as "profession unitName(crew)" entries for the generation log. */
    private static String summarizeCarriers(List<CarrierSpec> topTierCarriers, List<CarrierSpec> detachmentCarriers) {
        List<String> parts = new ArrayList<>();
        for (CarrierSpec spec : topTierCarriers) {
            parts.add(spec.professionLabel() + " " + spec.unitName() + "(" + spec.crew().size() + ")");
        }
        for (CarrierSpec spec : detachmentCarriers) {
            parts.add(spec.professionLabel() + " " + spec.unitName() + "(" + spec.crew().size() + ")");
        }
        return String.join(", ", parts);
    }

    /**
     * Creates each of {@code vehicle}'s crewless copies, crews it from the section pool starting at
     * {@code startIndex} (up to the vehicle's full crew size, understaffed if the pool runs out), and
     * files it under the section. Returns the number of people consumed as crew. Stops early once the
     * pool is exhausted.
     */
    private static int addCapabilityVehicles(Campaign campaign, Formation section, VehicleSpec vehicle,
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
                campaign.addUnitToFormation(unit, section.getId());
                LOGGER.info("[CompanyGen][SupportTOE]   {} vehicle '{}' crewed with {}/{} staff",
                      section.getName(), vehicle.unitName(), crewSize, crewNeeded);
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

    /** Packs each profession in {@code people} into its own carriers via {@link #packPool}. */
    private static List<CarrierSpec> packByProfession(List<Person> people, EchelonProfile profile,
          boolean useClanStructure) {
        List<CarrierSpec> specs = new ArrayList<>();
        for (Map.Entry<PersonnelRole, List<Person>> roleEntry : groupByPrimaryRole(people).entrySet()) {
            String professionLabel = roleEntry.getKey().getLabel(useClanStructure);
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
            return specs;
        }

        // Promote the remainder to a platoon / Point once it exceeds (squadsPerTopUnit - 0.5) squads
        // - 3.5 squads for a 4-squad IS platoon, 4.5 for a 5-squad Clan Point. Integer form of
        // remainder / squadSize > squadsPerTopUnit - 0.5.
        if (remainder * 2 > 2 * profile.topUnitSize() - profile.squadUnitSize()) {
            specs.add(new CarrierSpec(profile.topUnitName(), crewSlice(people, index, remainder), true, label));
            return specs;
        }

        while (remainder > 0) {
            int crewSize = Math.min(profile.squadUnitSize(), remainder);
            specs.add(new CarrierSpec(profile.squadUnitName(), crewSlice(people, index, crewSize), false, label));
            index += crewSize;
            remainder -= crewSize;
        }
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
        }
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
        return formation;
    }

    private static int ceilDiv(int dividend, int divisor) {
        return (dividend + divisor - 1) / divisor;
    }

    private static String label(String key) {
        return getTextAt(RESOURCE_BUNDLE, "SupportPersonnelToTOE." + key + ".label");
    }
}
