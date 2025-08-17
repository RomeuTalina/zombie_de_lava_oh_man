package net.minecraft.gametest.framework;

import com.google.common.collect.Lists;
import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.Util;

public class GameTestTicker {
    public static final GameTestTicker SINGLETON = new GameTestTicker();
    private final Collection<GameTestInfo> testInfos = Lists.newCopyOnWriteArrayList();
    @Nullable
    private GameTestRunner runner;
    private GameTestTicker.State state = GameTestTicker.State.IDLE;
    private volatile boolean ticking = false;

    private GameTestTicker() {
    }

    public void add(GameTestInfo pTestInfo) {
        this.testInfos.add(pTestInfo);
    }

    public void clear() {
        if (this.state != GameTestTicker.State.IDLE) {
            this.state = GameTestTicker.State.HALTING;
        } else {
            this.testInfos.clear();
            if (this.runner != null) {
                this.runner.stop();
                this.runner = null;
            }
        }
    }

    public void setRunner(GameTestRunner pRunner) {
        if (this.runner != null) {
            Util.logAndPauseIfInIde("The runner was already set in GameTestTicker");
        }

        this.runner = pRunner;
    }

    public void startTicking() {
        this.ticking = true;
    }

    public void tick() {
        if (this.runner != null && this.ticking) {
            this.state = GameTestTicker.State.RUNNING;
            this.testInfos.forEach(p_328686_ -> p_328686_.tick(this.runner));
            this.testInfos.removeIf(GameTestInfo::isDone);
            GameTestTicker.State gametestticker$state = this.state;
            this.state = GameTestTicker.State.IDLE;
            if (gametestticker$state == GameTestTicker.State.HALTING) {
                this.clear();
            }
        }
    }

    static enum State {
        IDLE,
        RUNNING,
        HALTING;
    }
}