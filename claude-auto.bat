@echo off
title Claude Code - 自动确认模式
cd /d "%~dp0"
echo ==============================
echo   Claude Code 自动确认模式
echo   不再问 Yes/No，全部自动通过
echo ==============================
echo.
claude --permission-mode bypassPermissions %*
pause