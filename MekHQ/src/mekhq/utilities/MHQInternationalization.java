/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package mekhq.utilities;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

import megamek.MegaMek;

/**
 * Class to handle MHQInternationalization (you will find online material on that looking for i18n) It makes use of some
 * short names to make it easier to use since it is used in many places
 */
public class MHQInternationalization {
    private static final String MISSING_RESOURCE_TAG = "!";

    private final String defaultBundle;
    private final ConcurrentHashMap<String, ResourceBundle> resourceBundles = new ConcurrentHashMap<>();
    protected static MHQInternationalization instance;

    static {
        instance = new MHQInternationalization("mekhq.resources.GUI");
    }

    public static MHQInternationalization getInstance() {
        return instance;
    }

    protected MHQInternationalization(String defaultBundle) {
        this.defaultBundle = defaultBundle;
    }

    private static class UTF8Control extends ResourceBundle.Control {
        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader,
              boolean reload)
              throws IOException {
            // The below is one approach; there are multiple ways to do this
            String resourceName = toResourceName(toBundleName(baseName, locale), "properties");
            try (InputStream is = loader.getResourceAsStream(resourceName);
                  InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                return new PropertyResourceBundle(isr);
            }
        }
    }

    ResourceBundle getResourceBundle(String bundleName) {
        return resourceBundles.computeIfAbsent(bundleName, k ->
                                                                 ResourceBundle.getBundle(bundleName,
                                                                       MegaMek.getMMOptions().getLocale(),
                                                                       new UTF8Control()));
    }

    /**
     * Get a localized string from a specific bundle
     *
     * @param bundleName the name of the bundle
     * @param key        the key of the string
     *
     * @return the localized string
     */
    public static String getTextAt(String bundleName, String key) {
        if (getInstance().getResourceBundle(bundleName).containsKey(key)) {
            return getInstance().getResourceBundle(bundleName).getString(key);
        }
        return MISSING_RESOURCE_TAG + key + MISSING_RESOURCE_TAG;
    }

    /**
     * Get a localized string from the default bundle
     *
     * @param key the key of the string
     *
     * @return the localized string
     */
    public static String getText(String key) {
        return getTextAt(getInstance().defaultBundle, key);
    }

    /**
     * Get a formatted localized string from the default bundle
     *
     * @param key  the key of the string
     * @param args the arguments to format the string
     *
     * @return the localized string
     */
    public static String getFormattedText(String key, Object... args) {
        return MessageFormat.format(getFormattedTextAt(getInstance().defaultBundle, key), args);
    }

    /**
     * Get a formatted localized string from the default bundle
     *
     * @param bundleName the name of the bundle
     * @param key        the key of the string
     * @param args       the arguments to format the string
     *
     * @return the localized string
     */
    public static String getFormattedTextAt(String bundleName, String key, Object... args) {
        return MessageFormat.format(getTextAt(bundleName, key), args);
    }


    /**
     * Checks if the given text is valid. A valid string does not start or end with an exclamation mark ('!').
     *
     * <p>If {@link MHQInternationalization} fails to fetch a valid return it returns the key
     * between two {@code !}. So by checking the returned string doesn't begin and end with that punctuation, we can
     * easily verify that all statuses have been provided results for the key s we're using.</p>
     *
     * @param text The text to validate.
     *
     * @return {@code true} if the text is valid (does not start and end with an '!'); {@code false} otherwise.
     */
    public static boolean isResourceKeyValid(String text) {
        return !(text.startsWith(MISSING_RESOURCE_TAG) && text.endsWith(MISSING_RESOURCE_TAG));
    }
}
