package org.openmrs.module.xdssender.api.cda.entry.impl;

import org.apache.commons.lang.NotImplementedException;
import org.marc.everest.datatypes.BL;
import org.marc.everest.datatypes.II;
import org.marc.everest.datatypes.NullFlavor;
import org.marc.everest.datatypes.generic.CD;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalStatement;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Component4;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Organizer;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ActRelationshipHasComponent;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ActStatus;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActClassDocumentEntryOrganizer;
import org.openmrs.BaseOpenmrsData;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.module.xdssender.XdsSenderConstants;
import org.openmrs.module.xdssender.api.cda.CdaMetadataUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Vital signs battery entry builder
 * 
 * @author JustinFyfe
 */
@Component("xdssender.VitalSignsBatteryEntryBuilder")
public class VitalSignsBatteryEntryBuilder extends EntryBuilderImpl {

	@Autowired
	private SimpleObservationEntryBuilder obsBuilder;

	@Autowired
	private CdaMetadataUtil cdaMetadataUtil;

	/**
	 * Generate the clincal statement from an encounter
	 */
	public ClinicalStatement generate(BaseOpenmrsData data) {
		throw new NotImplementedException();
	}
	
	/**
	 * Create the organizer from the discrete obs
	 */
	public ClinicalStatement generate(Obs systolicBpObs, Obs diastolicBpObs, Obs weightObs, Obs heightObs, Obs temperatureObs
			, Obs baselineCD4CountObs, Obs artStartRegimenObs, Obs firstHIVPosTestObs, Obs hivViralLoadObs, Obs pregnancyStatusObs) {
		Encounter batteryEnc = systolicBpObs.getEncounter();
		
		if (heightObs != null && !batteryEnc.getId().equals(heightObs.getEncounter().getId())
		        || !batteryEnc.getId().equals(systolicBpObs.getEncounter().getId()) || diastolicBpObs != null
		        && !batteryEnc.getId().equals(diastolicBpObs.getEncounter().getId()) || temperatureObs != null
		        && !batteryEnc.getId().equals(temperatureObs.getEncounter().getId()) || weightObs != null
		        && !batteryEnc.getId().equals(weightObs.getEncounter().getId()))
			throw new IllegalArgumentException("All arguments for the flowsheet panel must come from the same encounter");
		
		Organizer batteryOrganizer = super.createOrganizer(x_ActClassDocumentEntryOrganizer.BATTERY, Arrays
		        .asList(XdsSenderConstants.ENT_TEMPLATE_VITAL_SIGNS_ORGANIZER,
		            XdsSenderConstants.ENT_TEMPLATE_CCD_VITAL_SIGNS_ORGANIZER), new CD<String>("46680005",
		        XdsSenderConstants.CODE_SYSTEM_SNOMED, "SNOMED CT", null, "Vital Signs", null), new II(getConfiguration()
		        .getEncounterRoot(), batteryEnc.getId().toString()), ActStatus.Completed, batteryEnc.getEncounterDatetime());
		
		//SimpleObservationEntryBuilder obsBuilder = new SimpleObservationEntryBuilder();
		
		batteryOrganizer.getComponent().add(
		    new Component4(ActRelationshipHasComponent.HasComponent, BL.TRUE, obsBuilder.generate(new CD<String>("8480-6",
		            XdsSenderConstants.CODE_SYSTEM_LOINC, XdsSenderConstants.CODE_SYSTEM_NAME_LOINC, null,
		            "Blood pressure - Systolic", null), systolicBpObs)));
		
		if (diastolicBpObs != null)
			batteryOrganizer.getComponent().add(
			    new Component4(ActRelationshipHasComponent.HasComponent, BL.TRUE, obsBuilder.generate(new CD<String>(
			            "8462-4", XdsSenderConstants.CODE_SYSTEM_LOINC, XdsSenderConstants.CODE_SYSTEM_NAME_LOINC, null,
			            "Blood pressure - Diastolic", null), diastolicBpObs)));
		if (weightObs != null)
			batteryOrganizer.getComponent().add(
			    new Component4(ActRelationshipHasComponent.HasComponent, BL.TRUE, obsBuilder.generate(new CD<String>(
			            "3141-9", XdsSenderConstants.CODE_SYSTEM_LOINC, XdsSenderConstants.CODE_SYSTEM_NAME_LOINC, null,
			            "Body weight measured", null), weightObs)));
		if (heightObs != null)
			batteryOrganizer.getComponent().add(
			    new Component4(ActRelationshipHasComponent.HasComponent, BL.TRUE, obsBuilder.generate(new CD<String>(
			            "8302-2", XdsSenderConstants.CODE_SYSTEM_LOINC, XdsSenderConstants.CODE_SYSTEM_NAME_LOINC, null,
			            "Body height measured", null), heightObs)));
		if (temperatureObs != null)
			batteryOrganizer.getComponent().add(
			    new Component4(ActRelationshipHasComponent.HasComponent, BL.TRUE, obsBuilder.generate(new CD<String>(
			            "8310-5", XdsSenderConstants.CODE_SYSTEM_LOINC, XdsSenderConstants.CODE_SYSTEM_NAME_LOINC, null,
			            "Body Temperature", null), temperatureObs)));


		// FOR FINAL HIV STATUS
		// (Positive or Negative) - has an observation date attached
		if (firstHIVPosTestObs != null){
			batteryOrganizer.getComponent().add(
					new Component4(ActRelationshipHasComponent.HasComponent, BL.TRUE, obsBuilder.generate(new CD<String>(
							"165816005", XdsSenderConstants.CODE_SYSTEM_SNOMED, "SNOMED CT", null,
							"HIV INFECTED", null), firstHIVPosTestObs)));
		}
		// Determine whether its 1st test or retesting


		// Linkage to care


		// DURING ART INITIATION
		// 1. CD4 Count
		if (baselineCD4CountObs != null){
			batteryOrganizer.getComponent().add(
					new Component4(ActRelationshipHasComponent.HasComponent, BL.TRUE, obsBuilder.generate(new CD<String>(
							"24467-3", XdsSenderConstants.CODE_SYSTEM_LOINC, XdsSenderConstants.CODE_SYSTEM_NAME_LOINC, null,
							"CD4 COUNT", null), baselineCD4CountObs)));
		}

		// 2. CD4 Percentage


		// 2. ARV Regimen given during ART Start
		// 3. ART Start Date

		// FOR LATEST FOLLOW UP ENCOUNTERS
		// 1. Viral Load Test Result
		if (hivViralLoadObs != null){
			batteryOrganizer.getComponent().add(
					new Component4(ActRelationshipHasComponent.HasComponent, BL.TRUE, obsBuilder.generate(new CD<String>(
							"315124004", XdsSenderConstants.CODE_SYSTEM_SNOMED, "SNOMED CT", null,
							"HIV VIRAL LOAD", null), hivViralLoadObs)));
		}
		// 2. CD4 count
		// 3. T Staging
		// 4. ART Regimen given
		if (artStartRegimenObs != null){
			/*CD<String> artRegimenCD = this.cdaMetadataUtil.getStandardizedCode(artStartRegimenObs.getConcept(),
					XdsSenderConstants.CODE_SYSTEM_SNOMED, CD.class);

			batteryOrganizer.getComponent().add(
					new Component4(ActRelationshipHasComponent.HasComponent, BL.TRUE, obsBuilder.generate(
							artRegimenCD, artStartRegimenObs)));*/

			batteryOrganizer.getComponent().add(
					new Component4(ActRelationshipHasComponent.HasComponent, BL.TRUE, obsBuilder.generate(new CD<String>(
							"999999900", XdsSenderConstants.CODE_SYSTEM_SNOMED, "SNOMED CT", null,
							"Human immunodeficiency virus treatment regimen", null), artStartRegimenObs)));
		}

		// 5. Clients Pregnancy Status
		if (pregnancyStatusObs != null){
			batteryOrganizer.getComponent().add(
					new Component4(ActRelationshipHasComponent.HasComponent, BL.TRUE, obsBuilder.generate(new CD<String>(
							"11449-6", XdsSenderConstants.CODE_SYSTEM_LOINC, XdsSenderConstants.CODE_SYSTEM_NAME_LOINC, null,
							"PREGNANCY STATUS", null), pregnancyStatusObs)));
		}

		// 5. TPT/IPT

		return batteryOrganizer;
	}
	
}
