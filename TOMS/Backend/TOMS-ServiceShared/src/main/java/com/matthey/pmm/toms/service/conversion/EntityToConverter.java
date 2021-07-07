package com.matthey.pmm.toms.service.conversion;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.ReferenceType;
import com.matthey.pmm.toms.repository.ReferenceRepository;
import com.matthey.pmm.toms.repository.ReferenceTypeRepository;

public abstract class EntityToConverter <Entity, TO> {
    protected static final Logger logger = LoggerFactory.getLogger(EntityToConverter.class);
    
	/**
	 * Converts an entity to its corresponding TO.
	 * @param entity The entity to be converted to a TO.
	 * @return
	 */
	public abstract TO toTo (Entity entity);
	
	public abstract ReferenceTypeRepository refTypeRepo();	

	public abstract ReferenceRepository refRepo();	

	
	/**
	 * Converts a TO to a JPA managed entity that is guaranteed to exist on
	 * the database. If necessary the TO is being persisted to the database.
	 * @param to The transport object to be converted to an entity.
	 * @return
	 */
	public abstract Entity toManagedEntity (TO to);	
	
	protected ReferenceType loadRefType(TO to, long refTypeId) {
		Optional<ReferenceType> type = refTypeRepo().findById(refTypeId);
		if (!type.isPresent()) {
			String msg = "Error why converting Transport Object '" + to.toString() + "': "
					+ " can't find the reference type having ID #" + refTypeId + "."
					+ " Please ensure all instances of member variables are present before conversion.";			
			logger.error(msg);
			throw new RuntimeException (msg);
		}
		return type.get();
	}
	
	protected Reference loadRef(TO to, long refId) {
		Optional<Reference> ref = refRepo().findById(refId);
		if (!ref.isPresent()) {
			String msg = "Error why converting Transport Object '" + to.toString() + "': "
					+ " can't find the reference having ID #" + refId + "."
					+ " Please ensure all instances of member variables are present before conversion.";			
			logger.error(msg);
			throw new RuntimeException (msg);
		}
		return ref.get();
	}
}
