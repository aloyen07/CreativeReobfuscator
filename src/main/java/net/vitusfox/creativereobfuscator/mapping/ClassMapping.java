package net.vitusfox.creativereobfuscator.mapping;

import net.vitusfox.creativereobfuscator.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClassMapping {

    public static final Pattern SIGNATURE_CLASS_PATTERN = Pattern.compile("(?<=L)[^;]+(?=;)");

    /**
     * Uses for remap class (Minecraft deobfuscated class - Minecraft obfuscated maps, for example: setBlock() -
     */

    public static Builder builder(int flags, String obfuscatedClass, String deobfuscatedClass) {
        return new Builder(flags, obfuscatedClass, deobfuscatedClass);
    }

    public static Builder builder(String obfuscatedClass, String deobfuscatedClass) {
        return new Builder(0, obfuscatedClass, deobfuscatedClass);
    }

    private final HashMap<String, String> fields;
    private final HashMap<Pair<String, String>, String> methods;
    private final String obfuscatedClass;
    private final String deobfuscatedClass;

    private ClassMapping(HashMap<String, String> fieldMappings, HashMap<Pair<String, String>, String> methodMappings,
                         String obfuscatedClass, String deobfuscatedClass) {
        this.fields = fieldMappings;
        this.methods = methodMappings;
        this.obfuscatedClass = obfuscatedClass;
        this.deobfuscatedClass = deobfuscatedClass;
    }

    public ClassMapping withNewCreditnails(String obfuscatedClass, String deobfuscatedClass) {
        return new ClassMapping(new HashMap<>(fields), new HashMap<>(methods), obfuscatedClass, deobfuscatedClass);
    }

    public ClassMapping withNewMappings(HashMap<String, String> fields, HashMap<Pair<String, String>, String> methods) {
        return new ClassMapping(new HashMap<>(fields), new HashMap<>(methods), obfuscatedClass, deobfuscatedClass);
    }

    /**
     * Returns deobfuscated field name by obfuscated name
     * @param obfuscated Obfuscated field name
     * @return Deobfuscated field name. Nullable. If null - mapping not found.
     */
    @Nullable
    public String getFieldByName(String obfuscated) {
        return fields.get(obfuscated);
    }

    /**
     * Returns deobfuscated method name by obfuscated name and signature
     * @param obfuscated Obfuscated method name
     * @param signature Signature of this method (e.g. (Lnet/minecraft/screens/Screen;III)V)
     * @return Deobfuscated method name
     */
    @Nullable
    public String getMethodByName(String obfuscated, String signature) {
        return methods.get(new Pair<>(obfuscated, signature));
    }

    public HashMap<Pair<String, String>, String> getMethods() {
        return methods;
    }

    public HashMap<String, String> getFields() {
        return fields;
    }

    /**
     * @return Deobfuscated class name
     */
    @NotNull
    public String getDeobfuscatedClass() {
        return deobfuscatedClass;
    }

    /**
     * @return Obfuscated class name
     */
    @NotNull
    public String getObfuscatedClass() {
        return obfuscatedClass;
    }

    public ClassMapping copy() {
        return new ClassMapping(new HashMap<>(fields), new HashMap<>(methods), obfuscatedClass, deobfuscatedClass);
    }

    public ClassMapping deobfuscateMethodsSignature(HashMap<String, String> dict) {
        ClassMapping deobfuscated = copy();
        deobfuscated.methods.clear();

        for (Map.Entry<Pair<String, String>, String> entry : methods.entrySet()) {
            // Don't touch names!
            String signature = entry.getKey().getSecond();
            String out = signature;

            Matcher matcher = SIGNATURE_CLASS_PATTERN.matcher(signature);
            while (matcher.find()) {
                String obfuscatedClass = matcher.group();
                String deobfuscatedClass = dict.get(obfuscatedClass);

                if (deobfuscatedClass != null) {
                    out = out.replace("L" + obfuscatedClass + ";", "L" + deobfuscatedClass + ";");
                }
            }

            // Putting it to copy
            deobfuscated.methods.put(new Pair<>(entry.getKey().getFirst(), out), entry.getValue());
        }

        return deobfuscated;
    }

    public static class Builder {

        private final HashMap<String, String> fieldMappings = new HashMap<>();
        private final HashMap<Pair<String, String>, String> methodMappings = new HashMap<>();

        private final boolean reversed;
        private final boolean reverseClasses;
        private final String obfuscatedClass;
        private final String deobfuscatedClass;

        private Builder(int flags, String obfuscatedClass, String deobfuscatedClass) {
            reversed = (flags & Mapping.REVERSE_MAPPINGS) == Mapping.REVERSE_MAPPINGS;
            reverseClasses = (flags & Mapping.REVERSED_CLASSES) == Mapping.REVERSED_CLASSES;
            this.obfuscatedClass = obfuscatedClass;
            this.deobfuscatedClass = deobfuscatedClass;
        }

        public void addField(FieldMapping mapping) {
            if (xor(reverseClasses, reversed)) {
                fieldMappings.put(mapping.getDeobfuscated(), mapping.getObfuscated());
            } else {
                fieldMappings.put(mapping.getObfuscated(), mapping.getDeobfuscated());
            }
        }

        public void addMethod(MethodMapping mapping) {
            if (xor(reverseClasses, reversed)) {
                methodMappings.put(new Pair<>(mapping.getDeobfuscatedName(), mapping.getSignature()), mapping.getObfuscatedName());
            } else {
                methodMappings.put(new Pair<>(mapping.getObfuscatedName(), mapping.getSignature()), mapping.getDeobfuscatedName());
            }
        }

        private boolean xor(boolean first, boolean second) {
            return first != second;
        }

        public ClassMapping build() {
            return new ClassMapping(fieldMappings, methodMappings, obfuscatedClass, deobfuscatedClass);
        }
    }
}
