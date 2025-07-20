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
package mekhq.campaign.personnel.enums;

import megamek.logging.MMLogger;
import mekhq.campaign.finances.Money;

/**
 * {@code ConnectionsLevel} represents various degrees of connections that a character may possess.
 *
 * <p>Each level is characterized by an associated numeric level, a chance for the connection to "burn" (go into
 * 'cooldown'), a pool of wealth available, and the maximum number of recruits that can be influenced by this
 * connection.</p>
 *
 * <p> This enum provides convenient getters for each property, as well as a static parser for converting
 * {@link Integer} values to their corresponding {@code ConnectionsLevel}.</p>
 */
public enum ConnectionsLevel {
    // If these values are changed, you must update the CONNECTIONS glossary entry.

    /** Burn chance: 10 - Wealth: 0 - Recruits: 0 */
    CONNECTIONS_ZERO(0, 10, Money.of(0), 0),
    /** Burn chance: 7 - Wealth: 1000 - Recruits: 0 */
    CONNECTIONS_ONE(1, 7, Money.of(1000), 0),
    /** Burn chance: 7 - Wealth: 2500 - Recruits: 0 */
    CONNECTIONS_TWO(2, 7, Money.of(2500), 0),
    /** Burn chance: 6 - Wealth: 5000 - Recruits: 0 */
    CONNECTIONS_THREE(3, 6, Money.of(5000), 0),
    /** Burn Chance: 6 - Wealth: 10,000 - Recruits: 1 */
    CONNECTIONS_FOUR(4, 6, Money.of(10000), 1),
    /** Burn chance: 5 - Wealth: 25,000 - Recruits: 2 */
    CONNECTIONS_FIVE(5, 5, Money.of(25000), 2),
    /** Burn Chance: 5 - Wealth: 50,000 - Recruits: 3 */
    CONNECTIONS_SIX(6, 5, Money.of(50000), 3),
    /** Burn chance: 4 - Wealth: 100,000 - Recruits: 4 */
    CONNECTIONS_SEVEN(7, 4, Money.of(100000), 4),
    /** Burn chance: 4 - Wealth: 250,000 - Recruits: 6 */
    CONNECTIONS_EIGHT(8, 4, Money.of(250000), 6),
    /** Burn chance: 3 - Wealth: 500,000 - Recruits: 8 */
    CONNECTIONS_NINE(9, 3, Money.of(500000), 8),
    /** Burn chance: 3 - Wealth: 1,000,000 - Recruits: 10 */
    CONNECTIONS_TEN(10, 3, Money.of(1000000), 10);

    private static final MMLogger LOGGER = MMLogger.create(ConnectionsLevel.class);

    private final int level;
    private final int burnChance;
    private final Money wealth;
    private final int recruits;

    /**
     * Constructs a new {@link ConnectionsLevel} enum constant.
     *
     * @param level      the numeric level of connection
     * @param burnChance the chance that the connection will "burn" (typically lower is better)
     * @param wealth     the wealth or resources represented by this level
     * @param recruits   the maximum number of recruits accessible with this level of connection
     */
    ConnectionsLevel(int level, int burnChance, Money wealth, int recruits) {
        this.level = level;
        this.burnChance = burnChance;
        this.wealth = wealth;
        this.recruits = recruits;
    }

    /**
     * Gets the unique numeric level representing the Connection.
     *
     * @return the connection level as an integer
     */
    public int getLevel() {
        return level;
    }

    /**
     * Gets the burn chance associated with this Connections level.
     *
     * <p>A lower value indicates a more reliable, less likely to be "burned" connection.</p>
     *
     * @return the burn chance value
     */
    public int getBurnChance() {
        return burnChance;
    }

    /**
     * Gets the wealth pool associated with this Connections level.
     *
     * @return the {@link Money} instance for this level
     */
    public Money getWealth() {
        return wealth;
    }

    /**
     * Gets the maximum number of additional recruits accessible at this connection level.
     *
     * @return the number of potential recruits
     */
    public int getRecruits() {
        return recruits;
    }

    /**
     * Attempts to retrieve a {@link ConnectionsLevel} by its numeric level value.
     *
     * <p>If no matching level is found, {@link #CONNECTIONS_ZERO} is returned and a warning is logged.</p>
     *
     * @param value the numeric level to parse
     *
     * @return the corresponding {@link ConnectionsLevel}
     */
    public static ConnectionsLevel parseConnectionsLevelFromInt(int value) {
        for (ConnectionsLevel connectionsLevel : ConnectionsLevel.values()) {
            if (connectionsLevel.level == value) {
                return connectionsLevel;
            }
        }

        LOGGER.warn("Failed to parse ConnectionsData from int: {} - returning CONNECTIONS_ZERO", value);
        return CONNECTIONS_ZERO;
    }
}
