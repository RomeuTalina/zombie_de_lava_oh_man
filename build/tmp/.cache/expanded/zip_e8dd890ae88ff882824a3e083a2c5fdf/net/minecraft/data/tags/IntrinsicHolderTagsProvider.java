package net.minecraft.data.tags;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagKey;

public abstract class IntrinsicHolderTagsProvider<T> extends TagsProvider<T> {
    private final Function<T, ResourceKey<T>> keyExtractor;

    public IntrinsicHolderTagsProvider(PackOutput pOutput, ResourceKey<? extends Registry<T>> pRegistryKey, CompletableFuture<HolderLookup.Provider> pLookupProvider, Function<T, ResourceKey<T>> pKeyExtractor) {
        super(pOutput, pRegistryKey, pLookupProvider);
        this.keyExtractor = pKeyExtractor;
    }

    public IntrinsicHolderTagsProvider(
        PackOutput pOutput,
        ResourceKey<? extends Registry<T>> pRegistryKey,
        CompletableFuture<HolderLookup.Provider> pLookupProvider,
        Function<T, ResourceKey<T>> pKeyExtractor,
        String modId,
        @org.jetbrains.annotations.Nullable net.minecraftforge.common.data.ExistingFileHelper existingFileHelper
    ) {
        super(pOutput, pRegistryKey, pLookupProvider, modId, existingFileHelper);
        this.keyExtractor = pKeyExtractor;
    }

    public IntrinsicHolderTagsProvider(PackOutput pOutput, ResourceKey<? extends Registry<T>> pRegistryKey, CompletableFuture<HolderLookup.Provider> pLookupProvider, CompletableFuture<TagsProvider.TagLookup<T>> pParentProvider, Function<T, ResourceKey<T>> pKeyExtractor) {
        super(pOutput, pRegistryKey, pLookupProvider, pParentProvider);
        this.keyExtractor = pKeyExtractor;
    }

    public IntrinsicHolderTagsProvider(
        PackOutput pOutput,
        ResourceKey<? extends Registry<T>> pRegistryKey,
        CompletableFuture<HolderLookup.Provider> pLookupProvider,
        CompletableFuture<TagsProvider.TagLookup<T>> pParentProvider,
        Function<T, ResourceKey<T>> pKeyExtractor,
        String modId,
        @org.jetbrains.annotations.Nullable net.minecraftforge.common.data.ExistingFileHelper existingFileHelper
    ) {
        super(pOutput, pRegistryKey, pLookupProvider, pParentProvider, modId, existingFileHelper);
        this.keyExtractor = pKeyExtractor;
    }

    protected TagAppender<T, T> tag(TagKey<T> pKey) {
        TagBuilder tagbuilder = this.getOrCreateRawBuilder(pKey);
        return TagAppender.<T>forBuilder(tagbuilder, this.modId).map(this.keyExtractor);
    }
}
