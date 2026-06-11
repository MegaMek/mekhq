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
 * of The Topps Company Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.campaignOptions;

import java.util.List;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Describes a navigable Campaign Options route.
 */
class CampaignOptionsRoute {
    private final String id;
    private final List<String> path;
    private final List<String> titleResourceNames;
    private final boolean showHelpPanel;
    private final String searchableText;
    private String sectionSearchText = "";

    CampaignOptionsRoute(@Nonnull String id, @Nonnull List<String> path, @Nonnull List<String> titleResourceNames) {
        this(id, path, titleResourceNames, true);
    }

    CampaignOptionsRoute(@Nonnull String id, @Nonnull List<String> path, @Nonnull List<String> titleResourceNames,
          boolean showHelpPanel) {
        this.id = id;
        this.path = List.copyOf(path);
        this.titleResourceNames = List.copyOf(titleResourceNames);
        this.showHelpPanel = showHelpPanel;
        this.searchableText = normalizeSearchText(String.join(" ", path) + " " + id + " "
                                                         + String.join(" ", titleResourceNames));
    }

    @Nonnull String getId() {
        return id;
    }

    @Nonnull List<String> getPath() {
        return path;
    }

    @Nonnull List<String> getTitleResourceNames() {
        return titleResourceNames;
    }

    boolean shouldShowHelpPanel() {
        return showHelpPanel;
    }

    @Nonnull String getTopLevelResourceName() {
        return titleResourceNames.get(0);
    }

    boolean isTopLevelRoute() {
        return titleResourceNames.size() == 1;
    }

    /**
     * Adds resolved section title and summary text to this route's search index so the navigation filter can match a
     * section heading, not only the page (tab) title.
     *
     * @param text the raw section text to index; ignored when {@code null} or blank
     */
    void setSectionSearchText(@Nullable String text) {
        this.sectionSearchText = text == null ? "" : normalizeSearchText(text);
    }

    boolean matches(@Nonnull String normalizedFilter) {
        if (normalizedFilter.isBlank()) {
            return true;
        }

        for (String token : normalizedFilter.split("\\s+")) {
            if (!token.isBlank() && !searchableText.contains(token) && !sectionSearchText.contains(token)) {
                return false;
            }
        }

        return true;
    }

    static @Nonnull String normalizeSearchText(@Nonnull String text) {
        return text.toLowerCase().replaceAll("[^a-z0-9]+", " ").trim();
    }

    @Override
    public @Nonnull String toString() {
        return id;
    }
}
