package org.dspace.app.webui.servlet;

import org.apache.log4j.Logger;
import org.dspace.anonim.services.IFaceAnonimizer;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamBackup;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.workflow.WorkflowItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

/**
 * Created by Marek on 2016-05-07.
 */
public class AnonymizeServlet extends DSpaceServlet
{
   /** log4j category */
   private static Logger log = Logger.getLogger(RetrieveServlet.class);

   /**
    * Threshold on Bitstream size before content-disposition will be set.
    */
   private int threshold;

   @Autowired
   private IFaceAnonimizer faceAnonimizer;


   @Override
   public void init(ServletConfig arg0) throws ServletException
   {

      super.init(arg0);
      threshold = ConfigurationManager.getIntProperty("webui.content_disposition_threshold");
      SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
   }

   protected void doDSGet(Context context, HttpServletRequest request,
                          HttpServletResponse response) throws ServletException, IOException,
           SQLException, AuthorizeException
   {
      boolean displayLicense = ConfigurationManager.getBooleanProperty("webui.licence_bundle.show", false);
      boolean isLicense = false;

      Integer bitstreamId = Integer.parseInt(request.getParameter("bitstream-id"));
      int itemID = Integer.parseInt(request.getParameter("item-id"));
      WorkflowItem workflowItem = WorkflowItem.findByItem(context, Item.find(context, itemID));

      // Find the corresponding bitstream
      Bitstream bitstream = Bitstream.find(context, bitstreamId);

      // Did we get a bitstream?
      if (bitstream != null)
      {

         // Check whether we got a License and if it should be displayed
         // (Note: list of bundles may be empty array, if a bitstream is a Community/Collection logo)
         Bundle bundle = bitstream.getBundles().length>0 ? bitstream.getBundles()[0] : null;

         if (bundle!=null &&
                 bundle.getName().equals(Constants.LICENSE_BUNDLE_NAME) &&
                 bitstream.getName().equals(Constants.LICENSE_BITSTREAM_NAME))
         {
            isLicense = true;
         }

         if (isLicense && !displayLicense && !AuthorizeManager.isAdmin(context))
         {
            throw new AuthorizeException();
         }
         log.info(LogManager.getHeader(context, "view_bitstream", "bitstream_id=" + bitstream.getID()));

         InputStream result = faceAnonimizer.findAndBlurFaces(bitstream.retrieve(), "." + bitstream.getFormat().getExtensions()[0]);

         BitstreamBackup bitstreamBackup = BitstreamBackup.create(context, bitstream); //backup
         bundle.removeBitstream(bitstream);
         bitstream.deleteForever(); //delete orginal
         Bitstream anonimized = Bitstream.create(context, result); //create anonimized
         anonimized.setFormat(bitstreamBackup.getFormat());
         anonimized.setName(bitstreamBackup.getName());
         anonimized.setDescription(bitstreamBackup.getDescription());
         anonimized.update();
         bitstreamBackup.setAnonimizedID(anonimized.getID());
         bitstreamBackup.update();
         bundle.addBitstream(anonimized); //add new bitstream
         bundle.update();

         // commit all changes to database
         context.commit();

         request.setAttribute("workflow.item", workflowItem);
         JSPManager.showJSP(request, response, "/mydspace/perform-task.jsp");
      }
      else
      {
         // No bitstream - we got an invalid ID
         log.info(LogManager.getHeader(context, "view_bitstream",
                 "invalid_bitstream_id=" + bitstreamId));

         JSPManager.showInvalidIDError(request, response, bitstreamId.toString(),
                 Constants.BITSTREAM);
      }
   }
}
