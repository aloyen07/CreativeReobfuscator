package net.vitusfox.creativereobfuscator.mapping;

import net.vitusfox.creativereobfuscator.Pair;
import net.vitusfox.creativereobfuscator.mapping.convert.LegacyMappingConverter;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Mapping {

    public static int REVERSE_MAPPINGS = 1;
    public static int NOT_REOBFUSCATE_CLASSES = 1 << 1;
    public static int REVERSED_CLASSES = 1 << 2;

    public static final String defaultTSRG;
    public static final String forgeMappingsTSRG;

    private static String getStringFromResource(String path) {
        InputStream is = Mapping.class.getResourceAsStream(path);

        StringBuilder builder = new StringBuilder();
        try {
            assert is != null;
            int c = is.read();
            while (c != -1) {
                builder.append((char) c);
                c = is.read();
            }

            is.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return builder.toString();
    }

    /**
     * Loading from file or creating new mapping for reobfuscation.
     * Creation may take some time
     * @param saveFile Nullable. If null, file not going to save and mapping will be created
     */
    public static Pair<LoadState, Mapping> getReobfuscationMapping(@Nullable File saveFile) throws IOException {
        boolean loading = false;
        if (saveFile != null) {
            if (saveFile.exists()) {
                loading = true;
            }
        }

        if (loading) {
            FileReader reader = new FileReader(saveFile);

            StringBuilder builder = new StringBuilder();

            int c = reader.read();
            while (c != -1) {
                builder.append((char) c);
                c = reader.read();
            }

            reader.close();

            return new Pair<>(LoadState.LOADED_FROM_FILE, new Mapping(builder.toString(), 0));
        } else {
            LegacyMappingConverter converter = new LegacyMappingConverter(getStringFromResource("/server_mappings.txt"),
                    getStringFromResource("/client_mappings.txt"));
            String ret = converter.convertAndMerge();

            Mapping legacy = new Mapping(ret, Mapping.REVERSED_CLASSES);
            Mapping mapping = new Mapping(getStringFromResource("/mcp_mappings.tsrg"), Mapping.REVERSED_CLASSES)
                    .deobfuscateSignatures();

            MappingMerger merger = new MappingMerger(legacy, mapping);
            Mapping joined = merger.merge(0).anonymizeClasses();

            if (saveFile != null) {
                if (!saveFile.exists()) {
                    if (!saveFile.createNewFile()) {
                        throw new IllegalStateException("Could not create new file " + saveFile);
                    }
                }

                joined.save(saveFile);
            }

            return new Pair<>(LoadState.CREATED_NEW, joined);
        }
    }

    /**
     * Loading from file or creating new mapping for reobfuscation.
     * Creation may take some time
     * @param saveFile Nullable. If null, file not going to save and mapping will be created
     */
    public static Pair<LoadState, Mapping> getReobfuscationMapping(@Nullable String saveFile) throws IOException {
        if (saveFile == null) {
            return getReobfuscationMapping((File) null);
        } else {
            return getReobfuscationMapping(new File(saveFile));
        }
    }

    static {
        defaultTSRG = getStringFromResource("/mcp_mappings.tsrg");
        forgeMappingsTSRG = new LegacyMappingConverter(getStringFromResource("/server_mappings.txt"),
                getStringFromResource("/client_mappings.txt")).convertAndMerge();
    }

    private final HashMap<String, ClassMapping> classes = new HashMap<>();

    private final boolean reversed;
    private final boolean reobfuscateClasses;
    private final boolean reversedClasses;
    private final int flags;

    public Mapping(String tsrg, int flags) {
        this.flags = flags;
        reobfuscateClasses = (flags & NOT_REOBFUSCATE_CLASSES) == NOT_REOBFUSCATE_CLASSES;
        reversed = (flags & Mapping.REVERSE_MAPPINGS) == Mapping.REVERSE_MAPPINGS;
        reversedClasses = (flags & REVERSED_CLASSES) == Mapping.REVERSED_CLASSES;

        ClassMapping.Builder builder = null;
        for (String s : tsrg.split("\n")) {
            if (s.trim().isEmpty()) {
                continue;
            }

            // Class definition
            if (!s.startsWith("\t")) {
                if (MappingCheckers.isClass(s)) {
                    String[] split = s.split(" ");

                    if (split.length != 2) {
                        throw new IllegalStateException("Invalid size of splitted class definer! Got: " + split.length + ", needs 2");
                    }

                    if (builder != null) {
                        add(builder.build());
                    }

                    builder = getBuilder(split[0], split[1]);

                    continue;
                }
            }

            if (builder == null) {
                throw new IllegalStateException("ClassBuilder is not defined on scope somewhere with line " + s);
            }

            // Method and fields definition
            MappingCheckers.Type type = MappingCheckers.getType(s);

            if (type == MappingCheckers.Type.NONE) {
                throw new IllegalStateException("Unknown type of string " + s);
            }

            if (type == MappingCheckers.Type.FIELD) {
                builder.addField(new FieldMapping(s));
            } else if (type == MappingCheckers.Type.METHOD) {
                builder.addMethod(new MethodMapping(s));
            }
        }
    }

    public void add(ClassMapping mapping) {

        if (reobfuscateClasses != reversed) {
            classes.put(mapping.getDeobfuscatedClass(), mapping);
        } else {
            classes.put(mapping.getObfuscatedClass(), mapping);
        }
    }

    public ClassMapping.Builder getBuilder(String obfuscated, String deobfuscated) {

        if (reobfuscateClasses) {
            if (reversedClasses) {
                return ClassMapping.builder(flags, obfuscated, obfuscated);
            } else {
                return ClassMapping.builder(flags, deobfuscated, deobfuscated);
            }
        } else {
            if (!reversed) {
                if (reversedClasses) {
                    return ClassMapping.builder(flags, obfuscated, deobfuscated);
                } else {
                    return ClassMapping.builder(flags, deobfuscated, obfuscated);
                }
            } else {
                if (reversedClasses) {
                    return ClassMapping.builder(flags, deobfuscated, obfuscated);
                } else {
                    return ClassMapping.builder(flags, obfuscated, deobfuscated);
                }
            }
        }
    }

    /// Sets deobfuscated name = obfuscated name
    public Mapping anonymizeClasses() {

        Mapping mapping = new Mapping("", flags);

        for (ClassMapping classMapping : classes.values()) {
            mapping.add(classMapping.withNewCreditnails(classMapping.getDeobfuscatedClass(), classMapping.getDeobfuscatedClass()));
        }

        return mapping;
    }

    public HashMap<String, ClassMapping> getClasses() {
        return classes;
    }

    /**
     * Returns ClassMapping by obfuscated name
     * @param obfuscated Obfuscated class name
     * @return Class mapping. Nullable. If null, this mapping not found
     */
    @Nullable
    public ClassMapping getByName(String obfuscated) {
        return classes.get(obfuscated);
    }

    public Mapping deobfuscateSignatures() {
        Mapping mapping = new Mapping("", flags);

        HashMap<String, String> obfuscatedSignatures = new HashMap<>(); // Obfuscated -> Deobfuscated
        for (Map.Entry<String, ClassMapping> mappingEntry : classes.entrySet()) {
            obfuscatedSignatures.put(mappingEntry.getKey(), mappingEntry.getValue().getDeobfuscatedClass());
        }

        for (Map.Entry<String, ClassMapping> mappingEntry : classes.entrySet()) {
            mapping.add(mappingEntry.getValue().deobfuscateMethodsSignature(obfuscatedSignatures));
        }

        return mapping;
    }

    public void save(File file) throws IOException {
        if (file.exists()) {
            if (!file.delete()) {
                throw new IllegalStateException("Could not delete old file with name " + file);
            }
        }

        if (!file.createNewFile()) {
            throw new IllegalStateException("Could not create new file with name " + file);
        }

        StringBuilder builder = new StringBuilder();

        for (ClassMapping classMapping : classes.values()) {
            builder.append(classMapping.getObfuscatedClass()).append(" ")
                    .append(classMapping.getDeobfuscatedClass()).append("\n");

            for (Map.Entry<String, String> field : classMapping.getFields().entrySet()) {
                builder.append("\t").append(field.getKey()).append(" ").append(field.getValue()).append("\n");
            }

            for (Map.Entry<Pair<String, String>, String> method : classMapping.getMethods().entrySet()) {
                builder.append("\t").append(method.getKey().getFirst()).append(" ")
                        .append(method.getKey().getSecond()).append(" ")
                        .append(method.getValue()).append("\n");
            }
        }

        FileOutputStream stream = new FileOutputStream(file);
        stream.write(builder.toString().getBytes(StandardCharsets.UTF_8));
        stream.flush();
        stream.close();
    }
}
