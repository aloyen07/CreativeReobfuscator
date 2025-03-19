package net.vitusfox.creativereobfuscator.mapping.convert;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LegacyMappingConverter {

    public static final Pattern CLASS_PATTERN = Pattern.compile("^.+ -> .+:$");
    public static final Pattern METHOD_PATTERN = Pattern.compile("^ {4}.+ .+\\((.+|)\\) -> .+$");
    public static final Pattern FIELD_PATTERN = Pattern.compile("^ {4}.+ [^()]+ -> .+$");

    public static final String LINES_PATTERN = "\\d+:\\d+:";
    public static final Pattern ARGUMENTS_PATTERN = Pattern.compile("(?<=\\().+(?=\\))");
    public static final Pattern METHOD_NAME_PATTERN = Pattern.compile(".+(?=\\(.*\\))");
    public static final Pattern FIELD_REPLACER_PATTERN = Pattern.compile("(?<= ).+ -> .+");

    private final String serverMappings;
    private final String clientMappings;

    public LegacyMappingConverter(String serverMappings, String clientMappings) {
        this.serverMappings = serverMappings;
        this.clientMappings = clientMappings;
    }

    public String convertAndMerge() {
        return toTSRG(serverMappings) + "\n" + toTSRG(clientMappings);
    }

    private boolean isEquals(Pattern p, String s) {
        Matcher matcher = p.matcher(s);
        return matcher.find();
    }

    private String toTSRG(String mapping) {

        StringBuilder builder = new StringBuilder();

        for (String s : mapping.split("\n")) {
            if (s.isEmpty() || s.trim().isEmpty() || s.startsWith("#")) {
                continue;
            }

            if (isEquals(METHOD_PATTERN, s)) {
                // Method
                // e.g.     105:112:com.mojang.datafixers.util.Pair approxGivensQuat(float,float,float) -> a
                s = s.trim();
                // 105:112:com.mojang.datafixers.util.Pair approxGivensQuat(float,float,float) -> a
                String[] x = s.split(" -> ");
                if (x.length != 2) {
                    throw new IllegalStateException("Invalid string. Splitted size: " + x.length + ", but need 2. String: " + s);
                }

                // First - lines, returns, name, arguments. Second - obfuscated name
                String[] first = x[0].split(" ");

                if (first.length != 2) {
                    throw new IllegalStateException("Invalid string. Splitted size: " + first.length + ", but need 2. String: " + x[0]);
                }

                String returns = first[0].replaceAll(LINES_PATTERN, "");
                String arguments = "";
                Matcher m = ARGUMENTS_PATTERN.matcher(first[1]);
                if (m.find()) {
                    arguments = m.group();
                }

                m = METHOD_NAME_PATTERN.matcher(first[1].trim());
                String name = null;
                if (m.find()) {
                    name = m.group();
                }

                if (name == null) {
                    throw new IllegalStateException("Could not find method name from string " + first[1]);
                }

                if (name.equals("<init>") || name.equals("<clinit>")) {
                    continue;
                }

                builder.append("\t").append(x[1]).append(" ").append(generateSignature(arguments, returns)).append(" ")
                        .append(name);

            } else if (isEquals(FIELD_PATTERN, s)) {

                s = s.trim();
                Matcher m = FIELD_REPLACER_PATTERN.matcher(s);

                if (m.find()) {
                    s = m.group();
                } else {
                    throw new IllegalStateException("Could not get signature of field " + s);
                }

                String[] x = s.split(" -> ");
                if (x.length != 2) {
                    throw new IllegalStateException("Invalid string. Splitted size: " + x.length + ", but need 2. String: " + s);
                }

                builder.append("\t").append(x[1]).append(" ").append(x[0]);

            } else if (isEquals(CLASS_PATTERN, s)) {
                // Class
                // e.g. com.mojang.math.Matrix3f -> a:
                String[] x = s.split(" -> ");
                if (x.length != 2) {
                    throw new IllegalStateException("Invalid string. Splitted size: " + x.length + ", but need 2. String: " + s);
                }

                builder.append(x[1].replace(":", "").replace(".", "/"))
                        .append(" ").append(x[0].replace(".", "/"));
            }

            builder.append("\n");
        }

        return builder.toString();
    }

    private String generateSignature(String arguments, String returns) {
        StringBuilder builder = new StringBuilder();
        arguments = arguments.trim();
        returns = returns.trim();

        if (arguments.isEmpty()) {
            builder.append("()");
        } else {
            builder.append("(");
            for (String s : arguments.split(",")) {
                builder.append(getPrimitive(s));
            }

            builder.append(")");
        }

        builder.append(getPrimitive(returns));


        return builder.toString();
    }

    private String getPrimitive(String args) {
        StringBuilder array = new StringBuilder();
        String type;

        int counts = countEntries(args, "[]");

        for (int i = 0; i < counts; i++) {
            array.append("[");
        }

        args = args.replace("[]", "");

        type = args;

        if (type.contains("byte") || type.contains("char") || type.contains("double") || type.contains("float")
            || type.contains("int") || type.contains("long") || type.contains("short") || type.contains("boolean")
            || type.contains("void")) {
            type = type.replace("byte", "B").replace("char", "C")
                    .replace("double", "D").replace("float", "F")
                    .replace("int", "I").replace("long", "J")
                    .replace("short", "S").replace("boolean", "Z")
                    .replace("void", "V");
        } else {
            type = "L" + type.replace(".", "/") + ";";
        }

        return array + type;
    }

    private int countEntries(String str, String findStr) {
        int lastIndex = 0;
        int count = 0;

        while (lastIndex != -1) {

            lastIndex = str.indexOf(findStr, lastIndex);

            if (lastIndex != -1) {
                count++;
                lastIndex += findStr.length();
            }
        }

        return count;
    }
}
