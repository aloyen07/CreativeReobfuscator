package net.vitusfox.creativereobfuscator.mapping;

import net.vitusfox.creativereobfuscator.Pair;
import net.vitusfox.creativereobfuscator.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MappingMerger {

    public static final Pattern SIGNATURE_FINDER = Pattern.compile("L[^;]+;");

    // Original obfuscated -> Deobfuscated
    private final Mapping legacyMapping;
    // Original obfuscated -> Deobfuscated
    private final Mapping mcpMapping;

    public MappingMerger(Mapping legacyMapping, Mapping mcpMapping) {
        this.legacyMapping = legacyMapping;
        this.mcpMapping = mcpMapping;
    }

    public Mapping merge(int flags) {
        Mapping mapping = new Mapping("", flags);

        HashMap<String, String> dict = new HashMap<>(); // Deobfuscated legacy -> Deobfuscated MCP (classes)
        // Legacy mapping is bigger
        for (Map.Entry<String, ClassMapping> mappingEntry : legacyMapping.getClasses().entrySet()) {
            ClassMapping mcpClassMapping = mcpMapping.getByName(mappingEntry.getKey());

            if (mcpClassMapping == null) { // MCP Mapping not contains this entry. Don't merge it.
                mapping.add(mappingEntry.getValue());
                continue;
            }

            // Adding new class signature...
            dict.put(mappingEntry.getValue().getDeobfuscatedClass(), mcpClassMapping.getDeobfuscatedClass());
        }


        // Okay. We collected all non-merged classes. Let's go merge it!
        for (Map.Entry<String, ClassMapping> mappingEntry : legacyMapping.getClasses().entrySet()) {
            ClassMapping mcpClassMapping = mcpMapping.getByName(mappingEntry.getKey());

            if (mcpClassMapping == null) {
                continue; // Non-possible for merge
            }

            // Deobfuscated and merged classMapping with merged signatures
            ClassMapping mergedClass = mappingEntry.getValue().deobfuscateMethodsSignature(dict);


            // Added new naming for class mapping. Let's go add new naming for classes and fields
            // f_99999_1_ -> getBlock

            // By default, mappings looks like getBlock -> aaa
            // Deobfuscated -> obfuscated
            // We need to change obfuscated names to seared naming

            HashMap<String, String> fields = new HashMap<>();
            HashMap<Pair<String, String>, String> methods = new HashMap<>();

            // Merging fields...
            for (Map.Entry<String, String> entry : mappingEntry.getValue().getFields().entrySet()) {
                String seared = null;

                for (Map.Entry<String, String> mcpEntry : mcpClassMapping.getFields().entrySet()) {
                    if (mcpEntry.getValue().equals(entry.getValue())) {
                        seared = mcpEntry.getKey();
                    }
                }

                if (seared == null) {
                    fields.put(entry.getKey(), entry.getValue());
                    continue;
                }

                fields.put(entry.getKey(), seared);
            }

            // Merging classes...
            for (Map.Entry<Pair<String, String>, String> entry : mappingEntry.getValue().getMethods().entrySet()) {
                Pair<String, String> seared = null;

                for (Map.Entry<Pair<String, String>, String> mcpEntry : mcpClassMapping.getMethods().entrySet()) {

                    if (mcpEntry.getValue().equals(entry.getValue())) {
                        seared = mcpEntry.getKey();
                    }
                }

                if (seared == null) {
                    methods.put(entry.getKey(), entry.getValue());
                    continue;
                }

                methods.put(new Pair<>(entry.getKey().getFirst(), seared.getSecond()), seared.getFirst());
            }

            ClassMapping merged = mergedClass
                    .withNewCreditnails(mappingEntry.getValue().getObfuscatedClass(),
                            Util.nonNullOrDefault(
                                    dict.get(mappingEntry.getValue().getDeobfuscatedClass()),
                                    mappingEntry.getValue().getDeobfuscatedClass()))
                    .withNewMappings(fields, methods);

            mapping.add(merged);
        }


        return mapping;
    }

    private String reobfuscateSignature(String signature, HashMap<String, String> dict) {
        Matcher matcher = SIGNATURE_FINDER.matcher(signature);

        String ret = signature;
        List<String> finded = new ArrayList<>();
        while (matcher.find()) {
            String obf = matcher.group();

            obf = obf.substring(1, obf.length() - 1);
            if (!finded.contains(obf)) {
                String deobf = dict.get(obf);

                if (deobf != null) {
                    ret = ret.replace(obf, deobf);
                }

                finded.add(obf);
            }
        }

        return ret;
    }
}
