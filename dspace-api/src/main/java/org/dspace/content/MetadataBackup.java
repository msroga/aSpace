/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Database access class representing a Dublin Core metadata value.
 * It represents a value of a given <code>MetadataField</code> on an Item.
 * (The Item can have many values of the same field.)  It contains                                           element, qualifier, value and language.
 * the field (which names the schema, element, and qualifier), language,
 * and a value.
 *
 * @author Martin Hald
 * @see MetadataSchema
 * @see MetadataField
 */
public class MetadataBackup
{
    /** The reference to the metadata field */
    private int fieldId = 0;

    /** The primary key for the metadata value */
    private int valueId = 0;

    /** The reference to the DSpace resource */
    private int resourceId;

    /** The reference to the DSpace resource type*/
    private int resourceTypeId;

    /** The value of the field */
    public String value;

    /** The language of the field, may be <code>null</code> */
    public String language;

    /** The position of the record. */
    public int place = 1;

    /** Authority key, if any */
    public String authority = null;

    /** Authority confidence value -- see Choices class for values */
    public int confidence = 0;

    /** log4j logger */
    private static Logger log = Logger.getLogger(MetadataBackup.class);

    /** The row in the table representing this type */
    private TableRow row;

    /**
     * Construct the metadata object from the matching database row.
     *
     * @param row database row to use for contents
     */
    public MetadataBackup(TableRow row)
    {
        if (row != null)
        {
            fieldId = row.getIntColumn("metadata_field_id");
            valueId = row.getIntColumn("metadata_backup_id");
            resourceId = row.getIntColumn("resource_id");
            resourceTypeId = row.getIntColumn("resource_type_id");
            value = row.getStringColumn("text_value");
            language = row.getStringColumn("text_lang");
            place = row.getIntColumn("place");
            authority = row.getStringColumn("authority");
            confidence = row.getIntColumn("confidence");
            this.row = row;
        }
    }

    /**
     * Default constructor.
     */
    public MetadataBackup()
    {
    }

    /**
     * Constructor to create a value for a given field.
     *
     * @param field initial value for field
     */
    public MetadataBackup(MetadataField field)
    {
        this.fieldId = field.getFieldID();
    }

    /**
     * Get the field ID the metadata value represents.
     *
     * @return metadata field ID
     */
    public int getFieldId()
    {
        return fieldId;
    }

    /**
     * Set the field ID that the metadata value represents.
     *
     * @param fieldId new field ID
     */
    public void setFieldId(int fieldId)
    {
        this.fieldId = fieldId;
    }

    /**
     * Get the resource type ID.
     *
     * @return resource type ID
     */
    public int getResourceTypeId() {
        return resourceTypeId;
    }

    /**
     * Set the resource type ID.
     *
     * @param resourceTypeId new resource type ID
     */
    public void setResourceTypeId(int resourceTypeId) {
        this.resourceTypeId = resourceTypeId;
    }

    /**
     * Get the resource id
     *
     * @return resource ID
     */
    public int getResourceId() {
        return resourceId;
    }

    /**
     * Set the resource type ID.
     *
     * @param resourceId new resource ID
     */
    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    /**
     * Get the language (e.g. "en").
     *
     * @return language
     */
    public String getLanguage()
    {
        return language;
    }

    /**
     * Set the language (e.g. "en").
     *
     * @param language new language
     */
    public void setLanguage(String language)
    {
        this.language = language;
    }

    /**
     * Get the place ordering.
     *
     * @return place ordering
     */
    public int getPlace()
    {
        return place;
    }

    /**
     * Set the place ordering.
     *
     * @param place new place (relative order in series of values)
     */
    public void setPlace(int place)
    {
        this.place = place;
    }

    /**
     * Get the value ID.
     *
     * @return value ID
     */
    public int getValueId()
    {
        return valueId;
    }

    /**
     * Get the metadata value.
     *
     * @return metadata value
     */
    public String getValue()
    {
        return value;
    }

    /**
     * Set the metadata value
     *
     * @param value new metadata value
     */
    public void setValue(String value)
    {
        this.value = value;
    }

    /**
     * Get the metadata authority
     *
     * @return metadata authority
     */
    public String getAuthority ()
    {
        return authority ;
    }

    /**
     * Set the metadata authority
     *
     * @param value new metadata authority
     */
    public void setAuthority (String value)
    {
        this.authority  = value;
    }

    /**
     * Get the metadata confidence
     *
     * @return metadata confidence
     */
    public int getConfidence()
    {
        return confidence;
    }

    /**
     * Set the metadata confidence
     *
     * @param value new metadata confidence
     */
    public void setConfidence(int value)
    {
        this.confidence = value;
    }

    /**
     * Creates a new metadata value.
     *
     * @param context
     *            DSpace context object
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     */
    public void create(Context context) throws SQLException, AuthorizeException
    {
        // Create a table row and update it with the values
        row = DatabaseManager.row("Metadata_Backup");
        row.setColumn("resource_id", resourceId);
        row.setColumn("resource_type_id", resourceTypeId);
        row.setColumn("metadata_field_id", fieldId);
        row.setColumn("text_value", value);
        row.setColumn("text_lang", language);
        row.setColumn("place", place);
        row.setColumn("authority", authority);
        row.setColumn("confidence", confidence);
        DatabaseManager.insert(context, row);

        // Remember the new row number
        this.valueId = row.getIntColumn("metadata_backup_id");

//        log.info(LogManager.getHeader(context, "create_metadata_value",
//                "metadata_value_id=" + valueId));
    }

    /**
     * Retrieves the metadata value from the database.
     *
     * @param context dspace context
     * @param valueId database key id of value
     * @return recalled metadata value
     * @throws java.io.IOException
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     */
    public static MetadataBackup find(Context context, int valueId)
            throws IOException, SQLException, AuthorizeException
    {
        // Grab rows from DB
        TableRowIterator tri = DatabaseManager.queryTable(context, "Metadata_Backup",
                "SELECT * FROM Metadata_Backup where metadata_backup_id= ? ",
                valueId);

        TableRow row = null;
        try
        {
            if (tri.hasNext())
            {
                row = tri.next();
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }

        if (row == null)
        {
            return null;
        }
        else
        {
            return new MetadataBackup(row);
        }
    }

    /**
     * Retrieves the metadata values for a given field from the database.
     *
     * @param context dspace context
     * @param fieldId field whose values to look for
     * @return a collection of metadata values
     * @throws java.io.IOException
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     */
    public static List<MetadataBackup> findByField(Context context, int fieldId)
            throws IOException, SQLException, AuthorizeException
    {
        // Grab rows from DB
        TableRowIterator tri = DatabaseManager.queryTable(context, "Metadata_Backup",
                "SELECT * FROM Metadata_Backup WHERE metadata_field_id= ? ",
                fieldId);

        TableRow row = null;
        List<MetadataBackup> ret = new ArrayList<MetadataBackup>();
        try
        {
            while (tri.hasNext())
            {
                row = tri.next();
                ret.add(new MetadataBackup(row));
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }

        return ret;
    }

    /**
     * Update the metadata value in the database.
     *
     * @param context dspace context
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     */
    public void update(Context context) throws SQLException, AuthorizeException
    {
        row.setColumn("resource_id", resourceId);
        row.setColumn("resource_type_id", resourceTypeId);
        row.setColumn("metadata_field_id", fieldId);
        row.setColumn("text_value", value);
        row.setColumn("text_lang", language);
        row.setColumn("place", place);
        row.setColumn("authority", authority);
        row.setColumn("confidence", confidence);
        DatabaseManager.update(context, row);

        log.info(LogManager.getHeader(context, "update_metadatabackup",
                "metadata_backup_id=" + getValueId()));
    }

    /**
     * Delete the metadata field.
     *
     * @param context dspace context
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     */
    public void delete(Context context) throws SQLException, AuthorizeException
    {
        log.info(LogManager.getHeader(context, "delete_metadata_backup",
                " metadata_backup_id=" + getValueId()));
        DatabaseManager.delete(context, row);
    }

    /**
     * Return <code>true</code> if <code>other</code> is the same MetadataValue
     * as this object, <code>false</code> otherwise
     *
     * @param obj
     *            object to compare to
     *
     * @return <code>true</code> if object passed in represents the same
     *         MetadataValue as this object
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final MetadataBackup other = (MetadataBackup) obj;
        if (this.fieldId != other.fieldId)
        {
            return false;
        }
        if (this.valueId != other.valueId)
        {
            return false;
        }
        if (this.resourceId != other.resourceId)
        {
            return false;
        }
        if (this.resourceTypeId != other.resourceTypeId)
        {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 47 * hash + this.fieldId;
        hash = 47 * hash + this.valueId;
        hash = 47 * hash + this.resourceId;
        hash = 47 * hash + this.resourceTypeId;
        return hash;
    }


   public static List<MetadataBackup> findBy(Context context, int resuorceId, int metdatafieldId)
           throws IOException, SQLException, AuthorizeException
   {
      // Grab rows from DB
      TableRowIterator tri = DatabaseManager.queryTable(context, "Metadata_Backup",
              "SELECT * FROM Metadata_Backup WHERE metadata_field_id= ? AND resource_id= ? order by place asc",
              metdatafieldId, resuorceId);

      TableRow row = null;
      List<MetadataBackup> ret = new ArrayList<MetadataBackup>();
      try
      {
         while (tri.hasNext())
         {
            row = tri.next();
            ret.add(new MetadataBackup(row));
         }
      }
      finally
      {
         // close the TableRowIterator to free up resources
         if (tri != null)
         {
            tri.close();
         }
      }

      return ret;
   }
}
