package com.lurids.disableportals;

import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;

@Mod(DisablePortals.MODID)
public class DisablePortals {
    public static final String MODID = "portal_disabler";

    public DisablePortals(IEventBus modBus) {
        NeoForge.EVENT_BUS.register(this);
    }

    private static PortalState getState(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(PortalState.factory(), PortalState.NAME);
    }

    public static boolean isPortalDisabled(MinecraftServer server) {
        return server != null && getState(server).isDisabled();
    }

    @SubscribeEvent
    public void onTravel(EntityTravelToDimensionEvent event) {
        if (event.getEntity().level() instanceof ServerLevel sl
                && getState(sl.getServer()).isDisabled()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("disableportals")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> {
                    boolean disabled = getState(ctx.getSource().getServer()).isDisabled();
                    ctx.getSource().sendSuccess(
                        () -> Component.literal(disabled ? "Portals are disabled" : "Portals are enabled"),
                        false
                    );
                    return 1;
                })
                .then(Commands.argument("value", BoolArgumentType.bool())
                    .executes(ctx -> {
                        boolean value = BoolArgumentType.getBool(ctx, "value");
                        MinecraftServer server = ctx.getSource().getServer();
                        getState(server).setDisabled(value);
                        if (value) clearExistingPortals(server);
                        ctx.getSource().sendSuccess(
                            () -> Component.literal(value ? "Portals disabled" : "Portals enabled"),
                            true
                        );
                        return 1;
                    })
                )
        );
    }

    private static void clearExistingPortals(MinecraftServer server) {
        BlockState air = Blocks.AIR.defaultBlockState();
        for (ServerLevel level : server.getAllLevels()) {
            for (ChunkHolder holder : level.getChunkSource().chunkMap.getChunks()) {
                LevelChunk chunk = holder.getTickingChunk();
                if (chunk == null) continue;
                LevelChunkSection[] sections = chunk.getSections();
                int chunkMinX = chunk.getPos().getMinBlockX();
                int chunkMinZ = chunk.getPos().getMinBlockZ();
                int worldMinY = level.getMinBuildHeight();
                for (int s = 0; s < sections.length; s++) {
                    LevelChunkSection section = sections[s];
                    if (section == null || section.hasOnlyAir()) continue;
                    if (!section.maybeHas(st -> st.is(Blocks.NETHER_PORTAL) || st.is(Blocks.END_PORTAL))) continue;
                    int sectionMinY = worldMinY + s * 16;
                    for (int x = 0; x < 16; x++) {
                        for (int y = 0; y < 16; y++) {
                            for (int z = 0; z < 16; z++) {
                                BlockState st = section.getBlockState(x, y, z);
                                if (st.is(Blocks.NETHER_PORTAL) || st.is(Blocks.END_PORTAL)) {
                                    level.setBlock(new BlockPos(chunkMinX + x, sectionMinY + y, chunkMinZ + z), air, 3);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
