package net.minecraft.world.item.component;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.block.entity.SkullBlockEntity;

public record ResolvableProfile(Optional<String> name, Optional<UUID> id, PropertyMap properties, GameProfile gameProfile) {
    private static final Codec<ResolvableProfile> FULL_CODEC = RecordCodecBuilder.create(
        p_333384_ -> p_333384_.group(
                ExtraCodecs.PLAYER_NAME.optionalFieldOf("name").forGetter(ResolvableProfile::name),
                UUIDUtil.CODEC.optionalFieldOf("id").forGetter(ResolvableProfile::id),
                ExtraCodecs.PROPERTY_MAP.optionalFieldOf("properties", new PropertyMap()).forGetter(ResolvableProfile::properties)
            )
            .apply(p_333384_, ResolvableProfile::new)
    );
    public static final Codec<ResolvableProfile> CODEC = Codec.withAlternative(
        FULL_CODEC, ExtraCodecs.PLAYER_NAME, p_329676_ -> new ResolvableProfile(Optional.of(p_329676_), Optional.empty(), new PropertyMap())
    );
    public static final StreamCodec<ByteBuf, ResolvableProfile> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.stringUtf8(16).apply(ByteBufCodecs::optional),
        ResolvableProfile::name,
        UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs::optional),
        ResolvableProfile::id,
        ByteBufCodecs.GAME_PROFILE_PROPERTIES,
        ResolvableProfile::properties,
        ResolvableProfile::new
    );

    public ResolvableProfile(Optional<String> pName, Optional<UUID> pId, PropertyMap pProperties) {
        this(pName, pId, pProperties, createGameProfile(pId, pName, pProperties));
    }

    public ResolvableProfile(GameProfile pProfile) {
        this(Optional.of(pProfile.getName()), Optional.of(pProfile.getId()), pProfile.getProperties(), pProfile);
    }

    @Nullable
    public ResolvableProfile pollResolve() {
        if (this.isResolved()) {
            return this;
        } else {
            Optional<GameProfile> optional;
            if (this.id.isPresent()) {
                optional = SkullBlockEntity.fetchGameProfile(this.id.get()).getNow(null);
            } else {
                optional = SkullBlockEntity.fetchGameProfile(this.name.orElseThrow()).getNow(null);
            }

            return optional != null ? this.createProfile(optional) : null;
        }
    }

    public CompletableFuture<ResolvableProfile> resolve() {
        if (this.isResolved()) {
            return CompletableFuture.completedFuture(this);
        } else {
            return this.id.isPresent()
                ? SkullBlockEntity.fetchGameProfile(this.id.get()).thenApply(this::createProfile)
                : SkullBlockEntity.fetchGameProfile(this.name.orElseThrow()).thenApply(this::createProfile);
        }
    }

    private ResolvableProfile createProfile(Optional<GameProfile> pProfile) {
        return new ResolvableProfile(pProfile.orElseGet(() -> createGameProfile(this.id, this.name)));
    }

    private static GameProfile createGameProfile(Optional<UUID> pId, Optional<String> pName) {
        return new GameProfile(pId.orElse(Util.NIL_UUID), pName.orElse(""));
    }

    private static GameProfile createGameProfile(Optional<UUID> pId, Optional<String> pName, PropertyMap pProperties) {
        GameProfile gameprofile = createGameProfile(pId, pName);
        gameprofile.getProperties().putAll(pProperties);
        return gameprofile;
    }

    public boolean isResolved() {
        return !this.properties.isEmpty() ? true : this.id.isPresent() == this.name.isPresent();
    }
}