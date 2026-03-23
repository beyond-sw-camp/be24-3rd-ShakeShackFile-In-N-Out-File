@echo off
chcp 65001 > nul

echo [1/3] 빌드 시작 (bootJar)
call gradlew bootJar
IF %ERRORLEVEL% NEQ 0 (
    echo 빌드 실패.
    exit /b %ERRORLEVEL%
)

echo [2/3] VM으로 JAR 파일 전송 (SCP)
scp build\libs\WaffleBear-0.0.1-SNAPSHOT.jar deploy@192.100.221.16:/home/deploy/
IF %ERRORLEVEL% NEQ 0 (
    echo SCP 전송 실패.
    exit /b %ERRORLEVEL%
)

echo [3/3] 기존 서버 종료 및 새 서버 실행 (SSH)
ssh deploy@192.100.221.16 "bash -lc '/home/deploy/start.sh'"
IF %ERRORLEVEL% NEQ 0 (
    echo SSH 원격 실행 실패.
    exit /b %ERRORLEVEL%
)

echo 배포 완료.
pause
