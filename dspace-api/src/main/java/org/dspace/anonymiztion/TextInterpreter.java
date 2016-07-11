package org.dspace.anonymiztion;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.SensitiveType;
import org.dspace.core.ConfigurationManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * Created by Marek on 2016-07-01.
 */
public class TextInterpreter
{
   private Logger logger = Logger.getLogger(TextInterpreter.class);

   private static TextInterpreter interpreter;

   public static final Pattern POSTAL_CODE_PATTERN = Pattern.compile("[\\d]{2}-[\\d]{3}");

   public final static Pattern PHONE_PATTERN = Pattern.compile(("(\\+\\d\\d)?([\\d\\- ]{9,13})"));

   //"^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*((\\.[A-Za-z]{2,}){1}$)"
   public static final Pattern EMAIL_PATTERN = Pattern.compile("(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])\n",
           Pattern.CASE_INSENSITIVE);

   public static final Pattern REGISTRATION_PLATE_PATTERN = Pattern.compile("[ZGNBFPCWEDOSTLKR]{1}[A-Z]{1,2} ?[A-Z0-9]{4,5}"); //todo : 3 lub 2 wielkie litery i dalej min 2 littery i 5 cyfr

   public static final Pattern URL_PATTERN = Pattern.compile("(http|https|Http|Https|www)?(:\\/\\/)?[a-zA-Z0-9\\\\._]+[\\\\.]{1}(com|pl|net|org|ru|gov){1}");

   //[A-Z]{1}[a-zA-Z]+

   private HashSet<String> firstNames;

   private HashSet<String> lastNames;

   private HashSet<String> roads;

   private HashSet<String> cities;

   private TextInterpreter()
   {
      String namesDictionaryPath = ConfigurationManager.getProperty("dictionaries.folder.path");

      try
      {
         firstNames = loadDictionary(namesDictionaryPath + "/person_first_nam.txt");
         lastNames = loadDictionary(namesDictionaryPath + "/person_last_nam.txt");
         roads = loadDictionary(namesDictionaryPath + "/road_nam.txt");
         cities = loadDictionary(namesDictionaryPath + "/city_nam.txt");
      }
      catch (IOException e)
      {
         logger.error(e);
      }

   }

   private HashSet<String> loadDictionary(String path) throws IOException
   {
      HashSet<String> set = new HashSet<>();
      BufferedReader br = new BufferedReader(new FileReader(path));
      String line;
      while ((line = br.readLine()) != null)
      {
         set.add(line.toUpperCase());
      }
      br.close();
      return set;
   }

   public static TextInterpreter getInstance()
   {
      if (interpreter == null)
      {
         interpreter = new TextInterpreter();
      }
      return interpreter;
   }

   public SensitiveType interpretate(String text)
   {
      SensitiveType result = null;

      if (StringUtils.isNotBlank(text.trim()))
      {
         if (result == null && isRegistrationPlate(text))
         {
            result = SensitiveType.PALTE;
         }

         if (result == null && isEmail(text))
         {
            result = SensitiveType.EMAIL;
         }

         if (result == null && isURL(text))
         {
            result = SensitiveType.URL;
         }

         if (result == null && isName(text))
         {
            result = SensitiveType.NAME;
         }

         if (result == null && isAddress(text))
         {
            result = SensitiveType.ADDRESS;
         }

         if (result == null && isAddress(text))
         {
            result = SensitiveType.ADDRESS;
         }
      }
      return result;
   }

   public boolean isRegistrationPlate(String text)
   {
      if (REGISTRATION_PLATE_PATTERN.matcher(text).find())
      {
         int letters = 0;
         int numbers = 0;
         String[] chars = StringUtils.split(text.trim());
         for (String elem : chars)
         {
            if (StringUtils.isNumeric(elem))
            {
               numbers ++;
            }
            if (StringUtils.isAlpha(elem) && StringUtils.isAllUpperCase(elem))
            {
               letters ++;
            }
         }

         if (letters >= 2 && letters <= 6 && numbers >=2 && numbers <= 5 && (letters + numbers == 7 || letters + numbers == 8))
         {
            return true;
         }
      }
      return false;
   }

   public boolean isEmail(String text)
   {
      return EMAIL_PATTERN.matcher(text).find();
   }

   public boolean isPhoneNumber(String text)
   {
      return PHONE_PATTERN.matcher(text).find();
   }

   public boolean isURL(String text)
   {
      return URL_PATTERN.matcher(text).find();
   }

   public boolean isAddress(String text)
   {
      if  (POSTAL_CODE_PATTERN.matcher(text).find())
      {
         return true;
      }
      if (roads.contains(text))
      {
         return true;
      }
      if (cities.contains(text))
      {
         return true;
      }
      return false;
   }

   public boolean isName(String text)
   {
      String upper = text.trim().toUpperCase();
      if (firstNames.contains(upper))
      {
         return true;
      }
      if (lastNames.contains(upper))
      {
         return true;
      }
      return false;
   }
}
