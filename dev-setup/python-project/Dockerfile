FROM python:3.11-slim

WORKDIR /app

# העתקת requirements
COPY requirements.txt .

# התקנת dependencies
RUN pip install --no-cache-dir -r requirements.txt

# העתקת הקוד
COPY . .

EXPOSE 5000

# הרצה
CMD ["python", "app.py"]