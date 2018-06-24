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
package com.austinv11.syringe.cglib;

import com.austinv11.syringe.inject.Injection;
import com.austinv11.syringe.inject.InjectionDelta;
import com.austinv11.syringe.inject.InjectionTarget;
import com.austinv11.syringe.inject.method.*;
import com.austinv11.syringe.inject.sites.ClassSite;
import com.austinv11.syringe.inject.sites.FieldSite;
import com.austinv11.syringe.inject.sites.MethodSite;
import com.austinv11.syringe.util.IncompatibleConfigurationException;
import com.austinv11.syringe.util.Lazy;
import com.austinv11.syringe.visitor.InjectionVisitor;
import net.sf.cglib.proxy.MethodProxy;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class CompiledInjectionVisitor implements InjectionVisitor {

    private final Collection<InjectionVisitor> originals;
    private volatile Optional<CompiledInjection> injection;

    public CompiledInjectionVisitor(Collection<InjectionVisitor> originals) {
        this.originals = originals;
    }

    @Override
    public Optional<Injection<ClassSite>> visitClass(Lazy<ClassSite> site) {
        return Optional.empty();
    }

    @Override
    public Optional<Injection<FieldSite>> visitField(Lazy<FieldSite> site) {
        return Optional.empty();
    }

    @Override
    public Optional<CompiledInjection> visitMethod(Lazy<MethodSite> site) {
        if (injection == null) {
            if (originals.size() == 0)
                injection = Optional.empty();
            else {
                Collection<Injection<MethodSite>> injections = originals.stream()
                                                                        .map(v -> v.visitMethod(site))
                                                                        .filter(Optional::isPresent)
                                                                        .map(Optional::get)
                                                                        .collect(Collectors.toList());
                if (injections.size() == 0)
                    injection = Optional.empty();
                else {
                    injection = Optional.of(new CompiledInjection(injections));
                }
            }
        }
        return injection;
    }

    public final static class CompiledInjection extends Injection<MethodSite> {

        private final List<Injection<MethodSite>> injections;

        public CompiledInjection(Collection<Injection<MethodSite>> injections) {
            super(InjectionTarget.METHOD, injections.size() == 0 ? InjectionDelta.NONE : InjectionDelta.MODIFICATION);
            this.injections = new ArrayList<>(injections);
            //Move removals and pre-hooks to be first and post hooks to be last
            this.injections.sort((o1, o2) -> {
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
            });
        }

        @Nullable
        public Object interceptCallback(Object obj,
                                        Method method,
                                        Object[] args,
                                        MethodProxy proxy,
                                        Lazy<MethodSite> siteInfo) throws Throwable {
            com.austinv11.syringe.proxy.MethodProxy sProxy = (a) -> proxy.invokeSuper(obj, a);
            boolean wasInvoked = false;
            Object returnVal = null;
            try {
                for (Injection<MethodSite> injection : injections) {
                    if (injection instanceof IgnoreMethodInjection) {
                        if (((IgnoreMethodInjection) injection).shouldIgnore(obj, args, siteInfo))
                            return null;
                    } else if (injection instanceof PreHookMethodInjection) {
                        args = ((PreHookMethodInjection) injection).hookParams(obj, args, siteInfo);
                    } else if (injection instanceof PostHookMethodInjection) {
                        if (!wasInvoked) {
                            returnVal = proxy.invokeSuper(obj, args);
                            wasInvoked = true;
                        }
                        returnVal = ((PostHookMethodInjection) injection).hookReturn(obj, args, returnVal, siteInfo);
                    } else if (injection instanceof ReplaceMethodInjection) {
                        if (!wasInvoked) {
                            returnVal = ((ReplaceMethodInjection) injection).apply(obj, args, sProxy, siteInfo);
                            wasInvoked = true;
                        } else {
                            Object finalReturnVal = returnVal;
                            returnVal = ((ReplaceMethodInjection) injection).apply(obj, args, (a) -> finalReturnVal, siteInfo);

                        }
                    } else if (!(injection instanceof ErrorRecoveryMethodInjection)) {
                        throw new IncompatibleConfigurationException("Cannot handle injection of type " + injection.getClass());
                    }
                }
                if (!wasInvoked)
                    return proxy.invokeSuper(obj, args);
                else
                    return returnVal;
            } catch (@Nullable Throwable t) {
                Collection<ErrorRecoveryMethodInjection> errorHandlers
                        = injections.stream()
                                    .filter(i -> i instanceof ErrorRecoveryMethodInjection)
                                    .map(i -> (ErrorRecoveryMethodInjection) i)
                                    .collect(Collectors.toList());
                for (ErrorRecoveryMethodInjection i : errorHandlers) {
                    try {
                        Object finalReturnVal1 = returnVal;
                        return i.tryRecovery(obj, args, t, wasInvoked ? (a) -> finalReturnVal1 : sProxy, siteInfo);
                    } catch (Throwable t2) {
                        t = t2;
                    }
                }
                throw t;
            }
        }
    }
}
