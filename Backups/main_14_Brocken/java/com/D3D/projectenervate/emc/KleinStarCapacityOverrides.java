package com.D3D.projectenervate.emc;

import moze_intel.projecte.gameObjs.items.KleinStar.KleinTier;

public final class KleinStarCapacityOverrides {

    private KleinStarCapacityOverrides() {
    }

    public static long getMaxEmc(KleinTier tier) {
        return switch (tier) {
            case EIN -> 10_000L;
            case ZWEI -> 40_000L;
            case DREI -> 160_000L;
            case VIER -> 640_000L;
            case SPHERE -> 2_560_000L;
            case OMEGA -> 10_240_000L;
        };
    }
}