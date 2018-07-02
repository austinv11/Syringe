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

import com.austinv11.syringe.direct.DirectFieldAccessor;
import com.austinv11.syringe.direct.DirectMethodAccessor;
import com.austinv11.syringe.inject.clazz.AddMethodInjection;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Deprecated
public class Test {

    public final static Runnable t2 = () -> {};

    private Object t;

    public static void thing(String hi) {
        hi.replace("f", "F");
    }

    public Test(AddMethodInjection.MethodDefinition def) throws Throwable {

        Object[] o = new Object[1];
        o[0] = "gr";

        DirectFieldAccessor accessor1 = new DirectFieldAccessor(t);
        Map<String, DirectFieldAccessor> accessors1 = new HashMap<>();
        accessors1.put("t", accessor1);
        DirectMethodAccessor accessor = new DirectMethodAccessor(new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] objects) {
                return thing(objects[0]);
            }
        });
        Map<String, DirectMethodAccessor> accessors = new HashMap<>();
        accessors.put("thing", accessor);
        def.callback(this, new Object[]{def}, accessors1, accessors);
        t = accessors1.get("t");
    }

    public Test(String s) {

    }

    private Object thing(Object i) {
        return t;
    }
}
