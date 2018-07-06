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

/**
 * This contains light-weight containers for java element-related information. This may be able to be converted into
 * traditional {@link java.lang.reflect} objects, however such conversion may be expensive! For transparent invocations
 * that do not incur the same overhead as reflection, take a look at the {@link syringe.access} package
 * contents.
 */
@NonNullPackage
package syringe.info;

import syringe.util.NonNullPackage;