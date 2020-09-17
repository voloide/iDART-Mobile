package mz.org.fgh.idartlite.model;

import android.os.Build;
import androidx.annotation.RequiresApi;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import mz.org.fgh.idartlite.base.BaseModel;
import mz.org.fgh.idartlite.dao.EpisodeDaoImpl;

import java.util.Objects;

@DatabaseTable(tableName = "episode", daoClass = EpisodeDaoImpl.class)
public class Episode extends BaseModel {

	public static final String COLUMN_ID = "id";
	public static final String COLUMN_EPISODE_DATE = "episode_date";
	public static final String COLUMN_START_REASON = "start_reason";
	public static final String COLUMN_STOP_REASON = "stop_reason";
	public static final String COLUMN_NOTES = "notes";
	public static final String COLUMN_UUID = "uuid";
	public static final String COLUMN_PATIENT_ID = "patient_id";

	@DatabaseField(columnName = COLUMN_ID, generatedId = true)
	private int id;

	@DatabaseField(columnName = COLUMN_EPISODE_DATE)
	private int episodeDate;

	@DatabaseField(columnName = COLUMN_START_REASON)
	private String startReason;

	@DatabaseField(columnName = COLUMN_STOP_REASON)
	private String stopReason;

	@DatabaseField(columnName = COLUMN_NOTES)
	private String notes;

	@DatabaseField(columnName = COLUMN_UUID)
	private String uuid;

	@DatabaseField(columnName = COLUMN_PATIENT_ID, canBeNull = false, foreign = true)
	private Patient patientId;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getEpisodeDate() {
		return episodeDate;
	}

	public void setEpisodeDate(int episodeDate) {
		this.episodeDate = episodeDate;
	}

	public String getStartReason() {
		return startReason;
	}

	public void setStartReason(String startReason) {
		this.startReason = startReason;
	}

	public String getStopReason() {
		return stopReason;
	}

	public void setStopReason(String stopReason) {
		this.stopReason = stopReason;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public Patient getPatientId() {
		return patientId;
	}

	public void setPatientId(Patient patientId) {
		this.patientId = patientId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Episode episode = (Episode) o;
		return uuid.equals(episode.uuid) &&	patientId.equals(episode.patientId);
	}

	@RequiresApi(api = Build.VERSION_CODES.KITKAT)
	@Override
	public int hashCode() {
		return Objects.hash(uuid, patientId);
	}

	@Override
	public String toString() {
		return "Episode{" +
				"episodeDate=" + episodeDate +
				", startReason='" + startReason + '\'' +
				", stopReason='" + stopReason + '\'' +
				", notes='" + notes + '\'' +
				", uuid='" + uuid + '\'' +
				'}';
	}
}
