package org.openmrs.module.xdssender.api.service.impl;
import javax.xml.ws.BindingProvider;

import java.io.IOException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetResponseType;
import org.openmrs.module.xdssender.api.service.XdsRetrieveDocumentSetService;

import org.dcm4chee.xds2.infoset.ihe.ProvideAndRegisterDocumentSetRequestType;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetRequestType;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetResponseType;
import org.dcm4chee.xds2.infoset.util.DocumentRegistryPortTypeFactory;
import org.dcm4chee.xds2.infoset.util.DocumentRepositoryPortTypeFactory;
import org.dcm4chee.xds2.infoset.ws.registry.DocumentRegistryPortType;
import org.dcm4chee.xds2.infoset.ws.repository.DocumentRepositoryPortType;
import org.openmrs.module.xdssender.XdsSenderConfig;
import org.openmrs.module.xdssender.api.handler.AuthenticationHandler;
import org.openmrs.module.xdssender.api.handler.XdsDocumentMessageHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by Teboho on 2019-03-21.
 */

@Component("xdsSender.XdsRetrieveDocumentSetService")
public class XdsRetriveDocumentSetServiceImpl implements XdsRetrieveDocumentSetService {

	@Autowired
	private XdsSenderConfig config;

	@Autowired
	private AuthenticationHandler authenticationHandler;

	@Autowired
	private XdsDocumentMessageHandler xdsDocumentMessageHandler;

	@Override
	public RetrieveDocumentSetResponseType retrieveDocumentSet(String documentId) {
		RetrieveDocumentSetRequestType request = createRetrieveDocumentSetbRequest(documentId);

		DocumentRepositoryPortTypeFactory.addHandler((BindingProvider) DocumentRepositoryPortTypeFactory
				.getDocumentRepositoryPortSoap12(config.getXdsRepositoryEndpoint()), authenticationHandler);

		DocumentRepositoryPortType port = DocumentRepositoryPortTypeFactory.getDocumentRepositoryPortSoap12(config
				.getXdsRepositoryEndpoint());

		DocumentRepositoryPortTypeFactory.addHandler((BindingProvider) port, xdsDocumentMessageHandler);

		((BindingProvider) port).getRequestContext().put(BindingProvider.USERNAME_PROPERTY,
				config.getXdsRepositoryUsername());
		((BindingProvider) port).getRequestContext().put(BindingProvider.PASSWORD_PROPERTY,
				config.getXdsRepositoryPassword());

		return port.documentRepositoryRetrieveDocumentSet(request);
	}

	@Override
	public String RetrieveDocument(String documentId) {
		RetrieveDocumentSetResponseType response = retrieveDocumentSet(documentId);
		String document;
		try {
			document = IOUtils.toString(response.getDocumentResponse().get(0).getDocument().getInputStream());
			return document;
		}catch(IOException ex){
			//throw new IOException("Failed to read response document for retrieve document set", ex);
		}
		return "";
	}

	private RetrieveDocumentSetRequestType createRetrieveDocumentSetbRequest(String documentId){
		RetrieveDocumentSetRequestType retrieveDocumentSetRequestType = new RetrieveDocumentSetRequestType();
		RetrieveDocumentSetRequestType.DocumentRequest documentRequest = new RetrieveDocumentSetRequestType.DocumentRequest();
		documentRequest.setDocumentUniqueId(documentId);
		documentRequest.setRepositoryUniqueId("1.19.6.24.109.42.1.5.1");
		retrieveDocumentSetRequestType.getDocumentRequest().add(documentRequest);
		return retrieveDocumentSetRequestType;
	}
}
