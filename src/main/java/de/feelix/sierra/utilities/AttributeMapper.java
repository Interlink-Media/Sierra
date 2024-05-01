package de.feelix.sierra.utilities;

import lombok.Getter;

@Getter
public enum AttributeMapper {

    Armor("generic.armor", 30, 0),
    ArmorTough("generic.armorToughness", 20, 0),
    AttackDamage("generic.attackDamage", 2048, 0),
    AttackSpeed("generic.attackSpeed", 1024, 0),
    AttackKnockback("generic.attack_knockback", 5, 0),
    FlyingSpeed("generic.flyingSpeed", 1024, 0),
    JumpStrength("horse.jumpStrength", 2, 0),
    KnockbackResistance("generic.knockbackResistance", 1, 0),
    Luck("generic.luck", 1024, -1024),
    MaxHealth("generic.maxHealth", 1024, 0),
    FollowRange("generic.followRange", 2048, 0),
    MovementSpeed("generic.movementSpeed", 1024, 0),
    SpawnReinforcements("zombie.spawnReinforcements", 1, 0);

    private final String key;
    private final int    max;
    private final int    min;

    AttributeMapper(String key, int max, int min) {
        this.key = key;
        this.max = max;
        this.min = min;
    }

    public static AttributeMapper getAttributeMapper(String key) {
        for (AttributeMapper value : values()) {
            if (value.key.equalsIgnoreCase(key)) {
                return value;
            }
        }
        return null;
    }
}
