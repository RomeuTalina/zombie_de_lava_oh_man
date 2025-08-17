package net.minecraft.world.entity.variant;

import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class VariantUtils {
    public static final String TAG_VARIANT = "variant";

    public static <T> Holder<T> getDefaultOrAny(RegistryAccess pRegistryAccess, ResourceKey<T> pKey) {
        Registry<T> registry = pRegistryAccess.lookupOrThrow(pKey.registryKey());
        return registry.get(pKey).or(registry::getAny).orElseThrow();
    }

    public static <T> Holder<T> getAny(RegistryAccess pRegistryAccess, ResourceKey<? extends Registry<T>> pRegistryKey) {
        return pRegistryAccess.lookupOrThrow(pRegistryKey).getAny().orElseThrow();
    }

    public static <T> void writeVariant(ValueOutput pOutput, Holder<T> pVariant) {
        pVariant.unwrapKey().ifPresent(p_405573_ -> pOutput.store("variant", ResourceLocation.CODEC, p_405573_.location()));
    }

    public static <T> Optional<Holder<T>> readVariant(ValueInput pInput, ResourceKey<? extends Registry<T>> pRegistryKey) {
        return pInput.read("variant", ResourceLocation.CODEC)
            .map(p_397274_ -> ResourceKey.create(pRegistryKey, p_397274_))
            .flatMap(pInput.lookup()::get);
    }

    public static <T extends PriorityProvider<SpawnContext, ?>> Optional<Holder.Reference<T>> selectVariantToSpawn(
        SpawnContext pContext, ResourceKey<Registry<T>> pRegistryKey
    ) {
        ServerLevelAccessor serverlevelaccessor = pContext.level();
        Stream<Holder.Reference<T>> stream = serverlevelaccessor.registryAccess().lookupOrThrow(pRegistryKey).listElements();
        return PriorityProvider.pick(stream, Holder::value, serverlevelaccessor.getRandom(), pContext);
    }
}