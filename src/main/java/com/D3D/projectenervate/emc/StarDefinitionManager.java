package com.D3D.projectenervate.emc;

import com.D3D.projectenervate.ProjectEnervateConfig;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class StarDefinitionManager {
    private static final String ENTRY_SEPARATOR = "|";
    private static final String RESOURCE_SEPARATOR = ",";

    private StarDefinitionManager() {
    }

    public static List<StarDefinition> configuredStars() {
        List<String> entries = ProjectEnervateConfig.stars();
        List<StarDefinition> result = new ArrayList<>();

        for (int i = 0; i < entries.size(); i++) {
            Optional<StarDefinition> parsed = parseDefinition(i, entries.get(i));
            parsed.ifPresent(result::add);
        }

        return result;
    }

    public static List<ActiveStar> activeStars() {
        List<ActiveStar> result = new ArrayList<>();

        for (StarDefinition definition : configuredStars()) {
            List<String> activeResources = new ArrayList<>();

            for (String resource : definition.resources()) {
                if (isSelectorActive(resource)) {
                    activeResources.add(resource);
                }
            }

            if (!activeResources.isEmpty()) {
                result.add(new ActiveStar(
                        definition.index(),
                        definition.name(),
                        definition.resources(),
                        List.copyOf(activeResources)
                ));
            }
        }

        return result;
    }

    public static ActiveStar findActiveStarForStack(ItemStack stack, ResourceLocation canonicalResource) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        for (ActiveStar star : activeStars()) {
            for (String resource : star.configuredResources()) {
                if (selectorMatchesStack(resource, stack, canonicalResource)) {
                    return star;
                }
            }
        }

        return null;
    }

    public static List<String> configuredStarNames() {
        List<String> names = new ArrayList<>();

        for (StarDefinition star : configuredStars()) {
            names.add(star.name());
        }

        return names;
    }

    public static StarDefinition findConfiguredStar(String name) {
        String normalized = normalizeName(name);

        for (StarDefinition star : configuredStars()) {
            if (normalizeName(star.name()).equals(normalized)) {
                return star;
            }
        }

        return null;
    }

    public static boolean createStar(String rawName) {
        String name = cleanName(rawName);

        if (name.isBlank() || findConfiguredStar(name) != null) {
            return false;
        }

        List<String> entries = ProjectEnervateConfig.stars();
        entries.add(encodeDefinition(name, List.of()));
        ProjectEnervateConfig.setStars(entries);
        return true;
    }

    public static AddResourceResult addItemResource(String starName, ResourceLocation itemId) {
        if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
            return AddResourceResult.INVALID_ITEM;
        }

        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == Items.AIR) {
            return AddResourceResult.INVALID_ITEM;
        }

        if (AdaptiveEmcOutputHelper.getBaseSingleEmc(new ItemStack(item)).signum() <= 0) {
            return AddResourceResult.NO_BASE_EMC;
        }

        String normalizedStarName = normalizeName(starName);
        List<String> entries = ProjectEnervateConfig.stars();
        boolean changed = false;
        boolean found = false;

        for (int i = 0; i < entries.size(); i++) {
            Optional<StarDefinition> parsed = parseDefinition(i, entries.get(i));
            if (parsed.isEmpty()) {
                continue;
            }

            StarDefinition star = parsed.get();
            if (!normalizeName(star.name()).equals(normalizedStarName)) {
                continue;
            }

            found = true;
            List<String> resources = new ArrayList<>(star.resources());
            String itemText = itemId.toString();

            for (String existing : resources) {
                if (existing.equalsIgnoreCase(itemText)) {
                    return AddResourceResult.ALREADY_EXISTS;
                }
            }

            resources.add(itemText);
            entries.set(i, encodeDefinition(star.name(), resources));
            changed = true;
            break;
        }

        if (!found) {
            return AddResourceResult.NO_STAR;
        }

        if (changed) {
            ProjectEnervateConfig.setStars(entries);
        }

        return AddResourceResult.ADDED;
    }

    public static RemoveResourceResult removeResource(String starName, int oneBasedResourceIndex) {
        if (oneBasedResourceIndex <= 0) {
            return RemoveResourceResult.INVALID_INDEX;
        }

        String normalizedStarName = normalizeName(starName);
        List<String> entries = ProjectEnervateConfig.stars();
        boolean found = false;

        for (int i = 0; i < entries.size(); i++) {
            Optional<StarDefinition> parsed = parseDefinition(i, entries.get(i));
            if (parsed.isEmpty()) {
                continue;
            }

            StarDefinition star = parsed.get();
            if (!normalizeName(star.name()).equals(normalizedStarName)) {
                continue;
            }

            found = true;
            List<String> resources = new ArrayList<>(star.resources());
            int zeroBasedIndex = oneBasedResourceIndex - 1;

            if (zeroBasedIndex < 0 || zeroBasedIndex >= resources.size()) {
                return RemoveResourceResult.INVALID_INDEX;
            }

            resources.remove(zeroBasedIndex);
            entries.set(i, encodeDefinition(star.name(), resources));
            ProjectEnervateConfig.setStars(entries);
            return RemoveResourceResult.REMOVED;
        }

        return found ? RemoveResourceResult.INVALID_INDEX : RemoveResourceResult.NO_STAR;
    }

    public static String describeSelectorState(String rawSelector) {
        String cleaned = cleanResource(rawSelector);

        if (cleaned.isBlank()) {
            return "blank";
        }

        Selector selector = parseSelector(cleaned);
        if (selector == null) {
            return "invalid";
        }

        if (selector.tag()) {
            return isSelectorActive(cleaned) ? "active tag" : "inactive or missing tag";
        }

        if (!BuiltInRegistries.ITEM.containsKey(selector.id())) {
            return "missing item";
        }

        Item item = BuiltInRegistries.ITEM.get(selector.id());
        if (item == Items.AIR) {
            return "missing item";
        }

        BigDecimal baseEmc = AdaptiveEmcOutputHelper.getBaseSingleEmc(new ItemStack(item));
        return baseEmc.signum() > 0 ? "active item" : "no base EMC";
    }

    public static List<ResourceLocation> activeConcreteItemResources(ActiveStar star) {
        if (star == null) {
            return List.of();
        }

        Set<ResourceLocation> result = new LinkedHashSet<>();

        for (String resource : star.activeResources()) {
            Selector selector = parseSelector(resource);
            if (selector == null) {
                continue;
            }

            if (selector.tag()) {
                TagKey<Item> tag = TagKey.create(Registries.ITEM, selector.id());
                for (Item item : BuiltInRegistries.ITEM) {
                    if (item == Items.AIR) {
                        continue;
                    }

                    ItemStack stack = new ItemStack(item);
                    if (stack.is(tag) && AdaptiveEmcOutputHelper.getBaseSingleEmc(stack).signum() > 0) {
                        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                        if (id != null) {
                            result.add(id);
                        }
                    }
                }
            } else if (BuiltInRegistries.ITEM.containsKey(selector.id())) {
                Item item = BuiltInRegistries.ITEM.get(selector.id());
                if (item != Items.AIR && AdaptiveEmcOutputHelper.getBaseSingleEmc(new ItemStack(item)).signum() > 0) {
                    result.add(selector.id());
                }
            }
        }

        return List.copyOf(result);
    }

    private static Optional<StarDefinition> parseDefinition(int index, String rawEntry) {
        if (rawEntry == null || rawEntry.isBlank()) {
            return Optional.empty();
        }

        String[] parts = rawEntry.split("\\|", 2);
        String name = cleanName(parts[0]);

        if (name.isBlank()) {
            return Optional.empty();
        }

        List<String> resources = new ArrayList<>();
        if (parts.length > 1) {
            String[] rawResources = parts[1].split(RESOURCE_SEPARATOR);
            for (String rawResource : rawResources) {
                String resource = cleanResource(rawResource);
                if (!resource.isBlank()) {
                    resources.add(resource);
                }
            }
        }

        return Optional.of(new StarDefinition(index, name, List.copyOf(resources)));
    }

    private static boolean isSelectorActive(String rawSelector) {
        Selector selector = parseSelector(rawSelector);

        if (selector == null) {
            return false;
        }

        if (selector.tag()) {
            TagKey<Item> tag = TagKey.create(Registries.ITEM, selector.id());

            for (Item item : BuiltInRegistries.ITEM) {
                if (item == Items.AIR) {
                    continue;
                }

                ItemStack stack = new ItemStack(item);
                if (stack.is(tag) && AdaptiveEmcOutputHelper.getBaseSingleEmc(stack).signum() > 0) {
                    return true;
                }
            }

            return false;
        }

        if (!BuiltInRegistries.ITEM.containsKey(selector.id())) {
            return false;
        }

        Item item = BuiltInRegistries.ITEM.get(selector.id());
        return item != Items.AIR && AdaptiveEmcOutputHelper.getBaseSingleEmc(new ItemStack(item)).signum() > 0;
    }

    private static boolean selectorMatchesStack(String rawSelector, ItemStack stack, ResourceLocation canonicalResource) {
        Selector selector = parseSelector(rawSelector);

        if (selector == null || stack == null || stack.isEmpty()) {
            return false;
        }

        if (selector.tag()) {
            TagKey<Item> tag = TagKey.create(Registries.ITEM, selector.id());
            if (stack.is(tag)) {
                return true;
            }

            ItemStack canonicalStack = stackForItemId(canonicalResource);
            return canonicalStack != null && canonicalStack.is(tag);
        }

        ResourceLocation stackId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return selector.id().equals(stackId) || selector.id().equals(canonicalResource);
    }

    private static ItemStack stackForItemId(ResourceLocation id) {
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return null;
        }

        Item item = BuiltInRegistries.ITEM.get(id);
        return item == Items.AIR ? null : new ItemStack(item);
    }

    private static Selector parseSelector(String rawSelector) {
        String cleaned = cleanResource(rawSelector);
        if (cleaned.isBlank()) {
            return null;
        }

        boolean tag = cleaned.startsWith("#");
        String idText = tag ? cleaned.substring(1) : cleaned;
        ResourceLocation id = ResourceLocation.tryParse(idText);
        return id == null ? null : new Selector(tag, id);
    }

    private static String encodeDefinition(String name, List<String> resources) {
        return cleanName(name) + ENTRY_SEPARATOR + String.join(RESOURCE_SEPARATOR, cleanResources(resources));
    }

    private static List<String> cleanResources(List<String> resources) {
        if (resources == null || resources.isEmpty()) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        for (String resource : resources) {
            String cleaned = cleanResource(resource);
            if (!cleaned.isBlank()) {
                result.add(cleaned);
            }
        }

        return result;
    }

    private static String cleanName(String value) {
        if (value == null) {
            return "";
        }

        return value.replace('|', ' ')
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace('\t', ' ')
                .trim();
    }

    private static String cleanResource(String value) {
        if (value == null) {
            return "";
        }

        return value.replace('|', ' ')
                .replace(',', ' ')
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace('\t', ' ')
                .trim();
    }

    private static String normalizeName(String value) {
        return cleanName(value).toLowerCase(Locale.ROOT);
    }

    public enum AddResourceResult {
        ADDED,
        NO_STAR,
        INVALID_ITEM,
        NO_BASE_EMC,
        ALREADY_EXISTS
    }

    public enum RemoveResourceResult {
        REMOVED,
        NO_STAR,
        INVALID_INDEX
    }

    private record Selector(boolean tag, ResourceLocation id) {
    }

    public record StarDefinition(
            int index,
            String name,
            List<String> resources
    ) {
    }

    public record ActiveStar(
            int index,
            String name,
            List<String> configuredResources,
            List<String> activeResources
    ) {
        public String primaryResourceLabel() {
            return activeResources.isEmpty() ? "" : activeResources.get(0);
        }

        public List<String> safeConfiguredResources() {
            return configuredResources == null ? Collections.emptyList() : configuredResources;
        }
    }
}
