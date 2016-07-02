package org.dspace.anonim.services;

import org.dspace.content.Metadatum;

/**
 * Created by Marek on 2016-05-24.
 */
public interface IMetadataAnonimizer
{
   void anonymize(Metadatum[] values);
}
