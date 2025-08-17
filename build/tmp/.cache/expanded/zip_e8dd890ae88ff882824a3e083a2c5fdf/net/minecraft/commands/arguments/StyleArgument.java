package net.minecraft.commands.arguments;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.serialization.DynamicOps;
import java.util.Collection;
import java.util.List;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.SnbtGrammar;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.parsing.packrat.commands.CommandArgumentParser;
import net.minecraft.util.parsing.packrat.commands.ParserBasedArgument;

public class StyleArgument extends ParserBasedArgument<Style> {
    private static final Collection<String> EXAMPLES = List.of("{bold: true}", "{color: 'red'}", "{}");
    public static final DynamicCommandExceptionType ERROR_INVALID_STYLE = new DynamicCommandExceptionType(
        p_310381_ -> Component.translatableEscape("argument.style.invalid", p_310381_)
    );
    private static final DynamicOps<Tag> OPS = NbtOps.INSTANCE;
    private static final CommandArgumentParser<Tag> TAG_PARSER = SnbtGrammar.createParser(OPS);

    private StyleArgument(HolderLookup.Provider pRegistries) {
        super(TAG_PARSER.withCodec(pRegistries.createSerializationContext(OPS), TAG_PARSER, Style.Serializer.CODEC, ERROR_INVALID_STYLE));
    }

    public static Style getStyle(CommandContext<CommandSourceStack> pContext, String pName) {
        return pContext.getArgument(pName, Style.class);
    }

    public static StyleArgument style(CommandBuildContext pContext) {
        return new StyleArgument(pContext);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}