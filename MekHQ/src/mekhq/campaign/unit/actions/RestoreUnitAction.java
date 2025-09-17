/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.unit.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import megamek.common.CriticalSlot;
import megamek.common.annotations.Nullable;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.loaders.MekFileParser;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.util.C3Util;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.events.units.UnitChangedEvent;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.parts.missing.MissingThrusters;
import mekhq.campaign.unit.Unit;

/**
 * Restores a unit to an undamaged state.
 */
public record RestoreUnitAction(IEntityCopyFactory entityCopyFactory) implements IUnitAction {
    private static final MMLogger LOGGER = MMLogger.create(RestoreUnitAction.class);

    /**
     * Creates a new {@code RestoreUnitAction} instance using the default means of creating entity copies.
     */
    public RestoreUnitAction() {
        this(new FileSystemEntityCopyFactory());
    }

    /**
     * Creates a new {@code RestoreUnitAction} instance using the provided {@link IEntityCopyFactory}.
     *
     * @param entityCopyFactory The factory to create entity copies with.
     */
    public RestoreUnitAction(IEntityCopyFactory entityCopyFactory) {
        this.entityCopyFactory = Objects.requireNonNull(entityCopyFactory);
    }

    @Override
    public void execute(Campaign campaign, Unit unit) {
        Entity newEntity = entityCopyFactory.copy(unit.getEntity());
        if (newEntity != null) {
            restoreUnit(campaign, unit, newEntity);
        } else {
            // Fall back to the old way of restoring a unit if we could not
            // create a copy of the entity from the summary cache
            oldUnitRestoration(campaign, unit);
        }

        MekHQ.triggerEvent(new UnitChangedEvent(unit));
    }

    /**
     * Restore a unit by swapping out its entity and replacing its parts.
     *
     * @param campaign  The campaign which owns the unit.
     * @param unit      The unit to restore.
     * @param newEntity The new entity to assign to the unit.
     */
    private void restoreUnit(Campaign campaign, Unit unit, Entity newEntity) {
        // CAW: this logic is broadly similar to Campaign::addNewUnit
        final Entity oldEntity = unit.getEntity();
        newEntity.setId(oldEntity.getId());

        campaign.getGame().removeEntity(oldEntity.getId(), 0);

        newEntity.setOwner(campaign.getPlayer());
        newEntity.setGame(campaign.getGame());
        newEntity.setExternalIdAsString(unit.getId().toString());
        campaign.getGame().addEntity(newEntity);

        C3Util.copyC3Networks(oldEntity, newEntity);

        unit.setEntity(newEntity);

        unit.removeParts();

        unit.initializeAllTransportSpace();
        campaign.updateTransportInTransports(unit);

        unit.initializeParts(true);
        unit.runDiagnostic(false);
        unit.setSalvage(false);
        unit.resetPilotAndEntity();
    }

    /**
     * Restores a unit using the old per-part logic.
     *
     * @param campaign The campaign which owns the unit.
     * @param unit     The unit to restore.
     */
    private void oldUnitRestoration(Campaign campaign, Unit unit) {
        LOGGER.warn("Falling back to old unit restoration logic");

        unit.setSalvage(false);

        boolean needsCheck = true;
        while (unit.isAvailable() && needsCheck) {
            needsCheck = false;
            List<Part> parts = new ArrayList<>(unit.getParts());
            for (Part part : parts) {
                if (part instanceof MissingPart) {
                    // Make sure we restore both left and right thrusters
                    if (part instanceof MissingThrusters) {
                        if (((Aero) unit.getEntity()).getLeftThrustHits() > 0) {
                            ((MissingThrusters) part).setLeftThrusters(true);
                        }
                    }
                    // We magically acquire a replacement part, then fix the missing one.
                    campaign.getQuartermaster().addPart(((MissingPart) part).getNewPart(), 0, false);
                    part.fix();
                    part.resetTimeSpent();
                    part.resetOvertime();
                    part.setTech(null);
                    part.cancelReservation();
                    part.remove(false);
                    needsCheck = true;
                } else {
                    if (part.needsFixing()) {
                        needsCheck = true;
                        part.fix();
                    } else {
                        part.resetRepairSettings();
                    }
                    part.resetTimeSpent();
                    part.resetOvertime();
                    part.setTech(null);
                    part.cancelReservation();
                }

                // replace damaged armor and reload ammo bins after fixing their respective
                // locations
                if (part instanceof Armor armor) {
                    armor.setAmount(armor.getTotalAmount());
                } else if (part instanceof AmmoBin ammoBin) {

                    // we magically find the ammo we need, then load the bin
                    // we only want to get the amount of ammo the bin actually needs
                    if (ammoBin.getShotsNeeded() > 0) {
                        ammoBin.setShotsNeeded(0);
                        ammoBin.updateConditionFromPart();
                    }
                }

            }

            // TODO: Make this less painful. We just want to fix hips and shoulders.
            Entity entity = unit.getEntity();
            if (entity instanceof Mek) {
                for (int loc : new int[] { Mek.LOC_CENTER_LEG, Mek.LOC_LEFT_LEG, Mek.LOC_RIGHT_LEG, Mek.LOC_LEFT_ARM,
                                           Mek.LOC_RIGHT_ARM }) {
                    int numberOfCriticalSlots = entity.getNumberOfCriticalSlots(loc);
                    for (int crit = 0; crit < numberOfCriticalSlots; ++crit) {
                        CriticalSlot slot = entity.getCritical(loc, crit);
                        if (null != slot) {
                            slot.setHit(false);
                            slot.setDestroyed(false);
                        }
                    }
                }
            }
        }
    }

    /**
     * Implementations allow copies of entities to be created.
     */
    public interface IEntityCopyFactory {
        /**
         * Gets a copy of the entity.
         *
         * @param entity The entity to copy.
         *
         * @return A copy of the entity, or {@code null} if a copy could not be made.
         */
        @Nullable
        Entity copy(Entity entity);
    }

    /**
     * Gets a copy of the entity from the file system, via {@link MekSummaryCache} and {@link MekFileParser}.
     */
    private static class FileSystemEntityCopyFactory implements IEntityCopyFactory {
        /**
         * Get a copy of the entity from the {@link MekSummaryCache}.
         *
         * @param entity The entity to copy.
         *
         * @return A copy of the entity, or {@code null} if a copy could not be made.
         */
        @Override
        public @Nullable Entity copy(Entity entity) {
            final MekSummary ms = MekSummaryCache.getInstance().getMek(entity.getShortNameRaw());
            try {
                if (ms != null) {
                    return new MekFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
                }
            } catch (EntityLoadingException e) {
                LOGGER.error("Cannot restore unit from entity, could not find: {}", entity.getShortNameRaw(), e);
            }

            return null;
        }
    }
}
