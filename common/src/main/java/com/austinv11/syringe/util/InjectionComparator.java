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

import com.austinv11.syringe.inject.Injection;
import com.austinv11.syringe.inject.InjectionDelta;
import com.austinv11.syringe.inject.method.PostHookMethodInjection;
import com.austinv11.syringe.inject.method.PreHookMethodInjection;

import java.util.Comparator;

public class InjectionComparator implements Comparator<Injection<?>> {

    @Override
    public int compare(Injection<?> o1, Injection<?> o2) {
        InjectionDelta d1 = o1.getDelta();
        InjectionDelta d2 = o2.getDelta();
        if (o1 instanceof PreHookMethodInjection)
            return -1;
        if (o2 instanceof PreHookMethodInjection)
            return 1;
        if (d1 == InjectionDelta.REMOVAL)
            return -1;
        if (d2 == InjectionDelta.REMOVAL)
            return 1;
        if (o1 instanceof PostHookMethodInjection)
            return 1;
        if (o2 instanceof PostHookMethodInjection)
            return -1;
        return 0;
    }
}
