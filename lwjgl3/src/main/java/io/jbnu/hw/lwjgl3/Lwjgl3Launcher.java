package io.jbnu.hw.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import io.jbnu.hw.MainGame;

public class Lwjgl3Launcher {
    public static void main(String[] args) {
        System.out.println("[Launcher] starting...");
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();

        // 🌟 윈도우 기본 설정
        config.setTitle("Gravity Direction");
        config.setWindowedMode(1280, 800);   // ✅ 화면 크기 ↑ (800x600 → 1280x800)
        config.setResizable(true);           // 크기 조절 가능
        config.useVsync(true);
        config.setForegroundFPS(60);

        // 선택 사항 (FPS 표시, 디버그용)
        // config.setIdleFPS(30); // 백그라운드 시 낮은 FPS 유지

        new Lwjgl3Application(new MainGame(), config);
    }

}
