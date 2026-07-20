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
package mekhq.campaign.force;

import static mekhq.campaign.market.contractMarket.ContractAutomation.performAutomatedActivation;

import java.util.Objects;

import jakarta.annotation.Nonnull;
import megamek.common.annotations.Nullable;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignNewDayManager;
import mekhq.campaign.DetachmentLocationManager;
import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.LocalHangar;
import mekhq.campaign.LocalPersonnel;
import mekhq.campaign.LocalWarehouse;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.location.IPlace;
import mekhq.campaign.location.LocationNode;
import mekhq.campaign.market.RequestedStockLevels;
import org.w3c.dom.Node;

/**
 * A group of a force's units, spare parts, and personnel that sits at a single location in the {@link LocationNode}
 * tree.
 *
 * <p>A {@code Detachment} is the {@link IPlace} that owns those resources: it holds its own
 * {@link mekhq.campaign.DetachmentLocationManager} (which owns the {@link LocationNode}) and parents its hangar,
 * warehouse, and personnel to itself, exactly like {@link mekhq.campaign.base.AbstractBase}. The upward {@code IPlace}
 * walk therefore terminates here.</p>
 *
 * <p>An {@link AbstractForce} owns one or more detachments (see {@link AbstractForce#getDetachments()}); a
 * {@link PlayerForce} owns exactly one. A detachment holds <em>no</em> reference back to its force or the
 * {@link Campaign} — campaign-level concerns are passed in as method parameters.</p>
 */
public class Detachment implements IPlace {

    /** Discriminator identifying a detachment as a serialized {@link ILocation} reference. */
    public static final String LOCATION_REFERENCE_TYPE = "detachment";

    private final DetachmentLocationManager detachmentLocationManager;

    // Owned resources — this detachment is the IPlace that owns them.
    private final LocalHangar units = new LocalHangar();
    private final LocalPersonnel personnel = new LocalPersonnel();
    private final RequestedStockLevels requestedStockLevels = new RequestedStockLevels();
    private LocalWarehouse parts = new LocalWarehouse();

    public Detachment() {
        // The manager owns the LocationNode; build it first so parenting the resources can resolve this node.
        this.detachmentLocationManager = new DetachmentLocationManager(this);
        LocationNode.LocationManager.setLocation(personnel, this);
        LocationNode.LocationManager.setLocation(units, this);
        LocationNode.LocationManager.setLocation(parts, this);
    }

    /**
     * Resolves a serialized reference to the player force's detachment back to the live instance. Because a campaign
     * has a single player force with a single detachment, the reference carries no identity beyond its discriminator.
     */
    public static @Nullable ILocation resolveReference(Campaign campaign, Node node) {
        return campaign.getPlayerForce().getForceDetachment();
    }

    public DetachmentLocationManager getDetachmentLocationManager() {
        return detachmentLocationManager;
    }

    @Override
    public @Nonnull LocationNode getLocationNode() {
        return detachmentLocationManager.getLocationNode();
    }

    @Override
    public LocalHangar getHangar() {
        return units;
    }

    @Override
    public LocalWarehouse getWarehouse() {
        return parts;
    }

    public void setWarehouse(LocalWarehouse warehouse) {
        parts = Objects.requireNonNull(warehouse);
    }

    @Override
    public LocalPersonnel getPersonnel() {
        return personnel;
    }

    @Override
    public RequestedStockLevels getRequestedStockLevels() {
        return requestedStockLevels;
    }

    /**
     * Runs the detachment's arrival behavior when it finishes travelling to a location. The {@link Campaign} is
     * supplied as a parameter (a detachment holds no campaign reference) for the systems this drives: automated
     * mothball activation, disease/inoculation checks, early-arrival contract checks, and refreshing local applicants.
     */
    @Override
    public void onArrival(Campaign campaign, boolean isSilentProcessing) {
        // This should be before inoculations so that we can correctly read the TO&E.
        if (!campaign.getPlayerForce().getAutomatedMothballUnits().isEmpty()) {
            performAutomatedActivation(campaign);
        }

        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        if (campaignOptions.isUseRandomDiseases() && campaignOptions.isUseAlternativeAdvancedMedical()) {
            if (getParentLocation() instanceof AbstractLocation loc) {
                loc.checkForDiseaseOrBioweaponOutbreaks(campaign, campaign.getLocalDate());
            }
        }

        // Inoculations (generic IPlace behavior)
        IPlace.super.onArrival(campaign, isSilentProcessing);

        if (getParentLocation() instanceof AbstractLocation loc) {
            loc.testForEarlyArrival(campaign);
        }

        // We've just stopped traveling, so we should see if there are any local applicants.
        if (!ForceHumanResources.isUsingLegacyPersonnelMarket(campaign.getCampaignOptions())) {
            campaign.getPlayerForce().getHumanResources().refreshApplicants(campaign, true);
            CampaignNewDayManager.showRarePersonnelDialog(campaign, false);
        }
    }

    @Override
    public String locationReferenceType() {
        return LOCATION_REFERENCE_TYPE;
    }

    @Override
    public boolean isInUse() {
        return true;
    }
}
