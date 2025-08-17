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

    public static <T> Holder<T> getDefaultOrAny(RegistryAccess p_392079_, ResourceKey<T> p_391441_) {
        Registry<T> registry = p_392079_.lookupOrThrow(p_391441_.registryKey());
        return registry.get(p_391441_).or(registry::getAny).orElseThrow();
    }

    public static <T> Holder<T> getAny(RegistryAccess p_392091_, ResourceKey<? extends Registry<T>> p_396932_) {
        return p_392091_.lookupOrThrow(p_396932_).getAny().orElseThrow();
    }

    public static <T> void writeVariant(ValueOutput p_407774_, Holder<T> p_397545_) {
        p_397545_.unwrapKey().ifPresent(p_405573_ -> p_407774_.store("variant", ResourceLocation.CODEC, p_405573_.location()));
    }

    public static <T> Optional<Holder<T>> readVariant(ValueInput p_409121_, ResourceKey<? extends Registry<T>> p_394094_) {
        return p_409121_.read("variant", ResourceLocation.CODEC)
            .map(p_397274_ -> ResourceKey.create(p_394094_, p_397274_))
            .flatMap(p_409121_.lookup()::get);
    }

    public static <T extends PriorityProvider<SpawnContext, ?>> Optional<Holder.Reference<T>> selectVariantToSpawn(
        SpawnContext p_409151_, ResourceKey<Registry<T>> p_409896_
    ) {
        ServerLevelAccessor serverlevelaccessor = p_409151_.level();
        Stream<Holder.Reference<T>> stream = serverlevelaccessor.registryAccess().lookupOrThrow(p_409896_).listElements();
        return PriorityProvider.pick(stream, Holder::value, serverlevelaccessor.getRandom(), p_409151_);
    }
}