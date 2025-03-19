package net.vitusfox.creativereobfuscator.mapping;

public class MethodMapping {

    private final String obfuscatedName;
    private final String deobfuscatedName;
    private final String signature;

    public MethodMapping(String obfuscationString) {
        MappingCheckers.isMethodOrThrow(obfuscationString);

        String[] split = obfuscationString.trim().split(" ");
        if (split.length != 3) {
            throw new IllegalStateException("Invalid split size: Needs 3, but got " + split.length + ". String: " + obfuscationString);
        }

        obfuscatedName = split[0];
        signature = split[1];
        deobfuscatedName = split[2];
    }

    public String getDeobfuscatedName() {
        return deobfuscatedName;
    }

    public String getObfuscatedName() {
        return obfuscatedName;
    }

    public String getSignature() {
        return signature;
    }
}
