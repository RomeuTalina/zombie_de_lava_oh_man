package net.minecraft.commands.synchronization;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.commands.PermissionCheck;
import org.slf4j.Logger;

public class ArgumentUtils {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final byte NUMBER_FLAG_MIN = 1;
    private static final byte NUMBER_FLAG_MAX = 2;

    public static int createNumberFlags(boolean pMin, boolean pMax) {
        int i = 0;
        if (pMin) {
            i |= 1;
        }

        if (pMax) {
            i |= 2;
        }

        return i;
    }

    public static boolean numberHasMin(byte pNumber) {
        return (pNumber & 1) != 0;
    }

    public static boolean numberHasMax(byte pNumber) {
        return (pNumber & 2) != 0;
    }

    private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void serializeArgumentCap(
        JsonObject pJson, ArgumentTypeInfo<A, T> pType, ArgumentTypeInfo.Template<A> pTemplate
    ) {
        pType.serializeToJson((T)pTemplate, pJson);
    }

    private static <T extends ArgumentType<?>> void serializeArgumentToJson(JsonObject pJson, T pType) {
        ArgumentTypeInfo.Template<T> template = ArgumentTypeInfos.unpack(pType);
        pJson.addProperty("type", "argument");
        pJson.addProperty("parser", String.valueOf(BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getKey(template.type())));
        JsonObject jsonobject = new JsonObject();
        serializeArgumentCap(jsonobject, template.type(), template);
        if (!jsonobject.isEmpty()) {
            pJson.add("properties", jsonobject);
        }
    }

    public static <S> JsonObject serializeNodeToJson(CommandDispatcher<S> pDispatcher, CommandNode<S> pNode) {
        JsonObject jsonobject = new JsonObject();
        switch (pNode) {
            case RootCommandNode<S> rootcommandnode:
                jsonobject.addProperty("type", "root");
                break;
            case LiteralCommandNode<S> literalcommandnode:
                jsonobject.addProperty("type", "literal");
                break;
            case ArgumentCommandNode<S, ?> argumentcommandnode:
                serializeArgumentToJson(jsonobject, argumentcommandnode.getType());
                break;
            default:
                LOGGER.error("Could not serialize node {} ({})!", pNode, pNode.getClass());
                jsonobject.addProperty("type", "unknown");
        }

        Collection<CommandNode<S>> collection = pNode.getChildren();
        if (!collection.isEmpty()) {
            JsonObject jsonobject1 = new JsonObject();

            for (CommandNode<S> commandnode : collection) {
                jsonobject1.add(commandnode.getName(), serializeNodeToJson(pDispatcher, commandnode));
            }

            jsonobject.add("children", jsonobject1);
        }

        if (pNode.getCommand() != null) {
            jsonobject.addProperty("executable", true);
        }

        if (pNode.getRequirement() instanceof PermissionCheck<?> permissioncheck) {
            jsonobject.addProperty("required_level", permissioncheck.requiredLevel());
        }

        if (pNode.getRedirect() != null) {
            Collection<String> collection1 = pDispatcher.getPath(pNode.getRedirect());
            if (!collection1.isEmpty()) {
                JsonArray jsonarray = new JsonArray();

                for (String s : collection1) {
                    jsonarray.add(s);
                }

                jsonobject.add("redirect", jsonarray);
            }
        }

        return jsonobject;
    }

    public static <T> Set<ArgumentType<?>> findUsedArgumentTypes(CommandNode<T> pNode) {
        Set<CommandNode<T>> set = new ReferenceOpenHashSet<>();
        Set<ArgumentType<?>> set1 = new HashSet<>();
        findUsedArgumentTypes(pNode, set1, set);
        return set1;
    }

    private static <T> void findUsedArgumentTypes(CommandNode<T> pNode, Set<ArgumentType<?>> pTypes, Set<CommandNode<T>> pNodes) {
        if (pNodes.add(pNode)) {
            if (pNode instanceof ArgumentCommandNode<T, ?> argumentcommandnode) {
                pTypes.add(argumentcommandnode.getType());
            }

            pNode.getChildren().forEach(p_235426_ -> findUsedArgumentTypes((CommandNode<T>)p_235426_, pTypes, pNodes));
            CommandNode<T> commandnode = pNode.getRedirect();
            if (commandnode != null) {
                findUsedArgumentTypes(commandnode, pTypes, pNodes);
            }
        }
    }
}