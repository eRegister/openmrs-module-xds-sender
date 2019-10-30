package org.openmrs.module.xdssender.api.service;

import javax.xml.bind.JAXBElement;
import java.util.Date;
import java.util.List;

import org.dcm4chee.xds2.infoset.rim.AdhocQueryRequest;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryResponse;
import org.dcm4chee.xds2.infoset.rim.ExtrinsicObjectType;
import org.dcm4chee.xds2.infoset.rim.RegistryResponseType;
import org.openmrs.module.xdssender.api.model.AdhocQueryDocumentData;
import org.openmrs.module.xdssender.api.model.AdhocQueryInfo;

public interface XdsAdhocQueryService {
	AdhocQueryResponse queryXdsRegistry(AdhocQueryRequest adhocQueryRequest);
	List<AdhocQueryDocumentData> queryXdsRegistry(String patientIdentifier, Date fromDate, Date toDate);
	String queryXdsRegistryString(String patientUuid);
}
