FROM eclipse-temurin:17-jdk

# Set the working directory inside the container
WORKDIR /app

# Copy your Java file into the container
COPY RentalApp.java .

# Compile the Java application
RUN javac RentalApp.java

# Run the application
CMD ["java", "RentalApp"]
