package com.lurids.disableportals;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

public class PortalState extends SavedData {
    public static final String NAME = DisablePortals.MODID;

    private boolean netherDisabled = false;
    private boolean endDisabled = false;

    public boolean isNetherDisabled() {
        return netherDisabled;
    }

    public boolean isEndDisabled() {
        return endDisabled;
    }

    public void setNetherDisabled(boolean value) {
        if (this.netherDisabled != value) {
            this.netherDisabled = value;
            setDirty();
        }
    }

    public void setEndDisabled(boolean value) {
        if (this.endDisabled != value) {
            this.endDisabled = value;
            setDirty();
        }
    }

    public static PortalState load(CompoundTag tag, HolderLookup.Provider provider) {
        PortalState state = new PortalState();
        state.netherDisabled = tag.getBoolean("netherDisabled");
        state.endDisabled = tag.getBoolean("endDisabled");
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putBoolean("netherDisabled", netherDisabled);
        tag.putBoolean("endDisabled", endDisabled);
        return tag;
    }

    public static SavedData.Factory<PortalState> factory() {
        return new SavedData.Factory<>(PortalState::new, PortalState::load, null);
    }
}
