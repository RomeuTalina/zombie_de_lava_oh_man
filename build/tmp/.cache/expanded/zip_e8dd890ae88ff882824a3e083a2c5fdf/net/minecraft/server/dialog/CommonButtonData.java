package net.minecraft.server.dialog;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

public record CommonButtonData(Component label, Optional<Component> tooltip, int width) {
    public static final int DEFAULT_WIDTH = 150;
    public static final MapCodec<CommonButtonData> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_407462_ -> p_407462_.group(
                ComponentSerialization.CODEC.fieldOf("label").forGetter(CommonButtonData::label),
                ComponentSerialization.CODEC.optionalFieldOf("tooltip").forGetter(CommonButtonData::tooltip),
                Dialog.WIDTH_CODEC.optionalFieldOf("width", 150).forGetter(CommonButtonData::width)
            )
            .apply(p_407462_, CommonButtonData::new)
    );

    public CommonButtonData(Component pLabel, int pWidth) {
        this(pLabel, Optional.empty(), pWidth);
    }
}