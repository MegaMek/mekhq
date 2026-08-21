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

import static mekhq.campaign.personnel.backgrounds.BackgroundsController.randomCivilianCompanyNameGenerator;
import static mekhq.campaign.personnel.backgrounds.BackgroundsController.randomCorporationCompanyNameGenerator;
import static mekhq.campaign.personnel.backgrounds.BackgroundsController.randomMercenaryCompanyNameGenerator;
import static mekhq.campaign.personnel.backgrounds.BackgroundsController.randomMilitiaCompanyNameGenerator;
import static mekhq.campaign.personnel.backgrounds.BackgroundsController.randomRebelCompanyNameGenerator;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import jakarta.annotation.Nullable;

public enum ChaosEmployerType {
    ANY_PLANETARY_GOVERNMENT("ChaosEmployerType.ANY_PLANETARY_GOVERNMENT.text", 0, 1, 0, 1, 0),
    ANY_SYSTEM_OWNER("ChaosEmployerType.ANY_SYSTEM_OWNER.text", 1, 2, 1, -2, -3),
    CIVILIAN_ORGANIZATION_BUSINESS("ChaosEmployerType.CIVILIAN_ORGANIZATION_BUSINESS.text", -2, -2, -1, 4, 4),
    CIVILIAN_ORGANIZATION_MILITIA("ChaosEmployerType.CIVILIAN_ORGANIZATION_MILITIA.text", -2, -2, -1, 4, 4),
    CIVILIAN_ORGANIZATION_REBELS("ChaosEmployerType.CIVILIAN_ORGANIZATION_REBELS.text", -2, -2, -1, 4, 4),
    CORPORATION("ChaosEmployerType.CORPORATION.text", 2, -2, 1, 2, 0),
    LOCAL_PLANETARY_GOVERNMENT("ChaosEmployerType.LOCAL_PLANETARY_GOVERNMENT.text", 0, 1, 0, 1, 0),
    LOCAL_SYSTEM_OWNER("ChaosEmployerType.LOCAL_SYSTEM_OWNER.text", 0, 2, 1, -2, -3),
    MERCENARY_SUBCONTRACT("ChaosEmployerType.MERCENARY_SUBCONTRACT.text", 0, 0, 0, 0, 3),
    NOBLE("ChaosEmployerType.NOBLE.text", 0, 0, 0, 0, 0);

    private static final String RESOURCE_BUNDLE = "mekhq.resources.Mission";

    private final String lookup;
    private final String label;
    private final int payRateModifier; // Hot Spots Draconis Reach pg 144 first printing
    private final int supportModifier; // Hot Spots Draconis Reach pg 144 first printing
    private final int transportModifier; // Hot Spots Draconis Reach pg 144 first printing
    private final int salvageRightsModifier; // Hot Spots Draconis Reach pg 144 first printing
    private final int commandRightsModifier; // Hot Spots Draconis Reach pg 144 first printing

    ChaosEmployerType(final String lookup, final int payRateModifier, final int supportModifier,
          final int transportModifier, final int salvageRightsModifier, final int commandRightsModifier) {
        this.lookup = lookup;
        this.label = getTextAt(RESOURCE_BUNDLE, lookup);
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

    /** @return the player-facing label for this employer type. */
    public String getLabel() {
        return label;
    }

    /**
     * Generates a flavorful, one-off employer name for this type using the matching random name generator.
     *
     * <p>Only the non-system-owner employer types that field a generic, landless flavor faction (mercenary commands,
     * corporations, and the civilian rebel/militia/business organizations) get a generated name. The system owners,
     * planetary governments, and nobles keep their own faction's name, so this returns {@code null} for them.</p>
     *
     * <p>No player commander is supplied, so any vanity prefix is seeded from a random callsign rather than the
     * player's own commander &mdash; the employer is not the player.</p>
     *
     * @return the generated employer name, or {@code null} if this type should use its faction's name instead
     */
    public @Nullable String generateEmployerName() {
        return switch (this) {
            case MERCENARY_SUBCONTRACT -> randomMercenaryCompanyNameGenerator(null);
            case CORPORATION -> randomCorporationCompanyNameGenerator();
            case CIVILIAN_ORGANIZATION_BUSINESS -> randomCivilianCompanyNameGenerator();
            case CIVILIAN_ORGANIZATION_MILITIA -> randomMilitiaCompanyNameGenerator(null);
            case CIVILIAN_ORGANIZATION_REBELS -> randomRebelCompanyNameGenerator(null);
            default -> null;
        };
    }

    /**
     * @return a short player-facing tag naming this employer type (for example {@code "Rebels"}), shown in brackets
     *       after a generated employer name in the contract-market GUI only, or {@code null} for types that keep their
     *       faction's own name. A non-null tag mirrors exactly the types for which {@link #generateEmployerName()}
     *       produces a name.
     */
    public @Nullable String getMarketDisplayTag() {
        return switch (this) {
            case MERCENARY_SUBCONTRACT ->
                  getTextAt(RESOURCE_BUNDLE, "ChaosEmployerType.MERCENARY_SUBCONTRACT.marketTag");
            case CORPORATION -> getTextAt(RESOURCE_BUNDLE, "ChaosEmployerType.CORPORATION.marketTag");
            case CIVILIAN_ORGANIZATION_BUSINESS ->
                  getTextAt(RESOURCE_BUNDLE, "ChaosEmployerType.CIVILIAN_ORGANIZATION_BUSINESS.marketTag");
            case CIVILIAN_ORGANIZATION_MILITIA ->
                  getTextAt(RESOURCE_BUNDLE, "ChaosEmployerType.CIVILIAN_ORGANIZATION_MILITIA.marketTag");
            case CIVILIAN_ORGANIZATION_REBELS ->
                  getTextAt(RESOURCE_BUNDLE, "ChaosEmployerType.CIVILIAN_ORGANIZATION_REBELS.marketTag");
            case NOBLE -> getTextAt(RESOURCE_BUNDLE, "ChaosEmployerType.NOBLE.marketTag");
            case LOCAL_PLANETARY_GOVERNMENT, ANY_PLANETARY_GOVERNMENT ->
                  getTextAt(RESOURCE_BUNDLE, "ChaosEmployerType.PLANETARY_GOVERNMENT.marketTag");
            default -> null;
        };
    }

    @Override
    public String toString() {
        return label;
    }
}
