package net.vitusfox.creativereobfuscator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Util {

    public static <T> T nonNullOrDefault(@Nullable T nullable, @NotNull T defaultValue) {
        if (nullable == null) {
            return defaultValue;
        } else {
            return nullable;
        }
    }
}
