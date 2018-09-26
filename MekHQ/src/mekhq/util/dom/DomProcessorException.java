/*
 * MekHQ - Copyright (C) 2018 - The MekHQ Team
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package mekhq.util.dom;

/**
 * Signals an unexpected condition while using a {@linkplain DomProcessor}
 */
public class DomProcessorException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @see RuntimeException#RuntimeException(String, Throwable)
     */
    public DomProcessorException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @see RuntimeException#RuntimeException(String)
     */
    public DomProcessorException(String message) {
        super(message);
    }

    /**
     * @see RuntimeException#RuntimeException(Throwable)
     */
    public DomProcessorException(Throwable cause) {
        super(cause);
    }

}
