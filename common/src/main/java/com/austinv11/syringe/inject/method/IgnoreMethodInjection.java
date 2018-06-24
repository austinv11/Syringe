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

package com.austinv11.syringe.inject.method;

import com.austinv11.syringe.inject.AnnotationInfo;
import com.austinv11.syringe.inject.Injection;
import com.austinv11.syringe.inject.InjectionDelta;
import com.austinv11.syringe.inject.InjectionTarget;
import com.austinv11.syringe.inject.sites.MethodSite;
import com.austinv11.syringe.util.Lazy;

import javax.annotation.Nullable;

public abstract class IgnoreMethodInjection extends Injection<MethodSite> {

    public IgnoreMethodInjection() {
        super(InjectionTarget.METHOD, InjectionDelta.REMOVAL);
    }

    public abstract boolean shouldIgnore(@Nullable Object thisInstance,
                                         Object[] params,
                                         Lazy<MethodSite> methodInfo);
}
