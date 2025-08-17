package net.minecraft.data.tags;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagKey;

public interface TagAppender<E, T> extends net.minecraftforge.common.extensions.IForgeTagAppender<E, T> {
    TagAppender<E, T> add(E p_406816_);

    default TagAppender<E, T> add(E... p_406204_) {
        return this.addAll(Arrays.stream(p_406204_));
    }

    default TagAppender<E, T> addAll(Collection<E> p_405923_) {
        p_405923_.forEach(this::add);
        return this;
    }

    default TagAppender<E, T> addAll(Stream<E> p_406577_) {
        p_406577_.forEach(this::add);
        return this;
    }

    TagAppender<E, T> addOptional(E p_409929_);

    TagAppender<E, T> addTag(TagKey<T> p_407738_);

    TagAppender<E, T> addOptionalTag(TagKey<T> p_406169_);

    static <T> TagAppender<ResourceKey<T>, T> forBuilder(final TagBuilder p_407840_) {
        return forBuilder(p_407840_, "unknown");
    }

    static <T> TagAppender<ResourceKey<T>, T> forBuilder(final TagBuilder p_407840_, String source) {
        return new TagAppender<ResourceKey<T>, T>() {
            public TagAppender<ResourceKey<T>, T> add(ResourceKey<T> p_406023_) {
                p_407840_.addElement(p_406023_.location());
                return this;
            }

            public TagAppender<ResourceKey<T>, T> addOptional(ResourceKey<T> p_409233_) {
                p_407840_.addOptionalElement(p_409233_.location());
                return this;
            }

            @Override
            public TagAppender<ResourceKey<T>, T> addTag(TagKey<T> p_410264_) {
                p_407840_.addTag(p_410264_.location());
                return this;
            }

            @Override
            public TagAppender<ResourceKey<T>, T> addOptionalTag(TagKey<T> p_407223_) {
                p_407840_.addOptionalTag(p_407223_.location());
                return this;
            }

            @Override
            public TagBuilder getInternalBuilder() {
                return p_407840_;
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

    default <U> TagAppender<U, T> map(final Function<U, E> p_409081_) {
        final TagAppender<E, T> tagappender = this;
        return new TagAppender<U, T>() {
            @Override
            public TagAppender<U, T> add(U p_409420_) {
                tagappender.add(p_409081_.apply(p_409420_));
                return this;
            }

            @Override
            public TagAppender<U, T> addOptional(U p_410494_) {
                tagappender.add(p_409081_.apply(p_410494_));
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
                tagappender.remove(p_409081_.apply(value));
                return this;
            }
        };
    }
}
