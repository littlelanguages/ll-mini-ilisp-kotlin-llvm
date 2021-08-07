ARG VARIANT="15"
FROM mcr.microsoft.com/vscode/devcontainers/java:0-${VARIANT}

ARG GRADLE_VERSION="6.7"
RUN su vscode -c "source /usr/local/sdkman/bin/sdkman-init.sh && sdk install gradle \"${GRADLE_VERSION}\""

RUN su vscode -c "curl -fsSL https://deno.land/x/install/install.sh | sh -s v1.4.6"
ENV DENO_INSTALL="/home/vscode/.deno"
ENV PATH="$DENO_INSTALL/bin:$PATH"

RUN apt-get update && export DEBIAN_FRONTEND=noninteractive \
   && apt-get -y install --no-install-recommends entr
