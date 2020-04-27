package org.worldbuild.mongo.domain.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Document(collection = "db_repl")
@TypeAlias("repl")
public class Replication {

    @Id
    private String id;
    private String primary;
    private String setName;
    private Integer setVersion;
    private List<String> hosts;
    private List<String> arbiters;
    private boolean ismaster;
    private boolean secondary;
    private String electionId;
    private Date lastWrite;
    private Integer rbid;
    @CreatedDate
    private Date add;
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPrimary() {
        return primary;
    }

    public void setPrimary(String primary) {
        this.primary = primary;
    }

    public String getSetName() {
        return setName;
    }

    public void setSetName(String setName) {
        this.setName = setName;
    }

    public Integer getSetVersion() {
        return setVersion;
    }

    public void setSetVersion(Integer setVersion) {
        this.setVersion = setVersion;
    }

    public List<String> getHosts() {
        return hosts;
    }

    public void setHosts(List<String> hosts) {
        this.hosts = hosts;
    }

    public List<String> getArbiters() {
        return arbiters;
    }

    public void setArbiters(List<String> arbiters) {
        this.arbiters = arbiters;
    }

    public boolean isIsmaster() {
        return ismaster;
    }

    public void setIsmaster(boolean ismaster) {
        this.ismaster = ismaster;
    }

    public boolean isSecondary() {
        return secondary;
    }

    public void setSecondary(boolean secondary) {
        this.secondary = secondary;
    }

    public String getElectionId() {
        return electionId;
    }

    public void setElectionId(String electionId) {
        this.electionId = electionId;
    }

    public Date getLastWrite() {
        return lastWrite;
    }

    public void setLastWrite(Date lastWrite) {
        this.lastWrite = lastWrite;
    }

    public Integer getRbid() {
        return rbid;
    }

    public void setRbid(Integer rbid) {
        this.rbid = rbid;
    }

    public Date getAdd() {
        return add;
    }

    public void setAdd(Date add) {
        this.add = add;
    }
}
