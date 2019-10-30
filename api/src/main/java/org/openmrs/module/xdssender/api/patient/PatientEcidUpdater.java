package org.openmrs.module.xdssender.api.patient;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;

@Component
@Transactional
public class PatientEcidUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(PatientEcidUpdater.class);

    private static final String ECID_UUID = "a5d38e09-efcb-4d91-a526-50ce1ba5011a";
    private static final String CODE_NATIONAL_UUID = "81433852-3f10-11e4-adec-0800271c1b75";
    private static final String FETCHER_BEAN_ID = "registrationcore.mpiPatientFetcherPdq";
    private static final String FETCH_PATIENT_METHOD = "fetchMpiPatient";

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private PatientService patientService;

    @Autowired
    private LocationService locationService;

    public void fetchEcidIfRequired(Patient patient) {
        if (StringUtils.isBlank(getEcid(patient))) {
            LOGGER.info("Fetching ECID for patient {}", patient.getPatientId());
            updateEcid(patient);
        } else {
            LOGGER.debug("Patient {} already has an ECID", patient.getPatientId());
        }
    }

    private void updateEcid(Patient patient) {
        String codeNational = getCodeNational(patient);
        Patient mpiPatient = fetchMpiPatient(codeNational);

        if (mpiPatient != null) {
            String ecid = getEcid(mpiPatient);

            PatientIdentifierType ecidIdType = patientService
                    .getPatientIdentifierTypeByUuid(ECID_UUID);

            patient.addIdentifier(new PatientIdentifier(ecid, ecidIdType,
                    locationService.getDefaultLocation()));

            patientService.savePatient(patient);
        }
    }

    private String getCodeNational(Patient patient) {
        return getIdentifier(patient, CODE_NATIONAL_UUID);
    }

    private String getEcid(Patient patient) {
        return getIdentifier(patient, ECID_UUID);
    }

    private String getIdentifier(Patient patient, String uuid) {
        for (PatientIdentifier identifier : patient.getIdentifiers()) {
            if (uuid.equals(identifier.getIdentifierType().getUuid())) {
                return identifier.getIdentifier();
            }
        }
        return null;
    }

    private Patient fetchMpiPatient(String codeNational) {
        // Using reflection as a quick hack to get around module dependencies
        // No time to do anything else currently

        Object fetcher = applicationContext.getBean(FETCHER_BEAN_ID);

        try {
            return (Patient) MethodUtils.invokeMethod(fetcher, FETCH_PATIENT_METHOD,
                    codeNational, CODE_NATIONAL_UUID);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Unable to invoke PDQ fetcher", e);
        }
    }
}
