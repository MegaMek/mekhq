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
package mekhq.campaign.digitalGM.stratCon.pointOfInterest;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import megamek.common.annotations.Nullable;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState.LocalDateAdapter;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;

/**
 * A point of interest placed in a StratCon sector: something on the map that is neither a scenario nor a facility.
 *
 * <p>This is the saved, per-instance half of a point of interest. It records its {@link #getTypeId() type ID}, and
 * everything that is the same for every point of interest of that type - whether it occupies its hex, whether it
 * starts hidden, its effects and its rules - comes from the type's {@link StratConPointOfInterestDefinition} and
 * {@link IStratConPointOfInterestBehavior}, looked up at runtime. Keeping this class the only saved one means new
 * types need no change to how campaigns are saved.</p>
 *
 * <p>Points of interest should be added to, moved around, and removed from a sector through its
 * {@link mekhq.campaign.digitalGM.stratCon.StratConTrackState} methods, never by changing {@link #setCoords} directly,
 * so the sector's lookup by hex stays correct.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConPointOfInterest {
    /**
     * Where a point of interest is in its life.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public enum PointOfInterestStatus {
        /** Still in play. */
        ACTIVE,
        /** Dealt with by the player - claimed, completed, or otherwise used up. */
        RESOLVED,
        /** Reached its expiry date without being resolved. */
        EXPIRED
    }

    private String id;
    private String typeId;
    private StratConCoords coords;
    private ForceAlignment owner;
    private PointOfInterestStatus status = PointOfInterestStatus.ACTIVE;
    private boolean revealed;
    private LocalDate expiryDate;
    private String displayNameOverride;
    private String descriptionOverride;
    private Map<String, String> state = new HashMap<>();

    /**
     * Creates a point of interest with a fresh ID and nothing else set. Used when loading saved campaigns, which
     * overwrite the ID with the saved one.
     */
    public StratConPointOfInterest() {
        id = UUID.randomUUID().toString();
    }

    /**
     * Creates a point of interest of the given type, at the given hex, with a fresh ID. The owner and expiry date are
     * left unset; see {@link #fromDefinition} to take them from a definition instead.
     *
     * @param typeId the type ID of the point of interest's definition
     * @param coords the hex it sits on
     *
     * @author Illiani
     * @since 0.51.01
     */
    public StratConPointOfInterest(String typeId, StratConCoords coords) {
        this();
        this.typeId = typeId;
        this.coords = coords;
    }

    /**
     * Creates a point of interest from a definition, taking its starting owner from the definition's default owner and
     * setting its expiry date from the definition's lifespan.
     *
     * @param definition the definition of the point of interest's type
     * @param coords     the hex it sits on
     * @param today      the current campaign date, from which the lifespan is counted
     *
     * @return the new point of interest; it is not yet placed in any sector
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static StratConPointOfInterest fromDefinition(StratConPointOfInterestDefinition definition,
          StratConCoords coords, LocalDate today) {
        StratConPointOfInterest pointOfInterest = new StratConPointOfInterest(definition.getTypeId(), coords);
        pointOfInterest.setOwner(definition.getDefaultOwner());

        if (definition.getLifespanDays() > 0) {
            pointOfInterest.setExpiryDate(today.plusDays(definition.getLifespanDays()));
        }

        return pointOfInterest;
    }

    /**
     * @return this point of interest's unique ID, stable across saves; strategic objectives refer to it by this ID
     */
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTypeId() {
        return typeId;
    }

    public void setTypeId(String typeId) {
        this.typeId = typeId;
    }

    public StratConCoords getCoords() {
        return coords;
    }

    /**
     * Sets the hex this point of interest sits on. Once it is placed in a sector, move it with
     * {@link mekhq.campaign.digitalGM.stratCon.StratConTrackState#movePointOfInterest} instead, so the sector's lookup
     * by hex stays correct.
     */
    public void setCoords(StratConCoords coords) {
        this.coords = coords;
    }

    /**
     * @return who owns this point of interest, or {@code null} if it is neutral
     */
    public @Nullable ForceAlignment getOwner() {
        return owner;
    }

    public void setOwner(@Nullable ForceAlignment owner) {
        this.owner = owner;
    }

    public PointOfInterestStatus getStatus() {
        return status;
    }

    public void setStatus(PointOfInterestStatus status) {
        this.status = (status == null) ? PointOfInterestStatus.ACTIVE : status;
    }

    /**
     * @return {@code true} once a player force has scouted this point of interest's hex
     */
    public boolean isRevealed() {
        return revealed;
    }

    public void setRevealed(boolean revealed) {
        this.revealed = revealed;
    }

    /**
     * @return the date this point of interest expires on, or {@code null} if it never expires
     */
    @XmlJavaTypeAdapter(value = LocalDateAdapter.class)
    public @Nullable LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(@Nullable LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    /**
     * @return a name for this point of interest that replaces its definition's, or {@code null} to use the
     *       definition's
     */
    public @Nullable String getDisplayNameOverride() {
        return displayNameOverride;
    }

    public void setDisplayNameOverride(@Nullable String displayNameOverride) {
        this.displayNameOverride = displayNameOverride;
    }

    /**
     * @return a description for this point of interest that replaces its definition's, or {@code null} to use the
     *       definition's
     */
    public @Nullable String getDescriptionOverride() {
        return descriptionOverride;
    }

    public void setDescriptionOverride(@Nullable String descriptionOverride) {
        this.descriptionOverride = descriptionOverride;
    }

    /**
     * Free-form, saved key/value storage for a type's behavior to keep per-instance state in (for example, a
     * countdown or which formation claimed it). Used for saving; prefer {@link #getStateValue} and
     * {@link #setStateValue}.
     */
    public Map<String, String> getState() {
        return state;
    }

    public void setState(Map<String, String> state) {
        this.state = (state == null) ? new HashMap<>() : state;
    }

    /**
     * @param key the state key
     *
     * @return the stored value, or {@code null} if none is stored under that key
     *
     * @author Illiani
     * @since 0.51.01
     */
    public @Nullable String getStateValue(String key) {
        return state.get(key);
    }

    /**
     * Stores a value under the given key, or removes the key if the value is {@code null}.
     *
     * @param key   the state key
     * @param value the value to store, or {@code null} to remove the key
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setStateValue(String key, @Nullable String value) {
        if (value == null) {
            state.remove(key);
        } else {
            state.put(key, value);
        }
    }

    /**
     * @return this point of interest's definition, or {@code null} if its type is no longer defined
     *
     * @author Illiani
     * @since 0.51.01
     */
    @XmlTransient
    public @Nullable StratConPointOfInterestDefinition getDefinition() {
        return StratConPointOfInterestDefinitions.getDefinition(typeId);
    }

    /**
     * @return the behavior driving this point of interest's type; the default, inert behavior if its type is no longer
     *       defined or names no registered behavior
     *
     * @author Illiani
     * @since 0.51.01
     */
    @XmlTransient
    public IStratConPointOfInterestBehavior getBehavior() {
        StratConPointOfInterestDefinition definition = getDefinition();
        return StratConPointOfInterestBehaviors.getBehavior((definition == null) ? null : definition.getBehaviorId());
    }

    /**
     * Whether this point of interest takes up its hex, the way a facility or scenario does. A point of interest whose
     * type is no longer defined does not, so a removed data file cannot leave a hex blocked by something unseen.
     *
     * @return {@code true} if this point of interest occupies its hex
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean occupiesHex() {
        StratConPointOfInterestDefinition definition = getDefinition();
        return (definition != null) && definition.isOccupiesHex();
    }

    /**
     * Whether the player can see this point of interest. It is visible if any of these hold:
     * <ul>
     *     <li>its type is not hidden until scouted;</li>
     *     <li>it has been revealed, or its hex has been scouted;</li>
     *     <li>it belongs to the player or an ally;</li>
     *     <li>the whole sector is revealed, by a facility or by the GM.</li>
     * </ul>
     * A point of interest whose type is no longer defined is treated as hidden until scouted.
     *
     * @param track the sector this point of interest sits in
     *
     * @return {@code true} if the player can see it
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isVisibleToPlayer(StratConTrackState track) {
        if (revealed || isOwnerAlliedToPlayer() || track.isGmRevealed() || track.hasActiveTrackReveal()) {
            return true;
        }

        if ((coords != null) && track.getRevealedCoords().contains(coords)) {
            return true;
        }

        StratConPointOfInterestDefinition definition = getDefinition();
        return (definition != null) && !definition.isHiddenUntilScouted();
    }

    /**
     * @return {@code true} if this point of interest has no owner
     *
     * @author Illiani
     * @since 0.51.01
     */
    @XmlTransient
    public boolean isNeutral() {
        return owner == null;
    }

    /**
     * @return {@code true} if this point of interest is owned by the player or an ally of the player
     *
     * @author Illiani
     * @since 0.51.01
     */
    @XmlTransient
    public boolean isOwnerAlliedToPlayer() {
        return (owner == ForceAlignment.Allied) || (owner == ForceAlignment.Player);
    }

    /**
     * @return {@code true} while this point of interest is still in play (neither resolved nor expired)
     *
     * @author Illiani
     * @since 0.51.01
     */
    @XmlTransient
    public boolean isActive() {
        return status == PointOfInterestStatus.ACTIVE;
    }

    /**
     * Whether this point of interest's expiry date has come. It expires at the start of its expiry date, so on that
     * date it is already due.
     *
     * @param today the current campaign date
     *
     * @return {@code true} if this point of interest has an expiry date that is today or earlier
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean hasReachedExpiryDate(LocalDate today) {
        return (expiryDate != null) && !today.isBefore(expiryDate);
    }

    /**
     * @return the name to show for this point of interest: its override, else its definition's name, else its type ID
     *
     * @author Illiani
     * @since 0.51.01
     */
    @XmlTransient
    public String getDisplayableName() {
        if (displayNameOverride != null) {
            return displayNameOverride;
        }

        StratConPointOfInterestDefinition definition = getDefinition();
        if ((definition != null) && (definition.getDisplayableName() != null)) {
            return definition.getDisplayableName();
        }

        return typeId;
    }

    /**
     * @return the description to show for this point of interest: its override, else its definition's description, or
     *       {@code null} if neither has one
     *
     * @author Illiani
     * @since 0.51.01
     */
    @XmlTransient
    public @Nullable String getDescription() {
        if (descriptionOverride != null) {
            return descriptionOverride;
        }

        StratConPointOfInterestDefinition definition = getDefinition();
        return (definition == null) ? null : definition.getDescription();
    }

    @Override
    public String toString() {
        return String.format("%s (%s) at %s", getDisplayableName(), id, coords);
    }
}
