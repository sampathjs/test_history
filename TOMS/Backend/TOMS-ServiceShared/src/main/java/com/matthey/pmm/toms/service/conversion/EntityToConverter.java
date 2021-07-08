package com.matthey.pmm.toms.service.conversion;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matthey.pmm.toms.model.Party;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.ReferenceType;
import com.matthey.pmm.toms.model.User;
import com.matthey.pmm.toms.repository.PartyRepository;
import com.matthey.pmm.toms.repository.ReferenceRepository;
import com.matthey.pmm.toms.repository.ReferenceTypeRepository;
import com.matthey.pmm.toms.repository.UserRepository;
import com.matthey.pmm.toms.service.TomsService;

public abstract class EntityToConverter <Entity, TO> {
    protected static final Logger logger = LoggerFactory.getLogger(EntityToConverter.class);
    
	/**
	 * Converts an entity to its corresponding TO.
	 * @param entity The entity to be converted to a TO.
	 * @return
	 */
	public abstract TO toTo (Entity entity);
	
	/**
	 * Overwrite this class in case you want to use the {@link #loadRefType(Object, long)} method
	 * in the child class.
	 * @return
	 */
	public ReferenceTypeRepository refTypeRepo() {
		return null;
	}

	/**
	 * Overwrite this class in case you want to use the {@link #loadType(Object, long)} method
	 * in the child class.
	 * @return
	 */
	public ReferenceRepository refRepo() {
		return null;
	}
	
	/**
	 * Overwrite this class in case you want to use the {@link #loadParty(Object, long)} method
	 * in the child class.
	 * @return
	 */
	public PartyRepository partyRepo() {
		return null;
	}
	
	/**
	 * Overwrite this class in case you want to use the {@link #loadUser(Object, long)} method
	 * in the child class.
	 * @return
	 */
	public UserRepository userRepo() {
		return null;
	}
	
	/**
	 * Converts a TO to a JPA managed entity that is guaranteed to exist on
	 * the database. If necessary the TO is being persisted to the database.
	 * @param to The transport object to be converted to an entity.
	 * @return
	 */
	public abstract Entity toManagedEntity (TO to);
	
	protected String formatDateTime (Date dateTime) {
		if (dateTime != null)  {
			SimpleDateFormat sdfDateTime = new SimpleDateFormat (TomsService.DATE_TIME_FORMAT);
			return sdfDateTime.format(dateTime);			
		}
		return null;
	}

	protected String formatDate (Date date) {
		if (date != null) {
			SimpleDateFormat sdfDateTime = new SimpleDateFormat (TomsService.DATE_FORMAT);
			return sdfDateTime.format(date);			
		} 
		return null;
	}
	
	protected Date parseDateTime (TO to, String dateTime) {
		if (dateTime != null) {
			SimpleDateFormat sdfDateTime = new SimpleDateFormat (TomsService.DATE_TIME_FORMAT);
			try {
				return sdfDateTime.parse(dateTime);
			} catch (ParseException e) {
				throw new RuntimeException ("Error while converting entity '" 
						+ to + "', DateTime: '" + dateTime + "'. Expected Date format is "
						 + TomsService.DATE_TIME_FORMAT);
			}			
		}
		return null;
	}
	
	protected Date parseDate (TO to, String date) {
		if (date != null) {
			SimpleDateFormat sdfDateTime = new SimpleDateFormat (TomsService.DATE_FORMAT);
			try {
				return sdfDateTime.parse(date);
			} catch (ParseException e) {
				throw new RuntimeException ("Error while converting entity '" 
						+ to + "', Date: '" + date + "'. Expected Date format is "
						 + TomsService.DATE_FORMAT);
			}			
		} 
		return null;
	}
	
	/**
	 * Implement the {@link #refTypeRepo()} method in the base class to provide a valid repository in case you want to use this method.
	 * @param to
	 * @param refTypeId
	 * @return
	 */
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

	/**
 	 * Implement the {@link #refRepo()} method in the base class to provide a valid repository in case you want to use this method.
	 * @param to
	 * @param refId
	 * @return
	 */
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

	/**
 	 * Implement the {@link #partyRepo()} method in the base class to provide a valid repository in case you want to use this method.
	 * @param to
	 * @param refId
	 * @return
	 */
	protected Party loadParty(TO to, long partyId) {
		Optional<Party> party = partyRepo().findById(partyId);
		if (!party.isPresent()) {
			String msg = "Error why converting Transport Object '" + to.toString() + "': "
					+ " can't find the party having ID #" + partyId + "."
					+ " Please ensure all instances of member variables are present before conversion.";			
			logger.error(msg);
			throw new RuntimeException (msg);
		}
		return party.get();
	}	
	
	/**
 	 * Implement the {@link #userRepo()} method in the base class to provide a valid repository in case you want to use this method.
	 * @param to
	 * @param refId
	 * @return
	 */
	protected User loadUser(TO to, long userId) {
		Optional<User> user = userRepo().findById(userId);
		if (!user.isPresent()) {
			String msg = "Error why converting Transport Object '" + to.toString() + "': "
					+ " can't find the user having ID #" + userId + "."
					+ " Please ensure all instances of member variables are present before conversion.";			
			logger.error(msg);
			throw new RuntimeException (msg);
		}
		return user.get();
	}	
}
