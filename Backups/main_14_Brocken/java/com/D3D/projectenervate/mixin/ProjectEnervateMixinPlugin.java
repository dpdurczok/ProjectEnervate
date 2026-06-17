package com.D3D.projectenervate.mixin;

import java.util.List;
import java.util.Set;
import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class ProjectEnervateMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("MouseTweaksMainMixin")) {
            return isMouseTweaksLoaded();
        }

        if (mixinClassName.contains("Create") || mixinClassName.endsWith("BlockGetDropsCreateEmcMixin")) {
            return isCreateLoaded();
        }

        return true;
    }

    private static boolean isMouseTweaksLoaded() {
        try {
            return LoadingModList.get().getModFileById("mousetweaks") != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isCreateLoaded() {
        try {
            return LoadingModList.get().getModFileById("create") != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}