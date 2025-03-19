package net.vitusfox.creativereobfuscator.mapping;

public class FieldMapping {

    private final String obfuscated;
    private final String deobfuscated;

    public FieldMapping(String obfuscationString) {
        MappingCheckers.isMethod(obfuscationString);

        String[] split = obfuscationString.trim().split(" ");
        if (split.length != 2) {
            throw new IllegalStateException("Invalid split size: Needs 2, but got " + split.length + ". String: " + obfuscationString);
        }

        obfuscated = split[0];
        deobfuscated = split[1];
    }

    public String getObfuscated() {
        return obfuscated;
    }

    public String getDeobfuscated() {
        return deobfuscated;
    }
}
