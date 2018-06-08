# DC schema
 
# --- !Ups


ALTER TABLE USER ADD COLUMN NICK varchar(12) DEFAULT 'anonymous';


# --- !Downs

ALTER TABLE USER DROP COLUMN NICK;