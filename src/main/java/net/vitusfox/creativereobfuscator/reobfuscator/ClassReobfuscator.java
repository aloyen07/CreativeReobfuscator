package net.vitusfox.creativereobfuscator.reobfuscator;

import net.vitusfox.creativereobfuscator.Pair;
import net.vitusfox.creativereobfuscator.mapping.ClassMapping;
import net.vitusfox.creativereobfuscator.mapping.FieldMapping;
import net.vitusfox.creativereobfuscator.mapping.Mapping;
import net.vitusfox.creativereobfuscator.mapping.MethodMapping;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ClassReobfuscator {

    private final Mapping mappings;

    public ClassReobfuscator(Mapping mapping) {
        this.mappings = mapping;
    }


    public byte[] reobfuscate(byte[] bytecode) {
        ClassReader reader = new ClassReader(bytecode);
        ClassNode node = new ClassNode();
        reader.accept(node, 0);

        reobfuscateClass(node);
        reobfuscateFields(node);
        reobfuscateMethods(node);
        reobfuscateMethodInstructions(node);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private void reobfuscateClass(ClassNode node) {
        // Now mapping class
        ClassMapping classMapping = mappings.getByName(node.name);
        if (classMapping != null) {
            node.name = classMapping.getObfuscatedClass();
        }

        // Reobf superclass and interfaces
        if (node.superName != null) {
            ClassMapping superMapping = mappings.getByName(node.superName);
            if (superMapping != null) {
                node.superName = superMapping.getObfuscatedClass();
            }
        }

        for (int i = 0; i < node.interfaces.size(); i++) {
            String interfaceName = node.interfaces.get(i);
            ClassMapping interfaceMapping = mappings.getByName(interfaceName);
            if (interfaceMapping != null) {
                node.interfaces.set(i, interfaceMapping.getObfuscatedClass());
            }
        }
    }

    private void reobfuscateFields(ClassNode node) {
        for (FieldNode field : node.fields) {
            // Now class mapping
            ClassMapping classMapping = mappings.getByName(node.name);
            if (classMapping != null) {
                // Renaming field
                String deobfuscatedFieldName = classMapping.getFieldByName(field.name);
                if (deobfuscatedFieldName != null) {
                    field.name = deobfuscatedFieldName;
                }
            }
        }
    }

    private void reobfuscateMethods(ClassNode node) {
        for (MethodNode method : node.methods) {
            // Now mapping class
            ClassMapping classMapping = mappings.getByName(node.name);
            if (classMapping != null) {
                // Renaming method
                String deobfuscatedMethodName = classMapping.getMethodByName(method.name, method.desc);
                if (deobfuscatedMethodName != null) {
                    method.name = deobfuscatedMethodName;
                }
            }
        }
    }

    private void reobfuscateMethodInstructions(ClassNode node) {
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode) {
                    // Reobfuscating invokes
                    MethodInsnNode methodInsn = (MethodInsnNode) instruction;
                    ClassMapping ownerMapping = mappings.getByName(methodInsn.owner);
                    if (ownerMapping != null) {
                        methodInsn.owner = ownerMapping.getObfuscatedClass();
                        String deobfuscatedMethodName = ownerMapping.getMethodByName(methodInsn.name, methodInsn.desc);
                        if (deobfuscatedMethodName != null) {
                            methodInsn.name = deobfuscatedMethodName;
                        }
                    }
                } else if (instruction instanceof FieldInsnNode) {
                    // Reobfuscating fields
                    FieldInsnNode fieldInsn = (FieldInsnNode) instruction;
                    ClassMapping ownerMapping = mappings.getByName(fieldInsn.owner);
                    if (ownerMapping != null) {
                        fieldInsn.owner = ownerMapping.getObfuscatedClass();
                        String deobfuscatedFieldName = ownerMapping.getFieldByName(fieldInsn.name);
                        if (deobfuscatedFieldName != null) {
                            fieldInsn.name = deobfuscatedFieldName;
                        }
                    }
                }
            }
        }
    }

}
