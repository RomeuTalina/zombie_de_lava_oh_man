package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.List.ListType;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import java.util.Set;
import net.minecraft.Util;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class RemoveBlockEntityTagFix extends DataFix {
    private final Set<String> blockEntityIdsToDrop;

    public RemoveBlockEntityTagFix(Schema pOutputSchema, Set<String> pBlockEntityIdsToDrop) {
        super(pOutputSchema, true);
        this.blockEntityIdsToDrop = pBlockEntityIdsToDrop;
    }

    @Override
    public TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.ITEM_STACK);
        OpticFinder<?> opticfinder = type.findField("tag");
        OpticFinder<?> opticfinder1 = opticfinder.type().findField("BlockEntityTag");
        Type<?> type1 = this.getInputSchema().getType(References.ENTITY);
        OpticFinder<?> opticfinder2 = DSL.namedChoice(
            "minecraft:falling_block", this.getInputSchema().getChoiceType(References.ENTITY, "minecraft:falling_block")
        );
        OpticFinder<?> opticfinder3 = opticfinder2.type().findField("TileEntityData");
        Type<?> type2 = this.getInputSchema().getType(References.STRUCTURE);
        OpticFinder<?> opticfinder4 = type2.findField("blocks");
        OpticFinder<?> opticfinder5 = DSL.typeFinder(((ListType)opticfinder4.type()).getElement());
        OpticFinder<?> opticfinder6 = opticfinder5.type().findField("nbt");
        OpticFinder<String> opticfinder7 = DSL.fieldFinder("id", NamespacedSchema.namespacedString());
        return TypeRewriteRule.seq(
            this.fixTypeEverywhereTyped(
                "ItemRemoveBlockEntityTagFix",
                type,
                p_409312_ -> p_409312_.updateTyped(opticfinder, p_406576_ -> this.removeBlockEntity(p_406576_, opticfinder1, opticfinder7, "BlockEntityTag"))
            ),
            this.fixTypeEverywhereTyped(
                "FallingBlockEntityRemoveBlockEntityTagFix",
                type1,
                p_410730_ -> p_410730_.updateTyped(opticfinder2, p_408853_ -> this.removeBlockEntity(p_408853_, opticfinder3, opticfinder7, "TileEntityData"))
            ),
            this.fixTypeEverywhereTyped(
                "StructureRemoveBlockEntityTagFix",
                type2,
                p_409735_ -> p_409735_.updateTyped(
                    opticfinder4, p_406486_ -> p_406486_.updateTyped(opticfinder5, p_407236_ -> this.removeBlockEntity(p_407236_, opticfinder6, opticfinder7, "nbt"))
                )
            ),
            this.convertUnchecked(
                "ItemRemoveBlockEntityTagFix - update block entity type",
                this.getInputSchema().getType(References.BLOCK_ENTITY),
                this.getOutputSchema().getType(References.BLOCK_ENTITY)
            )
        );
    }

    private Typed<?> removeBlockEntity(Typed<?> pData, OpticFinder<?> pTagFinder, OpticFinder<String> pIdFinder, String pKey) {
        Optional<? extends Typed<?>> optional = pData.getOptionalTyped(pTagFinder);
        if (optional.isEmpty()) {
            return pData;
        } else {
            String s = optional.get().getOptional(pIdFinder).orElse("");
            return !this.blockEntityIdsToDrop.contains(s) ? pData : Util.writeAndReadTypedOrThrow(pData, pData.getType(), p_407128_ -> p_407128_.remove(pKey));
        }
    }
}