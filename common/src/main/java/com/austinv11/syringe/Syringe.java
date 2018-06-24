/*
 * This file is part of Syringe.
 *
 * Syringe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Syringe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Syringe.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.austinv11.syringe;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Simple data class designed for "signing" visited and modified classes from syringes.
 */
public final class Syringe implements Serializable {

    private static final long serialVersionUID = -3077886476765797679L;

    /**
     * Checks if an object was visited by a syringe.
     *
     * @param o The object to check.
     * @return True if visited, false if otherwise.
     */
    public static boolean wasVisited(Object o) {
        return o instanceof SyringeVisited;
    }

    /**
     * Checks if an object was modified by a syringe.
     *
     * @param o The object to check.
     * @return True if modified, false if otherwise.
     */
    public static boolean wasModified(Object o) {
        return wasVisited(o) && ((SyringeVisited) o).didSyringeModify();
    }

    /**
     * Gets the syringe information which visited/modified an object.
     *
     * @param o The object to get the syringe from.
     * @return The syringe information if visited by a syringe, null if no syringe visited the object.
     */
    public static @Nullable Syringe getSyringe(Object o) {
        if (!wasVisited(o))
            return null;

        return ((SyringeVisited) o).getSyringe();
    }

    private final String name;
    private final String version;
    private final String description;

    public Syringe(String name, String version, String description) {
        this.name = name;
        this.version = version;
        this.description = description;
    }

    /**
     * Gets the human-readable name of the syringe.
     *
     * @return The syringe name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the version of the syringe. This is entirely arbitrary.
     *
     * @return The syringe version.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Gets the description of the syringe.
     *
     * @return The syringe description.
     */
    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Syringe)) {
            return false;
        }
        Syringe syringe = (Syringe) o;
        return Objects.equals(getName(), syringe.getName()) &&
                Objects.equals(getVersion(), syringe.getVersion()) &&
                Objects.equals(getDescription(), syringe.getDescription());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getVersion(), getDescription());
    }

    @Override
    public String toString() {
        return "Syringe{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", description='" + description + '\'' +
                '}';
    }

    /**
     * An interface which gets programmatically added to visited classes from syringes.
     */
    public interface SyringeVisited {

        /**
         * The syringe descriptor of the visitor.
         *
         * @return The syringe descriptor.
         */
        Syringe getSyringe();

        /**
         * Gets whether the syringe actually modified this class in anyway, making it different from the
         * source it's compiled from.
         *
         * @return True if the syringe actually made changes.
         */
        boolean didSyringeModify(); //Weird name to prevent potential conflict
    }
}
