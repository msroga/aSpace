BEGIN;
SET SESSION AUTHORIZATION 'dspace';

CREATE TABLE bitstream_backup
(
  bitstream_backup_id integer PRIMARY KEY,
  bitstream_id integer,
  bitstream_format_id integer,
  checksum character varying(64),
  checksum_algorithm character varying(32),
  internal_id character varying(256),
  deleted boolean,
  store_number integer,
  sequence_id integer,
  size_bytes bigint,
  /*CONSTRAINT bitstream_backup_bitstream_id_fkey FOREIGN KEY (bitstream_id)
      REFERENCES bitstream (bitstream_id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
*/  CONSTRAINT bitstream_backup_bitstream_format_id_fkey FOREIGN KEY (bitstream_format_id)
      REFERENCES bitstreamformatregistry (bitstream_format_id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE INDEX bitbackup_bitstream_fk_idx ON bitstream_backup USING btree (bitstream_id);
CREATE INDEX bitbackup_bitstream_format_fk_idx ON bitstream_backup USING btree (bitstream_format_id);

CREATE SEQUENCE bitstream_backup_seq
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 35
  CACHE 1;

COMMIT;