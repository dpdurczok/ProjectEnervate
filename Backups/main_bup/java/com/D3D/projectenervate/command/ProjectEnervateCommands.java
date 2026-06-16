package com.D3D.projectenervate.command;

import com.D3D.projectenervate.emc.AdaptiveEmcValues;
import com.D3D.projectenervate.emc.AdaptiveEmcOutputHelper;
import com.D3D.projectenervate.emc.ProjectEnervateSourceHelper;
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

        BigDecimal normalizedValue = AdaptiveEmcValues.normalizeInternal(parsedValue);

        if (normalizedValue.signum() <= 0) {
            if (!ProjectEnervateSourceHelper.markZeroIfBaseEmc(stack)) {
                ProjectEnervateSourceHelper.clearProjectEnervateData(stack);
            }
        } else {
            BigDecimal baseSingle = AdaptiveEmcOutputHelper.getBaseSingleEmc(stack);

            if (baseSingle.signum() > 0 && normalizedValue.compareTo(baseSingle) >= 0) {
                if (!ProjectEnervateSourceHelper.markVerifiedIfBaseEmc(stack)) {
                    ProjectEnervateSourceHelper.clearProjectEnervateData(stack);
                }
            } else {
                ProjectEnervateSourceHelper.markAdaptive(stack, normalizedValue);
            }
        }

        player.inventoryMenu.broadcastChanges();

        ctx.getSource().sendSuccess(
                () -> Component.literal(
                        "ProjectEnervate EMC state set to "
                                + ProjectEnervateCommands.describeState(stack, normalizedValue)
                ),
                true
        );
        return Command.SINGLE_SUCCESS;
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
