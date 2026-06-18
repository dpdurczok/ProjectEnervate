package com.D3D.projectenervate.api;

import java.math.BigInteger;
import net.minecraft.world.item.ItemStack;

public interface ProjectEnervateTransmutationAccess {
    BigInteger projectenervate$getFreeStarEmc();

    boolean projectenervate$canAcceptEmc(BigInteger value);

    boolean projectenervate$canStoreEmcHolder(ItemStack stack);

    boolean projectenervate$hasAnyEmcHolder();

    void projectenervate$showStorageMessage(String message);
}
