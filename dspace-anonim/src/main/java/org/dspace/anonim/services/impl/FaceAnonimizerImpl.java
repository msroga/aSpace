package org.dspace.anonim.services.impl;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.commons.lang3.StringUtils;
import org.dspace.anonim.services.IFaceAnonimizer;
import org.dspace.anonim.services.ImageProcessor;
import org.dspace.anonim.services.TextInterpreter;
import org.dspace.core.ConfigurationManager;
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
import java.util.regex.Pattern;

/**
 * Created by Marek on 2016-03-27.
 */
@Service("faceAnonimizer")
public class FaceAnonimizerImpl implements InitializingBean, IFaceAnonimizer
{
   private static ITesseract tesseract;

   private static TextInterpreter textInterpreter = TextInterpreter.getInstance();

   private static CascadeClassifier faceDetector;

   private static Pattern pattern = Pattern.compile("[^a-zA-Z0-9ąćęłóńśźżĄĆĘŁŃÓŚŹŻ]*[a-zA-Z0-9ąćęłóńśźżĄĆĘŁŃÓŚŹŻ]{2,}[^a-zA-Z0-9ąćęłóńśźżĄĆĘŁŃÓŚŹŻ]*");

   @Override
   public void afterPropertiesSet() throws Exception
   {
      String tesseractPath = ConfigurationManager.getProperty("tesseract.path");
      String tesseractLang = ConfigurationManager.getProperty("tesseract.lang");
      String openCVDllPath = ConfigurationManager.getProperty("opencv.dll.path");
      String cascade = ConfigurationManager.getProperty("faces.model");
      System.load(openCVDllPath);
      tesseract = new Tesseract();  // JNA Interface Mapping
      tesseract.setDatapath(tesseractPath);
      tesseract.setLanguage(tesseractLang);
      faceDetector = new CascadeClassifier(cascade);
   }

   @Override
   public InputStream anonymize(InputStream inputStream, String ext) throws IOException, TesseractException
   {
      ImageProcessor imageProcessor = new ImageProcessor(ext);
      imageProcessor.loadImage(inputStream);

      findAndBlurFaces(imageProcessor);
      findAndBlurText(imageProcessor);

      return imageProcessor.getResult();
   }

   private static void findAndBlurFaces(Mat image) throws IOException
   {
      MatOfRect faceDetections = new MatOfRect();
      faceDetector.detectMultiScale(image, faceDetections);

      for (Rect rect : faceDetections.toArray()) {
         rect.width = rect.width % 2 == 1 ? rect.width : rect.width + 1;
         rect.height = rect.height % 2 == 1 ? rect.height : rect.height + 1;
         Imgproc.GaussianBlur(image.submat(rect), image.submat(rect), new Size(rect.width, rect.height), 55);
      }
   }

   public static void findAndBlurFaces(ImageProcessor imageProcessor) throws IOException
   {
      Mat image = imageProcessor.getImage();
      for (int i = 0 ; i < 4; i++)
      {
         Core.transpose(image, image);
         Core.flip(image, image, 1);
         findAndBlurFaces(image);
      }
      imageProcessor.save();
   }

   public static void findAndBlurText(ImageProcessor imageProcessor) throws IOException, TesseractException
   {
      Mat image = imageProcessor.getImage();
      Mat src_gray = imageProcessor.getGrayImage();

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

      MatOfByte file = new MatOfByte();
      Highgui.imencode(imageProcessor.getExtension(), readable, file);
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
      imageProcessor.save();
   }

   public static void testFindAndBlurText(ImageProcessor imageProcessor) throws IOException, TesseractException
   {
      Mat image = imageProcessor.getImage();
//      System.out.println("rows: " + image.rows() + " cols: " + image.cols());
//      System.out.println("height: " + image.height() + " width: " + image.width());
      Mat src_gray = imageProcessor.getGrayImage();

      Mat morph = new Mat();
      Mat morphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(6,6));
      Imgproc.morphologyEx(src_gray, morph, Imgproc.MORPH_GRADIENT, morphKernel); //kontury

      Mat readable = new Mat();
      Mat bw = new Mat();

      Imgproc.threshold(morph, readable, 128, 255, Imgproc.THRESH_BINARY);//wersja readable
      Imgproc.GaussianBlur(readable, bw, new Size(25, 25), 0);
      Imgproc.threshold(bw, bw, 128, 255, Imgproc.THRESH_OTSU);

      morphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(15,2));
      Mat lines = new Mat();
      Imgproc.morphologyEx(bw, lines, Imgproc.MORPH_CLOSE, morphKernel);

      Mat connected = new Mat();
      morphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(30,1));
      Imgproc.dilate(lines, connected, morphKernel);

      Mat mask = Mat.zeros(bw.size(), CvType.CV_8UC1);
      List<MatOfPoint> contours = new ArrayList<>();
      MatOfInt4 hierarchy = new MatOfInt4();
      Imgproc.findContours(connected, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0,0));

      MatOfByte file = new MatOfByte();
      Highgui.imencode(imageProcessor.getExtension(), src_gray, file);
      BufferedImage inputImage = ImageIO.read(new ByteArrayInputStream(file.toArray()));
      long timeOCR = 0;
      for (int idx = 0; idx < hierarchy.total(); idx ++)
      {
         MatOfPoint point = contours.get(idx);
         Rect rect = Imgproc.boundingRect(point);
         Imgproc.drawContours(mask, contours, idx, new Scalar(255, 255, 255), Core.FILLED);

//            rect.width = rect.width % 2 == 1 ? rect.width : rect.width + 1;
//            rect.height = rect.height % 2 == 1 ? rect.height : rect.height + 1;
//            Core.rectangle(image, rect.br(), rect.tl(), new Scalar(255, 255, 0));

         if (rect.height > 15 && rect.height < rect.width)
         {
            long tStart = System.currentTimeMillis();
            String result = tesseract.doOCR(inputImage, new Rectangle(rect.x, rect.y, rect.width, rect.height));
            long tEnd = System.currentTimeMillis();
            timeOCR += (tEnd - tStart);

            if (StringUtils.isNotBlank(result) && pattern.matcher(result).find())
            {
//               result = result.replace(";","");
//               result = result.replace("\n","");
//               System.out.print("(" + result + ")");
               rect.width = rect.width % 2 == 1 ? rect.width : rect.width + 1;
               rect.height = rect.height % 2 == 1 ? rect.height : rect.height + 1;
               Core.rectangle(image, rect.br(), rect.tl(), new Scalar(0, 255, 0), 2);
//               if (isSensitiveData(result))
//               {
//                  rect.width = rect.width % 2 == 1 ? rect.width : rect.width + 1;
//                  rect.height = rect.height % 2 == 1 ? rect.height : rect.height + 1;
//                  Imgproc.GaussianBlur(image.submat(rect), image.submat(rect), new Size(rect.width, rect.height), 55);
//               }

            }
         }
      }
      //System.out.println("OCR time[s]: " + timeOCR / 1000.0);
      System.out.print(";" + (timeOCR / 1000.0) + ";");
      imageProcessor.save();
      //Highgui.imencode(".jpeg", lines, imageProcessor.getFile());
   }


   private static boolean isSensitiveData(String result)
   {
      if (textInterpreter.interpretate(result) != null)
      {
            return true;
      }
      return false;
   }

   public static void main2(String argv[])
   {
      try
      {
//         File file = new File("C:\\Users\\Marek\\Desktop\\mgr\\testy\\ja2.jpg");
//         File file = new File("C:\\Users\\Marek\\Pictures\\tempates\\Game-Thrones-Comic-Con-Panel-2014.jpg");
         System.load("C:\\Program Files\\opencv\\build\\java\\x64\\opencv_java249.dll");
         faceDetector = new CascadeClassifier("C:/dspace/haarcascade_frontalface_alt.xml");

         String folderPath = "C:\\Users\\Marek\\Desktop\\mgr\\testy\\face";
         File folder = new File(folderPath);

         System.out.println("File;Size;WorkTime;");

         for (File file : folder.listFiles())
         {
            if (!file.isDirectory())
            {
               double size = (file.length() / 1024);
               //System.out.println("procesing " + file.getName() + " size[KB]: " + size);
               InputStream inputstrem = new FileInputStream(file);
               ImageProcessor imageProcessor = new ImageProcessor(".jpeg");
               imageProcessor.loadImage(inputstrem);

               long tStart = System.currentTimeMillis();
               findAndBlurFaces(imageProcessor);
               long tEnd = System.currentTimeMillis();
               double timeSeconds = (tEnd - tStart) / 1000.0;
              // System.out.println("work time [s]: " + timeSeconds);
               System.out.println(file.getName() + ";" + size + ";" + timeSeconds + ";");


               InputStream result = imageProcessor.getResult();
               inputstrem.close();

               FileOutputStream resultFile = new FileOutputStream(folderPath + "\\results\\" + file.getName());
               int read = 0;
               byte[] bytes = new byte[1024];
               while ((read = result.read(bytes)) != -1) {
                  resultFile.write(bytes, 0, read);
               }

               result.close();
               resultFile.close();
            }
         }
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

   public static void main(String argv[])
   {
      try
      {
         System.load("C:\\Program Files\\opencv\\build\\java\\x64\\opencv_java249.dll");
         tesseract = new Tesseract();  // JNA Interface Mapping
         tesseract.setDatapath("C:\\Program Files\\Tesseract OCR");
         tesseract.setLanguage("pol");

         String folderPath = "C:\\Users\\Marek\\Desktop\\mgr\\testy\\text\\chars74";
         File folder = new File(folderPath);

         System.out.println("File;Size;OCRTime;WorkTime;");
         for (File file : folder.listFiles())
         {
            if (!file.isDirectory())
            {
               double size = (file.length() / 1024);
               //System.out.println("procesing " + file.getName() + " size[KB]: " + size);
               System.out.print(file.getName() + ";" + size + ";");
               InputStream inputstrem = new FileInputStream(file);

               ImageProcessor imageProcessor = new ImageProcessor(".jpeg");
               imageProcessor.loadImage(inputstrem);

               long tStart = System.currentTimeMillis();
               testFindAndBlurText(imageProcessor);
               long tEnd = System.currentTimeMillis();
               double timeSeconds = (tEnd - tStart) / 1000.0;
               //System.out.println("work time [s]: " + timeSeconds);
               System.out.println(timeSeconds + ";");
               InputStream result = imageProcessor.getResult();
               inputstrem.close();

               saveFile(result, folderPath + "\\results\\" + file.getName());

               result.close();
            }
         }

      } catch (Exception e) {
         System.err.println(e.getMessage());
      }
   }


   public static void main3(String argv[])
   {
      try
      {
         System.load("C:\\Program Files\\opencv\\build\\java\\x64\\opencv_java249.dll");
         tesseract = new Tesseract();  // JNA Interface Mapping
         tesseract.setDatapath("C:\\Program Files\\Tesseract OCR");
         tesseract.setLanguage("pol");

         String folderPath = "C:\\Users\\Marek\\Desktop\\mgr\\testy\\text";
         File ifile = new File(folderPath + "\\test.jpg");

         double size = (ifile.length() / 1024);
         System.out.println("procesing " + ifile.getName() + " size[KB]: " + size);
         InputStream inputstrem = new FileInputStream(ifile);

         ImageProcessor imageProcessor = new ImageProcessor(".jpeg");
         imageProcessor.loadImage(inputstrem);

         long tStart = System.currentTimeMillis();
         //-----------------------------------------------------------------------------------------------------------

         testFindAndBlurText(imageProcessor);

         //-----------------------------------------------------------------------------------------------------------
         long tEnd = System.currentTimeMillis();
         double timeSeconds = (tEnd - tStart) / 1000.0;
         System.out.println("work time [s]: " + timeSeconds);
         InputStream result = imageProcessor.getResult();
         inputstrem.close();

         saveFile(result, folderPath + "\\results\\" + ifile.getName());

         result.close();

      }
      catch (Exception e)
      {
         System.err.println(e.getMessage());
      }
   }


   private static void saveFile(InputStream result, String path) throws IOException
   {
      FileOutputStream resultFile = new FileOutputStream(path);
      int read = 0;
      byte[] bytes = new byte[1024];

      while ((read = result.read(bytes)) != -1)
      {
         resultFile.write(bytes, 0, read);
      }
      resultFile.close();
   }
}
