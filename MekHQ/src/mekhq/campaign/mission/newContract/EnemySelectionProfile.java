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
package mekhq.campaign.mission.newContract;

import java.time.LocalDate;

import megamek.common.compute.Compute;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.RandomFactionGenerator;

/**
 * Describes how a contract's enemy faction should be selected, based on what kind of operation the contract represents.
 * The default weighted pool (regional presence adjusted for diplomatic stance) fits a conventional front-line contract,
 * but not every type does: riots are internal unrest, training cadres are harassed by raiders rather than peer states,
 * planetary assaults need an actual war, and covert operations can even target allies.
 *
 * <p>The companion of {@link MissionLocationProfile}: that one decides <em>where</em> a contract type's mission
 * happens, this one decides <em>who</em> it happens against. They are deliberately separate axes &mdash; e.g.
 * ASSASSINATION selects its enemy under covert rules but places the mission with deep-raid reach.</p>
 *
 * <p>Like the location profiles, the pool-based profiles are preferences with fallbacks: an employer at war with
 * nobody still gets a valid enemy from the default pool rather than failing generation. The synthetic-faction profiles
 * ({@link #PIRATES}, {@link #REBELS}, {@link #RAIDERS}) have no pool to run dry and always resolve.</p>
 */
public enum EnemySelectionProfile {
    /** The standard weighted enemy pool, unmodified. */
    DEFAULT {
        @Override
        public Faction selectEnemy(RandomFactionGenerator generator, ILocation location, LocalDate date,
              Faction employer) {
            return generator.getRandomEnemy(false, location, date, employer);
        }
    },
    /**
     * The enemy is the pirate faction (Bandit Caste for a Clan employer): pirate-hunting contracts have their target
     * baked into the type.
     */
    PIRATES {
        @Override
        public Faction selectEnemy(RandomFactionGenerator generator, ILocation location, LocalDate date,
              Faction employer) {
            return pirateFactionFor(employer);
        }
    },
    /** The enemy is the rebel faction: a riot is internal unrest, not a foreign power's front line. */
    REBELS {
        @Override
        public Faction selectEnemy(RandomFactionGenerator generator, ILocation location, LocalDate date,
              Faction employer) {
            return Factions.getInstance().getFaction(Faction.REBEL_FACTION_CODE);
        }
    },
    /**
     * The enemy is an irregular force &mdash; pirates or rebels &mdash; harassing a rear-area posting: nobody hires a
     * training cadre to fight a peer state's line regiments.
     */
    RAIDERS {
        @Override
        public Faction selectEnemy(RandomFactionGenerator generator, ILocation location, LocalDate date,
              Faction employer) {
            return raiderEnemy(employer);
        }
    },
    /**
     * The enemy should be a faction the employer is actually at war with: a planetary assault or relief operation
     * presupposes a shooting war, not a neutral neighbor.
     */
    AT_WAR {
        @Override
        public Faction selectEnemy(RandomFactionGenerator generator, ILocation location, LocalDate date,
              Faction employer) {
            return generator.atWarEnemy(location, date, employer);
        }
    },
    /**
     * The enemy should be a faction at war with the employer that occupies worlds recently taken from it: a guerrilla
     * campaign is fought on your own conquered ground. Pairs with {@link MissionLocationProfile#OCCUPIED_TERRITORY},
     * whose flipped-world location tier can only fire when the enemy actually holds such worlds.
     */
    OCCUPYING_POWER {
        @Override
        public Faction selectEnemy(RandomFactionGenerator generator, ILocation location, LocalDate date,
              Faction employer) {
            return generator.occupyingPowerEnemy(location, date, employer);
        }
    },
    /**
     * The enemy is drawn from the standard pool under covert rules, where even allies become rare, low-chance targets:
     * espionage and sabotage don't respect alliances the way open warfare does.
     */
    COVERT {
        @Override
        public Faction selectEnemy(RandomFactionGenerator generator, ILocation location, LocalDate date,
              Faction employer) {
            return generator.getRandomEnemy(true, location, date, employer);
        }
    };

    /**
     * Selects the enemy faction for {@code employer} under this profile's strategy. The generator owns the actual
     * selection algorithms (they depend on its border tracker and faction hints); this method routes to the right one.
     * Callers should resolve a {@code null} employer to a fallback before dispatching here &mdash; see
     * {@link RandomFactionGenerator#getRandomEnemy(ILocation, LocalDate, Faction, EnemySelectionProfile)}.
     *
     * @param generator the generator supplying the selection algorithms and regional context
     * @param location  the location to center any pool-based search on
     * @param date      the date to check faction control and diplomatic relations against
     * @param employer  the employer faction; must not be {@code null}
     *
     * @return the selected enemy faction
     */
    public abstract Faction selectEnemy(RandomFactionGenerator generator, ILocation location, LocalDate date,
          Faction employer);

    /**
     * @param employer the employer faction
     *
     * @return the pirate faction appropriate to the employer: the Bandit Caste for a Clan employer, otherwise the
     *       regular pirate faction
     */
    private static Faction pirateFactionFor(Faction employer) {
        return Factions.getInstance()
                     .getFaction(employer.isClan() ? Faction.BANDIT_CASTE_FACTION_CODE : Faction.PIRATE_FACTION_CODE);
    }

    /**
     * Picks an irregular rear-area harasser: pirates or rebels, even odds. Either can end up as its own enemy (rival
     * pirate bands, rival rebel cells), so no special-casing is needed when the employer itself is one of the two.
     *
     * @param employer the employer faction
     *
     * @return the raider enemy faction
     */
    private static Faction raiderEnemy(Faction employer) {
        if (Compute.randomInt(2) == 0) {
            return Factions.getInstance().getFaction(Faction.REBEL_FACTION_CODE);
        }
        return pirateFactionFor(employer);
    }
}
