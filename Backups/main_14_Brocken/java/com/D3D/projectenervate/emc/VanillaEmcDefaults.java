package com.D3D.projectenervate.emc;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import moze_intel.projecte.api.ItemInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class VanillaEmcDefaults {
    private VanillaEmcDefaults() {
    }

    public static void apply(Object2LongMap<ItemInfo> data) {
        if (data == null) {
            return;
        }

        long coal = valueOf(data, "coal", 128);
        long charcoal = valueOf(data, "charcoal", coal);
        long rawIron = valueOf(data, "raw_iron", 256);
        long rawGold = valueOf(data, "raw_gold", 2048);
        long rawCopper = valueOf(data, "raw_copper", 85);
        long goldNugget = valueOf(data, "gold_nugget", Math.max(1, Math.round(rawGold / 9.0F)));
        long redstone = valueOf(data, "redstone", 64);
        long lapis = valueOf(data, "lapis_lazuli", 864);
        long diamond = valueOf(data, "diamond", 8192);
        long emerald = valueOf(data, "emerald", 16384);
        long quartz = valueOf(data, "quartz", 256);
        long amethyst = valueOf(data, "amethyst_shard", 512);
        long netheriteScrap = valueOf(data, "netherite_scrap", diamond * 4);
        long ironIngot = valueOf(data, "iron_ingot", 256);
        long copperIngot = valueOf(data, "copper_ingot", rawCopper);
        long goldIngot = valueOf(data, "gold_ingot", rawGold);
        long stick = valueOf(data, "stick", 4);
        long bone = valueOf(data, "bone", 144);
        long string = valueOf(data, "string", 12);
        long paper = valueOf(data, "paper", 32);
        long leather = valueOf(data, "leather", 64);
        long prismarineShard = valueOf(data, "prismarine_shard", 128);
        long prismarineCrystals = valueOf(data, "prismarine_crystals", 256);
        long echoShard = valueOf(data, "echo_shard", 4096);
        long netherrack = valueOf(data, "netherrack", 1);
        long cobblestone = valueOf(data, "cobblestone", 1);
        long deepslate = valueOf(data, "deepslate", 1);
        long stone = valueOf(data, "stone", 1);
        long sandstone = valueOf(data, "sandstone", 4);
        long blackstone = valueOf(data, "blackstone", 1);
        long endStone = valueOf(data, "end_stone", 1);
        long terracotta = valueOf(data, "terracotta", 64);
        long packedIce = valueOf(data, "packed_ice", 144);
        long blueIce = valueOf(data, "blue_ice", packedIce * 9);
        long slimeBall = valueOf(data, "slime_ball", 24);
        long honeycomb = valueOf(data, "honeycomb", 32);
        long blazeRod = valueOf(data, "blaze_rod", 1536);
        long ghastTear = valueOf(data, "ghast_tear", 4096);
        long phantomMembrane = valueOf(data, "phantom_membrane", 512);
        long scute = valueOf(data, "turtle_scute", 512);
        long dragonBreath = valueOf(data, "dragon_breath", 2048);
        long netherStar = valueOf(data, "nether_star", 139264);

        putMissing(data, "coal_ore", coal);
        putMissing(data, "deepslate_coal_ore", coal);
        putMissing(data, "iron_ore", rawIron);
        putMissing(data, "deepslate_iron_ore", rawIron);
        putMissing(data, "gold_ore", rawGold);
        putMissing(data, "deepslate_gold_ore", rawGold);
        putMissing(data, "copper_ore", average(rawCopper, 3.5D));
        putMissing(data, "deepslate_copper_ore", average(rawCopper, 3.5D));
        putMissing(data, "redstone_ore", average(redstone, 4.5D));
        putMissing(data, "deepslate_redstone_ore", average(redstone, 4.5D));
        putMissing(data, "lapis_ore", average(lapis, 6.5D));
        putMissing(data, "deepslate_lapis_ore", average(lapis, 6.5D));
        putMissing(data, "diamond_ore", diamond);
        putMissing(data, "deepslate_diamond_ore", diamond);
        putMissing(data, "emerald_ore", emerald);
        putMissing(data, "deepslate_emerald_ore", emerald);
        putMissing(data, "nether_gold_ore", average(goldNugget, 4.0D));
        putMissing(data, "nether_quartz_ore", quartz);
        putMissing(data, "ancient_debris", netheriteScrap);

        putMissing(data, "amethyst_cluster", amethyst * 4);
        putMissing(data, "large_amethyst_bud", amethyst * 3);
        putMissing(data, "medium_amethyst_bud", amethyst * 2);
        putMissing(data, "small_amethyst_bud", amethyst);
        putMissing(data, "budding_amethyst", amethyst * 8);
        putMissing(data, "calcite", 8);
        putMissing(data, "tuff", 1);
        putMissing(data, "smooth_basalt", 1);
        putMissing(data, "pointed_dripstone", 16);
        putMissing(data, "dripstone_block", 64);
        putMissing(data, "rooted_dirt", 8);
        putMissing(data, "hanging_roots", 4);
        putMissing(data, "spore_blossom", 128);
        putMissing(data, "moss_block", 8);
        putMissing(data, "moss_carpet", 3);
        putMissing(data, "azalea", 32);
        putMissing(data, "flowering_azalea", 48);
        putMissing(data, "big_dripleaf", 32);
        putMissing(data, "small_dripleaf", 16);
        putMissing(data, "glow_lichen", 16);
        putMissing(data, "powder_snow_bucket", valueOf(data, "bucket", ironIngot * 3));

        putMissing(data, "sculk", 32);
        putMissing(data, "sculk_vein", 8);
        putMissing(data, "sculk_catalyst", 2048);
        putMissing(data, "sculk_sensor", 1024);
        putMissing(data, "calibrated_sculk_sensor", 1024 + amethyst * 3);
        putMissing(data, "sculk_shrieker", 4096);
        putMissing(data, "echo_shard", echoShard);
        putMissing(data, "recovery_compass", valueOf(data, "compass", ironIngot * 4 + redstone) + echoShard * 8);
        putMissing(data, "disc_fragment_5", 512);

        putMissing(data, "sniffer_egg", 2048);
        putMissing(data, "torchflower_seeds", 128);
        putMissing(data, "torchflower", 192);
        putMissing(data, "pitcher_pod", 128);
        putMissing(data, "pitcher_plant", 192);
        putMissing(data, "pink_petals", 8);

        putMissing(data, "trial_key", 2048);
        putMissing(data, "ominous_trial_key", 8192);
        putMissing(data, "breeze_rod", blazeRod);
        putMissing(data, "wind_charge", Math.max(1, blazeRod / 4));
        putMissing(data, "heavy_core", netherStar);
        putMissing(data, "mace", netherStar + blazeRod + valueOf(data, "stick", stick));

        putMissing(data, "ominous_bottle", 1024);
        putMissing(data, "resin_clump", 64);
        putMissing(data, "resin_block", 64 * 9);
        putMissing(data, "resin_brick", 64);
        putMissing(data, "resin_bricks", 64 * 4);
        putMissing(data, "creaking_heart", 4096);
        putMissing(data, "pale_oak_log", valueOf(data, "oak_log", 32));
        putMissing(data, "pale_oak_wood", valueOf(data, "oak_wood", 32));
        putMissing(data, "stripped_pale_oak_log", valueOf(data, "stripped_oak_log", 32));
        putMissing(data, "stripped_pale_oak_wood", valueOf(data, "stripped_oak_wood", 32));
        putMissing(data, "pale_oak_sapling", valueOf(data, "oak_sapling", 32));
        putMissing(data, "pale_oak_leaves", valueOf(data, "oak_leaves", 1));
        putMissing(data, "pale_moss_block", 8);
        putMissing(data, "pale_moss_carpet", 3);
        putMissing(data, "pale_hanging_moss", 4);
        putMissing(data, "open_eyeblossom", 16);
        putMissing(data, "closed_eyeblossom", 16);

        putMissing(data, "goat_horn", 1024);
        putMissing(data, "turtle_scute", scute);
        putMissing(data, "armadillo_scute", scute);
        putMissing(data, "brush", copperIngot + stick + valueOf(data, "feather", 48));
        putMissing(data, "bundle", leather + string * 2);
        putMissing(data, "rabbit_hide", Math.max(1, leather / 4));
        putMissing(data, "phantom_membrane", phantomMembrane);
        putMissing(data, "dragon_breath", dragonBreath);
        putMissing(data, "elytra", 32768);
        putMissing(data, "dragon_egg", 262144);

        putMissing(data, "suspicious_sand", valueOf(data, "sand", 1) + 128);
        putMissing(data, "suspicious_gravel", valueOf(data, "gravel", 4) + 128);

        addMusicDiscs(data, 2048);
        addPotterySherds(data, 512);
        addBannerPatterns(data, paper + 1024);
        addSmithingTemplates(data, diamond, netherrack, cobblestone, deepslate, stone, sandstone, blackstone, endStone, terracotta, packedIce, blueIce, prismarineShard, prismarineCrystals, slimeBall, honeycomb, ghastTear);
    }

    private static void addMusicDiscs(Object2LongMap<ItemInfo> data, long value) {
        String[] discs = {
                "music_disc_13", "music_disc_cat", "music_disc_blocks", "music_disc_chirp", "music_disc_far",
                "music_disc_mall", "music_disc_mellohi", "music_disc_stal", "music_disc_strad", "music_disc_ward",
                "music_disc_11", "music_disc_wait", "music_disc_otherside", "music_disc_5", "music_disc_pigstep",
                "music_disc_relic", "music_disc_creator", "music_disc_creator_music_box", "music_disc_precipice"
        };

        for (String disc : discs) {
            putMissing(data, disc, value);
        }
    }

    private static void addPotterySherds(Object2LongMap<ItemInfo> data, long value) {
        String[] sherds = {
                "angler_pottery_sherd", "archer_pottery_sherd", "arms_up_pottery_sherd", "blade_pottery_sherd",
                "brewer_pottery_sherd", "burn_pottery_sherd", "danger_pottery_sherd", "explorer_pottery_sherd",
                "flow_pottery_sherd", "friend_pottery_sherd", "guster_pottery_sherd", "heart_pottery_sherd",
                "heartbreak_pottery_sherd", "howl_pottery_sherd", "miner_pottery_sherd", "mourner_pottery_sherd",
                "plenty_pottery_sherd", "prize_pottery_sherd", "scrape_pottery_sherd", "sheaf_pottery_sherd",
                "shelter_pottery_sherd", "skull_pottery_sherd", "snort_pottery_sherd"
        };

        for (String sherd : sherds) {
            putMissing(data, sherd, value);
        }
    }

    private static void addBannerPatterns(Object2LongMap<ItemInfo> data, long defaultValue) {
        String[] patterns = {
                "flower_banner_pattern", "creeper_banner_pattern", "skull_banner_pattern", "mojang_banner_pattern",
                "globe_banner_pattern", "piglin_banner_pattern", "flow_banner_pattern", "guster_banner_pattern"
        };

        for (String pattern : patterns) {
            putMissing(data, pattern, defaultValue);
        }
    }

    private static void addSmithingTemplates(
            Object2LongMap<ItemInfo> data,
            long diamond,
            long netherrack,
            long cobblestone,
            long deepslate,
            long stone,
            long sandstone,
            long blackstone,
            long endStone,
            long terracotta,
            long packedIce,
            long blueIce,
            long prismarineShard,
            long prismarineCrystals,
            long slimeBall,
            long honeycomb,
            long ghastTear
    ) {
        putMissing(data, "netherite_upgrade_smithing_template", diamond * 7 + netherrack);
        putMissing(data, "sentry_armor_trim_smithing_template", diamond * 7 + cobblestone);
        putMissing(data, "dune_armor_trim_smithing_template", diamond * 7 + sandstone);
        putMissing(data, "coast_armor_trim_smithing_template", diamond * 7 + cobblestone);
        putMissing(data, "wild_armor_trim_smithing_template", diamond * 7 + mossValue(data));
        putMissing(data, "ward_armor_trim_smithing_template", diamond * 7 + deepslate);
        putMissing(data, "eye_armor_trim_smithing_template", diamond * 7 + endStone);
        putMissing(data, "vex_armor_trim_smithing_template", diamond * 7 + cobblestone);
        putMissing(data, "tide_armor_trim_smithing_template", diamond * 7 + prismarineShard + prismarineCrystals);
        putMissing(data, "snout_armor_trim_smithing_template", diamond * 7 + blackstone);
        putMissing(data, "rib_armor_trim_smithing_template", diamond * 7 + netherrack);
        putMissing(data, "spire_armor_trim_smithing_template", diamond * 7 + endStone);
        putMissing(data, "wayfinder_armor_trim_smithing_template", diamond * 7 + terracotta);
        putMissing(data, "shaper_armor_trim_smithing_template", diamond * 7 + terracotta);
        putMissing(data, "silence_armor_trim_smithing_template", diamond * 7 + deepslate);
        putMissing(data, "raiser_armor_trim_smithing_template", diamond * 7 + terracotta);
        putMissing(data, "host_armor_trim_smithing_template", diamond * 7 + terracotta);
        putMissing(data, "flow_armor_trim_smithing_template", diamond * 7 + stone + slimeBall);
        putMissing(data, "bolt_armor_trim_smithing_template", diamond * 7 + copperValue(data) + honeycomb);
        putMissing(data, "miner_pottery_sherd", 512);
        putMissing(data, "angler_pottery_sherd", 512);
        putMissing(data, "blue_ice", blueIce);
        putMissing(data, "packed_ice", packedIce);
        putMissing(data, "ghast_tear", ghastTear);
    }

    private static long mossValue(Object2LongMap<ItemInfo> data) {
        return valueOf(data, "moss_block", 8);
    }

    private static long copperValue(Object2LongMap<ItemInfo> data) {
        return valueOf(data, "copper_ingot", valueOf(data, "raw_copper", 85));
    }

    private static long average(long singleValue, double averageCount) {
        return Math.max(1L, Math.round(singleValue * averageCount));
    }

    private static long valueOf(Object2LongMap<ItemInfo> data, String minecraftItemName, long fallback) {
        ItemInfo info = itemInfo(minecraftItemName);

        if (info == null) {
            return fallback;
        }

        long value = data.getLong(info);
        return value > 0 ? value : fallback;
    }

    private static void putMissing(Object2LongMap<ItemInfo> data, String minecraftItemName, long value) {
        if (value <= 0) {
            return;
        }

        ItemInfo info = itemInfo(minecraftItemName);

        if (info != null && !data.containsKey(info)) {
            data.put(info, value);
        }
    }

    private static ItemInfo itemInfo(String minecraftItemName) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("minecraft", minecraftItemName);

        if (!BuiltInRegistries.ITEM.containsKey(id)) {
            return null;
        }

        Item item = BuiltInRegistries.ITEM.get(id);

        if (item == Items.AIR) {
            return null;
        }

        return ItemInfo.fromItem(item);
    }
}
