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

import java.util.function.Supplier;

/**
 * Represents a lazily-retrieved and stored value.
 */
public final class Lazy<T> {

    private final Supplier<T> supplier;
    private volatile T obj = null;
    private volatile boolean isSet = false;

    public Lazy(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public Lazy(T obj) {
        this.supplier = () -> obj;
    }

    public T get() {
        if (!isSet) {
            obj = supplier.get();
            isSet = true;
        }
        return obj;
    }
}
