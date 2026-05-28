package com.arnav.lootjournal.mixin;

import com.arnav.lootjournal.LootJournalSession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
    @Unique private BlockState lootjournal$pendingBlock;
    @Unique private BlockPos   lootjournal$pendingPos;

    @Inject(method = "destroyBlock(Lnet/minecraft/core/BlockPos;)Z", at = @At("HEAD"))
    private void lootjournal$capturePre(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            this.lootjournal$pendingBlock = mc.level.getBlockState(pos);
            this.lootjournal$pendingPos   = pos.immutable();
        }
    }

    @Inject(method = "destroyBlock(Lnet/minecraft/core/BlockPos;)Z", at = @At("TAIL"))
    private void lootjournal$capturePost(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue()) && this.lootjournal$pendingBlock != null) {
            Minecraft mc  = Minecraft.getInstance();
            long tick     = mc.level != null ? mc.level.getGameTime() : 0L;
            String blockId = BuiltInRegistries.BLOCK.getKey(this.lootjournal$pendingBlock.getBlock()).toString();
            BlockPos p    = this.lootjournal$pendingPos;
            LootJournalSession.onBlockBroken(blockId, p.getX(), p.getY(), p.getZ(), tick);
        }
        this.lootjournal$pendingBlock = null;
        this.lootjournal$pendingPos   = null;
    }
}
