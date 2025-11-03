package io.jbnu.hw;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

/** 흰 배경에서도 확실히 보이는 사각형 문 */
public class ExitDoor {
    private final Rectangle bounds;
    private Color color = new Color(0.16f, 0.55f, 1f, 1f); // 밝은 파랑
    private Texture texture;
    public void setTexture(Texture tex) {
        this.texture = tex;
    }
    public ExitDoor(float x, float y, float w, float h) {
        this.bounds = new Rectangle(x, y, w, h);
    }
    public Rectangle getBounds() { return bounds; }
    public void setColor(Color c) { this.color.set(c); }

    public void render(SpriteBatch batch, Texture px) {
        Color old = batch.getColor();
        batch.setColor(color);
        batch.draw(px, bounds.x, bounds.y, bounds.width, bounds.height);
        if (texture != null) {
            batch.setColor(Color.WHITE); // 색 영향 제거
            batch.draw(texture, bounds.x, bounds.y, bounds.width, bounds.height);
        }
        batch.setColor(old);
    }
}
