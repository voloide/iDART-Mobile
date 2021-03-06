package mz.org.fgh.idartlite.service.restService;

import android.app.Application;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import mz.org.fgh.idartlite.base.BaseModel;
import mz.org.fgh.idartlite.base.BaseService;
import mz.org.fgh.idartlite.model.Clinic;
import mz.org.fgh.idartlite.model.Dispense;
import mz.org.fgh.idartlite.model.DispensedDrug;
import mz.org.fgh.idartlite.model.Drug;
import mz.org.fgh.idartlite.model.Episode;
import mz.org.fgh.idartlite.model.Patient;
import mz.org.fgh.idartlite.model.PrescribedDrug;
import mz.org.fgh.idartlite.model.Prescription;
import mz.org.fgh.idartlite.model.Stock;
import mz.org.fgh.idartlite.model.SyncDispense;
import mz.org.fgh.idartlite.model.User;
import mz.org.fgh.idartlite.rest.RESTServiceHandler;
import mz.org.fgh.idartlite.service.ClinicService;
import mz.org.fgh.idartlite.service.DispenseDrugService;
import mz.org.fgh.idartlite.service.DispenseService;
import mz.org.fgh.idartlite.service.DispenseTypeService;
import mz.org.fgh.idartlite.service.DrugService;
import mz.org.fgh.idartlite.service.EpisodeService;
import mz.org.fgh.idartlite.service.PrescribedDrugService;
import mz.org.fgh.idartlite.service.PrescriptionService;
import mz.org.fgh.idartlite.service.StockService;
import mz.org.fgh.idartlite.service.TherapeuthicLineService;
import mz.org.fgh.idartlite.service.TherapheuticRegimenService;
import mz.org.fgh.idartlite.util.DateUtilitis;
import mz.org.fgh.idartlite.util.Utilities;

public class RestDispenseService extends BaseService {

    private static final String TAG = "RestDispenseService";
    private static DispenseService dispenseService;
    private static DispenseDrugService dispenseDrugService;
    private static EpisodeService episodeService;
    private static PrescriptionService prescriptionService;
    private static List<DispensedDrug> dispensedDrugList;
    private static List<Episode> episodeList;

    public RestDispenseService(Application application, User currentUser) {
        super(application, currentUser);
        dispenseService = new DispenseService(application, currentUser);
        prescriptionService = new PrescriptionService(application, currentUser);
    }

    public static void restGetLastDispense(Patient patient) throws SQLException {

        dispenseService = new DispenseService(getApp(), null);
        dispenseDrugService = new DispenseDrugService(getApp(), null);
        prescriptionService = new PrescriptionService(getApp(), null);
        episodeService = new EpisodeService(getApp(), null);

        episodeList = episodeService.getAllEpisodesByPatient(patient);

        Episode episode = episodeList.get(episodeList.size() - 1);

        if (episode != null) {

            String url = BaseService.baseUrl + "/sync_temp_dispense?uuidopenmrs=eq." + patient.getUuid() + "&mainclinicuuid=eq." + episode.getUsUuid() + "&order=pickupdate.desc";

            try {
                getRestServiceExecutor().execute(() -> {

                    RESTServiceHandler handler = new RESTServiceHandler();

                    try {
                        handler.addHeader("Content-Type", "application/json");
                        handler.objectRequest(url, Request.Method.GET, null, Object[].class, new Response.Listener<Object[]>() {

                            @Override
                            public void onResponse(Object[] dispenses) {
                                try {
                                    if (dispenses.length > 0) {
                                        for (Object dispense : dispenses) {
                                            Log.d(TAG, "onResponse: Dispensa " + dispense);

                                            Prescription newPrescription = getPrescroptionRest(dispense, patient);
                                            Prescription lastPrescription = prescriptionService.getLastPatientPrescription(patient);

                                            if (DateUtilitis.dateDiff(newPrescription.getPrescriptionDate(), lastPrescription.getPrescriptionDate(), DateUtilitis.DAY_FORMAT) > 0) {
                                                prescriptionService.createPrescription(newPrescription);
                                                savePrescribedDrugOnRest(dispense, newPrescription);
                                                lastPrescription.setExpiryDate(newPrescription.getPrescriptionDate());
                                                prescriptionService.updatePrescriptionEntity(lastPrescription);
                                            } else if (DateUtilitis.dateDiff(newPrescription.getPrescriptionDate(), lastPrescription.getPrescriptionDate(), DateUtilitis.DAY_FORMAT) == 0) {
                                                newPrescription = lastPrescription;
                                            } else {
                                                break;
                                            }

                                            Dispense newDispense = getDispenseOnRest(dispense, newPrescription);
                                            Dispense lastDispense = dispenseService.getLastDispenseFromPrescription(newPrescription);

                                            if (lastDispense != null) {
                                                if (DateUtilitis.dateDiff(newDispense.getPickupDate(), lastDispense.getPickupDate(), DateUtilitis.DAY_FORMAT) > 0) {
                                                    dispenseService.createDispense(newDispense);
                                                    saveDispensedOnRest(dispense, newDispense);
                                                } else {
                                                    break;
                                                }
                                            } else {
                                                dispenseService.createDispense(newDispense);
                                                saveDispensedOnRest(dispense, newDispense);
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.d(TAG, "onErrorResponse: Erro no GET :" + generateErrorMsg(error));
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void restPostDispense(Dispense dispense) {

        String url = BaseService.baseUrl + "/sync_temp_dispense";

        dispenseService = new DispenseService(getApp(), null);
        dispenseDrugService = new DispenseDrugService(getApp(), null);

        try {
            getRestServiceExecutor().execute(() -> {

                RESTServiceHandler handler = new RESTServiceHandler();

                try {
                    dispensedDrugList = dispenseDrugService.findDispensedDrugByDispenseId(dispense.getId());

                    for (DispensedDrug dispensedDrug : dispensedDrugList) {

                        SyncDispense syncDispense = setSyncDispense(dispense, dispensedDrug);

                        Gson g = new Gson();
                        String restObject = g.toJson(syncDispense);

                        handler.addHeader("Content-Type", "application/json");
                        JSONObject jsonObject = new JSONObject(restObject);

                        handler.objectRequest(url, Request.Method.POST, jsonObject, Object.class, new Response.Listener<Object>() {

                            @Override
                            public void onResponse(Object response) {
                                Log.d(TAG, "onResponse: Dispensa enviada" + response);
                                try {
                                    dispense.setSyncStatus(BaseModel.SYNC_SATUS_SENT);
                                    dispenseService.udpateDispense(dispense);
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                            }
                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.d(TAG, "onErrorResponse: Erro no POST :" + generateErrorMsg(error));
                            }
                        });
                    }
                } catch (SQLException | JSONException e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Prescription getPrescroptionRest(Object dispense, Patient patient) throws SQLException {

        LinkedTreeMap<String, Object> itemresult = (LinkedTreeMap<String, Object>) (Object) dispense;
        DispenseTypeService dispenseTypeService = new DispenseTypeService(getApp(), null);
        TherapheuticRegimenService therapheuticRegimenService = new TherapheuticRegimenService(getApp(), null);
        TherapeuthicLineService therapeuthicLineService = new TherapeuthicLineService(getApp(), null);
        Prescription prescription = new Prescription();

        if (itemresult.get("dispensatrimestral").toString().contains("1"))
            prescription.setDispenseType(dispenseTypeService.getDispenseType("Dispensa Trimestral (DT)"));
        else if (itemresult.get("dispensasemestral").toString().contains("1"))
            prescription.setDispenseType(dispenseTypeService.getDispenseType("Dispensa Semestral (DS)"));
        else
            prescription.setDispenseType(dispenseTypeService.getDispenseType("Dispensa Mensal (DM)"));
        prescription.setUuid(UUID.randomUUID().toString());
        if (itemresult.get("prescricaoespecial").toString().contains("F") || itemresult.get("prescricaoespecial").toString().contains("f")) {
            prescription.setUrgentPrescription(Prescription.URGENT_PRESCRIPTION);
            prescription.setUrgentNotes(itemresult.get("motivocriacaoespecial").toString());
        } else {
            prescription.setUrgentPrescription(Prescription.NOT_URGENT_PRESCRIPTION);
            prescription.setUrgentNotes("");
        }
        prescription.setTherapeuticRegimen(therapheuticRegimenService.getTherapeuticRegimenFromDescription(itemresult.get("regimenome").toString()));
        prescription.setTherapeuticLine(therapeuthicLineService.getTherapeuticLine(itemresult.get("linhanome").toString()));
        prescription.setSyncStatus(BaseModel.SYNC_SATUS_SENT);
        prescription.setSupply((int) Float.parseFloat(itemresult.get("duration").toString()));
        prescription.setPrescriptionSeq(itemresult.get("prescriptionid").toString());
        prescription.setPatient(patient);
        if (itemresult.get("expirydate") != null)
            prescription.setExpiryDate(DateUtilitis.createDate(itemresult.get("expirydate").toString(), "yyyy-MM-dd"));
        prescription.setPrescriptionDate(DateUtilitis.createDate(itemresult.get("date").toString(), "yyyy-MM-dd"));

        return prescription;

    }

    private static Dispense getDispenseOnRest(Object dispense, Prescription prescription) {
        LinkedTreeMap<String, Object> itemresult = (LinkedTreeMap<String, Object>) (Object) dispense;
        Dispense localDispense = new Dispense();

        localDispense.setSyncStatus(BaseModel.SYNC_SATUS_SENT);
        localDispense.setNextPickupDate(BaseService.getUtilDateFromString(itemresult.get("dateexpectedstring").toString(), "dd MMM yyyy"));
        localDispense.setPickupDate(DateUtilitis.createDate(itemresult.get("pickupdate").toString(), "yyyy-MM-dd"));
        localDispense.setSupply((int) Float.parseFloat(itemresult.get("weekssupply").toString()));
        localDispense.setPrescription(prescription);
        localDispense.setUuid(UUID.randomUUID().toString());

        return localDispense;
    }

    private static void saveDispensedOnRest(Object dispense, Dispense dispensed) throws SQLException {
        LinkedTreeMap<String, Object> itemresult = (LinkedTreeMap<String, Object>) (Object) dispense;
        DrugService drugService = new DrugService(getApp(), null);
        DispenseDrugService dispenseDrugService = new DispenseDrugService(getApp(), null);
        StockService stockService = new StockService(getApp(), null);
        ClinicService clinicService = new ClinicService(getApp(), null);
        DispensedDrug dispensedDrug = new DispensedDrug();

        Clinic clinic = clinicService.getCLinic().get(0);
        Drug localDrug = drugService.getDrugFromDescription(itemresult.get("drugname").toString());
        String inHand = itemresult.get("qtyinhand").toString();
        if (!inHand.isEmpty())
            inHand = inHand.replace('(', ' ').replace(')', ' ');
        else
            inHand = "0";

        if (localDrug != null) {

            List<Stock> stockList = stockService.getAllStocksByClinicAndDrug(clinic, localDrug);

            dispensedDrug.setDispense(dispensed);
            dispensedDrug.setQuantitySupplied(Integer.parseInt(inHand.trim()));
            if (!stockList.isEmpty())
                dispensedDrug.setStock(stockList.get(0));
            dispensedDrug.setSyncStatus(BaseModel.SYNC_SATUS_SENT);
            dispenseDrugService.createDispensedDrug(dispensedDrug);
        }

    }

    private static void savePrescribedDrugOnRest(Object dispense, Prescription prescription) throws SQLException {
        LinkedTreeMap<String, Object> itemresult = (LinkedTreeMap<String, Object>) (Object) dispense;
        DrugService drugService = new DrugService(getApp(), null);
        PrescribedDrugService prescribedDrugService = new PrescribedDrugService(getApp(), null);
        PrescribedDrug prescribedDrug = new PrescribedDrug();

        Drug localDrug = drugService.getDrugFromDescription(itemresult.get("drugname").toString());

        if (localDrug != null) {
            prescribedDrug.setPrescription(prescription);
            prescribedDrug.setDrug(localDrug);
            prescribedDrugService.createPrescribedDrug(prescribedDrug);
        }
    }

    private static SyncDispense setSyncDispense(Dispense dispense, DispensedDrug dispensedDrug) {

        episodeService = new EpisodeService(getApp(), null);
        SyncDispense syncDispense = new SyncDispense();
        try {
            Episode episode = episodeService.getAllEpisodesByPatient(dispense.getPrescription().getPatient()).get(0);
            syncDispense.setDate(dispense.getPrescription().getPrescriptionDate());
            syncDispense.setClinicalstage(0);
            syncDispense.setCurrent('T');
            syncDispense.setReasonforupdate("Manter");
            syncDispense.setDuration(dispense.getPrescription().getSupply());
            syncDispense.setDoctor(1);
            syncDispense.setEnddate(null);
            syncDispense.setRegimenome(dispense.getPrescription().getTherapeuticRegimen().getDescription());
            syncDispense.setLinhanome(dispense.getPrescription().getTherapeuticLine().getDescription());
            syncDispense.setNotes("Mobile Pharmacy");
            syncDispense.setMotivomudanca("");
            syncDispense.setWeight(null);
            syncDispense.setAf(Character.toUpperCase('f'));
            syncDispense.setCa(Character.toUpperCase('f'));
            syncDispense.setCcr(Character.toUpperCase('f'));
            syncDispense.setPpe(Character.toUpperCase('f'));
            syncDispense.setPtv(Character.toUpperCase('f'));
            syncDispense.setTb(Character.toUpperCase('f'));
            syncDispense.setFr(Character.toUpperCase('f'));
            syncDispense.setGaac(Character.toUpperCase('f'));
            syncDispense.setSaaj(Character.toUpperCase('f'));
            syncDispense.setTpc(Character.toUpperCase('f'));
            syncDispense.setTpi(Character.toUpperCase('f'));
            syncDispense.setPatient(dispense.getPrescription().getPatient().getId());

            if (dispense.getPrescription().getDispenseType().getDescription().contains("DT")) {
                syncDispense.setDispensatrimestral(1);
                syncDispense.setTipodt("Manuntecao");
            } else {
                syncDispense.setDispensatrimestral(0);
                syncDispense.setTipodt(null);
            }
            if (dispense.getPrescription().getDispenseType().getDescription().contains("DS")) {
                syncDispense.setDispensasemestral(1);
                syncDispense.setTipods("Manuntecao");
            } else {
                syncDispense.setDispensasemestral(0);
                syncDispense.setTipods(null);
            }

            syncDispense.setDrugtypes("ARV");
            syncDispense.setDatainicionoutroservico(null);
            syncDispense.setModified('T');
            syncDispense.setPatientid(dispense.getPrescription().getPatient().getNid());
            syncDispense.setPatientfirstname(dispense.getPrescription().getPatient().getFirstName());
            syncDispense.setPatientlastname(dispense.getPrescription().getPatient().getLastName());
            syncDispense.setPickupdate(dispense.getPickupDate());
            syncDispense.setQtyinhand("(" + dispensedDrug.getQuantitySupplied() + ")");
            syncDispense.setQtyinlastbatch("(" + dispensedDrug.getQuantitySupplied() + ")");
            syncDispense.setSummaryqtyinhand("(" + dispensedDrug.getQuantitySupplied() + ")");
            syncDispense.setTimesperday(1);
            syncDispense.setWeekssupply(dispense.getPrescription().getSupply());
            syncDispense.setExpirydate(dispense.getNextPickupDate());

            syncDispense.setDateexpectedstring(BaseService.getStringDateFromDate(dispense.getNextPickupDate(), "dd MMM yyyy"));
            syncDispense.setDrugname(dispensedDrug.getStock().getDrug().getDescription());
            syncDispense.setDispensedate(dispense.getPickupDate());

            syncDispense.setMainclinic(0);
            syncDispense.setMainclinicname(episode.getSanitaryUnit());
            syncDispense.setMainclinicuuid(episode.getUsUuid());

            syncDispense.setSyncstatus('P');
            syncDispense.setPrescriptionid(String.valueOf(dispense.getPrescription().getPrescriptionSeq()));

            syncDispense.setDurationsentence("");
            syncDispense.setDc(Character.toUpperCase('T'));
            syncDispense.setPrep(Character.toUpperCase('f'));
            syncDispense.setCe(Character.toUpperCase('f'));
            syncDispense.setCpn(Character.toUpperCase('f'));
            if (!dispense.getPrescription().getUrgentPrescription().equalsIgnoreCase(Prescription.URGENT_PRESCRIPTION)) {
                syncDispense.setPrescricaoespecial(Character.toUpperCase('f'));
                syncDispense.setMotivocriacaoespecial("");
            } else {
                syncDispense.setPrescricaoespecial(Character.toUpperCase('T'));
                syncDispense.setMotivocriacaoespecial(dispense.getPrescription().getUrgentNotes());
            }

            syncDispense.setUuidopenmrs(dispense.getPrescription().getPatient().getUuid());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return syncDispense;

    }
}