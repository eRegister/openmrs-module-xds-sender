package org.openmrs.module.xdssender.api.service;

import java.io.IOException;
import java.util.List;

import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetResponseType;

/**
 * Created by Teboho on 2019-03-21.
 */
public interface XdsRetrieveDocumentSetService {
	public RetrieveDocumentSetResponseType retrieveDocumentSet(String documentId);
	String RetrieveDocument(String documentId) throws IOException;
}
