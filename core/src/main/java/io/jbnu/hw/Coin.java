package io.jbnu.hw;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/**
 * 코인 객체 (현재 프로젝트에서는 방향 제약 없이 '겹치면 수집')
 * - Dir는 외부 참조 호환을 위해 남겨둠(실사용 X)
 */
public class Coin {

    /** 외부 코드 호환용(현재 판정에는 사용하지 않음) */
    public enum Dir { UP, DOWN, LEFT, RIGHT }

    private final Texture texture;      // 코인 텍스처
    private final Vector2 pos;          // 좌하단 좌표(월드)
    private final float size = 24f;     // 렌더/충돌 크기
    private final Rectangle bounds = new Rectangle(); // 충돌 사각형
    private boolean collected = false;                // 수집 여부

    /** 제한 없는 코인 */
    public Coin(Texture texture, Vector2 pos) {
        this.texture = texture;
        this.pos = new Vector2(pos);
        updateBounds();
    }

    /** 호환용 생성자(방향 인자는 무시). 기존 코드 변경 없이 빌드 가능하게 유지 */
    public Coin(Texture texture, Vector2 pos, Dir ignored) {
        this(texture, pos);
    }

    public boolean isCollected() { return collected; }

    /** 현재는 '겹치면 수집'만 수행 (Dir 인자는 무시) */
    public boolean tryCollect(Rectangle playerBounds, Dir currentGravityDirOrNull) {
        if (collected) return false;
        if (!playerBounds.overlaps(bounds)) return false;
        collected = true;
        return true;
    }

    public void render(SpriteBatch batch) {
        if (!collected) {
            batch.draw(texture, pos.x, pos.y, size, size);
        }
    }

    private void updateBounds() {
        bounds.set(pos.x, pos.y, size, size);
    }

    /** 충돌 판정용 Bounds(외부에서 사용) */
    public Rectangle getBounds() { return bounds; }

    /** 디버그/거리 계산용 중심 좌표 */
    public Vector2 getPosition() {
        return new Vector2(bounds.x + bounds.width * 0.5f,
            bounds.y + bounds.height * 0.5f);
    }
}
