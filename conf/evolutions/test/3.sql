# DC schema
 
# --- !Ups


CREATE TABLE SESSION (
    ID integer NOT NULL AUTO_INCREMENT PRIMARY KEY,
    SESSION_KEY varchar(32) NOT NULL,
    USER_ID integer NOT NULL
);


# --- !Downs

DROP TABLE SESSION;