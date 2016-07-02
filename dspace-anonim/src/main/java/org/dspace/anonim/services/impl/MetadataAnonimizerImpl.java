package org.dspace.anonim.services.impl;

import org.apache.commons.lang3.StringUtils;
import org.dspace.anonim.services.IMetadataAnonimizer;
import org.dspace.content.Metadatum;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Created by Marek on 2016-05-24.
 */
@Service("metadataAnonimizer")
public class MetadataAnonimizerImpl implements IMetadataAnonimizer
{
   private static final String COMMA = ",";

   @Override
   public void anonymize(Metadatum[] values)
   {
      if (values != null && values.length > 0)
      {
         for (Metadatum metadatum : values)
         {
            String value = process(metadatum);
            if (value != null)
            {
               metadatum.value = value;
            }
         }
      }
   }

   private String process(Metadatum metadatum)
   {
      StringBuilder sb = new StringBuilder();
      if (metadatum.element.equalsIgnoreCase("contributor")) //metadatum.element.equalsIgnoreCase("author")
      {
         String[] names = StringUtils.split(metadatum.value, COMMA);
         Iterator<String> it = Arrays.asList(names).iterator();
         while (it.hasNext())
         {
            String name = it.next().replaceAll("\\s+","");
            sb.append(name.substring(0, 1));
            if (it.hasNext())
            {
               sb.append("., ");
            }
            else
            {
               sb.append(".");
            }
         }
         return sb.toString();
      }
//      else if (metadatum.element.equalsIgnoreCase("title"))
//      {
//
//      }

      return null;
   }
}
