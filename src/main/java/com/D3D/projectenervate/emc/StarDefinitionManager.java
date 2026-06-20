package com.D3D.projectenervate.emc;

import com.D3D.projectenervate.ProjectEnervateConfig;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    private static final Object CACHE_LOCK = new Object();
    private static volatile StarCache cachedStarData;

    private StarDefinitionManager() {
    }

    public static void invalidateCaches() {
        synchronized (CACHE_LOCK) {
            cachedStarData = null;
        }
    }

    public static List<StarDefinition> configuredStars() {
        return cache().configuredStars();
    }

    public static List<ActiveStar> activeStars() {
        return cache().activeStars();
    }

    public static ActiveStar findActiveStarForStack(ItemStack stack, ResourceLocation canonicalResource) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        StarCache cache = cache();
        ResourceLocation stackId = BuiltInRegistries.ITEM.getKey(stack.getItem());

        if (stackId != null) {
            ActiveStar stackStar = cache.itemToActiveStar().get(stackId);
            if (stackStar != null) {
                return stackStar;
            }
        }

        return canonicalResource == null ? null : cache.itemToActiveStar().get(canonicalResource);
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

        return cache().activeConcreteItemsByStarIndex().getOrDefault(star.index(), List.of());
    }

    private static StarCache cache() {
        List<String> entries = ProjectEnervateConfig.stars();
        long revision = ProjectEnervateConfig.starsRevision();
        StarCache localCache = cachedStarData;

        if (localCache != null && localCache.revision() == revision) {
            return localCache;
        }

        synchronized (CACHE_LOCK) {
            localCache = cachedStarData;
            if (localCache != null && localCache.revision() == revision) {
                return localCache;
            }

            StarCache rebuilt = buildCache(revision, entries);
            cachedStarData = rebuilt;
            return rebuilt;
        }
    }

    private static StarCache buildCache(long revision, List<String> entries) {
        List<StarDefinition> configured = new ArrayList<>();

        for (int i = 0; i < entries.size(); i++) {
            Optional<StarDefinition> parsed = parseDefinition(i, entries.get(i));
            parsed.ifPresent(configured::add);
        }

        List<ActiveStar> activeStars = new ArrayList<>();
        Map<ResourceLocation, ActiveStar> itemToActiveStar = new LinkedHashMap<>();
        Map<Integer, List<ResourceLocation>> activeConcreteItemsByStarIndex = new LinkedHashMap<>();
        Map<String, Boolean> selectorActiveState = new HashMap<>();

        for (StarDefinition definition : configured) {
            List<String> activeResources = new ArrayList<>();
            Set<ResourceLocation> concreteItems = new LinkedHashSet<>();

            for (String resource : definition.resources()) {
                String cleaned = cleanResource(resource);
                Set<ResourceLocation> matches = concreteItemsForSelector(cleaned);
                boolean active = !matches.isEmpty();
                selectorActiveState.put(cleaned, active);

                if (active) {
                    activeResources.add(cleaned);
                    concreteItems.addAll(matches);
                }
            }

            if (!activeResources.isEmpty()) {
                ActiveStar activeStar = new ActiveStar(
                        definition.index(),
                        definition.name(),
                        definition.resources(),
                        List.copyOf(activeResources)
                );
                activeStars.add(activeStar);
                activeConcreteItemsByStarIndex.put(definition.index(), List.copyOf(concreteItems));

                for (ResourceLocation itemId : concreteItems) {
                    itemToActiveStar.putIfAbsent(itemId, activeStar);
                }
            }
        }

        return new StarCache(
                revision,
                List.copyOf(configured),
                List.copyOf(activeStars),
                Map.copyOf(itemToActiveStar),
                copyConcreteMap(activeConcreteItemsByStarIndex),
                Map.copyOf(selectorActiveState)
        );
    }

    private static Map<Integer, List<ResourceLocation>> copyConcreteMap(Map<Integer, List<ResourceLocation>> source) {
        Map<Integer, List<ResourceLocation>> result = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<ResourceLocation>> entry : source.entrySet()) {
            result.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Map.copyOf(result);
    }

    private static Set<ResourceLocation> concreteItemsForSelector(String rawSelector) {
        Selector selector = parseSelector(rawSelector);

        if (selector == null) {
            return Set.of();
        }

        Set<ResourceLocation> result = new LinkedHashSet<>();

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

            return result;
        }

        if (!BuiltInRegistries.ITEM.containsKey(selector.id())) {
            return Set.of();
        }

        Item item = BuiltInRegistries.ITEM.get(selector.id());
        if (item != Items.AIR && AdaptiveEmcOutputHelper.getBaseSingleEmc(new ItemStack(item)).signum() > 0) {
            result.add(selector.id());
        }

        return result;
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
        String cleaned = cleanResource(rawSelector);
        return cache().selectorActiveState().getOrDefault(cleaned, false);
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

    private record StarCache(
            long revision,
            List<StarDefinition> configuredStars,
            List<ActiveStar> activeStars,
            Map<ResourceLocation, ActiveStar> itemToActiveStar,
            Map<Integer, List<ResourceLocation>> activeConcreteItemsByStarIndex,
            Map<String, Boolean> selectorActiveState
    ) {
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
