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

import com.austinv11.syringe.Syringe;
import com.austinv11.syringe.inject.InjectionDelta;
import com.austinv11.syringe.inject.sites.MethodSite;
import com.austinv11.syringe.util.Lazy;
import com.austinv11.syringe.visitor.InjectionVisitor;
import net.sf.cglib.core.Signature;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.objectweb.asm.Type;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class SyringeCallback implements MethodInterceptor {

    private static final Syringe.SyringeVisited visitedInstance = new Syringe.SyringeVisited() {
        @Override
        public Syringe getSyringe() {
            return CglibSyringe.SYRINGE;
        }

        @Override
        public boolean didSyringeModify() {
            return true;
        }
    };

    private final CglibSyringe syringe;
    private final Map<Method, Lazy<MethodSite>> siteCache = new ConcurrentHashMap<>();
    private final Map<Method, Optional<CompiledInjectionVisitor.CompiledInjection>> injectionCache
            = new ConcurrentHashMap<>();

    public SyringeCallback(CglibSyringe syringe) {
        this.syringe = syringe;
    }

    @Override
    @Nullable
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        Supplier<?> sMethod = getSyringeVisitedMethod(proxy);
        if (sMethod != null) {
            return sMethod.get();
        } else {
            CompiledInjectionVisitor visitor = new CompiledInjectionVisitor(syringe.visitors);
            Optional<CompiledInjectionVisitor.CompiledInjection> injection =
                    injectionCache.computeIfAbsent(method, (m) ->
                            visitor.visitMethod(siteCache.computeIfAbsent(method, MethodSite::fromMethod)));
            if (injection.isPresent()) {
                CompiledInjectionVisitor.CompiledInjection i = injection.get();
                if (i.getDelta() != InjectionDelta.NONE) {
                    return i.interceptCallback(obj, method, args, proxy, siteCache.get(method));
                }
            }
        }
        return proxy.invokeSuper(obj, args);
    }

    @Nullable
    private Supplier<?> getSyringeVisitedMethod(MethodProxy proxy) {
        if (proxy.getSignature().getName().contains("Syringe")) {
            Signature s = proxy.getSignature();
            if (s.getName().equals("getSyringe") && s.getReturnType() == Type.getType(String.class)
                    && s.getArgumentTypes().length == 0) {
                return visitedInstance::getSyringe;
            } else if (s.getName().equals("didSyringeModify") && s.getReturnType() == Type.BOOLEAN_TYPE
                    && s.getArgumentTypes().length == 0) {
                return visitedInstance::didSyringeModify;
            }
        }
        return null;
    }
}
