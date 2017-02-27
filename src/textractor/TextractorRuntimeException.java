/*
 * Copyright (C) 2004-2009 Institute for Computational Biomedicine,
 *                         Weill Medical College of Cornell University
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package textractor;

/**
 * Used to indicate a problem during Textractor execution.
 */
public final class TextractorRuntimeException extends RuntimeException {
    /**
     * Used during deserialization to verify that objects are compatible.
     */
    private static final long serialVersionUID = 1;

    /**
     * Constructs a new runtime exception with <code>null</code> as its
     * detail message.
     */
    public TextractorRuntimeException() {
        super();
    }

    /**
     * Constructs a new runtime exception with the specified detail message.
     * @param message The detail message.
     */
    public TextractorRuntimeException(final String message) {
        super(message);
    }

    /**
     * Constructs a new runtime exception with the specified cause and
     * <code>null</code> as its detail message..  This constructor is useful
     * for runtime exceptions that are little more than wrappers for other
     * throwables.
     *
     * @param cause The cause. (A <tt>null</tt> value is permitted, and
     * indicates that the cause is nonexistent or unknown.)
     */
    public TextractorRuntimeException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new runtime exception with the specified detail message and
     * cause.
     * @param message The detail message.
     * @param cause The cause. (A <tt>null</tt> value is permitted, and
     * indicates that the cause is nonexistent or unknown.)
     */
    public TextractorRuntimeException(final String message,
            final Throwable cause) {
        super(message, cause);
    }
}
