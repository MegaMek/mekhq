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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.SwingUtilities;

import jakarta.annotation.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.utilities.glossary.DocumentationEntry;

/**
 * Every guide that ships with MekHQ, read once in the background so the document viewer's library can search inside
 * them. Reading all of them takes a few seconds, so it starts when the viewer first opens and the library's search
 * grows as each guide is read.
 *
 * @author Illiani
 * @since 0.51.01
 */
final class GuideLibrary {
    private static final MMLogger LOGGER = MMLogger.create(GuideLibrary.class);

    private static GuideLibrary instance;

    private final Map<DocumentationEntry, GuideDocument> documents = new ConcurrentHashMap<>();
    private final Set<DocumentationEntry> unreadable = ConcurrentHashMap.newKeySet();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private final ExecutorService reader = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "Guide library reader");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });
    private boolean isReadingStarted;

    private GuideLibrary() {
    }

    /** @return the library, shared by every document viewer */
    static synchronized GuideLibrary getInstance() {
        if (instance == null) {
            instance = new GuideLibrary();
        }
        return instance;
    }

    /**
     * @param entry a guide
     *
     * @return the guide's file
     */
    static File fileFor(DocumentationEntry entry) {
        return new File(entry.getFileAddress());
    }

    /** @return every guide, in title order */
    static List<DocumentationEntry> entries() {
        return Arrays.stream(DocumentationEntry.values())
                     .sorted(Comparator.comparing(DocumentationEntry::getTitle, String.CASE_INSENSITIVE_ORDER))
                     .toList();
    }

    /**
     * @param entry a guide
     *
     * @return what has been read of the guide, or {@code null} if it has not been read yet
     */
    @Nullable GuideDocument document(DocumentationEntry entry) {
        return documents.get(entry);
    }

    /**
     * Keeps a guide the viewer has read while opening it, so it need not be read again.
     *
     * @param entry    the guide
     * @param document what was read of it
     */
    void store(DocumentationEntry entry, GuideDocument document) {
        if (documents.putIfAbsent(entry, document) == null) {
            fireChanged();
        }
    }

    /** @return {@code true} once every guide has been read, or found unreadable */
    boolean isComplete() {
        return documents.size() + unreadable.size() >= DocumentationEntry.values().length;
    }

    /** Starts reading every guide not yet read, one after another, unless that has already started. */
    synchronized void readAll() {
        if (isReadingStarted) {
            return;
        }
        isReadingStarted = true;
        for (DocumentationEntry entry : entries()) {
            reader.submit(() -> readOne(entry));
        }
    }

    private void readOne(DocumentationEntry entry) {
        if (documents.containsKey(entry)) {
            return;
        }
        try {
            documents.putIfAbsent(entry, GuideReader.read(fileFor(entry)));
        } catch (IOException | RuntimeException ex) {
            unreadable.add(entry);
            LOGGER.warn(ex, "Unable to read the guide {} for searching", entry.getFileAddress());
        }
        fireChanged();
    }

    /**
     * Asks to be told, on the Swing thread, whenever another guide has been read.
     *
     * @param listener called after each guide is read
     */
    void addListener(Runnable listener) {
        listeners.add(listener);
    }

    void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void fireChanged() {
        SwingUtilities.invokeLater(() -> listeners.forEach(Runnable::run));
    }

    /** @return every guide as the library's search sees it, in title order; unread guides have no page text yet */
    List<GuideSearch.Guide> searchable() {
        return entries().stream().map(entry -> {
            GuideDocument document = documents.get(entry);
            return new GuideSearch.Guide(entry.getLookUpName(), entry.getTitle(),
                  document == null ? List.of() : document.pageTexts());
        }).toList();
    }
}
