# DC schema
 
# --- !Ups


CREATE TABLE ENTRY (
    ID integer NOT NULL AUTO_INCREMENT PRIMARY KEY,
    NUTRITION varchar(255) NOT NULL,
    READING integer NOT NULL,
    READING_TIME integer NOT NULL,
);



 
# --- !Downs

DROP TABLE ENTRY;