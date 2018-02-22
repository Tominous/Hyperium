package com.hcc.mixins;

import com.hcc.utils.mods.AsyncScreenshotSaver;
import com.hcc.utils.ChatColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ScreenShotHelper;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.io.File;
import java.nio.IntBuffer;

@Mixin(ScreenShotHelper.class)
public class MixinScreenShotHelper {

    @Shadow
    private static IntBuffer pixelBuffer;

    @Shadow
    private static int[] pixelValues;

    @Shadow
    @Final
    private static Logger logger;

    @Shadow
    private static File getTimestampedPNGFileForDirectory(File gameDirectory) {
        return null;
    }


    /**
     * Saves a screenshot in the game directory with the given file name (or null to generate a time-stamped name).
     * Fixes MC-113208 and MC-117793
     * TODO: Imgur uploader
     *
     * @param gameDirectory
     * @param screenshotName
     * @param width
     * @param height
     * @param buffer
     * @return
     * @author Kevin Brewster, Orange Marhsall, Mojang
     */
    @Overwrite
    public static IChatComponent saveScreenshot(File gameDirectory, String screenshotName, int width, int height, Framebuffer buffer) {
        final File file1 = new File(Minecraft.getMinecraft().mcDataDir, "screenshots");
        file1.mkdir();
        if (OpenGlHelper.isFramebufferEnabled()) {
            width = buffer.framebufferTextureWidth;
            height = buffer.framebufferTextureHeight;
        }
        final int i = width * height;
        if (pixelBuffer == null || pixelBuffer.capacity() < i) {
            pixelBuffer = BufferUtils.createIntBuffer(i);
            pixelValues = new int[i];
        }
        GL11.glPixelStorei(3333, 1);
        GL11.glPixelStorei(3317, 1);
        pixelBuffer.clear();
        if (OpenGlHelper.isFramebufferEnabled()) {
            GlStateManager.bindTexture(buffer.framebufferTexture);
            GL11.glGetTexImage(3553, 0, 32993, 33639, pixelBuffer);
        } else {
            GL11.glReadPixels(0, 0, width, height, 32993, 33639, pixelBuffer);
        }
        pixelBuffer.get(pixelValues);
        boolean upload = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        new Thread(new AsyncScreenshotSaver(width, height, pixelValues, Minecraft.getMinecraft().getFramebuffer(), new File(Minecraft.getMinecraft().mcDataDir, "screenshots"), upload)).start();
        if(!upload) return new ChatComponentText(ChatColor.RED + "[HCC] " + ChatColor.WHITE + "Capturing...");
        return new ChatComponentText(ChatColor.RED + "[HCC] " + ChatColor.WHITE + "Uploading...");
    }

}