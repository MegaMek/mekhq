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
package mekhq.gui.campaignOptions.components;

import javax.swing.JComponent;

import jakarta.annotation.Nullable;
import mekhq.gui.baseComponents.MHQCollapsiblePanel;

/**
 * Implemented by a collapsible section's content component when it wants to surface a small control and an
 * enabled/disabled state in the section header itself, so the state can be read and toggled without expanding the
 * section.
 *
 * <p>When a {@link CampaignOptionsPagePanel} section's content implements this interface, the page mounts
 * {@link #getSectionHeaderControl()} at the trailing edge of the {@link MHQCollapsiblePanel} header and mutes the
 * collapsed section title whenever {@link #isSectionEnabled()} reports {@code false}. The content notifies the page of
 * state changes through the callback registered with {@link #setSectionStateListener(Runnable)}.</p>
 */
public interface SectionHeaderControlProvider {
    /**
     * @return the component to mount at the trailing edge of the section header (shown whether the section is collapsed
     *       or expanded), or {@code null} for none
     */
    @Nullable
    JComponent getSectionHeaderControl();

    /**
     * @return {@code true} if this section is currently enabled; the page uses this to style the section header title
     */
    boolean isSectionEnabled();

    /**
     * Registers a callback the content invokes whenever its enabled state changes, allowing the page to restyle the
     * section header. A {@code null} listener clears any previously registered callback.
     *
     * @param listener the callback to run on state changes, or {@code null} to clear it
     */
    void setSectionStateListener(@Nullable Runnable listener);
}
