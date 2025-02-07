/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.utilities.glossary;

/**
 * Represents an immutable glossary entry containing details about a specific term. This record is
 * designed to encapsulate the key information required to define or describe an entry in a
 * glossary.
 *
 * <p>Validation is performed during construction to ensure none of the fields are {@code null} or
 * blank.</p>
 *
 * <p>Fields:</p>
 * <ul>
 *     <li>{@code title}: A short, human-friendly title for the glossary entry</li>
 *     <li>{@code description}: A detailed explanation or definition of the glossary entry</li>
 * </ul>
 *
 * @param title The title of the glossary entry. Must not be {@code null} or blank.
 * @param description The description of the glossary entry. Must not be {@code null} or blank.
 */
public record GlossaryEntry(String title, String description) {
    /**
     * Compact constructor for {@code GlossaryEntry}.
     * Performs validation to ensure all fields are non-null and non-blank.
     *
     * @param title       The title of the glossary entry.
     * @param description The description of the glossary entry.
     * @throws IllegalArgumentException If any field is {@code null} or blank.
     */
    public GlossaryEntry {
        validateField(title, "Title");
        validateField(description, "Description");
    }

    /**
     * Validates that the provided field is neither {@code null} nor blank.
     *
     * @param field     The field to validate.
     * @param fieldName The name of the field (used in exception messages).
     * @throws IllegalArgumentException If the field is {@code null} or blank.
     */
    private static void validateField(String field, String fieldName) {
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be null or blank.");
        }
    }
}
