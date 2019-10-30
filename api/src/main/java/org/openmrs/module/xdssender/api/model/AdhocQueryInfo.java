package org.openmrs.module.xdssender.api.model;

import javax.xml.bind.JAXBElement;

import com.sun.corba.se.spi.ior.Identifiable;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryResponse;
import org.dcm4chee.xds2.infoset.rim.ClassificationType;
import org.dcm4chee.xds2.infoset.rim.ExternalIdentifierType;
import org.dcm4chee.xds2.infoset.rim.ExtrinsicObjectType;
import org.dcm4chee.xds2.infoset.rim.IdentifiableType;
import org.dcm4chee.xds2.infoset.rim.RegistryObjectListType;
import org.dcm4chee.xds2.infoset.rim.RegistryResponseType;
import org.dcm4chee.xds2.infoset.rim.SlotType1;
import org.openmrs.module.xdssender.api.xds.ExtrinsicObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Represents basic information about an adhoc query response
 *
 * @author Teboho
 */
public class AdhocQueryInfo {

	// Response status
	private String status;

	// AdhocQueryResponse
	private AdhocQueryResponse adhocQueryResponse;

	//List of AdhocQuery documents
	private List<AdhocQueryDocumentData> adhocQueryDocumentList;

	private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm";

	public AdhocQueryInfo(){
		this.adhocQueryDocumentList = new ArrayList<AdhocQueryDocumentData>();
	}

	public AdhocQueryInfo(AdhocQueryResponse adhocQueryResponse, String status) {
		this.adhocQueryResponse = adhocQueryResponse;
		this.status = status;
		this.adhocQueryDocumentList = new ArrayList<AdhocQueryDocumentData>();
	}

	public AdhocQueryInfo(AdhocQueryResponse adhocQueryResponse, String status, List<AdhocQueryDocumentData> adhocQueryDocumentList ) {
		this.adhocQueryResponse = adhocQueryResponse;
		this.status = status;
		this.adhocQueryDocumentList = adhocQueryDocumentList;
	}

	public List<AdhocQueryDocumentData> getAdhocQueryDocumentList() {
		return this.adhocQueryDocumentList;
	}

	public void setAdhocQueryDocumentList(List<AdhocQueryDocumentData> adhocQueryDocumentList){
		this.adhocQueryDocumentList = adhocQueryDocumentList;
	}

	/**
	 * Gets the registry object list
	 *
	 * @return RegistryObjectListType
	 */
	public AdhocQueryResponse getAdhocQueryResponse() {
		return this.adhocQueryResponse;
	}

	/**
	 * Sets the registry object list
	 *
	 * @param adhocQueryResponse
	 */
	public void setAdhocQueryResponse(AdhocQueryResponse adhocQueryResponse) {
		this.adhocQueryResponse = adhocQueryResponse;
	}

	/**
	 * Gets the adhoc query response status
	 *
	 * @return String
	 */
	public String getStatus() {
		return this.status;
	}

	/**
	 * Sets the adhoc query response status
	 *
	 * @param status
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	public void populateAdhocQueryDocumentList(){
		// The JAXBElement represent the actual patient clinical encounter, expecting at least 3 for 3 months
		// The JAXBElement in this case is the Extrinsic Object
		// Each Extrinsic Object should be represented by an AdhocQueryDocumentData object
		for(JAXBElement jaxbElement : this.adhocQueryResponse.getRegistryObjectList().getIdentifiable()){
			ExtrinsicObjectType eo = (ExtrinsicObjectType)jaxbElement.getValue();
			AdhocQueryDocumentData data = new AdhocQueryDocumentData();
			populateSlotData(eo, data);
			populateClassificationData(eo, data);
			populateExternalIdentifierData(eo, data);
			this.adhocQueryDocumentList.add(data);
		}
	}

	private void populateSlotData(ExtrinsicObjectType eo, AdhocQueryDocumentData data){
		for(SlotType1 slot : eo.getSlot()){
			if(slot.getName().equalsIgnoreCase("serviceStartTime")){
				data.setEncounterDate(slot.getValueList().getValue().get(0));
			}
			if(slot.getName().equalsIgnoreCase("sourcePatientId")){
				data.setPatientID(slot.getValueList().getValue().get(0));
			}
		}
	}

	private void populateClassificationData(ExtrinsicObjectType eo, AdhocQueryDocumentData data){
		for(ClassificationType ct : eo.getClassification()){
			if("urn:uuid:93606bcf-9494-43ec-9b4e-a7748d1a838d".equalsIgnoreCase(ct.getClassificationScheme())){
				for(SlotType1 slot : ct.getSlot()){
					if("authorInstitution".equalsIgnoreCase(slot.getName())) {
						data.setLocation(slot.getValueList().getValue().get(0));
					}
					if("authorPerson".equalsIgnoreCase(slot.getName())){
						data.setProviderID(slot.getValueList().getValue().get(0));
					}
				}
			}
		}
	}

	private void populateExternalIdentifierData(ExtrinsicObjectType eo, AdhocQueryDocumentData data){
		for(ExternalIdentifierType eit : eo.getExternalIdentifier()) {
			if("XDSDocumentEntry.uniqueId".equalsIgnoreCase(eit.getName().getLocalizedString().get(0).getValue())) {
				data.setDocumentID(eit.getValue());
			}
		}
	}
}
