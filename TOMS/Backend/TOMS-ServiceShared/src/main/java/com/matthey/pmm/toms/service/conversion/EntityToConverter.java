package com.matthey.pmm.toms.service.conversion;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matthey.pmm.toms.model.CreditCheck;
import com.matthey.pmm.toms.model.DatabaseFile;
import com.matthey.pmm.toms.model.Fill;
import com.matthey.pmm.toms.model.IndexEntity;
import com.matthey.pmm.toms.model.Order;
import com.matthey.pmm.toms.model.OrderComment;
import com.matthey.pmm.toms.model.OrderStatus;
import com.matthey.pmm.toms.model.Party;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.ReferenceOrderLeg;
import com.matthey.pmm.toms.model.ReferenceType;
import com.matthey.pmm.toms.model.User;
import com.matthey.pmm.toms.repository.CreditCheckRepository;
import com.matthey.pmm.toms.repository.DatabaseFileRepository;
import com.matthey.pmm.toms.repository.FillRepository;
import com.matthey.pmm.toms.repository.IndexRepository;
import com.matthey.pmm.toms.repository.OrderCommentRepository;
import com.matthey.pmm.toms.repository.OrderRepository;
import com.matthey.pmm.toms.repository.OrderStatusRepository;
import com.matthey.pmm.toms.repository.PartyRepository;
import com.matthey.pmm.toms.repository.ReferenceOrderLegRepository;
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
	 * Converts a TO to a JPA managed entity that is guaranteed to exist on
	 * the database. If necessary the TO is being persisted to the database.
	 * @param to The transport object to be converted to an entity.
	 * @return
	 */
	public abstract Entity toManagedEntity (TO to);
	
	/**
	 * Overwrite this method in case you want to use the {@link #loadRefType(Object, long)} method
	 * in the child class.
	 * @return
	 */
	public ReferenceTypeRepository refTypeRepo() {
		return null;
	}

	/**
	 * Overwrite this method in case you want to use the {@link #loadType(Object, long)} method
	 * in the child class.
	 * @return
	 */
	public ReferenceRepository refRepo() {
		return null;
	}
	
	/**
	 * Overwrite this method in case you want to use the {@link #loadParty(Object, long)} method
	 * in the child class.
	 * @return
	 */
	public PartyRepository partyRepo() {
		return null;
	}
	
	/**
	 * Overwrite this method in case you want to use the {@link #loadUser(Object, long)} method
	 * in the child class.
	 * @return
	 */
	public UserRepository userRepo() {
		return null;
	}
	
	/**
	 * Overwrite this method in case you want to use the {@link #loadFill(Object, long)} method
	 * in the child class.
	 * @return
	 */
	public FillRepository fillRepo() {
		return null;
	}

	/**
	 * Overwrite this method in case you want to use the {@link #loadOrderComment(Object, long)} method
	 * in the child class.
	 * @return
	 */
	public OrderCommentRepository orderCommentRepo() {
		return null;
	}
	
	/**
	 * Overwrite this method in case you want to use the {@link #loadCreditCheck(Object, long)} method
	 * in the child class.
	 * @return
	 */
	public CreditCheckRepository creditCheckRepo() {
		return null;
	}

	/**
	 * Overwrite this method in case you want to use the {@link #loadOrderStatus(Object, long)} method
	 * in the child class.
	 * @return
	 */
	public OrderStatusRepository orderStatusRepo() {
		return null;
	}
	
	/**
	 * Overwrite this method in case you want to use the {@link #loadIndex(Object, long)} method
	 * in the child class.
	 * @return
	 */
	public IndexRepository indexRepo() {
		return null;
	}
	
	/**
	 * Overwrite this method in case you want to use the {@link #loadReferenceOrderLeg(Object, long)} method
	 * in the child class.
	 * @return
	 */
	public ReferenceOrderLegRepository referenceOrderLegRepo() {
		return null;
	}
	
	/**
	 * Overwrite this method in case you want to use the {@link #loadDatabaseFile(Object, long)} method
	 * in the child class.
	 * @return
	 */
	public DatabaseFileRepository databaseFileRepo() {
		return null;
	}
	
	/**
	 * Overwrite this method in case you want to use the {@link #loadOrder(Object, long)} method
	 * in the child class.
	 * @return
	 */
	public OrderRepository orderRepo() {
		return null;
	}
	
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
			String msg = "Error while converting Transport Object '" + to.toString() + "': "
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
			String msg = "Error while converting Transport Object '" + to.toString() + "': "
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
			String msg = "Error while converting Transport Object '" + to.toString() + "': "
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
			String msg = "Error while converting Transport Object '" + to.toString() + "': "
					+ " can't find the user having ID #" + userId + "."
					+ " Please ensure all instances of member variables are present before conversion.";			
			logger.error(msg);
			throw new RuntimeException (msg);
		}
		return user.get();
	}
	
	/**
 	 * Implement the {@link #fillRepo()} method in the base class to provide a valid repository in case you want to use this method.
	 * @param to
	 * @param refId
	 * @return
	 */
	protected Fill loadFill(TO to, long fillId) {
		Optional<Fill> fill = fillRepo().findById(fillId);
		if (!fill.isPresent()) {
			String msg = "Error while converting Transport Object '" + to.toString() + "': "
					+ " can't find the fill having ID #" + fillId + "."
					+ " Please ensure all instances of member variables are present before conversion.";			
			logger.error(msg);
			throw new RuntimeException (msg);
		}
		return fill.get();
	}
	
	/**
 	 * Implement the {@link #orderCommentRepo()} method in the base class to provide a valid repository in case you want to use this method.
	 * @param to
	 * @param refId
	 * @return
	 */
	protected OrderComment loadOrderComment(TO to, long orderCommentId) {
		Optional<OrderComment> orderComment = orderCommentRepo().findById(orderCommentId);
		if (!orderComment.isPresent()) {
			String msg = "Error while converting Transport Object '" + to.toString() + "': "
					+ " can't find the order comment having ID #" + orderCommentId + "."
					+ " Please ensure all instances of member variables are present before conversion.";			
			logger.error(msg);
			throw new RuntimeException (msg);
		}
		return orderComment.get();
	}
	
	/**
 	 * Implement the {@link #creditCheckRepo()} method in the base class to provide a valid repository in case you want to use this method.
	 * @param to
	 * @param refId
	 * @return
	 */
	protected CreditCheck loadCreditCheck(TO to, long creditCheckId) {
		Optional<CreditCheck> creditCheck = creditCheckRepo().findById(creditCheckId);
		if (!creditCheck.isPresent()) {
			String msg = "Error while converting Transport Object '" + to.toString() + "': "
					+ " can't find the credit check having ID #" + creditCheckId + "."
					+ " Please ensure all instances of member variables are present before conversion.";			
			logger.error(msg);
			throw new RuntimeException (msg);
		}
		return creditCheck.get();
	}
	
	/**
 	 * Implement the {@link #creditCheckRepo()} method in the base class to provide a valid repository in case you want to use this method.
	 * @param to
	 * @param refId
	 * @return
	 */
	protected OrderStatus loadOrderStatus(TO to, long orderStatusId) {
		Optional<OrderStatus> orderStatus = orderStatusRepo().findById(orderStatusId);
		if (!orderStatus.isPresent()) {
			String msg = "Error while converting Transport Object '" + to.toString() + "': "
					+ " can't find the order status having ID #" + orderStatusId + "."
					+ " Please ensure all instances of member variables are present before conversion.";			
			logger.error(msg);
			throw new RuntimeException (msg);
		}
		return orderStatus.get();
	}
	
	/**
 	 * Implement the {@link #indexRepo()} method in the base class to provide a valid repository in case you want to use this method.
	 * @param to
	 * @param indexId
	 * @return
	 */
	protected IndexEntity loadIndex(TO to, long indexId) {
		Optional<IndexEntity> index = indexRepo().findById(indexId);
		if (!index.isPresent()) {
			String msg = "Error while converting Transport Object '" + to.toString() + "': "
					+ " can't find the index having ID #" + indexId + "."
					+ " Please ensure all instances of member variables are present before conversion.";			
			logger.error(msg);
			throw new RuntimeException (msg);
		}
		return index.get();
	}
	
	/**
 	 * Implement the {@link #referenceOrderLegRepo()} method in the base class to provide a valid repository in case you want to use this method.
	 * @param to
	 * @param legId
	 * @return
	 */
	protected ReferenceOrderLeg loadReferenceOrderLeg(TO to, long legId) {
		Optional<ReferenceOrderLeg> leg = referenceOrderLegRepo().findById(legId);
		if (!leg.isPresent()) {
			String msg = "Error while converting Transport Object '" + to.toString() + "': "
					+ " can't find the Reference Order Leg having ID #" + legId + "."
					+ " Please ensure all instances of member variables are present before conversion.";			
			logger.error(msg);
			throw new RuntimeException (msg);
		}
		return leg.get();
	}
	
	/**
 	 * Implement the {@link #databaseFileRepo()} method in the base class to provide a valid repository in case you want to use this method.
	 * @param to
	 * @param databaseFileId
	 * @return 
	 */
	protected DatabaseFile loadDatabaseFile(TO to, long databaseFileId) {
		Optional<DatabaseFile> file = databaseFileRepo().findById(databaseFileId);
		if (!file.isPresent()) {
			String msg = "Error while converting Transport Object '" + to.toString() + "': "
					+ " can't find the Database File having ID #" + databaseFileId + "."
					+ " Please ensure all instances of member variables are present before conversion.";			
			logger.error(msg);
			throw new RuntimeException (msg);
		}
		return file.get();
	}
	
	/**
 	 * Implement the {@link #databaseFileRepo()} method in the base class to provide a valid repository in case you want to use this method.
	 * @param to
	 * @param databaseFileId
	 * @return 
	 */
	protected Order loadOrder(TO to, long orderId) {
		Optional<Order> file = orderRepo().findLatestByOrderId(orderId);
		if (!file.isPresent()) {
			String msg = "Error while converting Transport Object '" + to.toString() + "': "
					+ " can't find the Order having ID #" + orderId + "."
					+ " Please ensure all instances of member variables are present before conversion.";			
			logger.error(msg);
			throw new RuntimeException (msg);
		}
		return file.get();
	}
}
