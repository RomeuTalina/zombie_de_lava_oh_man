package com.mojang.realmsclient.dto;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Set;
import net.minecraft.util.LenientJsonParser;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Ops extends ValueObject {
    public Set<String> ops = Sets.newHashSet();

    public static Ops parse(String pJson) {
        Ops ops = new Ops();

        try {
            JsonObject jsonobject = LenientJsonParser.parse(pJson).getAsJsonObject();
            JsonElement jsonelement = jsonobject.get("ops");
            if (jsonelement.isJsonArray()) {
                for (JsonElement jsonelement1 : jsonelement.getAsJsonArray()) {
                    ops.ops.add(jsonelement1.getAsString());
                }
            }
        } catch (Exception exception) {
        }

        return ops;
    }
}