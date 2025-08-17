package net.minecraft.gametest.framework;

import net.minecraft.network.chat.Component;

public class GameTestAssertException extends GameTestException {
    protected final Component message;
    protected final int tick;

    public GameTestAssertException(Component pMessage, int pTick) {
        super(pMessage.getString());
        this.message = pMessage;
        this.tick = pTick;
    }

    @Override
    public Component getDescription() {
        return Component.translatable("test.error.tick", this.message, this.tick);
    }

    @Override
    public String getMessage() {
        return this.getDescription().getString();
    }
}