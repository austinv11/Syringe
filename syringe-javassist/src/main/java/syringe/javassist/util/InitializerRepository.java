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

package syringe.javassist.util;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public final class InitializerRepository {

    private final static Map<String, Object> repo = new HashMap<>();

    private InitializerRepository() {}

    public static void put(String k, @Nullable Object o) {
        repo.put(k, o);
    }

    @Nullable
    public static Object getAndRemove(String k) {
        return repo.remove(k);
    }
}
