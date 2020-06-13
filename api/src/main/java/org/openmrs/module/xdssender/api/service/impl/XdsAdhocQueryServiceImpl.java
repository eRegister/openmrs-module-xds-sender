package org.openmrs.module.xdssender.api.service.impl;

import javax.xml.bind.JAXBElement;
import javax.xml.ws.BindingProvider;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryRequest;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryResponse;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryType;
import org.dcm4chee.xds2.infoset.rim.ExtrinsicObjectType;
import org.dcm4chee.xds2.infoset.rim.IdentifiableType;
import org.dcm4chee.xds2.infoset.rim.ObjectFactory;
import org.dcm4chee.xds2.infoset.rim.RegistryResponseType;
import org.dcm4chee.xds2.infoset.rim.ResponseOptionType;
import org.dcm4chee.xds2.infoset.rim.SlotType1;
import org.dcm4chee.xds2.infoset.rim.ValueListType;
import org.dcm4chee.xds2.infoset.util.DocumentRegistryPortTypeFactory;
import org.dcm4chee.xds2.infoset.ws.registry.DocumentRegistryPortType;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.xdssender.XdsSenderConfig;
import org.openmrs.module.xdssender.api.handler.AuthenticationHandler;
import org.openmrs.module.xdssender.api.handler.XdsDocumentMessageHandler;
import org.openmrs.module.xdssender.api.model.AdhocQueryDocumentData;
import org.openmrs.module.xdssender.api.model.AdhocQueryInfo;
import org.openmrs.module.xdssender.api.service.XdsAdhocQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

public class XdsAdhocQueryServiceImpl extends BaseOpenmrsService implements XdsAdhocQueryService {

	@Autowired
	private XdsSenderConfig config;

	@Autowired
	private AuthenticationHandler authenticationHandler;

	@Autowired
	private XdsDocumentMessageHandler xdsDocumentMessageHandler;

	private static ObjectFactory factory = new ObjectFactory();
	private List<SlotType1> paramSlots;
	public static final String TEST_PAT_ID = "'A2052002908/18^^^&2.25.71280592878078638113873461180761116318&PI'";
	private static final String DATE_PATTERN = "yyyyMMdd";
	private static final String START_TIME = "0800";
	private static final String STOP_TIME = "2300";

	private static final SlotType1[] DEFAULT_PARAMS = new SlotType1[] {
			toQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_PATIENT_ID, TEST_PAT_ID),
			toQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_STATUS, "('"+XDSConstants.STATUS_APPROVED+"')")
	};

	@Override
	public AdhocQueryResponse queryXdsRegistry(AdhocQueryRequest adhocQueryRequest) {
		DocumentRegistryPortTypeFactory.addHandler((BindingProvider) DocumentRegistryPortTypeFactory
				.getDocumentRegistryPortSoap12(config.getXdsRegistryEndpoint()), authenticationHandler);

		DocumentRegistryPortType port2 = DocumentRegistryPortTypeFactory.getDocumentRegistryPortSoap12(
				config.getXdsRegistryEndpoint());

		DocumentRegistryPortTypeFactory.addHandler((BindingProvider) port2, xdsDocumentMessageHandler);

		((BindingProvider) port2).getRequestContext().put(BindingProvider.USERNAME_PROPERTY,
				config.getXdsRepositoryUsername());
		((BindingProvider) port2).getRequestContext().put(BindingProvider.PASSWORD_PROPERTY,
				config.getXdsRepositoryPassword());

		return port2.documentRegistryRegistryStoredQuery(adhocQueryRequest);
	}

	@Override
	public List<AdhocQueryDocumentData> queryXdsRegistry(String patientIdentifier, Date fromDate, Date toDate) {
		String PATIENT_ID = "'" + patientIdentifier + "^^^&" + config.getCodeNationalRoot() + "&PI'";
		String DOCUMENT_ENTRY_STATUS = "('"+XDSConstants.STATUS_APPROVED+"')";
		String SERVICE_START_TIME = formatDate(fromDate) + START_TIME;
		String SERVICE_STOP_TIME = formatDate(toDate) + STOP_TIME;

		SlotType1[] DOC_PARAMS = new SlotType1[] {
				toQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_PATIENT_ID, PATIENT_ID),
				toQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_STATUS, DOCUMENT_ENTRY_STATUS),
				toQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_SERVICE_START_TIME_FROM, SERVICE_START_TIME),
				toQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_SERVICE_STOP_TIME_TO, SERVICE_STOP_TIME)
		};

		AdhocQueryRequest adhocQueryRequest = getQueryRequest(XDSConstants.XDS_FindDocuments,
				XDSConstants.QUERY_RETURN_TYPE_LEAF, DOC_PARAMS);
		AdhocQueryResponse adhocQueryResponse = queryXdsRegistry(adhocQueryRequest);
		AdhocQueryInfo response = new AdhocQueryInfo(adhocQueryResponse, adhocQueryResponse.getStatus());
		response.populateAdhocQueryDocumentList();

		ArrayList<AdhocQueryDocumentData> arrayList = new ArrayList<AdhocQueryDocumentData>();

		for(AdhocQueryDocumentData data : response.getAdhocQueryDocumentList()){
			AdhocQueryDocumentData elem = new AdhocQueryDocumentData();
			elem.setDocumentID(data.getDocumentID());
			elem.setPatientID(data.getPatientID());
			elem.setProviderID(data.getProviderID());
			elem.setEncounterDate(data.getEncounterDate());
			elem.setLocation(data.getLocation());
			elem.setDocumentName("ART Summary");
			elem.setDocumentType("Medical Summary");
			arrayList.add(elem);
		}
		return arrayList;
	}

	@Override
	public String queryXdsRegistryString(String patientUuid) {
		return String.format("The following string with uniqueId %s has been passed.", patientUuid);
	}

	private AdhocQueryRequest getQueryRequest(String queryId, String returnType, SlotType1[] defaults) {
		AdhocQueryRequest req = factory.createAdhocQueryRequest();
		ResponseOptionType responseOption = factory.createResponseOptionType();
		responseOption.setReturnComposedObjects(true);
		responseOption.setReturnType(returnType);
		req.setResponseOption(responseOption);
		AdhocQueryType adhocQuery = factory.createAdhocQueryType();
		adhocQuery.setId(queryId);
		paramSlots = adhocQuery.getSlot();
		if (defaults != null) {
			for (int i = 0 ; i < defaults.length ; i++) {
				paramSlots.add(defaults[i]);
			}
		}
		req.setAdhocQuery(adhocQuery);
		return req;
	}

	public static SlotType1 toQueryParam(String name, String value) {
		SlotType1 slot = factory.createSlotType1();
		slot.setName(name);
		ValueListType valueList = factory.createValueListType();
		valueList.getValue().add(value);
		slot.setValueList(valueList);
		return slot;
	}

	private String formatDate(Date date){
		DateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);
		String formatedDate = dateFormat.format(date);
		return formatedDate;
	}
}
