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

CREATE TABLE metadata_backup
(
  metadata_backup_id integer PRIMARY KEY,
  resource_id integer NOT NULL,
  metadata_field_id integer,
  text_value text,
  text_lang character varying(24),
  place integer,
  authority character varying(100),
  confidence integer DEFAULT (-1),
  resource_type_id integer NOT NULL,
  CONSTRAINT metadatavalue_metadata_backup_id_fkey FOREIGN KEY (metadata_field_id)
      REFERENCES metadatafieldregistry (metadata_field_id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE SEQUENCE metadata_backup_seq
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 35
  CACHE 1;

CREATE INDEX metadatabackup_field_fk_idx ON metadatavalue USING btree (metadata_field_id);
CREATE INDEX metadatabackup_item_idx ON metadatavalue USING btree (resource_id);
CREATE INDEX metadatabackup_item_idx2 ON metadatavalue USING btree (resource_id, metadata_field_id);

alter table item add column backup boolean default FALSE;

COMMIT;