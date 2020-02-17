# Base image is java 11
FROM openjdk:11-slim
LABEL "velcom-server"="true"

# We use nginx as a reverse proxy
# We use git for fast cloning
# We use sudo to drop privileges
RUN apt update && apt install nginx -y && apt install sudo -y && apt install git -y

# Preconfigured nginx configs for development on aaaaaaah.de
COPY nginx-site /etc/nginx/sites-available
COPY nginx.conf /etc/nginx/nginx.conf

RUN ln -s /etc/nginx/sites-available/nginx-site /etc/nginx/sites-enabled && \
    rm /etc/nginx/sites-enabled/default && \
    useradd velcom

# Expose the config dir
VOLUME ["/home/velcom/config"]
# Frontend serving port (from nginx config)
EXPOSE 80
# Backend API port (from nginx config)
EXPOSE 81
# Runner port (from nginx config)
EXPOSE 82

COPY start-backend-docker.sh /home/velcom/start.sh

# Include backend files
COPY backend.jar /home/velcom/velcom.jar
COPY dist /home/velcom/frontend

# We should be able to place temp data in there
# Not *strictly* needed though
RUN chown -R velcom:velcom /home/velcom

# Execute the server, needs the path to the configuration file passed as an argument
ENTRYPOINT ["/home/velcom/start.sh"]
