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
package syringe.util;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Represents a lazily-retrieved and stored value. This tends to be used in scenarios where values must be computed in
 * order to get a usable value, where the computations are expensive.
 */
public final class Lazy<T> implements Supplier<T> {

    private final Supplier<T> supplier;
    @Nullable
    private volatile T obj = null;
    private volatile boolean isSet = false;

    public Lazy(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public Lazy(@Nullable T obj) {
        this.supplier = () -> obj;
    }

    public Lazy() {
        this.supplier = () -> null;
    }

    @Override
    @Nullable
    public T get() {
        if (!isSet) {
            obj = supplier.get();
            isSet = true;
        }
        return obj;
    }

    public Lazy<Optional<T>> optional() {
        return new Lazy<>(() -> Optional.ofNullable(supplier.get()));
    }

    public Lazy<T> or(Lazy<T> other) {
        return new Lazy<>(() -> {
            T firstTry = other.get();
            if (firstTry != null && (!firstTry.getClass().isArray() || Array.getLength(firstTry) > 0))
                return firstTry;
            return other.get();
        });
    }
}