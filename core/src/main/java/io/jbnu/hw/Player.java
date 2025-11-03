package io.jbnu.hw;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class Player {
    private final Vector2 pos;
    private final Vector2 vel = new Vector2();
    private Texture texture;
    private float width = 32f;
    private float height = 32f;
    private final Rectangle bounds = new Rectangle();

    public Player(Vector2 startPos) {
        this.pos = new Vector2(startPos);
        updateBounds();
    }

    /** GameWorld에서 호출하는 메서드 */
    public void setTexture(Texture texture) {
        this.texture = texture;
        // 텍스처 크기를 그대로 쓰고 싶다면 주석 해제
        // if (texture != null) {
        //     width = texture.getWidth();
        //     height = texture.getHeight();
        // }
        updateBounds();
    }

    public void addVelocity(float vx, float vy) {
        vel.add(vx, vy);
    }

    public void stop() {
        vel.setZero();
    }

    /** GameWorld.update()에서 호출: 속도를 위치에 반영 */
    public void integrate(float delta) {
        // vel은 이미 delta가 반영된 값으로 더해지므로 여기서 추가로 곱하지 않습니다.
        pos.add(vel);
        // 가벼운 감쇠(원하면 제거)
        vel.scl(0.98f);
        updateBounds();
    }

    public void setPosition(float x, float y) {
        pos.set(x, y);
        updateBounds();
    }

    public Vector2 getPosition() {
        return pos;
    }

    public float getX() { return pos.x; }
    public float getY() { return pos.y; }

    public Rectangle getBounds() {
        return bounds;
    }

    public void render(SpriteBatch batch) {
        if (texture != null) {
            batch.draw(texture, pos.x, pos.y, width, height);
        }
    }

    private void updateBounds() {
        bounds.set(pos.x, pos.y, width, height);
    }
}
