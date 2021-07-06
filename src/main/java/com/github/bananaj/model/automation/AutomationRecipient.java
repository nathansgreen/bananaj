package com.github.bananaj.model.automation;

import org.json.JSONObject;

public class AutomationRecipient {

	private String listId;
	private boolean listIsActive;
	private String listName;
	//private SegmentOpts segment_opts;
	private String storeId;

	/**
	 * Construct AutomationRecipient for automation creation operation. 
	 * @param listId
	 * @param storeId
	 */
	public AutomationRecipient(String listId, String storeId) {
		super();
		this.listId = listId;
		this.storeId = storeId;
	}

	public AutomationRecipient(JSONObject recipients) {
		this.listId = recipients.getString("list_id");
		this.listIsActive = recipients.getBoolean("list_is_active");
		this.listName = recipients.getString("list_name");
		if (recipients.has("store_id")) {
			this.storeId = recipients.getString("store_id");
		}
	}

	public AutomationRecipient() {

	}

	public AutomationRecipient(Builder b) {
		this.listId = b.listId;
		this.listIsActive = b.listIsActive;
		this.listName = b.listName;
		//this.segment_opts = b.segment_opts;
		b.storeId = b.storeId;
	}

	/**
	 * The unique list id
	 */
	public String getListId() {
		return listId;
	}

	/**
	 * The unique list id
	 * @param listId
	 */
	public void setListId(String listId) {
		this.listId = listId;
	}

	/**
	 * The id of the store
	 */
	public String getStoreId() {
		return storeId;
	}

	/**
	 * The id of the store
	 * @param storeId
	 */
	public void setStoreId(String storeId) {
		this.storeId = storeId;
	}

	/**
	 * The status of the list used, namely if it’s deleted or disabled
	 */
	public boolean isListIsActive() {
		return listIsActive;
	}

	/**
	 * List Name
	 */
	public String getListName() {
		return listName;
	}

	/**
	 * Helper method to convert JSON for mailchimp PATCH/POST operations
	 */
	public JSONObject getJsonRepresentation() throws Exception {
		JSONObject json = new JSONObject();

		if (listId != null) {
			json.put("list_id", listId);
		}
		if (storeId != null) {
			json.put("store_id", storeId);
		}

		return json;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return 
				"Recipients:" + System.lineSeparator() +
				"    List Id: " + getListId() + System.lineSeparator() +
				"    List Name: " + getListName() + System.lineSeparator() +
				"    List is Active: " + isListIsActive() + System.lineSeparator() +
				"    Store Id: " + getStoreId();
	}

	/**
	 * Builder for {@link AutomationRecipient}
	 */
	public static class Builder {
		private String listId;
		private boolean listIsActive;
		private String listName;
		//private SegmentOpts segment_opts;
		private String storeId;

		public Builder setListId(String listId) {
			this.listId = listId;
			return this;
		}

		public Builder setListIsActive(boolean listIsActive) {
			this.listIsActive = listIsActive;
			return this;
		}

		public Builder setListName(String listName) {
			this.listName = listName;
			return this;
		}

		public Builder setStoreId(String storeId) {
			this.storeId = storeId;
			return this;
		}

		public AutomationRecipient build() {
			return new AutomationRecipient(this);
		}
	}
}
