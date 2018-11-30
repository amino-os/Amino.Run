
FROM dcap/sapphire_base:0.1

COPY ./ /DCAP-Sapphire/

WORKDIR /DCAP-Sapphire/sapphire

# Install GraalVM ruby component
# NOTE: This is a temporary measure until dcap/sapphire_base:0.2 has been updated.
# Once that has been done, the following command should be removed.
RUN bash gu install ruby

# Verifying the build
RUN bash gradlew build
