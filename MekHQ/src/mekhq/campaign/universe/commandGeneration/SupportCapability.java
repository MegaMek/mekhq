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
package mekhq.campaign.universe.commandGeneration;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.universe.commandGeneration.SupportPersonnelToTOE.SupportSection;

/**
 * The support vehicles a command is granted for each capability it switches on, together with everything the rest of
 * support generation needs to know about them: which campaign option grants them, which unit is fielded, how many,
 * which Table of Organisation and Equipment formation holds them, which support section crews them, and whether they
 * add to the mechanic demand.
 *
 * <p>This is the single list every part of support generation reads. The mechanic count, the company generator, the
 * step that organises staff into support teams, and the campaign-option dialogs all ask this enum rather than
 * repeating the option checks themselves. Keeping one copy is what stops them drifting apart, which is what left a
 * command with no recovery vehicles at all (issue #10059).</p>
 *
 * @since 0.51.01
 */
public enum SupportCapability {
    /** Recovery vehicles, crewed by the maintenance section, granted under CamOps salvage rules. */
    SALVAGE(campaignOptions -> campaignOptions.get(CampaignOption.IS_USE_CAM_OPS_SALVAGE),
          campaign -> SupportUnitGenerator.SALVAGE_UNIT,
          SupportUnitGenerator::scaledCount,
          SupportTOEFormationTypes.SALVAGE_FORMATION,
          SupportSection.MAINTENANCE,
          PersonnelRole.VEHICLE_CREW_GROUND,
          true,
          "mekhq.resources.SalvageCampaignOptionsChangedConfirmationDialog",
          "SalvageCampaignOptionsChangedConfirmationDialog"),

    /** MASH trucks, crewed by the medical section, scaled to treat the command's combatants. */
    MEDICAL(campaignOptions -> campaignOptions.get(CampaignOption.USE_MASH_THEATRES),
          campaign -> SupportUnitGenerator.MEDICAL_UNIT,
          SupportUnitGenerator::medicalUnitCount,
          SupportTOEFormationTypes.MEDICAL_FORMATION,
          SupportSection.MEDICAL,
          PersonnelRole.VEHICLE_CREW_GROUND,
          true,
          "mekhq.resources.MASHTheaterTrackingCampaignOptionsChangedConfirmationDialog",
          "MASHTheaterTrackingCampaignOptionsChangedConfirmationDialog"),

    /** Flatbed trucks for the StratCon supply convoy, generated with their own crews. */
    LOGISTICS(CampaignOptions::isUseStratCon,
          campaign -> SupportUnitGenerator.LOGISTICS_UNIT,
          SupportUnitGenerator::scaledCount,
          SupportTOEFormationTypes.LOGISTICS_FORMATION,
          null,
          PersonnelRole.VEHICLE_CREW_GROUND,
          true,
          "mekhq.resources.StratConConvoyCampaignOptionsChangedConfirmationDialog",
          "StratConConvoyCampaignOptionsChangedConfirmationDialog"),

    /** Mobile canteens feeding the command once fatigue is tracked. */
    COMMISSARY(campaignOptions -> campaignOptions.get(CampaignOption.USE_FATIGUE),
          campaign -> SupportUnitGenerator.COMMISSARY_UNIT,
          SupportUnitGenerator::commissaryUnitCount,
          SupportTOEFormationTypes.COMMISSARY_FORMATION,
          null,
          PersonnelRole.VEHICLE_CREW_GROUND,
          true,
          "mekhq.resources.FatigueTrackingCampaignOptionsChangedConfirmationDialog",
          "FatigueTrackingCampaignOptionsChangedConfirmationDialog"),

    /** Rifle infantry guarding prisoners. Infantry carry no mechanic demand. */
    SECURITY(campaignOptions -> !campaignOptions.get(CampaignOption.PRISONER_CAPTURE_STYLE).isNone(),
          SupportCapability::securityUnitName,
          SupportCapability::securityCount,
          SupportTOEFormationTypes.SECURITY_FORMATION,
          null,
          PersonnelRole.SOLDIER,
          false,
          "mekhq.resources.PrisonerTrackingCampaignOptionsChangedConfirmationDialog",
          "PrisonerTrackingCampaignOptionsChangedConfirmationDialog");

    private final Predicate<CampaignOptions> enabledCheck;
    private final Function<Campaign, String> unitNameResolver;
    private final ToIntFunction<Campaign> targetCountResolver;
    private final SupportTOEFormationTypes formationType;
    private final SupportSection crewSection;
    private final PersonnelRole crewRole;
    private final boolean needsMechanics;
    private final String resourceBundle;
    private final String resourceKeyPrefix;

    SupportCapability(Predicate<CampaignOptions> enabledCheck, Function<Campaign, String> unitNameResolver,
          ToIntFunction<Campaign> targetCountResolver, SupportTOEFormationTypes formationType,
          @Nullable SupportSection crewSection, PersonnelRole crewRole, boolean needsMechanics,
          String resourceBundle, String resourceKeyPrefix) {
        this.enabledCheck = enabledCheck;
        this.unitNameResolver = unitNameResolver;
        this.targetCountResolver = targetCountResolver;
        this.formationType = formationType;
        this.crewSection = crewSection;
        this.crewRole = crewRole;
        this.needsMechanics = needsMechanics;
        this.resourceBundle = resourceBundle;
        this.resourceKeyPrefix = resourceKeyPrefix;
    }

    /**
     * Whether the campaign has switched this capability on. This is the same check that decides whether the
     * capability's campaign-option dialog offers its free vehicles.
     *
     * @param campaign the campaign to inspect
     *
     * @return {@code true} when the capability's campaign option is enabled
     */
    public boolean isEnabled(Campaign campaign) {
        return enabledCheck.test(campaign.getCampaignOptions());
    }

    /**
     * The unit this capability fields, which for the security detail depends on the size of the force and whether the
     * command is Clan.
     *
     * @param campaign the campaign the vehicles are generated into
     *
     * @return the unit name as the unit cache holds it
     */
    public String unitName(Campaign campaign) {
        return unitNameResolver.apply(campaign);
    }

    /**
     * How many of the unit a command of this size should field, before subtracting whatever it already owns.
     *
     * @param campaign the campaign the vehicles are generated into
     *
     * @return the target count
     */
    public int targetCount(Campaign campaign) {
        return targetCountResolver.applyAsInt(campaign);
    }

    /**
     * The formation these vehicles are filed under in the Table of Organisation and Equipment.
     *
     * @return the support formation type
     */
    public SupportTOEFormationTypes formationType() {
        return formationType;
    }

    /**
     * The support section whose staff crew these vehicles, or {@code null} when the vehicles come with their own
     * crews, as the convoy, the canteens and the security detail do.
     *
     * @return the crewing section, or {@code null}
     */
    public @Nullable SupportSection crewSection() {
        return crewSection;
    }

    /**
     * The role that crews these units, which decides whether they are crewed from the temporary crew pool: ground
     * vehicle crew for the support vehicles, and soldiers for the security detail.
     *
     * @return the crew role
     */
    public PersonnelRole crewRole() {
        return crewRole;
    }

    /**
     * Whether these vehicles need mechanics, so the personnel stage can count them before they exist. Infantry do
     * not.
     *
     * @return {@code true} when the vehicles add to the mechanic demand
     */
    public boolean needsMechanics() {
        return needsMechanics;
    }

    /**
     * Whether these vehicles join a support section rather than being generated with their own crew. A capability
     * joins its section only when it has one and the campaign uses support teams. This is the single rule that
     * decides where every support vehicle goes.
     *
     * @param campaign the campaign the vehicles are generated into
     *
     * @return {@code true} when the vehicles are built inside their support section
     */
    public boolean joinsSection(Campaign campaign) {
        return (crewSection != null) && SupportCarrierReconciler.isEnabled(campaign);
    }

    /**
     * The resource bundle holding this capability's campaign-option dialog text.
     *
     * @return the bundle name
     */
    public String resourceBundle() {
        return resourceBundle;
    }

    /**
     * The key prefix this capability's dialog text uses inside its bundle, so the shared dialog reads the
     * description, and the two button labels, that were written for it.
     *
     * @return the resource key prefix
     */
    public String resourceKeyPrefix() {
        return resourceKeyPrefix;
    }

    /** The security detail's unit, which depends on the size of the force and whether it is a Clan command. */
    private static String securityUnitName(Campaign campaign) {
        return SupportUnitGenerator.securityUnitName(SupportUnitGenerator.securityTier(campaign),
              campaign.getPlayerForce().isClanForce());
    }

    /** The security detail's unit count: a company-sized detail is fielded as repeated platoons. */
    private static int securityCount(Campaign campaign) {
        return SupportUnitGenerator.securityTier(campaign) == SupportUnitGenerator.SecurityTier.COMPANY
                     ? SupportUnitGenerator.PLATOONS_PER_COMPANY
                     : 1;
    }
}
