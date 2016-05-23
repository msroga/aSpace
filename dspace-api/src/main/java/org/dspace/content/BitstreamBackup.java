package org.dspace.content;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.event.Event;
import org.dspace.storage.bitstore.BitstreamStorageManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

/**
 * Created by Marek on 2016-05-07.
 */
public class BitstreamBackup extends DSpaceObject
{
   /** log4j logger */
   private static final Logger log = Logger.getLogger(BitstreamBackup.class);

   /** The row in the table representing this bitstream */
   private final TableRow bRow;

   /** The bitstream format corresponding to this bitstream */
   private BitstreamFormat bitstreamFormat;

   /** Flag set when data is modified, for events */
   private boolean modified;

   BitstreamBackup(Context context, TableRow row) throws SQLException
   {
      super(context);

      // Ensure that my TableRow is typed.
      if (null == row.getTable())
      {
         row.setTable("bitstream_backup");
      }
      bRow = row;

      // Get the bitstream format
      bitstreamFormat = BitstreamFormat.find(context, row.getIntColumn("bitstream_format_id"));

      if (bitstreamFormat == null)
      {
         // No format: use "Unknown"
         bitstreamFormat = BitstreamFormat.findUnknown(context);

         // Panic if we can't find it
         if (bitstreamFormat == null)
         {
            throw new IllegalStateException("No Unknown bitstream format");
         }
      }

      // Cache ourselves
      context.cache(this, row.getIntColumn("bitstream_id"));

      modified = false;
      clearDetails();
   }

   public static BitstreamBackup find(Context context, int id) throws SQLException
   {
      // First check the cache
      BitstreamBackup fromCache = (BitstreamBackup) context
              .fromCache(BitstreamBackup.class, id);

      if (fromCache != null)
      {
         return fromCache;
      }

      TableRow row = DatabaseManager.find(context, "bitstream_backup", id);

      if (row == null)
      {
         if (log.isDebugEnabled())
         {
            log.debug(LogManager.getHeader(context, "find_bitstream",
                    "not_found,bitstream_id=" + id));
         }

         return null;
      }

      // not null, return Bitstream
      if (log.isDebugEnabled())
      {
         log.debug(LogManager.getHeader(context, "find_bitstream",
                 "bitstream_id=" + id));
      }

      return new BitstreamBackup(context, row);
   }

   public static BitstreamBackup create(Context context, Bitstream oryginal)
           throws IOException, SQLException, AuthorizeException
   {
      // Store the bits
      InputStream is = oryginal.retrieve();
      int bitstreamID = BitstreamStorageManager.backup(context, oryginal);

      log.info(LogManager.getHeader(context, "create_bitstream", "bitstream_id=" + bitstreamID));

      BitstreamBackup bitstream = find(context, bitstreamID);
      bitstream.setDescription(oryginal.getDescription());
      bitstream.setName(oryginal.getName());
      bitstream.setSource(oryginal.getSource());
//      context.addEvent(new Event(Event.CREATE, Constants.BITSTREAM,
//              bitstreamID, null, bitstream.getIdentifiers(context)));

      return bitstream;
   }

   public int getID()
   {
      return bRow.getIntColumn("bitstream_backup_id");
   }

   public int getAnonimizedID()
   {
      return bRow.getIntColumn("bitstream_id");
   }

   public void setAnonimizedID(Integer id)
   {
      bRow.setColumn("bitstream_id", id);
   }

   public String getHandle()
   {
      // No Handles for bitstreams
      return null;
   }

   /**
    * Get the sequence ID of this bitstream
    *
    * @return the sequence ID
    */
   public int getSequenceID()
   {
      return bRow.getIntColumn("sequence_id");
   }

   /**
    * Set the sequence ID of this bitstream
    *
    * @param sid
    *            the ID
    */
   public void setSequenceID(int sid)
   {
      bRow.setColumn("sequence_id", sid);
      modifiedMetadata = true;
      addDetails("SequenceID");
   }

   /**
    * Get the name of this bitstream - typically the filename, without any path
    * information
    *
    * @return the name of the bitstream
    */
   public String getName(){
      return getMetadataFirstValue(MetadataSchema.DC_SCHEMA, "title", null, Item.ANY);
   }

   /**
    * Set the name of the bitstream
    *
    * @param n
    *            the new name of the bitstream
    */
   public void setName(String n) {
      setMetadataSingleValue(MetadataSchema.DC_SCHEMA, "title", null, null, n);
   }

   /**
    * Get the source of this bitstream - typically the filename with path
    * information (if originally provided) or the name of the tool that
    * generated this bitstream
    *
    * @return the source of the bitstream
    */
   public String getSource()
   {
      return getMetadataFirstValue(MetadataSchema.DC_SCHEMA, "source", null, Item.ANY);
   }

   /**
    * Set the source of the bitstream
    *
    * @param n
    *            the new source of the bitstream
    */
   public void setSource(String n) {
      setMetadataSingleValue(MetadataSchema.DC_SCHEMA, "source", null, null, n);
   }

   /**
    * Get the description of this bitstream - optional free text, typically
    * provided by a user at submission time
    *
    * @return the description of the bitstream
    */
   public String getDescription()
   {
      return getMetadataFirstValue(MetadataSchema.DC_SCHEMA, "description", null, Item.ANY);
   }

   /**
    * Set the description of the bitstream
    *
    * @param n
    *            the new description of the bitstream
    */
   public void setDescription(String n) {
      setMetadataSingleValue(MetadataSchema.DC_SCHEMA, "description", null, null, n);
   }

   /**
    * Get the checksum of the content of the bitstream, for integrity checking
    *
    * @return the checksum
    */
   public String getChecksum()
   {
      return bRow.getStringColumn("checksum");
   }

   /**
    * Get the algorithm used to calculate the checksum
    *
    * @return the algorithm, e.g. "MD5"
    */
   public String getChecksumAlgorithm()
   {
      return bRow.getStringColumn("checksum_algorithm");
   }

   /**
    * Get the size of the bitstream
    *
    * @return the size in bytes
    */
   public long getSize()
   {
      return bRow.getLongColumn("size_bytes");
   }

   /**
    * Set the user's format description. This implies that the format of the
    * bitstream is uncertain, and the format is set to "unknown."
    *
    * @param desc
    *            the user's description of the format
    * @throws SQLException
    */
   public void setUserFormatDescription(String desc) throws SQLException {
      setFormat(null);
      setMetadataSingleValue(MetadataSchema.DC_SCHEMA, "format", null, null, desc);
   }

   /**
    * Get the user's format description. Returns null if the format is known by
    * the system.
    *
    * @return the user's format description.
    */
   public String getUserFormatDescription()
   {
      return getMetadataFirstValue(MetadataSchema.DC_SCHEMA, "format", null, Item.ANY);
   }

   /**
    * Get the description of the format - either the user's or the description
    * of the format defined by the system.
    *
    * @return a description of the format.
    */
   public String getFormatDescription()
   {
      if (bitstreamFormat.getShortDescription().equals("Unknown"))
      {
         // Get user description if there is one
         String desc = getUserFormatDescription();

         if (desc == null)
         {
            return "Unknown";
         }

         return desc;
      }

      // not null or Unknown
      return bitstreamFormat.getShortDescription();
   }

   /**
    * Get the format of the bitstream
    *
    * @return the format of this bitstream
    */
   public BitstreamFormat getFormat()
   {
      return bitstreamFormat;
   }

   /**
    * Set the format of the bitstream. If the user has supplied a type
    * description, it is cleared. Passing in <code>null</code> sets the type
    * of this bitstream to "unknown".
    *
    * @param f
    *            the format of this bitstream, or <code>null</code> for
    *            unknown
    * @throws SQLException
    */
   public void setFormat(BitstreamFormat f) throws SQLException
   {
      // FIXME: Would be better if this didn't throw an SQLException,
      // but we need to find the unknown format!
      if (f == null)
      {
         // Use "Unknown" format
         bitstreamFormat = BitstreamFormat.findUnknown(ourContext);
      }
      else
      {
         bitstreamFormat = f;
      }

      // Remove user type description
      clearMetadata(MetadataSchema.DC_SCHEMA,"format",null, Item.ANY);

      // Update the ID in the table row
      bRow.setColumn("bitstream_format_id", bitstreamFormat.getID());
      modified = true;
   }

   public int getType()
   {
      return Constants.BITSTREAM;
   }

   public int getStoreNumber() {
      return bRow.getIntColumn("store_number");
   }

   public String getInternalID() {
      return bRow.getStringColumn("internal_id");
   }

   @Override
   public void update() throws SQLException, AuthorizeException
   {
      // Check authorisation
      AuthorizeManager.authorizeAction(ourContext, this, Constants.WRITE);

      log.info(LogManager.getHeader(ourContext, "update_bitstream", "bitstream_id=" + getID()));

      DatabaseManager.update(ourContext, bRow);

      if (modified)
      {
         ourContext.addEvent(new Event(Event.MODIFY, Constants.BITSTREAM, getID(), null, getIdentifiers(ourContext)));
         modified = false;
      }
      if (modifiedMetadata)
      {
         updateMetadata();
         clearDetails();
      }
   }

   public void delete() throws SQLException
   {

      // changed to a check on remove
      // Check authorisation
      //AuthorizeManager.authorizeAction(ourContext, this, Constants.DELETE);
      log.info(LogManager.getHeader(ourContext, "delete_bitstream",
              "bitstream_id=" + getID()));

      // Remove from cache
      ourContext.removeCached(this, getID());

      // Remove policies
      AuthorizeManager.removeAllPolicies(ourContext, this);

      // Remove bitstream itself
      DatabaseManager.updateQuery(ourContext,
              "delete from Bitstream_Backup where bitstream_backup_id = ? ",
              bRow.getIntColumn("bitstream_backup_id"));

      removeMetadataFromDatabase();
   }

   @Override
   public void updateLastModified()
   {

   }

   public static BitstreamBackup findByAnonymized(Context context, Integer bitstreamId) throws SQLException
   {

      TableRow row = DatabaseManager.findByUnique(context, "bitstream_backup", "bitstream_id", bitstreamId);
      return new BitstreamBackup(context, row);
   }
}
