# Dockerfile to build container for unit testing.
#
# To build the image, run the following from this directory:
#   docker build -t testing .
#
# To run the tests, use
#   docker run testing
#

FROM openjdk:15.0.1-jdk-slim-buster

# Install Ant https://hub.docker.com/r/webratio/ant/dockerfile
ENV ANT_VERSION 1.10.9
RUN cd && \
    wget -q http://www.us.apache.org/dist//ant/binaries/apache-ant-${ANT_VERSION}-bin.tar.gz && \
    tar -xzf apache-ant-${ANT_VERSION}-bin.tar.gz && \
    mv apache-ant-${ANT_VERSION} /opt/ant && \
    rm apache-ant-${ANT_VERSION}-bin.tar.gz
ENV ANT_HOME /opt/ant
ENV PATH ${PATH}:/opt/ant/bin

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
