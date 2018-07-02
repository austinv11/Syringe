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

package com.austinv11.syringe.asm;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AsmConstantHolder {

    private static final Map<String, Object> holder = new ConcurrentHashMap<>();

    public static String add(Object o) {
        String uuid;
        do {
            uuid = UUID.randomUUID().toString().replaceAll("-", "_");
        } while (holder.containsKey(uuid));
        holder.put(uuid, o);
        return uuid;
    }

    public static Object get(String uuid) {
        if (!holder.containsKey(uuid))
            throw new RuntimeException("Report this to the dev!");
        return holder.get(uuid);
    }
}
