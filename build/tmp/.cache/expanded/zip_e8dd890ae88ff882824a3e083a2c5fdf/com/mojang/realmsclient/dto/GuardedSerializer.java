package com.mojang.realmsclient.dto;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuardedSerializer {
    ExclusionStrategy strategy = new ExclusionStrategy() {
        @Override
        public boolean shouldSkipClass(Class<?> p_410689_) {
            return false;
        }

        @Override
        public boolean shouldSkipField(FieldAttributes p_410708_) {
            return p_410708_.getAnnotation(Exclude.class) != null;
        }
    };
    private final Gson gson = new GsonBuilder()
        .addSerializationExclusionStrategy(this.strategy)
        .addDeserializationExclusionStrategy(this.strategy)
        .create();

    public String toJson(ReflectionBasedSerialization pReflectionBasedSerialization) {
        return this.gson.toJson(pReflectionBasedSerialization);
    }

    public String toJson(JsonElement pJson) {
        return this.gson.toJson(pJson);
    }

    @Nullable
    public <T extends ReflectionBasedSerialization> T fromJson(String pJson, Class<T> pClassOfT) {
        return this.gson.fromJson(pJson, pClassOfT);
    }
}