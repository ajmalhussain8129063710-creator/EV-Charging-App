@echo off
echo ==========================================
echo      EV Charging App - Git Push Helper
echo ==========================================

set repo_url=https://github.com/ajmalhussain8129063710-creator/EV-Charging-App.git

if exist .git (
    echo Git repository already initialized.
) else (
    echo Initializing Git Repository...
    git init
)

echo.
echo Adding files...
git add .

echo.
echo Committing changes...
git commit -m "Update EV Charging App code"

echo.
echo Configuring remote...
git remote remove origin 2>nul
git remote add origin %repo_url%

echo.
echo Pushing to GitHub...
git branch -M main
git push -u origin main

echo.
echo ==========================================
echo Done! Code pushed to:
echo %repo_url%
echo ==========================================
pause
