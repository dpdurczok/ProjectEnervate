package com.D3D.projectenervate.emc;

import com.D3D.projectenervate.ProjectEnervateConfig;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

public final class ProjectECollectorDisabler {
    private ProjectECollectorDisabler() {
    }

    public static boolean shouldDisableCollectors() {
        return ProjectEnervateConfig.disableCollectors();
    }

    public static boolean isCollectorStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());

        if (id == null || !"projecte".equals(id.getNamespace())) {
            return false;
        }

        String path = id.getPath();
        return "collector_mk1".equals(path)
                || "collector_mk2".equals(path)
                || "collector_mk3".equals(path);
    }

    public static boolean isCollectorRecipe(RecipeHolder<?> holder, HolderLookup.Provider registryAccess) {
        if (holder == null) {
            return false;
        }

        ResourceLocation id = holder.id();
        if (id != null && "projecte".equals(id.getNamespace())) {
            String path = id.getPath();
            if (path.equals("collector_mk1")
                    || path.equals("collector_mk2")
                    || path.equals("collector_mk3")
                    || path.endsWith("/collector_mk1")
                    || path.endsWith("/collector_mk2")
                    || path.endsWith("/collector_mk3")) {
                return true;
            }
        }

        try {
            Recipe<?> recipe = holder.value();
            ItemStack result = recipe.getResultItem(registryAccess);
            return isCollectorStack(result);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public static Optional<RecipeHolder<?>> filterOptional(
            Optional<? extends RecipeHolder<?>> optional,
            HolderLookup.Provider registryAccess
    ) {
        if (!shouldDisableCollectors() || optional.isEmpty()) {
            @SuppressWarnings("unchecked")
            Optional<RecipeHolder<?>> cast = (Optional<RecipeHolder<?>>) optional;
            return cast;
        }

        RecipeHolder<?> holder = optional.get();
        return isCollectorRecipe(holder, registryAccess) ? Optional.empty() : Optional.of(holder);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static List filterList(List recipes, HolderLookup.Provider registryAccess) {
        if (!shouldDisableCollectors() || recipes == null || recipes.isEmpty()) {
            return recipes;
        }

        return (List) recipes.stream()
                .filter(recipe -> recipe instanceof RecipeHolder<?> holder && !isCollectorRecipe(holder, registryAccess))
                .toList();
    }

    public static Collection<RecipeHolder<?>> filterCollection(
            Collection<RecipeHolder<?>> recipes,
            HolderLookup.Provider registryAccess
    ) {
        if (!shouldDisableCollectors() || recipes == null || recipes.isEmpty()) {
            return recipes;
        }

        return recipes.stream()
                .filter(recipe -> !isCollectorRecipe(recipe, registryAccess))
                .toList();
    }
}
