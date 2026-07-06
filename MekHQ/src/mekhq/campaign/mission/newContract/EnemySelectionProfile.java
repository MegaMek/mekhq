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

import mekhq.campaign.mission.enums.AtBContractType;

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
    DEFAULT,
    /**
     * The enemy is the pirate faction (Bandit Caste for a Clan employer): pirate-hunting contracts have their target
     * baked into the type.
     */
    PIRATES,
    /** The enemy is the rebel faction: a riot is internal unrest, not a foreign power's front line. */
    REBELS,
    /**
     * The enemy is an irregular force &mdash; pirates or rebels &mdash; harassing a rear-area posting: nobody hires a
     * training cadre to fight a peer state's line regiments.
     */
    RAIDERS,
    /**
     * The enemy should be a faction the employer is actually at war with: a planetary assault or relief operation
     * presupposes a shooting war, not a neutral neighbor.
     */
    AT_WAR,
    /**
     * The enemy should be a faction at war with the employer that occupies worlds recently taken from it: a guerrilla
     * campaign is fought on your own conquered ground. Pairs with {@link MissionLocationProfile#OCCUPIED_TERRITORY},
     * whose flipped-world location tier can only fire when the enemy actually holds such worlds.
     */
    OCCUPYING_POWER,
    /**
     * The enemy is drawn from the standard pool under covert rules, where even allies become rare, low-chance targets:
     * espionage and sabotage don't respect alliances the way open warfare does.
     */
    COVERT;

    /**
     * Maps a contract type to its enemy-selection profile. Deliberately exhaustive with no {@code default} branch so
     * that adding a new contract type forces a conscious decision about who its missions are fought against.
     *
     * <p>{@link #PIRATES} and {@link #REBELS} absorb what were previously inline special cases in
     * {@code AbstractContractMarket#setEnemyCode} and {@code AtbMonthlyContractMarket#generateAtBSubcontract}, so
     * pirate-hunting and riot-duty behavior is unchanged &mdash; it just lives in the same policy as the rest.</p>
     *
     * @param contractType the contract's type
     *
     * @return the enemy-selection profile to use when choosing the contract's enemy faction
     */
    public static EnemySelectionProfile fromContractType(AtBContractType contractType) {
        return switch (contractType) {
            case PIRATE_HUNTING -> PIRATES;
            case RIOT_DUTY -> REBELS;
            case CADRE_DUTY -> RAIDERS;
            case PLANETARY_ASSAULT, RELIEF_DUTY -> AT_WAR;
            case GUERRILLA_WARFARE -> OCCUPYING_POWER;
            case ESPIONAGE, SABOTAGE, TERRORISM, ASSASSINATION -> COVERT;
            case GARRISON_DUTY, SECURITY_DUTY, RETAINER, DIVERSIONARY_RAID, OBJECTIVE_RAID, RECON_RAID,
                 EXTRACTION_RAID, OBSERVATION_RAID, MOLE_HUNTING, UNDEFINED -> DEFAULT;
        };
    }
}
