package com.lurids.disableportals;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

public class PortalState extends SavedData {
    public static final String NAME = DisablePortals.MODID;

    private boolean disabled = false;

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean value) {
        if (this.disabled != value) {
            this.disabled = value;
            setDirty();
        }
    }

    public static PortalState load(CompoundTag tag, HolderLookup.Provider provider) {
        PortalState state = new PortalState();
        state.disabled = tag.getBoolean("disabled");
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putBoolean("disabled", disabled);
        return tag;
    }

    public static SavedData.Factory<PortalState> factory() {
        return new SavedData.Factory<>(PortalState::new, PortalState::load, null);
    }
}
