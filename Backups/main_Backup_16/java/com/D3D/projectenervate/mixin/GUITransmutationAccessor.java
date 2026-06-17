package com.D3D.projectenervate.mixin;

import moze_intel.projecte.gameObjs.gui.GUITransmutation;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = GUITransmutation.class, remap = false)
public interface GUITransmutationAccessor {

    @Accessor("textBoxFilter")
    EditBox projectenervate$getTextBoxFilter();
}
