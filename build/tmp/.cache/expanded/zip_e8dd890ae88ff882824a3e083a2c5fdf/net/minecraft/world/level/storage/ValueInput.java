package net.minecraft.world.level.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;

public interface ValueInput {
    <T> Optional<T> read(String pKey, Codec<T> pCodec);

    @Deprecated
    <T> Optional<T> read(MapCodec<T> pCodec);

    Optional<ValueInput> child(String pKey);

    ValueInput childOrEmpty(String pKey);

    Optional<ValueInput.ValueInputList> childrenList(String pKey);

    ValueInput.ValueInputList childrenListOrEmpty(String pKey);

    <T> Optional<ValueInput.TypedInputList<T>> list(String pKey, Codec<T> pElementCodec);

    <T> ValueInput.TypedInputList<T> listOrEmpty(String pKey, Codec<T> pElementCodec);

    boolean getBooleanOr(String pKey, boolean pDefaultValue);

    byte getByteOr(String pKey, byte pDefaultValue);

    int getShortOr(String pKey, short pDefaultValue);

    Optional<Integer> getInt(String pKey);

    int getIntOr(String pKey, int pDefaultValue);

    long getLongOr(String pKey, long pDefaultValue);

    Optional<Long> getLong(String pKey);

    float getFloatOr(String pKey, float pDefaultValue);

    double getDoubleOr(String pKey, double pDefaultValue);

    Optional<String> getString(String pKey);

    String getStringOr(String pKey, String pDefaultValue);

    Optional<int[]> getIntArray(String pKey);

    @Deprecated
    HolderLookup.Provider lookup();

    public interface TypedInputList<T> extends Iterable<T> {
        boolean isEmpty();

        Stream<T> stream();
    }

    public interface ValueInputList extends Iterable<ValueInput> {
        boolean isEmpty();

        Stream<ValueInput> stream();
    }
}