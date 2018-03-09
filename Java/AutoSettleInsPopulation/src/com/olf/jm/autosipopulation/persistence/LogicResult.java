package com.olf.jm.autosipopulation.persistence;

import java.util.ArrayList;
import java.util.List;

import com.olf.jm.autosipopulation.model.DecisionData;
import com.olf.jm.autosipopulation.model.Pair;

/*
 * History:
 * 2015-04-29	V1.0	jwaechter	- Initial version.
 * 2015-09-29	V1.1	jwaechter	- corrected defect in constructor for CONFIRM_NO_INTERNAL_SET_EXTERNAL / CONFIRM_NO_EXTERNAL_SET_INTERNAL
 * 2016-06-01   V1.2    jwaechter   - added flip logic 
 */

/**
 * Class representing "what to do" as reaction to the input parameters. It's used for storage purposes only.
 * Note that is can also store the input from the user if this is required. 
 * There is also an additional part of logic that is flipping the action if necessary. This flipping is related to offset
 * deals and means that internal and external business unit are mirrored if comparing the main deal with it's offset deal.
 * The flipping logic is flipping internal with external actions therefore.
 * @author jwaechter
 * @version 1.2
 */
public class LogicResult {
	public static enum Action {SET_BOTH, SELECT_USER_INTERNAL_SET_EXTERNAL, SELECT_USER_EXTERNAL_SET_INTERNAL, 
		SELECT_USER_INTERNAL_CONFIRM_EXTERNAL, SELECT_USER_EXTERNAL_CONFIRM_INTERNAL, SELECT_USER_BOTH,
		CONFIRM_NO_INTERNAL_SET_EXTERNAL, CONFIRM_NO_EXTERNAL_SET_INTERNAL, CONFIRM_BOTH, NONE };

		private final Action action;
		private final String messageToUser;
		private final List<Pair<Integer, String>> internalSettleIns;
		private final List<Pair<Integer, String>> externalSettleIns;
		private final int intStlIns;
		private final int extStlIns;
		private final boolean hasBeenFlipped;
		
		private final DecisionData dd;

		private int userSelectedIntStlIns=-1;
		private int userSelectedExtStlIns=-1;

		/**
		 * Constructs a clone of an existing logic result but changes the tran num on the 
		 * decision data and flips internal / external. 
		 */
		public LogicResult (LogicResult toBeFlipped, int tranNum, String offsetTranType) {
			this.dd = new DecisionData(tranNum, toBeFlipped.getDd().getLegNum(), toBeFlipped.getDd());
			this.dd.setOffsetTranType(offsetTranType);
			this.dd.flipInternalExternal();
			this.hasBeenFlipped = true;
			
			if (toBeFlipped.hasBeenFlipped == false) {
				switch (toBeFlipped.action) {
				default:
				case CONFIRM_BOTH:
				case SELECT_USER_BOTH:
				case SET_BOTH:
				case NONE:
					this.action = toBeFlipped.action;
					break;
				case CONFIRM_NO_EXTERNAL_SET_INTERNAL:
					this.action = Action.CONFIRM_NO_INTERNAL_SET_EXTERNAL;
					break;
				case CONFIRM_NO_INTERNAL_SET_EXTERNAL:
					this.action = Action.CONFIRM_NO_EXTERNAL_SET_INTERNAL;
					break;
				case SELECT_USER_EXTERNAL_CONFIRM_INTERNAL:
					this.action = Action.SELECT_USER_INTERNAL_CONFIRM_EXTERNAL;
					break;
				case SELECT_USER_EXTERNAL_SET_INTERNAL:
					this.action = Action.SELECT_USER_INTERNAL_SET_EXTERNAL;
					break;
				case SELECT_USER_INTERNAL_CONFIRM_EXTERNAL:
					this.action = Action.SELECT_USER_EXTERNAL_CONFIRM_INTERNAL;
					break;
				case SELECT_USER_INTERNAL_SET_EXTERNAL:
					this.action = Action.SELECT_USER_EXTERNAL_SET_INTERNAL;
					break;
				}
				messageToUser = "<should not pop up>";
				internalSettleIns = toBeFlipped.getExternalSettleIns();
				externalSettleIns = toBeFlipped.getInternalSettleIns();
				intStlIns = toBeFlipped.getExtStlIns();
				extStlIns = toBeFlipped.getIntStlIns();
				userSelectedExtStlIns = toBeFlipped.getUserSelectedIntStlIns();
				userSelectedIntStlIns = toBeFlipped.getUserSelectedExtStlIns();		
			} else {
				messageToUser = "<should not pop up>";
				action = toBeFlipped.action;
				externalSettleIns = toBeFlipped.externalSettleIns;
				extStlIns = toBeFlipped.extStlIns;
				internalSettleIns = toBeFlipped.internalSettleIns;
				intStlIns = toBeFlipped.intStlIns;
				userSelectedExtStlIns = toBeFlipped.userSelectedExtStlIns;
				userSelectedIntStlIns = toBeFlipped.userSelectedIntStlIns;
			}
		}		

		/**
		 * Constructor for {@link Action#NONE} 
		 */
		public LogicResult (final DecisionData dd) {
			this.hasBeenFlipped = false;
			this.action = Action.NONE;
			this.messageToUser = null;
			this.internalSettleIns = null;
			this.externalSettleIns = null;		
			this.extStlIns = 0;
			this.intStlIns = 0;
			this.dd = dd;
		}		

		/**
		 * Constructor for {@link Action#SET_BOTH}
		 * @param selIntStlIns
		 * @param selExtStlIns
		 */
		public LogicResult (int selIntStlIns, int selExtStlIns, final DecisionData dd) {
			this.hasBeenFlipped = false;
			this.action = Action.SET_BOTH;
			this.messageToUser = null;
			this.internalSettleIns = null;
			this.externalSettleIns = null;		
			this.intStlIns = selIntStlIns;
			this.extStlIns = selExtStlIns;
			this.dd = dd;
		}		

		/**
		 * Constructor for {@link Action#CONFIRM_NO_EXTERNAL_SET_INTERNAL}, {@link Action#CONFIRM_NO_INTERNAL_SET_EXTERNAL}
		 * @param action 
		 * @param messageToUser 
		 */
		public LogicResult (Action action, String messageToUser, int settleIns, final DecisionData dd) {
			this.hasBeenFlipped = false;
			this.action = action;
			this.messageToUser = messageToUser;
			this.internalSettleIns = null;
			this.externalSettleIns = null;	

			switch (action) {
			case CONFIRM_NO_INTERNAL_SET_EXTERNAL:
				this.intStlIns = 0;
				this.extStlIns = settleIns;
				break;
			case CONFIRM_NO_EXTERNAL_SET_INTERNAL:
				this.extStlIns = 0;
				this.intStlIns = settleIns;
				break;
			default:
				throw new IllegalArgumentException ("Error: action " + action + " has to be either " + Action.CONFIRM_NO_EXTERNAL_SET_INTERNAL + " or " + Action.CONFIRM_NO_EXTERNAL_SET_INTERNAL);
			}
			this.dd = dd;
		}

		/**
		 * Constructor for {@link Action#SELECT_USER_EXTERNAL_CONFIRM_INTERNAL} and {@link Action#SELECT_USER_INTERNAL_CONFIRM_EXTERNAL} 
		 * @param action
		 * @param messageToUser
		 * @param settleIns
		 */
		public LogicResult (Action action, String messageToUser, List<Pair<Integer, String>> settleIns, final DecisionData dd) {
			this.hasBeenFlipped = false;
			this.action = action;
			this.messageToUser = messageToUser;
			this.extStlIns = 0;
			this.intStlIns = 0;
			switch (action) {
			case SELECT_USER_EXTERNAL_CONFIRM_INTERNAL:
				this.internalSettleIns = null;
				this.externalSettleIns = settleIns;						
				break;
			case SELECT_USER_INTERNAL_CONFIRM_EXTERNAL:
				this.internalSettleIns = settleIns;
				this.externalSettleIns = null;
				break;
			default:
				throw new IllegalArgumentException ("Error: action " + action + " has to be either " + Action.SELECT_USER_EXTERNAL_CONFIRM_INTERNAL + " or " + Action.SELECT_USER_INTERNAL_CONFIRM_EXTERNAL);
			}
			this.dd = dd;
		}

		/**
		 * Constructor for {@link Action#SELECT_USER_EXTERNAL_SET_INTERNAL } and {@link Action#SELECT_USER_INTERNAL_SET_EXTERNAL} 
		 * @param action
		 * @param messageToUser
		 * @param settleInstruments
		 */
		public LogicResult (Action action, String messageToUser, List<Pair<Integer, String>> settleInstruments, int settleIns, final DecisionData dd) {
			this.hasBeenFlipped = false;
			this.action = action;
			this.messageToUser = messageToUser;
			switch (action) {
			case SELECT_USER_EXTERNAL_SET_INTERNAL:
				this.internalSettleIns = null;
				this.externalSettleIns = settleInstruments;						
				this.intStlIns = settleIns;
				this.extStlIns = 0;
				break;
			case SELECT_USER_INTERNAL_SET_EXTERNAL:
				this.internalSettleIns = settleInstruments;
				this.externalSettleIns = null;
				this.extStlIns = settleIns;
				this.intStlIns = 0;
				break;
			default:
				throw new IllegalArgumentException ("Error: action " + action + " has to be either " + Action.SELECT_USER_EXTERNAL_SET_INTERNAL + " or " + Action.SELECT_USER_INTERNAL_SET_EXTERNAL);
			}
			this.dd = dd;
		}

		/**
		 * Constructor for {@link Action#CONFIRM_BOTH}
		 * @param messageToUser
		 * @param intSettleIns
		 * @param extSettleIns
		 */
		public LogicResult (String messageToUser, final DecisionData dd) {
			this.hasBeenFlipped = false;
			this.action = Action.CONFIRM_BOTH;
			this.messageToUser = messageToUser;
			this.extStlIns = 0;
			this.intStlIns = 0;

			this.internalSettleIns = null;
			this.externalSettleIns = null;	
			this.dd = dd;
		}

		/**
		 * Constructor for {@link Action#SELECT_USER_BOTH}
		 * @param messageToUser
		 * @param intSettleIns
		 * @param extSettleIns
		 */
		public LogicResult (String messageToUser, List<Pair<Integer, String>> intSettleIns, List<Pair<Integer, String>> extSettleIns, final DecisionData dd) {
			this.hasBeenFlipped = false;
			this.action = Action.SELECT_USER_BOTH;
			this.messageToUser = messageToUser;
			this.extStlIns = 0;
			this.intStlIns = 0;

			this.internalSettleIns = intSettleIns;
			this.externalSettleIns = extSettleIns;			
			this.dd = dd;
		}

		public DecisionData getDd() {
			return dd;
		}

		public Action getAction() {
			return action;
		}

		public String getMessageToUser() {
			return messageToUser;
		}

		public List<Pair<Integer, String>> getInternalSettleIns() {
			return internalSettleIns;
		}

		public List<Pair<Integer, String>> getExternalSettleIns() {
			return externalSettleIns;
		}

		public int getIntStlIns() {
			return intStlIns;
		}

		public int getExtStlIns() {
			return extStlIns;
		}

		public int getUserSelectedIntStlIns() {
			return userSelectedIntStlIns;
		}

		public void setUserSelectedIntStlIns(int userSelectedIntStlIns) {
			this.userSelectedIntStlIns = userSelectedIntStlIns;
		}

		public int getUserSelectedExtStlIns() {
			return userSelectedExtStlIns;
		}

		public void setUserSelectedExtStlIns(int userSelectedExtStlIns) {
			this.userSelectedExtStlIns = userSelectedExtStlIns;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((action == null) ? 0 : action.hashCode());
			result = prime * result + extStlIns;
			result = prime
					* result
					+ ((externalSettleIns == null) ? 0 : externalSettleIns
							.hashCode());
			result = prime * result + intStlIns;
			result = prime
					* result
					+ ((internalSettleIns == null) ? 0 : internalSettleIns
							.hashCode());
			result = prime * result
					+ ((messageToUser == null) ? 0 : messageToUser.hashCode());
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
			LogicResult other = (LogicResult) obj;
			if (action != other.action)
				return false;
			if (extStlIns != other.extStlIns)
				return false;
			if (externalSettleIns == null) {
				if (other.externalSettleIns != null)
					return false;
			} else if (!externalSettleIns.equals(other.externalSettleIns))
				return false;
			if (intStlIns != other.intStlIns)
				return false;
			if (internalSettleIns == null) {
				if (other.internalSettleIns != null)
					return false;
			} else if (!internalSettleIns.equals(other.internalSettleIns))
				return false;
			if (messageToUser == null) {
				if (other.messageToUser != null)
					return false;
			} else if (!messageToUser.equals(other.messageToUser))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "LogicResult [action=" + action + ", messageToUser="
					+ messageToUser + ", internalSettleIns=" + internalSettleIns
					+ ", externalSettleIns=" + externalSettleIns + ", intStlIns="
					+ intStlIns + ", extStlIns=" + extStlIns + "]";
		}
}
