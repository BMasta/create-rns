package com.bmaster.createrns.deposit.capability;

//public class DepositIndexProvider implements ICapabilitySerializable<CompoundTag> {
//    private final DepositIndex data = new DepositIndex();
//    private final LazyOptional<IDepositIndex> opt;
//
//    public DepositIndexProvider() {
//        this.opt = LazyOptional.of(() -> data);
//    }
//
//    @Override
//    public CompoundTag serializeNBT() {
//        return data.serializeNBT();
//    }
//
//    @Override
//    public void deserializeNBT(CompoundTag tag) {
//            data.deserializeNBT(tag);
//    }
//
//    @NotNull
//    @Override
//    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
//        return RNSContent.DEPOSIT_INDEX.orEmpty(cap, opt);
//    }
//}
