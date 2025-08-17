package net.minecraft.client.renderer.block.model;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public record BlockElement(
    Vector3fc from,
    Vector3fc to,
    Map<Direction, BlockElementFace> faces,
    @Nullable BlockElementRotation rotation,
    boolean shade,
    int lightEmission
) {
    private static final boolean DEFAULT_RESCALE = false;
    private static final float MIN_EXTENT = -16.0F;
    private static final float MAX_EXTENT = 32.0F;

    public BlockElement(Vector3fc pFrom, Vector3fc pTo, Map<Direction, BlockElementFace> pFaces) {
        this(pFrom, pTo, pFaces, null, true, 0);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Deserializer implements JsonDeserializer<BlockElement> {
        private static final boolean DEFAULT_SHADE = true;
        private static final int DEFAULT_LIGHT_EMISSION = 0;

        public BlockElement deserialize(JsonElement pJson, Type pType, JsonDeserializationContext pContext) throws JsonParseException {
            JsonObject jsonobject = pJson.getAsJsonObject();
            Vector3f vector3f = this.getFrom(jsonobject);
            Vector3f vector3f1 = this.getTo(jsonobject);
            BlockElementRotation blockelementrotation = this.getRotation(jsonobject);
            Map<Direction, BlockElementFace> map = this.getFaces(pContext, jsonobject);
            if (jsonobject.has("shade") && !GsonHelper.isBooleanValue(jsonobject, "shade")) {
                throw new JsonParseException("Expected shade to be a Boolean");
            } else {
                boolean flag = GsonHelper.getAsBoolean(jsonobject, "shade", true);
                int i = 0;
                if (jsonobject.has("light_emission")) {
                    boolean flag1 = GsonHelper.isNumberValue(jsonobject, "light_emission");
                    if (flag1) {
                        i = GsonHelper.getAsInt(jsonobject, "light_emission");
                    }

                    if (!flag1 || i < 0 || i > 15) {
                        throw new JsonParseException("Expected light_emission to be an Integer between (inclusive) 0 and 15");
                    }
                }

                return new BlockElement(vector3f, vector3f1, map, blockelementrotation, flag, i);
            }
        }

        @Nullable
        private BlockElementRotation getRotation(JsonObject pJson) {
            BlockElementRotation blockelementrotation = null;
            if (pJson.has("rotation")) {
                JsonObject jsonobject = GsonHelper.getAsJsonObject(pJson, "rotation");
                Vector3f vector3f = this.getVector3f(jsonobject, "origin");
                vector3f.mul(0.0625F);
                Direction.Axis direction$axis = this.getAxis(jsonobject);
                float f = this.getAngle(jsonobject);
                boolean flag = GsonHelper.getAsBoolean(jsonobject, "rescale", false);
                blockelementrotation = new BlockElementRotation(vector3f, direction$axis, f, flag);
            }

            return blockelementrotation;
        }

        private float getAngle(JsonObject pJson) {
            float f = GsonHelper.getAsFloat(pJson, "angle");
            if (Mth.abs(f) > 45.0F) {
                throw new JsonParseException("Invalid rotation " + f + " found, only values in [-45,45] range allowed");
            } else {
                return f;
            }
        }

        private Direction.Axis getAxis(JsonObject pJson) {
            String s = GsonHelper.getAsString(pJson, "axis");
            Direction.Axis direction$axis = Direction.Axis.byName(s.toLowerCase(Locale.ROOT));
            if (direction$axis == null) {
                throw new JsonParseException("Invalid rotation axis: " + s);
            } else {
                return direction$axis;
            }
        }

        private Map<Direction, BlockElementFace> getFaces(JsonDeserializationContext pContext, JsonObject pJson) {
            Map<Direction, BlockElementFace> map = this.filterNullFromFaces(pContext, pJson);
            if (map.isEmpty()) {
                throw new JsonParseException("Expected between 1 and 6 unique faces, got 0");
            } else {
                return map;
            }
        }

        private Map<Direction, BlockElementFace> filterNullFromFaces(JsonDeserializationContext pContext, JsonObject pJson) {
            Map<Direction, BlockElementFace> map = Maps.newEnumMap(Direction.class);
            JsonObject jsonobject = GsonHelper.getAsJsonObject(pJson, "faces");

            for (Entry<String, JsonElement> entry : jsonobject.entrySet()) {
                Direction direction = this.getFacing(entry.getKey());
                map.put(direction, pContext.deserialize(entry.getValue(), BlockElementFace.class));
            }

            return map;
        }

        private Direction getFacing(String pName) {
            Direction direction = Direction.byName(pName);
            if (direction == null) {
                throw new JsonParseException("Unknown facing: " + pName);
            } else {
                return direction;
            }
        }

        private Vector3f getTo(JsonObject pJson) {
            Vector3f vector3f = this.getVector3f(pJson, "to");
            if (!(vector3f.x() < -16.0F)
                && !(vector3f.y() < -16.0F)
                && !(vector3f.z() < -16.0F)
                && !(vector3f.x() > 32.0F)
                && !(vector3f.y() > 32.0F)
                && !(vector3f.z() > 32.0F)) {
                return vector3f;
            } else {
                throw new JsonParseException("'to' specifier exceeds the allowed boundaries: " + vector3f);
            }
        }

        private Vector3f getFrom(JsonObject pJson) {
            Vector3f vector3f = this.getVector3f(pJson, "from");
            if (!(vector3f.x() < -16.0F)
                && !(vector3f.y() < -16.0F)
                && !(vector3f.z() < -16.0F)
                && !(vector3f.x() > 32.0F)
                && !(vector3f.y() > 32.0F)
                && !(vector3f.z() > 32.0F)) {
                return vector3f;
            } else {
                throw new JsonParseException("'from' specifier exceeds the allowed boundaries: " + vector3f);
            }
        }

        private Vector3f getVector3f(JsonObject pJson, String pMemberName) {
            JsonArray jsonarray = GsonHelper.getAsJsonArray(pJson, pMemberName);
            if (jsonarray.size() != 3) {
                throw new JsonParseException("Expected 3 " + pMemberName + " values, found: " + jsonarray.size());
            } else {
                float[] afloat = new float[3];

                for (int i = 0; i < afloat.length; i++) {
                    afloat[i] = GsonHelper.convertToFloat(jsonarray.get(i), pMemberName + "[" + i + "]");
                }

                return new Vector3f(afloat[0], afloat[1], afloat[2]);
            }
        }
    }
}