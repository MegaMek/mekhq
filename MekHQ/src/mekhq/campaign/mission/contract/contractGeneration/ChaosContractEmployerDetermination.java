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
package mekhq.campaign.mission.contract.contractGeneration;

import static megamek.common.compute.Compute.d6;
import static megamek.common.compute.Compute.randomInt;
import static mekhq.campaign.mission.utilities.RandomFactionCamouflage.pickRandomCamouflage;
import static mekhq.campaign.universe.Faction.COMSTAR_FACTION_CODE;
import static mekhq.campaign.universe.Faction.MERCENARY_FACTION_CODE;
import static mekhq.campaign.universe.Faction.REBEL_FACTION_CODE;
import static mekhq.campaign.universe.Faction.WORD_OF_BLAKE_FACTION_CODE;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import jakarta.annotation.Nullable;
import megamek.common.icons.Camouflage;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.mission.contract.contractData.EmployerData;
import mekhq.campaign.mission.contract.contractGeneration.negotiationsAndNPCs.EmployerLiaison;
import mekhq.campaign.mission.contract.contractGeneration.negotiationsAndNPCs.EmployerNegotiator;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.RandomFactionGenerator;
import mekhq.campaign.universe.enums.HiringHallLevel;
import org.jspecify.annotations.NonNull;

public class ChaosContractEmployerDetermination {
    private static final MMLogger LOGGER = MMLogger.create(ChaosContractEmployerDetermination.class);

    private static final int COMSTAR_EMPLOYER_CHANCE = 100;
    private static final int WORD_OF_BLAKE_EMPLOYER_CHANCE = 40;

    /**
     * Aggregate factions (pirates, the Bandit Caste, rebels, mercenaries) and Commands (whose short name carries a dot)
     * do not plausibly issue contracts, so the pool-based employer pickers exclude them. Employer types that are
     * deliberately fielded by such a faction (rebellions, mercenary subcontracts) assign it directly rather than
     * drawing from the pool, so they are unaffected.
     */
    private static final Predicate<Faction> PLAUSIBLE_EMPLOYER =
          faction -> !faction.isAggregate() && !faction.isSubunit() && !faction.isMercenaryOrganization();

    public static @Nullable EmployerData getEmployerGenerationData(LocalDate currentDate, ILocation currentLocation,
          Campaign campaign, ContractSearchType searchType) {
        ChaosEmployerType type = determineEmployerType();

        boolean isMercenarySearch = searchType == ContractSearchType.MERCENARY;
        EmployerFactions employerFactions = determineEmployerFactions(type, currentDate, currentLocation,
              isMercenarySearch);
        if (employerFactions == null) {
            LOGGER.info("Failed to select an employer for current location. Contract generation failed");
            return null;
        }

        // A special employer may override the rolled type (ComStar fronts a CORPORATION), so use the effective type
        // from here on for the display name and the stored employer data.
        type = employerFactions.type();
        Faction employer = employerFactions.flavor();
        int currentYear = currentDate.getYear();
        String factionCode = employer.getShortName();
        String anchorFactionCode = employerFactions.anchor().getShortName();
        Faction sponsor = employerFactions.sponsor();
        String sponsorFactionCode = sponsor == null ? null : sponsor.getShortName();
        // Non-system-owner employers (mercenary commands, corporations, and civilian rebel/militia/business
        // organizations) field a generic, landless flavor faction, so they get their own generated name. System
        // owners, planetary governments, and nobles keep their faction's own name.
        String generatedName = type.generateEmployerName();
        String displayName = (generatedName != null) ? generatedName : employer.getFullName(currentYear);

        Planet currentPlanet = currentLocation.getPlanet();
        if (currentPlanet == null) {
            LOGGER.info("Player is not on a planet, unable to negotiate contracts.");
            return null;
        }
        HiringHallLevel hiringHall = currentPlanet.getHiringHallLevel(currentDate);

        Person negotiator = EmployerNegotiator.generateNegotiator(campaign, searchType, employer, hiringHall);
        Person liaison = EmployerLiaison.generateLiaison(campaign,
              searchType,
              employer.isClan(),
              employer.getShortName());

        Camouflage camouflage = pickRandomCamouflage(currentYear, factionCode);

        return new EmployerData(type,
              factionCode,
              anchorFactionCode,
              sponsorFactionCode,
              displayName,
              negotiator,
              liaison,
              camouflage);
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

    /**
     * The factions describing an employer: the {@code flavor} faction the player is contracted with (matching the
     * {@link ChaosEmployerType} theme), the {@code anchor} faction whose territory near the player the contract's
     * conflict is situated in, and an optional covert {@code sponsor} bankrolling the employer. Flavor and anchor are
     * equal for territorial employer types and differ when the flavor faction is landless (rebels, a mercenary command,
     * a corporation, a stateless noble). The sponsor is {@code null} unless a patron is backing the employer.
     *
     * <p>The {@code type} is usually the rolled employer type, but a special employer may override it &mdash; a ComStar
     * takeover fronts a {@link ChaosEmployerType#CORPORATION} while ComStar itself stays in the shadows as anchor and
     * sponsor &mdash; so the caller reads the effective type from here rather than the one it rolled.</p>
     */
    record EmployerFactions(@NonNull ChaosEmployerType type, @NonNull Faction flavor, @NonNull Faction anchor,
          @Nullable Faction sponsor) {}

    /**
     * Resolves the flavor (paying) faction, the territorial anchor faction, and any covert sponsor for the given
     * employer type.
     *
     * <p>The flavor faction is chosen to match the {@link ChaosEmployerType} theme (a corporation for a corporation, a
     * mercenary command for a subcontract, and so on), which may be a faction with no territory of its own. The anchor
     * faction is always a faction that holds ground near the player, so the downstream enemy and target-system
     * selection have real geography to work with even when the flavor faction is landless.</p>
     *
     * <p>On a mercenary search a ComStar/Word of Blake patron may step in. For rebels it does so covertly, bankrolling
     * the uprising while the rebels remain the visible employer (mirroring an enemy fielding sponsored mercenaries);
     * for every other type it openly takes over as the employer, as before.</p>
     *
     * @return the flavor/anchor/sponsor factions, or {@code null} if no eligible employer could be found for the
     *       location
     */
    static @Nullable EmployerFactions determineEmployerFactions(ChaosEmployerType employerType,
          LocalDate currentDate, ILocation currentLocation, boolean isMercenarySearch) {
        if (employerType == ChaosEmployerType.CIVILIAN_ORGANIZATION_REBELS) {
            Faction rebels = resolveFlavorFaction(employerType, currentDate, currentLocation, isMercenarySearch);
            if (rebels == null) {
                return null;
            }
            Faction anchor = resolveAnchorFaction(employerType, currentDate, currentLocation, isMercenarySearch,
                  rebels);
            // A Blakist/ComStar patron does not replace the rebels as employer; it secretly funds them.
            Faction sponsor = isMercenarySearch ? checkForSpecialEmployer(currentDate.getYear()) : null;
            return new EmployerFactions(employerType, rebels, anchor, sponsor);
        }

        Faction specialEmployer;
        if (isMercenarySearch && (specialEmployer = checkForSpecialEmployer(currentDate.getYear())) != null) {
            if (specialEmployer.getShortName().equals(COMSTAR_FACTION_CODE)) {
                // ComStar works in the shadows: rather than take the contract openly it fronts a corporation as the
                // visible employer while bankrolling (sponsor) and territorially anchoring the work itself. The
                // employer type becomes CORPORATION, so the player sees a corporation with its own generated name,
                // never ComStar.
                Faction corporation = resolveFlavorFaction(ChaosEmployerType.CORPORATION, currentDate, currentLocation,
                      isMercenarySearch);
                if (corporation == null) {
                    return null;
                }
                return new EmployerFactions(ChaosEmployerType.CORPORATION, corporation, specialEmployer,
                      specialEmployer);
            }
            // Word of Blake openly takes over as the employer, keeping the rolled type but anchoring on a regional
            // owner: in most eras it holds little or no territory, so the conflict still needs a landed power to sit
            // inside.
            Faction anchor = pickRegionalOwner(currentDate, currentLocation, isMercenarySearch, specialEmployer);
            return new EmployerFactions(employerType, specialEmployer, anchor, null);
        }

        Faction flavor = resolveFlavorFaction(employerType, currentDate, currentLocation, isMercenarySearch);
        if (flavor == null) {
            return null;
        }

        Faction anchor = resolveAnchorFaction(employerType, currentDate, currentLocation, isMercenarySearch, flavor);
        // A mercenary subcontract is a merc command (the flavor) fighting on behalf of the power that actually hired
        // them - the anchor - so that power is the sponsor bankrolling the work. Only when the anchor is a real power
        // distinct from the merc command itself.
        Faction sponsor = (employerType == ChaosEmployerType.MERCENARY_SUBCONTRACT) && !anchor.equals(flavor)
                                ? anchor
                                : null;
        return new EmployerFactions(employerType, flavor, anchor, sponsor);
    }

    /**
     * Selects the flavor faction &mdash; who is paying the unit &mdash; matching the employer type's theme.
     */
    private static @Nullable Faction resolveFlavorFaction(ChaosEmployerType employerType, LocalDate currentDate,
          ILocation currentLocation, boolean isMercenarySearch) {
        Factions factions = Factions.getInstance();
        return switch (employerType) {
            // Owner of the world/system the player is standing on, falling back to any regional owner off-world.
            case LOCAL_SYSTEM_OWNER, LOCAL_PLANETARY_GOVERNMENT, CIVILIAN_ORGANIZATION_MILITIA ->
                  firstNonNull(getCurrentSystemEmployer(currentDate, currentLocation),
                        () -> pickRegionalOwner(currentDate, currentLocation, isMercenarySearch, null));
            // Any landed government with a presence near the player.
            case ANY_SYSTEM_OWNER, ANY_PLANETARY_GOVERNMENT ->
                  pickRegionalOwner(currentDate, currentLocation, isMercenarySearch, null);
            // A noble house, a corporation, or a corporate business: prefer one operating near the player, otherwise
            // draw any active faction of that kind, and finally fall back to a regional owner so generation never fails
            // just because no themed faction is in range.
            case NOBLE -> firstNonNull(pickThemedFaction(currentDate, currentLocation, isMercenarySearch,
                        Faction::isNoble),
                  () -> pickRegionalOwner(currentDate, currentLocation, isMercenarySearch, null));
            case CORPORATION, CIVILIAN_ORGANIZATION_BUSINESS ->
                  firstNonNull(pickThemedFaction(currentDate, currentLocation, isMercenarySearch,
                              Faction::isCorporation),
                        () -> pickRegionalOwner(currentDate, currentLocation, isMercenarySearch, null));
            case CIVILIAN_ORGANIZATION_REBELS -> factions.getFaction(REBEL_FACTION_CODE);
            case MERCENARY_SUBCONTRACT -> factions.getFaction(MERCENARY_FACTION_CODE);
        };
    }

    /**
     * Selects the territorial anchor faction for the given type. Territorial employer types anchor on the flavor
     * faction itself; landless or stateless flavor types anchor on a nearby landed power whose war the contract sits
     * inside. Rebels are special: they fight their own local government, so the anchor is the current system's owner.
     */
    private static Faction resolveAnchorFaction(ChaosEmployerType employerType, LocalDate currentDate,
          ILocation currentLocation, boolean isMercenarySearch, Faction flavor) {
        return switch (employerType) {
            case LOCAL_SYSTEM_OWNER, LOCAL_PLANETARY_GOVERNMENT, CIVILIAN_ORGANIZATION_MILITIA,
                 ANY_SYSTEM_OWNER, ANY_PLANETARY_GOVERNMENT -> flavor;
            case CIVILIAN_ORGANIZATION_REBELS -> firstNonNull(getCurrentSystemEmployer(currentDate, currentLocation),
                  () -> pickRegionalOwner(currentDate, currentLocation, isMercenarySearch, flavor));
            case NOBLE, CORPORATION, CIVILIAN_ORGANIZATION_BUSINESS, MERCENARY_SUBCONTRACT ->
                // These types usually field a landless flavor faction (a mercenary command, a corporation, a
                // stateless noble), so the conflict anchors on a nearby landed power. When the flavor faction is
                // itself a landed government - for example a noble of a Great House - it anchors on itself, rather
                // than borrowing an unrelated neighbor and risking an employer-versus-itself contract.
                  flavor.isGovernment()
                        ? flavor
                        : pickRegionalOwner(currentDate, currentLocation, isMercenarySearch, flavor);
        };
    }

    /**
     * Picks a landed government faction with a presence near the player, falling back to any regional employer and
     * finally to {@code fallback} so a territorial anchor is always available.
     */
    private static Faction pickRegionalOwner(LocalDate currentDate, ILocation currentLocation,
          boolean isMercenarySearch, @Nullable Faction fallback) {
        RandomFactionGenerator generator = RandomFactionGenerator.getInstance();
        Faction owner = generator.getRandomEmployerFaction(currentLocation, currentDate, isMercenarySearch,
              PLAUSIBLE_EMPLOYER.and(Faction::isGovernment));
        if (owner == null) {
            owner = generator.getRandomEmployerFaction(currentLocation, currentDate, isMercenarySearch,
                  PLAUSIBLE_EMPLOYER);
        }
        return owner != null ? owner : fallback;
    }

    /**
     * Picks a faction matching {@code predicate}, preferring one with a regional presence near the player and otherwise
     * drawing from all active factions of that kind. Returns {@code null} if no such faction exists at all.
     */
    private static @Nullable Faction pickThemedFaction(LocalDate currentDate, ILocation currentLocation,
          boolean isMercenarySearch, Predicate<Faction> predicate) {
        final Predicate<Faction> employerPredicate = predicate.and(PLAUSIBLE_EMPLOYER);
        Faction regional = RandomFactionGenerator.getInstance()
                                 .getRandomEmployerFaction(currentLocation, currentDate, isMercenarySearch,
                                       employerPredicate);
        if (regional != null) {
            return regional;
        }

        List<Faction> candidates = Factions.getInstance()
                                         .getActiveFactions(currentDate)
                                         .stream()
                                         .filter(employerPredicate)
                                         .toList();
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(randomInt(candidates.size()));
    }

    /**
     * Returns {@code first} if it is non-null, otherwise the value produced by {@code fallback}. The fallback is a
     * supplier so its (potentially expensive) regional lookup is only performed when the primary faction is missing.
     */
    private static @Nullable Faction firstNonNull(@Nullable Faction first, Supplier<Faction> fallback) {
        return first != null ? first : fallback.get();
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
            // Skip pirate-, rebel-, or command-held worlds; they do not issue contracts. Falls through to the regional
            // pool (also filtered) when the current system has no plausible employer.
            if (employer != null && PLAUSIBLE_EMPLOYER.test(employer)) {
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
