package net.minecraft.world;

import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

public enum Difficulty implements StringRepresentable {
    PEACEFUL(0, "peaceful"),
    EASY(1, "easy"),
    NORMAL(2, "normal"),
    HARD(3, "hard");

    public static final StringRepresentable.EnumCodec<Difficulty> CODEC = StringRepresentable.fromEnum(Difficulty::values);
    private static final IntFunction<Difficulty> BY_ID = ByIdMap.continuous(Difficulty::getId, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
    public static final StreamCodec<ByteBuf, Difficulty> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Difficulty::getId);
    private final int id;
    private final String key;

    private Difficulty(final int pId, final String pKey) {
        this.id = pId;
        this.key = pKey;
    }

    public int getId() {
        return this.id;
    }

    public Component getDisplayName() {
        return Component.translatable("options.difficulty." + this.key);
    }

    public Component getInfo() {
        return Component.translatable("options.difficulty." + this.key + ".info");
    }

    @Deprecated
    public static Difficulty byId(int pId) {
        return BY_ID.apply(pId);
    }

    @Nullable
    public static Difficulty byName(String pName) {
        return CODEC.byName(pName);
    }

    public String getKey() {
        return this.key;
    }

    @Override
    public String getSerializedName() {
        return this.key;
    }
}