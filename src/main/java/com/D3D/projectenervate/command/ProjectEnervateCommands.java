package com.D3D.projectenervate.command;

import com.D3D.projectenervate.emc.AdaptiveEmcValues;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.math.BigDecimal;
import moze_intel.projecte.PEPermissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import com.D3D.projectenervate.emc.ProjectEnervateSourceHelper;

public final class ProjectEnervateCommands {

    private static final SimpleCommandExceptionType NO_ITEM =
            new SimpleCommandExceptionType(Component.literal("Hold an item first."));

    private static final SimpleCommandExceptionType INVALID_VALUE =
            new SimpleCommandExceptionType(Component.literal("Adaptive EMC must be a number greater than or equal to 0."));

    private ProjectEnervateCommands() {
    }

    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("projecte")
                        .then(registerSetAdaptiveEmc())
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> registerSetAdaptiveEmc() {
        return Commands.literal("setadaptiveemc")
                .requires(PEPermissions.COMMAND_SET_EMC)
                .then(Commands.argument("value", StringArgumentType.string())
                        .executes(ProjectEnervateCommands::setAdaptiveEmc));
    }

    private static int setAdaptiveEmc(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String rawValue = StringArgumentType.getString(ctx, "value");
        BigDecimal parsedValue = parseValue(rawValue);

        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack stack = getHeldItem(player);

        BigDecimal normalizedValue = AdaptiveEmcValues.normalize(parsedValue);
        AdaptiveEmcValues.set(stack, normalizedValue);

        player.inventoryMenu.broadcastChanges();

        ctx.getSource().sendSuccess(
                () -> Component.literal(
                        "Adaptive EMC Set to"
                                + AdaptiveEmcValues.format(normalizedValue)
                ),
                true
        );
        ProjectEnervateSourceHelper.markKnown(
                stack,
                ProjectEnervateSourceHelper.SOURCE_COMMAND
        );
        return Command.SINGLE_SUCCESS;
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