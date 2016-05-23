package org.dspace.anonim.services.impl;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.anonim.services.IFaceAnonimizer;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Marek on 2016-03-27.
 */
@Service("faceAnonimizer")
public class FaceAnonimizerImpl implements InitializingBean, IFaceAnonimizer
{
   private static ITesseract tesseract;

   @Override
   public void afterPropertiesSet() throws Exception
   {
      System.load("C:\\Program Files\\opencv\\build\\java\\x64\\opencv_java249.dll"); //fixme myo
      ITesseract instance = new Tesseract();  // JNA Interface Mapping
      instance.setDatapath("C:\\Program Files\\Tesseract OCR");
      instance.setLanguage("pol");
   }

   @Override
   public InputStream findAndBlurFaces(InputStream inputStream, String ext) throws IOException
   {
      CascadeClassifier faceDetector = new CascadeClassifier("C:/dspace/haarcascade_frontalface_alt.xml");

      MatOfByte file = new MatOfByte();
      file.fromArray(IOUtils.toByteArray(inputStream));
      Mat image = Highgui.imdecode(file, Highgui.IMREAD_COLOR);

      MatOfRect faceDetections = new MatOfRect();
      faceDetector.detectMultiScale(image, faceDetections);


      for (Rect rect : faceDetections.toArray()) {
         rect.width = rect.width % 2 == 1 ? rect.width : rect.width + 1;
         rect.height = rect.height % 2 == 1 ? rect.height : rect.height + 1;
         Imgproc.GaussianBlur(image.submat(rect), image.submat(rect), new Size(rect.width, rect.height), 55);
      }

      Highgui.imencode(ext, image, file);
      return new ByteArrayInputStream(file.toArray());
   }

   //todo: myo better algoritm?
   public static InputStream findAndBlurText(InputStream inputStream, String ext) throws IOException, TesseractException
   {
      MatOfByte file = new MatOfByte();
      file.fromArray(IOUtils.toByteArray(inputStream));
      Mat image = Highgui.imdecode(file, Highgui.IMREAD_COLOR);

      Mat src_gray = new Mat();
      Imgproc.cvtColor(image, src_gray, Imgproc.COLOR_RGB2GRAY);

      Mat readable = new Mat();
      Imgproc.threshold(src_gray, readable, 128, 255, Imgproc.THRESH_OTSU);
      Imgproc.GaussianBlur(readable, readable, new Size(5, 5), 0);
      Imgproc.threshold(readable, readable, 128, 255, Imgproc.THRESH_OTSU);

      Mat morphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(6,6));
      Imgproc.morphologyEx(src_gray, src_gray, Imgproc.MORPH_GRADIENT, morphKernel);

      Mat bw = new Mat();
      Imgproc.threshold(src_gray, bw, 128, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
      morphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(15,1));
      Mat connected = new Mat();
      Imgproc.morphologyEx(bw, connected, Imgproc.MORPH_CLOSE, morphKernel);

      Mat mask = Mat.zeros(bw.size(), CvType.CV_8UC1);
      List<MatOfPoint> contours = new ArrayList<>();
      MatOfInt4 hierarchy = new MatOfInt4();
      Imgproc.findContours(connected, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0,0));

      Highgui.imencode(ext, readable, file);
      BufferedImage inputImage = ImageIO.read(new ByteArrayInputStream(file.toArray()));
      for (int idx = 0; idx < hierarchy.total(); idx ++)
      {
         MatOfPoint point = contours.get(idx);
         Rect rect = Imgproc.boundingRect(point);
         Mat maskROI = mask.submat(rect);
         maskROI = maskROI.setTo(new Scalar(0,0,0));

         Imgproc.drawContours(mask, contours, idx, new Scalar(255, 255, 255), Core.FILLED);

         double r = (double) Core.countNonZero(maskROI) / (rect.width*rect.height);
         if (r > 0.45 && (rect.height > 16 && rect.width > 16))
         {
            String result = tesseract.doOCR(inputImage, new Rectangle(rect.x, rect.y, rect.width, rect.height));
            if (isSensitiveData(result))
            {
               rect.width = rect.width % 2 == 1 ? rect.width : rect.width + 1;
               rect.height = rect.height % 2 == 1 ? rect.height : rect.height + 1;
               Imgproc.GaussianBlur(image.submat(rect), image.submat(rect), new Size(rect.width, rect.height), 55);
            }
         }
      }

      Highgui.imencode(ext, image, file);
      return new ByteArrayInputStream(file.toArray());
   }

   public static boolean isSensitiveData(String result)
   {
      if (StringUtils.isNotBlank(result))
      {
         result = result.trim();
         if (result.length() > 1)
         {
            return true;
         }
      }
      return false;
   }

//   public static void main(String argv[])
//   {
//      try
//      {
//         System.load("C:\\Program Files\\opencv\\build\\java\\x64\\opencv_java249.dll");
//         File file = new File("C:\\Users\\Marek\\Pictures\\tempates\\Game-Thrones-Comic-Con-Panel-2014.jpg");
//         InputStream result = findAndBlurFaces(new FileInputStream(file), ".jpg");
//         FileOutputStream resultFile = new FileOutputStream("C:\\Users\\Marek\\Pictures\\tempates\\result.jpg");
//         int read = 0;
//         byte[] bytes = new byte[1024];
//
//
//         while ((read = result.read(bytes)) != -1) {
//            resultFile.write(bytes, 0, read);
//         }
//      }
//      catch (Exception e)
//      {
//         e.printStackTrace();
//      }
//   }

   public static void main(String argv[])
   {
      try
      {
         System.load("C:\\Program Files\\opencv\\build\\java\\x64\\opencv_java249.dll");

         tesseract = new Tesseract();  // JNA Interface Mapping
         tesseract.setDatapath("C:\\Program Files\\Tesseract OCR");
         tesseract.setLanguage("pol");

//         File fileSource = new File("C:\\Users\\Marek\\Pictures\\tempates\\wiz2.jpg");
         File fileSource = new File("C:\\Users\\Marek\\Pictures\\tempates\\FakturaVAT_29_12_2009.png");
//         File fileSource = new File("C:\\Users\\Marek\\Pictures\\tempates\\lista_studentow.jpg");

         InputStream result = findAndBlurText(new FileInputStream(fileSource), ".jpg");

         FileOutputStream resultFile = new FileOutputStream("C:\\Users\\Marek\\Pictures\\tempates\\resultOCR.jpg");
         int read = 0;
         byte[] bytes = new byte[1024];

         while ((read = result.read(bytes)) != -1) {
            resultFile.write(bytes, 0, read);
         }


      } catch (Exception e) {
         System.err.println(e.getMessage());
      }
   }

}
