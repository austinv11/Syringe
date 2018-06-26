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
import com.austinv11.syringe.util.Lazy;

import java.util.Optional;

public interface ClassInjectionVisitor extends InjectionVisitor {

    Optional<? extends Injection> visitClass(Lazy<ClassSite> site);

    @Override
    default Injection<?>[] visit(Lazy<ClassSite> site) {
        Optional<? extends Injection> injection = visitClass(site);
        return injection.map(injection1 -> new Injection[]{injection1}).orElseGet(() -> new Injection[0]);
    }
}
