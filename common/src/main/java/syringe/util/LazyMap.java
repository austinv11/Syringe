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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Simple lazy + immutability enforcing map wrapper. It's purposefully limited.
 */
public class LazyMap<K, V> {

    public static <K, V> LazyMap<K, V> convert(Map<K, Lazy<V>> map) {
        return new LazyMap<>(new HashMap<>(), map.keySet(), k -> map.getOrDefault(k, new Lazy<>()).get());
    }

    private final Map<K, V> map;
    private final Set<K> keys;
    private final Function<K, V> generator;

    public LazyMap(Map<K, V> map, Set<K> keys, Function<K, V> generator) {
        this.map = map;
        this.keys = keys;
        this.generator = generator;
    }

    public LazyMap(Map<K, V> map) {
        this(map, map.keySet(), map::get);
    }

    public V get(K key) {
        return map.computeIfAbsent(key, generator);
    }

    public Set<K> keys() {
        return keys;
    }
}
