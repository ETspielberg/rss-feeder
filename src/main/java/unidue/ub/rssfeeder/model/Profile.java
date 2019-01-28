package unidue.ub.rssfeeder.model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public abstract class Profile implements Serializable {

    private String identifier;

    private Status status = Status.CREATED;

    private Timestamp lastrun;

    private Timestamp created = Timestamp.valueOf(LocalDateTime.now());

    public Timestamp getCreated() {
        return created;
    }

    public void setCreated(Timestamp created) {
        this.created = created;
    }

    public Timestamp getLastrun() {
        return lastrun;
    }

    public void setLastrun(Timestamp lastrun) {
        this.lastrun = lastrun;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
