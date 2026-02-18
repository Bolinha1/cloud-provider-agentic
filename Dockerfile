FROM eclipse-temurin:21-jre

RUN apt-get update && \
    apt-get install -y --no-install-recommends unzip curl && \
    curl -fsSL https://releases.hashicorp.com/terraform/1.12.1/terraform_1.12.1_linux_amd64.zip -o /tmp/terraform.zip && \
    unzip /tmp/terraform.zip -d /usr/local/bin && \
    rm /tmp/terraform.zip && \
    apt-get purge -y unzip curl && \
    apt-get autoremove -y && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
