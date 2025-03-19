package net.vitusfox.creativereobfuscator.mapping;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MappingCheckers {

    public static final String CLASS_REGEX = "^[^ \\t]+ [^ \\t]+$";
    public static final Pattern CLASS_PATTERN = Pattern.compile(CLASS_REGEX);

    public static final String FIELD_REGEX = "^\\t[^ \\t]+ [^ \\t]+$";
    public static final Pattern FIELD_PATTERN = Pattern.compile(FIELD_REGEX);

    public static final String METHOD_REGEX = "^\\t[^ \\t]+ \\(.*\\).+( |; )[^ \\t]+$";
    public static final Pattern METHOD_PATTERN = Pattern.compile(METHOD_REGEX);

    private static boolean genericCheck(Pattern pattern, String s) {
        Matcher m = pattern.matcher(s);
        return m.find();
    }

    public static boolean isClass(String s) {
        return genericCheck(CLASS_PATTERN, s);
    }

    public static boolean isField(String s) {
        return genericCheck(FIELD_PATTERN, s);
    }

    public static boolean isMethod(String s) {
        return genericCheck(METHOD_PATTERN, s);
    }

    private static void genericThrow(String s, String name) {
        throw new IllegalStateException("String " + s + " is not " + name + " obfuscation string!");
    }

    public static void isClassOrThrow(String s) {
        if (!isClass(s)) {
            genericThrow(s, "class");
        }
    }

    public static void isFieldOrThrow(String s) {
        if (!isField(s)) {
            genericThrow(s, "field");
        }
    }

    public static void isMethodOrThrow(String s) {
        if (!isMethod(s)) {
            genericThrow(s, "method");
        }
    }

    public static Type getType(String s) {
        if (isMethod(s)) {
            return Type.METHOD;
        } else if (isField(s)) {
            return Type.FIELD;
        } else if (isClass(s)) {
            return Type.CLASS;
        } else {
            return Type.NONE;
        }
    }

    public static enum Type {

        METHOD,
        FIELD,
        CLASS,
        NONE

    }
}
