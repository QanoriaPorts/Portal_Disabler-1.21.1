package com.lurids.disableportals;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.portal.PortalShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.function.Predicate;

@Mod(DisablePortals.MODID)
public class DisablePortals {
    public static final String MODID = "portal_disabler";

    public DisablePortals(IEventBus modBus) {
        NeoForge.EVENT_BUS.register(this);
    }

    private static PortalState getState(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(PortalState.factory(), PortalState.NAME);
    }

    public static boolean isNetherDisabled(MinecraftServer server) {
        return server != null && getState(server).isNetherDisabled();
    }

    public static boolean isEndDisabled(MinecraftServer server) {
        return server != null && getState(server).isEndDisabled();
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;
        if (!(level instanceof ServerLevel sl)) return;
        if (!getState(sl.getServer()).isNetherDisabled()) return;

        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.FLINT_AND_STEEL) && !stack.is(Items.FIRE_CHARGE)) return;

        BlockState clicked = level.getBlockState(event.getPos());
        if (!clicked.is(Blocks.OBSIDIAN)) return;

        BlockPos firePos = event.getPos().relative(event.getFace());
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (level.getBlockState(firePos.offset(dx, dy, dz)).is(Blocks.FIRE)) {
                        event.setCanceled(true);
                        return;
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onTravel(EntityTravelToDimensionEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel sl)) return;
        PortalState state = getState(sl.getServer());
        ResourceKey<Level> target = event.getDimension();
        if (target == Level.NETHER && state.isNetherDisabled()) {
            event.setCanceled(true);
        } else if (target == Level.END && state.isEndDisabled()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("disable")
            .requires(s -> s.hasPermission(2))
            .executes(this::showStatus)
            .then(Commands.literal("nether")
                .then(Commands.argument("value", BoolArgumentType.bool())
                    .executes(ctx -> setNether(ctx, BoolArgumentType.getBool(ctx, "value")))))
            .then(Commands.literal("end")
                .then(Commands.argument("value", BoolArgumentType.bool())
                    .executes(ctx -> setEnd(ctx, BoolArgumentType.getBool(ctx, "value")))))
            .then(Commands.literal("all")
                .then(Commands.argument("value", BoolArgumentType.bool())
                    .executes(ctx -> setAll(ctx, BoolArgumentType.getBool(ctx, "value")))));
        event.getDispatcher().register(root);
    }

    private int showStatus(CommandContext<CommandSourceStack> ctx) {
        PortalState state = getState(ctx.getSource().getServer());
        boolean nether = state.isNetherDisabled();
        boolean end = state.isEndDisabled();
        ctx.getSource().sendSuccess(() -> Component.literal(
            "Nether portals are " + (nether ? "disabled" : "enabled") + "\n"
            + "End portals are " + (end ? "disabled" : "enabled")
        ), false);
        return 1;
    }

    private int setNether(CommandContext<CommandSourceStack> ctx, boolean value) {
        MinecraftServer server = ctx.getSource().getServer();
        getState(server).setNetherDisabled(value);
        if (value) {
            clearPortals(server, st -> st.is(Blocks.NETHER_PORTAL));
        } else {
            clearStuckNetherFires(server);
        }
        ctx.getSource().sendSuccess(
            () -> Component.literal(value ? "Nether portals disabled" : "Nether portals enabled"),
            true);
        return 1;
    }

    private int setEnd(CommandContext<CommandSourceStack> ctx, boolean value) {
        MinecraftServer server = ctx.getSource().getServer();
        getState(server).setEndDisabled(value);
        if (value) clearPortals(server, st -> st.is(Blocks.END_PORTAL));
        ctx.getSource().sendSuccess(
            () -> Component.literal(value ? "End portals disabled" : "End portals enabled"),
            true);
        return 1;
    }

    private int setAll(CommandContext<CommandSourceStack> ctx, boolean value) {
        MinecraftServer server = ctx.getSource().getServer();
        PortalState state = getState(server);
        state.setNetherDisabled(value);
        state.setEndDisabled(value);
        if (value) {
            clearPortals(server, st -> st.is(Blocks.NETHER_PORTAL) || st.is(Blocks.END_PORTAL));
        } else {
            clearStuckNetherFires(server);
        }
        ctx.getSource().sendSuccess(
            () -> Component.literal(value ? "All portals disabled" : "All portals enabled"),
            true);
        return 1;
    }

    private static void clearStuckNetherFires(MinecraftServer server) {
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
                    if (!section.maybeHas(st -> st.is(Blocks.FIRE))) continue;
                    int sectionMinY = worldMinY + s * 16;
                    for (int x = 0; x < 16; x++) {
                        for (int y = 0; y < 16; y++) {
                            for (int z = 0; z < 16; z++) {
                                BlockState st = section.getBlockState(x, y, z);
                                if (st.is(Blocks.FIRE)) {
                                    BlockPos pos = new BlockPos(chunkMinX + x, sectionMinY + y, chunkMinZ + z);
                                    if (PortalShape.findEmptyPortalShape(level, pos, Direction.Axis.X).isPresent()) {
                                        level.setBlock(pos, air, 3);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void clearPortals(MinecraftServer server, Predicate<BlockState> match) {
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
                    if (!section.maybeHas(match)) continue;
                    int sectionMinY = worldMinY + s * 16;
                    for (int x = 0; x < 16; x++) {
                        for (int y = 0; y < 16; y++) {
                            for (int z = 0; z < 16; z++) {
                                BlockState st = section.getBlockState(x, y, z);
                                if (match.test(st)) {
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
