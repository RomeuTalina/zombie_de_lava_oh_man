package net.minecraft.client.gui.render.state;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.mutable.MutableInt;

@OnlyIn(Dist.CLIENT)
public class GuiRenderState {
    private static final int DEBUG_RECTANGLE_COLOR = 2000962815;
    private final List<GuiRenderState.Node> strata = new ArrayList<>();
    private int firstStratumAfterBlur = Integer.MAX_VALUE;
    private GuiRenderState.Node current;
    private final Set<Object> itemModelIdentities = new HashSet<>();
    @Nullable
    private ScreenRectangle lastElementBounds;

    public GuiRenderState() {
        this.nextStratum();
    }

    public void nextStratum() {
        this.current = new GuiRenderState.Node(null);
        this.strata.add(this.current);
    }

    public void blurBeforeThisStratum() {
        if (this.firstStratumAfterBlur != Integer.MAX_VALUE) {
            throw new IllegalStateException("Can only blur once per frame");
        } else {
            this.firstStratumAfterBlur = this.strata.size() - 1;
        }
    }

    public void up() {
        if (this.current.up == null) {
            this.current.up = new GuiRenderState.Node(this.current);
        }

        this.current = this.current.up;
    }

    public void down() {
        if (this.current.down == null) {
            this.current.down = new GuiRenderState.Node(this.current);
        }

        this.current = this.current.down;
    }

    public void submitItem(GuiItemRenderState pRenderState) {
        if (this.findAppropriateNode(pRenderState)) {
            this.itemModelIdentities.add(pRenderState.itemStackRenderState().getModelIdentity());
            this.current.submitItem(pRenderState);
            this.sumbitDebugRectangleIfEnabled(pRenderState.bounds());
        }
    }

    public void submitText(GuiTextRenderState pRenderState) {
        if (this.findAppropriateNode(pRenderState)) {
            this.current.submitText(pRenderState);
            this.sumbitDebugRectangleIfEnabled(pRenderState.bounds());
        }
    }

    public void submitPicturesInPictureState(PictureInPictureRenderState pRenderState) {
        if (this.findAppropriateNode(pRenderState)) {
            this.current.submitPicturesInPictureState(pRenderState);
            this.sumbitDebugRectangleIfEnabled(pRenderState.bounds());
        }
    }

    public void submitGuiElement(GuiElementRenderState pRenderState) {
        if (this.findAppropriateNode(pRenderState)) {
            this.current.submitGuiElement(pRenderState);
            this.sumbitDebugRectangleIfEnabled(pRenderState.bounds());
        }
    }

    private void sumbitDebugRectangleIfEnabled(@Nullable ScreenRectangle pDebugRectangle) {
    }

    private boolean findAppropriateNode(ScreenArea pScreenArea) {
        ScreenRectangle screenrectangle = pScreenArea.bounds();
        if (screenrectangle == null) {
            return false;
        } else {
            if (this.lastElementBounds != null && this.lastElementBounds.encompasses(screenrectangle)) {
                this.up();
            } else {
                this.navigateToAboveHighestElementWithIntersectingBounds(screenrectangle);
            }

            this.lastElementBounds = screenrectangle;
            return true;
        }
    }

    private void navigateToAboveHighestElementWithIntersectingBounds(ScreenRectangle pRectangle) {
        GuiRenderState.Node guirenderstate$node = this.strata.getLast();

        while (guirenderstate$node.up != null) {
            guirenderstate$node = guirenderstate$node.up;
        }

        boolean flag = false;

        while (!flag) {
            flag = this.hasIntersection(pRectangle, guirenderstate$node.elementStates)
                || this.hasIntersection(pRectangle, guirenderstate$node.itemStates)
                || this.hasIntersection(pRectangle, guirenderstate$node.textStates)
                || this.hasIntersection(pRectangle, guirenderstate$node.picturesInPictureStates);
            if (guirenderstate$node.parent == null) {
                break;
            }

            if (!flag) {
                guirenderstate$node = guirenderstate$node.parent;
            }
        }

        this.current = guirenderstate$node;
        if (flag) {
            this.up();
        }
    }

    private boolean hasIntersection(ScreenRectangle pRectangle, @Nullable List<? extends ScreenArea> pScreenAreas) {
        if (pScreenAreas != null) {
            for (ScreenArea screenarea : pScreenAreas) {
                ScreenRectangle screenrectangle = screenarea.bounds();
                if (screenrectangle != null && screenrectangle.intersects(pRectangle)) {
                    return true;
                }
            }
        }

        return false;
    }

    public void submitBlitToCurrentLayer(BlitRenderState pRenderState) {
        this.current.submitGuiElement(pRenderState);
    }

    public void submitGlyphToCurrentLayer(GuiElementRenderState pRenderState) {
        this.current.submitGlyph(pRenderState);
    }

    public Set<Object> getItemModelIdentities() {
        return this.itemModelIdentities;
    }

    public void forEachElement(GuiRenderState.LayeredElementConsumer pAction, GuiRenderState.TraverseRange pTraverseRange) {
        MutableInt mutableint = new MutableInt(0);
        this.traverse(p_405927_ -> {
            if (p_405927_.elementStates != null || p_405927_.glyphStates != null) {
                int i = mutableint.incrementAndGet();
                if (p_405927_.elementStates != null) {
                    for (GuiElementRenderState guielementrenderstate : p_405927_.elementStates) {
                        pAction.accept(guielementrenderstate, i);
                    }
                }

                if (p_405927_.glyphStates != null) {
                    for (GuiElementRenderState guielementrenderstate1 : p_405927_.glyphStates) {
                        pAction.accept(guielementrenderstate1, i);
                    }
                }
            }
        }, pTraverseRange);
    }

    public void forEachItem(Consumer<GuiItemRenderState> pAction) {
        GuiRenderState.Node guirenderstate$node = this.current;
        this.traverse(p_409949_ -> {
            if (p_409949_.itemStates != null) {
                this.current = p_409949_;

                for (GuiItemRenderState guiitemrenderstate : p_409949_.itemStates) {
                    pAction.accept(guiitemrenderstate);
                }
            }
        }, GuiRenderState.TraverseRange.ALL);
        this.current = guirenderstate$node;
    }

    public void forEachText(Consumer<GuiTextRenderState> pAction) {
        GuiRenderState.Node guirenderstate$node = this.current;
        this.traverse(p_409407_ -> {
            if (p_409407_.textStates != null) {
                for (GuiTextRenderState guitextrenderstate : p_409407_.textStates) {
                    this.current = p_409407_;
                    pAction.accept(guitextrenderstate);
                }
            }
        }, GuiRenderState.TraverseRange.ALL);
        this.current = guirenderstate$node;
    }

    public void forEachPictureInPicture(Consumer<PictureInPictureRenderState> pAction) {
        GuiRenderState.Node guirenderstate$node = this.current;
        this.traverse(p_407281_ -> {
            if (p_407281_.picturesInPictureStates != null) {
                this.current = p_407281_;

                for (PictureInPictureRenderState pictureinpicturerenderstate : p_407281_.picturesInPictureStates) {
                    pAction.accept(pictureinpicturerenderstate);
                }
            }
        }, GuiRenderState.TraverseRange.ALL);
        this.current = guirenderstate$node;
    }

    public void sortElements(Comparator<GuiElementRenderState> pComparator) {
        this.traverse(p_406585_ -> {
            if (p_406585_.elementStates != null) {
                p_406585_.elementStates.sort(pComparator);
            }
        }, GuiRenderState.TraverseRange.ALL);
    }

    private void traverse(Consumer<GuiRenderState.Node> pAction, GuiRenderState.TraverseRange pTraverseRange) {
        int i = 0;
        int j = this.strata.size();
        if (pTraverseRange == GuiRenderState.TraverseRange.BEFORE_BLUR) {
            j = Math.min(this.firstStratumAfterBlur, this.strata.size());
        } else if (pTraverseRange == GuiRenderState.TraverseRange.AFTER_BLUR) {
            i = this.firstStratumAfterBlur;
        }

        for (int k = i; k < j; k++) {
            GuiRenderState.Node guirenderstate$node = this.strata.get(k);
            this.traverse(guirenderstate$node, pAction);
        }
    }

    private void traverse(GuiRenderState.Node pNode, Consumer<GuiRenderState.Node> pAction) {
        if (pNode.down != null) {
            this.traverse(pNode.down, pAction);
        }

        pAction.accept(pNode);
        if (pNode.up != null) {
            this.traverse(pNode.up, pAction);
        }
    }

    public void reset() {
        this.itemModelIdentities.clear();
        this.strata.clear();
        this.firstStratumAfterBlur = Integer.MAX_VALUE;
        this.nextStratum();
    }

    @OnlyIn(Dist.CLIENT)
    public interface LayeredElementConsumer {
        void accept(GuiElementRenderState pRenderState, int pLayer);
    }

    @OnlyIn(Dist.CLIENT)
    static class Node {
        @Nullable
        public final GuiRenderState.Node parent;
        @Nullable
        public GuiRenderState.Node up;
        @Nullable
        public GuiRenderState.Node down;
        @Nullable
        public List<GuiElementRenderState> elementStates;
        @Nullable
        public List<GuiElementRenderState> glyphStates;
        @Nullable
        public List<GuiItemRenderState> itemStates;
        @Nullable
        public List<GuiTextRenderState> textStates;
        @Nullable
        public List<PictureInPictureRenderState> picturesInPictureStates;

        Node(@Nullable GuiRenderState.Node pParent) {
            this.parent = pParent;
        }

        public void submitItem(GuiItemRenderState pRenderState) {
            if (this.itemStates == null) {
                this.itemStates = new ArrayList<>();
            }

            this.itemStates.add(pRenderState);
        }

        public void submitText(GuiTextRenderState pRenderState) {
            if (this.textStates == null) {
                this.textStates = new ArrayList<>();
            }

            this.textStates.add(pRenderState);
        }

        public void submitPicturesInPictureState(PictureInPictureRenderState pRenderState) {
            if (this.picturesInPictureStates == null) {
                this.picturesInPictureStates = new ArrayList<>();
            }

            this.picturesInPictureStates.add(pRenderState);
        }

        public void submitGuiElement(GuiElementRenderState pRenderState) {
            if (this.elementStates == null) {
                this.elementStates = new ArrayList<>();
            }

            this.elementStates.add(pRenderState);
        }

        public void submitGlyph(GuiElementRenderState pRenderState) {
            if (this.glyphStates == null) {
                this.glyphStates = new ArrayList<>();
            }

            this.glyphStates.add(pRenderState);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static enum TraverseRange {
        ALL,
        BEFORE_BLUR,
        AFTER_BLUR;
    }
}