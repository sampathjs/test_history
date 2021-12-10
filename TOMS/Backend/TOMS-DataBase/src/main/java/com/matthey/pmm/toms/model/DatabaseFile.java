package com.matthey.pmm.toms.model;

import java.util.Arrays;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;

/**
 * Entity representing a file saved on the database. It has a path, a name and a potentially large content.
 * 
 * @author jwaechter
 * @version 1.0
 */
@Entity
@Table(schema = DbConstants.SCHEMA_NAME, name = "database_file", 
    indexes = { @Index(name = "i_database_file_id", columnList = "database_file_id", unique = true),
        @Index(name = "i_database_file_file_type_reference", columnList = "file_type_reference_id", unique = false),
        @Index(name = "i_database_file_path_name", columnList = "path,name", unique = true)})
public class DatabaseFile {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "database_file_id_seq")
	@SequenceGenerator(name = "database_file_id_seq", initialValue = 10000, allocationSize = 1,
	    sequenceName = "database_file_id_seq" , schema = DbConstants.SCHEMA_NAME)
	@Column(name = "database_file_id", updatable = false, nullable = false)
	private Long id; 
	
	@Column(name="name", nullable=false)
	private String name;

	@Column(name="path", nullable=false)
	private String path;
	
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="file_type_reference_id")
	@ReferenceTypeDesignator(referenceTypes=DefaultReferenceType.FILE_TYPE)
	private Reference fileType;
	
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="reference_lifecycle_status_id")
	@ReferenceTypeDesignator(referenceTypes = DefaultReferenceType.LIFECYCLE_STATUS)
	private Reference lifecycleStatus;
	
	@Lob
	@Column(name="file_content", nullable = false)
	private byte[] fileContent;
		
	@Column(name = "created_at")
	@Temporal(TemporalType.TIMESTAMP)
	private Date createdAt;
	
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="created_by", nullable = false)
	private User createdByUser;
	
	@Column(name = "last_update")
	@Temporal(TemporalType.TIMESTAMP)
	private Date lastUpdate;

	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="updated_by", nullable = false)
	private User updatedByUser;
	
	/**
	 * For JPA purposes only. Do not use.
	 */
	protected DatabaseFile() {
	}

	public DatabaseFile(final String name, final String path, final Reference fileType, final Reference lifecycleStatus, 
			final byte[] fileContent,
			final Date createdAt, final User createdByUser, final Date lastUpdate, final User updatedByUser) {
		this.name = name;
		this.path = path;
		this.fileType = fileType;
		this.lifecycleStatus = lifecycleStatus;
		this.fileContent = fileContent;
		this.createdAt = createdAt;
		this.createdByUser = createdByUser;
		this.lastUpdate = lastUpdate;
		this.updatedByUser = updatedByUser;		
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Reference getFileType() {
		return fileType;
	}

	public void setFileType(Reference fileType) {
		this.fileType = fileType;
	}

	public Reference getLifecycleStatus() {
		return lifecycleStatus;
	}

	public void setLifecycleStatus(Reference lifecycleStatus) {
		this.lifecycleStatus = lifecycleStatus;
	}

	public byte[] getFileContent() {
		return fileContent;
	}

	public void setFileContent(byte[] fileContent) {
		this.fileContent = fileContent;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public User getCreatedByUser() {
		return createdByUser;
	}

	public void setCreatedByUser(User createdByUser) {
		this.createdByUser = createdByUser;
	}

	public Date getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(Date lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	public User getUpdatedByUser() {
		return updatedByUser;
	}

	public void setUpdatedByUser(User updatedByUser) {
		this.updatedByUser = updatedByUser;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DatabaseFile other = (DatabaseFile) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DatabaseFile [id=" + id + ", name=" + name + ", path=" + path + ", fileType=" + fileType
				+ ", lifecycleStatus=" + lifecycleStatus
				+ ", fileContent=" + Arrays.toString(fileContent) + "]";
	}
}
