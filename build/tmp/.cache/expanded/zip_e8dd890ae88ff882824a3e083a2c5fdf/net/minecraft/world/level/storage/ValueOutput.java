package net.minecraft.world.level.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;

public interface ValueOutput {
    <T> void store(String pKey, Codec<T> pCodec, T pValue);

    <T> void storeNullable(String pKey, Codec<T> pCodec, @Nullable T pValue);

    @Deprecated
    <T> void store(MapCodec<T> pCodec, T pValue);

    void putBoolean(String pKey, boolean pValue);

    void putByte(String pKey, byte pValue);

    void putShort(String pKey, short pValue);

    void putInt(String pKey, int pValue);

    void putLong(String pKey, long pValue);

    void putFloat(String pKey, float pValue);

    void putDouble(String pKey, double pValue);

    void putString(String pKey, String pValue);

    void putIntArray(String pKey, int[] pValue);

    ValueOutput child(String pKey);

    ValueOutput.ValueOutputList childrenList(String pKey);

    <T> ValueOutput.TypedOutputList<T> list(String pKey, Codec<T> pElementCodec);

    void discard(String pKey);

    boolean isEmpty();

    public interface TypedOutputList<T> {
        void add(T pElement);

        boolean isEmpty();
    }

    public interface ValueOutputList {
        ValueOutput addChild();

        void discardLast();

        boolean isEmpty();
    }
}