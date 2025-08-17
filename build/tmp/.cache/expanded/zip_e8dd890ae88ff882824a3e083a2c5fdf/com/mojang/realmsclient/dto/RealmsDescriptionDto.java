package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsDescriptionDto extends ValueObject implements ReflectionBasedSerialization {
    @SerializedName("name")
    @Nullable
    public String name;
    @SerializedName("description")
    public String description;

    public RealmsDescriptionDto(@Nullable String pName, String pDescription) {
        this.name = pName;
        this.description = pDescription;
    }
}