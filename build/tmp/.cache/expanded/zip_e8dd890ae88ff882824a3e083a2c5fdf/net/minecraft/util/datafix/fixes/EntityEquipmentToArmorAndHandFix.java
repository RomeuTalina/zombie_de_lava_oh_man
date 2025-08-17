package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Lists;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class EntityEquipmentToArmorAndHandFix extends DataFix {
    public EntityEquipmentToArmorAndHandFix(Schema pOutputSchema) {
        super(pOutputSchema, true);
    }

    @Override
    public TypeRewriteRule makeRule() {
        return this.cap(this.getInputSchema().getTypeRaw(References.ITEM_STACK), this.getOutputSchema().getTypeRaw(References.ITEM_STACK));
    }

    private <ItemStackOld, ItemStackNew> TypeRewriteRule cap(Type<ItemStackOld> pOldItemStackType, Type<ItemStackNew> pNewItemStackType) {
        Type<Pair<String, Either<List<ItemStackOld>, Unit>>> type = DSL.named(
            References.ENTITY_EQUIPMENT.typeName(), DSL.optional(DSL.field("Equipment", DSL.list(pOldItemStackType)))
        );
        Type<Pair<String, Pair<Either<List<ItemStackNew>, Unit>, Pair<Either<List<ItemStackNew>, Unit>, Pair<Either<ItemStackNew, Unit>, Either<ItemStackNew, Unit>>>>>> type1 = DSL.named(
            References.ENTITY_EQUIPMENT.typeName(),
            DSL.and(
                DSL.optional(DSL.field("ArmorItems", DSL.list(pNewItemStackType))),
                DSL.optional(DSL.field("HandItems", DSL.list(pNewItemStackType))),
                DSL.optional(DSL.field("body_armor_item", pNewItemStackType)),
                DSL.optional(DSL.field("saddle", pNewItemStackType))
            )
        );
        if (!type.equals(this.getInputSchema().getType(References.ENTITY_EQUIPMENT))) {
            throw new IllegalStateException("Input entity_equipment type does not match expected");
        } else if (!type1.equals(this.getOutputSchema().getType(References.ENTITY_EQUIPMENT))) {
            throw new IllegalStateException("Output entity_equipment type does not match expected");
        } else {
            return TypeRewriteRule.seq(
                this.fixTypeEverywhereTyped(
                    "EntityEquipmentToArmorAndHandFix - drop chances",
                    this.getInputSchema().getType(References.ENTITY),
                    p_390245_ -> p_390245_.update(DSL.remainderFinder(), EntityEquipmentToArmorAndHandFix::fixDropChances)
                ),
                this.fixTypeEverywhere(
                    "EntityEquipmentToArmorAndHandFix - equipment",
                    type,
                    type1,
                    p_390243_ -> {
                        ItemStackNew itemstacknew = pNewItemStackType.read(new Dynamic<>(p_390243_).emptyMap())
                            .result()
                            .orElseThrow(() -> new IllegalStateException("Could not parse newly created empty itemstack."))
                            .getFirst();
                        Either<ItemStackNew, Unit> either = Either.right(DSL.unit());
                        return p_390252_ -> p_390252_.mapSecond(p_390248_ -> {
                            List<ItemStackOld> list = p_390248_.map(Function.identity(), p_390244_ -> List.of());
                            Either<List<ItemStackNew>, Unit> either1 = Either.right(DSL.unit());
                            Either<List<ItemStackNew>, Unit> either2 = Either.right(DSL.unit());
                            if (!list.isEmpty()) {
                                either1 = Either.left(Lists.newArrayList((ItemStackNew[])(new Object[]{list.getFirst(), itemstacknew})));
                            }

                            if (list.size() > 1) {
                                List<ItemStackNew> list1 = Lists.newArrayList(itemstacknew, itemstacknew, itemstacknew, itemstacknew);

                                for (int i = 1; i < Math.min(list.size(), 5); i++) {
                                    list1.set(i - 1, (ItemStackNew)list.get(i));
                                }

                                either2 = Either.left(list1);
                            }

                            return Pair.of(either2, Pair.of(either1, Pair.of(either, either)));
                        });
                    }
                )
            );
        }
    }

    private static Dynamic<?> fixDropChances(Dynamic<?> pData) {
        Optional<? extends Stream<? extends Dynamic<?>>> optional = pData.get("DropChances").asStreamOpt().result();
        pData = pData.remove("DropChances");
        if (optional.isPresent()) {
            Iterator<Float> iterator = Stream.concat(optional.get().map(p_390249_ -> p_390249_.asFloat(0.0F)), Stream.generate(() -> 0.0F)).iterator();
            float f = iterator.next();
            if (pData.get("HandDropChances").result().isEmpty()) {
                pData = pData.set("HandDropChances", pData.createList(Stream.of(f, 0.0F).map(pData::createFloat)));
            }

            if (pData.get("ArmorDropChances").result().isEmpty()) {
                pData = pData.set(
                    "ArmorDropChances",
                    pData.createList(Stream.of(iterator.next(), iterator.next(), iterator.next(), iterator.next()).map(pData::createFloat))
                );
            }
        }

        return pData;
    }
}