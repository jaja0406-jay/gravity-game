package io.jbnu.hw;

// CameraManager: 카메라 연출(예: 셰이크)을 관리하는 간단한 매니저
// 실제 카메라(OrthographicCamera/Viewport)와 연결해 흔들림 값을 적용하는 방식으로 확장할 수 있습니다.
public class CameraManager {                 // 클래스 선언 시작
    private float time = 0f;                 // 남은 셰이크 시간(초)
    private float intensity = 0f;            // 셰이크 강도(픽셀 또는 각도 등으로 해석)

    // 셰이크 시작: seconds 동안, power 강도로 흔듭니다.
    public void start(float seconds, float power) { // 시작 함수
        this.time = seconds;               // 지속 시간 세팅
        this.intensity = power;            // 강도 세팅
    }

    // 프레임마다 호출: 남은 시간을 줄이고, 필요시 카메라에 오프셋을 적용합니다.
    public void update(float delta) {      // 업데이트 함수
        if (time > 0f) {                   // 셰이크가 활성화되어 있다면
            time -= delta;                 // 남은 시간 감소
            // 실제 프로젝트에서는 여기서 카메라 위치/회전을 난수로 약간 흔들어주고,
            // time이 0이 되면 원래 위치로 복귀시키는 코드를 작성합니다.
        }
    }

    // (선택) 셰이크가 진행 중인지 외부에서 확인할 수 있도록 도우미를 제공합니다.
    public boolean isShaking() {           // 진행 여부 확인
        return time > 0f;                  // 남은 시간이 있으면 true
    }

    // (선택) 강도를 노출해 외부에서 현재 강도 기반 연출을 할 수도 있습니다.
    public float getIntensity() {          // 강도 게터
        return intensity;                  // 현재 강도 반환
    }
}                                          // CameraManager 클래스 끝
