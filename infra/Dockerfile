# ==========================================================================
# Servidor SSH de despliegue (base Debian)
# Node (latest) + Java 25 (Temurin) + Podman + podman-compose + usuario pawner
# ==========================================================================
#
# ADVERTENCIA IMPORTANTE (léela antes de construir/correr esta imagen):
# Podman dentro de un contenedor Docker necesita ejecutarse en modo
# "privileged" (o con capabilities específicas + /dev/fuse) porque maneja
# sus propios namespaces/cgroups anidados. Este Dockerfile deja todo listo
# (subuid/subgid, fuse-overlayfs, slirp4netns), pero al ARRANCAR el
# contenedor debes darle --privileged (ver docker-compose.yml adjunto).
# Sin eso, "podman run"/"podman-compose" fallarán dentro del contenedor.
#
# La conectividad entre tu equipo y este servidor es vía Tailscale, que
# según indicas ya está configurado en el host — este Dockerfile NO instala
# Tailscale dentro del contenedor; solo expone el puerto 22 para que el
# host (unido al tailnet) lo reenvíe/publique según corresponda.

FROM debian:12-slim

ARG JAVA_VERSION=25
ENV DEBIAN_FRONTEND=noninteractive

# --------------------------------------------------------------------------
# 1. Paquetes base
# --------------------------------------------------------------------------
RUN apt-get update && apt-get install -y --no-install-recommends \
        openssh-server \
        sudo \
        curl \
        wget \
        gnupg \
        ca-certificates \
        lsb-release \
        git \
        nano \
        htop \
        unzip \
        python3 \
        python3-pip \
        python3-venv \
        build-essential \
    && rm -rf /var/lib/apt/lists/*

# --------------------------------------------------------------------------
# 2. Node.js (latest / current release line)
# --------------------------------------------------------------------------
RUN curl -fsSL https://deb.nodesource.com/setup_current.x | bash - \
    && apt-get install -y --no-install-recommends nodejs \
    && rm -rf /var/lib/apt/lists/* \
    && node -v && npm -v

# --------------------------------------------------------------------------
# 3. Java 25 (Eclipse Temurin, repo oficial de Adoptium)
# --------------------------------------------------------------------------
RUN mkdir -p /etc/apt/keyrings \
    && wget -qO /etc/apt/keyrings/adoptium.asc https://packages.adoptium.net/artifactory/api/gpg/key/public \
    && echo "deb [signed-by=/etc/apt/keyrings/adoptium.asc] https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print $2}' /etc/os-release) main" \
        > /etc/apt/sources.list.d/adoptium.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends temurin-${JAVA_VERSION}-jdk \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/lib/jvm/temurin-${JAVA_VERSION}-jdk-amd64
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# --------------------------------------------------------------------------
# 4. Podman + podman-compose (rootless)
# --------------------------------------------------------------------------
RUN apt-get update && apt-get install -y --no-install-recommends \
        podman \
        uidmap \
        slirp4netns \
        fuse-overlayfs \
        dbus-user-session \
    && rm -rf /var/lib/apt/lists/* \
    && pip3 install --break-system-packages --no-cache-dir podman-compose

# --------------------------------------------------------------------------
# 5. Usuario "pawner" (sin usar root para trabajar/desplegar)
# --------------------------------------------------------------------------
RUN useradd -m -s /bin/bash -G sudo pawner \
    && echo "pawner ALL=(ALL) NOPASSWD:ALL" > /etc/sudoers.d/pawner \
    && chmod 440 /etc/sudoers.d/pawner \
    # Rango de subuid/subgid para que "pawner" pueda correr Podman rootless
    && echo "pawner:100000:65536" >> /etc/subuid \
    && echo "pawner:100000:65536" >> /etc/subgid

# --------------------------------------------------------------------------
# 6. Claves SSH autorizadas (las 4 del equipo)
#    -> Reemplaza el contenido de ./ssh_keys/authorized_keys antes de buildear
# --------------------------------------------------------------------------
RUN mkdir -p /home/pawner/.ssh && chmod 700 /home/pawner/.ssh
COPY ssh_keys/authorized_keys /home/pawner/.ssh/authorized_keys
RUN chmod 600 /home/pawner/.ssh/authorized_keys \
    && chown -R pawner:pawner /home/pawner/.ssh

# --------------------------------------------------------------------------
# 7. Directorio de trabajo compartido con el host (volumen)
# --------------------------------------------------------------------------
RUN mkdir -p /srv/app && chown -R pawner:pawner /srv/app
VOLUME ["/srv/app"]

# --------------------------------------------------------------------------
# 8. Hardening de SSH: solo llave pública, sin root, solo usuario pawner
# --------------------------------------------------------------------------
RUN mkdir -p /var/run/sshd \
    && ssh-keygen -A \
    && sed -i \
        -e 's/^#\?PermitRootLogin.*/PermitRootLogin no/' \
        -e 's/^#\?PasswordAuthentication.*/PasswordAuthentication no/' \
        -e 's/^#\?PubkeyAuthentication.*/PubkeyAuthentication yes/' \
        -e 's/^#\?X11Forwarding.*/X11Forwarding no/' \
        /etc/ssh/sshd_config \
    && echo "AllowUsers pawner" >> /etc/ssh/sshd_config

EXPOSE 22

WORKDIR /srv/app

CMD ["/usr/sbin/sshd", "-D", "-e"]
