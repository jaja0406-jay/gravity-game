package io.jbnu.hw;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;
import java.util.List;
import static io.jbnu.hw.Coin.Dir;

public class GameWorld {

    private enum LevelRule { ANY, MATCH_DIR }
    private Texture bgSky;     // 하늘 배경 (그라디언트 + 구름)
    private Texture doorTex;   // 예쁜 문(라운드/문손잡이 포함)

    // ====== 공개 상태 (HUD) ======
    public int stage = 1;
    public int switchCount = 0;
    public int switchLimit = 0;
    public float timeLeft = 0f;

    // 배너(자동 재시작 안내)
    private String bannerText = "";
    private float  bannerTimer = 0f; // >0 이면 표시

    // ====== 내부 규칙/파라미터 ======
    private LevelRule rule = LevelRule.ANY;
    private int coinsRequired = 10;

    // 물리/중력
    private final Vector2 gravityDir = new Vector2(0, -1);
    private float gravityAccel = 18f;            // L1: 18, L2: 20, L3: 22
    private float gravityCooldown = 0f;
    private static final float GRAVITY_SWITCH_COOLDOWN = 0.15f;

    // ====== 월드/카메라 경계 ======
    private static final float WORLD_MIN_X = 0f;
    private static final float WORLD_MAX_X = 1200f;
    private static final float FLOOR_Y     = 32f;
    private static final float CEILING_Y   = 560f;
    private static final float SIDE_OVERHANG = 80f;

    // 카메라(800x600) — X 추적, Y 고정
    private final OrthographicCamera camera = new OrthographicCamera(800, 600);

    // 오브젝트
    private final List<Block> blocks = new ArrayList<>();
    private final List<Coin>  coins  = new ArrayList<>();
    private ExitDoor door;

    // 그리기 리소스
    private Texture px;                // 1x1 흰 픽셀(문 색칠용)
    private final Texture texPlayer = new Texture("t.png");
    private final Texture texCoin   = new Texture("coin.png");

    private final Player player;
    private int collected = 0;
    private static final float STAGE3_SWEEP_EPS = 6f;

    // 시작 위치(재시작 시 여기로)
    private static final Vector2 START_POS = new Vector2(80, 160);

    public GameWorld() {
        camera.position.set(400, 300, 0);
        camera.update();

        // 1x1 흰 텍스처(문/버튼 등 사각형 칠하기용)
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        px = new Texture(pm);
        pm.dispose();

        player = new Player(new Vector2(START_POS));
        player.setTexture(texPlayer);
        // [추가] 하늘/문 텍스처 생성 (외부 파일 필요 없음)
        bgSky  = createSkyTexture(1024, 600);
        doorTex = createDoorTexture(48, 64);
    }

    // ====== 스테이지 구성 ======
    public void configureStage(int stageNo) {
        stage = Math.max(1, Math.min(3, stageNo));
        blocks.clear();
        coins.clear();
        door = null;
        collected = 0;
        switchCount = 0;
        timeLeft = 0f;
        gravityDir.set(0, -1);
        bannerText = "";
        bannerTimer = 0f;

        if (stage == 1) {
            // L1: 2단 배치, 코인 랜덤, 제약 없음
            coinsRequired = 10;
            rule = LevelRule.ANY;
            gravityAccel = 16f;     // ★ L1 가속
            switchLimit = 0;
            buildLevel1();

        } else if (stage == 2) {
            // ★ 교체: L2 = 타임 60초 제한(ANY), 스위치 제한 없음
            coinsRequired = 10;
            rule = LevelRule.ANY;
            gravityAccel = 18f;     // ★ L2 가속
            switchLimit = 0;        // ★ 스위치 제한 없음
            timeLeft = 30f;         // ★ 타임 제한 30초
            buildLevel2();

        } else {
            // ★ 교체: L3 = MATCH_DIR + 스위치 제한(20회)
            coinsRequired = 10;
            rule = LevelRule.ANY;
            gravityAccel = 20f;     // ★ L3 가속
            switchLimit = 20;       // ★ 스위치 제한
            timeLeft = 0f;          // ★ 시간 제한 없음
            buildLevel3();
        }

        // 플레이어 / 카메라 초기 위치
        player.setPosition(START_POS.x, START_POS.y);
        camera.position.x = 400f;
        camera.position.y = 300f;
        camera.update();
        // ★ 스테이지 시작 배너 추가
        bannerText  = "Stage " + stage + " Start!";
        bannerTimer = 2.5f;   // 2.5초 동안 표시
    }

    // ---- 블록 2단 + 코인 랜덤
    private void buildLevel1() {
        blocks.clear();
        blocks.add(new Block(0, 0, 1200, 32));
        blocks.add(new Block(140, 140, 300, 20));
        blocks.add(new Block(520, 240, 280, 20));
        spawnRandomCoinsAnyDir(13, 40, 1160, 80, 520);
        door = new ExitDoor(1120, 48, 48, 64);
        door.setColor(new Color(0.16f, 0.55f, 1f, 1f));
        door.setTexture(doorTex);                 // [추가] 문 이미지 적용
    }

    // ---- 블록 3단 + 코인 랜덤  ★이제 타임 제한 스테이지
    private void buildLevel2() {
        blocks.clear();
        blocks.add(new Block(0, 0, 1200, 32));
        blocks.add(new Block(120, 140, 160, 20));  // 좌
        blocks.add(new Block(520, 240, 160, 20));  // 우 멀리
        blocks.add(new Block(860, 340, 160, 20));  // 더 우측
        spawnRandomCoinsAnyDir(14, 40, 1160, 80, 520);
        door = new ExitDoor(1120, 48, 48, 64);
        door.setColor(new Color(0.16f, 0.55f, 1f, 1f));
        door.setTexture(doorTex);                 // [추가] 문 이미지 적용
    }

    // ---- 블록 4단 + 방향 일치 코인 랜덤  ★이제 스위치 제한 스테이지
    private void buildLevel3() {
        blocks.clear();
        blocks.add(new Block(0, 0, 1200, 32));
        // 오른쪽 상승
        blocks.add(new Block(220, 120, 140, 20));  // A
        blocks.add(new Block(520, 210, 140, 20));  // B
        blocks.add(new Block(820, 300, 140, 20));  // C
        // 왼쪽 되돌림(돌다리)
        blocks.add(new Block(700, 360, 90, 20));   // D
        blocks.add(new Block(560, 390, 100, 20));  // E
        blocks.add(new Block(420, 420, 100, 20));  // F
        // 최상단 마무리
        blocks.add(new Block(980, 440, 160, 20));  // G
        spawnRandomCoinsAnyDir(16, 40, 1160, 80, 520);
        door = new ExitDoor(1120, 48, 48, 64);
        door.setColor(new Color(0.16f, 0.55f, 1f, 1f));
        door.setTexture(doorTex);                 // [추가] 문 이미지 적용
    }


    public void update(float delta) {
        // 배너/쿨다운 유지
        if (bannerTimer > 0f) bannerTimer -= delta;
        if (gravityCooldown > 0f) gravityCooldown -= delta;

        // L2 타이머: 0이면 자동 재시작
        if (stage == 2 && timeLeft > 0f) {
            timeLeft -= delta;
            if (timeLeft <= 0f) {
                timeLeft = 0f;
                triggerRestart("Stage 2 restart! (time)");
                return;
            }
        }

        // ---- 서브스텝 설정: L3만 3회, 그 외 1회 ----
        final int SUB_STEPS = (stage == 3 ? 3 : 1);   // ★ 레벨3에서만 3등분
        final float subDt   = delta / SUB_STEPS;

        for (int s = 0; s < SUB_STEPS; s++) {
            // 중력 적용(서브 스텝 dt 사용)
            player.addVelocity(gravityDir.x * gravityAccel * subDt,
                gravityDir.y * gravityAccel * subDt);

            // 이전 박스(스윕용) 백업
            Rectangle prevPB = new Rectangle(player.getBounds());

            // 이동
            player.integrate(subDt);

            // 충돌(최소축 분리)
            Rectangle pb = player.getBounds();
            for (Block b : blocks) {
                Rectangle br = b.getBounds();
                if (!pb.overlaps(br)) continue;

                float overlapX = Math.min(pb.x + pb.width,  br.x + br.width)  - Math.max(pb.x, br.x);
                float overlapY = Math.min(pb.y + pb.height, br.y + br.height) - Math.max(pb.y, br.y);
                if (overlapX <= 0 || overlapY <= 0) continue;

                if (overlapX < overlapY) {
                    if (pb.x + pb.width * 0.5f < br.x + br.width * 0.5f) pb.x -= overlapX;
                    else pb.x += overlapX;
                    player.setPosition(pb.x, pb.y);
                } else {
                    if (pb.y + pb.height * 0.5f < br.y + br.height * 0.5f) pb.y -= overlapY;
                    else pb.y += overlapY;
                    player.setPosition(pb.x, pb.y);
                }
            }

            // 월드 경계 클램프(위/아래 고정, 좌/우 약간 오버행 허용)
            float px = player.getX();
            float py = player.getY();
            float ph = player.getBounds().height;

            if (py < FLOOR_Y) py = FLOOR_Y;
            if (py + ph > CEILING_Y) py = CEILING_Y - ph;

            float minX = WORLD_MIN_X - SIDE_OVERHANG;
            float maxX = WORLD_MAX_X + SIDE_OVERHANG - player.getBounds().width;
            if (px < minX) px = minX;
            if (px > maxX) px = maxX;

            player.setPosition(px, py);

            // 코인/문 처리 (스윕 포함 보정)
            checkCoinCollection(prevPB, player.getBounds());
            // 문은 스텝마다 체크해도 부작용 없음
            checkExit();
        }

        // 카메라: X 추적(+클램프), Y 고정 (기존 로직 유지)
        Rectangle pb = player.getBounds();
        float targetX = player.getX() + pb.width * 0.5f;
        camera.position.x += (targetX - camera.position.x) * 0.12f;

        float halfW = camera.viewportWidth * 0.5f;
        float camMin = (WORLD_MIN_X - SIDE_OVERHANG) + halfW;
        float camMax = (WORLD_MAX_X + SIDE_OVERHANG) - halfW;
        if (camMax < camMin) camMax = camMin;
        if (camera.position.x < camMin) camera.position.x = camMin;
        if (camera.position.x > camMax) camera.position.x = camMax;

        camera.position.y = 300f;
        camera.update();
    }

    public void draw(SpriteBatch batch) {
        batch.setProjectionMatrix(camera.combined);
        float bgX = camera.position.x - camera.viewportWidth * 0.5f * 0.98f;
        float bgY = camera.position.y - camera.viewportHeight * 0.5f;
        batch.draw(bgSky, bgX, bgY, camera.viewportWidth * 0.98f, camera.viewportHeight);
        Color old = batch.getColor();
        batch.setColor(Color.WHITE);

        for (Block b : blocks) b.render(batch);
        for (Coin c : coins)  c.render(batch);
        if (door != null)     door.render(batch, px);
        player.render(batch);

        batch.setColor(old);
    }

    // ====== 입력 연동 ======
    /** 0:UP, 1:DOWN, 2:LEFT, 3:RIGHT */
    public void trySetGravity(int dir) {
        if (gravityCooldown > 0f) return;
        switch (dir) {
            case 0: gravityDir.set(0, +1); break;
            case 1: gravityDir.set(0, -1); break;
            case 2: gravityDir.set(-1, 0); break;
            case 3: gravityDir.set(+1, 0); break;
            default: return;
        }
        gravityCooldown = GRAVITY_SWITCH_COOLDOWN;

        // ★ 교체 반영: L3 = 스위치 제한 스테이지 → 초과 시 자동 재시작
        if (stage == 3 && switchLimit > 0) {
            switchCount++;
            if (switchCount > switchLimit) {
                triggerRestart("Stage 3 restart! (switches)");
            }
        }
    }

    public void onPlayerLeft() {
        float speed = 160f;
        player.addVelocity(-speed * com.badlogic.gdx.Gdx.graphics.getDeltaTime(), 0);
    }

    public void onPlayerRight() {
        float speed = 160f;
        player.addVelocity(+speed * com.badlogic.gdx.Gdx.graphics.getDeltaTime(), 0);
    }

    // ★ 레벨3: prevDir/currDir 둘 중 하나라도 맞으면 수집 인정
    private void checkCoinCollection(Rectangle prevPB, Rectangle currPB) {
        Coin.Dir currDir = currentCoinDir();

        // prevDir 계산: prevPB 기준으로 "이전 위치에서의" 중력 방향을 추정
        // (이번 프레임 중 중력은 바뀌지 않으므로 currDir과 같을 때가 대부분이지만
        //  입력 타이밍 이슈를 대비해 prevDir도 동일하게 허용)
        Coin.Dir prevDir = currDir;

        // 기본: 현재 박스로 1차 판정
        for (Coin c : coins) {
            if (c.isCollected()) continue;

            boolean ok = (rule == LevelRule.ANY)
                ? c.tryCollect(currPB, null)
                : ( c.tryCollect(currPB, currDir) || (stage == 3 && c.tryCollect(currPB, prevDir)) );

            if (ok) { collected++; }
        }

        // 레벨3만 보정: prev 박스 → 스윕 박스
        if (stage == 3) {
            // prev 박스 시도 (방향은 prev/curr 둘 다 허용)
            for (Coin c : coins) {
                if (c.isCollected()) continue;

                boolean okPrev = (rule == LevelRule.ANY)
                    ? c.tryCollect(prevPB, null)
                    : ( c.tryCollect(prevPB, currDir) || c.tryCollect(prevPB, prevDir) );

                if (okPrev) { collected++; }
            }

            // 스윕 박스(ε 확장) 시도
            final float EPS = 8f;
            float sx = Math.min(prevPB.x, currPB.x);
            float sy = Math.min(prevPB.y, currPB.y);
            float sw = Math.max(prevPB.x + prevPB.width,  currPB.x + currPB.width)  - sx;
            float sh = Math.max(prevPB.y + prevPB.height, currPB.y + currPB.height) - sy;
            Rectangle sweepRect = new Rectangle(sx - EPS, sy - EPS, sw + EPS*2f, sh + EPS*2f);

            for (Coin c : coins) {
                if (c.isCollected()) continue;

                boolean okSweep = (rule == LevelRule.ANY)
                    ? c.tryCollect(sweepRect, null)
                    : ( c.tryCollect(sweepRect, currDir) || c.tryCollect(sweepRect, prevDir) );

                if (okSweep) { collected++; }
            }
        }
    }
    private void checkExit() {
        if (collected < coinsRequired || door == null) return;
        if (player.getBounds().overlaps(door.getBounds())) {
            if (stage < 3) configureStage(stage + 1);
            else {
                bannerText = "Clear!";   // ★ 클리어 문구 표시
                bannerTimer = 9999f;     // 계속 유지
            }
        }
    }

    private Coin.Dir currentCoinDir() {
        if (Math.abs(gravityDir.x) > Math.abs(gravityDir.y)) {
            return gravityDir.x > 0 ? Dir.RIGHT : Dir.LEFT;
        } else {
            return gravityDir.y > 0 ? Dir.UP : Dir.DOWN;
        }
    }

    public int getScore() { return collected; }

    // 배너 노출용
    public String getBannerText() { return bannerText; }
    public float  getBannerTime() { return bannerTimer; }

    // 조건 위반 시 공통 재시작 (현재 스테이지 처음부터)
    private void triggerRestart(String msg) {
        bannerText  = msg;
        bannerTimer = 2.2f;
        configureStage(stage);
    }

    private void spawnRandomCoinsAnyDir(int count, float minX, float maxX, float minY, float maxY) {
        final float minDist = 56f;          // 코인끼리 최소 간격
        final int   triesPerCoin = 300;     // 코인 하나당 최대 시도

        // y를 두 구간(하/상)으로 분할해서 반반 스폰
        float midY  = (minY + maxY) * 0.5f;
        float gap   = 24f;                  // 중간 빈틈(상/하 구간이 겹치지 않게)
        float lowMinY  = minY;
        float lowMaxY  = Math.max(minY, midY - gap);
        float highMinY = Math.min(maxY, midY + gap);
        float highMaxY = maxY;

        List<Rectangle> placed = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            boolean placedOk = false;
            for (int t = 0; t < triesPerCoin && !placedOk; t++) {
                // 짝수: 아래 구간, 홀수: 위 구간
                boolean useLower = (i % 2 == 0);

                float x = MathUtils.random(minX, maxX);
                float y = useLower
                    ? MathUtils.random(lowMinY,  lowMaxY)
                    : MathUtils.random(highMinY, highMaxY);

                Coin candidate = new Coin(texCoin, new Vector2(x, y));
                Rectangle cb = candidate.getBounds();

                // 1) 블록과 겹치면 탈락
                boolean overlapBlock = false;
                for (Block b : blocks) {
                    if (cb.overlaps(b.getBounds())) { overlapBlock = true; break; }
                }
                if (overlapBlock) continue;

                // 2) 이미 배치된 코인들과 최소 거리 유지
                boolean tooClose = false;
                for (Rectangle r : placed) {
                    float dx = (cb.x + cb.width*0.5f)  - (r.x + r.width*0.5f);
                    float dy = (cb.y + cb.height*0.5f) - (r.y + r.height*0.5f);
                    if (dx*dx + dy*dy < minDist*minDist) { tooClose = true; break; }
                }
                if (tooClose) continue;

                // 3) ★ 문 영역과도 겹치지 않게 (손잡이가 코인처럼 보이는 현상 방지)
                if (door != null && cb.overlaps(door.getBounds())) continue;

                coins.add(candidate);
                placed.add(new Rectangle(cb));
                placedOk = true;
            }

            // 최후 보정: 위/아래 중 현재 구간의 시작점에라도 한 개
            if (!placedOk) {
                boolean useLower = (i % 2 == 0);
                float y = useLower ? lowMinY : highMinY;
                Coin fallback = new Coin(texCoin, new Vector2(minX, y));
                // 그래도 문/블록/코인과 겹치면 그냥 스킵
                Rectangle cb = fallback.getBounds();
                boolean bad = false;
                if (door != null && cb.overlaps(door.getBounds())) bad = true;
                if (!bad) {
                    for (Block b : blocks) { if (cb.overlaps(b.getBounds())) { bad = true; break; } }
                }
                if (!bad) {
                    for (Rectangle r : placed) {
                        float dx = (cb.x + cb.width*0.5f)  - (r.x + r.width*0.5f);
                        float dy = (cb.y + cb.height*0.5f) - (r.y + r.height*0.5f);
                        if (dx*dx + dy*dy < minDist*minDist) { bad = true; break; }
                    }
                }
                if (!bad) {
                    coins.add(fallback);
                    placed.add(new Rectangle(cb));
                }
            }
        }
    }


    private boolean coinOverlapsBlocks(Coin c) {
        Rectangle cb = c.getBounds();
        for (Block b : blocks) {
            if (cb.overlaps(b.getBounds())) return true;
        }
        return false;
    }
    // ★ 외부에서 배너 강제 표시용
    public void showBanner(String msg) {
        bannerText = msg;
        bannerTimer = 2.0f; // 2초 동안 표시
    }

    public void dispose() {
        texPlayer.dispose();
        texCoin.dispose();
        if (px != null) px.dispose();
        if (doorTex != null) doorTex.dispose();   // [추가]
        if (bgSky   != null) bgSky.dispose();     // [추가]
    }
    // [추가] 하늘 배경 생성: 위는 진한 하늘색, 아래는 밝은 하늘색 + 부드러운 구름
    private Texture createSkyTexture(int w, int h) {
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        // 수직 그라디언트
        for (int y = 0; y < h; y++) {
            float t = (float)y / (h - 1);
            float r = (1f - t) * 0.45f + t * 0.80f; // 위쪽 파랑 → 아래쪽 연한 하늘
            float g = (1f - t) * 0.70f + t * 0.92f;
            float b = (1f - t) * 0.95f + t * 1.00f;
            pm.setColor(r, g, b, 1f);
            pm.drawLine(0, y, w, y);
        }
        // 구름(밝은 타원 몇 개)
        pm.setColor(1f, 1f, 1f, 0.25f);
        for (int i = 0; i < 14; i++) {
            int cx = MathUtils.random(0, w);
            int cy = MathUtils.random(h/4, h - 40);
            int rx = MathUtils.random(50, 150);
            int ry = MathUtils.random(18, 40);
            pm.fillCircle(cx, cy, rx);
            pm.setColor(1f, 1f, 1f, 0.18f);
            pm.fillCircle(cx + rx/2, cy + ry/3, (int)(rx*0.7f));
            pm.setColor(1f, 1f, 1f, 0.25f);
        }
        Texture t = new Texture(pm);
        pm.dispose();
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }

    // 문 텍스처: 단순 패널 + 테두리 + 손잡이 1개 (겹쳐 보이는 원호 제거)
    private Texture createDoorTexture(int w, int h) {
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pm.setBlending(Pixmap.Blending.SourceOver);

        // 문틀
        pm.setColor(0.16f, 0.80f, 0.78f, 1f);              // 민트 테두리
        pm.fillRectangle(0, 0, w, h);
        pm.setColor(1f, 1f, 1f, 0.22f);
        pm.fillRectangle(2, 2, w - 4, h - 4);

        // 문짝(라운드 사각 느낌을 단순화: 그냥 한 톤)
        int inset = 6;
        pm.setColor(0.16f, 0.55f, 1f, 1f);                 // 파란 문짝
        pm.fillRectangle(inset, inset, w - 2 * inset, h - 2 * inset);

        // ★ 손잡이 1개만 명확히
        pm.setColor(0.98f, 0.82f, 0.12f, 1f);              // 골드
        int knobX = w - inset - 7;                         // 오른쪽에 배치
        int knobY = h / 2 - 1;
        pm.fillCircle(knobX, knobY, 4);

        Texture t = new Texture(pm);
        pm.dispose();
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest); // 번짐 방지
        return t;
    }
}
