FROM openjdk:17-jdk-slim

WORKDIR /app

# העתקת קבצי Maven
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# הורדת dependencies
RUN ./mvnw dependency:go-offline

# העתקת הקוד
COPY src src

# בנייה
RUN ./mvnw clean package -DskipTests

# הרצה
EXPOSE 8080
CMD ["java", "-jar", "target/*.jar"]