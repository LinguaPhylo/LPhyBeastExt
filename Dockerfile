# Dockerfile to build container for unit testing.
#
# To build the image, run the following from this directory:
#   docker build -t testing .
#
# To run the tests, use
#   docker run testing
#

FROM openjdk:11

# Install Apache Ant
RUN apt-get update && apt-get install -y ant

ENV USER root

# Install LPhy
RUN cd /root && git clone https://github.com/LinguaPhylo/linguaPhylo.git
RUN cd /root/linguaPhylo && ant build

# Install beast-outercore
RUN cd /root && git clone https://github.com/LinguaPhylo/beast-outercore.git
RUN cd /root/beast-outercore && ant build

# Ant build fails if the repo dir isn't named LPhyBEAST
RUN mkdir /root/LPhyBEAST
WORKDIR /root/LPhyBEAST

ADD . ./

CMD ant travis
