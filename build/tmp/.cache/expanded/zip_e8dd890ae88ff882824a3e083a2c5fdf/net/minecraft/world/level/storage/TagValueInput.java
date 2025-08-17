package net.minecraft.world.level.storage;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Streams;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.DataResult.Error;
import com.mojang.serialization.DataResult.Success;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagType;
import net.minecraft.util.ProblemReporter;

public class TagValueInput implements ValueInput {
    private final ProblemReporter problemReporter;
    private final ValueInputContextHelper context;
    private final CompoundTag input;

    private TagValueInput(ProblemReporter pProblemReporter, ValueInputContextHelper pContext, CompoundTag pInput) {
        this.problemReporter = pProblemReporter;
        this.context = pContext;
        this.input = pInput;
    }

    public static ValueInput create(ProblemReporter pProblemReporter, HolderLookup.Provider pLookup, CompoundTag pInput) {
        return new TagValueInput(pProblemReporter, new ValueInputContextHelper(pLookup, NbtOps.INSTANCE), pInput);
    }

    public static ValueInput.ValueInputList create(ProblemReporter pProblemReporter, HolderLookup.Provider pLookup, List<CompoundTag> pInput) {
        return new TagValueInput.CompoundListWrapper(pProblemReporter, new ValueInputContextHelper(pLookup, NbtOps.INSTANCE), pInput);
    }

    @Override
    public <T> Optional<T> read(String p_410337_, Codec<T> p_409146_) {
        Tag tag = this.input.get(p_410337_);
        if (tag == null) {
            return Optional.empty();
        } else {
            return switch (p_409146_.parse(this.context.ops(), tag)) {
                case Success<T> success -> Optional.of(success.value());
                case Error<T> error -> {
                    this.problemReporter.report(new TagValueInput.DecodeFromFieldFailedProblem(p_410337_, tag, error));
                    yield error.partialValue();
                }
                default -> throw new MatchException(null, null);
            };
        }
    }

    @Override
    public <T> Optional<T> read(MapCodec<T> p_409230_) {
        DynamicOps<Tag> dynamicops = this.context.ops();

        return switch (dynamicops.getMap(this.input).flatMap(p_407099_ -> p_409230_.decode(dynamicops, (MapLike<Tag>)p_407099_))) {
            case Success<T> success -> Optional.of(success.value());
            case Error<T> error -> {
                this.problemReporter.report(new TagValueInput.DecodeFromMapFailedProblem(error));
                yield error.partialValue();
            }
            default -> throw new MatchException(null, null);
        };
    }

    @Nullable
    private <T extends Tag> T getOptionalTypedTag(String pKey, TagType<T> pType) {
        Tag tag = this.input.get(pKey);
        if (tag == null) {
            return null;
        } else {
            TagType<?> tagtype = tag.getType();
            if (tagtype != pType) {
                this.problemReporter.report(new TagValueInput.UnexpectedTypeProblem(pKey, pType, tagtype));
                return null;
            } else {
                return (T)tag;
            }
        }
    }

    @Nullable
    private NumericTag getNumericTag(String pKey) {
        Tag tag = this.input.get(pKey);
        if (tag == null) {
            return null;
        } else if (tag instanceof NumericTag numerictag) {
            return numerictag;
        } else {
            this.problemReporter.report(new TagValueInput.UnexpectedNonNumberProblem(pKey, tag.getType()));
            return null;
        }
    }

    @Override
    public Optional<ValueInput> child(String p_407813_) {
        CompoundTag compoundtag = this.getOptionalTypedTag(p_407813_, CompoundTag.TYPE);
        return compoundtag != null ? Optional.of(this.wrapChild(p_407813_, compoundtag)) : Optional.empty();
    }

    @Override
    public ValueInput childOrEmpty(String p_409207_) {
        CompoundTag compoundtag = this.getOptionalTypedTag(p_409207_, CompoundTag.TYPE);
        return compoundtag != null ? this.wrapChild(p_409207_, compoundtag) : this.context.empty();
    }

    @Override
    public Optional<ValueInput.ValueInputList> childrenList(String p_408394_) {
        ListTag listtag = this.getOptionalTypedTag(p_408394_, ListTag.TYPE);
        return listtag != null ? Optional.of(this.wrapList(p_408394_, this.context, listtag)) : Optional.empty();
    }

    @Override
    public ValueInput.ValueInputList childrenListOrEmpty(String p_409200_) {
        ListTag listtag = this.getOptionalTypedTag(p_409200_, ListTag.TYPE);
        return listtag != null ? this.wrapList(p_409200_, this.context, listtag) : this.context.emptyList();
    }

    @Override
    public <T> Optional<ValueInput.TypedInputList<T>> list(String p_409428_, Codec<T> p_406552_) {
        ListTag listtag = this.getOptionalTypedTag(p_409428_, ListTag.TYPE);
        return listtag != null ? Optional.of(this.wrapTypedList(p_409428_, listtag, p_406552_)) : Optional.empty();
    }

    @Override
    public <T> ValueInput.TypedInputList<T> listOrEmpty(String p_408566_, Codec<T> p_408822_) {
        ListTag listtag = this.getOptionalTypedTag(p_408566_, ListTag.TYPE);
        return listtag != null ? this.wrapTypedList(p_408566_, listtag, p_408822_) : this.context.emptyTypedList();
    }

    @Override
    public boolean getBooleanOr(String p_409227_, boolean p_408143_) {
        NumericTag numerictag = this.getNumericTag(p_409227_);
        return numerictag != null ? numerictag.byteValue() != 0 : p_408143_;
    }

    @Override
    public byte getByteOr(String p_409575_, byte p_407762_) {
        NumericTag numerictag = this.getNumericTag(p_409575_);
        return numerictag != null ? numerictag.byteValue() : p_407762_;
    }

    @Override
    public int getShortOr(String p_407575_, short p_410301_) {
        NumericTag numerictag = this.getNumericTag(p_407575_);
        return numerictag != null ? numerictag.shortValue() : p_410301_;
    }

    @Override
    public Optional<Integer> getInt(String p_410418_) {
        NumericTag numerictag = this.getNumericTag(p_410418_);
        return numerictag != null ? Optional.of(numerictag.intValue()) : Optional.empty();
    }

    @Override
    public int getIntOr(String p_410149_, int p_406744_) {
        NumericTag numerictag = this.getNumericTag(p_410149_);
        return numerictag != null ? numerictag.intValue() : p_406744_;
    }

    @Override
    public long getLongOr(String p_409726_, long p_409661_) {
        NumericTag numerictag = this.getNumericTag(p_409726_);
        return numerictag != null ? numerictag.longValue() : p_409661_;
    }

    @Override
    public Optional<Long> getLong(String p_408151_) {
        NumericTag numerictag = this.getNumericTag(p_408151_);
        return numerictag != null ? Optional.of(numerictag.longValue()) : Optional.empty();
    }

    @Override
    public float getFloatOr(String p_407890_, float p_408107_) {
        NumericTag numerictag = this.getNumericTag(p_407890_);
        return numerictag != null ? numerictag.floatValue() : p_408107_;
    }

    @Override
    public double getDoubleOr(String p_405977_, double p_410529_) {
        NumericTag numerictag = this.getNumericTag(p_405977_);
        return numerictag != null ? numerictag.doubleValue() : p_410529_;
    }

    @Override
    public Optional<String> getString(String p_410016_) {
        StringTag stringtag = this.getOptionalTypedTag(p_410016_, StringTag.TYPE);
        return stringtag != null ? Optional.of(stringtag.value()) : Optional.empty();
    }

    @Override
    public String getStringOr(String p_407564_, String p_408786_) {
        StringTag stringtag = this.getOptionalTypedTag(p_407564_, StringTag.TYPE);
        return stringtag != null ? stringtag.value() : p_408786_;
    }

    @Override
    public Optional<int[]> getIntArray(String p_406433_) {
        IntArrayTag intarraytag = this.getOptionalTypedTag(p_406433_, IntArrayTag.TYPE);
        return intarraytag != null ? Optional.of(intarraytag.getAsIntArray()) : Optional.empty();
    }

    @Override
    public HolderLookup.Provider lookup() {
        return this.context.lookup();
    }

    private ValueInput wrapChild(String pKey, CompoundTag pTag) {
        return (ValueInput)(pTag.isEmpty()
            ? this.context.empty()
            : new TagValueInput(this.problemReporter.forChild(new ProblemReporter.FieldPathElement(pKey)), this.context, pTag));
    }

    static ValueInput wrapChild(ProblemReporter pProblemReporter, ValueInputContextHelper pContext, CompoundTag pTag) {
        return (ValueInput)(pTag.isEmpty() ? pContext.empty() : new TagValueInput(pProblemReporter, pContext, pTag));
    }

    private ValueInput.ValueInputList wrapList(String pKey, ValueInputContextHelper pContext, ListTag pTag) {
        return (ValueInput.ValueInputList)(pTag.isEmpty()
            ? pContext.emptyList()
            : new TagValueInput.ListWrapper(this.problemReporter, pKey, pContext, pTag));
    }

    private <T> ValueInput.TypedInputList<T> wrapTypedList(String pKey, ListTag pTag, Codec<T> pCodec) {
        return (ValueInput.TypedInputList<T>)(pTag.isEmpty()
            ? this.context.emptyTypedList()
            : new TagValueInput.TypedListWrapper<>(this.problemReporter, pKey, this.context, pCodec, pTag));
    }

    static class CompoundListWrapper implements ValueInput.ValueInputList {
        private final ProblemReporter problemReporter;
        private final ValueInputContextHelper context;
        private final List<CompoundTag> list;

        public CompoundListWrapper(ProblemReporter pProblemReporter, ValueInputContextHelper pContext, List<CompoundTag> pList) {
            this.problemReporter = pProblemReporter;
            this.context = pContext;
            this.list = pList;
        }

        ValueInput wrapChild(int pIndex, CompoundTag pTag) {
            return TagValueInput.wrapChild(this.problemReporter.forChild(new ProblemReporter.IndexedPathElement(pIndex)), this.context, pTag);
        }

        @Override
        public boolean isEmpty() {
            return this.list.isEmpty();
        }

        @Override
        public Stream<ValueInput> stream() {
            return Streams.mapWithIndex(this.list.stream(), (p_409052_, p_409963_) -> this.wrapChild((int)p_409963_, p_409052_));
        }

        @Override
        public Iterator<ValueInput> iterator() {
            final ListIterator<CompoundTag> listiterator = this.list.listIterator();
            return new AbstractIterator<ValueInput>() {
                @Nullable
                protected ValueInput computeNext() {
                    if (listiterator.hasNext()) {
                        int i = listiterator.nextIndex();
                        CompoundTag compoundtag = listiterator.next();
                        return CompoundListWrapper.this.wrapChild(i, compoundtag);
                    } else {
                        return this.endOfData();
                    }
                }
            };
        }
    }

    public record DecodeFromFieldFailedProblem(String name, Tag tag, Error<?> error) implements ProblemReporter.Problem {
        @Override
        public String description() {
            return "Failed to decode value '" + this.tag + "' from field '" + this.name + "': " + this.error.message();
        }
    }

    public record DecodeFromListFailedProblem(String name, int index, Tag tag, Error<?> error) implements ProblemReporter.Problem {
        @Override
        public String description() {
            return "Failed to decode value '"
                + this.tag
                + "' from field '"
                + this.name
                + "' at index "
                + this.index
                + "': "
                + this.error.message();
        }
    }

    public record DecodeFromMapFailedProblem(Error<?> error) implements ProblemReporter.Problem {
        @Override
        public String description() {
            return "Failed to decode from map: " + this.error.message();
        }
    }

    static class ListWrapper implements ValueInput.ValueInputList {
        private final ProblemReporter problemReporter;
        private final String name;
        final ValueInputContextHelper context;
        private final ListTag list;

        ListWrapper(ProblemReporter pProblemReporter, String pName, ValueInputContextHelper pContext, ListTag pList) {
            this.problemReporter = pProblemReporter;
            this.name = pName;
            this.context = pContext;
            this.list = pList;
        }

        @Override
        public boolean isEmpty() {
            return this.list.isEmpty();
        }

        ProblemReporter reporterForChild(int pIndex) {
            return this.problemReporter.forChild(new ProblemReporter.IndexedFieldPathElement(this.name, pIndex));
        }

        void reportIndexUnwrapProblem(int pIndex, Tag pTag) {
            this.problemReporter.report(new TagValueInput.UnexpectedListElementTypeProblem(this.name, pIndex, CompoundTag.TYPE, pTag.getType()));
        }

        @Override
        public Stream<ValueInput> stream() {
            return Streams.<Tag, ValueInput>mapWithIndex(this.list.stream(), (p_409123_, p_409836_) -> {
                if (p_409123_ instanceof CompoundTag compoundtag) {
                    return TagValueInput.wrapChild(this.reporterForChild((int)p_409836_), this.context, compoundtag);
                } else {
                    this.reportIndexUnwrapProblem((int)p_409836_, p_409123_);
                    return null;
                }
            }).filter(Objects::nonNull);
        }

        @Override
        public Iterator<ValueInput> iterator() {
            final Iterator<Tag> iterator = this.list.iterator();
            return new AbstractIterator<ValueInput>() {
                private int index;

                @Nullable
                protected ValueInput computeNext() {
                    while (iterator.hasNext()) {
                        Tag tag = iterator.next();
                        int i = this.index++;
                        if (tag instanceof CompoundTag compoundtag) {
                            return TagValueInput.wrapChild(ListWrapper.this.reporterForChild(i), ListWrapper.this.context, compoundtag);
                        }

                        ListWrapper.this.reportIndexUnwrapProblem(i, tag);
                    }

                    return this.endOfData();
                }
            };
        }
    }

    static class TypedListWrapper<T> implements ValueInput.TypedInputList<T> {
        private final ProblemReporter problemReporter;
        private final String name;
        final ValueInputContextHelper context;
        final Codec<T> codec;
        private final ListTag list;

        TypedListWrapper(ProblemReporter pProblemReporter, String pName, ValueInputContextHelper pContext, Codec<T> pCodec, ListTag pList) {
            this.problemReporter = pProblemReporter;
            this.name = pName;
            this.context = pContext;
            this.codec = pCodec;
            this.list = pList;
        }

        @Override
        public boolean isEmpty() {
            return this.list.isEmpty();
        }

        void reportIndexUnwrapProblem(int pIndex, Tag pTag, Error<?> pError) {
            this.problemReporter.report(new TagValueInput.DecodeFromListFailedProblem(this.name, pIndex, pTag, pError));
        }

        @Override
        public Stream<T> stream() {
            return Streams.<Tag, T>mapWithIndex(this.list.stream(), (p_408439_, p_410583_) -> {
                return (T)(switch (this.codec.parse(this.context.ops(), p_408439_)) {
                    case Success<T> success -> (Object)success.value();
                    case Error<T> error -> {
                        this.reportIndexUnwrapProblem((int)p_410583_, p_408439_, error);
                        yield error.partialValue().orElse(null);
                    }
                    default -> throw new MatchException(null, null);
                });
            }).filter(Objects::nonNull);
        }

        @Override
        public Iterator<T> iterator() {
            final ListIterator<Tag> listiterator = this.list.listIterator();
            return new AbstractIterator<T>() {
                @Nullable
                @Override
                protected T computeNext() {
                    while (listiterator.hasNext()) {
                        int i = listiterator.nextIndex();
                        Tag tag = listiterator.next();
                        switch (TypedListWrapper.this.codec.parse((DynamicOps<T>)TypedListWrapper.this.context.ops(), (T)tag)) {
                            case Success<T> success:
                                return success.value();
                            case Error<T> error:
                                TypedListWrapper.this.reportIndexUnwrapProblem(i, tag, error);
                                if (!error.partialValue().isPresent()) {
                                    break;
                                }

                                return error.partialValue().get();
                            default:
                                throw new MatchException(null, null);
                        }
                    }

                    return (T)this.endOfData();
                }
            };
        }
    }

    public record UnexpectedListElementTypeProblem(String name, int index, TagType<?> expected, TagType<?> actual)
        implements ProblemReporter.Problem {
        @Override
        public String description() {
            return "Expected list '"
                + this.name
                + "' to contain at index "
                + this.index
                + " value of type "
                + this.expected.getName()
                + ", but got "
                + this.actual.getName();
        }
    }

    public record UnexpectedNonNumberProblem(String name, TagType<?> actual) implements ProblemReporter.Problem {
        @Override
        public String description() {
            return "Expected field '" + this.name + "' to contain number, but got " + this.actual.getName();
        }
    }

    public record UnexpectedTypeProblem(String name, TagType<?> expected, TagType<?> actual) implements ProblemReporter.Problem {
        @Override
        public String description() {
            return "Expected field '" + this.name + "' to contain value of type " + this.expected.getName() + ", but got " + this.actual.getName();
        }
    }
}