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

package trash;

import syringe.access.FieldAccessor;
import syringe.access.LazyMap;
import syringe.access.MethodAccessor;

import javax.annotation.Nullable;
import java.util.HashMap;

public class Test {

    private final static ThingAccessor thingAccessor = new ThingAccessor();
    private final Thing2Accessor thing2Accessor = new Thing2Accessor();

    public static void main(String[] args) {
        HashMap<String, MethodAccessor> map = new HashMap<>();
        map.put("thingAccessor", thingAccessor);
        LazyMap<String, MethodAccessor> lazyMap = new LazyMap<>(map);
    }

    static String thing(Object o1) throws Exception {
        return "";
    }

    static class ThingAccessor implements MethodAccessor {

        @Nullable
        @Override
        public Object invoke(@Nullable Object... args) throws Throwable {
            return thing(args[0]);
        }
    }

    String thing2(Object o2) throws Exception {
        return "";
    }

    class Thing2Accessor implements MethodAccessor {

        @Nullable
        @Override
        public Object invoke(@Nullable Object... args) throws Throwable {
            return thing2(args[0]);
        }
    }
}
