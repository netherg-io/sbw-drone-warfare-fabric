package nl.smartstreamlabs.sbwdroneconfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class JammerScreen extends Screen {
    private final net.minecraft.core.BlockPos blockPos;
    private final List<String> whitelistedGamertags;
    private RangeSlider rangeSlider;
    private EditBox input;

    protected JammerScreen(net.minecraft.core.BlockPos blockPos, int currentRange, List<String> whitelistedGamertags) {
        super(Component.translatable("screen.sbwdroneconfig.jammer.title"));
        this.blockPos = blockPos;
        this.whitelistedGamertags = new ArrayList<>(whitelistedGamertags);
        this.rangeSlider = new RangeSlider(0, 0, 0, 0, currentRange);
    }

    public static void open(OpenJammerConfigMessage message) {
        Minecraft.getInstance().setScreen(new JammerScreen(message.blockPos(), message.range(), message.whitelistedGamertags()));
    }

    @Override
    protected void init() {
        super.init();
        int panelWidth = JammerScreenLayout.PANEL_WIDTH;
        int left = (width - panelWidth) / 2;
        int top = JammerScreenLayout.PANEL_TOP;

        rangeSlider = addRenderableWidget(new RangeSlider(
                left + JammerScreenLayout.PADDING,
                top + JammerScreenLayout.RANGE_SLIDER_Y,
                panelWidth - (JammerScreenLayout.PADDING * 2),
                JammerScreenLayout.BUTTON_HEIGHT,
                rangeSlider.getRange()
        ));

        input = new EditBox(
                font,
                left + JammerScreenLayout.PADDING,
                top + JammerScreenLayout.WHITELIST_INPUT_Y,
                panelWidth - (JammerScreenLayout.PADDING * 2),
                JammerScreenLayout.BUTTON_HEIGHT,
                Component.translatable("screen.sbwdroneconfig.jammer.whitelist_input")
        );
        input.setMaxLength(32);
        addRenderableWidget(input);

        addRenderableWidget(Button.builder(Component.translatable("screen.sbwdroneconfig.jammer.whitelist_add"), button -> submitWhitelist(false))
                .pos(left + JammerScreenLayout.PADDING, top + JammerScreenLayout.WHITELIST_BUTTON_Y)
                .size(116, JammerScreenLayout.BUTTON_HEIGHT)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("screen.sbwdroneconfig.jammer.whitelist_remove"), button -> submitWhitelist(true))
                .pos(left + 134, top + JammerScreenLayout.WHITELIST_BUTTON_Y)
                .size(116, JammerScreenLayout.BUTTON_HEIGHT)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("screen.sbwdroneconfig.jammer.save"), button -> saveAndClose())
                .pos(left + JammerScreenLayout.PADDING, top + JammerScreenLayout.SAVE_BUTTON_Y)
                .size(116, JammerScreenLayout.BUTTON_HEIGHT)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .pos(left + 134, top + JammerScreenLayout.SAVE_BUTTON_Y)
                .size(116, JammerScreenLayout.BUTTON_HEIGHT)
                .build());
    }

    private void saveAndClose() {
        AddonNetwork.updateJammerRange(new UpdateJammerRangeMessage(blockPos, rangeSlider.getRange()));
        onClose();
    }

    private void submitWhitelist(boolean remove) {
        String value = input == null ? "" : input.getValue().trim();
        if (value.isEmpty()) {
            return;
        }
        AddonNetwork.updateJammerWhitelist(new UpdateJammerWhitelistMessage(blockPos, value, remove));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (input != null && input.isFocused() && keyCode == 257) {
            submitWhitelist(false);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);

        int panelWidth = JammerScreenLayout.PANEL_WIDTH;
        int panelHeight = JammerScreenLayout.PANEL_HEIGHT;
        int left = (width - panelWidth) / 2;
        int top = JammerScreenLayout.PANEL_TOP;
        guiGraphics.fill(left, top, left + panelWidth, top + panelHeight, 0xD0101010);
        guiGraphics.drawCenteredString(font, title, width / 2, top + JammerScreenLayout.TITLE_Y, 0xFFFFFF);
        guiGraphics.drawString(font, Component.translatable("screen.sbwdroneconfig.jammer.range"), left + JammerScreenLayout.PADDING, top + JammerScreenLayout.RANGE_LABEL_Y, 0xC0C0C0, false);
        guiGraphics.drawString(font, Component.translatable("screen.sbwdroneconfig.jammer.whitelist_input"), left + JammerScreenLayout.PADDING, top + JammerScreenLayout.WHITELIST_INPUT_LABEL_Y, 0xC0C0C0, false);
        guiGraphics.drawString(font, Component.translatable("screen.sbwdroneconfig.jammer.whitelist"), left + JammerScreenLayout.PADDING, top + JammerScreenLayout.WHITELIST_LIST_LABEL_Y, 0xC0C0C0, false);

        int y = top + JammerScreenLayout.WHITELIST_LIST_Y;
        for (int i = 0; i < Math.min(4, whitelistedGamertags.size()); i++) {
            guiGraphics.drawString(font, "- " + whitelistedGamertags.get(i), left + 14, y + i * 11, 0xFFFFFF, false);
        }
        if (whitelistedGamertags.size() > 4) {
            guiGraphics.drawString(font, Component.translatable("screen.sbwdroneconfig.jammer.whitelist_more", whitelistedGamertags.size() - 4), left + 14, y + 44, 0xA0A0A0, false);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class RangeSlider extends AbstractSliderButton {
        private int range;

        private RangeSlider(int x, int y, int width, int height, int currentRange) {
            super(x, y, width, height, Component.empty(), toValue(currentRange));
            this.range = clampRange(currentRange);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("screen.sbwdroneconfig.jammer.range_value", range));
        }

        @Override
        protected void applyValue() {
            this.range = clampRange((int) Math.round(JammerBlockEntity.MIN_RANGE + value * (JammerBlockEntity.MAX_RANGE - JammerBlockEntity.MIN_RANGE)));
            updateMessage();
        }

        public int getRange() {
            return range;
        }

        private static double toValue(int range) {
            int clamped = clampRange(range);
            return (double) (clamped - JammerBlockEntity.MIN_RANGE) / (double) (JammerBlockEntity.MAX_RANGE - JammerBlockEntity.MIN_RANGE);
        }

        private static int clampRange(int range) {
            return Math.max(JammerBlockEntity.MIN_RANGE, Math.min(JammerBlockEntity.MAX_RANGE, range));
        }
    }
}
