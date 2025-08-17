package net.minecraft.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.Reader;

public class LenientJsonParser {
    public static JsonElement parse(Reader pReader) throws JsonIOException, JsonSyntaxException {
        return JsonParser.parseReader(pReader);
    }

    public static JsonElement parse(String pReader) throws JsonSyntaxException {
        return JsonParser.parseString(pReader);
    }
}