@echo off
title OTP Share - Quick Start
echo 🚀 Starting OTP Share Server...
cd backend
echo 📦 Checking dependencies...
call npm install --quiet
echo ✅ Dependencies ready!
echo 🚀 Launching server...
node launcher.js
pause
