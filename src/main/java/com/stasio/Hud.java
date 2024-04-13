package com.stasio;

import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;

import static com.stasio.ProjectOne.characterHealth;
import static com.stasio.ProjectOne.score;

public class Hud {
    private BitmapFont guiFont;
    private Node guiNode;
    private BitmapText hudText;
    private BitmapText YouLostText;

    public Hud(BitmapFont guiFont, Node guiNode) {
        this.guiFont = guiFont;
        this.guiNode = guiNode;
        hudText = new BitmapText(guiFont, false);
        YouLostText = new BitmapText(guiFont, false);
    }

    public void UIBuilder() {
        hudText.setSize(guiFont.getCharSet().getRenderedSize());      // font size
        hudText.setColor(ColorRGBA.Red);                             // font color
        hudText.setText("score: " + (int)score/100 + "\n" + "Character health: " + characterHealth);             // the text
        hudText.setLocalTranslation(300, 400, 0); // position
        guiNode.attachChild(hudText);
    }
    public void decreseHealth(){
        hudText.setText("score: " + (int)score/100 + "\n" + "Character health: " + characterHealth--);
    }
    public void YouLostText(){
        hudText.setSize(guiFont.getCharSet().getRenderedSize());      // font size
        hudText.setColor(ColorRGBA.Red);                             // font color
        hudText.setText("You lost");             // the text
        hudText.setLocalTranslation(300, 400, 0); // position
        guiNode.attachChild(hudText);
    }
}
