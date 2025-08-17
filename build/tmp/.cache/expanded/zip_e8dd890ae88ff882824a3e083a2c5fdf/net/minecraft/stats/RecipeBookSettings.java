package net.minecraft.stats;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import io.netty.buffer.ByteBuf;
import java.util.function.UnaryOperator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.inventory.RecipeBookType;

// TODO: [Forge][Custom Recipe Book Types - Add Optional<Map<RecipeBookType, TypeSettings>> to network codecs
public final class RecipeBookSettings {
    public static final StreamCodec<FriendlyByteBuf, RecipeBookSettings> STREAM_CODEC = StreamCodec.composite(
        RecipeBookSettings.TypeSettings.STREAM_CODEC,
        p_405231_ -> p_405231_.crafting,
        RecipeBookSettings.TypeSettings.STREAM_CODEC,
        p_405234_ -> p_405234_.furnace,
        RecipeBookSettings.TypeSettings.STREAM_CODEC,
        p_405229_ -> p_405229_.blastFurnace,
        RecipeBookSettings.TypeSettings.STREAM_CODEC,
        p_405228_ -> p_405228_.smoker,
        RecipeBookSettings::new
    );
    public static final MapCodec<RecipeBookSettings> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_405232_ -> p_405232_.group(
                RecipeBookSettings.TypeSettings.CRAFTING_MAP_CODEC.forGetter(p_405235_ -> p_405235_.crafting),
                RecipeBookSettings.TypeSettings.FURNACE_MAP_CODEC.forGetter(p_405230_ -> p_405230_.furnace),
                RecipeBookSettings.TypeSettings.BLAST_FURNACE_MAP_CODEC.forGetter(p_405233_ -> p_405233_.blastFurnace),
                RecipeBookSettings.TypeSettings.SMOKER_MAP_CODEC.forGetter(p_405236_ -> p_405236_.smoker)
            )
            .apply(p_405232_, RecipeBookSettings::new)
    );
    private RecipeBookSettings.TypeSettings crafting;
    private RecipeBookSettings.TypeSettings furnace;
    private RecipeBookSettings.TypeSettings blastFurnace;
    private RecipeBookSettings.TypeSettings smoker;

    public RecipeBookSettings() {
        this(
            RecipeBookSettings.TypeSettings.DEFAULT,
            RecipeBookSettings.TypeSettings.DEFAULT,
            RecipeBookSettings.TypeSettings.DEFAULT,
            RecipeBookSettings.TypeSettings.DEFAULT
        );
    }

    private RecipeBookSettings(
        RecipeBookSettings.TypeSettings pCrafting,
        RecipeBookSettings.TypeSettings pFurnace,
        RecipeBookSettings.TypeSettings pBlastFurnace,
        RecipeBookSettings.TypeSettings pSmoker
    ) {
        this.crafting = pCrafting;
        this.furnace = pFurnace;
        this.blastFurnace = pBlastFurnace;
        this.smoker = pSmoker;
    }

    @VisibleForTesting
    public RecipeBookSettings.TypeSettings getSettings(RecipeBookType pType) {
        return switch (pType) {
            case CRAFTING -> this.crafting;
            case FURNACE -> this.furnace;
            case BLAST_FURNACE -> this.blastFurnace;
            case SMOKER -> this.smoker;
        };
    }

    private void updateSettings(RecipeBookType pType, UnaryOperator<RecipeBookSettings.TypeSettings> pUpdater) {
        switch (pType) {
            case CRAFTING:
                this.crafting = pUpdater.apply(this.crafting);
                break;
            case FURNACE:
                this.furnace = pUpdater.apply(this.furnace);
                break;
            case BLAST_FURNACE:
                this.blastFurnace = pUpdater.apply(this.blastFurnace);
                break;
            case SMOKER:
                this.smoker = pUpdater.apply(this.smoker);
        }
    }

    public boolean isOpen(RecipeBookType pBookType) {
        return this.getSettings(pBookType).open;
    }

    public void setOpen(RecipeBookType pBookType, boolean pOpen) {
        this.updateSettings(pBookType, p_358758_ -> p_358758_.setOpen(pOpen));
    }

    public boolean isFiltering(RecipeBookType pBookType) {
        return this.getSettings(pBookType).filtering;
    }

    public void setFiltering(RecipeBookType pBookType, boolean pFiltering) {
        this.updateSettings(pBookType, p_358756_ -> p_358756_.setFiltering(pFiltering));
    }

    public RecipeBookSettings copy() {
        return new RecipeBookSettings(this.crafting, this.furnace, this.blastFurnace, this.smoker);
    }

    public void replaceFrom(RecipeBookSettings pOther) {
        this.crafting = pOther.crafting;
        this.furnace = pOther.furnace;
        this.blastFurnace = pOther.blastFurnace;
        this.smoker = pOther.smoker;
    }

    public record TypeSettings(boolean open, boolean filtering) {
        public static final RecipeBookSettings.TypeSettings DEFAULT = new RecipeBookSettings.TypeSettings(false, false);
        public static final MapCodec<RecipeBookSettings.TypeSettings> CRAFTING_MAP_CODEC = codec("isGuiOpen", "isFilteringCraftable");
        public static final MapCodec<RecipeBookSettings.TypeSettings> FURNACE_MAP_CODEC = codec("isFurnaceGuiOpen", "isFurnaceFilteringCraftable");
        public static final MapCodec<RecipeBookSettings.TypeSettings> BLAST_FURNACE_MAP_CODEC = codec("isBlastingFurnaceGuiOpen", "isBlastingFurnaceFilteringCraftable");
        public static final MapCodec<RecipeBookSettings.TypeSettings> SMOKER_MAP_CODEC = codec("isSmokerGuiOpen", "isSmokerFilteringCraftable");
        public static final StreamCodec<ByteBuf, RecipeBookSettings.TypeSettings> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            RecipeBookSettings.TypeSettings::open,
            ByteBufCodecs.BOOL,
            RecipeBookSettings.TypeSettings::filtering,
            RecipeBookSettings.TypeSettings::new
        );

        @Override
        public String toString() {
            return "[open=" + this.open + ", filtering=" + this.filtering + "]";
        }

        public RecipeBookSettings.TypeSettings setOpen(boolean pOpen) {
            return new RecipeBookSettings.TypeSettings(pOpen, this.filtering);
        }

        public RecipeBookSettings.TypeSettings setFiltering(boolean pFiltering) {
            return new RecipeBookSettings.TypeSettings(this.open, pFiltering);
        }

        private static MapCodec<RecipeBookSettings.TypeSettings> codec(String pOpenKey, String pFilteringKey) {
            return RecordCodecBuilder.mapCodec(
                p_407394_ -> p_407394_.group(
                        Codec.BOOL.optionalFieldOf(pOpenKey, false).forGetter(RecipeBookSettings.TypeSettings::open),
                        Codec.BOOL.optionalFieldOf(pFilteringKey, false).forGetter(RecipeBookSettings.TypeSettings::filtering)
                    )
                    .apply(p_407394_, RecipeBookSettings.TypeSettings::new)
            );
        }
    }

    // FORGE -- called automatically on Enum creation - used for serialization
    public static void register(RecipeBookType type) {
        String name = type.name().toLowerCase(java.util.Locale.ROOT).replace("_","");
        var codec = TypeSettings.codec("is" + name + "GuiOpen", "is" + name + "FilteringCraftable");
        //TAG_FIELDS.put(type, Pair.of(openTag, filteringTag));
        throw new IllegalStateException("This is not implemented yet, poke Forge if you actually use this");
    }
}
