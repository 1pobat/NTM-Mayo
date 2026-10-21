package dev.hbmmayo.neoforge;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** Loaded exclusively on the physical client; server runtime never references this class. */
@Mod(value=HbmMayonnaise.ID,dist=Dist.CLIENT)
public final class MayoClient {
    public MayoClient() {
        NeoForge.EVENT_BUS.addListener(MayoClient::render);
        NeoForge.EVENT_BUS.addListener(MayoClient::disconnect);
    }
    private static void disconnect(ClientPlayerNetworkEvent.LoggingOut event) { ClientData.clear(); }
    private static void render(RenderGuiLayerEvent.Post event) {
        if(!event.getName().equals(VanillaGuiLayers.HOTBAR)) return;
        var mc=Minecraft.getInstance();var data=ClientData.latest();
        if(mc.player==null || data==null || !data.active() || !data.owner().equals(mc.player.getUUID())) return;
        var graphics=event.getGuiGraphics();
        int x=HbmMayonnaise.hudX,y=HbmMayonnaise.hudY;
        String value=String.format(Locale.ROOT,"%.1f",data.instability());
        var text=Component.translatable("gui.hbmmayo.instability",value);
        int width=Math.max(132,mc.font.width(text)+6);
        graphics.fill(x,y,x+width,y+19,0xb0202020);
        int color=data.instability()>=75?0xffff6b35:0xffffcf41;
        graphics.text(mc.font,text,x+3,y+2,color,true);
        graphics.fill(x+3,y+14,x+width-3,y+17,0xff474747);
        graphics.fill(x+3,y+14,x+3+(int)Math.round((width-6)*data.instability()/100),y+17,color);
    }
}
