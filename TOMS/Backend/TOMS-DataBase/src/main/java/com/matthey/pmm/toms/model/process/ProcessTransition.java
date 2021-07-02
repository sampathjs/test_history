package com.matthey.pmm.toms.model.process;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.matthey.pmm.toms.enums.DefaultReferenceType;
import com.matthey.pmm.toms.model.DbConstants;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.ReferenceTypeDesignator;

/**
 * Class representing a transition of a certain status to the next status is allowed
 * @author jwaechter
 * @version 1.0
 */

@Entity
@Table(name = "process_transition", schema = DbConstants.SCHEMA_NAME,
	indexes = { @Index(name = "i_process_transition", columnList = "id", unique = true) })
public class ProcessTransition {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "process_transition_id_seq")
	@SequenceGenerator(name = "process_transition_id_seq", initialValue = 10000, allocationSize = 1,
	    sequenceName = "process_transition_id_seq", schema = DbConstants.SCHEMA_NAME)
	@Column(name = "id", updatable = false, nullable = false)
	private int id;
	
	@ReferenceTypeDesignator(referenceType = DefaultReferenceType.PROCESS_TRANSITION_TYPE)
	@ManyToOne(fetch = FetchType.EAGER, cascade = { CascadeType.MERGE }, optional = true)
	@JoinColumn(name = "type")
	private Reference category;
	
	
	@Column(name = "id_current_status", updatable = true, nullable = false)
	private int idCurrentStatus;
	
	@Column(name = "id_next_status", updatable = true, nullable = false)
	private int idNextStatus;
	
	/**
	 * For JPA use only.
	 */
	protected ProcessTransition () {
	}
	
	public ProcessTransition (final Reference category, final int idCurrentStatus, final int idNextStatus) {
		this.category = category;
		this.idCurrentStatus = idCurrentStatus;
		this.idNextStatus = idNextStatus;
	}
}
