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
package mekhq.gui.dialog.glossary;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import megamek.Version;
import megamek.client.ui.preferences.PreferenceElement;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.logging.MMLogger;
import mekhq.MekHQ;

/**
 * Remembers which glossary entries the player has read, in the version they read them. An entry updated since it was
 * read counts as unread again, so the console can point the player at what changed.
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class GlossaryReadPreferences {
    private static final MMLogger LOGGER = MMLogger.create(GlossaryReadPreferences.class);
    static final String READ_ENTRIES = "readEntries";
    private static final String SEPARATOR = ",";
    private static final String VERSION_MARK = "@";

    private static GlossaryReadPreferences instance;

    private final Set<String> read = new LinkedHashSet<>();

    GlossaryReadPreferences(PreferencesNode node) {
        try {
            node.manage(new ReadList(this));
        } catch (Exception exception) {
            LOGGER.error(exception, "Failed to register preference {}", READ_ENTRIES);
        }
    }

    /**
     * @return the player's read entries, loaded from MekHQ's preferences the first time it is asked for
     */
    public static synchronized GlossaryReadPreferences getInstance() {
        if (instance == null) {
            instance = new GlossaryReadPreferences(MekHQ.getMHQPreferences().forClass(GlossaryReadPreferences.class));
        }
        return instance;
    }

    /**
     * @param lookUpName the entry's lookup name
     * @param version    the version the entry was added or last updated in
     *
     * @return {@code true} if the player has read the entry since it was last updated
     */
    public boolean isRead(String lookUpName, Version version) {
        return read.contains(record(lookUpName, version));
    }

    /**
     * Notes that the player has read an entry as it stands in the given version. A record of an older version of the
     * same entry is dropped.
     *
     * @param lookUpName the entry's lookup name
     * @param version    the version the entry was added or last updated in
     */
    public void markRead(String lookUpName, Version version) {
        read.removeIf(entry -> entry.startsWith(lookUpName + VERSION_MARK));
        read.add(record(lookUpName, version));
    }

    /**
     * Builds the record from the version's numbers alone. {@link Version#toString()} is not used because it appends
     * the running build's branch and git hash, which would make every entry unread again with each new build.
     */
    private static String record(String lookUpName, Version version) {
        return lookUpName + VERSION_MARK + String.format("%d.%02d.%02d%s",
              version.getMajor(),
              version.getMinor(),
              version.getPatch(),
              version.hasRevision() ? "." + version.getRevision() : "");
    }

    String serialize() {
        return String.join(SEPARATOR, read);
    }

    void deserialize(String saved) {
        read.clear();
        Arrays.stream(saved.split(SEPARATOR))
              .map(String::strip)
              .filter(entry -> entry.contains(VERSION_MARK))
              .forEach(read::add);
    }

    /** Saves the read entries as one comma-separated preference. */
    private static final class ReadList extends PreferenceElement {
        private final GlossaryReadPreferences owner;

        private ReadList(GlossaryReadPreferences owner) throws Exception {
            super(READ_ENTRIES);
            this.owner = owner;
        }

        @Override
        protected String getValue() {
            return owner.serialize();
        }

        @Override
        protected void initialize(String value) {
            owner.deserialize(value);
        }

        @Override
        protected void dispose() {
            // Nothing to release: the list is not tied to a component.
        }
    }
}
