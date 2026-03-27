package qdeeme.xp_simplifier.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.nbt.NbtCompound;

import qdeeme.xp_simplifier.util.MerchantPersistence;

/**
 * Restore merchant XP from persistent storage when a villager is loaded/created.
 * This ensures merchants retain their XP accumulation across sessions.
 * Uses multiple save points to ensure persistence even when screens close.
 */
@Mixin(VillagerEntity.class)
public abstract class MixinVillagerXpRestore {

	@Shadow
	public abstract int getExperience();

	/**
	 * When reading NBT data (loading from world), check if we have stored XP for this villager
	 * and restore it. This is called when a villager is loaded from the world NBT.
	 */
	@Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
	private void onReadNbt(NbtCompound nbt, CallbackInfo ci) {
		VillagerEntity villager = (VillagerEntity) (Object) this;
		
		// Try to restore from persistent storage if this is a known merchant
		if (MerchantPersistence.hasMerchantXp(villager.getUuid())) {
			int storedXp = MerchantPersistence.getMerchantXp(villager.getUuid());
			villager.setExperienceFromServer(storedXp);
		}
	}

	/**
	 * Save merchant XP whenever experience is set (this catches trade completions).
	 * This ensures XP is saved immediately after any change.
	 */
	@Inject(method = "setExperienceFromServer", at = @At("HEAD"))
	private void onExperienceSet(int experience, CallbackInfo ci) {
		VillagerEntity villager = (VillagerEntity) (Object) this;
		
		// Save to persistent storage when experience is set
		MerchantPersistence.setMerchantXp(villager.getUuid(), experience);
		MerchantPersistence.save();
	}

	/**
	 * Save merchant XP to persistent storage when the villager is unloaded.
	 * This captures their current XP level for the next session.
	 */
	@Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
	private void onWriteNbt(NbtCompound nbt, CallbackInfo ci) {
		VillagerEntity villager = (VillagerEntity) (Object) this;
		
		// Save current XP to persistent storage
		int currentXp = villager.getExperience();
		MerchantPersistence.setMerchantXp(villager.getUuid(), currentXp);
		MerchantPersistence.save();
	}
}
