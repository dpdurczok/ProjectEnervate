package com.D3D.projectenervate.machine;

import java.math.BigDecimal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class PendingConversionBudget {
    private final BlockPos pos;
    private BigDecimal budget;
    private final long expiresAtTick;

    public PendingConversionBudget(BlockPos pos, BigDecimal budget, long expiresAtTick) {
        this.pos = pos.immutable();
        this.budget = budget;
        this.expiresAtTick = expiresAtTick;
    }

    public BlockPos pos() {
        return pos;
    }

    public BigDecimal budget() {
        return budget;
    }

    public boolean isEmpty() {
        return budget.signum() <= 0;
    }

    public boolean isExpired(long gameTime) {
        return gameTime > expiresAtTick;
    }

    public double distanceToSqr(Vec3 vec) {
        double dx = pos.getX() + 0.5D - vec.x;
        double dy = pos.getY() + 0.5D - vec.y;
        double dz = pos.getZ() + 0.5D - vec.z;

        return dx * dx + dy * dy + dz * dz;
    }
}