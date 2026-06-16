package com.D3D.projectenervate.mixin;

import com.D3D.projectenervate.emc.TradeAdaptiveEmcHelper;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantContainer.class)
public abstract class MerchantContainerMixin {

    private static final int PROJECTENERVATE_TRADE_RESULT_SLOT = 2;

    @Shadow
    public abstract ItemStack getItem(int slotIndex);

    @Shadow
    public abstract MerchantOffer getActiveOffer();

    @Inject(method = "updateSellItem", at = @At("RETURN"))
    private void projectenervate$applyAdaptiveEmcToVisibleTradeOutput(CallbackInfo ci) {
        MerchantOffer offer = getActiveOffer();

        if (offer == null) {
            return;
        }

        ItemStack outputStack = getItem(PROJECTENERVATE_TRADE_RESULT_SLOT);

        if (outputStack.isEmpty()) {
            return;
        }

        TradeAdaptiveEmcHelper.applyToTradeOutput(
                (MerchantContainer) (Object) this,
                offer,
                outputStack
        );
    }
}