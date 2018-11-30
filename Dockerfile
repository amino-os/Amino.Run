
FROM dcap/sapphire_base:0.2

COPY ./ /DCAP-Sapphire/

WORKDIR /DCAP-Sapphire/sapphire

# Verifying the build
RUN bash gradlew build
