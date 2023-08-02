FROM openjdk:8

RUN mkdir /usr/local/MASTER_APP_FILES

RUN mkdir /usr/local/MASTER_APP_FILES/GATEWAY

RUN mkdir /usr/local/MASTER_APP_FILES/GATEWAY/data

RUN mkdir /usr/local/MASTER_APP_FILES/BOM

RUN mkdir /usr/local/MASTER_APP_FILES/VULN

RUN mkdir /usr/local/MASTER_APP_FILES/DB

RUN mkdir /usr/local/MASTER_APP_FILES/DT_OUT

RUN mkdir /usr/local/MASTER_APP_FILES/VULNDB_LOAD_REPORT

ENV oracle_password=orapassword123


COPY ./target/MasterAnalyserCloud-0.0.1-jar-with-dependencies.jar  /usr/local/

CMD java -jar /usr/local/MasterAnalyserCloud-0.0.1-jar-with-dependencies.jar



