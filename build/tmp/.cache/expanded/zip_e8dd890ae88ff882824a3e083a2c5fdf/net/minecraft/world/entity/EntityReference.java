package net.minecraft.world.entity;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.players.OldUsersConverter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.UUIDLookup;
import net.minecraft.world.level.entity.UniquelyIdentifyable;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class EntityReference<StoredEntityType extends UniquelyIdentifyable> {
    private static final Codec<? extends EntityReference<?>> CODEC = UUIDUtil.CODEC.xmap(EntityReference::new, EntityReference::getUUID);
    private static final StreamCodec<ByteBuf, ? extends EntityReference<?>> STREAM_CODEC = UUIDUtil.STREAM_CODEC
        .map(EntityReference::new, EntityReference::getUUID);
    private Either<UUID, StoredEntityType> entity;

    public static <Type extends UniquelyIdentifyable> Codec<EntityReference<Type>> codec() {
        return (Codec<EntityReference<Type>>)CODEC;
    }

    public static <Type extends UniquelyIdentifyable> StreamCodec<ByteBuf, EntityReference<Type>> streamCodec() {
        return (StreamCodec<ByteBuf, EntityReference<Type>>)STREAM_CODEC;
    }

    public EntityReference(StoredEntityType pEntity) {
        this.entity = Either.right(pEntity);
    }

    public EntityReference(UUID pUuid) {
        this.entity = Either.left(pUuid);
    }

    public UUID getUUID() {
        return this.entity.map(p_392547_ -> (UUID)p_392547_, UniquelyIdentifyable::getUUID);
    }

    @Nullable
    public StoredEntityType getEntity(UUIDLookup<? super StoredEntityType> pUuidLookup, Class<StoredEntityType> pEntityClass) {
        Optional<StoredEntityType> optional = this.entity.right();
        if (optional.isPresent()) {
            StoredEntityType storedentitytype = optional.get();
            if (!storedentitytype.isRemoved()) {
                return storedentitytype;
            }

            this.entity = Either.left(storedentitytype.getUUID());
        }

        Optional<UUID> optional1 = this.entity.left();
        if (optional1.isPresent()) {
            StoredEntityType storedentitytype1 = this.resolve(pUuidLookup.getEntity(optional1.get()), pEntityClass);
            if (storedentitytype1 != null && !storedentitytype1.isRemoved()) {
                this.entity = Either.right(storedentitytype1);
                return storedentitytype1;
            }
        }

        return null;
    }

    @Nullable
    private StoredEntityType resolve(@Nullable UniquelyIdentifyable pEntity, Class<StoredEntityType> pEntityClass) {
        return pEntity != null && pEntityClass.isAssignableFrom(pEntity.getClass()) ? pEntityClass.cast(pEntity) : null;
    }

    public boolean matches(StoredEntityType pEntity) {
        return this.getUUID().equals(pEntity.getUUID());
    }

    public void store(ValueOutput pOutput, String pKey) {
        pOutput.store(pKey, UUIDUtil.CODEC, this.getUUID());
    }

    public static void store(@Nullable EntityReference<?> pKey, ValueOutput pOutput, String pUuid) {
        if (pKey != null) {
            pKey.store(pOutput, pUuid);
        }
    }

    @Nullable
    public static <StoredEntityType extends UniquelyIdentifyable> StoredEntityType get(
        @Nullable EntityReference<StoredEntityType> pReference, UUIDLookup<? super StoredEntityType> pUuidLookup, Class<StoredEntityType> pEntityClass
    ) {
        return pReference != null ? pReference.getEntity(pUuidLookup, pEntityClass) : null;
    }

    @Nullable
    public static <StoredEntityType extends UniquelyIdentifyable> EntityReference<StoredEntityType> read(ValueInput pInput, String pKey) {
        return pInput.read(pKey, EntityReference.<StoredEntityType>codec()).orElse(null);
    }

    @Nullable
    public static <StoredEntityType extends UniquelyIdentifyable> EntityReference<StoredEntityType> readWithOldOwnerConversion(
        ValueInput pInput, String pKey, Level pLevel
    ) {
        Optional<UUID> optional = pInput.read(pKey, UUIDUtil.CODEC);
        return optional.isPresent()
            ? new EntityReference<>(optional.get())
            : pInput.getString(pKey)
                .map(p_395444_ -> OldUsersConverter.convertMobOwnerIfNecessary(pLevel.getServer(), p_395444_))
                .map(EntityReference<StoredEntityType>::new)
                .orElse(null);
    }

    @Override
    public boolean equals(Object pOther) {
        return pOther == this ? true : pOther instanceof EntityReference<?> entityreference && this.getUUID().equals(entityreference.getUUID());
    }

    @Override
    public int hashCode() {
        return this.getUUID().hashCode();
    }
}