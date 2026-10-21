package dev.hbmmayo.neoforge;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Clientbound only; clients cannot request instability or infusion changes. */
public record MayoPayload(UUID owner,boolean active,double instability,long revision) implements CustomPacketPayload {
    public static final Type<MayoPayload> TYPE=new Type<>(MayoRegistry.id("infusion"));
    public static final StreamCodec<RegistryFriendlyByteBuf,MayoPayload> CODEC=StreamCodec.of(
        (buf,p)->{ buf.writeLong(p.owner.getMostSignificantBits());buf.writeLong(p.owner.getLeastSignificantBits());
            buf.writeBoolean(p.active);buf.writeDouble(p.instability);buf.writeLong(p.revision); },
        buf->new MayoPayload(new UUID(buf.readLong(),buf.readLong()),buf.readBoolean(),buf.readDouble(),buf.readLong()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE,CODEC,(payload,context)->
            context.enqueueWork(()->ClientData.accept(payload)));
    }
}
