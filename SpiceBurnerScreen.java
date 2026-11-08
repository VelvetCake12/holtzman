package dune.holtzman;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class SpiceBurnerScreen extends AbstractContainerScreen<SpiceBurnerMenu> {
    static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(CreateHeat.MODID, "textures/gui/spice_burner.png");

    public SpiceBurnerScreen(SpiceBurnerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 133;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        g.blit(TEX, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, titleLabelX, titleLabelY, 0xD8D0C8, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xB8B0A8, false);
        int s = menu.burn() / 20;
        String t = menu.burn() > 0 ? "Heat: " + (s / 60) + "m " + (s % 60) + "s" : "Cold. Feed it Melange.";
        g.drawString(font, t, 104, 26, menu.burn() > 0 ? 0xE0873A : 0x9A9088, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
    }
}
