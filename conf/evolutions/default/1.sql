# DC schema
 
# --- !Ups


CREATE TABLE ENTRY (
    ID integer NOT NULL AUTO_INCREMENT PRIMARY KEY,
    NUTRITION integer NOT NULL,
    READING integer NOT NULL,
    READING_TIME integer NOT NULL,
    READING_DATE date Not NULL
);


# --- !Downs

DROP TABLE ENTRY;