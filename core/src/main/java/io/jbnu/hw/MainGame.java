package io.jbnu.hw;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;      // ★ 1x1 버튼 배경용
import com.badlogic.gdx.graphics.Texture;    // ★ 1x1 버튼 배경용
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;


public class MainGame extends ApplicationAdapter {

    private GameWorld world;
    private SpriteBatch batch;
    private BitmapFont font;
    private GameState state;

    // ★ 화면 고정용 카메라
    private OrthographicCamera uiCamera;
    private int vpW = 800, vpH = 600;

    // ★ 버튼용 1x1 텍스처(반투명 사각형 그릴 때 사용)
    private Texture px;

    // ★ 버튼 배치(오른쪽 상단)
    private float BTN_W = 140, BTN_H = 36;
    private float btnPauseX, btnPauseY;     // [Pause/Resume]
    private float btnRestartX, btnRestartY; // [Restart]
    // [추가] UI 버튼 영역 (화면 고정 좌표, uiCamera 기준)
    private Texture iconPause, iconPlay, iconRestart;
    private final com.badlogic.gdx.math.Rectangle btnPause = new com.badlogic.gdx.math.Rectangle(0,0,36,36);
    private final com.badlogic.gdx.math.Rectangle btnRestart = new com.badlogic.gdx.math.Rectangle(0,0,36,36);

    @Override
    public void create() {
        batch = new SpriteBatch();
        font  = new BitmapFont();
        font.getData().setScale(2f);      // ★ 글자 크게
        font.setColor(Color.BLACK);         // ★ 흰 배경 대비

        // ★ 1x1 흰 텍스처 생성(버튼 배경)
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        px = new Texture(pm);
        pm.dispose();

        world = new GameWorld();     // ★ 다른 클래스 변경 없음
        world.configureStage(1);
        state = GameState.RUNNING;

        // ★ HUD 카메라
        uiCamera = new OrthographicCamera();
        uiCamera.setToOrtho(false, vpW, vpH);
        uiCamera.update();
        iconPause   = makePauseIcon(24, 24);     // ⏸
        iconPlay    = makePlayIcon(24, 24);      // ▶
        iconRestart = makeRestartIcon(24, 24);   // ↻

        layoutButtons();             // ★ 버튼 위치 계산
        btnPause.set(vpW - 36 - 12, vpH - 36 - 12, 36, 36);
        btnRestart.set(vpW - 36 - 12 - 42, vpH - 36 - 12, 36, 36);
    }

    @Override
    public void resize(int width, int height) {
        vpW = Math.max(1, width);
        vpH = Math.max(1, height);
        if (uiCamera != null) {
            uiCamera.setToOrtho(false, vpW, vpH);
            uiCamera.update();
        }
        btnPause.set(vpW - 36 - 12, vpH - 36 - 12, 36, 36);
        btnRestart.set(vpW - 36 - 12 - 42, vpH - 36 - 12, 36, 36);
        layoutButtons(); // ★ 화면 크기 바뀌면 버튼도 재배치
    }

    // ★ 버튼을 오른쪽 상단에 고정 배치
    private void layoutButtons() {
        float margin = 14f;
        btnPauseX   = vpW - margin - BTN_W;
        btnPauseY   = vpH - margin - BTN_H;               // 최우측 상단
        btnRestartX = vpW - margin - BTN_W;
        btnRestartY = btnPauseY - (BTN_H + 10f);          // 그 아래
    }

    @Override
    public void render() {
        handleInputKeys();   // ★ 키 입력은 기존대로 유지

        if (state == GameState.RUNNING) {
            world.update(Gdx.graphics.getDeltaTime());
        }

        // ★ 배경 흰색
        Gdx.gl.glClearColor(1f, 1f, 1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 1) 월드 렌더(월드 카메라 내부에서 처리)
        batch.begin();
        world.draw(batch);
        batch.end();

        // 2) HUD/버튼(화면 고정 좌표)
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        drawHUD();
        drawButtons();
        drawBannerIfAny();
        batch.end();

        // 3) 마우스 클릭 처리(버튼)
        handleMouseClicks();
    }


    /** ★ 키 입력 처리(ESC/ENTER/방향키/A,D) — 요청 반영: Clear 상태 단축키는 일시정지와 무관하게 동작 */
    private void handleInputKeys() {
        // ESC: 일시정지/재개
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            state = (state == GameState.RUNNING) ? GameState.PAUSED : GameState.RUNNING;
        }
        // ENTER: 재개
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            state = GameState.RUNNING;
        }

        // ★ Clear 상태 단축키(R: 현재 스테이지 리스타트 / Q: 종료)
        //    - 배너 텍스트가 "Clear!" 로 시작하면 동작 (일시정지 여부와 무관)
        if (world.stage == 3 && world.getBannerText() != null
            && world.getBannerText().startsWith("Clear")) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                // ★ 요구사항: 전체(1스테이지) 초기화가 아니라 "현재 스테이지" 재시작
                world.configureStage(1);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
                Gdx.app.exit();
            }
        }

        // 일시정지 중이면 아래 이동/중력 입력은 무시
        if (state != GameState.RUNNING) return;

        // 방향키: 중력 전환
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP))    world.trySetGravity(0);
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN))  world.trySetGravity(1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT))  world.trySetGravity(2);
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) world.trySetGravity(3);

        // A / D: 좌우 이동 가속
        if (Gdx.input.isKeyPressed(Input.Keys.A)) world.onPlayerLeft();
        if (Gdx.input.isKeyPressed(Input.Keys.D)) world.onPlayerRight();
    }

    /** ★ HUD(왼쪽 상단 고정). 레벨2/3 교체에 맞춰 표기 변경 */
    private void drawHUD() {
        final float x = 20f;
        final float yTop = vpH - 20f;
        final float line = 30f;
        // [추가] 반투명 패널
        Color old = batch.getColor();
        batch.setColor(1f, 1f, 1f, 0.65f);
        // 하얀 패널(살짝 투명) — 좌상단 HUD영역
        // px 텍스처가 없으니 글씨만 깔끔하게: 패널은 생략하고 글자만 쓰고 싶으면 이 블록을 주석 처리
        batch.setColor(old);

        font.draw(batch, "Stage: " + world.stage,                x, yTop);
        font.draw(batch, "Coins: " + world.getScore() + " / 10", x, yTop - line);

        // ★ 교체 반영: 이제 Stage 2는 타임 제한, Stage 3는 스위치 제한
        if (world.stage == 2 && world.timeLeft > 0) {
            font.draw(batch, "Time: " + (int) world.timeLeft,    x, yTop - line * 2);
        }
        if (world.stage == 3) {
            font.draw(batch, "Switches: " + world.switchCount + " / " + world.switchLimit,
                x, yTop - line * 3);
        }

        if (state == GameState.PAUSED) {
            font.draw(batch, "[PAUSED] ENTER to Resume",         x, yTop - line * 6);
        }
        ;

        // ★ 게임 클리어 처리 (Stage 3 이후)
        if (world.stage == 3 && world.getScore() >= 10 && world.getBannerText().equals("Clear!")) {
            font.draw(batch, "CLEAR!", vpW * 0.5f - 40, vpH * 0.5f + 20);
            font.draw(batch, "[R] Restart    [Q] Quit", vpW * 0.5f - 80, vpH * 0.5f - 20);
        }
    }

    /** ★ 버튼 렌더링(오른쪽 상단) */
    private void drawButtons() {
        // Pause/Resume 버튼 배경
        batch.setColor(0f, 0f, 0f, 0.12f);
        batch.draw(px, btnPauseX, btnPauseY, BTN_W, BTN_H);
        // Restart 버튼 배경
        batch.draw(px, btnRestartX, btnRestartY, BTN_W, BTN_H);
        batch.setColor(Color.WHITE);

        Texture pp = (state == GameState.RUNNING) ? iconPause : iconPlay;
        batch.draw(pp,          btnPauseX   + 8f, btnPauseY   + 6f, 24f, 24f);
        batch.draw(iconRestart, btnRestartX + 8f, btnRestartY + 6f, 24f, 24f);

        // 라벨
        //String pauseLabel = (state == GameState.RUNNING) ? "Pause" : "Resume";
        //font.draw(batch, pauseLabel, btnPauseX + 12f, btnPauseY + BTN_H - 10f);
        //font.draw(batch, "Restart",  btnRestartX + 12f, btnRestartY + BTN_H - 10f);
    }

    /** ★ 자동 재시작 배너(예: Stage 3 restart!) */
    private void drawBannerIfAny() {
        if (world.getBannerTime() <= 0f) return;
        String msg = world.getBannerText();
        if (msg == null || msg.isEmpty()) return;

        float bw = Math.min(vpW * 0.8f, 520f);
        float bh = 40f;
        float bx = (vpW - bw) * 0.5f;
        float by = vpH - 120f;

        batch.setColor(0f, 0f, 0f, 0.15f);
        batch.draw(px, bx, by, bw, bh);
        batch.setColor(Color.WHITE);

        font.draw(batch, msg, bx + 14f, by + bh - 10f);
    }

    /** ★ 마우스 클릭으로 버튼 동작 */
    private void handleMouseClicks() {
        if (!Gdx.input.justTouched()) return;

        float mx = Gdx.input.getX();
        float my = vpH - Gdx.input.getY(); // y 뒤집기

        // Pause/Resume
        if (mx >= btnPauseX && mx <= btnPauseX + BTN_W &&
            my >= btnPauseY && my <= btnPauseY + BTN_H) {
            state = (state == GameState.RUNNING) ? GameState.PAUSED : GameState.RUNNING;
            return;
        }

        // Restart (현재 스테이지 처음부터)
        if (mx >= btnRestartX && mx <= btnRestartX + BTN_W &&
            my >= btnRestartY && my <= btnRestartY + BTN_H) {
            world.configureStage(world.stage);   // ★ 현재 스테이지 리스타트
            world.showBanner("Restart!");
            state = GameState.RUNNING;
        }

    }
    // ⏸ Pause : 세로 막대 2개
    private Texture makePauseIcon(int w, int h) {
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pm.setColor(0f, 0f, 0f, 1f);
        int barW = Math.max(3, w / 5);
        int gap  = barW;
        int x1 = (w - (barW*2 + gap)) / 2;
        int x2 = x1 + barW + gap;
        pm.fillRectangle(x1, 3, barW, h - 6);
        pm.fillRectangle(x2, 3, barW, h - 6);
        Texture t = new Texture(pm);
        pm.dispose();
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return t;
    }

    // ▶ Play 아이콘 (깔끔한 꽉 찬 삼각형)
    private Texture makePlayIcon(int w, int h) {
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pm.setBlending(Pixmap.Blending.SourceOver);

        // 투명 배경
        pm.setColor(0f, 0f, 0f, 0f);
        pm.fill();

        // 삼각형(오른쪽 방향)
        pm.setColor(0f, 0f, 0f, 1f);
        int pad = Math.max(2, w / 8);   // 가장자리 여백
        int x1 = pad,     y1 = pad;         // 좌상
        int x2 = pad,     y2 = h - pad;     // 좌하
        int x3 = w - pad, y3 = h / 2;       // 우중앙(화살촉)

        pm.fillTriangle(x1, y1, x2, y2, x3, y3);

        Texture t = new Texture(pm);
        pm.dispose();
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }

    // ↻ Restart : 원형 화살표
    private Texture makeRestartIcon(int w, int h) {
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pm.setColor(0f, 0f, 0f, 1f);
        int cx = w/2, cy = h/2, r = Math.min(w, h)/2 - 3;

        // 원호
        for (int a = 40; a <= 320; a++) {
            double rad = Math.toRadians(a);
            int x = cx + (int)(Math.cos(rad)*r);
            int y = cy + (int)(Math.sin(rad)*r);
            pm.drawPixel(x, y);
        }
        // 화살촉 (끝점 근처 삼각형)
        int ax = cx + (int)(Math.cos(Math.toRadians(40)) * r);
        int ay = cy + (int)(Math.sin(Math.toRadians(40)) * r);
        pm.fillTriangle(ax, ay, ax-6, ay+2, ax-2, ay+6);

        Texture t = new Texture(pm);
        pm.dispose();
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return t;
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (font  != null) font.dispose();
        if (px    != null) px.dispose();  // ★ 버튼 텍스처 해제
        if (iconPause   != null) iconPause.dispose();
        if (iconPlay    != null) iconPlay.dispose();
        if (iconRestart != null) iconRestart.dispose();
        if (world != null) world.dispose();
    }
}
