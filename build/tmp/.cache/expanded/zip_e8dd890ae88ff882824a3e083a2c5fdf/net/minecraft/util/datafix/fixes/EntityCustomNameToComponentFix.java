package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class EntityCustomNameToComponentFix extends DataFix {
    public EntityCustomNameToComponentFix(Schema pOutputSchema) {
        super(pOutputSchema, true);
    }

    @Override
    public TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.ENTITY);
        Type<?> type1 = this.getOutputSchema().getType(References.ENTITY);
        OpticFinder<String> opticfinder = DSL.fieldFinder("id", NamespacedSchema.namespacedString());
        OpticFinder<String> opticfinder1 = (OpticFinder<String>)type.findField("CustomName");
        Type<?> type2 = type1.findFieldType("CustomName");
        return this.fixTypeEverywhereTyped(
            "EntityCustomNameToComponentFix", type, type1, p_405249_ -> fixEntity(p_405249_, type1, opticfinder, opticfinder1, type2)
        );
    }

    private static <T> Typed<?> fixEntity(
        Typed<?> pData, Type<?> pEntityType, OpticFinder<String> pCustomNameOptic, OpticFinder<String> pIdOptic, Type<T> pNewType
    ) {
        Optional<String> optional = pData.getOptional(pIdOptic);
        if (optional.isEmpty()) {
            return ExtraDataFixUtils.cast(pEntityType, pData);
        } else if (optional.get().isEmpty()) {
            return Util.writeAndReadTypedOrThrow(pData, pEntityType, p_405244_ -> p_405244_.remove("CustomName"));
        } else {
            String s = pData.getOptional(pCustomNameOptic).orElse("");
            Dynamic<?> dynamic = fixCustomName(pData.getOps(), optional.get(), s);
            return pData.set(pIdOptic, Util.readTypedOrThrow(pNewType, dynamic));
        }
    }

    private static <T> Dynamic<T> fixCustomName(DynamicOps<T> pOps, String pCustomName, String pId) {
        return "minecraft:commandblock_minecart".equals(pId)
            ? new Dynamic<>(pOps, pOps.createString(pCustomName))
            : LegacyComponentDataFixUtils.createPlainTextComponent(pOps, pCustomName);
    }
}