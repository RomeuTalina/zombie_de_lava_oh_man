package net.minecraft.data.tags;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagKey;

public interface TagAppender<E, T> extends net.minecraftforge.common.extensions.IForgeTagAppender<E, T> {
    TagAppender<E, T> add(E pBlock);

    default TagAppender<E, T> add(E... pElements) {
        return this.addAll(Arrays.stream(pElements));
    }

    default TagAppender<E, T> addAll(Collection<E> pElements) {
        pElements.forEach(this::add);
        return this;
    }

    default TagAppender<E, T> addAll(Stream<E> pStream) {
        pStream.forEach(this::add);
        return this;
    }

    TagAppender<E, T> addOptional(E pBlock);

    TagAppender<E, T> addTag(TagKey<T> pTag);

    TagAppender<E, T> addOptionalTag(TagKey<T> pTag);

    static <T> TagAppender<ResourceKey<T>, T> forBuilder(final TagBuilder pBuilder) {
        return forBuilder(pBuilder, "unknown");
    }

    static <T> TagAppender<ResourceKey<T>, T> forBuilder(final TagBuilder pBuilder, String source) {
        return new TagAppender<ResourceKey<T>, T>() {
            public TagAppender<ResourceKey<T>, T> add(ResourceKey<T> p_406023_) {
                pBuilder.addElement(p_406023_.location());
                return this;
            }

            public TagAppender<ResourceKey<T>, T> addOptional(ResourceKey<T> p_409233_) {
                pBuilder.addOptionalElement(p_409233_.location());
                return this;
            }

            @Override
            public TagAppender<ResourceKey<T>, T> addTag(TagKey<T> p_410264_) {
                pBuilder.addTag(p_410264_.location());
                return this;
            }

            @Override
            public TagAppender<ResourceKey<T>, T> addOptionalTag(TagKey<T> p_407223_) {
                pBuilder.addOptionalTag(p_407223_.location());
                return this;
            }

            @Override
            public TagBuilder getInternalBuilder() {
                return pBuilder;
            }

            @Override
            public String getSourceName() {
                return source;
            }

            @Override
            public TagAppender<ResourceKey<T>, T> remove(ResourceKey<T> value) {
                return this.remove(value.location());
            }
        };
    }

    default <U> TagAppender<U, T> map(final Function<U, E> pMapper) {
        final TagAppender<E, T> tagappender = this;
        return new TagAppender<U, T>() {
            @Override
            public TagAppender<U, T> add(U p_409420_) {
                tagappender.add(pMapper.apply(p_409420_));
                return this;
            }

            @Override
            public TagAppender<U, T> addOptional(U p_410494_) {
                tagappender.add(pMapper.apply(p_410494_));
                return this;
            }

            @Override
            public TagAppender<U, T> addTag(TagKey<T> p_409457_) {
                tagappender.addTag(p_409457_);
                return this;
            }

            @Override
            public TagAppender<U, T> addOptionalTag(TagKey<T> p_409695_) {
                tagappender.addOptionalTag(p_409695_);
                return this;
            }

            @Override
            public TagBuilder getInternalBuilder() {
                return tagappender.getInternalBuilder();
            }

            @Override
            public String getSourceName() {
                return tagappender.getSourceName();
            }

            @Override
            public TagAppender<U, T> remove(U value) {
                tagappender.remove(pMapper.apply(value));
                return this;
            }
        };
    }
}
