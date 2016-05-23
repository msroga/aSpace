package org.dspace.anonim.services;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Marek on 2016-03-27.
 */
public interface IFaceAnonimizer
{
   InputStream findAndBlurFaces(InputStream inputStream, String ext) throws IOException;
}
