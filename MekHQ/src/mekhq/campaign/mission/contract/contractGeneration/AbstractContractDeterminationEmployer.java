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

/**
 * Determines the employer for a generated Chaos contract: who is paying the unit, whose territory the conflict sits in,
 * and any covert backer. The shared work &mdash; rolling the employer type, assembling the {@link EmployerData} and its
 * negotiator, liaison, and camouflage &mdash; lives here; the concrete faction selection differs by contract source and
 * is deferred to {@link #determineEmployerFactions}.
 *
 * <p>{@link ChaosContractDeterminationEmployerMercenary} handles mercenary searches (themed flavor factions, borrowed
 * territorial anchors, and ComStar/Word of Blake patrons); {@link ChaosContractDeterminationEmployerGovernment} handles
 * government contracts, where the player's own faction is always the employer. Use {@link #forSearchType} to obtain the
 * right one.</p>
 */
public abstract class AbstractContractDeterminationEmployer {
    private static final MMLogger LOGGER = MMLogger.create(AbstractContractDeterminationEmployer.class);

    /**
     * Aggregate factions (pirates, the Bandit Caste, rebels, mercenaries) and Commands (whose short name carries a dot)
     * do not plausibly issue contracts, so the pool-based employer pickers exclude them. Employer types that are
     * deliberately fielded by such a faction (rebellions, mercenary subcontracts) assign it directly rather than
     * drawing from the pool, so they are unaffected.
     */
    protected static final Predicate<Faction> PLAUSIBLE_EMPLOYER =
          faction -> !faction.isAggregate() && !faction.isSubunit() && !faction.isMercenaryOrganization();

    private static final int COMSTAR_EMPLOYER_CHANCE = 100;
    private static final int WORD_OF_BLAKE_EMPLOYER_CHANCE = 40;

    /**
     * Returns the employer determination appropriate to the search: a mercenary search draws a themed employer, while
     * anything else is a government contract issued by the player's own faction.
     */
    public static AbstractContractDeterminationEmployer forSearchType(ContractSearchType searchType) {
        return searchType == ContractSearchType.MERCENARY
                     ? new ChaosContractDeterminationEmployerMercenary()
                     : new ChaosContractDeterminationEmployerGovernment();
    }

    public @Nullable EmployerData getEmployerGenerationData(LocalDate currentDate, ILocation currentLocation,
          Campaign campaign, ContractSearchType searchType, boolean covertViable) {
        ChaosEmployerType type = determineEmployerType();

        Faction playerFaction = campaign.getPlayerForce().getFaction();
        EmployerFactions employerFactions = determineEmployerFactions(type, currentDate, currentLocation, covertViable,
              playerFaction);
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

    protected ChaosEmployerType determineEmployerType() {
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

    protected @NonNull ChaosEmployerType getCivilianEmployer() {
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
     * <p>This is the shared algorithm. Its per-faction decisions are delegated to the overridable hooks
     * {@link #resolveFlavorFaction}, {@link #resolveAnchorFaction}, and {@link #checkForSpecialEmployer}: the default
     * (mercenary) behavior draws themed flavor factions, borrows a territorial anchor, and lets a ComStar/Word of Blake
     * patron front, take over, or covertly sponsor the work, while the government determination overrides those hooks to
     * force the player's own faction and bar special employers.</p>
     *
     * @param employerType    the rolled employer type, whose theme guides flavor selection
     * @param currentDate     the campaign date, for faction-operating windows and regional lookups
     * @param currentLocation the player's location, whose neighborhood the anchor is drawn from
     * @param covertViable    whether the contract's objective can be run covertly, which gates a ComStar patron
     * @param playerFaction   the player's own faction, the employer for a government contract
     *
     * @return the flavor/anchor/sponsor factions, or {@code null} if no eligible employer could be found for the
     *       location
     */
    @Nullable
    EmployerFactions determineEmployerFactions(ChaosEmployerType employerType, LocalDate currentDate,
          ILocation currentLocation, boolean covertViable, Faction playerFaction) {
        if (employerType == ChaosEmployerType.CIVILIAN_ORGANIZATION_REBELS) {
            Faction rebels = resolveFlavorFaction(employerType, currentDate, currentLocation, playerFaction);
            if (rebels == null) {
                return null;
            }
            Faction anchor = resolveAnchorFaction(employerType, currentDate, currentLocation, playerFaction, rebels);
            // A Blakist/ComStar patron does not replace the rebels as employer; it secretly funds them.
            Faction sponsor = checkForSpecialEmployer(currentDate.getYear(), covertViable);
            return new EmployerFactions(employerType, rebels, anchor, sponsor);
        }

        Faction specialEmployer = checkForSpecialEmployer(currentDate.getYear(), covertViable);
        if (specialEmployer != null) {
            if (specialEmployer.getShortName().equals(COMSTAR_FACTION_CODE)) {
                // ComStar works in the shadows: rather than take the contract openly it fronts a corporation as the
                // visible employer while bankrolling (sponsor) and territorially anchoring the work itself. The
                // employer type becomes CORPORATION, so the player sees a corporation with its own generated name,
                // never ComStar.
                Faction corporation = resolveFlavorFaction(ChaosEmployerType.CORPORATION, currentDate, currentLocation,
                      playerFaction);
                if (corporation == null) {
                    return null;
                }
                return new EmployerFactions(ChaosEmployerType.CORPORATION, corporation, specialEmployer,
                      specialEmployer);
            }
            // Word of Blake openly takes over as the employer, keeping the rolled type but anchoring on a regional
            // owner: in most eras it holds little or no territory, so the conflict still needs a landed power to sit
            // inside.
            Faction anchor = pickRegionalOwner(currentDate, currentLocation, specialEmployer);
            return new EmployerFactions(employerType, specialEmployer, anchor, null);
        }

        Faction flavor = resolveFlavorFaction(employerType, currentDate, currentLocation, playerFaction);
        if (flavor == null) {
            return null;
        }

        Faction anchor = resolveAnchorFaction(employerType, currentDate, currentLocation, playerFaction, flavor);
        // A mercenary subcontract is a merc command (the flavor) fighting on behalf of the power that actually hired
        // them - the anchor - so that power is the sponsor bankrolling the work. Only when the anchor is a real power
        // distinct from the merc command itself.
        Faction sponsor = (employerType == ChaosEmployerType.MERCENARY_SUBCONTRACT) && !anchor.equals(flavor)
                                ? anchor
                                : null;
        return new EmployerFactions(employerType, flavor, anchor, sponsor);
    }

    /**
     * Selects the flavor faction &mdash; who is paying the unit &mdash; matching the employer type's theme. This is an
     * overridable hook: the government determination returns the player's own faction here instead, ignoring
     * {@code employerType}. {@code playerFaction} is supplied for that override; the default themed resolution does not
     * use it.
     */
    protected @Nullable Faction resolveFlavorFaction(ChaosEmployerType employerType, LocalDate currentDate,
          ILocation currentLocation, Faction playerFaction) {
        Factions factions = Factions.getInstance();
        return switch (employerType) {
            // Owner of the world/system the player is standing on, falling back to any regional owner off-world.
            case LOCAL_SYSTEM_OWNER, LOCAL_PLANETARY_GOVERNMENT, CIVILIAN_ORGANIZATION_MILITIA ->
                  firstNonNull(getCurrentSystemEmployer(currentDate, currentLocation),
                        () -> pickRegionalOwner(currentDate, currentLocation, null));
            // Any landed government with a presence near the player.
            case ANY_SYSTEM_OWNER, ANY_PLANETARY_GOVERNMENT -> pickRegionalOwner(currentDate, currentLocation, null);
            // A noble house, a corporation, or a corporate business: prefer one operating near the player, otherwise
            // draw any active faction of that kind, and finally fall back to a regional owner so generation never fails
            // just because no themed faction is in range.
            case NOBLE -> firstNonNull(pickThemedFaction(currentDate, currentLocation, Faction::isNoble),
                  () -> pickRegionalOwner(currentDate, currentLocation, null));
            case CORPORATION, CIVILIAN_ORGANIZATION_BUSINESS ->
                  firstNonNull(pickThemedFaction(currentDate, currentLocation, Faction::isCorporation),
                        () -> pickRegionalOwner(currentDate, currentLocation, null));
            case CIVILIAN_ORGANIZATION_REBELS -> factions.getFaction(REBEL_FACTION_CODE);
            case MERCENARY_SUBCONTRACT -> factions.getFaction(MERCENARY_FACTION_CODE);
        };
    }

    /**
     * Selects the territorial anchor faction for the given type. Territorial employer types anchor on the flavor
     * faction itself; landless or stateless flavor types anchor on a nearby landed power whose war the contract sits
     * inside. Rebels are special: they fight their own local government, so the anchor is the current system's owner.
     *
     * <p>This is an overridable hook: the government determination returns the player's own faction here instead.
     * {@code playerFaction} is supplied for that override; the default territorial resolution does not use it.</p>
     */
    protected Faction resolveAnchorFaction(ChaosEmployerType employerType, LocalDate currentDate,
          ILocation currentLocation, Faction playerFaction, Faction flavor) {
        return switch (employerType) {
            case LOCAL_SYSTEM_OWNER, LOCAL_PLANETARY_GOVERNMENT, CIVILIAN_ORGANIZATION_MILITIA,
                 ANY_SYSTEM_OWNER, ANY_PLANETARY_GOVERNMENT -> flavor;
            case CIVILIAN_ORGANIZATION_REBELS -> firstNonNull(getCurrentSystemEmployer(currentDate, currentLocation),
                  () -> pickRegionalOwner(currentDate, currentLocation, flavor));
            case NOBLE, CORPORATION, CIVILIAN_ORGANIZATION_BUSINESS, MERCENARY_SUBCONTRACT ->
                // These types usually field a landless flavor faction (a mercenary command, a corporation, a
                // stateless noble), so the conflict anchors on a nearby landed power. When the flavor faction is
                // itself a landed government - for example a noble of a Great House - it anchors on itself, rather
                // than borrowing an unrelated neighbor and risking an employer-versus-itself contract.
                  flavor.isGovernment()
                        ? flavor
                        : pickRegionalOwner(currentDate, currentLocation, flavor);
        };
    }

    /**
     * Returns {@code first} if it is non-null, otherwise the value produced by {@code fallback}. The fallback is a
     * supplier so its (potentially expensive) regional lookup is only performed when the primary faction is missing.
     */
    protected static @Nullable Faction firstNonNull(@Nullable Faction first, Supplier<Faction> fallback) {
        return first != null ? first : fallback.get();
    }

    protected static @Nullable Faction getCurrentSystemEmployer(LocalDate currentDate, ILocation currentLocation) {
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

    /**
     * Picks a landed government faction with a presence near the player, falling back to any regional employer and
     * finally to {@code fallback} so a territorial anchor is always available.
     */
    protected static Faction pickRegionalOwner(LocalDate currentDate, ILocation currentLocation,
          @Nullable Faction fallback) {
        RandomFactionGenerator generator = RandomFactionGenerator.getInstance();
        // Only the mercenary employer determination reaches these regional lookups, so the pool is queried as a
        // mercenary search.
        Faction owner = generator.getRandomEmployerFaction(currentLocation, currentDate, true,
              PLAUSIBLE_EMPLOYER.and(Faction::isGovernment));
        if (owner == null) {
            owner = generator.getRandomEmployerFaction(currentLocation, currentDate, true, PLAUSIBLE_EMPLOYER);
        }
        return owner != null ? owner : fallback;
    }

    /**
     * Picks a faction matching {@code predicate}, preferring one with a regional presence near the player and otherwise
     * drawing from all active factions of that kind. Returns {@code null} if no such faction exists at all.
     */
    protected static @Nullable Faction pickThemedFaction(LocalDate currentDate, ILocation currentLocation,
          Predicate<Faction> predicate) {
        final Predicate<Faction> employerPredicate = predicate.and(PLAUSIBLE_EMPLOYER);
        // Only the mercenary employer determination reaches these regional lookups, so the pool is queried as a
        // mercenary search.
        Faction regional = RandomFactionGenerator.getInstance()
                                 .getRandomEmployerFaction(currentLocation, currentDate, true, employerPredicate);
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
     * Rolls whether ComStar or the Word of Blake steps into this contract - either fronting, taking over, or covertly
     * sponsoring it (see the subclass {@link #determineEmployerFactions}). ComStar only involves itself in covert-viable
     * contracts, since its interest is in running them as deniable false flags; the Word of Blake is under no such
     * restriction and will take any contract. ComStar is rolled first (it takes priority), then the Word of Blake.
     *
     * <p>This is the shared hook a subclass may override to bar special employers entirely; the government determination
     * does so, since a state never fronts its own contracts through ComStar or the Word of Blake.</p>
     *
     * @param currentYear  the year, for each faction's operating window
     * @param covertViable whether this contract's objective can be run covertly, which gates ComStar's involvement
     *
     * @return ComStar or the Word of Blake if one steps in, otherwise {@code null}
     */
    protected @Nullable Faction checkForSpecialEmployer(int currentYear, boolean covertViable) {
        if (covertViable) {
            Faction comStar = checkForEmployerOverride(currentYear,
                  COMSTAR_FACTION_CODE,
                  COMSTAR_EMPLOYER_CHANCE,
                  true);
            if (comStar != null) {
                return comStar;
            }
        }

        return checkForEmployerOverride(currentYear, WORD_OF_BLAKE_FACTION_CODE, WORD_OF_BLAKE_EMPLOYER_CHANCE, false);
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
