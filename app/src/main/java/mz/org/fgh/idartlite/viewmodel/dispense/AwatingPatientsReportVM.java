package mz.org.fgh.idartlite.viewmodel.dispense;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.databinding.Bindable;

import com.itextpdf.text.DocumentException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import mz.org.fgh.idartlite.BR;
import mz.org.fgh.idartlite.R;
import mz.org.fgh.idartlite.base.model.BaseModel;
import mz.org.fgh.idartlite.base.service.IBaseService;
import mz.org.fgh.idartlite.base.viewModel.SearchVM;
import mz.org.fgh.idartlite.model.Clinic;
import mz.org.fgh.idartlite.model.Dispense;
import mz.org.fgh.idartlite.model.Patient;
import mz.org.fgh.idartlite.service.dispense.DispenseService;
import mz.org.fgh.idartlite.service.dispense.IDispenseService;
import mz.org.fgh.idartlite.util.DateUtilities;
import mz.org.fgh.idartlite.util.Utilities;
import mz.org.fgh.idartlite.view.reports.DispenseReportActivity;
import mz.org.fgh.idartlite.view.reports.DispensedDrugsReportActivity;
import mz.org.fgh.idartlite.view.reports.PatientsAwaitingReportActivity;

public class AwatingPatientsReportVM extends SearchVM<Dispense> {


    private IDispenseService dispenseService;

    private String startDate;

    private String endDate;


    public AwatingPatientsReportVM(@NonNull Application application) {
        super(application);
        dispenseService = new DispenseService(application, getCurrentUser());


    }

    @Override
    protected IBaseService initRelatedService() {
        return null;
    }

    @Override
    protected BaseModel initRecord() {
        return null;
    }

    @Override
    protected void initFormData() {

    }

    public List<Dispense> getDispensesByDates(Date startDate,Date endDate, long offset, long limit) throws SQLException {
        return dispenseService.getDispensesBetweenNextPickupDateStartDateAndEndDateWithLimit(startDate, endDate,offset,limit);
    }



    @Override
    public void initSearch(){
        if(!Utilities.stringHasValue(startDate) || !Utilities.stringHasValue(endDate)) {
            Utilities.displayAlertDialog(getRelatedActivity(),getRelatedActivity().getString(R.string.start_end_date_is_mandatory)).show();
        }
       else if (DateUtilities.dateDiff(DateUtilities.createDate(endDate, DateUtilities.DATE_FORMAT), DateUtilities.createDate(startDate, DateUtilities.DATE_FORMAT), DateUtilities.DAY_FORMAT) < 0){
            Utilities.displayAlertDialog(getRelatedActivity(), "A data inicio deve ser menor que a data fim.").show();
        }else
        if ((int) (DateUtilities.dateDiff(DateUtilities.getCurrentDate(), DateUtilities.createDate(startDate, DateUtilities.DATE_FORMAT), DateUtilities.DAY_FORMAT)) > 0){
            Utilities.displayAlertDialog(getRelatedActivity(), "A data inicio deve ser maior ou igual que a data corrente.").show();
        }
        else {

            try {
                super.initSearch();
                if(getAllDisplyedRecords().size()>0){
                    getRelatedActivity().generatePdfButton(true);
                }
                else {
                    getRelatedActivity().generatePdfButton(false);
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }

        }
    }

    public void generatePDF() {
        try {
            this.getRelatedActivity().createPdfDocument();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doOnNoRecordFound() {

    }


    public List<Dispense> doSearch(long offset, long limit) throws SQLException {

        return getDispensesByDates(DateUtilities.createDate(startDate, DateUtilities.DATE_FORMAT), DateUtilities.createDate(endDate, DateUtilities.DATE_FORMAT),offset,limit);
    }



    @Override
    public void displaySearchResults() {
        Utilities.hideSoftKeyboard(getRelatedActivity());

        getRelatedActivity().displaySearchResult();
    }




    public PatientsAwaitingReportActivity getRelatedActivity() {
        return (PatientsAwaitingReportActivity) super.getRelatedActivity();
    }

    @Override
    public void preInit() {

    }

    @Bindable
    public Clinic getClinic(){
        return getCurrentClinic();
    }

    @Bindable
    public String getSearchParam() {
        return startDate;
    }

    public void setSearchParam(String searchParam) {
        this.startDate = searchParam;
        notifyPropertyChanged(BR.searchParam);
    }

    @Bindable
    public String getSearchParam2() {
        return endDate;
    }

    public void setSearchParam2(String searchParam) {
        this.endDate = searchParam;
        notifyPropertyChanged(BR.searchParam);
    }

}
