package com.github.bananaj.model.automation;

import org.json.JSONObject;

/**
 * Mailchimp's Automation feature lets you build a series of emails that send to
 * subscribers when triggered by a specific date, activity, or event. Use the
 * API to manage Automation workflows, emails, and queues.
 *
 */
public class Subscriber {

	private String id;
	private String workflowId;
	private String listId;
	private String emailAddress;

	public Subscriber(JSONObject jsonObj) {
		id = jsonObj.getString("id");
		this.workflowId = jsonObj.getString("workflow_id");
		this.listId = jsonObj.getString("list_id");
		this.emailAddress = jsonObj.getString("email_address");
	}

	public Subscriber() {

	}

	public Subscriber(Builder b) {
		this.id = b.id;
		this.workflowId = b.workflowId;
		this.listId = b.listId;
		this.emailAddress = b.emailAddress;
	}

	/**
	 * @return The MD5 hash of the lowercase version of the list member's email address.
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * A string that uniquely identifies an Automation workflow
	 */
	public String getWorkflowId() {
		return workflowId;
	}

	/**
	 * A string that uniquely identifies a list
	 */
	public String getListId() {
		return listId;
	}

	/**
	 * The list member’s email address
	 */
	public String getEmailAddress() {
		return emailAddress;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return 
				"    Subscriber Id: " + getId() + System.lineSeparator() +
				"    Workflow Id: " + getWorkflowId() + System.lineSeparator() +
				"    List Id: " + getListId() + System.lineSeparator() +
				"    Email Address: " + getEmailAddress();
	}

	public static class Builder {
		private String id;
		private String workflowId;
		private String listId;

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setWorkflowId(String workflowId) {
			this.workflowId = workflowId;
			return this;
		}

		public Builder setListId(String listId) {
			this.listId = listId;
			return this;
		}

		public Builder setEmailAddress(String emailAddress) {
			this.emailAddress = emailAddress;
			return this;
		}

		private String emailAddress;

		public Subscriber build() {
			return new Subscriber(this);
		}
	}
}
