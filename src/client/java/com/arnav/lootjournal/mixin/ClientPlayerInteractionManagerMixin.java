package com.arnav.lootjournal.mixin;

import com.arnav.lootjournal.session.SessionTracker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
    @Unique private BlockState lootjournal$pendingBlock;
    @Unique private BlockPos lootjournal$pendingPos;

    @Inject(
        method = "breakBlock(Lnet/minecraft/util/math/BlockPos;)Z",
        at = @At("HEAD")
    )
    private void lootjournal$capturePre(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) {
            this.lootjournal$pendingBlock = client.world.getBlockState(pos);
            this.lootjournal$pendingPos = pos.toImmutable();
        }
    }

    @Inject(
        method = "breakBlock(Lnet/minecraft/util/math/BlockPos;)Z",
        at = @At("TAIL")
    )
    private void lootjournal$capturePost(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue()) && this.lootjournal$pendingBlock != null) {
            MinecraftClient client = MinecraftClient.getInstance();
            long tick = client.world != null ? client.world.getTime() : 0L;
            String blockId = Registries.BLOCK.getId(this.lootjournal$pendingBlock.getBlock()).toString();
            BlockPos p = this.lootjournal$pendingPos;
            SessionTracker.onBlockBroken(blockId, p.getX(), p.getY(), p.getZ(), tick);
        }
        this.lootjournal$pendingBlock = null;
        this.lootjournal$pendingPos = null;
    }
}
