package net.insane96mcp.xpholder.gui;

import java.io.IOException;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import net.insane96mcp.xpholder.XpHolder;
import net.insane96mcp.xpholder.lib.Properties;
import net.insane96mcp.xpholder.lib.Translatable;
import net.insane96mcp.xpholder.network.DepositMessage;
import net.insane96mcp.xpholder.network.GetXpHeldMessage;
import net.insane96mcp.xpholder.network.PacketHandler;
import net.insane96mcp.xpholder.network.WithdrawMessage;
import net.insane96mcp.xpholder.tileentity.TileEntityXpHolder;
import net.insane96mcp.xpholder.utils.Experience;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiPageButtonList.GuiResponder;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlider;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import scala.xml.Elem;

public class XpHolderGui extends GuiScreen{
	private final int GUI_WIDTH = 176;
	private final int GUI_HEIGHT = 100;
	private final int EXPERIENCE_COLOR = 8453920;
	private static ResourceLocation GUI_TEXTURE;
	private ButtonClose buttonClose;
	private GuiButton buttonWithdraw;
	private GuiButton buttonDeposit;
	
	private SliderResponder sliderResponder;
	private GuiSlider sliderXp;
	private SliderFormat sliderFormat;
	private float sliderValue;
	
	private static ResourceLocation VANILLA_TEXTURES;
	
	private static int buttonId = 0;
	
	private int x, y, z;
	public int xpHeld = 0;
	public int levelsHeld = 0;
	public int xpCap = 0;
	public float currentLevelXp = 0;
	//public int
	
	boolean isHoverWithdraw, isHoverDeposit, isHoverSlider;
	
	public Experience playerExperience;
	
	public XpHolderGui(int x, int y, int z) {
		GUI_TEXTURE = new ResourceLocation(XpHolder.MOD_ID, "textures/gui/xpholder.png");
		VANILLA_TEXTURES = new ResourceLocation("textures/gui/icons.png");
		
		this.x = x;
		this.y = y;
		this.z = z;
		
		playerExperience = new Experience();
	}
	
	@Override
	public void onGuiClosed() {

	}
	
	@Override
	public void initGui() {
        Keyboard.enableRepeatEvents(true);
        
		buttonClose = new ButtonClose(buttonId++, (width / 2) + (GUI_WIDTH / 2) + 2, (height / 2) - (GUI_HEIGHT / 2) + 2, "");
		buttonList.add(buttonClose);

		buttonWithdraw = new GuiButton(buttonId++, (width / 2) - (GUI_WIDTH / 2) + 4, (height / 2) + (GUI_HEIGHT / 2) - 24, 80, 20, I18n.format(Translatable.XpHolder.withdraw));
		buttonList.add(buttonWithdraw);
		
		buttonDeposit = new GuiButton(buttonId++, (width / 2) + (GUI_WIDTH / 2) - 84, (height / 2) + (GUI_HEIGHT / 2) - 24, 80, 20, I18n.format(Translatable.XpHolder.deposit));
		buttonList.add(buttonDeposit);
		
		sliderResponder = new SliderResponder(this);
		sliderFormat = new SliderFormat();
		sliderXp = new GuiSlider(sliderResponder, 0, width / 2 - 75, height / 2 - 8, "xpWithdraw", 0.0f, 100.0f, 0.0f, sliderFormat);
		buttonList.add(sliderXp);
		
		GetExperienceData();
	}

	Experience playerXpAfterDeposit, xpAfterDeposit;
	Experience playerXpAfterWithdraw, xpAfterWithdraw;
	
	@Override
	public void updateScreen() {
		EntityPlayerSP player = Minecraft.getMinecraft().player;
		
		buttonWithdraw.enabled = xpHeld > 0;
		buttonDeposit.enabled = player.experienceTotal > 0 && this.levelsHeld < Properties.General.maxLevelsHeld;
		sliderXp.enabled = xpHeld > 0 || player.experienceTotal > 0;
		
		int selectedXp = (int) (this.xpHeld * (sliderValue / 100f));
		int selectedPlayerXp = (int) (player.experienceTotal * (sliderValue / 100f));

		playerXpAfterDeposit = Experience.GetLevelsFromExperience((int)(player.experienceTotal - selectedPlayerXp));
		xpAfterDeposit = Experience.GetLevelsFromExperience((int)(this.xpHeld + selectedPlayerXp));
		
		xpAfterWithdraw = Experience.GetLevelsFromExperience(this.xpHeld - selectedXp);
		if (xpAfterWithdraw.levelsHeld >= Properties.General.levelCostOnWithdraw)
			xpAfterWithdraw.levelsHeld -= Properties.General.levelCostOnWithdraw;
		playerXpAfterWithdraw = Experience.GetLevelsFromExperience((int)(player.experienceTotal + selectedXp));
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
		Minecraft minecraft = Minecraft.getMinecraft();
		minecraft.getTextureManager().bindTexture(GUI_TEXTURE);
		int offsetScreenLeft = (width - GUI_WIDTH) / 2;
		int offsetScreenTop = (height - GUI_HEIGHT) / 2;
		drawTexturedModalRect(offsetScreenLeft, offsetScreenTop, 0, 0, GUI_WIDTH, GUI_HEIGHT);

		if (minecraft.player.ticksExisted % 5 == 0) {
			GetExperienceData();
		}
		
		DrawTileExperience();
		
		String text;
		int x, y;
		
		isHoverSlider = (mouseX >= sliderXp.x 
				&& mouseY >= sliderXp.y 
				&& mouseX < sliderXp.x + sliderXp.width 
				&& mouseY < sliderXp.y + sliderXp.height);
		
		isHoverWithdraw = (mouseX >= buttonWithdraw.x 
				&& mouseY >= buttonWithdraw.y 
				&& mouseX < buttonWithdraw.x + buttonWithdraw.width 
				&& mouseY < buttonWithdraw.y + buttonWithdraw.height);
		
		isHoverDeposit = (mouseX >= buttonDeposit.x 
				&& mouseY >= buttonDeposit.y 
				&& mouseX < buttonDeposit.x + buttonDeposit.width 
				&& mouseY < buttonDeposit.y + buttonDeposit.height);
		
		if (isHoverWithdraw 
				&& buttonWithdraw.enabled 
				&& sliderValue > 0f 
				&& this.xpHeld > 0f) {
			
			text = I18n.format(Translatable.XpHolder.withdraw) + ":";
			x = (width - fontRenderer.getStringWidth(text)) / 2;
			y = height - 42;
			fontRenderer.drawStringWithShadow(text, x, y, 16777215);
			
			text = I18n.format(Translatable.XpHolder.withdraw_cost, Properties.General.levelCostOnWithdraw);
			if (sliderValue >= 100f)
				text += " " + I18n.format(Translatable.XpHolder.maxWithdraw);
			x = (width - fontRenderer.getStringWidth(text)) / 2;
			y = (height - GUI_HEIGHT) / 2 - 10;
			fontRenderer.drawStringWithShadow(text, x, y, 16777215);
			
			text = String.valueOf(minecraft.player.experienceLevel);
	        x = (width - fontRenderer.getStringWidth(text)) / 2 - 22;
	        y = height - 31;
			DrawStringExperience(text, x, y);
			
			text = "->";
	        x = (width - fontRenderer.getStringWidth(text)) / 2;
	        y = height - 31;
	        DrawStringExperience(text, x, y);
			
			text = String.valueOf(this.playerXpAfterWithdraw.levelsHeld);
	        x = (width - fontRenderer.getStringWidth(text)) / 2 + 22;
	        y = height - 31;
			DrawStringExperience(text, x, y);
			
			text = String.valueOf(this.xpAfterWithdraw.levelsHeld);
	        x = (width - fontRenderer.getStringWidth(text)) / 2 + 22;
	        y = (height / 2) - (GUI_HEIGHT / 2) + 14;
			DrawStringExperience(text, x, y);

			text = "->";
	        x = (width - fontRenderer.getStringWidth(text)) / 2;
	        y = (height / 2) - (GUI_HEIGHT / 2) + 14;
	        fontRenderer.drawString(text, x, y, 0);
			
			text = I18n.format(Translatable.XpHolder.withdraw) + ":";
			x = (width - fontRenderer.getStringWidth(text)) / 2;
			y = (height / 2) - (GUI_HEIGHT / 2) + 4;
	        fontRenderer.drawString(text, x, y, 0);
		}
		
		if (isHoverDeposit 
				&& buttonDeposit.enabled 
				&& sliderValue > 0f 
				&& minecraft.player.experienceTotal > 0f) {
			if (this.xpAfterDeposit.levelsHeld > Properties.General.maxLevelsHeld)
				text = String.valueOf(Properties.General.maxLevelsHeld);
			else
				text = String.valueOf(this.xpAfterDeposit.levelsHeld);
	        x = (width - fontRenderer.getStringWidth(text)) / 2 + 22;
	        y = (height / 2) - (GUI_HEIGHT / 2) + 14;
			DrawStringExperience(text, x, y);

			text = "->";
	        x = (width - fontRenderer.getStringWidth(text)) / 2;
	        y = (height / 2) - (GUI_HEIGHT / 2) + 14;
	        fontRenderer.drawString(text, x, y, 0);
			
			text = I18n.format(Translatable.XpHolder.deposit) + ":";
			x = (width - fontRenderer.getStringWidth(text)) / 2;
			y = (height / 2) - (GUI_HEIGHT / 2) + 4;
	        fontRenderer.drawString(text, x, y, 0);
			
			text = I18n.format(Translatable.XpHolder.deposit) + ":";
			x = (width - fontRenderer.getStringWidth(text)) / 2;
			y = height - 42;
			fontRenderer.drawStringWithShadow(text, x, y, 16777215);
			
			text = String.valueOf(minecraft.player.experienceLevel);
	        x = (width - fontRenderer.getStringWidth(text)) / 2 - 22;
	        y = height - 31;
			DrawStringExperience(text, x, y);
			
			text = "->";
	        x = (width - fontRenderer.getStringWidth(text)) / 2;
	        y = height - 31;
	        DrawStringExperience(text, x, y);
	        
	        if (this.xpAfterDeposit.levelsHeld > Properties.General.maxLevelsHeld) {
				int xpDiff = Experience.GetExperienceFromLevel(Properties.General.maxLevelsHeld);
				int xpToAdd = xpDiff - this.xpHeld;
				text = String.valueOf(Experience.GetLevelsFromExperience(minecraft.player.experienceTotal - xpToAdd).levelsHeld);
	        }
			else
				text = String.valueOf(this.playerXpAfterDeposit.levelsHeld);
	        x = (width - fontRenderer.getStringWidth(text)) / 2 + 22;
	        y = height - 31;
			DrawStringExperience(text, x, y);
		}

		TileEntityXpHolder holder = (TileEntityXpHolder) minecraft.player.world.getTileEntity(new BlockPos(this.x, this.y, this.z));
		if (holder.HasBankUpgrade()) {
			text = I18n.format(Translatable.Upgrade.has_bank);
			x = (width - fontRenderer.getStringWidth(text)) / 2;
			y = (height - GUI_HEIGHT) / 2 - fontRenderer.FONT_HEIGHT;
			fontRenderer.drawStringWithShadow(text, x, y, 16777215);
		}
		else if (holder.HasPickUpUpgrade()) {
			text = I18n.format(Translatable.Upgrade.has_pick_up);
			x = (width - fontRenderer.getStringWidth(text)) / 2;
			y = (height - GUI_HEIGHT) / 2 - fontRenderer.FONT_HEIGHT;
			fontRenderer.drawStringWithShadow(text, x, y, 16777215);
		}
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.equals(buttonClose))
			mc.displayGuiScreen((GuiScreen)null);
		else if (button.equals(buttonWithdraw)){
			PacketHandler.SendToServer(new WithdrawMessage(sliderValue, new BlockPos(x, y, z)));
			GetExperienceData();
		}
		else if (button.equals(buttonDeposit)){
			PacketHandler.SendToServer(new DepositMessage(sliderValue, new BlockPos(x, y, z)));
			GetExperienceData();
		}
	}
	
    @Override
    public boolean doesGuiPauseGame() {
    	return false;
    }
    
    private void GetExperienceData() {
    	PacketHandler.SendToServer(new GetXpHeldMessage(new BlockPos(x, y, z)));
    }
    
    private void DrawTileExperience() {
    	//Draw XP Bar
		int cap = this.xpCap;
        int left = width / 2 - 91;
        
        if (cap > 0)
        {
            short barWidth = 162;
            int filled = (int)(this.currentLevelXp * (float)(barWidth + 1));
            int top = (height / 2) - (GUI_HEIGHT / 2) + 23;
            drawTexturedModalRect((width / 2) - (GUI_WIDTH / 2) + 7, top, 0, GUI_HEIGHT, barWidth, 5);

            if (filled > 0)
            {
                drawTexturedModalRect((width / 2) - (GUI_WIDTH / 2) + 7, top, 0, GUI_HEIGHT + 5, filled, 5);
            }
        }
        
        //Draw Level number
        String text = String.valueOf(this.levelsHeld);
        int x = (width - fontRenderer.getStringWidth(text)) / 2;
        int y = (height / 2) - (GUI_HEIGHT / 2) + 17;
        if (sliderValue > 0f 
        		&& (this.xpHeld > 0 || Minecraft.getMinecraft().player.experienceTotal > 0)
        		&& ((isHoverWithdraw && buttonWithdraw.enabled) || (isHoverDeposit && buttonDeposit.enabled))) {
        	x -= 22;
        	y -= 3;
        }
        DrawStringExperience(text, x, y);
    }
	
	private void DrawStringExperience(String text, int x, int y) {
        fontRenderer.drawString(text, x + 1, y, 0);
        fontRenderer.drawString(text, x - 1, y, 0);
        fontRenderer.drawString(text, x, y + 1, 0);
        fontRenderer.drawString(text, x, y - 1, 0);
        fontRenderer.drawString(text, x, y, EXPERIENCE_COLOR);
	}
	
	public boolean ShouldBarHide(ElementType type) {
		if (type.equals(ElementType.EXPERIENCE) || type.equals(ElementType.HEALTH) || type.equals(ElementType.FOOD) || type.equals(ElementType.ARMOR)) {
			if (sliderValue > 0f 
					&& (this.xpHeld > 0 || Minecraft.getMinecraft().player.experienceTotal > 0) 
					&& ((isHoverWithdraw && buttonWithdraw.enabled) || (isHoverDeposit && buttonDeposit.enabled))) {
				return true;
			}
		}
		return false;
	}
    
    static class ButtonClose extends GuiButton{

		public ButtonClose(int buttonId, int x, int y, String buttonText) {
			super(buttonId, x, y, 11, 11, "");
		}
    	
		@Override
		public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
			if (visible) {
				boolean isButtonHover = (mouseX >= this.x 
					&& mouseY >= this.y 
					&& mouseX < this.x + width 
					&& mouseY < this.y + height);

                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
				mc.getTextureManager().bindTexture(GUI_TEXTURE);
				int textureX = 176;
				int textureY = 0;
				
				if (isButtonHover)
					textureY += 11;
				
				drawTexturedModalRect(this.x, this.y, textureX, textureY, 11, 11);
			}
		}
    }
    
    private class SliderResponder implements GuiResponder{

    	private XpHolderGui gui;
    	
    	public SliderResponder(XpHolderGui gui) {
			this.gui = gui;
		}
    	
		@Override
		public void setEntryValue(int id, boolean value) { }

		@Override
		public void setEntryValue(int id, float value) {
			gui.sliderValue = value;
		}

		@Override
		public void setEntryValue(int id, String value) { }
    	
    }
    
    private class SliderFormat implements GuiSlider.FormatHelper{

		@Override
		public String getText(int id, String name, float value) {
			return I18n.format(Translatable.XpHolder.experience) + ": " + String.format("%.1f", value) + "%";
		}
    }
}
