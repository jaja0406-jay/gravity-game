package io.jbnu.hw;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

public class Block {
    private static Texture TEX; // 단색 사각형 텍스처
    private final Rectangle rect;

    public Block(float x, float y, float w, float h) {
        ensureTex();
        rect = new Rectangle(x, y, w, h);
    }

    public Rectangle getBounds() {
        return rect;
    }

    public void render(SpriteBatch batch) {
        batch.draw(TEX, rect.x, rect.y, rect.width, rect.height);
    }

    private static void ensureTex() {
        if (TEX == null) {
            Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pm.setColor(0.7f, 0.7f, 0.7f, 1f); // 연회색
            pm.fill();
            TEX = new Texture(pm);
            pm.dispose();
        }
    }
}
