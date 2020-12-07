# vim: ft=dockerfile

# Base image is java 11
FROM openjdk:11-slim
LABEL "velcom-server"="true"


#  ____            _
# |  _ \ __ _  ___| | ____ _  __ _  ___  ___ 
# | |_) / _` |/ __| |/ / _` |/ _` |/ _ \/ __|
# |  __/ (_| | (__|   < (_| | (_| |  __/\__ \
# |_|   \__,_|\___|_|\_\__,_|\__, |\___||___/
#                            |___/

# We use nginx as a reverse proxy
# We use git for fast cloning
# We use sudo to drop privileges
RUN apt update && apt install nginx -y && apt install sudo -y && apt install git -y


#  _   _       _
# | \ | | __ _(_)_ __ __  __
# |  \| |/ _` | | '_ \\ \/ /
# | |\  | (_| | | | | |>  < 
# |_| \_|\__, |_|_| |_/_/\_\
#        |___/

# Preconfigured nginx configs for development on aaaaaaah.de
COPY nginx-site /etc/nginx/sites-available
COPY nginx.conf /etc/nginx/nginx.conf

RUN ln -s /etc/nginx/sites-available/nginx-site /etc/nginx/sites-enabled && \
    rm /etc/nginx/sites-enabled/default


#  _   _
# | | | |___  ___ _ __
# | | | / __|/ _ \ '__|
# | |_| \__ \  __/ |
#  \___/|___/\___|_|

# The user id is injected at build time
ARG USER_ID
RUN useradd --uid $USER_ID velcom


# __     __    _
# \ \   / /__ | |_   _ _ __ ___   ___  ___
#  \ \ / / _ \| | | | | '_ ` _ \ / _ \/ __|
#   \ V / (_) | | |_| | | | | | |  __/\__ \
#    \_/ \___/|_|\__,_|_| |_| |_|\___||___/

# Set up some volumes. You should mount these volumes to your host file system
# or a named docker volumne using the '-v' option.
# See the example backend config for a more elaborate explanation of these
# directories

# - config/ (the suggested location for your config file)
VOLUME ["/home/velcom/config"]
# - data/  : Essential files (e.g. the database)
VOLUME ["/home/velcom/data"]
# - cache/ : Non-essential files that shoukd be persisted between restarts
VOLUME ["/home/velcom/cache"]


#  ____            _
# |  _ \ ___  _ __| |_ ___
# | |_) / _ \| '__| __/ __|
# |  __/ (_) | |  | |_\__ \
# |_|   \___/|_|   \__|___/

# Frontend serving port (from nginx config)
EXPOSE 80
# Backend API port (from nginx config)
EXPOSE 81
# Runner port (from nginx config)
EXPOSE 82


#  ____  _             _
# / ___|| |_ __ _ _ __| |_ _   _ _ __
# \___ \| __/ _` | '__| __| | | | '_ \
#  ___) | || (_| | |  | |_| |_| | |_) |
# |____/ \__\__,_|_|   \__|\__,_| .__/
#                               |_|

# This docker image needs a custom startup script that starts velcom *and*
# nginx
COPY start-backend-docker.sh /home/velcom/start.sh


# __     __   _  ____                  _____ _ _
# \ \   / /__| |/ ___|___  _ __ ___   |  ___(_) | ___  ___
#  \ \ / / _ \ | |   / _ \| '_ ` _ \  | |_  | | |/ _ \/ __|
#   \ V /  __/ | |__| (_) | | | | | | |  _| | | |  __/\__ \
#    \_/ \___|_|\____\___/|_| |_| |_| |_|   |_|_|\___||___/

# Include backend files
# Copy aspectjweaver if it exists, as it is an optional dependency used to
# collect more metrics. Copy needs at least one existing file as an argument,
# so we need the backend.jar there as well
COPY backend.jar aspectjweaver.ja[r] /home/velcom/
RUN mv /home/velcom/backend.jar /home/velcom/velcom.jar
COPY dist /home/velcom/frontend

# We should be able to place temp data in there
# Not *strictly* needed though
RUN chown -R velcom:velcom /home/velcom


#  _____       _                          _       _
# | ____|_ __ | |_ _ __ _   _ _ __   ___ (_)_ __ | |_
# |  _| | '_ \| __| '__| | | | '_ \ / _ \| | '_ \| __|
# | |___| | | | |_| |  | |_| | |_) | (_) | | | | | |_
# |_____|_| |_|\__|_|   \__, | .__/ \___/|_|_| |_|\__|
#                       |___/|_|


# Execute the server, needs the path to the configuration file passed as an argument
ENTRYPOINT ["/home/velcom/start.sh"]
