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
package com.austinv11.syringe.util;

import com.austinv11.syringe.inject.sites.ClassSite;

public class Property {

    private final ClassSite type;
    private final boolean initialized;
    private final Object value;

    public Property(ClassSite type, boolean initialized, Object value) {
        this.type = type;
        this.initialized = initialized;
        this.value = value;
    }

    public ClassSite getType() {
        return type;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public Object getValue() {
        return value;
    }
}