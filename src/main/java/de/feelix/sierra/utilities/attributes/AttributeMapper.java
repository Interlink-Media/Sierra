package de.feelix.sierra.utilities.attributes;

import lombok.Getter;

/**
 * AttributeMapper is an enumeration class that represents mappings between attribute keys and their maximum and minimum values.
 * It provides a way to retrieve an AttributeMapper object based on a given key.
 */
@Getter
public enum AttributeMapper {

    Armor("generic.armor", 30, 0),
    ArmorTough("generic.armorToughness", 20, 0),
    AttackDamage("generic.attackDamage", 2.048, 0),
    AttackKnockback("generic.attackKnockback", 5, 0),
    AttackSpeed("generic.attackSpeed", 1.024, 0),
    FlyingSpeed("generic.flyingSpeed", 1.024, 0),
    HoseJumpStrength("horse.jumpStrength", 2, 0),
    JumpStrength("generic.jumpStrength", 32, 0),
    PlayerBlockInteraction("player.blockInteractionRange", 64, 0),
    PlayerEntityRange("player.entityInteractionRange", 64, 0),
    PlayerBlockBreak("player.blockBreakSpeed", 1024, 0),
    KnockbackResistance("generic.knockbackResistance", 1, 0),
    Luck("generic.luck", 1.024, -1.024),
    Gravity("generic.gravity", 1, -1),
    FallDistance("generic.safeFallDistance", 1.024, -1.024),
    FallDamageMultiplier("generic.fallDamageMultiplier", 100, 0),
    MaxHealth("generic.maxHealth", 1, -1),
    MaxAbsorption("generic.maxAbsorption", 2.048, 0),
    Scale("generic.scale", 16, 0.0625),
    StepHeight("generic.stepHeight", 10, 0),
    FollowRange("generic.followRange", 2.048, 0),
    MovementSpeed("generic.movementSpeed", 1.024, 0),
    SpawnReinforcements("zombie.spawnReinforcements", 1, 0);

    /**
     * This variable represents the key of an AttributeMapper object. The key is used to identify the attribute mapping in the AttributeMapper enum class.
     * It is a private final String variable.
     * <p>
     * Example usage:
     * <p>
     * AttributeMapper attribute = AttributeMapper.getAttributeMapper(key);
     * if (attribute != null) {
     *     String attributeKey = attribute.getKey();
     *     // Use attributeKey for further processing
     * }
     *
     * @see AttributeMapper
     */
    private final String key;

    /**
     * This variable represents the maximum value of an attribute.
     */
    private final double max;

    /**
     * The minimum value.
     *
     * <p>
     * This variable represents the minimum value that can be assigned. It is of type double, and is declared as final.
     * The {@code min} variable should be initialized during object creation and cannot be modified afterwards.
     * </p>
     */
    private final double min;

    /**
     * Represents an attribute mapper with a key, maximum value, and minimum value.
     * This class is used to map attribute keys to their corresponding maximum and minimum values.
     */
    AttributeMapper(String key, double max, double min) {
        this.key = key;
        this.max = max;
        this.min = min;
    }

    /**
     * Retrieves the AttributeMapper based on the given key.
     *
     * @param key the key of the AttributeMapper
     * @return the AttributeMapper object corresponding to the key, or null if not found
     */
    public static AttributeMapper getAttributeMapper(String key) {
        key = key.toLowerCase().replace("minecraft:", "");
        for (AttributeMapper value : values()) {
            String valueKey = value.getKey().toLowerCase().replace("_", "");
            if (valueKey.equalsIgnoreCase(key)) {
                return value;
            }
        }
        return null;
    }
}
