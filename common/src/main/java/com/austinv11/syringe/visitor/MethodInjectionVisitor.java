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

package com.austinv11.syringe.visitor;

import com.austinv11.syringe.inject.Injection;
import com.austinv11.syringe.inject.sites.ClassSite;
import com.austinv11.syringe.inject.sites.MethodSite;
import com.austinv11.syringe.util.Lazy;

import java.util.Optional;

public interface MethodInjectionVisitor extends InjectionVisitor {

    Optional<? extends Injection<MethodSite>> visitMethod(Lazy<MethodSite> site);

    @Override
    default Injection<?>[] visit(Lazy<ClassSite> site) {
        Lazy<MethodSite>[] fields = site.get().getMethods();
        Injection<?>[] injections = new Injection[fields.length];
        int effectiveI = 0;
        for (Lazy<MethodSite> field : fields) {
            Optional<? extends Injection> injection = visitMethod(field);
            if (injection.isPresent()) {
                injections[effectiveI++] = injection.get();
            }
        }
        if (effectiveI < fields.length) {
            Injection<?>[] realInjections = new Injection[effectiveI];
            System.arraycopy(injections, 0, realInjections, 0, effectiveI);
            return realInjections;
        } else {
            return injections;
        }
    }
}
