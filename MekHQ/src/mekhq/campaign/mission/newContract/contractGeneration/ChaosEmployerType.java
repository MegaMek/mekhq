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
package mekhq.campaign.mission.newContract.contractGeneration;

public enum ChaosEmployerType {
    ANY_PLANETARY_GOVERNMENT(0, 1, 0, 1, 0),
    ANY_SYSTEM_OWNER(1, 2, 1, -2, -3),
    CIVILIAN_ORGANIZATION_BUSINESS(-2, -2, -1, 4, 4),
    CIVILIAN_ORGANIZATION_MILITIA(-2, -2, -1, 4, 4),
    CIVILIAN_ORGANIZATION_REBELS(-2, -2, -1, 4, 4),
    CORPORATION(2, -2, 1, 2, 0),
    LOCAL_PLANETARY_GOVERNMENT(0, 1, 0, 1, 0),
    LOCAL_SYSTEM_OWNER(0, 2, 1, -2, -3),
    MERCENARY_SUBCONTRACT(0, 0, 0, 0, 3),
    NOBLE(0, 0, 0, 0, 0);

    private final int payRateModifier; // Hot Spots Draconis Reach pg 144 first printing
    private final int supportModifier; // Hot Spots Draconis Reach pg 144 first printing
    private final int transportModifier; // Hot Spots Draconis Reach pg 144 first printing
    private final int salvageRightsModifier; // Hot Spots Draconis Reach pg 144 first printing
    private final int commandRightsModifier; // Hot Spots Draconis Reach pg 144 first printing

    ChaosEmployerType(final int payRateModifier, final int supportModifier, final int transportModifier,
          final int salvageRightsModifier, final int commandRightsModifier) {
        this.payRateModifier = payRateModifier;
        this.supportModifier = supportModifier;
        this.transportModifier = transportModifier;
        this.salvageRightsModifier = salvageRightsModifier;
        this.commandRightsModifier = commandRightsModifier;
    }

    public boolean isCurrentSystemEmployer() {
        return this == LOCAL_SYSTEM_OWNER ||
                     this == LOCAL_PLANETARY_GOVERNMENT;
    }

    public boolean isSystemOwner() {
        return this == LOCAL_SYSTEM_OWNER ||
                     this == ANY_SYSTEM_OWNER;
    }

    public int getPayRateModifier() {
        return payRateModifier;
    }

    public int getSupportModifier() {
        return supportModifier;
    }

    public int getTransportModifier() {
        return transportModifier;
    }

    public int getSalvageRightsModifier() {
        return salvageRightsModifier;
    }

    public int getCommandRightsModifier() {
        return commandRightsModifier;
    }
}
