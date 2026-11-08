package dune.holtzman;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** active: field is up. flashRgb: 0 = no flash, else colour of a flash that fades over flashTicks. */
public record FxPayload(int entityId, boolean active, int fieldRgb, int flashRgb, int flashTicks) implements CustomPacketPayload {
    public static final Type<FxPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateHeat.MODID, "fx"));
    public static final StreamCodec<ByteBuf, FxPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FxPayload::entityId,
            ByteBufCodecs.BOOL, FxPayload::active,
            ByteBufCodecs.INT, FxPayload::fieldRgb,
            ByteBufCodecs.INT, FxPayload::flashRgb,
            ByteBufCodecs.VAR_INT, FxPayload::flashTicks,
            FxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
