package com.lurids.disableportals.mixin;

import com.lurids.disableportals.DisablePortals;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.EnderEyeItem;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EnderEyeItem.class)
public abstract class EnderEyeItemMixin {
    @Redirect(
        method = "useOn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/pattern/BlockPattern;find(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/pattern/BlockPattern$BlockPatternMatch;"
        )
    )
    private BlockPattern.BlockPatternMatch disableportals$skipPortalOpen(BlockPattern instance, LevelReader level, BlockPos pos) {
        if (level instanceof ServerLevel sl && DisablePortals.isEndDisabled(sl.getServer())) {
            return null;
        }
        return instance.find(level, pos);
    }
}
