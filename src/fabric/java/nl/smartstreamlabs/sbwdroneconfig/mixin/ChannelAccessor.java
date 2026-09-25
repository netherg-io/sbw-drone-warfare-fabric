package nl.smartstreamlabs.sbwdroneconfig.mixin;

import com.mojang.blaze3d.audio.Channel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The OpenAL source of a playing sound, which Sound Physics Remastered evaluates. */
@Mixin(Channel.class)
public interface ChannelAccessor {
    @Accessor("source") int sbwdroneconfig$source();
}
