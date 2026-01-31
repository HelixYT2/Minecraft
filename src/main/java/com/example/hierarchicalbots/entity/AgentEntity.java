package com.example.hierarchicalbots.entity;

import java.util.UUID;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class AgentEntity extends PathAwareEntity {
    private static final TrackedData<Integer> SKIN_SEED = DataTracker.registerData(AgentEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> AGE_DAYS = DataTracker.registerData(AgentEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Byte> INJURY_FLAGS = DataTracker.registerData(AgentEntity.class, TrackedDataHandlerRegistry.BYTE);
    private static final TrackedData<Byte> DISEASE_STATE = DataTracker.registerData(AgentEntity.class, TrackedDataHandlerRegistry.BYTE);

    private static final int YOUTH_DAYS = 20;
    private static final int PRIME_DAYS = 60;
    private static final int SENIOR_DAYS = 100;
    private static final UUID AGE_SPEED_MODIFIER_ID = UUID.fromString("4b442b2c-b07b-4ca7-8f03-9fe0a31db5d9");

    private int diseaseCheckCooldown = 0;

    public AgentEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return net.minecraft.entity.mob.MobEntity.createMobAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.1)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(SKIN_SEED, this.random.nextInt());
        builder.add(AGE_DAYS, 0);
        builder.add(INJURY_FLAGS, (byte) 0);
        builder.add(DISEASE_STATE, (byte) 0);
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("SkinSeed")) {
            dataTracker.set(SKIN_SEED, nbt.getInt("SkinSeed"));
        }
        if (nbt.contains("AgeDays")) {
            dataTracker.set(AGE_DAYS, nbt.getInt("AgeDays"));
        }
        if (nbt.contains("InjuryFlags")) {
            dataTracker.set(INJURY_FLAGS, nbt.getByte("InjuryFlags"));
        }
        if (nbt.contains("DiseaseState")) {
            dataTracker.set(DISEASE_STATE, nbt.getByte("DiseaseState"));
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("SkinSeed", dataTracker.get(SKIN_SEED));
        nbt.putInt("AgeDays", dataTracker.get(AGE_DAYS));
        nbt.putByte("InjuryFlags", dataTracker.get(INJURY_FLAGS));
        nbt.putByte("DiseaseState", dataTracker.get(DISEASE_STATE));
    }

    @Override
    public void tick() {
        super.tick();
        if (!getWorld().isClient) {
            updateAgeFromWorld();
            applyAgeModifiers();
            applyInjuryDebuffs();
            applyDiseaseEffects();
            attemptDiseaseSpread();
        }
    }

    public int getSkinSeed() {
        return dataTracker.get(SKIN_SEED);
    }

    public void setSkinSeed(int seed) {
        dataTracker.set(SKIN_SEED, seed);
    }

    public int getAgeDays() {
        return dataTracker.get(AGE_DAYS);
    }

    public void setInjuryFlags(byte flags) {
        dataTracker.set(INJURY_FLAGS, flags);
    }

    public boolean isInjured() {
        return dataTracker.get(INJURY_FLAGS) != 0;
    }

    public void setDiseaseState(byte state) {
        dataTracker.set(DISEASE_STATE, state);
    }

    public boolean isDiseased() {
        return dataTracker.get(DISEASE_STATE) != 0;
    }

    private void updateAgeFromWorld() {
        long day = getWorld().getTimeOfDay() / 24000L;
        dataTracker.set(AGE_DAYS, (int) day);
    }

    private void applyAgeModifiers() {
        double speedModifier = 0.0;
        int age = getAgeDays();
        if (age < YOUTH_DAYS) {
            speedModifier = 0.05;
        } else if (age >= PRIME_DAYS && age < SENIOR_DAYS) {
            speedModifier = -0.01;
        } else if (age >= SENIOR_DAYS) {
            speedModifier = -0.03;
        }

        if (getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED) == null) {
            return;
        }
        getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).removeModifier(AGE_SPEED_MODIFIER_ID);
        if (speedModifier != 0.0) {
            getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).addPersistentModifier(
                new EntityAttributeModifier(AGE_SPEED_MODIFIER_ID, "Age speed modifier", speedModifier, EntityAttributeModifier.Operation.ADD_VALUE)
            );
        }
    }

    private void applyInjuryDebuffs() {
        if (isInjured()) {
            addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 0, true, false, true));
            addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 40, 0, true, false, true));
        }
    }

    private void applyDiseaseEffects() {
        if (isDiseased()) {
            addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 60, 0, true, false, true));
            addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 60, 0, true, false, true));
        }
    }

    private void attemptDiseaseSpread() {
        if (diseaseCheckCooldown > 0) {
            diseaseCheckCooldown--;
            return;
        }
        diseaseCheckCooldown = 40;
        if (!isDiseased()) {
            return;
        }
        Vec3d center = getPos();
        Box box = new Box(center, center).expand(2.5);
        for (AgentEntity agent : getWorld().getEntitiesByClass(AgentEntity.class, box, agent -> agent != this)) {
            if (!agent.isDiseased() && random.nextFloat() < 0.1f) {
                agent.setDiseaseState((byte) 1);
            }
        }
    }

    public void applySpawnReset() {
        setVelocity(0.0, 0.0, 0.0);
        fallDistance = 0.0f;
        setNoGravity(false);
        noClip = false;
    }

    @Override
    protected void playStepSound(net.minecraft.util.math.BlockPos pos, net.minecraft.block.BlockState state) {
        super.playStepSound(pos, state);
    }

    @Override
    public boolean damage(net.minecraft.entity.damage.DamageSource source, float amount) {
        if (source.getAttacker() instanceof PlayerEntity && random.nextFloat() < 0.05f) {
            setInjuryFlags((byte) 1);
        }
        return super.damage(source, amount);
    }
}
