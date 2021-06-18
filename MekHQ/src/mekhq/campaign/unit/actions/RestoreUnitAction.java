/*
 * Copyright (c) 2020 The Megamek Team. All rights reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.unit.actions;

import java.util.*;

import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.loaders.EntityLoadingException;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.UnitChangedEvent;
import mekhq.campaign.parts.*;
import mekhq.campaign.parts.equipment.*;
import mekhq.campaign.unit.Unit;

/**
 * Restores a unit to an undamaged state.
 */
public class RestoreUnitAction implements IUnitAction {

    private final IEntityCopyFactory entityCopyFactory;

    /**
     * Creates a new {@code RestoreUnitAction} instance using
     * the default means of creating entity copies.
     */
    public RestoreUnitAction() {
        this(new FileSystemEntityCopyFactory());
    }

    /**
     * Creates a new {@code RestoreUnitAction} instance using
     * the provided {@link IEntityCopyFactory}.
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
     * @param campaign The campaign which owns the unit.
     * @param unit The unit to restore.
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
        campaign.getGame().addEntity(newEntity.getId(), newEntity);

        unit.setEntity(newEntity);

        unit.removeParts();

        unit.initializeBaySpace();

        unit.initializeParts(true);
        unit.runDiagnostic(false);
        unit.setSalvage(false);
        unit.resetPilotAndEntity();
    }

    /**
     * Restores a unit using the old per-part logic.
     * @param campaign The campaign which owns the unit.
     * @param unit The unit to restore.
     */
    private void oldUnitRestoration(Campaign campaign, Unit unit) {
        MekHQ.getLogger().warning("Falling back to old unit restoration logic");

        unit.setSalvage(false);

        boolean needsCheck = true;
        while (unit.isAvailable() && needsCheck) {
            needsCheck = false;
            List<Part> parts = new ArrayList<>(unit.getParts());
            for (Part part : parts) {
                if (part instanceof MissingPart) {
                    //Make sure we restore both left and right thrusters
                    if (part instanceof MissingThrusters) {
                        if (((Aero) unit.getEntity()).getLeftThrustHits() > 0) {
                            ((MissingThrusters) part).setLeftThrusters(true);
                        }
                    }
                    // We magically acquire a replacement part, then fix the missing one.
                    campaign.getQuartermaster().addPart(((MissingPart) part).getNewPart(), 0);
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

                // replace damaged armor and reload ammo bins after fixing their respective locations
                if (part instanceof Armor) {
                    final Armor armor = (Armor) part;
                    armor.setAmount(armor.getTotalAmount());
                } else if (part instanceof AmmoBin) {
                    final AmmoBin ammoBin = (AmmoBin) part;

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
            if (entity instanceof Mech) {
                for (int loc : new int[] {
                    Mech.LOC_CLEG, Mech.LOC_LLEG, Mech.LOC_RLEG, Mech.LOC_LARM, Mech.LOC_RARM}) {
                    int numberOfCriticals = entity.getNumberOfCriticals(loc);
                    for (int crit = 0; crit < numberOfCriticals; ++ crit) {
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
         * @param entity The entity to copy.
         * @return A copy of the entity, or {@code null} if a copy could not be made.
         */
        @Nullable
        public Entity copy(Entity entity);
    }

    /**
     * Gets a copy of the entity from the file system, via {@link MechSummaryCache}
     * and {@link MechFileParser}.
     */
    private static class FileSystemEntityCopyFactory implements IEntityCopyFactory {
        /**
         * Get a copy of the entity from the {@link MechSummaryCache}.
         * @param entity The entity to copy.
         * @return A copy of the entity, or {@code null} if a copy could not be made.
         */
        @Nullable
        public Entity copy(Entity entity) {
            final MechSummary ms = MechSummaryCache.getInstance().getMech(entity.getShortNameRaw());
            try {
                if (ms != null) {
                    return new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
                }
            } catch (EntityLoadingException e) {
                MekHQ.getLogger().error("Cannot restore unit from entity, could not find: " + entity.getShortNameRaw(), e);
            }

            return null;
        }
    }
}
