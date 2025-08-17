package net.minecraft.world.item.component;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import io.netty.buffer.ByteBuf;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.apache.commons.lang3.function.TriConsumer;

public record ItemAttributeModifiers(List<ItemAttributeModifiers.Entry> modifiers) {
    public static final ItemAttributeModifiers EMPTY = new ItemAttributeModifiers(List.of());
    public static final Codec<ItemAttributeModifiers> CODEC = ItemAttributeModifiers.Entry.CODEC
        .listOf()
        .xmap(ItemAttributeModifiers::new, ItemAttributeModifiers::modifiers);
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemAttributeModifiers> STREAM_CODEC = StreamCodec.composite(
        ItemAttributeModifiers.Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), ItemAttributeModifiers::modifiers, ItemAttributeModifiers::new
    );
    public static final DecimalFormat ATTRIBUTE_MODIFIER_FORMAT = Util.make(
        new DecimalFormat("#.##"), p_334862_ -> p_334862_.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT))
    );

    public static ItemAttributeModifiers.Builder builder() {
        return new ItemAttributeModifiers.Builder();
    }

    public ItemAttributeModifiers withModifierAdded(Holder<Attribute> pAttribute, AttributeModifier pModifier, EquipmentSlotGroup pSlot) {
        ImmutableList.Builder<ItemAttributeModifiers.Entry> builder = ImmutableList.builderWithExpectedSize(this.modifiers.size() + 1);

        for (ItemAttributeModifiers.Entry itemattributemodifiers$entry : this.modifiers) {
            if (!itemattributemodifiers$entry.matches(pAttribute, pModifier.id())) {
                builder.add(itemattributemodifiers$entry);
            }
        }

        builder.add(new ItemAttributeModifiers.Entry(pAttribute, pModifier, pSlot));
        return new ItemAttributeModifiers(builder.build());
    }

    public void forEach(EquipmentSlotGroup pSlot, TriConsumer<Holder<Attribute>, AttributeModifier, ItemAttributeModifiers.Display> pAction) {
        for (ItemAttributeModifiers.Entry itemattributemodifiers$entry : this.modifiers) {
            if (itemattributemodifiers$entry.slot.equals(pSlot)) {
                pAction.accept(itemattributemodifiers$entry.attribute, itemattributemodifiers$entry.modifier, itemattributemodifiers$entry.display);
            }
        }
    }

    public void forEach(EquipmentSlotGroup pSlotGroup, BiConsumer<Holder<Attribute>, AttributeModifier> pAction) {
        for (ItemAttributeModifiers.Entry itemattributemodifiers$entry : this.modifiers) {
            if (itemattributemodifiers$entry.slot.equals(pSlotGroup)) {
                pAction.accept(itemattributemodifiers$entry.attribute, itemattributemodifiers$entry.modifier);
            }
        }
    }

    public void forEach(EquipmentSlot pEquipmentSlot, BiConsumer<Holder<Attribute>, AttributeModifier> pAction) {
        for (ItemAttributeModifiers.Entry itemattributemodifiers$entry : this.modifiers) {
            if (itemattributemodifiers$entry.slot.test(pEquipmentSlot)) {
                pAction.accept(itemattributemodifiers$entry.attribute, itemattributemodifiers$entry.modifier);
            }
        }
    }

    public double compute(double pBaseValue, EquipmentSlot pEquipmentSlot) {
        double d0 = pBaseValue;

        for (ItemAttributeModifiers.Entry itemattributemodifiers$entry : this.modifiers) {
            if (itemattributemodifiers$entry.slot.test(pEquipmentSlot)) {
                double d1 = itemattributemodifiers$entry.modifier.amount();

                d0 += switch (itemattributemodifiers$entry.modifier.operation()) {
                    case ADD_VALUE -> d1;
                    case ADD_MULTIPLIED_BASE -> d1 * pBaseValue;
                    case ADD_MULTIPLIED_TOTAL -> d1 * d0;
                };
            }
        }

        return d0;
    }

    public static class Builder {
        private final ImmutableList.Builder<ItemAttributeModifiers.Entry> entries = ImmutableList.builder();

        Builder() {
        }

        public ItemAttributeModifiers.Builder add(Holder<Attribute> pAttribute, AttributeModifier pModifier, EquipmentSlotGroup pSlot) {
            this.entries.add(new ItemAttributeModifiers.Entry(pAttribute, pModifier, pSlot));
            return this;
        }

        public ItemAttributeModifiers.Builder add(
            Holder<Attribute> pAttribute, AttributeModifier pModifier, EquipmentSlotGroup pSlot, ItemAttributeModifiers.Display pDisplay
        ) {
            this.entries.add(new ItemAttributeModifiers.Entry(pAttribute, pModifier, pSlot, pDisplay));
            return this;
        }

        public ItemAttributeModifiers build() {
            return new ItemAttributeModifiers(this.entries.build());
        }
    }

    public interface Display {
        Codec<ItemAttributeModifiers.Display> CODEC = ItemAttributeModifiers.Display.Type.CODEC
            .dispatch("type", ItemAttributeModifiers.Display::type, p_409403_ -> p_409403_.codec);
        StreamCodec<RegistryFriendlyByteBuf, ItemAttributeModifiers.Display> STREAM_CODEC = ItemAttributeModifiers.Display.Type.STREAM_CODEC
            .<RegistryFriendlyByteBuf>cast()
            .dispatch(ItemAttributeModifiers.Display::type, ItemAttributeModifiers.Display.Type::streamCodec);

        static ItemAttributeModifiers.Display attributeModifiers() {
            return ItemAttributeModifiers.Display.Default.INSTANCE;
        }

        static ItemAttributeModifiers.Display hidden() {
            return ItemAttributeModifiers.Display.Hidden.INSTANCE;
        }

        static ItemAttributeModifiers.Display override(Component pComponent) {
            return new ItemAttributeModifiers.Display.OverrideText(pComponent);
        }

        ItemAttributeModifiers.Display.Type type();

        void apply(Consumer<Component> pOutput, @Nullable Player pPlayer, Holder<Attribute> pAttribute, AttributeModifier pModifier);

        public record Default() implements ItemAttributeModifiers.Display {
            static final ItemAttributeModifiers.Display.Default INSTANCE = new ItemAttributeModifiers.Display.Default();
            static final MapCodec<ItemAttributeModifiers.Display.Default> CODEC = MapCodec.unit(INSTANCE);
            static final StreamCodec<RegistryFriendlyByteBuf, ItemAttributeModifiers.Display.Default> STREAM_CODEC = StreamCodec.unit(INSTANCE);

            @Override
            public ItemAttributeModifiers.Display.Type type() {
                return ItemAttributeModifiers.Display.Type.DEFAULT;
            }

            @Override
            public void apply(Consumer<Component> p_406087_, @Nullable Player p_409823_, Holder<Attribute> p_408648_, AttributeModifier p_406254_) {
                double d0 = p_406254_.amount();
                boolean flag = false;
                if (p_409823_ != null) {
                    if (p_406254_.is(Item.BASE_ATTACK_DAMAGE_ID)) {
                        d0 += p_409823_.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
                        flag = true;
                    } else if (p_406254_.is(Item.BASE_ATTACK_SPEED_ID)) {
                        d0 += p_409823_.getAttributeBaseValue(Attributes.ATTACK_SPEED);
                        flag = true;
                    }
                }

                double d1;
                if (p_406254_.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    || p_406254_.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                    d1 = d0 * 100.0;
                } else if (p_408648_.is(Attributes.KNOCKBACK_RESISTANCE)) {
                    d1 = d0 * 10.0;
                } else {
                    d1 = d0;
                }

                if (flag) {
                    p_406087_.accept(
                        CommonComponents.space()
                            .append(
                                Component.translatable(
                                    "attribute.modifier.equals." + p_406254_.operation().id(),
                                    ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(d1),
                                    Component.translatable(p_408648_.value().getDescriptionId())
                                )
                            )
                            .withStyle(ChatFormatting.DARK_GREEN)
                    );
                } else if (d0 > 0.0) {
                    p_406087_.accept(
                        Component.translatable(
                                "attribute.modifier.plus." + p_406254_.operation().id(),
                                ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(d1),
                                Component.translatable(p_408648_.value().getDescriptionId())
                            )
                            .withStyle(p_408648_.value().getStyle(true))
                    );
                } else if (d0 < 0.0) {
                    p_406087_.accept(
                        Component.translatable(
                                "attribute.modifier.take." + p_406254_.operation().id(),
                                ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(-d1),
                                Component.translatable(p_408648_.value().getDescriptionId())
                            )
                            .withStyle(p_408648_.value().getStyle(false))
                    );
                }
            }
        }

        public record Hidden() implements ItemAttributeModifiers.Display {
            static final ItemAttributeModifiers.Display.Hidden INSTANCE = new ItemAttributeModifiers.Display.Hidden();
            static final MapCodec<ItemAttributeModifiers.Display.Hidden> CODEC = MapCodec.unit(INSTANCE);
            static final StreamCodec<RegistryFriendlyByteBuf, ItemAttributeModifiers.Display.Hidden> STREAM_CODEC = StreamCodec.unit(INSTANCE);

            @Override
            public ItemAttributeModifiers.Display.Type type() {
                return ItemAttributeModifiers.Display.Type.HIDDEN;
            }

            @Override
            public void apply(Consumer<Component> p_407940_, @Nullable Player p_406085_, Holder<Attribute> p_408160_, AttributeModifier p_407914_) {
            }
        }

        public record OverrideText(Component component) implements ItemAttributeModifiers.Display {
            static final MapCodec<ItemAttributeModifiers.Display.OverrideText> CODEC = RecordCodecBuilder.mapCodec(
                p_408964_ -> p_408964_.group(
                        ComponentSerialization.CODEC.fieldOf("value").forGetter(ItemAttributeModifiers.Display.OverrideText::component)
                    )
                    .apply(p_408964_, ItemAttributeModifiers.Display.OverrideText::new)
            );
            static final StreamCodec<RegistryFriendlyByteBuf, ItemAttributeModifiers.Display.OverrideText> STREAM_CODEC = StreamCodec.composite(
                ComponentSerialization.STREAM_CODEC, ItemAttributeModifiers.Display.OverrideText::component, ItemAttributeModifiers.Display.OverrideText::new
            );

            @Override
            public ItemAttributeModifiers.Display.Type type() {
                return ItemAttributeModifiers.Display.Type.OVERRIDE;
            }

            @Override
            public void apply(Consumer<Component> p_408852_, @Nullable Player p_406427_, Holder<Attribute> p_407789_, AttributeModifier p_410336_) {
                p_408852_.accept(this.component);
            }
        }

        public static enum Type implements StringRepresentable {
            DEFAULT("default", 0, ItemAttributeModifiers.Display.Default.CODEC, ItemAttributeModifiers.Display.Default.STREAM_CODEC),
            HIDDEN("hidden", 1, ItemAttributeModifiers.Display.Hidden.CODEC, ItemAttributeModifiers.Display.Hidden.STREAM_CODEC),
            OVERRIDE("override", 2, ItemAttributeModifiers.Display.OverrideText.CODEC, ItemAttributeModifiers.Display.OverrideText.STREAM_CODEC);

            static final Codec<ItemAttributeModifiers.Display.Type> CODEC = StringRepresentable.fromEnum(ItemAttributeModifiers.Display.Type::values);
            private static final IntFunction<ItemAttributeModifiers.Display.Type> BY_ID = ByIdMap.continuous(
                ItemAttributeModifiers.Display.Type::id, values(), ByIdMap.OutOfBoundsStrategy.ZERO
            );
            static final StreamCodec<ByteBuf, ItemAttributeModifiers.Display.Type> STREAM_CODEC = ByteBufCodecs.idMapper(
                BY_ID, ItemAttributeModifiers.Display.Type::id
            );
            private final String name;
            private final int id;
            final MapCodec<? extends ItemAttributeModifiers.Display> codec;
            private final StreamCodec<RegistryFriendlyByteBuf, ? extends ItemAttributeModifiers.Display> streamCodec;

            private Type(
                final String pName,
                final int pId,
                final MapCodec<? extends ItemAttributeModifiers.Display> pCodec,
                final StreamCodec<RegistryFriendlyByteBuf, ? extends ItemAttributeModifiers.Display> pStreamCodec
            ) {
                this.name = pName;
                this.id = pId;
                this.codec = pCodec;
                this.streamCodec = pStreamCodec;
            }

            @Override
            public String getSerializedName() {
                return this.name;
            }

            private int id() {
                return this.id;
            }

            private StreamCodec<RegistryFriendlyByteBuf, ? extends ItemAttributeModifiers.Display> streamCodec() {
                return this.streamCodec;
            }
        }
    }

    public record Entry(Holder<Attribute> attribute, AttributeModifier modifier, EquipmentSlotGroup slot, ItemAttributeModifiers.Display display) {
        public static final Codec<ItemAttributeModifiers.Entry> CODEC = RecordCodecBuilder.create(
            p_405642_ -> p_405642_.group(
                    Attribute.CODEC.fieldOf("type").forGetter(ItemAttributeModifiers.Entry::attribute),
                    AttributeModifier.MAP_CODEC.forGetter(ItemAttributeModifiers.Entry::modifier),
                    EquipmentSlotGroup.CODEC.optionalFieldOf("slot", EquipmentSlotGroup.ANY).forGetter(ItemAttributeModifiers.Entry::slot),
                    ItemAttributeModifiers.Display.CODEC
                        .optionalFieldOf("display", ItemAttributeModifiers.Display.Default.INSTANCE)
                        .forGetter(ItemAttributeModifiers.Entry::display)
                )
                .apply(p_405642_, ItemAttributeModifiers.Entry::new)
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, ItemAttributeModifiers.Entry> STREAM_CODEC = StreamCodec.composite(
            Attribute.STREAM_CODEC,
            ItemAttributeModifiers.Entry::attribute,
            AttributeModifier.STREAM_CODEC,
            ItemAttributeModifiers.Entry::modifier,
            EquipmentSlotGroup.STREAM_CODEC,
            ItemAttributeModifiers.Entry::slot,
            ItemAttributeModifiers.Display.STREAM_CODEC,
            ItemAttributeModifiers.Entry::display,
            ItemAttributeModifiers.Entry::new
        );

        public Entry(Holder<Attribute> pAttribute, AttributeModifier pModifier, EquipmentSlotGroup pSlot) {
            this(pAttribute, pModifier, pSlot, ItemAttributeModifiers.Display.attributeModifiers());
        }

        public boolean matches(Holder<Attribute> pAttribute, ResourceLocation pId) {
            return pAttribute.equals(this.attribute) && this.modifier.is(pId);
        }
    }
}