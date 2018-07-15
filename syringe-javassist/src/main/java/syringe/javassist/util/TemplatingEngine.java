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

package syringe.javassist.util;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.StringJoiner;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is a simplistic templating engine designed for templating java source files. As a result, it can recognize a
 * few different patterns in order to allow for templates to be java-compatible.
 *
 * Examples of supported templating patterns:
 * {@code
 *  // {{ template_key }}
 *  /* {{ template_key }} *\/ //(Must be single line!)
 *  {{ template_key }}
 *  Dummy.syringe_template("template_key")
 *  syringe_template("template_key");
 * }
 *
 * Template insertion is managed via the visitor pattern by implementing
 * {@link syringe.javassist.util.TemplatingEngine.TemplateVisitor} or by a statically defined lookup map.
 */
public final class TemplatingEngine {

    private TemplatingEngine() {}

    private static final Pattern[] patterns = new Pattern[] {
            Pattern.compile("(/{2}\\s*[{]{2}\\s*)([a-zA-Z0-9]+)(\\s*[}]{2})"), // // {{ something }}
            Pattern.compile("(/\\*\\s*[{]{2}\\s*)([a-zA-Z0-9]+)(\\s*[}]{2}\\s*\\*/)"), // /* {{ something }} */
            Pattern.compile("([{]{2}\\s*)([a-zA-Z0-9]+)(\\s*[}]{2})"), // {{ something }}
            Pattern.compile("((?:(?:syringe\\.javassist\\.)?Dummy\\.)?syringe_template\\(\")([a-zA-Z0-9]+)(\"\\);?)") // Dummy.syringe_template("something");
    };

    public static String template(String contents, TemplateVisitor visitor) {
        contents = visitor.visit(contents);
        StringJoiner sj = new StringJoiner("\n");
        StringTokenizer tokenizer = new StringTokenizer(contents, "\n");
        while (tokenizer.hasMoreTokens()) {
            String line = visitor.visitLine(tokenizer.nextToken());
            if (line != null && !line.isEmpty()) {
                // Heuristics to speed up matching
                if ((line.contains("{{") && line.contains("}}")) || line.contains("syringe_template")) {
                    for (Pattern pattern : patterns) {
                        Matcher matcher = pattern.matcher(line);
                        if (matcher.find()) {
                            line = matcher.replaceAll(r -> {
                                String key = r.group(2); //1-indexed
                                String replacement = visitor.visitSubstitution(key);
                                return replacement == null ? "" : replacement;
                            });
                        }
                    }
                }
                sj.add(line);
            }
        }
        return sj.toString();
    }

    public static String template(String contents, Map<String, String> mappedArgs) {
        return template(contents, new TemplateVisitor() {
            @Nullable
            @Override
            public String visitSubstitution(String key) {
                return mappedArgs.getOrDefault(key, null);
            }
        });
    }

    public static abstract class TemplateVisitor {

        public String visit(String template) {
            return template;
        }

        @Nullable
        public String visitLine(String line) {
            return line;
        }

        // /* {{ key }} */, // {{ key }}, {{ key }}, Dummy.syringe_template("key"), or syringe_template("key")
        @Nullable
        public String visitSubstitution(String key) {
            return null;
        }
    }
}
