package net.minecraft.data.tags;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagKey;

public abstract class KeyTagProvider<T> extends TagsProvider<T> {
    protected KeyTagProvider(PackOutput p_407442_, ResourceKey<? extends Registry<T>> p_405854_, CompletableFuture<HolderLookup.Provider> p_409645_) {
        super(p_407442_, p_405854_, p_409645_);
    }

    protected KeyTagProvider(PackOutput p_407442_, ResourceKey<? extends Registry<T>> p_405854_, CompletableFuture<HolderLookup.Provider> p_409645_, String modId, @org.jetbrains.annotations.Nullable net.minecraftforge.common.data.ExistingFileHelper existingFileHelper) {
        super(p_407442_, p_405854_, p_409645_, modId, existingFileHelper);
    }

    protected TagAppender<ResourceKey<T>, T> tag(TagKey<T> pKey) {
        TagBuilder tagbuilder = this.getOrCreateRawBuilder(pKey);
        return TagAppender.forBuilder(tagbuilder, this.modId);
    }
}
