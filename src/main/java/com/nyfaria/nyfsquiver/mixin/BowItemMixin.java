package com.nyfaria.nyfsquiver.mixin;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.nyfaria.nyfsquiver.item.QuiverItem;
import com.nyfaria.nyfsquiver.ui.QuiverScreenHandler;

import java.util.List;
import java.util.Optional;

@Mixin(BowItem.class)
public class BowItemMixin {
    @Inject(method = "releaseUsing", at = @At(value="TAIL", shift = At.Shift.BEFORE), cancellable = true)
    public void boop(ItemStack itemStack, Level level, LivingEntity livingEntity, int i, CallbackInfo c) {
        Optional<TrinketComponent> component = TrinketsApi.getTrinketComponent(livingEntity);
        if (component.isPresent() && !(((Player) livingEntity).getAbilities().instabuild || EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, itemStack) > 0)) {
            List<Tuple<SlotReference, ItemStack>> allEquipped = component.get().getAllEquipped();
            for (Tuple<SlotReference, ItemStack> entry : allEquipped) {
                ItemStack beep = entry.getB();
                if (beep.getItem() instanceof QuiverItem quiverItem) {
                    QuiverScreenHandler quiverContainer = new QuiverScreenHandler(0, ((Player) livingEntity).getInventory(), beep);
                    int currentSlotIndex = beep.getOrCreateTag().getInt("current_slot");
                    ItemStack currentArrow = quiverContainer.getSlot(currentSlotIndex).getItem();

                    // 尝试从当前槽位射箭
                    if (!currentArrow.isEmpty()) {
                        currentArrow.shrink(1); // 消耗一支箭
                        quiverContainer.getSlot(currentSlotIndex).setChanged(); // 通知槽位变化

                        // 如果当前槽位的箭已耗尽，寻找下一个槽位
                        if (currentArrow.getCount() <= 0) {
                            findNextAvailableSlot(beep, quiverContainer, currentSlotIndex, quiverItem.getTotalSlots());
                        }
                    } else {
                        // 当前槽位没有箭矢，查找下一个槽位
                        findNextAvailableSlot(beep, quiverContainer, currentSlotIndex, quiverItem.getTotalSlots());
                    }
                }
            }
        }
    }

    private void findNextAvailableSlot(ItemStack beep, QuiverScreenHandler quiverContainer, int currentSlotIndex, int maxSlots) {
        int nextSlotIndex = (currentSlotIndex + 1) % maxSlots;
        int startSlotIndex = currentSlotIndex;

        do {
            if (!quiverContainer.getSlot(nextSlotIndex).getItem().isEmpty()) {
                beep.getOrCreateTag().putInt("current_slot", nextSlotIndex); // 更新为下一个槽位
                return; // 找到槽位后返回
            }
            nextSlotIndex = (nextSlotIndex + 1) % maxSlots;
        } while (nextSlotIndex != startSlotIndex); // 确保不会无限搜索
    }

}
