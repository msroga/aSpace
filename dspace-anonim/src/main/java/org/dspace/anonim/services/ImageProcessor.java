package org.dspace.anonim.services;

import org.apache.commons.io.IOUtils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Marek on 2016-06-29.
 */
public class ImageProcessor
{
   private MatOfByte file;

   private Mat image;

   private Mat src_gray;

   private String ext;

   public ImageProcessor(String extension)
   {
      file = new MatOfByte();
      image = new Mat();
      src_gray = new Mat();
      ext = extension;
   }

   public void loadImage(InputStream inputStream) throws IOException
   {
      file.fromArray(IOUtils.toByteArray(inputStream));
      image = Highgui.imdecode(file, Highgui.IMREAD_COLOR);
      Imgproc.cvtColor(image, src_gray, Imgproc.COLOR_RGB2GRAY);
   }

   public ImageProcessor save()
   {
      Highgui.imencode(ext, image, file);
      image = Highgui.imdecode(file, Highgui.IMREAD_COLOR);
      return this;
   }

   public InputStream getResult()
   {
      return new ByteArrayInputStream(file.toArray());
   }

   public MatOfByte getFile()
   {
      return file;
   }

   public Mat getImage()
   {
      return image;
   }

   public Mat getGrayImage()
   {
      return src_gray;
   }

   public String getExtension()
   {
      return ext;
   }
}
