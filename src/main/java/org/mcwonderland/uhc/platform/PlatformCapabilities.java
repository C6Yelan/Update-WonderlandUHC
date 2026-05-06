package org.mcwonderland.uhc.platform;

public final class PlatformCapabilities {

    private final boolean absorption;
    private final boolean fastBlockSet;
    private final boolean customExpOrb;
    private final boolean oldEnchant;
    private final boolean deathAnimation;
    private final boolean pickupExpControl;
    private final boolean largeChestMerge;

    private PlatformCapabilities(
            boolean absorption,
            boolean fastBlockSet,
            boolean customExpOrb,
            boolean oldEnchant,
            boolean deathAnimation,
            boolean pickupExpControl,
            boolean largeChestMerge
    ) {
        this.absorption = absorption;
        this.fastBlockSet = fastBlockSet;
        this.customExpOrb = customExpOrb;
        this.oldEnchant = oldEnchant;
        this.deathAnimation = deathAnimation;
        this.pickupExpControl = pickupExpControl;
        this.largeChestMerge = largeChestMerge;
    }

    public static PlatformCapabilities allLegacyNmsAvailable() {
        return new PlatformCapabilities(true, true, true, true, true, true, true);
    }

    public static PlatformCapabilities noLegacyNmsAvailable() {
        return new PlatformCapabilities(false, false, false, false, false, false, false);
    }

    public boolean hasAbsorption() {
        return absorption;
    }

    public boolean hasFastBlockSet() {
        return fastBlockSet;
    }

    public boolean hasCustomExpOrb() {
        return customExpOrb;
    }

    public boolean hasOldEnchant() {
        return oldEnchant;
    }

    public boolean hasDeathAnimation() {
        return deathAnimation;
    }

    public boolean hasPickupExpControl() {
        return pickupExpControl;
    }

    public boolean hasLargeChestMerge() {
        return largeChestMerge;
    }
}
