package com.D3D.projectenervate.command;

import com.D3D.projectenervate.ProjectEnervateConfig;
import com.D3D.projectenervate.emc.AdaptiveEmcOutputHelper;
import com.D3D.projectenervate.emc.AdaptiveEmcValues;
import com.D3D.projectenervate.emc.ProjectEnervateSourceHelper;
import com.D3D.projectenervate.emc.ResourceCourseManager;
import com.D3D.projectenervate.emc.StarDefinitionManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.math.BigDecimal;
import java.util.List;
import moze_intel.projecte.PEPermissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class ProjectEnervateCommands {

    private static final SimpleCommandExceptionType NO_ITEM =
            new SimpleCommandExceptionType(Component.literal("Hold an item first."));

    private static final SimpleCommandExceptionType INVALID_VALUE =
            new SimpleCommandExceptionType(Component.literal("Adaptive EMC must be a number greater than or equal to 0."));

    private static final SimpleCommandExceptionType STAR_EXISTS =
            new SimpleCommandExceptionType(Component.literal("A star with that name already exists."));

    private static final SimpleCommandExceptionType STAR_NOT_FOUND =
            new SimpleCommandExceptionType(Component.literal("No star with that name exists."));

    private static final SimpleCommandExceptionType RESOURCE_EXISTS =
            new SimpleCommandExceptionType(Component.literal("That resource is already listed on this star."));

    private static final SimpleCommandExceptionType RESOURCE_NO_BASE_EMC =
            new SimpleCommandExceptionType(Component.literal("That item has no ProjectE base EMC, so it cannot be added to a star."));

    private static final SimpleCommandExceptionType RESOURCE_INDEX_INVALID =
            new SimpleCommandExceptionType(Component.literal("That resource index does not exist on this star."));

    private ProjectEnervateCommands() {
    }

    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("projectenervate")
                        .then(registerSetAdaptiveEmc())
                        .then(registerValidate())
                        .then(registerCourses())
                        .then(registerStar(event.getBuildContext()))
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> registerSetAdaptiveEmc() {
        return Commands.literal("setadaptiveemc")
                .requires(PEPermissions.COMMAND_SET_EMC)
                .then(Commands.argument("value", StringArgumentType.string())
                        .executes(ProjectEnervateCommands::setAdaptiveEmc));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> registerValidate() {
        return Commands.literal("validate")
                .requires(PEPermissions.COMMAND_SET_EMC)
                .executes(ProjectEnervateCommands::validateHeldItem);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> registerCourses() {
        return Commands.literal("courses")
                .executes(ProjectEnervateCommands::printCourses);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> registerStar(CommandBuildContext buildContext) {
        return Commands.literal("star")
                .then(Commands.literal("list")
                        .executes(ProjectEnervateCommands::listStars)
                        .then(Commands.argument("star_name", StringArgumentType.greedyString())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(StarDefinitionManager.configuredStarNames(), builder))
                                .executes(ProjectEnervateCommands::listStarResources)))
                .then(Commands.literal("create")
                        .requires(PEPermissions.COMMAND_SET_EMC)
                        .then(Commands.argument("star_name", StringArgumentType.greedyString())
                                .executes(ProjectEnervateCommands::createStar)))
                .then(Commands.literal("addresource")
                        .requires(PEPermissions.COMMAND_SET_EMC)
                        .then(Commands.argument("star_name", StringArgumentType.string())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(StarDefinitionManager.configuredStarNames(), builder))
                                .then(Commands.argument("item", ItemArgument.item(buildContext))
                                        .executes(ProjectEnervateCommands::addStarResource))))
                .then(Commands.literal("remove")
                        .requires(PEPermissions.COMMAND_SET_EMC)
                        .then(Commands.literal("resource")
                                .then(Commands.argument("star_name", StringArgumentType.string())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(StarDefinitionManager.configuredStarNames(), builder))
                                        .then(Commands.argument("resource_index", IntegerArgumentType.integer(1))
                                                .executes(ProjectEnervateCommands::removeStarResource)))));
    }

    private static int printCourses(CommandContext<CommandSourceStack> ctx) {
        List<ResourceCourseManager.StarCourseView> stars = ResourceCourseManager.currentStarCourses(ctx.getSource().getLevel());

        if (stars.isEmpty()) {
            ctx.getSource().sendSuccess(
                    () -> Component.literal("ProjectEnervate courses: no active configured stars."),
                    false
            );
            return Command.SINGLE_SUCCESS;
        }

        ctx.getSource().sendSuccess(
                () -> Component.literal("ProjectEnervate current star courses (" + ResourceCourseManager.currentCourseStatus(ctx.getSource().getLevel()) + "):"),
                false
        );

        for (ResourceCourseManager.StarCourseView star : stars) {
            ctx.getSource().sendSuccess(
                    () -> Component.literal("[" + star.index() + "] " + star.name() + ": x" + AdaptiveEmcValues.format(star.multiplier())
                            + " resources=" + String.join(", ", star.activeResources())),
                    false
            );
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int listStars(CommandContext<CommandSourceStack> ctx) {
        List<StarDefinitionManager.StarDefinition> configured = StarDefinitionManager.configuredStars();
        List<StarDefinitionManager.ActiveStar> active = StarDefinitionManager.activeStars();

        if (configured.isEmpty()) {
            ctx.getSource().sendSuccess(
                    () -> Component.literal("ProjectEnervate stars: none configured."),
                    false
            );
            return Command.SINGLE_SUCCESS;
        }

        ctx.getSource().sendSuccess(
                () -> Component.literal("ProjectEnervate stars (active " + active.size() + "/" + configured.size() + "):"),
                false
        );

        for (StarDefinitionManager.StarDefinition star : configured) {
            boolean isActive = active.stream().anyMatch(activeStar -> activeStar.index() == star.index());
            String state = isActive ? "active" : "skipped";
            ctx.getSource().sendSuccess(
                    () -> Component.literal("[" + star.index() + "] " + star.name() + " (" + state + ", " + star.resources().size() + " resources)"),
                    false
            );
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int listStarResources(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String starName = StringArgumentType.getString(ctx, "star_name");
        StarDefinitionManager.StarDefinition star = StarDefinitionManager.findConfiguredStar(starName);

        if (star == null) {
            throw STAR_NOT_FOUND.create();
        }

        ctx.getSource().sendSuccess(
                () -> Component.literal("ProjectEnervate star [" + star.index() + "] " + star.name() + " resources:"),
                false
        );

        if (star.resources().isEmpty()) {
            ctx.getSource().sendSuccess(
                    () -> Component.literal("No configured resources. The star will not generate until at least one valid base-EMC resource is added."),
                    false
            );
            return Command.SINGLE_SUCCESS;
        }

        for (int i = 0; i < star.resources().size(); i++) {
            String resource = star.resources().get(i);
            String state = StarDefinitionManager.describeSelectorState(resource);
            int displayIndex = i + 1;
            ctx.getSource().sendSuccess(
                    () -> Component.literal("[" + displayIndex + "] " + resource + " (" + state + ")"),
                    false
            );
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int createStar(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String starName = StringArgumentType.getString(ctx, "star_name");

        if (!StarDefinitionManager.createStar(starName)) {
            throw STAR_EXISTS.create();
        }

        String configPath = ProjectEnervateConfig.configFilePath().toAbsolutePath().toString();
        ctx.getSource().sendSuccess(
                () -> Component.literal("Created ProjectEnervate star: " + starName + ". Saved to " + configPath),
                true
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int addStarResource(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String starName = StringArgumentType.getString(ctx, "star_name");
        ItemInput itemInput = ItemArgument.getItem(ctx, "item");
        ItemStack stack = itemInput.createItemStack(1, false);
        ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());

        StarDefinitionManager.AddResourceResult result = StarDefinitionManager.addItemResource(starName, itemId);

        switch (result) {
            case ADDED -> {
                String configPath = ProjectEnervateConfig.configFilePath().toAbsolutePath().toString();
                ctx.getSource().sendSuccess(
                        () -> Component.literal("Added " + itemId + " to star " + starName + ". Saved to " + configPath),
                        true
                );
                return Command.SINGLE_SUCCESS;
            }
            case NO_STAR -> throw STAR_NOT_FOUND.create();
            case NO_BASE_EMC -> throw RESOURCE_NO_BASE_EMC.create();
            case ALREADY_EXISTS -> throw RESOURCE_EXISTS.create();
            case INVALID_ITEM -> throw RESOURCE_NO_BASE_EMC.create();
            default -> throw RESOURCE_NO_BASE_EMC.create();
        }
    }

    private static int removeStarResource(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String starName = StringArgumentType.getString(ctx, "star_name");
        int resourceIndex = IntegerArgumentType.getInteger(ctx, "resource_index");
        StarDefinitionManager.RemoveResourceResult result = StarDefinitionManager.removeResource(starName, resourceIndex);

        switch (result) {
            case REMOVED -> {
                String configPath = ProjectEnervateConfig.configFilePath().toAbsolutePath().toString();
                ctx.getSource().sendSuccess(
                        () -> Component.literal("Removed resource " + resourceIndex + " from star " + starName + ". Saved to " + configPath),
                        true
                );
                return Command.SINGLE_SUCCESS;
            }
            case NO_STAR -> throw STAR_NOT_FOUND.create();
            case INVALID_INDEX -> throw RESOURCE_INDEX_INVALID.create();
            default -> throw RESOURCE_INDEX_INVALID.create();
        }
    }

    private static int setAdaptiveEmc(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String rawValue = StringArgumentType.getString(ctx, "value");
        BigDecimal parsedValue = parseValue(rawValue);

        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack stack = getHeldItem(player);

        BigDecimal normalizedValue = AdaptiveEmcValues.normalizeInternal(parsedValue);

        if (normalizedValue.signum() <= 0) {
            if (!ProjectEnervateSourceHelper.markZeroIfBaseEmc(stack)) {
                ProjectEnervateSourceHelper.clearProjectEnervateData(stack);
            }
        } else {
            BigDecimal baseSingle = AdaptiveEmcOutputHelper.getBaseSingleEmc(stack);

            if (baseSingle.signum() > 0
                    && normalizedValue.compareTo(baseSingle) >= 0
                    && ProjectEnervateConfig.chooseBaseEmcIfLower()) {
                if (!ProjectEnervateSourceHelper.markVerifiedIfBaseEmc(stack)) {
                    ProjectEnervateSourceHelper.clearProjectEnervateData(stack);
                }
            } else {
                ProjectEnervateSourceHelper.markAdaptive(stack, normalizedValue);
            }
        }

        syncHeldItem(player, stack);

        ctx.getSource().sendSuccess(
                () -> Component.literal(
                        "ProjectEnervate EMC state set to "
                                + ProjectEnervateCommands.describeState(stack, normalizedValue)
                ),
                true
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int validateHeldItem(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack stack = getHeldItem(player);

        if (!ProjectEnervateSourceHelper.markVerifiedIfBaseEmc(stack)) {
            ProjectEnervateSourceHelper.clearProjectEnervateData(stack);
        }

        syncHeldItem(player, stack);

        ctx.getSource().sendSuccess(
                () -> Component.literal("ProjectEnervate EMC state set to verified"),
                true
        );
        return Command.SINGLE_SUCCESS;
    }

    private static void syncHeldItem(ServerPlayer player, ItemStack stack) {
        player.getInventory().setChanged();
        player.setItemInHand(player.getMainHandItem() == stack
                ? net.minecraft.world.InteractionHand.MAIN_HAND
                : net.minecraft.world.InteractionHand.OFF_HAND, stack);

        player.inventoryMenu.broadcastChanges();
        player.containerMenu.broadcastChanges();
        player.inventoryMenu.broadcastFullState();
        player.containerMenu.broadcastFullState();

        int inventorySlot = player.getMainHandItem() == stack ? player.getInventory().selected : 40;
        player.connection.send(new ClientboundContainerSetSlotPacket(-2, 0, inventorySlot, stack.copy()));
    }

    private static String describeState(ItemStack stack, BigDecimal requestedValue) {
        if (ProjectEnervateSourceHelper.isVerified(stack)) {
            return "verified";
        }

        if (ProjectEnervateSourceHelper.isZero(stack)) {
            return "zero";
        }

        if (ProjectEnervateSourceHelper.isAdaptive(stack)) {
            return "adaptive (" + AdaptiveEmcValues.format(requestedValue) + ")";
        }

        return "cleared";
    }

    private static BigDecimal parseValue(String rawValue) throws CommandSyntaxException {
        try {
            BigDecimal value = new BigDecimal(rawValue.trim());

            if (value.signum() < 0) {
                throw INVALID_VALUE.create();
            }

            return value;
        } catch (NumberFormatException exception) {
            throw INVALID_VALUE.create();
        }
    }

    private static ItemStack getHeldItem(ServerPlayer player) throws CommandSyntaxException {
        ItemStack stack = player.getMainHandItem();

        if (stack.isEmpty()) {
            stack = player.getOffhandItem();
        }

        if (stack.isEmpty()) {
            throw NO_ITEM.create();
        }

        return stack;
    }
}
