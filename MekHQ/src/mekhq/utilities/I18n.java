/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 *  This file is part of MekHQ.
 *
 *  MekHQ is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MekHQ is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.utilities;

import mekhq.MekHQ;

import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

/**
 * I18n class to handle internationalization
 * It makes use of some short names to make it easier to use since it is used in many places
 */
public class I18n {

    private static final String PREFIX = "mekhq.resources.";
    private static final String DEFAULT = "messages";
    private final ConcurrentHashMap<String, ResourceBundle> resourceBundles = new ConcurrentHashMap<>();
    private static final I18n instance = new I18n();

    private I18n() {
    }

    public static I18n getInstance() {
        return instance;
    }

    ResourceBundle getResourceBundle(String bundleName) {
        return resourceBundles.computeIfAbsent(bundleName, k ->
            ResourceBundle.getBundle(PREFIX + bundleName, MekHQ.getMHQOptions().getLocale()));
    }

    /**
     * Get a localized string from a specific bundle
     * @param bundleName the name of the bundle
     * @param key the key of the string
     * @return the localized string
     */
    public static String getLocalizedText(String bundleName, String key) {
        if (I18n.getInstance().getResourceBundle(bundleName).containsKey(key)) {
            return I18n.getInstance().getResourceBundle(bundleName).getString(key);
        }
        return "!" + key + "!";
    }

    /**
     * Get a localized string from a specific bundle
     * @param bundleName the name of the bundle
     * @param key the key of the string
     * @return the localized string
     */
    public static String t(String bundleName, String key) {
        return getLocalizedText(bundleName, key);
    }

    /**
     * Get a localized string from the default bundle
     * @param key the key of the string
     * @return the localized string
     */
    public static String t(String key) {
        return getLocalizedText(DEFAULT, key);
    }
    
    /**
     * Get a formatted localized string from the default bundle
     * @param key the key of the string
     * @param args the arguments to format the string
     * @return the localized string
     */
    public static String ft(String key, Object... args) {
        return MessageFormat.format(getFormattedLocalizedText(DEFAULT, key), args);
    }

    /**
     * Get a formatted localized string from a bundle
     * @param bundleName the name of the bundle
     * @param key the key of the string
     * @param args the arguments to format the string
     * @return the localized string
     */
    public static String flt(String bundleName, String key, Object... args) {
        return MessageFormat.format(getFormattedLocalizedText(bundleName, key), args);
    }


    /**
     * Get a formatted localized string from the default bundle
     * @param bundleName the name of the bundle
     * @param key the key of the string
     * @param args the arguments to format the string
     * @return the localized string
     */
    public static String getFormattedLocalizedText(String bundleName, String key, Object... args) {
        return MessageFormat.format(getLocalizedText(bundleName, key), args);
    }

}
