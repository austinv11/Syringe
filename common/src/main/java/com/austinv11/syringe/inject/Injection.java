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
package com.austinv11.syringe.inject;

import com.austinv11.syringe.Syringe;

/**
 * This representation an action of injection.
 */
public abstract class Injection {

    private final Syringe syringe;

    private final InjectionTarget target;
    private final InjectionDelta delta;

    private final InjectionSiteFilter filter;

    public Injection(Syringe syringe, InjectionTarget target, InjectionDelta delta, InjectionSiteFilter filter) {
        this.syringe = syringe;
        this.target = target;
        this.delta = delta;
        this.filter = filter;
    }

    public Syringe getSyringe() {
        return syringe;
    }

    public InjectionTarget getTarget() {
        return target;
    }

    public InjectionDelta getDelta() {
        return delta;
    }

    public InjectionSiteFilter getFilter() {
        return filter;
    }
}
