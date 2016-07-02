package org.dspace.anonim.services;

import net.sourceforge.tess4j.TesseractException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Marek on 2016-03-27.
 */
public interface IFaceAnonimizer
{
   InputStream anonymize(InputStream inputStream, String ext) throws IOException, TesseractException;
}
