package net.minecraft.network.protocol.game;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.function.BiPredicate;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.ResourceLocation;

public class ClientboundCommandsPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundCommandsPacket> STREAM_CODEC = Packet.codec(
        ClientboundCommandsPacket::write, ClientboundCommandsPacket::new
    );
    private static final byte MASK_TYPE = 3;
    private static final byte FLAG_EXECUTABLE = 4;
    private static final byte FLAG_REDIRECT = 8;
    private static final byte FLAG_CUSTOM_SUGGESTIONS = 16;
    private static final byte FLAG_RESTRICTED = 32;
    private static final byte TYPE_ROOT = 0;
    private static final byte TYPE_LITERAL = 1;
    private static final byte TYPE_ARGUMENT = 2;
    private final int rootIndex;
    private final List<ClientboundCommandsPacket.Entry> entries;

    public <S> ClientboundCommandsPacket(RootCommandNode<S> pRoot, ClientboundCommandsPacket.NodeInspector<S> pNodeInspector) {
        Object2IntMap<CommandNode<S>> object2intmap = enumerateNodes(pRoot);
        this.entries = createEntries(object2intmap, pNodeInspector);
        this.rootIndex = object2intmap.getInt(pRoot);
    }

    private ClientboundCommandsPacket(FriendlyByteBuf pBuffer) {
        this.entries = pBuffer.readList(ClientboundCommandsPacket::readNode);
        this.rootIndex = pBuffer.readVarInt();
        validateEntries(this.entries);
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeCollection(this.entries, (p_237642_, p_237643_) -> p_237643_.write(p_237642_));
        pBuffer.writeVarInt(this.rootIndex);
    }

    private static void validateEntries(List<ClientboundCommandsPacket.Entry> pEntries, BiPredicate<ClientboundCommandsPacket.Entry, IntSet> pValidator) {
        IntSet intset = new IntOpenHashSet(IntSets.fromTo(0, pEntries.size()));

        while (!intset.isEmpty()) {
            boolean flag = intset.removeIf(p_237637_ -> pValidator.test(pEntries.get(p_237637_), intset));
            if (!flag) {
                throw new IllegalStateException("Server sent an impossible command tree");
            }
        }
    }

    private static void validateEntries(List<ClientboundCommandsPacket.Entry> pEntries) {
        validateEntries(pEntries, ClientboundCommandsPacket.Entry::canBuild);
        validateEntries(pEntries, ClientboundCommandsPacket.Entry::canResolve);
    }

    private static <S> Object2IntMap<CommandNode<S>> enumerateNodes(RootCommandNode<S> pRootNode) {
        Object2IntMap<CommandNode<S>> object2intmap = new Object2IntOpenHashMap<>();
        Queue<CommandNode<S>> queue = new ArrayDeque<>();
        queue.add(pRootNode);

        CommandNode<S> commandnode;
        while ((commandnode = queue.poll()) != null) {
            if (!object2intmap.containsKey(commandnode)) {
                int i = object2intmap.size();
                object2intmap.put(commandnode, i);
                queue.addAll(commandnode.getChildren());
                if (commandnode.getRedirect() != null) {
                    queue.add(commandnode.getRedirect());
                }
            }
        }

        return object2intmap;
    }

    private static <S> List<ClientboundCommandsPacket.Entry> createEntries(
        Object2IntMap<CommandNode<S>> pNodes, ClientboundCommandsPacket.NodeInspector<S> pNodeInspector
    ) {
        ObjectArrayList<ClientboundCommandsPacket.Entry> objectarraylist = new ObjectArrayList<>(pNodes.size());
        objectarraylist.size(pNodes.size());

        for (Object2IntMap.Entry<CommandNode<S>> entry : Object2IntMaps.fastIterable(pNodes)) {
            objectarraylist.set(entry.getIntValue(), createEntry(entry.getKey(), pNodeInspector, pNodes));
        }

        return objectarraylist;
    }

    private static ClientboundCommandsPacket.Entry readNode(FriendlyByteBuf pBuffer) {
        byte b0 = pBuffer.readByte();
        int[] aint = pBuffer.readVarIntArray();
        int i = (b0 & 8) != 0 ? pBuffer.readVarInt() : 0;
        ClientboundCommandsPacket.NodeStub clientboundcommandspacket$nodestub = read(pBuffer, b0);
        return new ClientboundCommandsPacket.Entry(clientboundcommandspacket$nodestub, b0, i, aint);
    }

    @Nullable
    private static ClientboundCommandsPacket.NodeStub read(FriendlyByteBuf pBuffer, byte pFlags) {
        int i = pFlags & 3;
        if (i == 2) {
            String s1 = pBuffer.readUtf();
            int j = pBuffer.readVarInt();
            ArgumentTypeInfo<?, ?> argumenttypeinfo = BuiltInRegistries.COMMAND_ARGUMENT_TYPE.byId(j);
            if (argumenttypeinfo == null) {
                return null;
            } else {
                ArgumentTypeInfo.Template<?> template = argumenttypeinfo.deserializeFromNetwork(pBuffer);
                ResourceLocation resourcelocation = (pFlags & 16) != 0 ? pBuffer.readResourceLocation() : null;
                return new ClientboundCommandsPacket.ArgumentNodeStub(s1, template, resourcelocation);
            }
        } else if (i == 1) {
            String s = pBuffer.readUtf();
            return new ClientboundCommandsPacket.LiteralNodeStub(s);
        } else {
            return null;
        }
    }

    private static <S> ClientboundCommandsPacket.Entry createEntry(
        CommandNode<S> pNode, ClientboundCommandsPacket.NodeInspector<S> pNodeInspector, Object2IntMap<CommandNode<S>> pNodes
    ) {
        int i = 0;
        int j;
        if (pNode.getRedirect() != null) {
            i |= 8;
            j = pNodes.getInt(pNode.getRedirect());
        } else {
            j = 0;
        }

        if (pNodeInspector.isExecutable(pNode)) {
            i |= 4;
        }

        if (pNodeInspector.isRestricted(pNode)) {
            i |= 32;
        }

        ClientboundCommandsPacket.NodeStub clientboundcommandspacket$nodestub;
        switch (pNode) {
            case RootCommandNode<S> rootcommandnode:
                i |= 0;
                clientboundcommandspacket$nodestub = null;
                break;
            case ArgumentCommandNode<S, ?> argumentcommandnode:
                ResourceLocation resourcelocation = pNodeInspector.suggestionId(argumentcommandnode);
                clientboundcommandspacket$nodestub = new ClientboundCommandsPacket.ArgumentNodeStub(
                    argumentcommandnode.getName(), ArgumentTypeInfos.unpack(argumentcommandnode.getType()), resourcelocation
                );
                i |= 2;
                if (resourcelocation != null) {
                    i |= 16;
                }
                break;
            case LiteralCommandNode<S> literalcommandnode:
                clientboundcommandspacket$nodestub = new ClientboundCommandsPacket.LiteralNodeStub(literalcommandnode.getLiteral());
                i |= 1;
                break;
            default:
                throw new UnsupportedOperationException("Unknown node type " + pNode);
        }

        int[] aint = pNode.getChildren().stream().mapToInt(pNodes::getInt).toArray();
        return new ClientboundCommandsPacket.Entry(clientboundcommandspacket$nodestub, i, j, aint);
    }

    @Override
    public PacketType<ClientboundCommandsPacket> type() {
        return GamePacketTypes.CLIENTBOUND_COMMANDS;
    }

    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleCommands(this);
    }

    public <S> RootCommandNode<S> getRoot(CommandBuildContext pContext, ClientboundCommandsPacket.NodeBuilder<S> pNodeBuilder) {
        return (RootCommandNode<S>)new ClientboundCommandsPacket.NodeResolver<>(pContext, pNodeBuilder, this.entries).resolve(this.rootIndex);
    }

    record ArgumentNodeStub(String id, ArgumentTypeInfo.Template<?> argumentType, @Nullable ResourceLocation suggestionId)
        implements ClientboundCommandsPacket.NodeStub {
        @Override
        public <S> ArgumentBuilder<S, ?> build(CommandBuildContext p_237656_, ClientboundCommandsPacket.NodeBuilder<S> p_407604_) {
            ArgumentType<?> argumenttype = this.argumentType.instantiate(p_237656_);
            return p_407604_.createArgument(this.id, argumenttype, this.suggestionId);
        }

        @Override
        public void write(FriendlyByteBuf p_237658_) {
            p_237658_.writeUtf(this.id);
            serializeCap(p_237658_, this.argumentType);
            if (this.suggestionId != null) {
                p_237658_.writeResourceLocation(this.suggestionId);
            }
        }

        private static <A extends ArgumentType<?>> void serializeCap(FriendlyByteBuf pBuffer, ArgumentTypeInfo.Template<A> pArgumentInfoTemplate) {
            serializeCap(pBuffer, pArgumentInfoTemplate.type(), pArgumentInfoTemplate);
        }

        private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void serializeCap(
            FriendlyByteBuf pBuffer, ArgumentTypeInfo<A, T> pArgumentInfo, ArgumentTypeInfo.Template<A> pArgumentInfoTemplate
        ) {
            pBuffer.writeVarInt(BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getId(pArgumentInfo));
            pArgumentInfo.serializeToNetwork((T)pArgumentInfoTemplate, pBuffer);
        }
    }

    record Entry(@Nullable ClientboundCommandsPacket.NodeStub stub, int flags, int redirect, int[] children) {
        public void write(FriendlyByteBuf pBuffer) {
            pBuffer.writeByte(this.flags);
            pBuffer.writeVarIntArray(this.children);
            if ((this.flags & 8) != 0) {
                pBuffer.writeVarInt(this.redirect);
            }

            if (this.stub != null) {
                this.stub.write(pBuffer);
            }
        }

        public boolean canBuild(IntSet pChildren) {
            return (this.flags & 8) != 0 ? !pChildren.contains(this.redirect) : true;
        }

        public boolean canResolve(IntSet pChildren) {
            for (int i : this.children) {
                if (pChildren.contains(i)) {
                    return false;
                }
            }

            return true;
        }
    }

    record LiteralNodeStub(String id) implements ClientboundCommandsPacket.NodeStub {
        @Override
        public <S> ArgumentBuilder<S, ?> build(CommandBuildContext p_237682_, ClientboundCommandsPacket.NodeBuilder<S> p_407985_) {
            return p_407985_.createLiteral(this.id);
        }

        @Override
        public void write(FriendlyByteBuf p_237684_) {
            p_237684_.writeUtf(this.id);
        }
    }

    public interface NodeBuilder<S> {
        ArgumentBuilder<S, ?> createLiteral(String pId);

        ArgumentBuilder<S, ?> createArgument(String pId, ArgumentType<?> pType, @Nullable ResourceLocation pSuggestionId);

        ArgumentBuilder<S, ?> configure(ArgumentBuilder<S, ?> pArgumentBuilder, boolean pExecutable, boolean pRestricted);
    }

    public interface NodeInspector<S> {
        @Nullable
        ResourceLocation suggestionId(ArgumentCommandNode<S, ?> pNode);

        boolean isExecutable(CommandNode<S> pNode);

        boolean isRestricted(CommandNode<S> pNode);
    }

    static class NodeResolver<S> {
        private final CommandBuildContext context;
        private final ClientboundCommandsPacket.NodeBuilder<S> builder;
        private final List<ClientboundCommandsPacket.Entry> entries;
        private final List<CommandNode<S>> nodes;

        NodeResolver(CommandBuildContext pContext, ClientboundCommandsPacket.NodeBuilder<S> pBuilder, List<ClientboundCommandsPacket.Entry> pEntries) {
            this.context = pContext;
            this.builder = pBuilder;
            this.entries = pEntries;
            ObjectArrayList<CommandNode<S>> objectarraylist = new ObjectArrayList<>();
            objectarraylist.size(pEntries.size());
            this.nodes = objectarraylist;
        }

        public CommandNode<S> resolve(int pIndex) {
            CommandNode<S> commandnode = this.nodes.get(pIndex);
            if (commandnode != null) {
                return commandnode;
            } else {
                ClientboundCommandsPacket.Entry clientboundcommandspacket$entry = this.entries.get(pIndex);
                CommandNode<S> commandnode1;
                if (clientboundcommandspacket$entry.stub == null) {
                    commandnode1 = new RootCommandNode<>();
                } else {
                    ArgumentBuilder<S, ?> argumentbuilder = clientboundcommandspacket$entry.stub.build(this.context, this.builder);
                    if ((clientboundcommandspacket$entry.flags & 8) != 0) {
                        argumentbuilder.redirect(this.resolve(clientboundcommandspacket$entry.redirect));
                    }

                    boolean flag = (clientboundcommandspacket$entry.flags & 4) != 0;
                    boolean flag1 = (clientboundcommandspacket$entry.flags & 32) != 0;
                    commandnode1 = this.builder.configure(argumentbuilder, flag, flag1).build();
                }

                this.nodes.set(pIndex, commandnode1);

                for (int i : clientboundcommandspacket$entry.children) {
                    CommandNode<S> commandnode2 = this.resolve(i);
                    if (!(commandnode2 instanceof RootCommandNode)) {
                        commandnode1.addChild(commandnode2);
                    }
                }

                return commandnode1;
            }
        }
    }

    interface NodeStub {
        <S> ArgumentBuilder<S, ?> build(CommandBuildContext pContext, ClientboundCommandsPacket.NodeBuilder<S> pNodeBuilder);

        void write(FriendlyByteBuf pBuffer);
    }
}