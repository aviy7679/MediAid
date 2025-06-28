@echo off
echo 🛑 עוצר את כל השירותים...

docker-compose down

echo ✅ כל השירותים נעצרו!
echo 💾 הנתונים נשמרו ב-volumes
pause