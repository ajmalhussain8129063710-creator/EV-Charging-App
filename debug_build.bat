@echo off
echo Running build with info logging...
call gradle :app:assembleDebug --info
pause
