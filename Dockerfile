
FROM dcap/sapphire_base:0.1

COPY ./ /DCAP-Sapphire/

WORKDIR /DCAP-Sapphire/sapphire

# Verifying the build
RUN bash gradlew build
