package org.openmrs.module.xdssender.api.cda;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.marc.everest.formatters.xml.its1.XmlIts1Formatter;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalDocument;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Section;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Location;
import org.openmrs.api.context.Context;
import org.openmrs.module.xdssender.XdsSenderConstants;
import org.openmrs.module.xdssender.api.cda.model.DocumentModel;
import org.openmrs.module.xdssender.api.cda.obs.ExtendedObs;
import org.openmrs.module.xdssender.api.cda.section.impl.ActiveProblemsSectionBuilder;
import org.openmrs.module.xdssender.api.cda.section.impl.AntepartumFlowsheetPanelSectionBuilder;
import org.openmrs.module.xdssender.api.cda.section.impl.EstimatedDeliveryDateSectionBuilder;
import org.openmrs.module.xdssender.api.cda.section.impl.MedicationsSectionBuilder;
import org.openmrs.module.xdssender.api.cda.section.impl.VitalSignsSectionBuilder;
import org.openmrs.module.xdssender.api.everest.EverestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component("xdsender.ClinicalDocumentBuilder")
public class ClinicalDocumentBuilder {
	
	private final Log log = LogFactory.getLog(this.getClass());
	
	@Autowired
	private CdaMetadataUtil metadataUtil;
	
	@Autowired
	private EstimatedDeliveryDateSectionBuilder eddSectionBuilder;
	
	@Autowired
	private AntepartumFlowsheetPanelSectionBuilder flowsheetSectionBuilder;
	
	@Autowired
	private VitalSignsSectionBuilder vitalSignsSectionBuilder;
	
	@Autowired
	private MedicationsSectionBuilder medSectionBuilder;
	
	@Autowired
	private ActiveProblemsSectionBuilder probBuilder;
	
	public DocumentModel buildDocument(Patient patient, Encounter encounter) throws InstantiationException,
	        IllegalAccessException {
		
		DocumentBuilder builder = new DocumentBuilderImpl();
		
		builder.setRecordTarget(patient);
		builder.setEncounterEvent(encounter);
		
		Obs estimatedDeliveryDateObs = null, lastMenstrualPeriodObs = null, prepregnancyWeightObs = null,
				gestgationalAgeObs = null, fundalHeightObs = null, systolicBpObs = null, diastolicBpObs = null,
				weightObs = null, heightObs = null, presentationObs = null, temperatureObs = null;

		List<Obs> medicationObs = new ArrayList<Obs>();

		// TODO: REMEMBER TO INCLUDE WHO STAGING AND T STAGING - TEBOHO KOMA
		// Include HIV Sentinel events observations
		Obs firstHIVPosTestObs = null, secondHIVPosRetestObs = null, artStartDate = null, artStartRegimenObs = null,
				baselineCD4Count = null, currentCD4Count = null, currentARVRegimen = null, hivViralLoadObs = null,
				pregnancyStatusObs = null;

		log.info("DISPLAYING OBS INSIDE THE ENCOUNTER --- TEBOHO KOMA");
		for (Obs obs : encounter.getObs()) {
			log.info(obs.getConcept().getName());

			if (obs.getConcept().getName().toString().equalsIgnoreCase("WEIGHT")) {
				weightObs = new Obs();
				weightObs = obs;
			} else if (obs.getConcept().getName().toString().equalsIgnoreCase("HEIGHT")) {
				heightObs = new Obs();
				heightObs = obs;
			} else if (obs.getConcept().getName().toString().equalsIgnoreCase("Temperature")) {
				temperatureObs = new Obs();
				temperatureObs = obs;
			} else if (obs.getConcept().getName().toString().equalsIgnoreCase("Systolic")) {
				systolicBpObs = new Obs();
				systolicBpObs = obs;
			} else if (obs.getConcept().getName().toString().equalsIgnoreCase("Diastolic")) {
				diastolicBpObs = new Obs();
				diastolicBpObs = obs;
			} else if (obs.getConcept().getName().toString().equalsIgnoreCase("HTC, Final HIV status")) {
				// Check if the root concept for the Obs is the HTS Testing Register
				String rootConceptName = getRootConceptName(obs);
				if (rootConceptName.equalsIgnoreCase("HIV Testing and Counseling Intake Template")) {
					firstHIVPosTestObs = new Obs();
					firstHIVPosTestObs = obs;
				} else if (rootConceptName.equalsIgnoreCase("HIV Testing Services Retesting Template")) {
					secondHIVPosRetestObs = new Obs();
					secondHIVPosRetestObs = obs;
				}
			} else if (obs.getConcept().getName().toString().equalsIgnoreCase("HIVTC, ART start date")) {
				artStartDate = new Obs();
				artStartDate = obs;
			} else if(obs.getConcept().getName().toString().equalsIgnoreCase("HIVTC, ART Regimen")){
				// Check if the root concept for the Observation is the HIV Treatment and Intake Form
				String rootConceptName = getRootConceptName(obs);
				if(rootConceptName.equalsIgnoreCase("HIV Treatment and Care Intake Template")) {
					artStartRegimenObs = new Obs();
					artStartRegimenObs = obs;
				} else if(rootConceptName.equalsIgnoreCase("HIV Treatment and Care Progress Template")){
					currentARVRegimen = new Obs();
					currentARVRegimen = obs;
				}
			} else if (obs.getConcept().getName().toString().equalsIgnoreCase("HIVTC, CD4")) {
				// Check if the root concept for the Obs is the HTS Testing Register
				String rootConceptName = getRootConceptName(obs);
				if (rootConceptName.equalsIgnoreCase("HIV Treatment and Care Intake Template")) {
					baselineCD4Count = new Obs();
					baselineCD4Count = obs;
				} else if (rootConceptName.equalsIgnoreCase("HIV Treatment and Care Progress Template")) {
					currentCD4Count = new Obs();
					currentCD4Count = obs;
				}
			} else if (obs.getConcept().getName().toString().equalsIgnoreCase("HIVTC, Viral Load")) {
				hivViralLoadObs = new Obs();
				hivViralLoadObs = obs;
			} else if(obs.getConcept().getName().toString().equalsIgnoreCase("HTC, Pregnancy Status")) {
				pregnancyStatusObs = new Obs();
				pregnancyStatusObs = obs;
			}
		}
		log.info("DONE DISPLAYING OBS INSIDE THE ENCOUNTER -- TEBOHO KOMA");

		
		// Obs relevant to this encounter
		Collection<Obs> relevantObs = null;
		//if (builder.getEncounterEvent() == Context.getObsService().getObservationsByPerson(builder.getRecordTarget()))
			relevantObs = builder.getEncounterEvent().getAllObs();
		//else
			//relevantObs = Context.getObsService().getObservationsByPerson(builder.getRecordTarget());
		
		for (Obs obs : relevantObs) {
			//we want to have all obs groups at the end of the list
			if (obs.hasGroupMembers()) {
				medicationObs.add(obs);
			} else {
				medicationObs.add(0, obs);	//this probably is some group member
			}
		}
		
		Section eddSection = null, flowsheetSection = null, vitalSignsSection = null, medicationsSection = null, probSection = null, allergySection = null;
		
		if (estimatedDeliveryDateObs != null && lastMenstrualPeriodObs != null)
			eddSection = eddSectionBuilder.generate(estimatedDeliveryDateObs, lastMenstrualPeriodObs);
		
		if (gestgationalAgeObs != null && systolicBpObs != null && diastolicBpObs != null && weightObs != null)
			flowsheetSection = flowsheetSectionBuilder.generate(prepregnancyWeightObs, gestgationalAgeObs, fundalHeightObs,
			    presentationObs, systolicBpObs, diastolicBpObs, weightObs);
		
		if (systolicBpObs != null && diastolicBpObs != null && weightObs != null && heightObs != null
		        && temperatureObs != null)
			vitalSignsSection = vitalSignsSectionBuilder.generate(systolicBpObs, diastolicBpObs, weightObs, heightObs,
			    temperatureObs, baselineCD4Count, artStartRegimenObs, firstHIVPosTestObs, hivViralLoadObs, pregnancyStatusObs);

		//medicationsSection = medSectionBuilder.generate(medicationObs.toArray(new Obs[] {}));


		Location visitLocation = Context.getLocationService().getDefaultLocation();;

		if(encounter.getVisit() != null)
			visitLocation = encounter.getVisit().getLocation();

		// Formatter
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			ClinicalDocument doc = builder.generate(visitLocation, eddSection, flowsheetSection, vitalSignsSection, medicationsSection, probSection,
			    allergySection);

			XmlIts1Formatter formatter = EverestUtil.createFormatter();
			formatter.graph(baos, doc);

			return DocumentModel.createInstance(baos.toByteArray(), builder.getTypeCode(),
					XdsSenderConstants.CODE_SYSTEM_LOINC, builder.getFormatCode(), doc);
		} catch (Exception e) {
			log.error("Error generating document:", e);
			throw new RuntimeException(e);
		}
	}

	public String getRootConceptName(Obs obs) {
		if(obs.getObsGroup() == null) {
			return obs.getConcept().getName().toString();
		}
		return getRootConceptName(obs.getObsGroup());
	}
}
