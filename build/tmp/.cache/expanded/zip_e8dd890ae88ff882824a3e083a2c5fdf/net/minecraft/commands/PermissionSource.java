package net.minecraft.commands;

import net.minecraft.server.commands.PermissionCheck;

public interface PermissionSource {
    boolean hasPermission(int pLevel);

    default boolean allowsSelectors() {
        return this.hasPermission(2);
    }

    public record Check<T extends PermissionSource>(int requiredLevel) implements PermissionCheck<T> {
        public boolean test(T pSource) {
            return pSource.hasPermission(this.requiredLevel);
        }

        @Override
        public int requiredLevel() {
            return this.requiredLevel;
        }
    }
}